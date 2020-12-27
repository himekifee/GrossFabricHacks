package net.devtech.grossfabrichacks.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Since Java 9, {@link ClassFileTransformer} is not a functional interface: it has 2 {@code default} methods.<br>
 * The purpose of this class is solving that problem by extending {@link ClassFileTransformer} and making the pre-Java 9 method abstract.
 */
public interface CompatibilityClassFileTransformer extends ClassFileTransformer {
    @Override
    byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer);
}
