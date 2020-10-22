package net.devtech.grossfabrichacks.reload;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.SecureClassLoader;
import java.util.Locale;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.discovery.ModResolver;
import net.gudenau.lib.unsafe.Unsafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;
import user11681.reflect.Fields;
import user11681.reflect.Invoker;
import user11681.reflect.Reflect;

@SuppressWarnings("ConfusingArgumentToVarargsMethod")
public class Reloader {
    private static final Logger logger = LogManager.getLogger("SameProcessRelauncher");

    public static boolean isReloaded() {
        return Boolean.getBoolean(GrossFabricHacks.Common.reloadedProperty);
    }

    public static void ensureReloaded() {
        if (!isReloaded()) {
            launchMain();
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

    public static SecureClassLoader getNewLoader() {
        logger.info("Commence same-process reload.");

        try {
            // close the in-memory file system to avoid later collision
            ((Closeable) Accessor.getObject(ModResolver.class, "inMemoryFs")).close();

            // look up the classloader hierarchy until we find Launcher$ExtClassLoader
            final String launcherClassName = Reflect.java9 ? "jdk.internal.loader.ClassLoaders" : "sun.misc.Launcher";
            final String appClassLoaderClassName = launcherClassName + "$AppClassLoader";
            final Class<?> extClassLoaderClass = Class.forName(launcherClassName + (Reflect.java9 ? "$PlatformClassLoader" : "$ExtClassLoader"));
            ClassLoader extClassLoader = FabricLoader.class.getClassLoader();

            while (extClassLoader.getClass() != extClassLoaderClass) {
                extClassLoader = extClassLoader.getParent();
            }

            // Make new ExtClassLoader
            final ClassLoader newExtClassLoader;

            if (Reflect.java9) {
                ClassLoader bootLoader = (ClassLoader) Invoker.unreflect(Class.forName(launcherClassName), "bootLoader").invoke();
                newExtClassLoader = (ClassLoader) Invoker.findConstructor(extClassLoaderClass, bootLoader.getClass()).invoke(bootLoader);
            } else {
                newExtClassLoader = (ClassLoader) Invoker.unreflect(extClassLoaderClass.getDeclaredMethod("getExtClassLoader")).invoke();
            }

            // Make new AppClassLoader
            final SecureClassLoader newAppClassLoader;

            if (Reflect.java9) {
                final Object ucp = Invoker.findConstructor(Classes.URLClassPath, URL[].class, Class.forName("java.net.URLStreamHandlerFactory"), AccessControlContext.class)
                    .invoke((Object) new URL[0], null, null);
                final String classPath = System.getProperty("java.class.path");

                if (!classPath.isEmpty()) {
                    for (final String path : classPath.split(File.pathSeparator)) {
                        Classes.addURL(ucp, new File(path).getCanonicalFile().toURI().toURL());
                    }
                }

                newAppClassLoader = (SecureClassLoader) Invoker.findConstructor(Class.forName(appClassLoaderClassName), extClassLoaderClass, Classes.URLClassPath).invoke(extClassLoader, ucp);
            } else {
                newAppClassLoader = (SecureClassLoader) Invoker.findStatic(newExtClassLoader.loadClass(appClassLoaderClassName), "getAppClassLoader", ClassLoader.class, ClassLoader.class).invoke(extClassLoader);
            }

            final Field systemClassLoader = Fields.getField(ClassLoader.class, "scl");

            if (systemClassLoader == null) {
                Accessor.putObject(ClassLoader.class, "applicationClassLoader", newAppClassLoader);
            } else {
                Accessor.putObject(systemClassLoader, newAppClassLoader);
            }

            return newAppClassLoader;
        } catch (final Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }
    }

    public static void launchMain() {
        launchMain(getNewLoader());
    }

    public static void launchMain(final ClassLoader newLoader) {
        LogManager.shutdown();

        try {
            ((Closeable) (Accessor.getObject(ModResolver.class, "inMemoryFs"))).close();
        } catch (final IOException exception) {
            throw Unsafe.throwException(exception);
        }

        System.setProperty("fabric.side", FabricLoader.getInstance().getEnvironmentType().name().toLowerCase(Locale.ROOT));

        Thread.currentThread().setContextClassLoader(newLoader);

        try {
            Invoker.findStatic(newLoader.loadClass("net.devtech.grossfabrichacks.reload.GFHMain"), "main", void.class, String[].class).invokeExact(FabricLoader.getInstance().getLaunchArguments(false));

            System.exit(0);
        } catch (final Throwable throwable) {
            GrossFabricHacks.Common.crash(throwable);
        }
    }
}
