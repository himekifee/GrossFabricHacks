package net.fabricmc.loader.launch.knot;

import java.net.URL;
import java.net.URLClassLoader;
import user11681.reflect.Classes;

public class SharingDynamicURLClassLoader extends URLClassLoader {
    public SharingDynamicURLClassLoader(final URL[] urls) {
        super(urls, new DummyClassLoader());
    }

    @Override
    protected void addURL(final URL url) {
        super.addURL(url);

        Classes.addURL(UnsafeKnotClassLoader.systemClassLoaderPath, url);
        System.out.println(url);
    }

    static {
        registerAsParallelCapable();

        for (final URL url : UnsafeKnotClassLoader.parent.getURLs()) {
            Classes.addURL(UnsafeKnotClassLoader.systemClassLoaderPath, url);
        }
    }
}
