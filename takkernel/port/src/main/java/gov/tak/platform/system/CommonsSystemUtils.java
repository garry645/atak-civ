
package gov.tak.platform.system;

/**
 * Taken from Apache Commons Lang3 v3.11
 * @link https://github.com/apache/commons-lang/blob/rel/commons-lang-3.11/src/main/java/org/apache/commons/lang3/SystemUtils.java
 */
class CommonsSystemUtils {
    private static final String OS_NAME_WINDOWS_PREFIX = "Windows";
    public static final String OS_ARCH = getSystemProperty("os.arch");
    public static final String OS_NAME = getSystemProperty("os.name");

    public static final boolean IS_OS_LINUX = getOsMatchesName("Linux") || getOsMatchesName("LINUX");
    public static final boolean IS_OS_MAC = getOsMatchesName("Mac");
    public static final boolean IS_OS_WINDOWS = getOsMatchesName(OS_NAME_WINDOWS_PREFIX);

    private CommonsSystemUtils() {
    }

    private static boolean getOsMatchesName(final String osNamePrefix) {
        return isOSNameMatch(OS_NAME, osNamePrefix);
    }

    private static String getSystemProperty(final String property) {
        try {
            return System.getProperty(property);
        } catch (final SecurityException ex) {
            // we are not allowed to look at this property
            // System.err.println("Caught a SecurityException reading the system property '" +
            // property
            // + "'; the SystemUtils property value will default to null.");
            return null;
        }
    }

    private static boolean isOSNameMatch(final String osName, final String osNamePrefix) {
        if (osName == null) {
            return false;
        }
        return osName.startsWith(osNamePrefix);
    }
}
