package net.devtech.grossfabrichacks.entrypoints;

/**
 * The earliest* possible entrypoint that can be thrown<br>
 * <br>
 * * I think<br>
 * <br>
 * To use, add <tt><br>
 * "entrypoints": [<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;"gfh:prePreLaunch": "your.preprelaunch.entrypoint.Class"<br>
 * ]<br></tt> to your <tt>fabric.mod.json</tt>.
 */
public interface PrePreLaunch {
	void onPrePreLaunch();
}
