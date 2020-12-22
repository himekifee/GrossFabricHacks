package net.devtech.grossfabrichacks.instrumentation;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.security.ProtectionDomain;

public class RetainingDummyTransformer implements CompatibilityClassFileTransformer {
    public final byte[][] bytecode;

    private final ReferenceOpenHashSet<Class<?>> classes;

    private int retransformedClassCount;

    public RetainingDummyTransformer(Class<?>... classes) {
        this.classes = new ReferenceOpenHashSet<>(classes);

        this.bytecode = new byte[classes.length][];
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (this.classes.remove(classBeingRedefined)) {
            this.bytecode[this.retransformedClassCount++] = classfileBuffer;
        }

        return classfileBuffer;
    }
}
