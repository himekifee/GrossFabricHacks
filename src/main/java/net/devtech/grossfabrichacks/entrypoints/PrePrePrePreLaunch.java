package net.devtech.grossfabrichacks.entrypoints;

/**
 * the earliest GrossFabricHacks entrypoint.
 * <b>Do NOT reference {@link net.devtech.grossfabrichacks.GrossFabricHacks GrossFabricHacks} from here.</b>
 * <b>FabricLoader will be in an uninitialized state.</b>
 * Entrypoint classes are specifically isolated, and are loaded without any other mod classes, even from the same mod, defined.
 */
public interface PrePrePrePreLaunch {
    void onPrePrePrePreLaunch();
}
