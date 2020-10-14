package net.fabricmc.loader.launch.knot;

import java.net.URLClassLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;
import user11681.reflect.Classes;

public class UnsafeKnotClassLoader extends KnotClassLoader {
    public static final UnsafeKnotClassLoader instance;
    public static final URLClassLoader parent;
    public static final ClassLoader dummyClassLoader;
    public static final KnotClassDelegate delegate;

    public UnsafeKnotClassLoader(final boolean isDevelopment, final EnvType envType, final GameProvider provider) {
        super(isDevelopment, envType, provider);
    }

    public Class<?> getLoadedClass(final String name) {
        return super.findLoadedClass(name);
    }

    @Override
    public boolean isClassLoaded(final String name) {
        synchronized (super.getClassLoadingLock(name)) {
            return super.findLoadedClass(name) != null || Classes.findLoadedClass(Classes.systemClassLoader, name) != null;
        }
    }

    @Override
    public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            Class<?> klass;

            if ((klass = this.findLoadedClass(name)) == null && (klass = Classes.findLoadedClass(Classes.systemClassLoader, name)) == null) {
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
        instance = Classes.setClass(knotClassLoader, UnsafeKnotClassLoader.class);

        Classes.setClass(delegate, EarlyKnotClassDelegate.class);
    }
}
