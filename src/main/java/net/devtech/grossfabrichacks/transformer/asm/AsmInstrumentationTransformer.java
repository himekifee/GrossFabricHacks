package net.devtech.grossfabrichacks.transformer.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public interface AsmInstrumentationTransformer {
    void transform(String name, ClassNode node);

    default AsmInstrumentationTransformer andThen(AsmInstrumentationTransformer fixer) {
        return (s, c) -> {
            this.transform(s, c);
            fixer.transform(s, c);
        };
    }

    default RawClassTransformer asRaw() {
        return (name, data) -> {
            ClassReader reader = new ClassReader(data);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            this.transform(node.name, node);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        };
    }
}
