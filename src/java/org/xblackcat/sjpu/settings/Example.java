package org.xblackcat.sjpu.settings;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.ann.*;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.config.ConfigInfo;
import org.xblackcat.sjpu.settings.converter.IParser;
import org.xblackcat.sjpu.settings.util.ClassUtils;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.*;
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

    private final List<ConfigInfo<?>> infoList = new ArrayList<>();
    private final Map<String, String> substitutionValues = new HashMap<>();
    private String header;
    private String footer;
    private boolean debugInfo;
    private boolean brief;
    private boolean pure;
    private boolean sortByName = true;

    private Example() {
    }

    public <T> Example and(Class<T> clazz) {
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        return and(clazz, prefixAnn != null ? prefixAnn.value() : "");
    }

    public <T> Example and(Class<T> clazz, String prefix) {
        infoList.add(new ConfigInfo<>(clazz, prefix));
        return this;
    }

    /**
     * Do not sort properties by name - use declaring methods order in an interface
     *
     * @return current {@linkplain Example} object reference to allow do chaining requests
     */
    public Example declaringOrder() {
        this.sortByName = false;
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

    public Example withDefaults(Map<String, String> defaults) {
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
     * @throws IOException       thrown if generated config can't be saved
     * @throws SettingsException thrown if config can't be generated
     */
    public void writeTo(PrintStream printStream) throws IOException, SettingsException {
        if (printStream == null) {
            throw new NullPointerException("Print stream can't be null");
        }

        // Print header
        if (!pure && printDescription(printStream, header)) {
            printStream.println();
        }

        final ConfigInfo[] configInfos;
        if (sortByName) {
            configInfos = infoList.stream().sorted(Comparator.comparing(ConfigInfo::getPrefix)).toArray(ConfigInfo[]::new);
        } else {
            configInfos = infoList.toArray(new ConfigInfo[infoList.size()]);
        }
        for (ConfigInfo<?> ci : configInfos) {
            if (!brief) {
                printStream.println("######");
            }
            printClass(printStream, ci.getPrefix(), ci.getClazz(), false);
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
        final Collection<Method> methods;
        if (sortByName) {
            TreeMap<String, Method> sortedMethods = new TreeMap<>(String::compareTo);
            for (Method m : clazz.getMethods()) {
                sortedMethods.put(ClassUtils.buildPropertyName(m), m);
            }
            methods = sortedMethods.values();
        } else {
            methods = Arrays.asList(clazz.getMethods());
        }
        for (Method m : methods) {
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

            final Enum<?>[] keyConstants = resolveKeyConstants(m);
            final Enum<?>[] valueConstants = resolveValueConstants(m);

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
                        printDescription(printStream, "Value format: ", parser.formatDescription());
                    }
                    if (showDelimiterInfo) {
                        printStream.println("# Values delimiter: '" + ClassUtils.getDelimiter(m) + "'");
                    }
                    if (showSplitterInfo) {
                        printStream.println("# Key-value separator: '" + ClassUtils.getSplitter(m) + "'");
                        if (keyConstants != null) {
                            printStream.println("# Valid keys for the property are: " + Arrays.toString(keyConstants));
                        }
                    }
                    if (valueConstants != null) {
                        printStream.println("# Valid values for the property are: " + Arrays.toString(valueConstants));
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

    private Enum<?>[] resolveValueConstants(Method m) {
        final Type grt = m.getGenericReturnType();
        final Class<?> rt = m.getReturnType();

        final Class<?> valueType;
        if (rt.isArray()) {
            valueType = rt.getComponentType();
        } else if (Collection.class.isAssignableFrom(rt)) {
            if (!(grt instanceof ParameterizedType)) {
                return null;
            }

            ParameterizedType pt = (ParameterizedType) grt;
            final Type[] ata = pt.getActualTypeArguments();
            if (ata.length != 1) {
                // Support only plain collections with only one type parameter
                return null;
            }

            final Map<TypeVariable<?>, Class<?>> typeVariables = BuilderUtils.resolveTypeVariables(grt);
            valueType = BuilderUtils.substituteTypeVariables(typeVariables, ata[0]);
        } else if (Map.class.equals(rt)) {
            if (!(grt instanceof ParameterizedType)) {
                return null;
            }

            ParameterizedType pt = (ParameterizedType) grt;
            final Type[] ata = pt.getActualTypeArguments();
            if (ata.length != 2) {
                // Support only standart maps with two type parameters
                return null;
            }

            final Map<TypeVariable<?>, Class<?>> typeVariables = BuilderUtils.resolveTypeVariables(grt);
            valueType = BuilderUtils.substituteTypeVariables(typeVariables, ata[1]);
        } else {
            valueType = rt;
        }

        if (valueType.isEnum()) {
            @SuppressWarnings("unchecked") final Class<Enum<?>> enumClass = (Class<Enum<?>>) valueType;
            return enumClass.getEnumConstants();
        }
        return null;
    }

    private Enum<?>[] resolveKeyConstants(Method m) {
        final Type grt = m.getGenericReturnType();
        final Class<?> rt = m.getReturnType();

        final Class<?> valueType;
        if (!Map.class.equals(rt)) {
            return null;
        }

        if (!(grt instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType pt = (ParameterizedType) grt;
        final Type[] ata = pt.getActualTypeArguments();
        if (ata.length != 2) {
            // Support only standart maps with two type parameters
            return null;
        }

        final Map<TypeVariable<?>, Class<?>> typeVariables = BuilderUtils.resolveTypeVariables(grt);
        valueType = BuilderUtils.substituteTypeVariables(typeVariables, ata[0]);
        if (valueType.isEnum()) {
            @SuppressWarnings("unchecked") final Class<Enum<?>> enumClass = (Class<Enum<?>>) valueType;
            return enumClass.getEnumConstants();
        }
        return null;
    }

    private String getDescription(AnnotatedElement e) {
        final Description annotation = e.getAnnotation(Description.class);
        return annotation == null ? null : annotation.value();
    }

    private static boolean printDescription(PrintStream printStream, String text) {
        return printDescription(printStream, "", text);
    }

    private static boolean printDescription(PrintStream printStream, String prefix, String text) {
        String padding = null;
        if (StringUtils.isNotBlank(text)) {
            boolean first = true;
            for (String line : StringUtils.split(text, "\n\r")) {
                printStream.print("# ");
                if (first) {
                    printStream.print(prefix);
                    first = false;
                } else {
                    if (padding == null) {
                        padding = StringUtils.repeat(' ', prefix.length());
                    }
                    printStream.print(padding);
                }
                printStream.println(line);
            }
            return true;
        }

        return false;
    }
}
