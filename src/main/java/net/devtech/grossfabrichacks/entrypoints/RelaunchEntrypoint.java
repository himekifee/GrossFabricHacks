package net.devtech.grossfabrichacks.entrypoints;

/**
 * The earliest GrossFabricHacks entrypoint.
 * <b>Do NOT reference {@link net.devtech.grossfabrichacks.GrossFabricHacks GrossFabricHacks} from here.</b>
 * <b>FabricLoader will be in an uninitialized state.</b>
 * Entrypoint classes are specifically isolated, and are loaded without any other mod classes,
 * even from the same mod, defined.<br>
 * <br>
 * To use, add <tt><br>
 * "entrypoints": [<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"gfh:relaunch": "your.relaunch.entrypoint.Class"<br>
 * ]<br></tt> to your <tt>fabric.mod.json</tt>.<br>
 * <br>
 * You can also use {@code gfh:prePrePrePreLaunch} for a more semantic but less
 * clear entrypoint identifier (its actually the same length as relaunchEntrypoint).
 * {@code gfh:prePrePrePreLaunch} entrypoints are guarenteed to be called before
 * {@code gfh:relaunchEntrypoint} entrypoints,
 */
public interface RelaunchEntrypoint {
    void onRelaunch();

    default boolean shouldRelaunch() {
        return true;
    }
}
