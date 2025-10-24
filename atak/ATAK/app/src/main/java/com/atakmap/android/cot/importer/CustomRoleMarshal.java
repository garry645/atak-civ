
package com.atakmap.android.cot.importer;

import android.net.Uri;

import com.atakmap.android.importexport.AbstractMarshal;
import com.atakmap.android.importexport.Marshal;
import com.atakmap.coremap.locale.LocaleUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Importer for user icon markers
 */
public class CustomRoleMarshal extends AbstractMarshal {
    public final static Marshal INSTANCE = new CustomRoleMarshal();

    public CustomRoleMarshal() {
        super(CustomRoleImporter.CONTENT_TYPE);
    }

    @Override
    public String marshal(InputStream inputStream, int limit)
            throws IOException {
        return null;
    }

    @Override
    public String marshal(Uri uri) throws IOException {
        File f = new File(uri.getPath());
        if (!f.exists())
            return null;
        if (!f.getName().toLowerCase(LocaleUtil.US).endsWith(".json"))
            return null;
        try (FileInputStream fis = new FileInputStream(f)) {
            return CustomRoleImporter.isRoles(fis) ? CustomRoleImporter.JSON_CSV
                    : null;
        }
    }

    @Override
    public int getPriorityLevel() {
        return 1;
    }

}
