package com.atakmap.android.image;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.chat.ModePicker;
import com.atakmap.android.gui.coordinateentry.CoordinateEntryCapability;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import gov.tak.api.cot.CoordinatedTime;

/**
 * Custom dialog that displays an EditText control to enter in a new file name for the provided path. Custom buttons
 * are available to quickly add keywords to the filename which are customizable. Intended for image files but can be used
 * for general files as well.
 */
class FileRenameDialog implements ModePicker.ModeUpdateListener {

    private final String TAG = "FileRenamingDialog";

    private final Resources _resources;

    private final File _file;
    private final String _ext;
    private final String previousName;
    private boolean needsUpdate;

    private EditText _fileNameEditText;
    private TableLayout _tableLayout;
    private ModePicker _picker;
    private final Context context;
    private final MapView mapView;

    private final AtakPreferences _prefs;

    private final int BUTTONS_PER_ROW = 4;
    private final int NUMBER_OF_ROWS = 5;

    private final String[] MODE_NAMES = new String[]{"1", "2", "3", "4", "5"};

    //stores the grouping and its values for the button layouts
    private final HashMap<String, List<Pair<String, String>>> buttons = new HashMap<>();


    //key that stores the stored set of strings that define the names for the
    private static final String QUICK_IMAGE_NAME_KEYS_KEY = "com.atakmap.android.image.quick_image_name_keys";
    private static final String QUICK_IMAGE_NAME_VALUES_KEY = "com.atakmap.android.image.quick_image_name_values";
    private static final String QUICK_IMAGE_MODE_NUM = "com.atakmap.android.image.quick_image_mode_num";


    private final FileRenamed _callback;

    protected interface FileRenamed {
        void onFileRenamed(File oldFile, File newFile);
    }

    public FileRenameDialog(MapView mapView, String path, FileRenamed callback) {
         this.context = mapView.getContext();
         this.mapView = mapView;

        _resources = context.getResources();
        _file = new File(path);
        _ext = FileSystemUtils.getExtension(_file, false, false);


        previousName = _file.getName().substring(0, _file.getName().length() - (_ext.length() + 1));

        _prefs = AtakPreferences.getInstance(mapView.getContext());
        _callback = callback;

        List<String> keys = _prefs.getStringList(QUICK_IMAGE_NAME_KEYS_KEY);
        List<String> vals = _prefs.getStringList(QUICK_IMAGE_NAME_VALUES_KEY);

        if (keys == null || keys.size() < BUTTONS_PER_ROW * NUMBER_OF_ROWS || 
                vals == null || vals.size() < BUTTONS_PER_ROW * NUMBER_OF_ROWS) {
            keys = new ArrayList<>();
            keys.add( _resources.getString(R.string.north));
            keys.add(_resources.getString(R.string.east));
            keys.add(_resources.getString(R.string.south));
            keys.add(_resources.getString(R.string.west));
            keys.add(_resources.getString(R.string.north_east));
            keys.add(_resources.getString(R.string.north_west));
            keys.add(_resources.getString(R.string.south_east));
            keys.add(_resources.getString(R.string.south_west));
            keys.add(_resources.getString(R.string.open));
            keys.add(_resources.getString(R.string.locked));
            keys.add(_resources.getString(R.string.concealed));
            keys.add(_resources.getString(R.string.blocked));
            keys.add(_resources.getString(R.string.dark));
            keys.add(_resources.getString(R.string.light));
            keys.add(_resources.getString(R.string.front));
            keys.add(_resources.getString(R.string.back));
            vals = new ArrayList<>(keys);
            
            // last row 
            keys.add(_resources.getString(R.string.date_macro));
            vals.add("$DATE");

            keys.add(_resources.getString(R.string.time_macro));
            vals.add("$TIME");

            keys.add(_resources.getString(R.string.callsign_macro));
            vals.add("$CALLSIGN");
            
            keys.add(_resources.getString(R.string.location_macro));
            vals.add("$LOCATION");
        }

        
        // put them together
        List<Pair<String, String>> keyvals = new ArrayList<>();
        for (int i = 0; i < vals.size(); ++i) {
            keyvals.add(new Pair<>(keys.get(i),vals.get(i)));
        }

        buttons.put(MODE_NAMES[0], keyvals.subList(0, 4));
        buttons.put(MODE_NAMES[1], keyvals.subList(4, 8));
        buttons.put(MODE_NAMES[2], keyvals.subList(8, 12));
        buttons.put(MODE_NAMES[3], keyvals.subList(12, 16));
        buttons.put(MODE_NAMES[4], keyvals.subList(16, 20));

        // don't allow touch outside to call dismiss() or the back button
    }

