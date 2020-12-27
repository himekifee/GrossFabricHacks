package net.devtech.grossfabrichacks.relaunch;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import net.devtech.grossfabrichacks.transformer.asm.AsmClassTransformer;
import net.devtech.grossfabrichacks.transformer.asm.RawClassTransformer;
import net.fabricmc.loader.launch.knot.GrossKnotClassDelegate;
import net.fabricmc.loader.launch.knot.TransformingClassLoaderInterface;
import net.gudenau.lib.unsafe.Unsafe;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public class TransformingClassLoader extends URLClassLoader implements TransformingClassLoaderInterface {
    public static final TransformingClassLoader instance = new TransformingClassLoader(TransformingClassLoader.class.getClassLoader());
    public static final ClassLoader parent;

    private static final MethodHandle findLoadedClass;

    public static RawClassTransformer rawTransformer;
    public static AsmClassTransformer asmTransformer;

    public static final GrossKnotClassDelegate delegate = new GrossKnotClassDelegate(false, null, instance, null);

    public static void registerRawTransformer(RawClassTransformer transformer) {
        if (rawTransformer == null) {
            rawTransformer = transformer;
        } else {
            rawTransformer = rawTransformer.andThen(transformer);
        }
    }

    public static void registerAsmTransformer(AsmClassTransformer transformer) {
        if (asmTransformer == null) {
            asmTransformer = transformer;
        } else {
            asmTransformer = asmTransformer.andThen(transformer);
        }
    }

    public TransformingClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) {
        Class<?> klass = this.findLoadedClass(name);

        try {
            if (klass == null && (klass = (Class<?>) findLoadedClass.invokeExact(name)) == null) {
                byte[] raw = delegate.getRawClassByteArray(name);

                if (raw == null) {
                    throw new ClassNotFoundException(name);
                }

                if (rawTransformer != null) {
                    raw = rawTransformer.transform(name, raw);
                }

                if (asmTransformer != null) {
                    asmTransformer.transform(name, raw);
                }

                klass = super.defineClass(name, raw, 0, raw.length);
            }
        } catch (Throwable throwable) {
            try {
                klass = parent.loadClass(name);
            } catch (Throwable notFound) {
                throw Unsafe.throwException(notFound);
            }
        }

        if (resolve) {
            super.resolveClass(klass);
        }

        return klass;
    }

    @Override
    public GrossKnotClassDelegate getDelegate() {
        return delegate;
    }

    @Override
    public boolean isClassLoaded(String name) {
        try {
            return this.findLoadedClass(name) != null || ((Class<?>) findLoadedClass.invokeExact(name)) != null;
        } catch (Throwable throwable) {
            throw Unsafe.throwException(throwable);
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public InputStream getResourceAsStream(String filename, boolean skipOriginalLoader) {
        return super.getResourceAsStream(filename);
    }

    static {
        parent = instance.getParent();

        try {
            findLoadedClass = Unsafe.trustedLookup.bind(parent, "findLoadedClass", MethodType.methodType(Class.class, String.class));
        } catch (Throwable exception) {
            throw Unsafe.throwException(exception);
        }

        Thread.currentThread().setContextClassLoader(instance);
    }

}
