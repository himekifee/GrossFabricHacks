package net.devtech.grossfabrichacks.relaunch;

import java.util.Locale;

public enum Platform {
    LINUX("linux"),

    MAC_OS("mac"),

    WINDOWS("win"),

    OTHER("other");

    public static final Platform platform;

    public final String string;

    Platform(final String string) {
        this.string = string;
    }

    private static String getArchitectureType() {
        switch (System.getProperty("os.arch")) {
            case "x86":
            case "i386":
            case "i686":
                return "x32";
            case "amd64":
                return "x64";
            case "arm":
                return "arm";
            case "aarch64_be":
            case "armv8b":
            case "armv8l":
            case "aarch64":
                return "aarch64";
            default:
                return "other";
        }
    }

    public static String getPlatform() {
        return platform.string + '-' + getArchitectureType();
    }

    static String getExecutableExtension() {
        if ("win".equals(platform.string)) {
            return ".exe";
        } else {
            return "";
        }
    }

    static {
        final String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        platform = operatingSystem.contains("mac") ? Platform.MAC_OS
            : operatingSystem.contains("linux") ? Platform.LINUX
                : operatingSystem.contains("windows") ? Platform.WINDOWS
                    : Platform.OTHER;
    }
}