    public void show(Runnable afterClose) {
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        ad.setTitle(R.string.image_renaming_dialog_title);
        ad.setView(createView());
        ad.setCancelable(false);
        ad.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //whats the new file name?
                String newFileName = _fileNameEditText.getText().toString();


                //make sure its not empty!
                if (StringUtils.isEmpty(newFileName)) {
                    Toast.makeText(context, _resources.getString(R.string.name_cannot_be_blank), Toast.LENGTH_SHORT).show();
                    show(afterClose);
                    return;
                }

                newFileName = replacePlaceholders(newFileName);

                //TODO need to sanitize the new name??? this should be done with the InputFilter though so might not be needed

                //create new file path based off parent and new name/ext
                File newFilePath = FileSystemUtils.combine(_file.getParent(), newFileName + "." + _ext);
                //copy the the old file to the new one!
                if (FileSystemUtils.renameTo(_file, newFilePath)) {
                    //tell the callback that we changed the image name
                    if (_callback != null) {
                        _callback.onFileRenamed(_file, newFilePath);
                    }
                }
                saveKeys();
                if (afterClose != null)
                    afterClose.run();
            }
        });
        ad.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                saveKeys();
                if (afterClose != null)
                    afterClose.run();
            }
        });
        ad.show();
    }


    private View createView() {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.image_rename_dialog, null, false);

        TextView extTextView = v.findViewById(R.id.fileExtTextView);
        extTextView.setText(_ext);

        _fileNameEditText = v.findViewById(R.id.fileNameEditText);

        //create custom input filter to only allow characters that pass for filenames
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!FileSystemUtils.isAcceptableInFilename(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }

        };

        _fileNameEditText.setFilters(new InputFilter[]{filter});
        _fileNameEditText.setText(previousName);
        _fileNameEditText.setSelection(0, previousName.length());

        final Button clearButton = v.findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _fileNameEditText.setText("");
            }
        });

        final Button spaceButton = v.findViewById(R.id.spaceButton);
        spaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cursorPosition = _fileNameEditText.getSelectionStart();
                if (cursorPosition == -1) {
                    cursorPosition = _fileNameEditText.getText().length() - 1;
                }
                //add a space at the current cursor position(should be the end)
                _fileNameEditText.setText(_fileNameEditText.getText().insert(cursorPosition, "_"));
                //make sure cursor is still at end of edittext
                _fileNameEditText.setSelection(cursorPosition + 1);
            }
        });


        _tableLayout = v.findViewById(R.id.button_table_layout);
        _picker = v.findViewById(R.id.mode_picker);

        //static names for each mode/grouping of buttons

        _picker.setValues(MODE_NAMES);
        _picker.setOnModeUpdateListener(this);
        //get that last mode used
        int savedMode = _prefs.get(QUICK_IMAGE_MODE_NUM, 0);
        this.onModeUpdate(MODE_NAMES[savedMode]);
        _picker.setIndex(savedMode);

        //setup the button layout for listeners, mode updates handle updating the text
        //we will always have 1 row
        for (int r = 0; r < 1; r++) {
            TableRow tableRow = (TableRow) _tableLayout.getChildAt(r);
            for (int i = 0; i < BUTTONS_PER_ROW; i++) {
                final Button btn = (Button) tableRow.getChildAt(i);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String[] tags = (String[])btn.getTag();
                        final String prev = _fileNameEditText.getText().toString();
                        if (prev.isEmpty()) {
                            _fileNameEditText.setText(String.format("%s", tags[2]));
                        } else {
                            _fileNameEditText.setText(String.format("%s_%s", prev, tags[2]));
                        }
                        //make sure cursor is still at end of edittext
                        _fileNameEditText.setSelection(_fileNameEditText.getText().toString().length());
                    }
                });
                btn.setHint(String.valueOf(r * 4 + i)); // We'll use the hint as a way to store the
                // buttons index
                btn.setOnLongClickListener(new PresetButtonEditor(btn));
            }
        }

        return v;
    }

    @Override
    public void onModeUpdate(String mode) {
        //update the buttons for the specific mode names
        List<Pair<String, String>> values = buttons.get(mode);
        if (values != null) {
            for (int r = 0; r < _tableLayout.getChildCount(); r++) {
                TableRow row = (TableRow) _tableLayout.getChildAt(r);
                for (int b = 0; b < 4; b++) {
                    Button btn = (Button) row.getChildAt(b);
                    btn.setText(values.get(b).first);
                    btn.setTag(new String[] { mode, Integer.toString(b), values.get(b).second });
                }
            }
        }
        //make sure to save the int index of the mode in persistent storage
        _prefs.set(QUICK_IMAGE_MODE_NUM, Integer.parseInt(mode) - 1);
    }


    private void saveKeys() {
        //save the current list if changed occurred in this instance
        if (needsUpdate) {
            final List<String> keys = new ArrayList<>();
            final List<String> vals = new ArrayList<>();

            for (String s : MODE_NAMES) {
                List<Pair<String, String>> v = buttons.get(s);
                if (v != null) {
                    for (int i = 0; i < v.size(); ++i) {
                        Pair<String, String> pair = v.get(i);
                        if (pair != null) {
                            keys.add(pair.first);
                            vals.add(pair.second);
                        }
                    }
                }
            }
            _prefs.set(QUICK_IMAGE_NAME_KEYS_KEY, keys);
            _prefs.set(QUICK_IMAGE_NAME_VALUES_KEY, vals);
        }
    }



    /**
     * Handles displaying a Dialog with a single EditText to capture the change of the button value
     */
    private class PresetButtonEditor implements View.OnLongClickListener {

        private final Button btn;

        public PresetButtonEditor(Button btn) {
            this.btn = btn;
        }

        @Override
        public boolean onLongClick(View arg0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    FileRenameDialog.this.context);
            final LayoutInflater inflater = LayoutInflater
                    .from(FileRenameDialog.this.context);
            View pocView = inflater.inflate(R.layout.custom_button,
                    null);
            builder.setView(pocView);
            builder.setTitle(R.string.chat_text1);
            final EditText buttonName = pocView
                    .findViewById(R.id.buttonName);
            buttonName.setText(btn.getText());

            final EditText buttonValue = pocView.findViewById(R.id.buttonValue);

            final String[] tags = (String[])btn.getTag();

            buttonValue.setText(tags[2]);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    final String key = buttonName.getText().toString().trim();
                    final String val = buttonValue.getText().toString().trim();
                    btn.setText(key);
                    tags[2] = val;
                    btn.setTag(tags);
                    List<Pair<String, String>> mode = buttons.get(tags[0]);
                    if (mode != null)
                        mode.set(Integer.parseInt(tags[1]), new Pair<>(key, val));
                    needsUpdate = true;
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            final AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }

    }


    private String replacePlaceholders(String filename) {

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy", LocaleUtil.US);
        SimpleDateFormat stf = new SimpleDateFormat("HHmm", LocaleUtil.US);
        Date d = CoordinatedTime.currentDate();

        String deviceCallsign = mapView.getMapData().getMetaString("deviceCallsign", "uknown");
        //responsible for replacing $LOCATION, $DATE, $TILE, and $CALLSIGN
        filename = filename.replaceAll("\\$CALLSIGN", deviceCallsign);
        filename = filename.replaceAll("\\$DATE", sdf.format(d));
        filename = filename.replaceAll("\\$TIME", stf.format(d));

        String location = "";
        if (mapView.getSelfMarker().getGroup() != null)
            location = CoordinateEntryCapability.getInstance(context).format("mgrs_pane_id",
                mapView.getSelfMarker().getGeoPointMetaData());

        filename = filename.replaceAll("\\$LOCATION", location);

        return filename;
    }
}
