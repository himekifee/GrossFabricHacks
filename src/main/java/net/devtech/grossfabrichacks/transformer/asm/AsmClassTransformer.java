package net.devtech.grossfabrichacks.transformer.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * a transformer using the ASM node
 */
public interface AsmClassTransformer {
	void transform(ClassNode node);

	default AsmClassTransformer andThen(AsmClassTransformer fixer) {
		return (c) -> {
			this.transform(c);
			fixer.transform(c);
		};
	}

	default RawClassTransformer asRaw() {
		return (name, data) -> {
			ClassReader reader = new ClassReader(data);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);
			this.transform(node);
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			return writer.toByteArray();
		};
	}
}
