package net.devtech.grossfabrichacks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.reload.GrossFabricHacksReloadException;
import net.devtech.grossfabrichacks.reload.Reloader;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.gui.FabricGuiEntry;
import net.fabricmc.loader.launch.common.FabricLauncher;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.gudenau.lib.unsafe.Unsafe;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.dynamicentry.DynamicEntry;

@SuppressWarnings("ConstantConditions")
public class GrossFabricHacks implements LanguageAdapter {
    private static final Logger LOGGER = LogManager.getLogger("GrossFabricHacks");

    @Override
    public native <T> T create(ModContainer mod, String value, Class<T> type);

    /**
     * This class is intended to be loaded by the system class loader so that classes loaded by different class loaders may share information.
     * It should be safe to load from any class loader after it is loaded by the system class loader.
     */
    public static class Common {
        public static final String reloadedProperty = "gfh.reloaded";

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

            final File agent = new File(source, "gross_agent.jar");

            if (!agent.exists()) {
                try {
                    File manifestFile = new File(source, "META-INF/MANIFEST.MF");

                    if (!manifestFile.exists()) {
                        manifestFile = new File(source, "../../../resources/main/META-INF/MANIFEST.MF");
                    }

                    final JarOutputStream agentJar = new JarOutputStream(new FileOutputStream(agent), new Manifest(new FileInputStream(manifestFile)));
                    final String agentPath = "net/devtech/grossfabrichacks/instrumentation/InstrumentationAgent.class";

                    agentJar.putNextEntry(new ZipEntry(agentPath));

                    IOUtils.write(IOUtils.toByteArray(GrossFabricHacks.class.getResourceAsStream("/" + agentPath)), agentJar);

                    agentJar.close();
                } catch (final IOException exception) {
                    crash(exception);
                }
            }

            return agent;
        }

        public static String getMainClass() {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? "net.fabricmc.loader.launch.knot.KnotClient"
                : "net.fabricmc.loader.launch.knot.KnotServer";
        }

        public static void crash(final Throwable throwable) {
            throwable.printStackTrace();

            FabricGuiEntry.displayCriticalError(new GrossFabricHacksReloadException(throwable), true);

            System.exit(-1);
        }
    }

    static {
        LOGGER.info("no good? no, this man is definitely up to evil.");

        try {
            final FabricLauncher launcher = FabricLauncherBase.getLauncher();
            final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            final ProtectionDomain protectionDomain = GrossFabricHacks.class.getProtectionDomain();
            final String[] classes = {
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
                "net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader"
            };

            final int totalClassCount = classes.length;
            final int definedClassCount = FabricLoader.getInstance().isDevelopmentEnvironment() ? 6 : 0;

            for (int i = definedClassCount; i < totalClassCount; i++) {
                final String name = classes[i];
                final byte[] bytecode = launcher.getClassByteArray(name, true);
                final Class<?> klass = Unsafe.defineClass(name, bytecode, 0, bytecode.length, systemClassLoader, protectionDomain);

                if (i == totalClassCount - 1) {
                    Unsafe.ensureClassInitialized(klass);
                }
            }
        } catch (final Throwable throwable) {
            Common.crash(throwable);
        }

        Reloader.ensureReloaded();

        DynamicEntry.tryExecute("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }
}
