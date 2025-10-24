
package com.atakmap.map.gdal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.ogr;
import org.gdal.osr.SpatialReference;

import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.io.ZipVirtualFile;
import com.atakmap.map.EngineLibrary;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.util.ConfigOptions;

import static com.atakmap.map.gdal.VSIJFileFilesystemHandler.installFilesystemHandler;

import gov.tak.platform.system.SystemUtils;

public class GdalLibrary
{
    public static SpatialReference EPSG_4326 = null;
    private static final String TAG = "GdalLibrary";
    private static boolean initialized = false;
    private static boolean initSuccess = false;

    private static Map<String, DatabaseInfo> readonlyDatabases = new HashMap<String, DatabaseInfo>();
    private static Map<String, DatabaseInfo> readwriteDatabases = new HashMap<String, DatabaseInfo>();

    static {
        init();
    }

    private GdalLibrary()
    {
    } // non-instantiable

    /*************************************************************************/
    public static synchronized boolean init()
    {
        if (!initialized)
        {
            try
            {
                EngineLibrary.initialize();

                initImpl();
            } catch (IOException ignored)
            {
            } finally
            {
                initialized = true;
            }
        }
        return initSuccess;
    }

    public static synchronized boolean isInitialized()
    {
        return initialized;
    }

    private static void initImpl() throws IOException
    {

        // IMPORTANT: as of GDAL upgrade 2.2.3, some SQLite containers are now
        //            supported natively by GDAL. Specify that these drivers
        //            should be skipped as they are breaking normal handling of
        //            GPKG and MBtiles.

        // XXX - use of Driver::Deregister() is observed NOT to work

        gdal.SetConfigOption("GDAL_SKIP", "MBTiles,GPKG");

        // construct the classpath path to the GDAL_DATA content
        String gdalDataPath = null;
        try
        {
            // obtain the resource URL
            URL u = GdalLibrary.class.getClassLoader().getResource("gdal/data/gdaldata.files");
            gdalDataPath = u.toString();
            // exchange protocol for `/vsizip/` prefix, remove the file and the
            // archive/entry separator character. The path to the archive is
            // enclosed in braces as GDAL is only automatically able to
            // determine archive path by ".zip" extension.
            if (SystemUtils.isOsWindows()) {
                gdalDataPath = gdalDataPath.replace("jar:file:/", "/vsizip/{");
                // Spaces in paths will be URL encoded. This issue currently only manifests in Windows.
                gdalDataPath = gdalDataPath.replace("%20", " ");
            } else {
                gdalDataPath = gdalDataPath.replace("jar:file:", "/vsizip/{");
            }
            gdalDataPath = gdalDataPath.replace("gdaldata.files", "");
            gdalDataPath = gdalDataPath.replace("!", "}");
        } catch (Exception e)
        {
            Log.d("GdalLibrary", "XXX: Failed construct GDAL_DATA path from classpath");
        }

        // NOTE: AllRegister automatically loads the shared libraries
        gdal.AllRegister();
        if (gdalDataPath != null)
            gdal.SetConfigOption("GDAL_DATA", gdalDataPath);
        // debugging
        gdal.SetConfigOption("CPL_DEBUG", "OFF");
        gdal.SetConfigOption("CPL_LOG_ERRORS", "ON");

        gdal.SetConfigOption("GDAL_DISABLE_READDIR_ON_OPEN", "TRUE");

        // NOTE: This has been added to address the usability requirements with GeoPDF
        // and according to https://gdal.org/drivers/raster/pdf.html
        // This setting moves the default from 150dpi to 300dpi, putting an increased
        // demand on memory but provides a usable map for the firefighters.
        // https://jira.takmaps.com/browse/ATAK-14344
        gdal.SetConfigOption("GDAL_PDF_DPI", "300");

        // try to init the spatial reference
        try
        {
            EPSG_4326 = new SpatialReference();
            EPSG_4326.ImportFromEPSG(4326);
        } catch (Exception e)
        {
            Log.d("GdalLibrary", "XXX: Failed to init GDAL_DATA");
        }

        registerProjectionSpi();

        VSIFileFileSystemHandler vsiJfileHandler = new VSIFileFileSystemHandler();
        installFilesystemHandler(vsiJfileHandler);
        // We want to ensure our custom handler gets deleted while takkernel is still loaded.
        Runtime.getRuntime().addShutdownHook(new Thread(gdal::GDALDestroyDriverManager));

        if (IOProviderFactory.isDefault())
            ConfigOptions.setOption("gdal-vsi-prefix", null);
        else
            ConfigOptions.setOption("gdal-vsi-prefix", VSIFileFileSystemHandler.PREFIX);

        initSuccess = true;
    }

