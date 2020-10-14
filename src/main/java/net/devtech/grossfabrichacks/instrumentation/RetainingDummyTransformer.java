package net.devtech.grossfabrichacks.instrumentation;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.security.ProtectionDomain;

public class RetainingDummyTransformer implements CompatibilityClassFileTransformer {
    public final byte[][] bytecode;

    private final ReferenceOpenHashSet<Class<?>> classes;

    private int retransformedClassCount;

    public RetainingDummyTransformer(final Class<?>... classes) {
        this.classes = new ReferenceOpenHashSet<>(classes);

        this.bytecode = new byte[classes.length][];
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        if (this.classes.remove(classBeingRedefined)) {
            this.bytecode[this.retransformedClassCount++] = classfileBuffer;
        }

        return classfileBuffer;
    }
}
