package org.xblackcat.sjpu.settings;

import javassist.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 10.01.13 11:37
 *
 * @author xBlackCat
 */
public final class SettingsProvider {
    private static final Log log = LogFactory.getLog(SettingsProvider.class);

    /**
     * Loads settings for specified interface from specified file.
     *
     * @param clazz target interface class for holding settings.
     * @param file  source input file (properties file)
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, File file) throws SettingsException, IOException {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            return loadDefaults(clazz, properties);
        }
    }

    /**
     * Loads settings for specified interface from specified resource specified by URI.
     *
     * @param clazz target interface class for holding settings.
     * @param uri   Uri to .properties file
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws org.xblackcat.sjpu.settings.SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, URI uri) throws SettingsException, IOException {
        return get(clazz, uri.toURL());
    }

    /**
     * Loads settings for specified interface from specified resource specified by URL.
     *
     * @param clazz target interface class for holding settings.
     * @param url   Url to .properties file
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, URL url) throws SettingsException, IOException {
        try (InputStream is = new BufferedInputStream(url.openStream())) {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            return loadDefaults(clazz, properties);
        }
    }

    /**
     * Loads settings for specified interface from specified InputStream. Input stream remains open after reading.
     *
     * @param clazz target interface class for holding settings.
     * @param is    source input stream (properties file)
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, InputStream is) throws SettingsException, IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));

        return loadDefaults(clazz, properties);
    }

    /**
     * Loads settings for specified interface. A default location of resource file is used. Default location is specified
     * by {@linkplain org.xblackcat.sjpu.settings.SettingsSource @SettingsSource} annotation.
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or interface is not annotated with
     *                           {@linkplain org.xblackcat.sjpu.settings.SettingsSource @SettingsSource}
     */
    public static <T> T get(Class<T> clazz) throws SettingsException {
        final SettingsSource sourceAnn = clazz.getAnnotation(SettingsSource.class);

        if (sourceAnn == null) {
            throw new SettingsException(
                    "No default source is specified for " +
                            clazz.getName() +
                            ". Should be annotated with @SettingsSource annotation"
            );
        }

        final String propertiesFile = sourceAnn.value();
        if (log.isDebugEnabled()) {
            log.debug("Load properites from " + propertiesFile);
        }

        return get(clazz, propertiesFile);
    }

