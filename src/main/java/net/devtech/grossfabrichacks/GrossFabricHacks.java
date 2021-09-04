package net.devtech.grossfabrichacks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.devtech.grossfabrichacks.entrypoints.RelaunchEntrypoint;
import net.devtech.grossfabrichacks.loader.GrossClassLoader;
import net.devtech.grossfabrichacks.loader.URLAdder;
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
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import user11681.dynamicentry.DynamicEntry;
import user11681.reflect.Classes;
import user11681.reflect.Reflect;

public class GrossFabricHacks implements LanguageAdapter {
    @Override
    public native <T> T create(ModContainer mod, String value, Class<T> type);

    /**
     * This class is intended to be loaded by the class loader that loaded {@linkplain net.fabricmc.loader.launch.knot.KnotClassLoader KnotClassLoader}<br>
     * so that classes loaded by different class loaders may share information.<br>
     * It may also be used for storing constants.<br>
     * It should be safe to load from {@linkplain Common#originalClassLoader the original class loader} and {@linkplain net.fabricmc.loader.launch.knot.KnotClassLoader KnotClassLoader}.
     */
    @SuppressWarnings("JavadocReference")
    public static class Common {
        /**
         * The property used for storing an array of names of classes to load instead of others present with the same name.
         */
        public static final String CLASS_PROPERTY = "gfh.common.classes";
        /**
         * The delimiter used for array elements in system properties.
         */
        public static final String DELIMITER = ",";

        public static final ClassLoader targetClassLoader = FabricLauncherBase.getLauncher().getTargetClassLoader();
        public static final ClassLoader originalClassLoader = targetClassLoader.getClass().getClassLoader();
        public static final GrossClassLoader classLoader;

        public static boolean mixinLoaded;
        public static boolean shouldHackMixin;
        public static boolean shouldWrite;

        public static RawClassTransformer preMixinRawClassTransformer;
        public static RawClassTransformer postMixinRawClassTransformer;
        public static AsmClassTransformer preMixinAsmClassTransformer;
        public static AsmClassTransformer postMixinAsmClassTransformer;

        public static String getMainClass() {
            return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? "net.fabricmc.loader.launch.knot.KnotClient"
                : "net.fabricmc.loader.launch.knot.KnotServer";
        }

        public static RuntimeException crash(Throwable throwable) {
            FabricGuiEntry.displayCriticalError(new RuntimeException("GrossFabricHacks encountered an error. Report it along with a log to https://github.com/GrossFabricHackers/GrossFabricHacks/issues", throwable), true);

            return Unsafe.throwException(throwable);
        }

        static {
            URLAdder.addURL(originalClassLoader, Common.class.getProtectionDomain().getCodeSource().getLocation());

            classLoader = Classes.reinterpret(Reflect.defaultClassLoader = targetClassLoader, UnsafeKnotClassLoader.class);
            classLoader.override(Common.class);

            for (Object klass : (Object[]) System.getProperties().remove(CLASS_PROPERTY)) {
                classLoader.override((Class<?>) klass);
            }
        }
    }

    static {
        LogManager.getLogger("GrossFabricHacks").info("no good? no, this man is definitely up to evil.");

        if (!Relauncher.relaunched()) {
            final List<String> entrypointNames = new ObjectArrayList<>();
            final MutableBoolean relaunch = new MutableBoolean();

            Consumer<RelaunchEntrypoint> handler = (RelaunchEntrypoint entrypoint) -> {
                entrypointNames.add(entrypoint.getClass().getName());

                if (entrypoint.shouldRelaunch()) {
                    relaunch.setTrue();
                }
            };

            DynamicEntry.execute("gfh:prePrePrePreLaunch", RelaunchEntrypoint.class, handler);
            DynamicEntry.execute("gfh:relaunch", RelaunchEntrypoint.class, handler);

            if (relaunch.booleanValue()) {
                new Relauncher().mainClass(Main.NAME).property(Relauncher.ENTRYPOINT_PROPERTY, String.join(Common.DELIMITER, entrypointNames)).relaunch();
            }
        }

        Object[] bootstrapClasses = new Object[]{
            "net.gudenau.lib.unsafe.Unsafe",
            "user11681.reflect.Accessor",
            "user11681.reflect.Classes",
            "user11681.reflect.Fields",
            "user11681.reflect.Invoker",
            "user11681.reflect.Reflect",
            "net.devtech.grossfabrichacks.unsafe.UnsafeUtil",
            "net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer",
            "net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer",
            "net.devtech.grossfabrichacks.transformer.TransformerApi",
            "net.devtech.grossfabrichacks.instrumentation.AsmInstrumentationTransformer",
            "net.devtech.grossfabrichacks.instrumentation.InstrumentationApi",
            "net.devtech.grossfabrichacks.loader.GrossClassLoader",
            "net.devtech.grossfabrichacks.loader.URLAdder"
        };

        ClassLoader preKnotClassLoader = GrossFabricHacks.class.getClassLoader().getClass().getClassLoader();

        for (int i = 0, length = bootstrapClasses.length; i < length; i++) {
            bootstrapClasses[i] = UnsafeUtil.findClass((String) bootstrapClasses[i], preKnotClassLoader);
        }

        System.getProperties().put(Common.CLASS_PROPERTY, bootstrapClasses);

        Unsafe.ensureClassInitialized(UnsafeUtil.findClass(GrossFabricHacks.class.getName() + "$Common", preKnotClassLoader));

//        DynamicEntry.tryExecute("gfh:prePrePreLaunch", PrePrePreLaunch.class, PrePrePreLaunch::onPrePrePreLaunch);
    }
}
