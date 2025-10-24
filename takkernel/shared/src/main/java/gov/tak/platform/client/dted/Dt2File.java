
package gov.tak.platform.client.dted;

import java.io.File;
import java.util.Objects;

class Dt2File
{

    public final String path, parent;
    public final int latitude, longitude, level;

    public Dt2File(String path)
    {
        this.path = path;

        int lastSlash = path.lastIndexOf(File.separatorChar);
        String name = path.substring(lastSlash + 1);
        char ns = Character.toUpperCase(name.charAt(0));
        int lastIdx = name.indexOf('.');
        if (lastIdx < 0)
        {
            throw new IllegalArgumentException(
                    "no extension found on this file");
        }
        this.latitude = parseInt(name.substring(1, lastIdx), 0)
                * (ns == 'S' ? -1 : 1);

        this.level = parseInt(name.substring(lastIdx + 3), 0);

        int parentSlash = lastSlash != -1 ? path.lastIndexOf(File.separatorChar,
                lastSlash - 1) : -1;
        if (parentSlash != -1)
        {
            this.parent = path.substring(0, parentSlash);
            name = path.substring(parentSlash + 1, lastSlash);
            char ew = Character.toUpperCase(name.charAt(0));
            this.longitude = parseInt(name.substring(1), 0)
                    * (ew == 'W' ? -1 : 1);
        } else
        {
            this.longitude = 0;
            this.parent = null;
        }
    }

    /**
     * Wrap a DTED file
     *
     * @param file the file to wrap
     */
    public Dt2File(File file)
    {
        this(Dt2FileWatcher.getRelativePath(file));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Dt2File dt2File = (Dt2File) o;
        return latitude == dt2File.latitude &&
                longitude == dt2File.longitude &&
                level == dt2File.level;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(latitude, longitude, level);
    }

    private static int parseInt(String value, int defaultVal)
    {
        if (value == null)
        {
            return defaultVal;
        }

        try
        {
            return Integer.parseInt(value.trim());
        } catch (Exception e)
        {
            return defaultVal;
        }
    }
}
