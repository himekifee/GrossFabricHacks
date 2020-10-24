package net.devtech.grossfabrichacks.reload;

import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.fabricmc.loader.launch.knot.KnotClient;
import net.fabricmc.loader.launch.knot.KnotServer;
import user11681.dynamicentry.DynamicEntry;

public class GFHMain {
    public static void main(final String[] args) {
        System.setProperty(GrossFabricHacks.Common.RELOADED_PROPERTY, "true");

        if (System.getProperty("fabric.side").equals("client")) {
            KnotClient.main(args);
        } else {
            KnotServer.main(args);
        }

        DynamicEntry.execute("gfh:prePrePrePreLaunch", RelaunchEntrypoint.class, RelaunchEntrypoint::onRelaunch);
        DynamicEntry.execute("gfh:relaunchEntrypoint", RelaunchEntrypoint.class, RelaunchEntrypoint::onRelaunch);
    }
}
