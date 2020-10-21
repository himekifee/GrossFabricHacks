package net.devtech.grossfabrichacks.relaunch;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecondProcessRelauncher {

    private static final Logger LOGGER = LogManager.getLogger("SecondProcessRelauncher");
    public static final File HOME_DIR = new File(System.getProperty("java.home"));
    public static final String IS_RELAUNCHED = SecondProcessRelauncher.class.getName() + ".IS_RELAUNCHED";

    private static boolean isInHome(final String targetFile) {
        return contains(HOME_DIR, new File(targetFile));
    }

    private static boolean contains(final File dir, final File targetFile) {
        final File[] files = dir.listFiles();

        if (files != null) {
            for (final File file : files) {
                try {
                    if (file.toPath().toRealPath().equals(targetFile.toPath().toRealPath()) || contains(file, targetFile)) {
                        return true;
                    }
                } catch (final IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
        }
        return false;
    }

    public static ObjectArrayList<String> getGameArguments() {
        try {
            // Class.forName("org.multimc.EntryPoint");

            // replace MultiMC's entrypoint with Fabric's
            final ObjectArrayList<String> mainArgs = ObjectArrayList.wrap(new String[2], 0);

            // set entrypoint
            mainArgs.add(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? "net.fabricmc.loader.launch.knot.KnotClient"
                : "net.fabricmc.loader.launch.knot.KnotServer"
            );

            // get arguments
            // add arguments
            mainArgs.addElements(mainArgs.size(), FabricLoader.getInstance().getLaunchArguments(false));

            return mainArgs;
        } catch (final Exception exception) {
            return ObjectArrayList.wrap(System.getProperty("sun.java.command").split(" "));
        }
    }

    public static ObjectArrayList<String> getVMArguments() {
        return ObjectArrayList.wrap(ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]));
    }

    public static boolean needsRelaunch() {
        return !"true".equals(System.getProperty(IS_RELAUNCHED));
    }

    public static void relaunchIfNeeded() {
        relaunch(null);
    }

    public static void relaunch(final String mainClass, final String... arguments) {
        LOGGER.info("Relaunching with SecondProcessRelauncher...");

        final ObjectArrayList<String> VMArgs = getVMArguments();

        // remove debugger
        VMArgs.removeIf((final String argument) -> argument.startsWith("-agentlib") /*|| argument.startsWith("-javaagent")*/);
//      VMArgs.add("-javaagent:" + GrossFabricHacks.getAgent());

        // notify of relaunch
        VMArgs.add("-D" + IS_RELAUNCHED + "=true");

        final List<String> mainArgs = getGameArguments();

        if (mainClass != null) {
            mainArgs.set(0, mainClass);
        }

        final String classpathStr = System.getProperty("java.class.path");
        // remove built-in Java libraries from classpath
        final List<String> classpath = new ArrayList<>(Arrays.asList(classpathStr.split(File.pathSeparator)));

        classpath.removeIf(SecondProcessRelauncher::isInHome);

        final String newClasspathStr = String.join(File.pathSeparator, classpath);
        final ReferenceArrayList<String> args = ReferenceArrayList.wrap(new String[0], 0);

        args.add(new File(new File(HOME_DIR, "bin"), "java" + OS.operatingSystem.executableExtension).getAbsolutePath());
        args.addAll(VMArgs);
        args.add("-cp");
        args.add(newClasspathStr);
        args.addAll(mainArgs);
        args.addElements(args.size(), arguments);

        // release lock on log file
        LogManager.shutdown();

        // Free up as much memory as possible
        System.gc();

        try {
            final Process process = new ProcessBuilder(args).inheritIO().start();

            try {
                System.exit(process.waitFor());
            } catch (final InterruptedException exception) {
                process.destroy();
                throw new RuntimeException(exception);
            }
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
