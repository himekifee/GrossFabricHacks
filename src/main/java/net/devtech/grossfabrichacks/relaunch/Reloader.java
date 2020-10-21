package net.devtech.grossfabrichacks.relaunch;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.gudenau.lib.unsafe.Unsafe;
import org.apache.logging.log4j.LogManager;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.reflect.Fields;
import user11681.reflect.Invoker;

@SuppressWarnings("ConfusingArgumentToVarargsMethod")
public class Reloader {
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

    public static void ensureReloaded() {
        if (!isReloaded()) {
            reload();
        }
    }

    public static void reload() {
        final SecureClassLoader currentLoader = (SecureClassLoader) ClassLoader.getSystemClassLoader();
        final Class<? extends SecureClassLoader> klass = currentLoader.getClass();
        final SecureClassLoader newLoader = Unsafe.allocateInstance(klass);

        try {
            for (final Field field : Fields.getAllFields(klass)) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    if (field.getDeclaringClass() == ClassLoader.class) {
                        if (field.getName().equals("classes")) {
                            Accessor.putObject(newLoader, field, Invoker.findConstructor(field.getType(), MethodType.methodType(void.class)).invoke());

                            continue;
                        } else if (field.getName().equals("parent")) {
                            continue;
                        }
                    }

                    Accessor.copyObject(newLoader, currentLoader, field);
                }
            }

            final FabricLoader fabric = FabricLoader.getInstance();
            final MethodHandle main = Invoker.findStatic(Classes.load(newLoader, fabric.getEnvironmentType() == EnvType.CLIENT
                ? "net.fabricmc.loader.launch.knot.KnotClient"
                : "net.fabricmc.loader.launch.knot.KnotServer"
            ), "main", MethodType.methodType(void.class, String[].class));

            LogManager.shutdown();

            System.setProperty("gfh.reloaded", "true");

            main.invokeExact(fabric.getLaunchArguments(false));

            System.exit(0);
        } catch (final Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }
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

    public static ObjectArrayList<String> getVMArguments() {
        return ObjectArrayList.wrap(ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]));
    }

    public static void relaunch() {
        relaunch(null);
    }

    public static void relaunch(final String mainClass, final String... arguments) {
        final ObjectArrayList<String> VMArgs = getVMArguments();

        // remove debugger
        VMArgs.removeIf((final String argument) -> argument.startsWith("-agentlib") /*|| argument.startsWith("-javaagent")*/);
//        VMArgs.add("-javaagent:" + GrossFabricHacks.getAgent());

        final List<String> mainArgs = getGameArguments();

        if (mainClass != null) {
            mainArgs.add(0, mainClass);
        }

        final String classpathStr = System.getProperty("java.class.path");
        // remove built-in Java libraries from classpath
        final List<String> classpath = new ArrayList<>(Arrays.asList(classpathStr.split(File.pathSeparator)));

        classpath.removeIf(Reloader::isInHome);

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

    public static boolean isReloaded() {
        return Boolean.parseBoolean(System.getProperty("gfh.reloaded"));
    }
}
