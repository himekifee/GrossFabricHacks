package net.fabricmc.loader.launch.knot;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.net.URLClassLoader;
import net.devtech.grossfabrichacks.unsafe.UnsafeUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.reflect.Classes;

public class UnsafeKnotClassLoader extends KnotClassLoader {
    public static final UnsafeKnotClassLoader instance;
    public static final Object2ReferenceOpenHashMap<String, Class<?>> classes = new Object2ReferenceOpenHashMap<>();
    public static final URLClassLoader parent;
    public static final ClassLoader dummyClassLoader;
    public static final KnotClassDelegate delegate;
    public static final Object systemClassLoaderPath;

    private static final Logger logger = LogManager.getLogger("GrossFabricHacks/UnsafeKnotClassLoader");

    public UnsafeKnotClassLoader(final boolean isDevelopment, final EnvType envType, final GameProvider provider) {
        super(isDevelopment, envType, provider);
    }

    public Class<?> defineClass(final String name, final byte[] bytes) {
        final Class<?> klass = UnsafeUtil.defineClass(name, bytes, null, null);

        classes.put(name, klass);

        return klass;
    }

    public Class<?> getLoadedClass(final String name) {
        final Class<?> klass = super.findLoadedClass(name);

        if (klass == null) {
            return classes.get(name);
        }

        return klass;
    }

    @Override
    public boolean isClassLoaded(final String name) {
        synchronized (super.getClassLoadingLock(name)) {
            return super.findLoadedClass(name) != null || classes.get(name) != null;
        }
    }

    @Override
    public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> klass;

            if ((klass = classes.get(name)) == null && (klass = this.findLoadedClass(name)) == null && (klass = Classes.findLoadedClass(Classes.systemClassLoader, name)) == null) {
                try {
                    if (!name.startsWith("com.google.gson.") && !name.startsWith("java.")) {
                        final byte[] input = delegate.getPostMixinClassByteArray(name);

                        if (input != null) {
                            final int pkgDelimiterPos = name.lastIndexOf('.');

                            if (pkgDelimiterPos > 0) {
                                final String pkgString = name.substring(0, pkgDelimiterPos);

                                if (super.getPackage(pkgString) == null) {
                                    super.definePackage(pkgString, null, null, null, null, null, null, null);
                                }
                            }

                            klass = super.defineClass(name, input, 0, input.length, delegate.getMetadata(name, parent.getResource(delegate.getClassFileName(name))).codeSource);
                        } else {
                            klass = Classes.systemClassLoader.loadClass(name);
                        }
                    } else {
                        klass = Classes.systemClassLoader.loadClass(name);
                    }
                } catch (final ClassFormatError formatError) {
                    logger.warn("A ClassFormatError was encountered while attempting to define {}; resorting to definition by the bootstrap class loader.", name);

                    classes.put(name, klass = UnsafeUtil.defineClass(name, delegate.getPostMixinClassByteArray(name)));
                }
            }

            if (resolve) {
                this.resolveClass(klass);
            }

            return klass;
        }
    }

    static {
        final KnotClassLoader knotClassLoader = (KnotClassLoader) Thread.currentThread().getContextClassLoader();

        parent = (URLClassLoader) knotClassLoader.getParent();
        dummyClassLoader = parent.getParent();
        delegate = knotClassLoader.getDelegate();
        instance = UnsafeUtil.unsafeCast(knotClassLoader, UnsafeKnotClassLoader.class);
        systemClassLoaderPath = Classes.getClassPath(Classes.systemClassLoader);

        UnsafeUtil.unsafeCast(parent, SharingDynamicURLClassLoader.class);
        UnsafeUtil.unsafeCast(delegate, EarlyKnotClassDelegate.class);
    }
}
