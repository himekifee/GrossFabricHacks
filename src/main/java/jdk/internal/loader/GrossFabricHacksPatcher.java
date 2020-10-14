package jdk.internal.loader;

import sun.misc.URLClassPath;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

public class GrossFabricHacksPatcher {

    public static Class<?> patchClass(String name, boolean resolve) {
        return null;
    }

    public static URLStreamHandlerFactory getFactory() throws Exception {
        Class<?> launcherClass = Class.forName("sun.misc.Launcher");
        Field factoryField = launcherClass.getDeclaredField("factory");
        factoryField.setAccessible(true);
        return (URLStreamHandlerFactory) factoryField.get(null);
    }

    public static File[] getClassPath(String s) throws Exception {
        Class<?> launcherClass = Class.forName("sun.misc.Launcher");
        Method getClassPath = launcherClass.getDeclaredMethod("getClassPath", String.class);
        getClassPath.setAccessible(true);
        return (File[]) getClassPath.invoke(null, s);
    }

    public static URL[] pathToURLs(File[] files) throws Exception {
        Class<?> launcherClass = Class.forName("sun.misc.Launcher");
        Method pathToURLs = launcherClass.getDeclaredMethod("pathToURLs", File[].class);
        pathToURLs.setAccessible(true);
        return (URL[]) pathToURLs.invoke(null, (Object) files);
    }

    public static String getBootClassPath() throws Exception {
        Class<?> launcherClass = Class.forName("sun.misc.Launcher");
        Field bootClassPathField = launcherClass.getDeclaredField("bootClassPath");
        bootClassPathField.setAccessible(true);
        return (String) bootClassPathField.get(null);
    }

    public static void initLookupCache(URLClassPath urlClassPath, ClassLoader classLoader) throws Exception {
        Method initLookupCache = urlClassPath.getClass().getDeclaredMethod("initLookupCache", ClassLoader.class);
        initLookupCache.setAccessible(true);
        initLookupCache.invoke(urlClassPath, classLoader);
    }

    public static boolean knownToNotExist(URLClassPath urlClassPath, String s) throws Exception {
        Method knownToNotExist = urlClassPath.getClass().getDeclaredMethod("knownToNotExist", String.class);
        knownToNotExist.setAccessible(true);
        return (boolean) knownToNotExist.invoke(urlClassPath, s);
    }

}
