package eu.cloudtm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class Util {

    public static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static String prettyPrintTime(long time) {
        if (time == -1) {
            return "N/A";
        }
        return TIME_FORMAT.format(new Date(time));
    }

    public static String prettyPrintDate(long time) {
        if (time == -1) {
            return "N/A";
        }
        return DATE_FORMAT.format(new Date(time));
    }

    public static String prettyPrintNumber(Number number) {
        return NUMBER_FORMAT.format(number);
    }

    public static Class<?> loadClass(String name) {
        for (ClassLoader loader : classLoaders()) {
            Class cl = tryLoad(name, loader);
            if (cl != null) {
                return cl;
            }
        }
        return null;
    }

    public static InputStream loadResource(String name) {
        File file = new File(name);
        if (file.exists()) {
            try {
                return new FileInputStream(name);
            } catch (FileNotFoundException e) {
                //ignored
            }
        }
        for (ClassLoader loader : classLoaders()) {
            InputStream inputStream = tryLoadResource(name, loader);
            if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    public static Class<?> tryLoad(String name, ClassLoader loader) {
        try {
           return loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            //no-op
        }
        return null;
    }

    public static InputStream tryLoadResource(String name, ClassLoader loader) {
        return loader.getResourceAsStream(name);
    }

    private static final ClassLoader[] classLoaders() {
        return new ClassLoader[]{Thread.currentThread().getContextClassLoader(),
                Util.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()};
    }

}
