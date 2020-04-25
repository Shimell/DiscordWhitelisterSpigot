package uk.co.angrybee.joe;

public class VersionInfo {
    public static String getVersion() {
        return version;
    }

    public static String getLongVersion() {
        return "v." + getVersion();
    }

    private static String version = "1.3.1";
}
