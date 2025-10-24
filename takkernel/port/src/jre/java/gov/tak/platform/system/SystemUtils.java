
package gov.tak.platform.system;

public class SystemUtils {
    private SystemUtils() {
    }

    public static boolean isOsLinux() {
        return CommonsSystemUtils.IS_OS_LINUX;
    }

    public static boolean isOsWindows() {
        return CommonsSystemUtils.IS_OS_WINDOWS;
    }

    public static boolean isOsAndroid() {
        return false;
    }

    public static boolean isOsMac() {
        // We leave in the check for "darwin" for historical purposes, but I don't suspect it is
        // used in any modern OS versions we support.
        return CommonsSystemUtils.IS_OS_MAC || CommonsSystemUtils.OS_NAME.toLowerCase().contains("darwin");
    }

    public static boolean isArch32() {
        return CommonsSystemUtils.OS_ARCH.contains("32") || CommonsSystemUtils.OS_ARCH.contains("86");
    }

    public static boolean isArch64() {
        return CommonsSystemUtils.OS_ARCH.contains("64");
    }
}
