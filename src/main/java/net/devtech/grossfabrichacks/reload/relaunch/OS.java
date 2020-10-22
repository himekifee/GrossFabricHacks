package net.devtech.grossfabrichacks.reload.relaunch;

import java.util.Locale;

@Deprecated
public enum OS {
    LINUX("linux"),

    MAC_OS("mac"),

    WINDOWS("win", ".exe"),

    OTHER("other");

    public static final OS operatingSystem;
    public static final String architecture;
    public static final String platform;

    public final String string;
    public final String executableExtension;
    
    OS(final String string) {
        this(string, "");
    }

    OS(final String string, final String executableExtension) {
        this.string = string;
        this.executableExtension = executableExtension;
    }

    static {
        final String operatingSystemName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        operatingSystem = operatingSystemName.contains("linux") ? LINUX
            : operatingSystemName.contains("mac") ? MAC_OS
                : operatingSystemName.contains("windows") ? WINDOWS
                    : OTHER;

        switch (System.getProperty("os.arch")) {
            case "x86":
            case "i386":
            case "i686":
                architecture = "x32";
                break;
            case "amd64":
                architecture = "x64";
                break;
            case "arm":
                architecture = "arm";
                break;
            case "aarch64_be":
            case "armv8b":
            case "armv8l":
            case "aarch64":
                architecture = "aarch64";
                break;
            default:
                architecture = "other";
        }

        platform = operatingSystem.string + '-' + architecture;
    }
}
