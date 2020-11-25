package net.devtech.grossfabrichacks;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.devtech.grossfabrichacks.relaunch.Main;
import net.devtech.grossfabrichacks.relaunch.Relauncher;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.devtech.grossfabrichacks.unsafe.UnsafeUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.gui.FabricGuiEntry;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader;
import net.gudenau.lib.unsafe.Unsafe;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.dynamicentry.DynamicEntry;

@SuppressWarnings("ConstantConditions")
public class GrossFabricHacks implements LanguageAdapter {
    private static final Logger logger = LogManager.getLogger("GrossFabricHacks");

    @Override
    public native <T> T create(ModContainer mod, String value, Class<T> type);

    /**
     * This class is intended to be loaded by the class loader that loaded {@linkplain net.fabricmc.loader.launch.knot.KnotClassLoader KnotClassLoader}<br>
     * so that classes loaded by different class loaders may share information.<br>
     * It may also be used for storing constants.<br>
     * It should be safe to load from {@link Common#preKnotClassLoader} and {@linkplain net.fabricmc.loader.launch.knot.KnotClassLoader KnotClassLoader}.
     */
    @SuppressWarnings("JavadocReference")
    public static class Common {
        /**
         * the system property used temporarily for transferring information about<br>
         * the classes that have to be checked in {@linkplain UnsafeKnotClassLoader#preKnotClassLoader} first
         */
        public static final String CLASS_PROPERTY = "gfh.elementary.classes";
        public static final String CLASS_DELIMITER = ",";

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
                    throw crash(exception);
                }
            }

            return agent;
        }

        public static String getMainClass() {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? "net.fabricmc.loader.launch.knot.KnotClient"
                : "net.fabricmc.loader.launch.knot.KnotServer";
        }

        public static RuntimeException crash(final Throwable throwable) {
            FabricGuiEntry.displayCriticalError(new RuntimeException("GrossFabricHacks encountered an error. Report it along with a log to https://github.com/user11681/GrossFabricHacks/issues", throwable), true);

            return new RuntimeException(throwable);
        }
    }

    static {
        logger.info("no good? no, this man is definitely up to evil.");

        if (!Relauncher.relaunched()) {
            final ReferenceArrayList<RelaunchEntrypoint> entrypoints = new ReferenceArrayList<>(0);
            final Consumer<RelaunchEntrypoint> handler = entrypoints::add;

            DynamicEntry.execute("gfh:prePrePrePreLaunch", RelaunchEntrypoint.class, handler);
            DynamicEntry.execute("gfh:relaunch", RelaunchEntrypoint.class, handler);

            final StringBuilder entrypointNames = new StringBuilder();
            boolean relaunch = false;

            for (final RelaunchEntrypoint entrypoint : entrypoints) {
                if (entrypoint.shouldRelaunch()) {
                    relaunch = true;
                }

                if (entrypointNames.length() != 0) {
                    entrypointNames.append(Common.CLASS_DELIMITER);
                }

                entrypointNames.append(entrypoint.getClass().getName());
            }

            if (relaunch) {
                if (!Relauncher.relaunched()) {
                    new Relauncher().mainClass(Main.NAME).property(Relauncher.ENTRYPOINT_PROPERTY, entrypointNames.toString()).relaunch();
                }
            }
        }

        final String[] primaryClasses = new String[]{
            "net.gudenau.lib.unsafe.Unsafe",
            "user11681.reflect.Accessor",
            "user11681.reflect.Classes",
            "user11681.reflect.Fields",
            "user11681.reflect.Invoker",
            "user11681.reflect.Reflect",
            "net.devtech.grossfabrichacks.unsafe.UnsafeUtil",
            "net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer",
            "net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer",
            "net.devtech.grossfabrichacks.transformer.TransformerApi",
            "net.devtech.grossfabrichacks.instrumentation.AsmInstrumentationTransformer",
            "net.devtech.grossfabrichacks.instrumentation.InstrumentationApi",
            "net.devtech.grossfabrichacks.GrossFabricHacks$Common"
        };

        System.setProperty(Common.CLASS_PROPERTY, String.join(Common.CLASS_DELIMITER, primaryClasses));

        final ClassLoader preKnotClassLoader = GrossFabricHacks.class.getClassLoader().getClass().getClassLoader();

        for (int i = FabricLauncherBase.getLauncher().isDevelopment() ? 6 : 0, length = primaryClasses.length; i < length; i++) {
            UnsafeUtil.findClass(primaryClasses[i], preKnotClassLoader);
        }

        Unsafe.ensureClassInitialized(UnsafeUtil.findClass("net.fabricmc.loader.launch.knot.UnsafeKnotClassLoader", preKnotClassLoader));

        DynamicEntry.tryExecute("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }
}
