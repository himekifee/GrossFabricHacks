package net.devtech.grossfabrichacks.entrypoints;

/**
 * The second-earliest GrossFabricHacks entrypoint. If a {@code gfh:prePrePreLaunch} class does not implement this interface,
 * then it is not instantiated and {@link PrePrePreLaunch#onPrePrePreLaunch} is not called, but the class is still initialized.<br>
 * <br>
 * To use, add <tt><br>
 * "entrypoints": [<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"gfh:prePrePreLaunch": "your.prepreprelaunch.entrypoint.Class"<br>
 * ]<br></tt> to your <tt>fabric.mod.json</tt>.
 */
public interface PrePrePreLaunch {
    void onPrePrePreLaunch();
}
