package com.atakmap.map.formats.msaccess;

import com.atakmap.database.DatabaseIface;
import com.atakmap.database.impl.DatabaseImpl;
import com.atakmap.interop.Interop;
import com.atakmap.interop.Pointer;

import java.io.File;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public class MsAccessDatabaseFactory {
    static {
        Interop.registerInterop(DatabaseIface.class, DatabaseImpl.class);
    }
    final static Interop<DatabaseIface> DatabaseIface_interop = Interop.findInterop(DatabaseIface.class);

    private MsAccessDatabaseFactory() {}

    public static DatabaseIface createDatabase(File dbFile) {
        Pointer ptr = createDatabaseNative(dbFile.getAbsolutePath());
        return DatabaseIface_interop.create(ptr);
    }

    private static native Pointer createDatabaseNative(String filePath);
}
