package net.devtech.grossfabrichacks;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.entrypoints.PrePrePrePreLaunch;
import net.devtech.grossfabrichacks.relaunch.RelaunchMarker;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.devtech.grossfabrichacks.unsafe.UnsafeUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.discovery.ModResolver;
import net.fabricmc.loader.game.MinecraftGameProvider;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.knot.Knot;
import net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader;
import net.gudenau.lib.unsafe.Unsafe;
import net.fabricmc.loader.util.Arguments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import user11681.dynamicentry.DynamicEntry;

public class GrossFabricHacks implements LanguageAdapter {
    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks");

    public static final UnsafeKnotClassLoader UNSAFE_LOADER;

    @Override
    public native <T> T create(net.fabricmc.loader.api.ModContainer mod, String value, Class<T> type);

/*
    private static void loadSimpleMethodHandle() {
        try {
            final String internalName = "net/devtech/grossfabrichacks/reflection/SimpleMethodHandle";
            final ClassReader reader = new ClassReader(GrossFabricHacks.class.getClassLoader().getResourceAsStream(internalName + ".class"));
            final ClassNode klass = new ClassNode();
            reader.accept(klass, 0);

            final MethodNode[] methods = klass.methods.toArray(new MethodNode[0]);

            for (final MethodNode method : methods) {
                if (method.desc.equals("([Ljava/lang/Object;)Ljava/lang/Object;")) {
                    method.access &= ~Opcodes.ACC_NATIVE;

                    method.visitVarInsn(Opcodes.ALOAD, 0);
                    method.visitFieldInsn(Opcodes.GETFIELD, internalName, "delegate", Type.getDescriptor(MethodHandle.class));
                    method.visitVarInsn(Opcodes.ALOAD, 1);
                    method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(MethodHandle.class), "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
                    method.visitInsn(Opcodes.ARETURN);
                }
            }
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
 */

    public static class State {
        public static boolean mixinLoaded;
        public static boolean manualLoad;

        public static boolean shouldWrite;
        // micro-optimization: cache transformer presence
        public static boolean transformPreMixinRawClass;
        public static boolean transformPreMixinAsmClass;
        public static boolean transformPostMixinRawClass;
        public static boolean transformPostMixinAsmClass;
        public static RawClassTransformer preMixinRawClassTransformer;
        public static RawClassTransformer postMixinRawClassTransformer;
        public static AsmClassTransformer preMixinAsmClassTransformer;
        public static AsmClassTransformer postMixinAsmClassTransformer;
    }

    static {
        // We don't use isAnnotationPresent because Knot won't
        // load the RelaunchMarker class from the AppClassLoader
        boolean isRelaunched = Arrays.asList(Knot.class.getAnnotations()).stream().anyMatch(a -> a.annotationType().getName().equals(RelaunchMarker.class.getName()));
        relaunch:
        if(!isRelaunched) {
            try {
                // get entrypoints
                ReferenceArrayList<PrePrePrePreLaunch> entrypoints = ReferenceArrayList.wrap((PrePrePrePreLaunch[])Array.newInstance(PrePrePrePreLaunch.class, 5), 0);
                DynamicEntry.executeOptionalEntrypoint("gfh:prePrePrePreLaunch", PrePrePrePreLaunch.class, entrypoints::add);

                // don't relaunch if there is no point in doing so
                // if(entrypoints.size() == 0) break relaunch;

                LOGGER.info("Relaunching...");

                // close the in-memory file system to avoid later collision
                Field inMemoryFsField = ModResolver.class.getDeclaredField("inMemoryFs");
                inMemoryFsField.setAccessible(true);
                ((Closeable) inMemoryFsField.get(null)).close();

                // look up the classloader hierarchy until we find Launcher$AppClassLoader
                final String appClassLoaderClassName = "sun.misc.Launcher$AppClassLoader";
                ClassLoader appClassLoader = FabricLoader.class.getClassLoader();
                while(!appClassLoader.getClass().getName().equals(appClassLoaderClassName)) {
                    appClassLoader = appClassLoader.getParent();
                }
                Class<?> appClassLoaderClass = Class.forName(appClassLoaderClassName);
                Method getAppClassLoader = appClassLoaderClass.getDeclaredMethod("getAppClassLoader", ClassLoader.class);
                getAppClassLoader.setAccessible(true);

                // Make new AppClassLoader
                ClassLoader newAppClassLoader = (ClassLoader) getAppClassLoader.invoke(null, appClassLoader.getParent());
                Thread.currentThread().setContextClassLoader(newAppClassLoader);

                // execute entrypoints
                defineClass(PrePrePrePreLaunch.class.getName(), FabricLauncherBase.getLauncher().getClassByteArray(PrePrePrePreLaunch.class.getName(), false), newAppClassLoader);
                entrypoints.forEach((entrypoint) -> {
                    String binaryName = entrypoint.getClass().getName();
                    try {
                        Class<?> entrypointClass = defineClass(binaryName, FabricLauncherBase.getLauncher().getClassByteArray(binaryName, false), newAppClassLoader);
                        Method onPrePrePrePreLaunch = entrypointClass.getMethod("onPrePrePrePreLaunch");
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
                String relaunchClassName = "net.fabricmc.loader.launch.knot.Knot";
                ClassReader relaunchReader = new ClassReader(FabricLauncherBase.getLauncher().getClassByteArray(relaunchClassName, false));
                ClassNode relaunchNode = new ClassNode();
                relaunchReader.accept(relaunchNode, 0);
                if(relaunchNode.visibleAnnotations == null) relaunchNode.visibleAnnotations = new ArrayList<>();
                relaunchNode.visibleAnnotations.add(new AnnotationNode("Lnet/devtech/grossfabrichacks/relaunch/RelaunchMarker;"));
                ClassWriter relaunchWriter = new ClassWriter(0);
                relaunchNode.accept(relaunchWriter);
                Class<?> newKnotClass = defineClass(relaunchClassName, relaunchWriter.toByteArray(), newAppClassLoader);

                // run Knot
                Method knotMain = newKnotClass.getMethod("main", String[].class);
                try {
                    knotMain.invoke(null, (Object) args.toArray());
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(-1);
                }
                System.exit(0);
            } catch (final Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        LOGGER.info("no good? no, this man is definitely up to evil.");
        try {
            final ClassLoader applicationClassLoader = FabricLoader.class.getClassLoader();
            final ClassLoader KnotClassLoader = GrossFabricHacks.class.getClassLoader();

        for (final String name : new String[]{
            "net.devtech.grossfabrichacks.GrossFabricHacks$State",
            "net.devtech.grossfabrichacks.unsafe.UnsafeUtil$FirstInt",
            "net.devtech.grossfabrichacks.unsafe.UnsafeUtil"}) {
            final InputStream classStream = KnotClassLoader.getResourceAsStream(name.replace('.', '/') + ".class");
            final byte[] bytecode = new byte[classStream.available()];

            while (classStream.read(bytecode) != -1) {}

            defineClass(name, bytecode, applicationClassLoader);
        }

        LOGGER.warn("KnotClassLoader, you fool! Loading me was a grave mistake.");

        UNSAFE_LOADER = UnsafeUtil.defineAndInitializeAndUnsafeCast(KnotClassLoader, "net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader", KnotClassLoader.getClass().getClassLoader());
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        DynamicEntry.executeOptionalEntrypoint("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }

    private static Class<?> defineClass(String name, byte[] bytecode, ClassLoader classLoader) {
        return Unsafe.defineClass(name, bytecode, 0, bytecode.length, classLoader, GrossFabricHacks.class.getProtectionDomain());
    }

}
