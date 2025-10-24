
package gov.tak.platform.engine.net;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakAuthenticationCredentials;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import gov.tak.api.commons.resources.IAssetManager;
import gov.tak.api.engine.net.ICertificateStore;
import gov.tak.api.engine.net.ICredentialsStore;

abstract class CertificateManagerBase
{

    public static final String TAG = "CertificateManagerBase";
    private final List<X509Certificate> certificates = new ArrayList<>();
    private final List<X509Certificate> publicCertificates = new ArrayList<>();
    private X509TrustManager localTrustManager = null;
    private X509TrustManager publicTrustManager = null;
    private X509TrustManager systemTrustManager = null;
    private ICertificateStore certificateStore = null;
    private ICredentialsStore credentialsStore = null;

    CertificateManagerBase(ICertificateStore certificateStore, ICredentialsStore credentialsStore, IAssetManager ctx)
    {
        try
        {
            this.certificateStore = certificateStore;
            this.credentialsStore = credentialsStore;

            final TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            // Using null here initialises the TMF with the default trust store.
            // In this case, this is setting the systemTrustManager to only the
            // trust supplied by the underlying system.   The only time this
            // trust manager is used is used is if a developer actively makes a
            // call to getSystemTrustManager()
            tmf.init((KeyStore) null);

            // Get hold of the default trust manager
            for (TrustManager tm : tmf.getTrustManagers())
            {
                if (tm instanceof X509TrustManager)
                {
                    systemTrustManager = (X509TrustManager) tm;
                    Log.d(TAG, "found the system X509TrustManager: " + tm);
                    break;
                }
            }
        } catch (Exception e)
        {
            Log.d(TAG, "unable to initialize X509TrustManager", e);
        }



        String jsonString = null;
        try (InputStream inputStream = ctx.open("certs/cert_catalog.json");
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        {
            
            char[] xferBuffer = new char[4096];
            StringBuilder stringBuilder = new StringBuilder();
            for (int n; (n = reader.read(xferBuffer, 0, xferBuffer.length)) > 0; )
                stringBuilder.append(xferBuffer, 0, n);
            jsonString = stringBuilder.toString();
        } catch (Exception e)
        {
            Log.e(TAG, "error occurred reading cert catalog", e);
        }
        
        if (jsonString != null) {
            int certCount = 0;
            try {
                JSONObject rootObj = new JSONObject(jsonString);
                JSONArray certs = rootObj.getJSONArray("certificates");
                for (int i = 0; i < certs.length(); ++i) {
                    JSONObject cert = certs.getJSONObject(i);
                    String certName = cert.getString("name");
                    boolean certIsPublic = cert.getBoolean("isPublic");
                    addCertNoRefresh(certIsPublic ? publicCertificates : certificates, ctx, "certs/" + certName);
                    certCount++;
                }
            } catch (Exception e) {
                Log.e(TAG, "error occurred parsing cert catalog", e);
            }
            Log.d(TAG, "Processed " + certCount + " embedded certificate files");
        }

        refresh();
    }

    private static void addCertNoRefresh(final List<X509Certificate> certList, final IAssetManager ctx, final String name)
    {

        try
        {
            final X509Certificate cert = getCertFromFile(ctx, name);

            if (cert != null)
                certList.add(cert);
        } catch (Exception e)
        {
            Log.d(TAG, "error initializing: " + name);
        }
    }

    /**
     * Add a cert to the current local trust store.
     *
     * @param cert the X509 certificate
     */
    public synchronized void addCertificate(X509Certificate cert)
    {
        if (cert == null)
        {
            return;
        }

        Log.d(TAG, "added: " + cert);
        certificates.add(cert);
        refresh();
    }

    /**
     * Add a cert to the current local trust store.
     *
     * @param cert the X509 certificate
     */
    public synchronized void removeCertificate(X509Certificate cert)
    {
        if (cert == null)
        {
            return;
        }
        Log.d(TAG, "removed: " + cert);

        certificates.remove(cert);
        refresh();
    }

    /**
     * Gets a CA Certificate.
     *
     * @return a list of {@link X509Certificate}
     */
    public static List<X509Certificate> getCACerts(ICertificateStore certdb, ICredentialsStore authdb)
    {
        return getCACerts(certdb, authdb, null);
    }
    /**
     * Gets CA Certificates configured for a given server, or all configured CA certificates 
     *
     * @param serverHost the server hostname to get CA certificates for, or null to get all configured CA certificates
     * @return a list of {@link X509Certificate}
     */
    public static List<X509Certificate> getCACerts(ICertificateStore certdb, ICredentialsStore authdb, String serverHost)
    {
        List<X509Certificate> retval = new ArrayList<X509Certificate>();
        try
        {

            byte[] caCertP12 = null;
            ICredentialsStore.Credentials caCertCredentials;

            // get legacy certs
            String[] servers = certdb.getServers(
                    ICertificateStore.TYPE_TRUST_STORE_CA);
            if (servers != null)
            {
                for (String server : servers)
                {
                    if (serverHost != null && !serverHost.equalsIgnoreCase(server))
                        continue;
                    caCertP12 = null;
                    if (ICertificateStore.validateCertificate(certdb, ICertificateStore.TYPE_TRUST_STORE_CA, server, -1))
                        caCertP12 = certdb.getCertificate(ICertificateStore.TYPE_TRUST_STORE_CA, server, -1);
                    caCertCredentials = authdb.getCredentials(
                            AtakAuthenticationCredentials.TYPE_caPassword, server);
                    if (caCertP12 != null && caCertCredentials != null &&
                            !FileSystemUtils.isEmpty(caCertCredentials.password))
                    {
                        List<X509Certificate> caCerts = CertificateManager.loadCertificate(
                                caCertP12, caCertCredentials.password);
                        if (caCerts != null)
                        {
                            retval.addAll(caCerts);
                        }
                    }
                }
            }

            URI[] netConnectStrings = certdb.getServerURIs(
                    ICertificateStore.TYPE_TRUST_STORE_CA);
            if (netConnectStrings != null)
            {
                for (URI netConnectString : netConnectStrings)
                {
                    if (serverHost != null && !serverHost.equalsIgnoreCase(netConnectString.getHost()))
                        continue;
                    caCertP12 = null;
                    if (ICertificateStore.validateCertificate(certdb,
                            ICertificateStore.TYPE_TRUST_STORE_CA,
                            netConnectString.getHost(), netConnectString.getPort()))
                        caCertP12 = certdb.getCertificate(ICertificateStore.TYPE_TRUST_STORE_CA,
                                netConnectString.getHost(), netConnectString.getPort());

                    caCertCredentials = authdb.getCredentials(
                            AtakAuthenticationCredentials.TYPE_caPassword, netConnectString.getHost());
                    if (caCertP12 != null && caCertCredentials != null &&
                            !FileSystemUtils.isEmpty(caCertCredentials.password))
                    {
                        List<X509Certificate> caCerts = CertificateManager.loadCertificate(
                                caCertP12, caCertCredentials.password);
                        if (caCerts != null)
                        {
                            retval.addAll(caCerts);
                        }
                    }
                }
            }

            Log.d(TAG, "getCACerts" + (serverHost != null ? (" for " + serverHost) : "") + " found " + retval.size() + " certs");

            // no results for specific server, or we want all certs, get the default ca cert
            if (retval.isEmpty() || serverHost == null) {
                if (ICertificateStore.validateCertificate(certdb, ICertificateStore.TYPE_TRUST_STORE_CA, null, -1))
                    caCertP12 = certdb.getCertificate(ICertificateStore.TYPE_TRUST_STORE_CA, null, -1);
                caCertCredentials = authdb.getCredentials(
                                AtakAuthenticationCredentials.TYPE_caPassword);
                if (caCertP12 != null && caCertCredentials != null
                        && !FileSystemUtils.isEmpty(caCertCredentials.password)) {
                    List<X509Certificate> caCerts = CertificateManager.loadCertificate(caCertP12, caCertCredentials.password);
                    if (caCerts != null) {
                        retval.addAll(caCerts);
                    }
                }
                Log.d(TAG, "getCACerts" + (serverHost != null ? (" for " + serverHost) : "") + " added default CA, now " + retval.size() + " certs");
            }

            // see if we have an update server ca cert
            caCertP12 = null;
            if (ICertificateStore.validateCertificate(certdb, ICertificateStore.TYPE_UPDATE_SERVER_TRUST_STORE_CA, null, -1))
                caCertP12 = certdb.getCertificate(ICertificateStore.TYPE_UPDATE_SERVER_TRUST_STORE_CA, null, -1);
            caCertCredentials = authdb.getCredentials(
                            AtakAuthenticationCredentials.TYPE_updateServerCaPassword);
            if (caCertP12 != null && caCertCredentials != null
                    && !FileSystemUtils.isEmpty(caCertCredentials.password))
            {
                List<X509Certificate> caCerts = CertificateManager.loadCertificate(caCertP12, caCertCredentials.password);
                if (caCerts != null)
                {
                    retval.addAll(caCerts);
                }
            }

            return retval;
        } catch (Exception e)
        {
            Log.e(TAG, "exception in getCACerts!", e);
            return null;
        }
    }

    private List<X509Certificate> getAcceptedPublicIssuers()
    {
        List<X509Certificate> trustedIssuers = new LinkedList<>(publicCertificates);
        trustedIssuers.addAll(certificates);
        return trustedIssuers;
    }

    private List<X509Certificate> getAcceptedIssuers(String host)
    {
        List<X509Certificate> trustedIssuers = new LinkedList<>(certificates);
        List<X509Certificate> certificateDatabaseCerts = getCACerts(certificateStore, credentialsStore, host);
        if (certificateDatabaseCerts != null)
        {
            trustedIssuers.addAll(certificateDatabaseCerts);
        }
        return trustedIssuers;
    }

    public ICertificateStore getCertificateStore()
    {
        return certificateStore;
    }

    public ICredentialsStore getCredentialsStore()
    {
        return credentialsStore;
    }

    /**
     * Rebuild the publicTrustManager and localTrustManager based on the currently supplied certificates.
     */
    public void refresh()
    {

        try
        {
            final KeyStore localTrustStore = KeyStore.getInstance("BKS");
            localTrustStore.load(null, null);

            for (final X509Certificate cert : getAcceptedIssuers(null))
            {
                final String alias = (cert.getSubjectX500Principal())
                        .hashCode() + "";
                localTrustStore.setCertificateEntry(alias, cert);
            }

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(tmfAlgorithm);
            tmf.init(localTrustStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            localTrustManager = (X509TrustManager) trustManagers[0];

            Log.d(TAG, "obtained localized trust manager");
        } catch (Exception e)
        {
            Log.d(TAG, "error obtaining localized trust manager", e);
        }

        try
        {
            final KeyStore publicTrustStore = KeyStore.getInstance("BKS");
            publicTrustStore.load(null, null);

            for (final X509Certificate cert : getAcceptedPublicIssuers())
            {
                final String alias = (cert.getSubjectX500Principal())
                        .hashCode() + "";
                publicTrustStore.setCertificateEntry(alias, cert);
            }
            for (final X509Certificate cert : getAcceptedIssuers(null))
            {
                final String alias = (cert.getSubjectX500Principal())
                        .hashCode() + "";
                publicTrustStore.setCertificateEntry(alias, cert);
            }
            
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(tmfAlgorithm);
            tmf.init(publicTrustStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            publicTrustManager = (X509TrustManager) trustManagers[0];

            Log.d(TAG, "obtained public trust manager");
        } catch (Exception e)
        {
            Log.d(TAG, "error obtaining public trust manager", e);
        }
    }

    /**
     * Given a X509 Trust Manager, provide the certificates to include the ones
     * internally contained within ATAK.
     */
    public X509Certificate[] getCertificates(final X509TrustManager x509Tm)
    {

        java.security.cert.X509Certificate[] localCerts;

        if (x509Tm == null)
            localCerts = new X509Certificate[0];
        else
            localCerts = x509Tm.getAcceptedIssuers();

        // in case x509Tm.getAcceptedIssuers() returns null
        if (localCerts == null)
            localCerts = new X509Certificate[0];

        List<X509Certificate> acceptedIssuers = getAcceptedIssuers(null);

        java.security.cert.X509Certificate[] certs = new java.security.cert.X509Certificate[localCerts.length
                + acceptedIssuers.size()];

        System.arraycopy(localCerts, 0, certs, 0, localCerts.length);

        for (int i = 0; i < acceptedIssuers.size(); ++i)
        {
            Log.d(TAG, "added: " + acceptedIssuers.get(i));
            certs[i + localCerts.length] = acceptedIssuers.get(i);
        }

        return certs;
    }

    /**
     *
     */
    private static X509Certificate getCertFromFile(IAssetManager assetManager, String path)
            throws Exception
    {

        InputStream inputStream;
        InputStream caInput = null;
        X509Certificate cert = null;

        try
        {
            inputStream = assetManager.open(path);
        } catch (IOException e)
        {
            Log.d(TAG, "error occured loading cert", e);
            return null;
        }
        try
        {
            if (inputStream != null)
            {
                caInput = new BufferedInputStream(inputStream);
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                cert = (X509Certificate) cf.generateCertificate(caInput);
                //Log.d(TAG, "completed: " + path + " " + cert.getSerialNumber());
            }
        } finally
        {

            if (caInput != null)
            {
                try
                {
                    caInput.close();
                } catch (IOException ignored)
                {
                }
            }
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                } catch (IOException ignored)
                {
                }
            }

        }
        return cert;
    }

    /**
     * Returns a local trust manager. Note that this includes public CAs and the user is cautioned
     * to perform hostname verification.
     * Deprecated in favor of single argument variant
     *
     * @return the trust manager controlled by ATAK and populated with known trusted sources.
     */
    @Deprecated
    public X509TrustManager getLocalTrustManager()
    {
        return getLocalTrustManager(false);
    }

    /**
     * Returns a local trust manager.  Caller must indicate if it wishes to accept widely available
     * public CAs. Because the public CAs have a wide availability and scope, their inclusion and
     * use should be accompanied by hostname verification to prevent potential man in the middle
     * attacks
     *
     * @param excludePublicCAs true to exclude usage of public CAs, false to include them.
     *                         If included, user should additionally perform hostname verification 
     * @return the trust manager controlled by ATAK and populated with known trusted sources.
     */
    public X509TrustManager getLocalTrustManager(boolean excludePublicCAs)
    {
        return excludePublicCAs ? localTrustManager : publicTrustManager;
    }

    /**
     * Returns a local trust manager with only trust anchors configured specifically for the
     * given host.
     *
     * @param host host for which to provide the trust manager 
     * @return the trust manager controlled by ATAK and populated with known trust anchors
     *         for the given host.
     */
    public X509TrustManager getLocalTrustManager(String host)
    {
        try
        {
            final KeyStore localTrustStore = KeyStore.getInstance("BKS");
            localTrustStore.load(null, null);

            for (final X509Certificate cert : getAcceptedIssuers(host))
            {
                final String alias = (cert.getSubjectX500Principal())
                        .hashCode() + "";
                localTrustStore.setCertificateEntry(alias, cert);
            }

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(tmfAlgorithm);
            tmf.init(localTrustStore);
            TrustManager[] trustManagers = tmf.getTrustManagers();
            return (X509TrustManager) trustManagers[0];
        } catch (Exception e)
        {
            Log.d(TAG, "error obtaining localized trust manager for " + host, e);
        }
        return null;

    }

    /**
     * Returns a system trust manager.   This is is an unverified trust manager that is supplied
     * with the Android OS and could have not very trustworthy sources in it.  This method should
     * only be called if the client then validates things.
     *
     * @return the system trust manager which is not validated.
     */
    public X509TrustManager getSystemTrustManager()
    {
        return systemTrustManager;
    }

    /**
     * Helper method that extracts a list of x509 certificates from the encrypted container
     *
     * @param p12      encrypted certificate container
     * @param password certificate container password
     * @param provider The provider implementation for the keystore, or <code>null</code> to use default
     * @param error    If non-<code>null</code> captures any <code>Throwable</code> raised that prevented loading
     */
    public static List<X509Certificate> loadCertificate(byte[] p12, String password, Provider provider, Throwable[] error)
    {
        try
        {
            List<X509Certificate> results = new LinkedList<X509Certificate>();

            ByteArrayInputStream caCertStream = new ByteArrayInputStream(p12);
            KeyStore ks = (provider != null) ?
                    KeyStore.getInstance("PKCS12", provider) : KeyStore.getInstance("PKCS12");
            ks.load(caCertStream, password.toCharArray());
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements())
            {
                X509Certificate cert = (X509Certificate) ks.getCertificate(aliases.nextElement());
                results.add(cert);
            }

            Log.d(TAG, "loadCertificate found " + results.size() + " certs");
            return results;
        } catch (Exception e)
        {
            if (error != null)
                error[0] = e;
        }
        return null;
    }
}
