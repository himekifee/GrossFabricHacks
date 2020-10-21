package net.devtech.grossfabrichacks;

import java.io.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.ProtectionDomain;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.relaunch.GrossFabricHacksRelaunchException;
import net.devtech.grossfabrichacks.relaunch.SameProcessRelauncher;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.gui.FabricGuiEntry;
import net.gudenau.lib.unsafe.Unsafe;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import user11681.dynamicentry.DynamicEntry;

@SuppressWarnings("ConstantConditions")
public class GrossFabricHacks implements LanguageAdapter {
    @Override
    public native <T> T create(ModContainer mod, String value, Class<T> type);

    /**
     * This class is intended to be loaded by the system class loader so that classes loaded by different class loaders may share information.
     */
    public static class Common {
        public static boolean mixinLoaded;
        public static boolean shouldHackMixin;

        public static boolean shouldWrite;
        public static boolean transformPreMixinRawClass;
        public static boolean transformPreMixinAsmClass;
        public static boolean transformPostMixinRawClass;
        public static boolean transformPostMixinAsmClass;
        public static RawClassTransformer preMixinRawClassTransformer;
        public static RawClassTransformer postMixinRawClassTransformer;
        public static AsmClassTransformer preMixinAsmClassTransformer;
        public static AsmClassTransformer postMixinAsmClassTransformer;

        public static File getAgent() {
            final String source = GrossFabricHacks.class.getProtectionDomain().getCodeSource().getLocation().getFile();

            if (source.endsWith(".jar")) {
                return new File(source);
            }

            final File agent = new File(System.getProperty("user.dir"), "gross_agent.jar");

            if (!agent.exists()) {
                try {
                    File manifestFile = new File(source, "/META-INF/MANIFEST.MF");
                    if(!manifestFile.exists()) {
                        manifestFile = new File(source, "../../../resources/main/META-INF/MANIFEST.MF");
                    }
                    final JarOutputStream agentJar = new JarOutputStream(new FileOutputStream(agent), new Manifest(new FileInputStream(manifestFile)));
                    final String agentPath = "net/devtech/grossfabrichacks/instrumentation/InstrumentationAgent.class";

                    agentJar.putNextEntry(new ZipEntry(agentPath));

                    IOUtils.write(IOUtils.toByteArray(GrossFabricHacks.class.getResourceAsStream("/" + agentPath)), agentJar);

                    agentJar.close();
                } catch (final IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }

            return agent;
        }
    }

    static {
        try {
            SameProcessRelauncher.relaunchIfNeeded();
        } catch (Throwable t) {
            logger.fatal("Relaunching did not succeed. Please report this as a bug to GrossFabricHacks: https://github.com/Devan-Kerman/GrossFabricHacks/issues/new", t);
            FabricGuiEntry.displayCriticalError(new GrossFabricHacksRelaunchException(t), true);
        }

        logger.info("no good? no, this man is definitely up to evil.");

        try {
            final ClassLoader knotClassLoader = GrossFabricHacks.class.getClassLoader();
            final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            final ProtectionDomain protectionDomain = GrossFabricHacks.class.getProtectionDomain();
            final String[] classes = new String[]{
                "net.gudenau.lib.unsafe.Unsafe",
                "user11681.reflect.Accessor",
                "user11681.reflect.Invoker",
                "user11681.reflect.Classes",
                "user11681.reflect.Fields",
                "user11681.reflect.Reflect",
                "net.devtech.grossfabrichacks.unsafe.UnsafeUtil",
                "net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer",
                "net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer",
                "net.devtech.grossfabrichacks.GrossFabricHacks$Common",
                "net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader",
            };

            final int totalClassCount = classes.length;
            final int definedClassCount = totalClassCount - (FabricLoader.getInstance().isDevelopmentEnvironment() ? 6 : 0);

            for (int i = definedClassCount; i < totalClassCount; i++) {
                final String name = classes[i];
                final byte[] bytecode = IOUtils.toByteArray(knotClassLoader.getResourceAsStream(name.replace('.', '/') + ".class"));
                final Class<?> klass = Unsafe.defineClass(name, bytecode, 0, bytecode.length, systemClassLoader, protectionDomain);

                if (i == totalClassCount - 1) {
                    Unsafe.ensureClassInitialized(klass);
                }
            }

        } catch (final Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }

        DynamicEntry.tryExecute("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }

}
