package net.devtech.grossfabrichacks.instrumentation;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.transformer.TransformerApi;
import net.devtech.grossfabrichacks.transformer.asm.AsmInstrumentationTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import user11681.reflect.Accessor;
import user11681.reflect.Classes;

public class InstrumentationApi {
    private static final Set<String> TRANSFORMABLE = new HashSet<>();
    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks/InstrumentationApi");

    public static Instrumentation instrumentation;

    /**
     * adds a transformer that pipes a class through TransformerBootstrap,
     * this allows you to mix into any all classes (including JDK!) classes, with a few caveats.
     * <p>
     * If the class was loaded *before* you get to it, do not call this method!
     * don't pipe classes that may already be transformable by mixin, or they may be called twice.
     *
     * @param cls the internal name of the class
     */
    public static void pipeClassThroughTransformerBootstrap(String cls) {
        TRANSFORMABLE.add(cls);
        Transformable.init();
    }

    /**
     * a convenience method for {@link InstrumentationApi#retransform(Class, AsmInstrumentationTransformer)}
     * intended to be used when the target class is not visible
     *
     * @param cls         the binary name (defined in the last section of the {@linkplain ClassLoader ClassLoader javadoc}
     *                    of the class to retransform
     * @param transformer the class transformer
     */
    public static void retransform(final String cls, final AsmInstrumentationTransformer transformer) {
        try {
            retransform(Class.forName(cls), transformer);
        } catch (final ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void retransform(Class<?> cls, AsmInstrumentationTransformer transformer) {
        retransform(cls, transformer.asRaw());
    }

    /**
     * a convenience method for {@link InstrumentationApi#retransform(Class, RawClassTransformer)}
     * intended to be used when the target class is not visible
     *
     * @param cls         the binary name
     *                    (defined in the last section of the {@linkplain ClassLoader ClassLoader Javadoc})
     *                    of the class to retransform
     * @param transformer the class transformer
     */
    public static void retransform(final String cls, final RawClassTransformer transformer) {
        try {
            retransform(Class.forName(cls), transformer);
        } catch (final ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * retransform the class represented by {@code cls} by {@code transformer}.
     * The {@code className} passed to {@code transformer} may be null if {@code cls} is a JDK class.
     *
     * @param cls         the class to retransform.
     * @param transformer the class transformer.
     */
    public static void retransform(Class<?> cls, RawClassTransformer transformer) {
        try {
            CompatibilityClassFileTransformer fileTransformer = (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
                if (cls == classBeingRedefined) {
                    return transformer.transform(className, classfileBuffer);
                }

                return classfileBuffer;
            };

            instrumentation.addTransformer(fileTransformer, true);
            instrumentation.retransformClasses(cls);
            instrumentation.removeTransformer(fileTransformer);
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassNode[] getNodes(final Class<?>... classes) {
        final int classCount = classes.length;
        final ClassNode[] nodes = new ClassNode[classCount];
        final byte[][] bytes = getBytecode(classes);

        for (int i = 0; i < classCount; i++) {
            new ClassReader(bytes[i]).accept(nodes[i] = new ClassNode(), 0);
        }

        return nodes;
    }

    public static ClassNode getNode(final Class<?> klass) {
        final ClassNode node = new ClassNode();

        new ClassReader(getBytecode(klass)).accept(node, 0);

        return node;
    }

    public static byte[][] getBytecode(final Class<?>... classes) {
        final RetainingDummyTransformer transformer = new RetainingDummyTransformer(classes);

        instrumentation.addTransformer(transformer);

        try {
            instrumentation.retransformClasses(classes);
        } catch (final UnmodifiableClassException exception) {
            throw new RuntimeException(exception);
        }

        return transformer.bytecode;
    }

    public static byte[] getBytecode(final Class<?> klass) {
        final RetainingDummyTransformer transformer = new RetainingDummyTransformer(klass);

        instrumentation.addTransformer(transformer);

        try {
            instrumentation.retransformClasses(klass);
        } catch (final UnmodifiableClassException exception) {
            throw new RuntimeException(exception);
        }

        instrumentation.removeTransformer(transformer);

        return transformer.bytecode[0];
    }

    // to seperate out the static block
    private static class Transformable {
        private static boolean init;
        private static final CompatibilityClassFileTransformer TRANSFORMER = (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            if (TRANSFORMABLE.remove(node.name)) {
                if (TRANSFORMABLE.isEmpty()) {
                    deinit();
                }

                return TransformerApi.transformClass(node);
            }

            return classfileBuffer;
        };

        private static void deinit() {
            instrumentation.removeTransformer(TRANSFORMER);
            init = false;
        }

        private static void init() {
            if (!init) {
                instrumentation.addTransformer(TRANSFORMER);
                init = true;
            }
        }

        static {
            // pipe transformer to
            TransformerApi.manualLoad();
        }
    }

    static {
        try {
            final String name = ManagementFactory.getRuntimeMXBean().getName();
            final File agent = GrossFabricHacks.getAgent();

            LOGGER.info("Attaching instrumentation agent to VM.");

            ByteBuddyAgent.attach(agent, name.substring(0, name.indexOf('@')));

            LOGGER.info("Successfully attached instrumentation agent.");

            agent.delete();

            instrumentation = Accessor.getObject(Classes.load("net.devtech.grossfabrichacks.instrumentation.InstrumentationAgent"), "instrumentation");
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
