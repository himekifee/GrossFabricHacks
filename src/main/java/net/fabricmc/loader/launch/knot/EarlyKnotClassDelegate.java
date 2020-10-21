package net.fabricmc.loader.launch.knot;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.transformer.TransformerApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.transformer.FabricTransformer;
import net.gudenau.lib.unsafe.Unsafe;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;
import user11681.reflect.Accessor;
import user11681.reflect.Invoker;

public class EarlyKnotClassDelegate extends KnotClassDelegate {
    public static final Class<?> superclass = KnotClassDelegate.class;

    private static FabricMixinTransformerProxy mixinTransformer;
    private static final boolean development;
    private static final EnvType environment;
    private static final GameProvider provider;
    private static final MethodHandle canTransformClass;

    public EarlyKnotClassDelegate(final boolean isDevelopment, final EnvType envType, final KnotClassLoaderInterface itf, final GameProvider provider) {
        super(isDevelopment, envType, itf, provider);
    }

    @Override
    public void initializeTransformers() {
        super.initializeTransformers();

        mixinTransformer = Accessor.getObject(this, superclass, "mixinTransformer");

        GrossFabricHacks.Common.mixinLoaded = true;

        if (GrossFabricHacks.Common.shouldWrite || GrossFabricHacks.Common.shouldHackMixin) {
            TransformerApi.manualLoad();
        }
    }

    public static boolean canTransformClass(final String klass) {
        try {
            return (boolean) canTransformClass.invokeExact(klass);
        } catch (final Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }
    }

    @Override
    public byte[] getPreMixinClassByteArray(String name, final boolean skipOriginalLoader) {
        name = name.replace('/', '.');

        try {
            if (!canTransformClass(name)) {
                return this.getRawClassByteArray(name, skipOriginalLoader);
            }

            if (!GrossFabricHacks.Common.mixinLoaded) {
                byte[] bytecode = this.getRawClassByteArray(name, skipOriginalLoader);

                if (GrossFabricHacks.Common.transformPreMixinRawClass) {
                    bytecode = GrossFabricHacks.Common.preMixinRawClassTransformer.transform(name, bytecode);
                }

                if (GrossFabricHacks.Common.preMixinAsmClassTransformer == null) {
                    return bytecode;
                }

                final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                final ClassNode node = new ClassNode();

                new ClassReader(bytecode).accept(node, 0);

                GrossFabricHacks.Common.preMixinAsmClassTransformer.transform(node);

                node.accept(writer);

                return writer.toByteArray();
            }

            byte[] input = provider.getEntrypointTransformer().transform(name);

            if (input == null) {
                input = this.getRawClassByteArray(name, skipOriginalLoader);
            }

            if (input != null) {
                return FabricTransformer.transform(development, environment, name, input);
            }

            return null;
        } catch (final IOException exception) {
            throw Unsafe.throwException(exception);
        }
    }

    @Override
    public byte[] getPostMixinClassByteArray(final String name) {
        final byte[] bytecode = this.getPreMixinClassByteArray(name, true);

        if (!canTransformClass(name)) {
            return bytecode;
        }

        if (GrossFabricHacks.Common.mixinLoaded) {
            return mixinTransformer.transformClassBytes(name, name, bytecode);
        }

        if (bytecode == null) {
            return null;
        }

        if (GrossFabricHacks.Common.shouldWrite) {
            final ClassNode node = new ClassNode();

            new ClassReader(bytecode).accept(node, 0);

            if (GrossFabricHacks.Common.transformPostMixinAsmClass) {
                GrossFabricHacks.Common.postMixinAsmClassTransformer.transform(node);
            }

            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            node.accept(writer);

            if (GrossFabricHacks.Common.transformPostMixinRawClass) {
                return GrossFabricHacks.Common.postMixinRawClassTransformer.transform(name.replace('.', '/'), writer.toByteArray());
            }

            return writer.toByteArray();
        }

        return bytecode;

    }

    static {
        final FabricLoader loader = FabricLoader.getInstance();

        development = loader.isDevelopmentEnvironment();
        environment = loader.getEnvironmentType();
        canTransformClass = Invoker.findStatic(superclass, "canTransformClass", MethodType.methodType(boolean.class, String.class));
        provider = Accessor.getObject(UnsafeKnotClassLoader.delegate, "provider");
    }
}
