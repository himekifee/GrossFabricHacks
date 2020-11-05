package net.fabricmc.loader.launch.knot;

import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.transformer.TransformerApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;
import user11681.reflect.Accessor;

public class EarlyKnotClassDelegate extends KnotClassDelegate {
    public static final Class<?> superclass = KnotClassDelegate.class;

    public static FabricMixinTransformerProxy mixinTransformer;

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
}
