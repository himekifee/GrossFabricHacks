package net.fabricmc.loader.launch.knot;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.transformer.FabricTransformer;
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

    public static boolean transformInitialized;

    public EarlyKnotClassDelegate(final boolean isDevelopment, final EnvType envType, final KnotClassLoaderInterface itf, final GameProvider provider) {
        super(isDevelopment, envType, itf, provider);
    }

    @Override
    public void initializeTransformers() {
        super.initializeTransformers();

        transformInitialized = true;

        mixinTransformer = Accessor.getObject(this, superclass, "mixinTransformer");
    }

    public static boolean canTransformClass(final String klass) {
        try {
            return (boolean) canTransformClass.invokeExact(klass);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public Object processClass(final String name, final boolean skipOriginalLoader) {
        try {
            byte[] bytecode = this.getRawClassByteArray(name, skipOriginalLoader);

            if (GrossFabricHacks.State.transformPreMixinRawClass) {
                bytecode = GrossFabricHacks.State.preMixinRawClassTransformer.transform(name, bytecode);
            }

            if (GrossFabricHacks.State.transformPreMixinAsmClass) {
                final ClassNode node = new ClassNode();

                new ClassReader(bytecode).accept(node, 0);

                GrossFabricHacks.State.preMixinAsmClassTransformer.transform(node);

                return node;
            }

            return bytecode;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public byte[] getPreMixinClassByteArray(String name, final boolean skipOriginalLoader) {
        name = name.replace('/', '.');

        try {
            if (!canTransformClass(name)) {
                return this.getRawClassByteArray(name, skipOriginalLoader);
            }

            if (!transformInitialized) {
                if (GrossFabricHacks.State.transformPreMixinAsmClass) {
                    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

                    ((ClassNode) this.processClass(name, skipOriginalLoader)).accept(writer);

                    return writer.toByteArray();
                }

                return (byte[]) this.processClass(name, skipOriginalLoader);
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
            throw new RuntimeException(String.format("Failed to load class file for \"%s\"", name), exception);
        }
    }

    @Override
    public byte[] getPostMixinClassByteArray(final String name) {
        if (!canTransformClass(name)) {
            return this.getPreMixinClassByteArray(name, true);
        }

        if (!transformInitialized) {
            if (GrossFabricHacks.State.transformPreMixinAsmClass) {
                final ClassNode node = (ClassNode) processClass(name, true);

                if (GrossFabricHacks.State.transformPostMixinAsmClass) {
                    GrossFabricHacks.State.postMixinAsmClassTransformer.transform(node);
                }

                final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

                node.accept(writer);

                if (GrossFabricHacks.State.transformPostMixinRawClass) {
                    return GrossFabricHacks.State.postMixinRawClassTransformer.transform(name.replace('.', '/'), writer.toByteArray());
                }

                return writer.toByteArray();
            }

            return this.getPreMixinClassByteArray(name, true);
        }

        return mixinTransformer.transformClassBytes(name, name, this.getPreMixinClassByteArray(name, true));
    }

    static {
        final FabricLoader loader = FabricLoader.getInstance();

        development = loader.isDevelopmentEnvironment();
        environment = loader.getEnvironmentType();
        canTransformClass = Invoker.findStatic(superclass, "canTransformClass", MethodType.methodType(boolean.class, String.class));
        provider = Accessor.getObject(UnsafeKnotClassLoader.delegate, "provider");
    }
}
