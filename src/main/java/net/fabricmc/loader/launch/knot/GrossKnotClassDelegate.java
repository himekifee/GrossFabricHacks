package net.fabricmc.loader.launch.knot;

import java.io.IOException;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.transformer.TransformerApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;
import net.gudenau.lib.unsafe.Unsafe;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;
import user11681.reflect.Accessor;

public class GrossKnotClassDelegate extends KnotClassDelegate {
    public static final Class<?> superclass = KnotClassDelegate.class;

    public static FabricMixinTransformerProxy mixinTransformer;

    public GrossKnotClassDelegate(final boolean isDevelopment, final EnvType envType, final KnotClassLoaderInterface itf, final GameProvider provider) {
        super(isDevelopment, envType, itf, provider);
    }

    public byte[] getRawClassByteArray(final String name) {
        try {
            return super.getRawClassByteArray(name, false);
        } catch (final IOException exception) {
            throw Unsafe.throwException(exception);
        }
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
}
