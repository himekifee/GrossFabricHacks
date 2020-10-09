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
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;

public class Relauncher {
    public static final File home = new File(System.getProperty("java.home"));

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

    public static ObjectArrayList<String> getGameArguments() {
        try {
            Class.forName("org.multimc.EntryPoint");

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
        } catch (final ClassNotFoundException exception) {
            return ObjectArrayList.wrap(System.getProperty("sun.java.command").split(" "));
        }
    }

    public static void relaunch() {
        relaunch(null);
    }

    public static void relaunch(final String mainClass, final String... arguments) {
        final ObjectArrayList<String> VMArgs = new ObjectArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
        final String source = GrossFabricHacks.class.getProtectionDomain().getCodeSource().getLocation().getFile();
/*
        final File agent;

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            agent = new File(System.getProperty("user.home"), "gross_agent.jar");

            try {
                final JarOutputStream agentJar = new JarOutputStream(new FileOutputStream(agent), new Manifest(new FileInputStream(new File(source, "/META-INF/MANIFEST.MF"))));
                final String agentPath = "net/devtech/grossfabrichacks/instrumentation/InstrumentationAgent.class";

                agentJar.putNextEntry(new ZipEntry(agentPath));

                IOUtils.write(IOUtils.toByteArray(GrossFabricHacks.class.getResourceAsStream("/" + agentPath)), agentJar);

                agentJar.close();
            } catch (final IOException exception) {
                throw new UncheckedIOException(exception);
            }
        } else {
            agent = new File(source);
        }
*/

        // remove debugger
        VMArgs.removeIf((final String argument) -> argument.startsWith("-agentlib") || argument.startsWith("-javaagent"));
//        VMArgs.add("-javaagent:" + agent);

        final List<String> mainArgs = getGameArguments();

        if (mainClass != null) {
            mainArgs.add(0, mainClass);
        }

        final String classpathStr = System.getProperty("java.class.path");
        // remove built-in Java libraries from classpath
        final List<String> classpath = new ArrayList<>(Arrays.asList(classpathStr.split(File.pathSeparator)));

        classpath.removeIf(Relauncher::isInHome);

        final String newClasspathStr = String.join(File.pathSeparator, classpath);
        final ReferenceArrayList<String> args = ReferenceArrayList.wrap(new String[0], 0);

        args.add(new File(new File(home, "bin"), "java" + Platform.getExecutableExtension()).getAbsolutePath());
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
