package net.devtech.grossfabrichacks.relaunch;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;
import java.util.Set;
import net.bytebuddy.agent.Installer;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.devtech.grossfabrichacks.loader.URLAdder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.util.SystemProperties;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.ApiStatus.Experimental;
import user11681.reflect.Classes;

@SuppressWarnings("unused")
@Experimental
@VisibleForTesting
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

    public final List<String> virtualMachineArguments;
    public final List<String> programArguments;

    public Relauncher() {
        this.virtualMachineArguments = new ObjectArrayList<>(getVMArguments());
        this.virtualMachineArguments.removeIf((String argument) -> argument.startsWith("-agentlib:jdwp") || argument.startsWith("-javaagent"));

        this.virtualMachineArgument("javaagent:", Installer.class.getProtectionDomain().getCodeSource().getLocation().getFile())
            .property(SystemProperties.DEVELOPMENT)
            .property(RELAUNCHED_PROPERTY, "true");

        this.programArguments = getProgramArguments();
    }

    public static List<String> getProgramArguments() {
        try {
            Class.forName("org.multimc.EntryPoint");

            List<String> mainArgs = ObjectArrayList.wrap(FabricLoader.getInstance().getLaunchArguments(false));

            // replace MultiMC's entry point with Fabric's
            mainArgs.add(0, GrossFabricHacks.Common.getMainClass());

            return mainArgs;
        } catch (ClassNotFoundException exception) {
            return ObjectArrayList.wrap(System.getProperty("sun.java.command").split(" "));
        }
    }

    public static List<String> getVMArguments() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments();
    }
    
    public static boolean relaunched() {
        return Boolean.getBoolean(RELAUNCHED_PROPERTY);
    }
    
    public static void ensureRelaunched() {
        if (!relaunched()) {
            new Relauncher().mainClass(Main.NAME).relaunch();
        }
    }

    public Relauncher mainClass(String name) {
        if (name != null && !name.equals(this.programArguments.get(0))) {
            this.programArguments.add(0, name);
        }

        return this;
    }

    public Relauncher programArgument(String argument) {
        this.programArguments.add(argument);

        return this;
    }

    public Relauncher programArgument(int index, String argument) {
        this.programArguments.add(index, argument);

        return this;
    }

    public Relauncher property(String name) {
        return this.property(name, System.getProperty(name));
    }

    public Relauncher property(String name, String value) {
        return this.virtualMachineArgument("D", String.format("%s=%s", name, value));
    }

    public Relauncher virtualMachineArgument(String argument) {
        this.virtualMachineArguments.add(argument);

        return this;
    }

    public Relauncher virtualMachineArgument(String option, String argument) {
        this.virtualMachineArguments.add('-' + option + argument);

        return this;
    }

    public void relaunch() {
        Set<String> newClassPath = new ObjectOpenHashSet<>();

        for (URL url : Classes.urls(ClassLoader.getSystemClassLoader())) {
            newClassPath.add(url.getFile());
        }

        // remove built-in Java libraries from the class path
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!path.startsWith(home)) {
                newClassPath.add(path);
            }
        }

        List<String> args = new ReferenceArrayList<>();
        args.add(new File(new File(home, "bin"), "java" + OS.operatingSystem.executableExtension).getAbsolutePath());
        args.addAll(this.virtualMachineArguments);
        args.add("-cp");
        args.add(String.join(File.pathSeparator, newClassPath));
        args.addAll(this.programArguments);

        // release lock on log file
        LogManager.shutdown();

        try {
            URLAdder.inMemoryFs.close();

            Process process = new ProcessBuilder(args).inheritIO().start();

            try {
                System.exit(process.waitFor());
            } catch (InterruptedException exception) {
                process.destroy();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
