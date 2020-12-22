package net.fabricmc.loader.launch.knot;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.net.URLClassLoader;
import java.util.Map;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import net.devtech.grossfabrichacks.loader.GrossClassLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;
import user11681.reflect.Classes;
import user11681.reflect.Reflect;

@SuppressWarnings("JavadocReference")
public class UnsafeKnotClassLoader extends KnotClassLoader implements GrossClassLoader {
    /**
     * {@linkplain net.fabricmc.loader.launch.server.InjectingURLClassLoader InjectingURLClassLoader} in production servers; {@linkplain ClassLoader#getSystemClassLoader system class loader} everywhere else
     */
    private static final ClassLoader preKnotClassLoader = KnotClassLoader.class.getClassLoader();
    private static final URLClassLoader parent;
    private static final GrossKnotClassDelegate delegate;

    public static final Object2ReferenceOpenHashMap<String, Class<?>> overridingClasses = new Object2ReferenceOpenHashMap<>();

    public UnsafeKnotClassLoader(boolean isDevelopment, EnvType envType, GameProvider provider) {
        super(isDevelopment, envType, provider);
    }

    @Override
    public ClassLoader getOriginalLoader() {
        return preKnotClassLoader;
    }

    @Override
    public Class<?> getLoadedClass(String name) {
        return super.findLoadedClass(name);
    }

    @Override
    public boolean isClassLoaded(String name) {
        synchronized (super.getClassLoadingLock(name)) {
            return super.findLoadedClass(name) != null || Classes.findLoadedClass(preKnotClassLoader, name) != null;
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (super.getClassLoadingLock(name)) {
            Class<?> klass;

            if ((klass = overridingClasses.get(name)) == null && (klass = super.findLoadedClass(name)) == null) {
                if (name.startsWith("com.google.gson.")) {
                    klass = preKnotClassLoader.loadClass(name);
                } else {
                    final byte[] input = delegate.getPostMixinClassByteArray(name);

                    if (input == null) {
                        klass = preKnotClassLoader.loadClass(name);
                    } else {
                        final int packageEnd = name.lastIndexOf('.');

                        if (packageEnd > 0) {
                            final String packageName = name.substring(0, packageEnd);

                            if (super.getPackage(packageName) == null) {
                                super.definePackage(packageName, null, null, null, null, null, null, null);
                            }
                        }

                        klass = super.defineClass(name, input, 0, input.length, delegate.getMetadata(name, parent.getResource(delegate.getClassFileName(name))).codeSource);
                    }
                }
            }

            if (resolve) {
                super.resolveClass(klass);
            }

            return klass;
        }
    }

    @Override
    public Map<String, Class<?>> getOverridingClasses() {
        return overridingClasses;
    }

    static {
        Reflect.defaultClassLoader = Thread.currentThread().getContextClassLoader();
        parent = (URLClassLoader) Reflect.defaultClassLoader.getParent();
        delegate = Classes.staticCast(((KnotClassLoader) Reflect.defaultClassLoader).getDelegate(), GrossKnotClassDelegate.class);
        GrossFabricHacks.Common.classLoader = Classes.staticCast(Reflect.defaultClassLoader, UnsafeKnotClassLoader.class);

        for (final String klass : System.clearProperty(GrossFabricHacks.Common.CLASS_PROPERTY).split(GrossFabricHacks.Common.CLASS_DELIMITER)) {
            GrossFabricHacks.Common.classLoader.override((ClassLoader) GrossFabricHacks.Common.classLoader, klass);
        }
    }
}
