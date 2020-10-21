package net.devtech.grossfabrichacks;

import java.io.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.relaunch.SameProcessRelauncher;
import net.devtech.grossfabrichacks.relaunch.SecondProcessRelauncher;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.dynamicentry.DynamicEntry;
import user11681.reflect.Classes;

public class GrossFabricHacks implements LanguageAdapter {
    private static final Logger logger = LogManager.getLogger("GrossFabricHacks");

    @Override
    public native <T> T create(ModContainer mod, String value, Class<T> type);

    /**
     * This class is intended to be loaded by the system class loader so that classes loaded by different class loaders may share information.
     */
    public static class Common {
        public static boolean mixinLoaded;
        public static boolean manualLoad;

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
        if(SecondProcessRelauncher.needsRelaunch()) {
            try {
                SameProcessRelauncher.relaunchIfNeeded();
            } catch (Throwable t) {
                logger.warn("Falling back to SecondProcessRelauncher", t);
                SecondProcessRelauncher.relaunchIfNeeded();
            }
        }

        logger.info("no good? no, this man is definitely up to evil.");

        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            final ClassLoader knotClassLoader = GrossFabricHacks.class.getClassLoader();

            for (final String name : new String[]{
                "net.gudenau.lib.unsafe.Unsafe",
                "net.devtech.grossfabrichacks.unsafe.UnsafeUtil",
                "user11681.reflect.Accessor",
                "user11681.reflect.Classes",
                "user11681.reflect.Fields",
                "user11681.reflect.Invoker",
                "user11681.reflect.Reflect"}) {
                Classes.defineSystemClass(knotClassLoader, name);
            }
        }

        Classes.addURL(Classes.systemClassLoader, GrossFabricHacks.class.getProtectionDomain().getCodeSource().getLocation());
        Classes.load(Classes.systemClassLoader,
            "net.devtech.grossfabrichacks.GrossFabricHacks$Common",
            "net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer",
            "net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer",
            "net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader"
        );

        DynamicEntry.tryExecute("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }

}