    public static int getSpatialReferenceID(SpatialReference srs)
    {
        if (srs == null)
            return -1;
        String value;

        value = srs.GetAttrValue("AUTHORITY", 0);
        if (value == null || !value.equals("EPSG"))
        {
            // note there are producers out there that are supplying prj files without
            // the AUTHORITY set such as "WGS_1984_UTM_Zone_56S", which really is
            // AUTHORITY["EPSG","32756"] -- it seems like ESRI is the product being used
            // to generate the prj files.

            // this could exist anywhere in the PROJCS list, we may need to revisit
            value = srs.GetAttrValue("PROJCS", 0);
            if (value != null)
            {
                return CoordSysName2EPSG.lookup(value);
            }

            return -1;
        }
        value = srs.GetAttrValue("AUTHORITY", 1);
        if (value == null || !value.matches("\\d+"))
            return -1;
        return Integer.parseInt(value);
    }

    public static synchronized SQLiteDatabase openDatabase(String path,
                                                           SQLiteDatabase.CursorFactory factory, int flags, DatabaseErrorHandler errorHandler)
    {
        Map<String, DatabaseInfo> databaseMap = (((flags & SQLiteDatabase.OPEN_READONLY) == SQLiteDatabase.OPEN_READONLY) ? readonlyDatabases
                : readwriteDatabases);
        DatabaseInfo info = databaseMap.get(path);
        if (info != null)
        {
            info.referenceCount++;
            return info.database;
        } else
        {
            SQLiteDatabase retval = SQLiteDatabase.openDatabase(path, factory, flags, errorHandler);
            databaseOpened(retval);
            return retval;
        }
    }

    public static synchronized SQLiteDatabase openDatabase(String path,
                                                           SQLiteDatabase.CursorFactory factory, int flags)
    {
        Map<String, DatabaseInfo> databaseMap = (((flags & SQLiteDatabase.OPEN_READONLY) == SQLiteDatabase.OPEN_READONLY) ? readonlyDatabases
                : readwriteDatabases);
        DatabaseInfo info = databaseMap.get(path);
        if (info != null)
        {
            info.referenceCount++;
            return info.database;
        } else
        {
            SQLiteDatabase retval = SQLiteDatabase.openDatabase(path, factory, flags);
            databaseOpened(retval);
            return retval;
        }
    }

