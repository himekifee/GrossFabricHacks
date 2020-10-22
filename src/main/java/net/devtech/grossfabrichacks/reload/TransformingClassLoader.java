package net.devtech.grossfabrichacks.reload;

import java.net.URL;
import java.net.URLClassLoader;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public class TransformingClassLoader extends URLClassLoader {
    public TransformingClassLoader(final ClassLoader parent) {
        super(new URL[0], parent);
    }

    // todo transform everything
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }
}
