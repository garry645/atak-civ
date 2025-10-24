package android.net;

import org.junit.Assert;
import org.junit.Test;

public class UriTest
{
    @Test
    public void UriTest_parse()
    {
        Uri uri = Uri.parse("http://user:pass@host.com:80/p/a/t/h?q=1&r=2#FOO");
        Assert.assertEquals(uri.getScheme(), "http");
        Assert.assertEquals(uri.getHost(), "host.com");
        Assert.assertEquals(uri.getPath(), "/p/a/t/h");
        Assert.assertEquals(uri.getQueryParameter("q"), "1");
        Assert.assertEquals(uri.getQueryParameter("r"), "2");
        Assert.assertEquals(uri.getFragment(), "FOO");
    }

    @Test
    public void UriTest_parse_with_space()
    {
        Uri uri = Uri.parse("http://user:pass@host.com:80/path with a space?q=1&r=2#FOO");
        Assert.assertEquals(uri.getScheme(), "http");
        Assert.assertEquals(uri.getHost(), "host.com");
        Assert.assertEquals(uri.getPath(), "/path with a space");
        Assert.assertEquals(uri.getQueryParameter("q"), "1");
        Assert.assertEquals(uri.getQueryParameter("r"), "2");
        Assert.assertEquals(uri.getFragment(), "FOO");
    }
}