    public static SQLiteDatabase openOrCreateDatabase(String path,
                                                      SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler)
    {
        return openDatabase(path, factory, SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path,
                                                      SQLiteDatabase.CursorFactory factory)
    {
        return openDatabase(path, factory, SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    public static SQLiteDatabase openOrCreateDatabase(File file,
                                                      SQLiteDatabase.CursorFactory factory)
    {
        return openDatabase(file.getPath(), factory, SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    public static synchronized void closeDatabase(SQLiteDatabase database)
    {
        Map<String, DatabaseInfo> databaseMap = (database.isReadOnly() ? readonlyDatabases
                : readwriteDatabases);

        DatabaseInfo info = databaseMap.get(database.getPath());
        if (info == null || info.referenceCount == 0)
            throw new IllegalStateException();
        info.referenceCount--;
        if (info.referenceCount < 1)
        {
            databaseMap.remove(database.getPath());
            database.close();
        }
    }

    private static void databaseOpened(SQLiteDatabase database)
    {
        Map<String, DatabaseInfo> databaseMap = (database.isReadOnly() ? readonlyDatabases
                : readwriteDatabases);
        databaseMap.put(database.getPath(), new DatabaseInfo(database));
    }

    /**
     * Returns the WKT for the associated SRID.
     *
     * @param srid The SRID
     * @return The WKT for the SRID or <code>null</code> if the WKT is not
     * defined for the SRID
     */
    public static String getWkt(int srid)
    {
        SpatialReference spatialRef = new SpatialReference();
        int err = ogr.OGRERR_FAILURE;
        try
        {
            err = spatialRef.ImportFromEPSG(srid);
        } catch (RuntimeException ignored) {}

        return (err == ogr.OGRERR_NONE) ? spatialRef.ExportToWkt() : null;
    }

    public static Dataset openDatasetFromFile(File file)
    {
        return openDatasetFromFile(file, gdalconst.GA_ReadOnly, null);
    }

    public static Dataset openDatasetFromFile(File file, int accessOpts)
    {
        return openDatasetFromFile(file, accessOpts, null);
    }

    public static Dataset openDatasetFromFile(File file, int accessOpts, Vector driverOpts)
    {
        // implement chaining per https://gdal.org/user/virtual_file_systems.html#chaining
        String path = file.getAbsolutePath();
        // custom TAK IO VSI
        if (!IOProviderFactory.isDefault())
            path = VSIFileFileSystemHandler.PREFIX + path;
        // zip VSI
        if (file instanceof ZipVirtualFile)
            path = "/vsizip/" + path;

        do {
            // wrapping as a subfile (with offset 0) effectively suppresses (most) sidecar file
            // checks, improving dataset open times by 2.5-3X
            Dataset ds = openImpl("/vsisubfile/0," + path, accessOpts, PriorityDrivers.noSidecarDrivers, driverOpts);
            if (ds != null)
                return ds;
        } while(false);

        // try to open using subset of prioritized drivers
        Dataset ds = openImpl(path, accessOpts, PriorityDrivers.drivers, driverOpts);
        if (ds != null)
            return ds;

        // try open with all drivers
        return openImpl(path, accessOpts, null, driverOpts);
    }

    public static Dataset openDatasetFromPath(String path)
    {
        return openDatasetFromPath(path, null);
    }

    public static Dataset openDatasetFromPath(String path, Vector driverOpts)
    {
        File file;
        String zipPrefix = SystemUtils.isOsWindows() ? "zip:/" : "zip://";
        if (path.startsWith(zipPrefix))
        {
            path = path.replace(zipPrefix, "");
            path = path.replace("%20", " ").
                    replace("%23", "#").
                    replace("%5B", "[").
                    replace("%5D", "]");
            file = new ZipVirtualFile(path);
        } else if (path.startsWith("/vsizip"))
        {
            path = path.replace("/vsizip", "");
            file = new ZipVirtualFile(path);
        } else if (FileSystemUtils.checkExtension(path, "zip"))
        {
            file = new ZipVirtualFile(path);
        } else
        {
            if (path.startsWith("file:///"))
                path = path.substring(7);
            else if (path.startsWith("file://"))
                path = path.substring(6);
            path = path.replace("%20", " ").
                    replace("%23", "#").
                    replace("%5B", "[").
                    replace("%5D", "]");
            file = new File(path);
        }

        // if the file doesn't exist, it may be a pre-baked path, try opening
        // directly before going through to the File based method
        if (!IOProviderFactory.exists(file))
        {
            Dataset dataset = openImpl(path, gdalconst.GA_ReadOnly, null, driverOpts);
            if (dataset != null)
                return dataset;
        }

        return openDatasetFromFile(file, gdalconst.GA_ReadOnly, driverOpts);
    }

    public static Dataset openDatasetFromMemory(byte[] data)
    {
        return openDatasetFromMemory(data, null);
    }

    public static Dataset openDatasetFromMemory(byte[] data, Vector driverOpts)
    {
        final String path = "/vsimem/" + UUID.randomUUID().toString();
        gdal.FileFromMemBuffer(path, data);
        try {
            return openImpl(path, gdalconst.GA_ReadOnly, null, driverOpts);
        } finally {
            gdal.Unlink(path);
        }
    }

    static Dataset openImpl(String path, long flags, Vector drivers, Vector driverOpts)
    {
        try {
            if (driverOpts != null && !driverOpts.isEmpty()) {
                // apply driver opts as thread local config options
                if (driverOpts != null && !driverOpts.isEmpty()) {
                    for(Object o : driverOpts) {
                        String opt = (String)o;
                        String[] kvp = opt.split("=");
                        if(kvp.length != 2)
                            continue;
                        setThreadLocalConfigOption(kvp[0], kvp[1]);
                    }
                }
            }
            return org.gdal.gdal.gdal.OpenEx(path, flags, drivers, driverOpts);
        } finally {
            // clear the thread local options
            if (driverOpts != null && !driverOpts.isEmpty()) {
                for(Object o : driverOpts) {
                    String opt = (String)o;
                    String[] kvp = opt.split("=");
                    if(kvp.length != 2)
                        continue;
                    setThreadLocalConfigOption(kvp[0], null);
                }
            }
        }
    }

    public static DataSource openDataSourceFromFile(String path)
    {
        if (IOProviderFactory.isDefault())
            return org.gdal.ogr.ogr.Open(path);
        else
            return org.gdal.ogr.ogr.Open(VSIFileFileSystemHandler.PREFIX + path);
    }

    public static native byte[] GetMemFileBuffer(String gdalMemoryFileOut);

    /**************************************************************************/

    private static class DatabaseInfo
    {
        public final SQLiteDatabase database;
        public int referenceCount;

        public DatabaseInfo(SQLiteDatabase database)
        {
            this.database = database;
            this.referenceCount = 1;
        }
    }

    final static class PriorityDrivers
    {
        static Vector noSidecarDrivers = new Vector();
        static Vector drivers = new Vector();
        static {
            noSidecarDrivers.add("NITF");

            drivers.add("MrSID");
            drivers.add("GTiff");
        }
    }

    public static native void registerProjectionSpi();
    static native void setThreadLocalConfigOption(String key, String value);
}
