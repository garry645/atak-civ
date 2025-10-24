package gov.tak.api.engine.net;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public interface IResponse extends AutoCloseable
{
    InputStream getBody();
    int getCode();
    long getLength();
    void close();

    static byte[] getBytes(IResponse response) throws IOException
    {
        return FileSystemUtils.read(response.getBody());
    }

    static String getString(IResponse response) throws IOException
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), Charset.forName("UTF-8")))) {
            StringBuilder s = new StringBuilder(4096);
            char[] buf = new char[4096];
            while(true) {
                final int n = reader.read(buf);
                if(n < 0)
                    break;
                else if(n > 0)
                    s.append(buf, 0, n);
            }
            return s.toString();
        }
    }
}
