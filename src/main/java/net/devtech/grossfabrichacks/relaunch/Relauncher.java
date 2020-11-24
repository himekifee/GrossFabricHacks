package net.devtech.grossfabrichacks.relaunch;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.ListIterator;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;

public class Relauncher {
    /**
     * the system property that indicates whether a {@linkplain #ensureRelaunched() relaunch} has occurred or not
     */
    public static final String RELAUNCHED_PROPERTY = "gfh.relaunched";
    
    private static final String home = new File(System.getProperty("java.home")).getAbsolutePath();

    public static ObjectArrayList<String> getGameArguments() {
        try {
            Class.forName("org.multimc.EntryPoint");

            // replace MultiMC's entrypoint with Fabric's
            final ObjectArrayList<String> mainArgs = ObjectArrayList.wrap(new String[2], 0);

            // set entrypoint
            mainArgs.add(GrossFabricHacks.Common.getMainClass());

            // get arguments
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
            relaunch();
        }
    }

    public static void relaunch() {
        relaunch(Main.NAME);
    }

    public static void relaunch(final String mainClass, final String... arguments) {
        final ObjectArrayList<String> VMArgs = getVMArguments();
        ListIterator<String> iterator = VMArgs.listIterator();
        String argument;

        // remove debugger
        while (iterator.hasNext()) {
            argument = iterator.next();

            if(argument.startsWith("-agentlib:jdwp") || argument.startsWith("-javaagent")) {
                iterator.remove();
            }
        }
        
        VMArgs.add("-javaagent:" + GrossFabricHacks.Common.getAgent());

        final List<String> mainArgs = getGameArguments();

        if (mainClass != null && !mainClass.equals(mainArgs.get(0))) {
            mainArgs.add(0, mainClass);
        }

        // remove built-in Java libraries from classpath
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
        args.addAll(VMArgs);
        args.add("-cp");
        args.add(newClassPath.toString());
        args.addAll(mainArgs);
        args.addElements(args.size(), arguments);

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
