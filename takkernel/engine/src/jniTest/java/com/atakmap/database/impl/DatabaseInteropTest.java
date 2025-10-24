package com.atakmap.database.impl;

import com.atakmap.database.DatabaseIface;
import com.atakmap.interop.Interop;
import com.atakmap.interop.Pointer;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class DatabaseInteropTest extends KernelJniTest {
    @Test
    public void managedFromNative() {
        Pointer pointer = DatabaseImpl.openImpl(null, null, 0);
        Assert.assertNotNull(pointer);
        Interop<DatabaseIface> interop = Interop.findInterop(DatabaseIface.class);
        Assert.assertNotNull(interop);
        DatabaseIface db = interop.create(pointer);
        Assert.assertNotNull(db);
    }
}
