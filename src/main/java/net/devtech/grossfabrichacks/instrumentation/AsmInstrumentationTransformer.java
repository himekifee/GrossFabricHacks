package net.devtech.grossfabrichacks.instrumentation;

import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public interface AsmInstrumentationTransformer extends RawClassTransformer {
    void transform(String name, ClassNode node);

    @Override
    default byte[] transform(String name, byte[] data) {
        final ClassNode node = new ClassNode();
        new ClassReader(data).accept(node, 0);

        this.transform(name, node);

        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);

        return writer.toByteArray();
    }

    default AsmInstrumentationTransformer andThen(AsmInstrumentationTransformer fixer) {
        return (name, node) -> {
            this.transform(name, node);
            fixer.transform(name, node);
        };
    }
}
