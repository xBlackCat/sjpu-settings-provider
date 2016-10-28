package org.xblackcat.sjpu.settings;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.ann.*;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.converter.IParser;
import org.xblackcat.sjpu.settings.util.ClassUtils;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 18.06.2016 19:54
 *
 * @author xBlackCat
 */
public class Example {
    private static final Log log = LogFactory.getLog(Example.class);

    public static Example of(Class<?> clazz) {
        final Example config = new Example();
        config.and(clazz);
        return config;
    }

    public static Example of(Class<?> clazz, String prefix) {
        final Example config = new Example();
        config.and(clazz, prefix);
        return config;
    }

    private final List<ConfigInfo<?>> infos = new ArrayList<>();
    private final Map<String, String> substitutionValues = new HashMap<>();
    private String header;
    private String footer;
    private boolean debugInfo;
    private boolean brief;
    private boolean pure;

    private Example() {
    }

    public <T> Example and(Class<T> clazz) {
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        return and(clazz, prefixAnn != null ? prefixAnn.value() : "");
    }

    public <T> Example and(Class<T> clazz, String prefix) {
        infos.add(new ConfigInfo<>(clazz, prefix));
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

    public Example withDefault(String key, String value) {
        substitutionValues.put(key, value);
        return this;
    }

    public Example withDefault(Map<String, String> defaults) {
        substitutionValues.putAll(defaults);
        return this;
    }

    /**
     * Put detailed info for each property to the result with references to target methods
     *
     * @return this {@linkplain Example} instance
     */
    public Example withDebugInfo() {
        debugInfo = true;
        brief = false;
        pure = false;
        return this;
    }

    /**
     * Do not generate comments and descriptions to properties except footer, header and default values
     *
     * @return this {@linkplain Example} instance
     */
    public Example brief() {
        debugInfo = false;
        brief = true;
        pure = false;
        return this;
    }

    /**
     * Avoid any kind of comments
     *
     * @return this {@linkplain Example} instance
     */
    public Example pure() {
        debugInfo = false;
        brief = true;
        pure = true;
        return this;
    }

    public void writeTo(SupplierEx<PrintStream, IOException> printStreamSupplier) throws IOException, SettingsException {
        if (printStreamSupplier == null) {
            throw new NullPointerException("Supplier can't be null");
        }
        try (PrintStream ps = printStreamSupplier.get()) {
            writeTo(ps);
        }
    }

    public void writeTo(File file) throws IOException, SettingsException {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }
        writeTo(() -> new PrintStream(new FileOutputStream(file)));
    }

    public void saveToFile(String fileName, OpenOption... options) throws IOException, SettingsException {
        if (fileName == null) {
            throw new NullPointerException("File name can't be null");
        }
        writeTo(Paths.get(fileName), options);
    }

    public void writeTo(Path file, OpenOption... options) throws IOException, SettingsException {
        if (file == null) {
            throw new NullPointerException("File can't be null");
        }
        writeTo(() -> new PrintStream(Files.newOutputStream(file, options)));
    }

    /**
     * Saves generated example of configs to specified print stream. Print stream remains open.
     *
     * @param printStream print stream for generated data
     * @throws IOException
     */
    public void writeTo(PrintStream printStream) throws IOException, SettingsException {
        if (printStream == null) {
            throw new NullPointerException("Print stream can't be null");
        }

        // Print header
        if (!pure && printDescription(printStream, header)) {
            printStream.println();
        }

        for (ConfigInfo<?> ci : infos) {
            if (!brief) {
                printStream.println("######");
            }
            printClass(printStream, ci.prefix, ci.clazz, false);
        }

        if (!pure) {
            printStream.println();
            if (printDescription(printStream, footer)) {
                printStream.println();
            }
        }
    }

    private void printClass(PrintStream printStream, String prefix, Class<?> clazz, boolean classIsOptional) throws SettingsException {
        if (!brief) {
            printDescription(printStream, getDescription(clazz));
        }

        if (debugInfo) {
            printStream.print("# (i) Interface ");
            printStream.print(BuilderUtils.getName(clazz));
            if (StringUtils.isNotBlank(prefix)) {
                printStream.print(" with prefix ");
                printStream.print(prefix);
            }
            printStream.println();
            printStream.println("###");
            printStream.println("#");
        }

        printMethods(printStream, clazz, prefix, classIsOptional);
    }

    private void printMethods(PrintStream printStream, Class<?> clazz, String prefix, boolean classIsOptional) throws SettingsException {
        for (Method m : clazz.getMethods()) {
            printMethod(printStream, clazz, prefix, m, classIsOptional);
        }
    }

