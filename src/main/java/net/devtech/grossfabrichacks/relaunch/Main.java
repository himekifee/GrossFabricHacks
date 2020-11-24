package net.devtech.grossfabrichacks.relaunch;

import java.util.Arrays;
import java.util.function.Consumer;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import user11681.dynamicentry.DynamicEntry;

public class Main {
    public static final String NAME = "net.devtech.grossfabrichacks.relaunch.Main";

    public static void main(final String[] args) throws Throwable {
        System.setProperty(Relauncher.RELAUNCHED_PROPERTY, "true");

        final Consumer<RelaunchEntrypoint> onRelaunch = RelaunchEntrypoint::onRelaunch;

        DynamicEntry.execute("gfh:prePrePrePreLaunch", RelaunchEntrypoint.class, onRelaunch);
        DynamicEntry.execute("gfh:relaunchEntrypoint", RelaunchEntrypoint.class, onRelaunch);

        Class.forName(args[0]).getDeclaredMethod("main", String[].class).invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
    }

}
