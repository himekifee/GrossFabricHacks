package net.devtech.grossfabrichacks.reload.relaunch;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.devtech.grossfabrichacks.reload.Reloader;
import org.apache.logging.log4j.LogManager;

@Deprecated
public class ProcessRelauncher {
    private static final File home = new File(System.getProperty("java.home"));

    private static boolean isInHome(final String targetFile) {
        return contains(home, new File(targetFile));
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

    public static void relaunch() {
        relaunch(null);
    }

    public static void relaunch(final String mainClass, final String... arguments) {
        final ObjectArrayList<String> VMArgs = Reloader.getVMArguments();

        // remove debugger
        VMArgs.removeIf((final String argument) -> argument.startsWith("-agentlib") /*|| argument.startsWith("-javaagent")*/);
//        VMArgs.add("-javaagent:" + GrossFabricHacks.getAgent());

        final List<String> mainArgs = Reloader.getGameArguments();

        if (mainClass != null) {
            mainArgs.add(0, mainClass);
        }

        final String classpathStr = System.getProperty("java.class.path");
        // remove built-in Java libraries from classpath
        final List<String> classpath = new ArrayList<>(Arrays.asList(classpathStr.split(File.pathSeparator)));

        classpath.removeIf(ProcessRelauncher::isInHome);

        final String newClasspathStr = String.join(File.pathSeparator, classpath);
        final ReferenceArrayList<String> args = ReferenceArrayList.wrap(new String[0], 0);

        args.add(new File(new File(home, "bin"), "java" + OS.operatingSystem.executableExtension).getAbsolutePath());
        args.addAll(VMArgs);
        args.add("-cp");
        args.add(newClasspathStr);
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
