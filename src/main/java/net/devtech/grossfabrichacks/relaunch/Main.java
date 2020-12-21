package net.devtech.grossfabrichacks.relaunch;

import java.util.Arrays;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public class Main {
    public static final String NAME = "net.devtech.grossfabrichacks.relaunch.Main";

    public static void main(final String[] args) throws Throwable {
        final String entrypoints = System.clearProperty(Relauncher.ENTRYPOINT_PROPERTY);

        if (entrypoints != null) {
            Class.forName("net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint");

            for (final String entrypoint : entrypoints.split(GrossFabricHacks.Common.CLASS_DELIMITER)) {
                ((RelaunchEntrypoint) TransformingClassLoader.instance.loadClass(entrypoint).getDeclaredConstructor().newInstance()).onRelaunch();
            }
        }

        TransformingClassLoader.instance.loadClass(args[0]).getDeclaredMethod("main", String[].class).invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
    }
}
