package net.devtech.grossfabrichacks.reload;

public class GrossFabricHacksReloadException extends RuntimeException {
    public GrossFabricHacksReloadException(final Throwable throwable) {
        super("Relaunching did not succeed.\nPlease report this as a bug to GrossFabricHacks:\nhttps://github.com/user11681/GrossFabricHacks/issues", throwable);
    }
}
