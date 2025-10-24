package com.atakmap.map.formats.dted;

import com.atakmap.android.androidtest.util.FileUtils;
import com.atakmap.map.elevation.ElevationSource;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DtedElevationSourceTest extends KernelJniTest
{
    @Test
    public void query_formats_asc() throws IOException
    {
        query_formats(ElevationSource.QueryParameters.Order.ResolutionAsc);
    }

    @Test
    public void query_formats_desc() throws IOException
    {
        query_formats(ElevationSource.QueryParameters.Order.ResolutionDesc);
    }

    void query_formats(ElevationSource.QueryParameters.Order order) throws IOException
    {
        try (FileUtils.AutoDeleteFile testDir = FileUtils.AutoDeleteFile
                .createTempDir(getTestContext())) {

            File cellDir = new File(testDir.file, "w077");
            cellDir.mkdir();

            Set<String> formats = new HashSet<>(Arrays.asList(".dt0", ".dt1", ".dt2", ".dt3"));
            for(String format : formats) {
                File f = new File(cellDir, "n35" + format);
                f.createNewFile();
            }

            ElevationSource source = DtedElevationSource.create(testDir.file.getAbsolutePath());

            ElevationSource.QueryParameters params = new ElevationSource.QueryParameters();
            params.order = Collections.singletonList(order);
            try(ElevationSource.Cursor result = source.query(params)) {
                while(result.moveToNext()) {
                    for(String format : formats) {
                        if(result.getUri().endsWith(format)) {
                            formats.remove(format);
                            break;
                        }
                    }
                }
            }
            Assert.assertTrue(formats.isEmpty());
        }
    }
}