    private void printMethod(
            PrintStream printStream,
            Class<?> clazz,
            String prefix,
            Method m,
            boolean classIsOptional
    ) throws SettingsException {
        if (m.isDefault()) {
            if (log.isTraceEnabled()) {
                log.trace("Ignore default method " + m + " in interface " + clazz.getName());
            }
            return;
        }
        if (m.isAnnotationPresent(Ignore.class)) {
            if (log.isTraceEnabled()) {
                log.trace("Method " + m + " is ignored by annotation in interface " + clazz.getName());
            }
            return;
        }

        if (m.getParameterTypes().length > 0) {
            throw new SettingsException("Method " + m.toString() + " has parameters - can't be processed as getter");
        }
        if (!brief) {
            printDescription(printStream, getDescription(m));
        }
        final boolean optional = m.isAnnotationPresent(Optional.class);

        final GroupField groupField = m.getAnnotation(GroupField.class);
        final String propertyName = ClassUtils.buildPropertyName(prefix, m);
        final DefaultValue annotation = m.getAnnotation(DefaultValue.class);
        final boolean hasDefault = annotation != null;
        final String defaultValue = hasDefault ? annotation.value() : null;

        if (groupField != null) {
            if (!brief) {
                printStream.print("#### ");
                printStream.print(propertyName);
                printStream.println(" group begin ####");
            }
            final Class<?> groupClass = groupField.value();
            if (!optional) {
                printClass(printStream, propertyName, groupClass, false);
            }
            printClass(printStream, propertyName + "[.<set name>]", groupClass, true);
            if (!brief) {
                printStream.print("#### ");
                printStream.print(propertyName);
                printStream.println(" group  end  ####");
            }
        } else {
            final Class<?> returnType = m.getReturnType();
            final boolean showSplitterInfo = Map.class.equals(returnType);
            final boolean showDelimiterInfo = returnType.isArray() || Collection.class.isAssignableFrom(returnType) || showSplitterInfo;

            final IParser<?> parser = ClassUtils.getCustomConverter(m);
            final boolean customObject = parser != null && returnType.isAssignableFrom(parser.getReturnType());
            if (!customObject && returnType.isInterface() && !showSplitterInfo && !showDelimiterInfo) {
                if (!brief) {
                    printStream.print("#### ");
                    printStream.print(propertyName);
                    printStream.println(" group begin ####");
                }
                printClass(printStream, propertyName, returnType, optional);
                if (!brief) {
                    printStream.print("#### ");
                    printStream.print(propertyName);
                    printStream.println(" group  end  ####");
                }
            } else {
                final boolean usedDefaultValue = !substitutionValues.containsKey(propertyName);
                final String exampleValue = substitutionValues.getOrDefault(propertyName, defaultValue);
                if (!brief) {
                    if (parser != null) {
                        printDescription(printStream, "Value format: " + parser.formatDescription());
                    }
                    if (showDelimiterInfo) {
                        printStream.println("# Values delimiter: '" + ClassUtils.getDelimiter(m) + "'");
                    }
                    if (showSplitterInfo) {
                        printStream.println("# Key-value separator: '" + ClassUtils.getSplitter(m) + "'");
                    }

                    if (debugInfo) {
                        printStream.print("# (i) Method ");
                        printStream.print(BuilderUtils.getName(clazz));
                        printStream.print("#");
                        printStream.print(m.getName());
                        printStream.println("()");
                        printStream.print("# (i) Java value type: ");
                        printStream.print(BuilderUtils.getName(m.getGenericReturnType()));
                        printStream.println();
                    }
                    if (!pure && (optional || classIsOptional)) {
                        printStream.println("# (Optional)");
                    }
                }

                if (!brief && hasDefault) {
                    printStream.print("# Default value: ");
                    printStream.println(defaultValue);
                }
                final boolean showDefault = optional || classIsOptional || hasDefault && usedDefaultValue;
                if (pure && showDefault) {
                    return;
                }
                if (showDefault) {
                    printStream.print('!');
                }
                printStream.print(propertyName);
                printStream.print('=');
                if (exampleValue != null) {
                    printStream.print(exampleValue);
                }
                printStream.println();
                if (!brief) {
                    printStream.println();
                }
            }
        }
    }

    private String getDescription(AnnotatedElement e) {
        final Description annotation = e.getAnnotation(Description.class);
        return annotation == null ? null : annotation.value();
    }

    private static boolean printDescription(PrintStream printStream, String text) {
        if (StringUtils.isNotBlank(text)) {
            for (String line : StringUtils.split(text, "\n\r")) {
                printStream.print("# ");
                printStream.println(line);
            }
            return true;
        }

        return false;
    }

    private static class ConfigInfo<T> {
        private final Class<T> clazz;
        private final String prefix;

        private ConfigInfo(Class<T> clazz, String prefix) {
            this.clazz = clazz;
            this.prefix = prefix;
        }
    }
}
