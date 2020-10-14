package org.spongepowered.asm.mixin.transformer;

import net.devtech.grossfabrichacks.GrossFabricHacks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;

public class HackedMixinTransformer extends MixinTransformer {
    public static final HackedMixinTransformer instance;
    public static final MixinProcessor processor;
    public static final Extensions extensions;

    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks/HackedMixinTransformer");

    @Override
    public byte[] transformClass(final MixinEnvironment environment, final String name, byte[] classBytes) {
        // raw class patching
        if (GrossFabricHacks.Common.transformPreMixinRawClass) {
            classBytes = GrossFabricHacks.Common.preMixinRawClassTransformer.transform(name, classBytes);
        }

        // ASM patching
        return this.transform(environment, this.readClass(classBytes), classBytes);
    }

    public byte[] transform(MixinEnvironment environment, ClassNode classNode, byte[] original) {
        final String name = classNode.name;

        if (GrossFabricHacks.Common.shouldWrite) {
            if (GrossFabricHacks.Common.transformPreMixinAsmClass) {
                GrossFabricHacks.Common.preMixinAsmClassTransformer.transform(classNode);
            }

            processor.applyMixins(environment, name.replace('/', '.'), classNode);

            if (GrossFabricHacks.Common.transformPostMixinAsmClass) {
                GrossFabricHacks.Common.postMixinAsmClassTransformer.transform(classNode);
            }

            // post mixin raw patching
            if (GrossFabricHacks.Common.transformPostMixinRawClass) {
                return GrossFabricHacks.Common.postMixinRawClassTransformer.transform(name, this.writeClass(classNode));
            }

            return this.writeClass(classNode);
        }

        if (processor.applyMixins(environment, name.replace('/', '.'), classNode)) {
            return this.writeClass(classNode);
        }

        return original;
    }

    static {
        final MixinTransformer mixinTransformer = (MixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();

        processor = Accessor.getObject(mixinTransformer, "processor");
        extensions = (Extensions) mixinTransformer.getExtensions();

        // here, we modify the klass pointer in the object to point towards the HackedMixinTransformer class, effectively turning the existing
        // MixinTransformer instance into an instance of HackedMixinTransformer
        Classes.setClass(mixinTransformer, HackedMixinTransformer.class);

        instance = (HackedMixinTransformer) mixinTransformer;
    }
}
