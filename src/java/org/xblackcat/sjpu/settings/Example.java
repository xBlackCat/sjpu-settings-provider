package org.xblackcat.sjpu.settings;

import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 18.06.2016 19:54
 *
 * @author xBlackCat
 */
public class Example {
    public static Example of(Class<?> clazz) {
        final Example config = new Example();
        config.and(clazz);
        return config;
    }

    private final List<ConfigInfo<?>> infos = new ArrayList<>();
    private String header;
    private String footer;

    private Example() {
    }

    public <T> Example and(Class<T> clazz) {
        return and(clazz, null, null);
    }

    public <T> Example and(Class<T> clazz, String prefix) {
        return and(clazz, prefix, null);
    }

    public <T> Example and(Class<T> clazz, T defValues) {
        return and(clazz, null, defValues);
    }

    public <T> Example and(Class<T> clazz, String prefix, T defValues) {
        infos.add(new ConfigInfo<>(clazz, prefix, defValues));
        return this;
    }

    public Example withHeader(String header) {
        this.header = header;
        return this;
    }

    public Example withFooter(String footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Saves generated example of configs to specified print stream. Print stream remains open.
     *
     * @param printStream print stream for generated data
     * @throws IOException
     */
    public void saveTo(PrintStream printStream) throws IOException {
        if (printStream == null) {
            throw new NullPointerException("Print stream can't be null");
        }

        // TODO: implement
    }

    public void saveTo(SupplierEx<PrintStream, IOException> printStreamSupplier) throws IOException {
        if (printStreamSupplier == null) {
            throw new NullPointerException("Supplier can't be null");
        }
        try (PrintStream ps = printStreamSupplier.get()) {
            saveTo(ps);
        }
    }

    public void saveTo(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }
        saveTo(() -> new PrintStream(new FileOutputStream(file)));
    }

    public void saveTo(Path file, OpenOption... options) throws IOException {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }
        saveTo(() -> new PrintStream(Files.newOutputStream(file, options)));
    }

    private static class ConfigInfo<T> {
        private final Class<T> clazz;
        private final String prefix;
        private final T defValues;

        private ConfigInfo(Class<T> clazz, String prefix, T defValues) {
            this.clazz = clazz;
            this.prefix = prefix;
            this.defValues = defValues;
        }
    }
}
