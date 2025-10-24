
package com.atakmap.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.support.util.Base64;
import android.text.Editable;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.location.LocationMapComponent;
import com.atakmap.android.util.AfterTextChangedWatcher;
import com.atakmap.annotations.FortifyFinding;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.database.QueryIface;
import com.atakmap.database.impl.DatabaseImpl;
import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

public class ATAKDatabaseHelper {

    final static int KEY_STATUS_OK = 1;
    final static int KEY_STATUS_FAILED = 0;
    private static final String TAG = "ATAKDatabaseHelper";
    private static final String[] ALL_ENC_DBS = new String[] {
            "Databases/ChatDb2.sqlite",
            "Databases/statesaver2.sqlite",
            "Databases/crumbs2.sqlite",
            "Databases/filteredcontacts.sqlite"
    };

    public interface KeyStatusListener {
        void onKeyStatus(int ks);
    }

    /**
     * Prompts for a key from the user and tests the key to make sure it is valid.
     * If it is valid, then the process continues.   If it is not valid, then the 
     * user has the option to retry, move the databases out of the way, delete the
     * databases, or to exit the app.  
     * @param context the application context.
     * @param ksl the key status listener for when the key is entered properly.
     */
    public static void promptForKey(final Context context,
            final KeyStatusListener ksl) {
        final Handler handler = new Handler();
        Thread t = new Thread(new Runnable() {
            public void run() {
                promptForKey(context, ksl, handler);
            }
        }, "promptForKey");
        t.start();
    }

    private static void promptForKey(final Context context,
            final KeyStatusListener ksl, final Handler handler) {

        final File testDb = FileSystemUtils.getItem("Databases/ChatDb2.sqlite");

        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        "com.atakmap.app.v2");

        if (credentials == null)
            credentials = new AtakAuthenticationCredentials();


        // DB doesn't exist and there is no key; create original key
        if (!IOProviderFactory.exists(testDb) && (credentials == null
            || FileSystemUtils.isEmpty(credentials.username))) {
            changeKeyImpl(context, true, ksl);
            return;

        }

        
        // DB doesn't exist but we have key; create
        if (!IOProviderFactory.exists(testDb)) {
            upgrade(context, ksl);
            return;
        }

        // Initial check if we can open db
        for (String db : ALL_ENC_DBS) {
            File dbFile = FileSystemUtils.getItem(db);

            if (IOProviderFactory.isDefault()
                    && !checkKeyAgainstDatabase(dbFile)) {
                Log.d(TAG, "Failed to open encrypted db " + db);
            } else if (!IOProviderFactory.isDefault()) {
                DatabaseIface ctDb = IOProviderFactory.createDatabase(dbFile);
                if (ctDb != null)
                    storePrefetchDb(dbFile, ctDb);
            }
        }

