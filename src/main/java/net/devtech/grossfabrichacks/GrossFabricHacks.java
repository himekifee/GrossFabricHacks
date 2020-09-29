package net.devtech.grossfabrichacks;

import java.io.InputStream;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.instrumentation.InstrumentationApi;
import net.devtech.grossfabrichacks.transformer.TransformerApi;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.dynamicentry.DynamicEntry;
import user11681.reflect.Reflect;

public class GrossFabricHacks implements LanguageAdapter {
    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks");

//    public static final UnsafeKnotClassLoader UNSAFE_LOADER;

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
        LOGGER.info("no good? no, this man is definitely up to evil.");

        try {
            final ClassLoader applicationClassLoader = FabricLoader.class.getClassLoader();
            final ClassLoader KnotClassLoader = GrossFabricHacks.class.getClassLoader();

            final String[] classes = {
                "net.gudenau.lib.unsafe.Unsafe",
                "net.devtech.grossfabrichacks.instrumentation.InstrumentationAgent",
                "net.devtech.grossfabrichacks.instrumentation.InstrumentationApi",
                "net.devtech.grossfabrichacks.GrossFabricHacks$State",
                "net.devtech.grossfabrichacks.mixin.GrossFabricHacksPlugin",
                "net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer",
                "net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer",
                "net.devtech.grossfabrichacks.transformer.TransformerApi",
                "net.devtech.grossfabrichacks.unsafe.UnsafeUtil",
                "net.devtech.grossfabrichacks.unsafe.UnsafeUtil$FirstInt",
                "net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader",
                "org.spongepowered.asm.mixin.transformer.HackedMixinTransformer"
            };

            final int classCount = classes.length;

            for (int i = FabricLoader.getInstance().isDevelopmentEnvironment() ? 1 : 0; i < classCount; i++) {
                final String name = classes[i];
                final InputStream classStream = KnotClassLoader.getResourceAsStream(name.replace('.', '/') + ".class");
                final byte[] bytecode = new byte[classStream.available()];

                while (classStream.read(bytecode) != -1) {}

                Reflect.defineClass(applicationClassLoader, name, bytecode, GrossFabricHacks.class.getProtectionDomain());
            }

            final String name = "net.devtech.grossfabrichacks.mixin.GrossFabricHacksPlugin";
            final InputStream classStream = KnotClassLoader.getResourceAsStream(name.replace('.', '/') + ".class");
            final byte[] bytecode = new byte[classStream.available()];

            while (classStream.read(bytecode) != -1) {}

            Reflect.defineClass(KnotClassLoader, name, bytecode, GrossFabricHacks.class.getProtectionDomain());
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        TransformerApi.registerPostMixinAsmClassTransformer((one, two) -> {});
        InstrumentationApi.retransform(Object.class, (one, two) -> {});

        DynamicEntry.executeOptionalEntrypoint("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }
}