    /**
     * Loads settings for specified interface from specified resource in class path.
     *
     * @param clazz        target interface class for holding settings.
     * @param resourceName resource name (properties file)
     * @param <T>          target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, String resourceName) throws SettingsException {
        InputStream is = null;
        try {
            if (!resourceName.endsWith(".properties")) {
                is = getInputStream(resourceName + ".properties");
            }

            if (is == null) {
                is = getInputStream(resourceName);
                if (is == null) {
                    throw new MissingResourceException(
                            "Can not find resource " + resourceName, SettingsProvider.class.getName(), resourceName
                    );
                }

            }

            Properties properties = new Properties();
            properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));

            return loadDefaults(clazz, properties);
        } catch (IOException e) {
            throw new SettingsException("Can't load values for " + clazz.getName(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.warn("Can't close stream. [" + clazz.getName() + "]", e);
                }
            }
        }
    }

    private static <T> T loadDefaults(Class<T> clazz, Properties properties) throws SettingsException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Load defaults for class " + clazz.getName());
        }

        final String prefixName;
        final ClassPool pool = ClassPool.getDefault();
        final Prefix prefixAnn = clazz.getAnnotation(Prefix.class);
        if (prefixAnn != null) {
            prefixName = prefixAnn.value();

            if (log.isDebugEnabled()) {
                log.debug("Prefix for property names is '" + prefixName + "'");
            }
        } else {
            prefixName = "";
        }

        @SuppressWarnings("unchecked") final Constructor<T> c = getSettingsConstructor(clazz, pool);

        List<Object> values = buildConstructorParameters(pool, clazz, properties, prefixName);

        return initialize(c, values);
    }

    private static <T> T initialize(Constructor<T> c, List<Object> values) throws SettingsException {
        try {
            return c.newInstance(values.toArray(new Object[values.size()]));
        } catch (InstantiationException e) {
            throw new SettingsException("Can't make a new instance of my own class :(", e);
        } catch (IllegalAccessException e) {
            throw new SettingsException("Can't get access to my own class :(", e);
        } catch (InvocationTargetException e) {
            throw new SettingsException("My class produces an exception :(", e);
        }
    }

    private static <T> List<Object> buildConstructorParameters(
            ClassPool pool,
            Class<T> clazz,
            Properties properties,
            String prefixName
    ) throws SettingsException {
        List<Object> values = new ArrayList<>();

        for (Method method : clazz.getMethods()) {
            final GroupField groupField = method.getAnnotation(GroupField.class);

            final Object value;
            if (groupField != null) {
                value = getGroupFieldValue(pool, groupField.value(), properties, prefixName, method);
            } else {
                final Class<?> returnType = method.getReturnType();
                if (returnType.isInterface()) {
                    final String propertyName = ClassUtils.buildPropertyName(prefixName, method);

                    @SuppressWarnings("unchecked") final Constructor<?> c = getSettingsConstructor(returnType, pool);

                    value = initialize(c, buildConstructorParameters(pool, returnType, properties, propertyName));
                } else if (returnType.isArray()) {
                    value = getArrayFieldValue(properties, prefixName, method, ",");
                } else {
                    String valueStr = getStringValue(properties, prefixName, method);

                    value = convertToObject(method.getReturnType(), valueStr);
                }
            }

            values.add(value);
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static Object getArrayFieldValue(
            Properties properties,
            String prefixName,
            Method method,
            String splitter
    ) throws SettingsException {
        final Class<?> returnType = method.getReturnType();
        String arrayString = getStringValue(properties, prefixName, method);

        String[] values = StringUtils.splitByWholeSeparator(arrayString, splitter);
        Class<?> targetType = returnType.getComponentType();
        if (targetType == null) {
            throw new IllegalStateException("Array component type is null? " + returnType.getName());
        }

        Object o = Array.newInstance(targetType, values.length);
        ArraySetter setter = getArraySetter(targetType);

        int i = 0;
        while (i < values.length) {
            String valueStr = values[i];
            try {
                if (valueStr == null) {
                    Array.set(o, i, null);
                } else {
                    setter.set(o, i, valueStr);
                }
            } catch (RuntimeException e) {
                throw new SettingsException("Can't parse value " + valueStr + " to type " + targetType.getName(), e);
            }

            i++;
        }

        return o;
    }

    @SuppressWarnings("unchecked")
    private static ArraySetter getArraySetter(Class<?> targetType) throws SettingsException {
        if (String.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, valueStr);
                }
            };
        } else if (Integer.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, Integer.parseInt(valueStr));
                }
            };
        } else if (Integer.TYPE == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.setInt(o, i, Integer.parseInt(valueStr));
                }
            };
        } else if (Long.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, Long.parseLong(valueStr));
                }
            };
        } else if (Long.TYPE == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.setLong(o, i, Long.parseLong(valueStr));
                }
            };
        } else if (Short.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, Short.parseShort(valueStr));
                }
            };
        } else if (Short.TYPE == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.setShort(o, i, Short.parseShort(valueStr));
                }
            };
        } else if (Byte.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, Byte.parseByte(valueStr));
                }
            };
        } else if (Byte.TYPE == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.setByte(o, i, Byte.parseByte(valueStr));
                }
            };
        } else if (Boolean.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, BooleanUtils.toBoolean(valueStr));
                }
            };
        } else if (Boolean.TYPE == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.setBoolean(o, i, BooleanUtils.toBoolean(valueStr));
                }
            };
        } else if (Character.class == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.set(o, i, valueStr.toCharArray()[0]);
                }
            };
        } else if (Character.TYPE == targetType) {
            return new ArraySetter() {
                public void set(Object o, int i, String valueStr) {
                    Array.setChar(o, i, valueStr.toCharArray()[0]);
                }
            };
        } else if (Enum.class.isAssignableFrom(targetType)) {
            return new EnumArraySetter((Class<Enum>) targetType);
        } else {
            throw new SettingsException("Unknown type to parse: " + targetType.getName());
        }
    }

    private static <T> Constructor<T> getSettingsConstructor(Class<T> clazz, ClassPool pool) throws SettingsException {
        final String implName = clazz.getName() + "$Impl";
        Class<?> aClass;
        try {
            aClass = Class.forName(implName);
        } catch (ClassNotFoundException e) {
            try {
                CtClass settingsClass = buildSettingsClass(clazz, pool);
                aClass = settingsClass.toClass();
                settingsClass.detach();
            } catch (CannotCompileException ee) {
                throw new SettingsException("Can't initialize a constructor for generated class " + clazz.getName(), ee);
            }
        }

        // We generate a class with single constructor
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        final Constructor<T> constructor = (Constructor<T>) aClass.getConstructors()[0];
        return constructor;
    }

    private static <T> Map<String, T> getGroupFieldValue(
            ClassPool pool,
            Class<T> clazz,
            Properties properties,
            String prefixName,
            Method method
    ) throws SettingsException {
        final Class<?> returnType = method.getReturnType();
        if (Map.class != returnType) {
            throw new SettingsException("Group field should have Map return type");
        }
        @SuppressWarnings("unchecked") final Constructor<T> c = getSettingsConstructor(clazz, pool);

        final String propertyName = ClassUtils.buildPropertyName(prefixName, method);
        final String propertyNameDot = propertyName + ".";

        Set<String> propertyNames = new HashSet<>();

        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(propertyNameDot)) {
                propertyNames.add(name);
            }
        }

        // Search for possible prefixes
        Set<String> prefixes = new HashSet<>();
        for (Method mm : clazz.getMethods()) {
            final String suffix = "." + ClassUtils.buildPropertyName(null, mm);

            for (String name : propertyNames) {
                if (name.endsWith(suffix)) {
                    final String prefix;
                    final int prefixLen = propertyNameDot.length();
                    final int cutLen = name.length() - suffix.length();
                    if (prefixLen >= cutLen) {
                        prefix = "";
                    } else {
                        prefix = name.substring(prefixLen, cutLen);
                    }

                    prefixes.add(prefix);
                }
            }
        }

        boolean required = method.getAnnotation(Optional.class) == null;
        if (required && !prefixes.contains("")) {
            throw new SettingsException("A default group set is required for method " + method.getName());
        }

        Map<String, T> result = new HashMap<>();

        for (String p : prefixes) {
            final String realPrefix;
            if (StringUtils.isNotBlank(p)) {
                realPrefix = propertyNameDot + p;
            } else {
                realPrefix = propertyName;
            }

            result.put(p, initialize(c, buildConstructorParameters(pool, clazz, properties, realPrefix)));
        }

        return Collections.unmodifiableMap(result);
    }

    private static <T> CtClass buildSettingsClass(Class<T> clazz, ClassPool pool) throws SettingsException {
        if (!clazz.isInterface()) {
            throw new SettingsException("Only annotated interfaces are supported. " + clazz.getName() + " is a class.");
        }

        final CtClass settingsClass;
        try {
            final CtClass settingsInterface = pool.get(clazz.getName());
            settingsClass = settingsInterface.makeNestedClass("Impl", true);
            settingsClass.addInterface(settingsInterface);
        } catch (NotFoundException e) {
            throw new SettingsException("Can't generate class for settings", e);
        }

        StringBuilder toStringBody = new StringBuilder();
        StringBuilder constructorBody = new StringBuilder();
        List<CtClass> constructorParameters = new ArrayList<>();
        constructorBody.append("{\n");
        toStringBody.append("{\nreturn \"");
        toStringBody.append(clazz.getSimpleName());
        toStringBody.append(" [\"");

        int idx = 1;
        for (Method m : clazz.getMethods()) {
            final String mName = m.getName();

            if (m.getParameterTypes().length > 0) {
                throw new SettingsException(
                        "Method " +
                                m.toString() +
                                " has parameters - can't be processed as getter"
                );
            }

            final Class<?> returnType = m.getReturnType();

            final String fieldName = ClassUtils.makeFieldName(mName);

            if (log.isTraceEnabled()) {
                log.trace("Generate a field " + fieldName + " for class " + clazz.getName());
            }

            final CtClass retType;
            try {
                retType = pool.get(returnType.getName());
            } catch (NotFoundException e) {
                throw new SettingsException("Somehow a class " + returnType.getName() + " can't be found", e);
            }

            try {
                CtField f = new CtField(retType, fieldName, settingsClass);
                f.setModifiers(Modifier.FINAL);
                f.setModifiers(Modifier.PRIVATE);
                settingsClass.addField(f);

                final CtMethod getter = CtNewMethod.make(
                        Modifier.FINAL | Modifier.PUBLIC,
                        retType,
                        mName,
                        ClassUtils.EMPTY_LIST,
                        ClassUtils.EMPTY_LIST,
                        "{ return this." + fieldName + "; }",
                        settingsClass
                );
                settingsClass.addMethod(getter);
            } catch (CannotCompileException e) {
                throw new SettingsException("Can't add a field " + fieldName + " to generated class", e);
            }

            constructorBody.append("this.");
            constructorBody.append(fieldName);
            constructorBody.append(" = $");
            constructorBody.append(idx);
            constructorBody.append(";\n");

            toStringBody.append(" + \"");
            toStringBody.append(fieldName);
            toStringBody.append("=\\\"\" + this.");
            toStringBody.append(fieldName);
            toStringBody.append(" + \"\\\"; \"");

            constructorParameters.add(retType);

            idx++;
        }

        constructorBody.append("}");
        toStringBody.setLength(toStringBody.length() - 3);
        toStringBody.append("]\";\n}");

        try {
            final CtMethod toString = CtNewMethod.make(
                    Modifier.FINAL | Modifier.PUBLIC,
                    pool.get(String.class.getName()),
                    "toString",
                    ClassUtils.EMPTY_LIST,
                    ClassUtils.EMPTY_LIST,
                    toStringBody.toString(),
                    settingsClass
            );

            settingsClass.addMethod(toString);

            final CtConstructor constructor = CtNewConstructor.make(
                    constructorParameters.toArray(new CtClass[constructorParameters.size()]),
                    ClassUtils.EMPTY_LIST,
                    constructorBody.toString(),
                    settingsClass
            );

            settingsClass.addConstructor(constructor);

            return settingsClass;
        } catch (CannotCompileException e) {
            throw new SettingsException("Can't generate a constructor for generated class " + clazz.getName(), e);
        } catch (NotFoundException e) {
            throw new SettingsException("Can't generate toString() method for generated class " + clazz.getName(), e);
        }
    }

    private static String getStringValue(Properties properties, String prefixName, Method m) throws SettingsException {
        final Class<?> returnType = m.getReturnType();

        final String propertyName = ClassUtils.buildPropertyName(prefixName, m);

        String valueStr = properties.getProperty(propertyName);
        if (log.isTraceEnabled()) {
            log.trace("Property " + propertyName + " for method " + m.getName() + " is " + valueStr);
        }

        if (valueStr == null) {
            // Check for default value
            final boolean required = m.getAnnotation(Optional.class) == null;

            if (required) {
                final DefaultValue field = m.getAnnotation(DefaultValue.class);

                if (field == null) {
                    // Default value is not defined
                    throw new SettingsException("Property " + propertyName + " is not set for method " + m.getName());
                }

                final String defValue = field.value();
                if ("".equals(defValue)) {
                    throw new SettingsException("Property " + propertyName + " is not set for method " + m.getName());
                }

                if (log.isTraceEnabled()) {
                    log.trace("Using default value " + defValue + " for property " + propertyName);
                }

                valueStr = defValue;
            }
        }

        if (returnType.isPrimitive() && (valueStr == null || valueStr.length() == 0)) {
            throw new SettingsException(
                    "Default value should be set for primitive type with @SettingField annotation for method " +
                            m.getName()
            );
        }
        return valueStr;
    }

    private static Object convertToObject(Class<?> targetType, String valueStr) throws SettingsException {
        final Object value;
        try {
            if (valueStr == null) {
                value = null;
            } else if (String.class == targetType) {
                value = valueStr;
            } else if (Integer.class == targetType || Integer.TYPE == targetType) {
                value = Integer.parseInt(valueStr);
            } else if (Long.class == targetType || Long.TYPE == targetType) {
                value = Long.parseLong(valueStr);
            } else if (Short.class == targetType || Short.TYPE == targetType) {
                value = Short.parseShort(valueStr);
            } else if (Byte.class == targetType || Byte.TYPE == targetType) {
                value = Byte.parseByte(valueStr);
            } else if (Boolean.class == targetType || Boolean.TYPE == targetType) {
                value = BooleanUtils.toBoolean(valueStr);
            } else if (Character.class == targetType || Character.TYPE == targetType) {
                value = valueStr.toCharArray()[0];
            } else if (Enum.class.isAssignableFrom(targetType)) {
                value = ClassUtils.searchForEnum((Class<Enum>) targetType, valueStr);
            } else {
                throw new SettingsException("Unknown type to parse: " + targetType.getName());
            }
        } catch (RuntimeException e) {
            throw new SettingsException("Can't parse value " + valueStr + " to type " + targetType.getName(), e);
        }
        return value;
    }

    static InputStream getInputStream(String propertiesFile) throws IOException {
        InputStream is = SettingsProvider.class.getResourceAsStream(propertiesFile);
        if (is == null) {
            is = SettingsProvider.class.getClassLoader().getResourceAsStream(propertiesFile);
        }

        return is;
    }

    private static interface ArraySetter {
        void set(Object array, int index, String value);
    }

    private static class EnumArraySetter implements ArraySetter {
        private final Class<Enum> targetType;

        private EnumArraySetter(Class<Enum> targetType) {
            this.targetType = targetType;
        }

        public void set(Object o, int i, String valueStr) {
            Array.set(o, i, ClassUtils.searchForEnum(targetType, valueStr));
        }
    }
}