        if (IOProviderFactory.isDefault() && !checkKeyAgainstDatabase(testDb)) {
            handler.post(new Runnable() {
                public void run() {
                    showPrompt(testDb, context, ksl);
                }
            });
        } else if (ksl != null) {
            handler.post(new Runnable() {
                public void run() {
                    ksl.onKeyStatus(KEY_STATUS_OK);
                }
            });
        }
    }

    private static void showPrompt(final File testDb, final Context context,
            final KeyStatusListener ksl) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setCancelable(false);

        LayoutInflater inflater = LayoutInflater.from(context);

        final View dkView = inflater.inflate(R.layout.database_key, null);

        ad.setView(dkView).setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int id) {
                        EditText et = dkView
                                .findViewById(R.id.key_entry);
                        String key = et.getText().toString();
                        if (FileSystemUtils.isEmpty(key)) {
                            promptForKey(context, ksl);
                        } else if (key.contains("'")) {
                            Toast.makeText(context,
                                    R.string.invalid_passphrase_tick,
                                    Toast.LENGTH_LONG)
                                    .show();

                            promptForKey(context, ksl);
                        } else {
                            AtakAuthenticationDatabase.saveCredentials(
                                    AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                                    "com.atakmap.app.v2", "atakuser", key,
                                    false);
                            if (checkKeyAgainstDatabase(testDb)) {
                                // Provided key passed. Also check all other encrypted dbs so they
                                // get cached/prefetched
                                promptForKey(context, ksl);

                            } else {
                                Toast.makeText(context,
                                        R.string.invalid_passphrase,
                                        Toast.LENGTH_SHORT).show();
                                promptForKey(context, ksl);
                            }
                        }

                    }
                }).setNeutralButton(R.string.remove_and_quit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                promptForRemoval(context, ksl);
                            }
                        })
                .setNegativeButton(R.string.quit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                if (ksl != null)
                                    ksl.onKeyStatus(KEY_STATUS_FAILED);
                            }
                        });
        ad.show();
    }

    public static void removeDatabases() {
        IOProviderFactory.delete(
                FileSystemUtils.getItem("Databases/ChatDb2.sqlite"),
                IOProvider.SECURE_DELETE);
        IOProviderFactory.delete(
                FileSystemUtils.getItem("Databases/statesaver2.sqlite"),
                IOProvider.SECURE_DELETE);
        IOProviderFactory.delete(
                FileSystemUtils.getItem("Databases/crumbs2.sqlite"),
                IOProvider.SECURE_DELETE);
    }

    private static void promptForRemoval(final Context context,
            final KeyStatusListener ksl) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setCancelable(false);

        ad.setTitle(R.string.encrypted_removal)
                .setMessage(R.string.encrypted_removal_message)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                removeDatabases();
                                if (ksl != null)
                                    ksl.onKeyStatus(KEY_STATUS_FAILED);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int id) {
                                if (ksl != null)
                                    ksl.onKeyStatus(KEY_STATUS_FAILED);
                            }
                        });
        ad.show();
    }

    private static void upgrade(final Context context,
            final KeyStatusListener ksl) {


        try {
            new Handler(Looper.getMainLooper()).post(() -> {
                final ProgressDialog encryptDialog = new ProgressDialog(
                        context);

                encryptDialog.setIcon(R.drawable.passphrase);
                encryptDialog.setTitle("Encrypting Data");
                encryptDialog.setMessage("please wait...");
                encryptDialog.setCancelable(false);
                encryptDialog.setIndeterminate(true);
                encryptDialog.show();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        encryptDb(FileSystemUtils.getItem("Databases/ChatDb.sqlite"),
                                FileSystemUtils.getItem("Databases/ChatDb2.sqlite"));
                        encryptDb(
                                FileSystemUtils.getItem("Databases/statesaver.sqlite"),
                                FileSystemUtils
                                        .getItem("Databases/statesaver2.sqlite"));
                        encryptDb(FileSystemUtils.getItem("Databases/crumbs.sqlite"),
                                FileSystemUtils.getItem("Databases/crumbs2.sqlite"));

                        encryptDialog.dismiss();
                        if (ksl != null)
                            ksl.onKeyStatus(KEY_STATUS_OK);
                    }
                };
                Thread t = new Thread(r, TAG + "-Upgrade");
                t.start();
            });
        } catch(Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            Log.e(TAG, "Unknown Error: " + ex.getMessage() + " - " + sw);
        }
    }

    private static final int REKEY_SUCCESS = 0;
    private static final int REKEY_SAME = 1;
    private static final int REKEY_FAILED = 2;

    private static int rekey(final String[] dbs, final String key,
            boolean legacy) {

        if (FileSystemUtils.isEmpty(key))
            return REKEY_FAILED;

        for (String db : dbs) {
            File ctFile = FileSystemUtils.getItem(db);
            if (!IOProviderFactory.exists(ctFile))
                return REKEY_FAILED;
        }

        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        legacy ? "com.atakmap.app" : "com.atakmap.app.v2");

        // do not rekey if the key is the same
        if (credentials != null
                && !FileSystemUtils.isEmpty(credentials.password) &&
                credentials.password.equals(key)) {
            return REKEY_SAME;
        }

        for (String db : dbs) {
            File ctFile = FileSystemUtils.getItem(db);

            if (credentials != null
                    && !FileSystemUtils.isEmpty(credentials.username)) {
                DatabaseIface ctDb = null;
                try {
                    ctDb = DatabaseImpl.open(ctFile.getAbsolutePath(),
                            credentials.password, DatabaseImpl.OPEN_CREATE);
                    ctDb.execute("PRAGMA rekey = '" + key + "'", null);
                } catch (Exception e) {
                    Log.e(TAG, "Database rekey failed for " + ctFile
                            + " - old key may be incorrect", e);
                } finally {
                    if (ctDb != null)
                        try {
                            ctDb.close();
                        } catch (Exception ignore) {
                        }
                }
            }

        }
        return REKEY_SUCCESS;

    }

    private static boolean cipherUpdate(File dbFile) {
        if (!IOProviderFactory.exists(dbFile))
            return false;

        AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                .getCredentials(
                        AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                        "com.atakmap.app.v2");

        if (credentials != null
                && !FileSystemUtils.isEmpty(credentials.username)) {
            DatabaseIface ctDb = Databases.openDatabase(
                    dbFile.getAbsolutePath(), false);
            try {
                try (QueryIface pragma = ctDb.compileQuery("PRAGMA key = '"
                        + credentials.password + "'")) {
                    pragma.moveToNext();
                }
                CursorIface ci = null;
                try {
                    ci = ctDb.query("PRAGMA cipher_migrate",
                            null);
                    long ret = -1;

                    if (ci.moveToNext()) {
                        ret = ci.getLong(0);
                    }
                    if (ret == 0)
                        return true;
                    else
                        Log.d(TAG, "cipher migration failed");

                } catch (SQLiteException e) {
                    Log.d(TAG, "corrupt database", e);
                } finally {
                    if (ci != null)
                        ci.close();
                }
            } finally {
                try {
                    ctDb.close();
                } catch (Exception ignore) {
                }
            }
        }
        return false;
    }

    public static void changeKey(final Context context) {
        changeKeyImpl(context, false, null);
    }

    private static void changeKeyImpl(final Context context, final boolean init, final KeyStatusListener listener) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            final View dkView = inflater.inflate(R.layout.database_rekey, null);

            final EditText key_entry = dkView.findViewById(R.id.key_entry);
            final EditText key_entry2 = dkView.findViewById(R.id.key_entry2);
            final RadioButton self = dkView.findViewById(R.id.self);
            final TextView passwordWarning = dkView.findViewById(R.id.warningText);
            final CheckBox checkBox = dkView.findViewById(R.id.password_checkbox);
            final RadioButton autogeneratedCheckBox = dkView.findViewById(R.id.autogenerated);

            // Disable the auto generated password and hide it,
            // Enable the password entry
            key_entry.setEnabled(true);
            key_entry2.setEnabled(true);
            autogeneratedCheckBox.setVisibility(View.GONE);
            self.setChecked(true);
            self.setVisibility(View.GONE);
            new Handler(Looper.getMainLooper()).post(() -> {
                final AlertDialog.Builder ad = new AlertDialog.Builder(context);
                ad.setCancelable(false);
                ad.setView(dkView).setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                EditText et = dkView
                                        .findViewById(R.id.key_entry);
                                String key = et.getText().toString();

                                if (FileSystemUtils.isEmpty(key)) {
                                    changeKeyImpl(context, init,listener);
                                } else if (key.contains("'")) {
                                    Toast.makeText(context,
                                                    "The passphrase cannot contain a ' mark",
                                                    Toast.LENGTH_LONG)
                                            .show();
                                    changeKeyImpl(context, init, listener);
                                } else if(init) {
                                    AtakAuthenticationDatabase.saveCredentials(
                                            AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                                            "com.atakmap.app.v2", "atakuser", key, false);
                                    promptForKey(context, listener);
                                } else {
                                    // please note, the actual encryption is triggered
                                    // by the database classes.  This is because of the
                                    // version number being required.

                                    final ProgressDialog encryptDialog = new ProgressDialog(
                                            context);
                                    encryptDialog.setIcon(R.drawable.passphrase);
                                    encryptDialog.setTitle("Passphrase Change");
                                    encryptDialog
                                            .setMessage(
                                                    "Please wait...\nApplication will exit when finished");
                                    encryptDialog.setCancelable(false);
                                    encryptDialog.setIndeterminate(true);
                                    encryptDialog.show();

                                    Runnable r = new Runnable() {
                                        @Override
                                        public void run() {

                                            final int status = rekey(new String[] {
                                                    "Databases/ChatDb2.sqlite",
                                                    "Databases/statesaver2.sqlite",
                                                    "Databases/crumbs2.sqlite"
                                            }, key, false);
                                            if (status == REKEY_SUCCESS) {

                                                AtakAuthenticationDatabase
                                                        .saveCredentials(
                                                                AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                                                                "com.atakmap.app.v2",
                                                                "atakuser", key,
                                                                false);
                                                Intent i = new Intent(
                                                        "com.atakmap.app.QUITAPP");
                                                i.putExtra("FORCE_QUIT", true);
                                                AtakBroadcast.getInstance()
                                                        .sendBroadcast(i);
                                                encryptDialog.dismiss();
                                            } else {
                                                ((Activity) context)
                                                        .runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                if (status == REKEY_FAILED) {
                                                                    Toast.makeText(
                                                                                    context,
                                                                                    "the passphrase change failed",
                                                                                    Toast.LENGTH_LONG)
                                                                            .show();
                                                                } else {
                                                                    Toast.makeText(
                                                                                    context,
                                                                                    "passphrase entered was already in use",
                                                                                    Toast.LENGTH_LONG)
                                                                            .show();
                                                                }
                                                                encryptDialog.dismiss();
                                                            }
                                                        });
                                            }
                                        }
                                    };
                                    Thread t = new Thread(r, TAG + "-ChangeKey");
                                    t.start();
                                }
                            }
                        }).setNegativeButton(R.string.cancel, null);

                AlertDialog dialog = ad.create();

                AfterTextChangedWatcher atcw = new AfterTextChangedWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            final String pass1 = key_entry.getText().toString();
                            final String pass2 = key_entry2.getText().toString();
                            final boolean passwordEqual = pass1.equals(pass2);
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(passwordEqual);
                            passwordWarning.setVisibility(
                                    passwordEqual ? View.INVISIBLE : View.VISIBLE);
                        } catch (Exception ignored) {
                        }
                    }
                };
                key_entry2.addTextChangedListener(atcw);
                key_entry.addTextChangedListener(atcw);

                ((RadioGroup) dkView.findViewById(R.id.decision))
                        .setOnCheckedChangeListener(
                                new RadioGroup.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(RadioGroup group,
                                                                 int checkedId) {
                                        key_entry.setEnabled(checkedId == R.id.self);
                                        key_entry2.setEnabled(checkedId == R.id.self);
                                        checkBox.setEnabled(checkedId == R.id.self);

                                        if (checkedId == R.id.self) {
                                            atcw.afterTextChanged(null);
                                        } else
                                            dialog.getButton(
                                                            AlertDialog.BUTTON_POSITIVE)
                                                    .setEnabled(true);

                                    }
                                });

                checkBox.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton,
                                                         boolean isChecked) {

                                for (EditText et : new EditText[] {
                                        key_entry, key_entry2
                                }) {
                                    if (isChecked) {
                                        et.setTransformationMethod(
                                                HideReturnsTransformationMethod
                                                        .getInstance());
                                    } else {
                                        et.setTransformationMethod(
                                                PasswordTransformationMethod
                                                        .getInstance());
                                    }
                                    et.setSelection(et.getText().length());
                                }
                            }
                        });

                dialog.show();
            });
        } catch(Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            Log.e(TAG, "Unknown error: " + ex.getMessage() + " - " + sw);
        }
    }

    /**
     * Handles the encryption and the re-encryption of an existing database.
     * Migrate over to encrypted database from plaintext.
     * <a href="https://discuss.zetetic.net/t/how-to-encrypt-a-plaintext-sqlite-database-to-use-sqlcipher-and-avoid-file-is-encrypted-or-is-not-a-database-errors/868">Avoid Errors</a>
     * @param ptFile the file name for the unencrypted database
     * @param ctFile the file name for the encrypted database
     */
    public static void encryptDb(final File ptFile, final File ctFile) {
        if (IOProviderFactory.exists(ptFile)
                && !IOProviderFactory.exists(ctFile)) {

            DatabaseIface ptDb = Databases.openOrCreateDatabase(
                    ptFile.getAbsolutePath());

            AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                    .getCredentials(
                            AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                            "com.atakmap.app.v2");

            @FortifyFinding(finding = "Key Management: Hardcoded Encryption Key", rational = "Used if no encryption is desired")
            String key;
            if (credentials != null
                    && !FileSystemUtils.isEmpty(credentials.username)) {
                key = credentials.password;
            } else {
                // This "empty string" key is from legacy code;  not sure why it is here, but this preserves existing function....
                key = "\"\"";
            }
            DatabaseIface ctDb = DatabaseImpl.open(ctFile.getAbsolutePath(),
                    key, DatabaseImpl.OPEN_CREATE);
            ctDb.setVersion(ptDb.getVersion());

            // Done with plain text db; will reopen as attached db below
            ptDb.close();

            ctDb.execute("ATTACH DATABASE '" + ptFile.getAbsolutePath()
                    + "' AS plain KEY ''", null);
            ctDb.execute("SELECT sqlcipher_export('main', 'plain'", null);
            ctDb.execute("DETACH DATABASE plain", null);
            ctDb.close();

            IOProviderFactory.delete(ptFile, IOProvider.SECURE_DELETE);

        }

    }

    private static boolean checkKeyAgainstDatabase(final File dbFile) {
        if (IOProviderFactory.exists(dbFile)) {

            final String s = "SQLite format 3";
            final byte[] b = new byte[s.length()];
            try (FileInputStream fis = IOProviderFactory
                    .getInputStream(dbFile)) {
                final int bytesRead = fis.read(b);
                if (bytesRead == s.length() && s
                        .equals(new String(b, FileSystemUtils.UTF8_CHARSET))) {
                    return false;
                }
            } catch (Exception ignored) {
                return false;
            }

            DatabaseIface ctDb = null;
            try {

                AtakAuthenticationCredentials credentials = AtakAuthenticationDatabase
                        .getCredentials(
                                AtakAuthenticationCredentials.TYPE_APK_DOWNLOADER,
                                "com.atakmap.app.v2");

                if (credentials != null
                        && !FileSystemUtils.isEmpty(credentials.username)) {
                    ctDb = DatabaseImpl.open(dbFile.getAbsolutePath(),
                            credentials.password, DatabaseImpl.OPEN_CREATE);

                    storePrefetchDb(dbFile, ctDb);
                    ctDb = null;
                    return true;
                }
            } catch (Exception e) {
                Log.d(TAG, "unable to open database " + dbFile
                        + "with current key", e);
            } finally {
                if (ctDb != null)
                    try {
                        ctDb.close();
                    } catch (Exception ignore) {
                    }
            }
            return false;
        } else {
            return true;
        }
    }

    private static final java.util.Map<String, DatabaseIface> dbcache = new HashMap<>();

    private static void storePrefetchDb(File file, DatabaseIface db) {
        synchronized (dbcache) {
            // evict and close any collision to prevent DB connection leak
            final DatabaseIface collision = dbcache
                    .remove(file.getAbsolutePath());
            if (collision != null)
                collision.close();

            // cache the prefetched db
            dbcache.put(file.getAbsolutePath(), db);
        }
    }

    /**
     * Returns a database that was prefetched during initialization.
     *
     * @param file The database path
     * @return  The prefetched database associated with the path or <code>null</code> if no database
     *          for that path was prefetched or if the instance associated with the path was
     *          previously returned from this method.
     */
    public static DatabaseIface getPrefetchDb(File file) {
        synchronized (dbcache) {
            return dbcache.remove(file.getAbsolutePath());
        }
    }
}
