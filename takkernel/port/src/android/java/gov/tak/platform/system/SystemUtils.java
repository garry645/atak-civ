
package gov.tak.platform.system;

public class SystemUtils {
    private SystemUtils() {
    }

    public static boolean isOsLinux() {
        return true;
    }

    public static boolean isOsWindows() {
        return false;
    }

    public static boolean isOsAndroid() {
        return true;
    }

    public static boolean isOsMac() {
        // We leave in the check for "darwin" for historical purposes, but I don't suspect it is
        // used in any modern OS versions we support.
        return false;
    }

    public static boolean isArch32() {
        return CommonsSystemUtils.OS_ARCH.contains("32") || CommonsSystemUtils.OS_ARCH.contains("86");
    }

    public static boolean isArch64() {
        return CommonsSystemUtils.OS_ARCH.contains("64");
    }
}

