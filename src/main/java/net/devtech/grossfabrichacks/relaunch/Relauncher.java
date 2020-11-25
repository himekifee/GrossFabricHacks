package net.devtech.grossfabrichacks.relaunch;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.ListIterator;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public class Relauncher {
    /**
     * the system property that indicates whether a {@linkplain #ensureRelaunched() relaunch} has occurred or not
     */
    public static final String RELAUNCHED_PROPERTY = "gfh.relaunch.relaunched";
    /**
     * the system property that contains the names of {@linkplain RelaunchEntrypoint relaunch entrypoint} classes
     */
    public static final String ENTRYPOINT_PROPERTY = "gfh.relaunch.entrypoints";
    
    private static final String home = new File(System.getProperty("java.home")).getAbsolutePath();

    public final ObjectArrayList<String> virtualMachineArguments;
    public final ObjectArrayList<String> programArguments;

    public Relauncher() {
        this.virtualMachineArguments = getVMArguments();

        final ListIterator<String> iterator = this.virtualMachineArguments.listIterator();

        // remove debugger
        while (iterator.hasNext()) {
            final String argument = iterator.next();

            if(argument.startsWith("-agentlib:jdwp") || argument.startsWith("-javaagent")) {
                iterator.remove();
            }
        }

        this.virtualMachineArgument("javaagent:", GrossFabricHacks.Common.getAgent().getAbsolutePath());
        this.property(SystemProperties.DEVELOPMENT);

        this.programArguments = getProgramArguments();
    }

    public static ObjectArrayList<String> getProgramArguments() {
        try {
            Class.forName("org.multimc.EntryPoint");

            // replace MultiMC's entrypoint with Fabric's
            final ObjectArrayList<String> mainArgs = ObjectArrayList.wrap(new String[2], 0);

            // set entrypoint
            mainArgs.add(GrossFabricHacks.Common.getMainClass());

            // add arguments
            mainArgs.addElements(mainArgs.size(), FabricLoader.getInstance().getLaunchArguments(false));

            return mainArgs;
        } catch (final ClassNotFoundException exception) {
            return ObjectArrayList.wrap(System.getProperty("sun.java.command").split(" "));
        }
    }

    public static ObjectArrayList<String> getVMArguments() {
        return ObjectArrayList.wrap(ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]));
    }
    
    public static boolean relaunched() {
        return Boolean.getBoolean(RELAUNCHED_PROPERTY);
    }
    
    public static void ensureRelaunched() {
        if (!relaunched()) {
            new Relauncher().relaunch();
        }
    }

    public Relauncher mainClass(final String name) {
        if (name != null && !name.equals(this.programArguments.get(0))) {
            this.programArguments.add(0, name);
        }

        return this;
    }

    public Relauncher programArgument(final String argument) {
        this.programArguments.add(argument);

        return this;
    }

    public Relauncher programArgument(final int index, final String argument) {
        this.programArguments.add(index, argument);

        return this;
    }

    public Relauncher property(final String name) {
        return this.property(name, System.getProperty(name));
    }

    public Relauncher property(final String name, final String value) {
        return this.virtualMachineArgument("D", String.format("%s=%s", name, value));
    }

    public Relauncher virtualMachineArgument(final String argument) {
        this.virtualMachineArguments.add(argument);

        return this;
    }

    public Relauncher virtualMachineArgument(final String option, final String argument) {
        this.virtualMachineArguments.add('-' + option + argument);

        return this;
    }

    public void relaunch() {
        // remove built-in Java libraries from class path
        final StringBuilder newClassPath = new StringBuilder();

        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            newClassPath.append(GrossFabricHacks.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        }

        for (final String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!path.startsWith(home)) {
                if (newClassPath.length() != 0) {
                    newClassPath.append(File.pathSeparatorChar);
                }

                newClassPath.append(path);
            }
        }

        final ReferenceArrayList<String> args = new ReferenceArrayList<>();
        args.add(new File(new File(home, "bin"), "java" + OS.operatingSystem.executableExtension).getAbsolutePath());
        args.addAll(this.virtualMachineArguments);
        args.add("-cp");
        args.add(newClassPath.toString());
        args.addAll(this.programArguments);

        // release lock on log file
        LogManager.shutdown();

        try {
            final Process process = new ProcessBuilder(args).inheritIO().start();

            try {
                System.exit(process.waitFor());
            } catch (final InterruptedException exception) {
                process.destroy();
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
