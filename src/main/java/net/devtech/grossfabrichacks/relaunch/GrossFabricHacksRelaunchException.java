package net.devtech.grossfabrichacks.relaunch;

public class GrossFabricHacksRelaunchException extends RuntimeException {

    public GrossFabricHacksRelaunchException(Throwable t) {
        super("Relaunching did not succeed.\nPlease report this as a bug to GrossFabricHacks:\nhttps://github.com/Devan-Kerman/GrossFabricHacks/issues/new", t);
    }

}
