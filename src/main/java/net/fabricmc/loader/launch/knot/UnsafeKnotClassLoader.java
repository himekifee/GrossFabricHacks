package net.fabricmc.loader.launch.knot;

import java.net.URLClassLoader;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.devtech.grossfabrichacks.GrossFabricHacks;
import user11681.reflect.Classes;
import user11681.reflect.Reflect;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.game.GameProvider;

@SuppressWarnings("JavadocReference")
public class UnsafeKnotClassLoader extends KnotClassLoader {
    /**
     * {@linkplain net.fabricmc.loader.launch.server.InjectingURLClassLoader InjectingURLClassLoader} in production servers; {@linkplain ClassLoader#getSystemClassLoader system class loader} everywhere else
     */
    public static final ClassLoader preKnotClassLoader = KnotClassLoader.class.getClassLoader();
    public static final UnsafeKnotClassLoader instance;
    public static final URLClassLoader parent;
    public static final ClassLoader dummyClassLoader;
    public static final GrossKnotClassDelegate delegate;

    public static final Reference2ReferenceOpenHashMap<String, Class<?>> overridingClasses = new Reference2ReferenceOpenHashMap<>();

    public UnsafeKnotClassLoader(final boolean isDevelopment, final EnvType envType, final GameProvider provider) {
        super(isDevelopment, envType, provider);
    }

    public static void override(final Class<?> klass) {
        overridingClasses.put(klass.getName().intern(), klass);
    }

    public static void override(final String name) {
        try {
            overridingClasses.put(name.intern(), preKnotClassLoader.loadClass(name));
        } catch (final ClassNotFoundException exception) {
            throw GrossFabricHacks.Common.crash(exception);
        }
    }

    public static void override(final ClassLoader classLoader, final String name) {
        try {
            overridingClasses.put(name.intern(), classLoader.loadClass(name));
        } catch (final ClassNotFoundException exception) {
            throw GrossFabricHacks.Common.crash(exception);
        }
    }

    public Class<?> getLoadedClass(final String name) {
        return super.findLoadedClass(name);
    }

    @Override
    public boolean isClassLoaded(final String name) {
        synchronized (super.getClassLoadingLock(name)) {
            return super.findLoadedClass(name) != null || Classes.findLoadedClass(preKnotClassLoader, name) != null;
        }
    }

    @Override
    public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (super.getClassLoadingLock(name)) {
            Class<?> klass;

            if ((klass = overridingClasses.get(name.intern())) == null && (klass = super.findLoadedClass(name)) == null) {
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

    static {
        for (final String klass : System.clearProperty(GrossFabricHacks.Common.CLASS_PROPERTY).split(GrossFabricHacks.Common.CLASS_DELIMITER)) {
            override(klass);
        }

        Reflect.defaultClassLoader = Thread.currentThread().getContextClassLoader();

        parent = (URLClassLoader) Reflect.defaultClassLoader.getParent();
        dummyClassLoader = parent.getParent();

        Classes.addURL(ClassLoader.getSystemClassLoader(), UnsafeKnotClassLoader.class.getProtectionDomain().getCodeSource().getLocation());

        delegate = Classes.staticCast(((KnotClassLoader) Reflect.defaultClassLoader).getDelegate(), GrossKnotClassDelegate.class);
        instance = Classes.staticCast(Reflect.defaultClassLoader, UnsafeKnotClassLoader.class);
    }
}
