package net.devtech.grossfabrichacks.relaunch;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.discovery.ModResolver;
import net.fabricmc.loader.game.MinecraftGameProvider;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.util.Arguments;
import net.gudenau.lib.unsafe.Unsafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.dynamicentry.DynamicEntry;
import user11681.reflect.Invoker;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.net.URL;
import java.security.AccessControlContext;

public class SameProcessRelauncher {

    private static final String ARG_IS_RELAUNCHED = SameProcessRelauncher.class.getName() + ".IS_RELAUNCHED";
    private static final Logger LOGGER = LogManager.getLogger("SameProcessRelauncher");

    public static void relaunchIfNeeded() {
        // We don't use isAnnotationPresent because Knot won't
        // load the RelaunchMarker class from the AppClassLoader
        boolean isRelaunched = Boolean.getBoolean(ARG_IS_RELAUNCHED);
        if(isRelaunched) return;
        try {
            // get entrypoints
            ReferenceArrayList<RelaunchEntrypoint> entrypoints = ReferenceArrayList.wrap((RelaunchEntrypoint[]) Array.newInstance(RelaunchEntrypoint.class, 5), 0);
            DynamicEntry.execute("gfh:prePrePrePreLaunch", RelaunchEntrypoint.class, entrypoints::add);
            DynamicEntry.execute("gfh:relaunchEntrypoint", RelaunchEntrypoint.class, entrypoints::add);

            // don't relaunch if there is no point in doing so
            // if(entrypoints.size() == 0) break relaunch;

            LOGGER.info("Relaunching with SameProcessRelauncher...");

            // close the in-memory file system to avoid later collision
            Field inMemoryFsField = ModResolver.class.getDeclaredField("inMemoryFs");
            inMemoryFsField.setAccessible(true);
            ((Closeable) inMemoryFsField.get(null)).close();

            boolean isJava9OrHigher = !System.getProperty("java.version").startsWith("1.");

            // look up the classloader hierarchy until we find Launcher$ExtClassLoader
            final String launcherClassName = isJava9OrHigher ? "jdk.internal.loader.ClassLoaders" : "sun.misc.Launcher";
            final String appClassLoaderClassName = launcherClassName + "$AppClassLoader";
            final String extClassLoaderClassName = launcherClassName + (isJava9OrHigher ? "$PlatformClassLoader" : "$ExtClassLoader");
            final String gfhPatcherClassName = "jdk/internal/loader/GrossFabricHacksPatcher";
            ClassLoader extClassLoader = FabricLoader.class.getClassLoader();
            while(!extClassLoader.getClass().getName().equals(extClassLoaderClassName)) {
                extClassLoader = extClassLoader.getParent();
            }
            Class<?> extClassLoaderClass = Class.forName(extClassLoaderClassName);

            // Make new ExtClassLoader
            ClassLoader newExtClassLoader;
            if(!isJava9OrHigher) {
                Method getExtClassLoader = extClassLoaderClass.getDeclaredMethod("getExtClassLoader");
                newExtClassLoader = (ClassLoader) Invoker.unreflect(getExtClassLoader).invoke();
            } else {
                Class<?> launcherClass = Class.forName(launcherClassName);
                Method bootLoaderMethod = launcherClass.getDeclaredMethod("bootLoader");
                ClassLoader bootLoader = (ClassLoader) Invoker.unreflect(bootLoaderMethod).invoke();
                Constructor<?> platformCtor = extClassLoaderClass.getDeclaredConstructor(bootLoader.getClass());
                newExtClassLoader = (ClassLoader) Invoker.unreflectConstructor(platformCtor).invoke(bootLoader);
            }

            // Make new AppClassLoader
            ClassLoader newAppClassLoader;
            if(!isJava9OrHigher) {
                Class<?> newAppClass = newExtClassLoader.loadClass(appClassLoaderClassName);
                Method getAppClassLoader = newAppClass.getDeclaredMethod("getAppClassLoader", ClassLoader.class);
                newAppClassLoader = (ClassLoader) Invoker.unreflect(getAppClassLoader).invoke(extClassLoader);
            } else {
                Class<?> urlClassPathClass = Class.forName("jdk.internal.loader.URLClassPath");
                Class<?> urlSHFClass = Class.forName("java.net.URLStreamHandlerFactory");
                Object ucp = Invoker.unreflectConstructor(urlClassPathClass.getDeclaredConstructor(URL[].class, urlSHFClass, AccessControlContext.class))
                        .invoke((Object) new URL[0], null, null);
                String cp = System.getProperty("java.class.path");
                if(!cp.isEmpty()) {
                    MethodHandle addUrl = Invoker.unreflect(urlClassPathClass.getDeclaredMethod("addURL", URL.class));
                    for(String s : cp.split(File.pathSeparator)) {
                        File file = new File(s).getCanonicalFile();
                        addUrl.invoke(ucp, file.toURI().toURL());
                    }
                }
                Class<?> appClassLoaderClass = Class.forName(appClassLoaderClassName);
                Constructor<?> appCtor = appClassLoaderClass.getDeclaredConstructor(extClassLoaderClass, urlClassPathClass);
                newAppClassLoader = (ClassLoader) Invoker.unreflectConstructor(appCtor).invoke(extClassLoader, ucp);
            }

            Field scl;
            try {
                scl = ClassLoader.class.getDeclaredField("scl");
            } catch (NoSuchFieldException e) {
                scl = ClassLoader.class.getDeclaredField("applicationClassLoader");
            }
            scl.setAccessible(true);
            scl.set(null, newAppClassLoader);
            Thread.currentThread().setContextClassLoader(newAppClassLoader);

            // execute entrypoints
            defineClass(RelaunchEntrypoint.class.getName(), FabricLauncherBase.getLauncher().getClassByteArray(RelaunchEntrypoint.class.getName(), false), newAppClassLoader);
            entrypoints.forEach((entrypoint) -> {
                String binaryName = entrypoint.getClass().getName();
                try {
                    Class<?> entrypointClass = defineClass(binaryName, FabricLauncherBase.getLauncher().getClassByteArray(binaryName, false), newAppClassLoader);
                    Method onPrePrePrePreLaunch = entrypointClass.getMethod("onRelaunch");
                    onPrePrePrePreLaunch.invoke(entrypointClass.getConstructor().newInstance());
                } catch (InvocationTargetException e) {
                    LOGGER.fatal(String.format("An error was encountered in the prePrePrePre entrypoint of class %s", binaryName), e);
                    System.exit(-1);
                } catch (ReflectiveOperationException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // grab main args
            Field mcArguments = MinecraftGameProvider.class.getDeclaredField("arguments");
            mcArguments.setAccessible(true);
            Arguments args = (Arguments) mcArguments.get(((net.fabricmc.loader.FabricLoader) FabricLoader.getInstance()).getGameProvider());
            if(System.getProperty("fabric.side") == null) {
                System.setProperty("fabric.side", FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "client" : "server");
            }

            // Set RelaunchLatch to true with ASM
            System.setProperty(ARG_IS_RELAUNCHED, "true");

            // run Knot
            Method knotMain = newAppClassLoader.loadClass("net.fabricmc.loader.launch.knot.Knot").getMethod("main", String[].class);
            try {
                knotMain.invoke(null, (Object) args.toArray());
            } catch (Throwable t) {
                if(t.getCause() != null) {
                    t.getCause().printStackTrace();
                } else {
                    t.printStackTrace();
                }
                System.exit(-1);
            }
            System.exit(0);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static Class<?> defineClass(String name, byte[] bytecode, ClassLoader classLoader) {
        return Unsafe.defineClass(name, bytecode, 0, bytecode.length, classLoader, GrossFabricHacks.class.getProtectionDomain());
    }

    private static byte[] getClassBytecode(String name, ClassLoader source) throws IOException, ClassNotFoundException {
        InputStream inputStream = source.getResourceAsStream(name.replace('.', '/') + ".class");
        if (inputStream == null) {
            throw new ClassNotFoundException();
        } else {
            int a = inputStream.available();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(a, 32));
            byte[] buffer = new byte[8192];
            int len;
            while((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            inputStream.close();
            return outputStream.toByteArray();
        }
    }

}
