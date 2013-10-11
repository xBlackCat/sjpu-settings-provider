package org.xblackcat.sjpu.settings;

import javassist.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
     * Loads settings for specified interface from specified InputStream. Input stream remains open after reading.
     *
     * @param clazz target interface class for holding settings.
     * @param <T>   target interface for holding settings.
     * @return initialized implementation of the specified interface class.
     * @throws SettingsException if interface methods are not annotated or settings can't be read.
     */
    public static <T> T get(Class<T> clazz, InputStream is) throws SettingsException, IOException {
        return loadDefaults(clazz, is);
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

        InputStream is = null;
        try {
            if (!propertiesFile.endsWith(".properties")) {
                try {
                    is = ClassUtils.getInputStream(propertiesFile + ".properties");
                } catch (MissingResourceException e) {
                    // Not found
                }
            }

            if (is == null) {
                // Will throw MissingResourceException
                is = ClassUtils.getInputStream(propertiesFile);
            }

            return loadDefaults(clazz, is);
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

    private static <T> T loadDefaults(Class<T> clazz, InputStream is) throws SettingsException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Load defaults for class " + clazz.getName());
        }

        Properties properties = new Properties();
        properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));

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

    static <T> T initialize(Constructor<T> c, List<Object> values) throws SettingsException {
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

    static <T> List<Object> buildConstructorParameters(
            ClassPool pool,
            Class<T> clazz,
            Properties properties,
            String prefixName
    ) throws SettingsException {
        List<Object> values = new ArrayList<>();

        for (Method m : clazz.getMethods()) {
            final GroupField groupField = m.getAnnotation(GroupField.class);

            final Object value;
            if (groupField != null) {
                value = getGroupFieldValue(
                        pool,
                        groupField.value(),
                        properties,
                        prefixName,
                        m,
                        m.getAnnotation(Optional.class) == null
                );
            } else {
                final Class<?> returnType = m.getReturnType();
                if (returnType.isInterface()) {
                    final String propertyName = ClassUtils.buildPropertyName(prefixName, m);

                    @SuppressWarnings("unchecked") final Constructor<?> c = getSettingsConstructor(returnType, pool);

                    value = initialize(c, buildConstructorParameters(pool, returnType, properties, propertyName));
                } else {
                    value = getSimpleFieldValue(
                            properties,
                            prefixName,
                            m
                    );
                }
            }

            values.add(value);
        }
        return values;
    }

    static <T> Constructor<T> getSettingsConstructor(
            Class<T> clazz,
            ClassPool pool
    ) throws SettingsException {
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
                throw new SettingsException(
                        "Can't initialize a constructor for generated class " + clazz.getName(),
                        ee
                );
            }
        }

        // We generate a class with single constructor
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        final Constructor<T> constructor = (Constructor<T>) aClass.getConstructors()[0];
        return constructor;
    }

    static <T> Map<String, T> getGroupFieldValue(
            ClassPool pool,
            Class<T> clazz,
            Properties properties,
            String prefixName,
            Method m, boolean required
    ) throws SettingsException {
        final Class<?> returnType = m.getReturnType();
        if (Map.class != returnType) {
            throw new SettingsException("Group field should have Map return type");
        }
        @SuppressWarnings("unchecked") final Constructor<T> c = getSettingsConstructor(clazz, pool);

        final String propertyName = ClassUtils.buildPropertyName(prefixName, m);
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

        if (required && !prefixes.contains("")) {
            throw new SettingsException("A default group set is required for method " + m.getName());
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

    static <T> CtClass buildSettingsClass(Class<T> clazz, ClassPool pool) throws SettingsException {
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

    @SuppressWarnings("unchecked")
    static Object getSimpleFieldValue(Properties properties, String prefixName, Method m) throws SettingsException {
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

        final Object value;
        try {
            if (valueStr == null) {
                value = null;
            } else if (String.class == returnType) {
                value = valueStr;
            } else if (Integer.class == returnType || Integer.TYPE == returnType) {
                value = Integer.parseInt(valueStr);
            } else if (Long.class == returnType || Long.TYPE == returnType) {
                value = Long.parseLong(valueStr);
            } else if (Short.class == returnType || Short.TYPE == returnType) {
                value = Short.parseShort(valueStr);
            } else if (Byte.class == returnType || Byte.TYPE == returnType) {
                value = Byte.parseByte(valueStr);
            } else if (Boolean.class == returnType || Boolean.TYPE == returnType) {
                value = BooleanUtils.toBoolean(valueStr);
            } else if (Character.class == returnType || Character.TYPE == returnType) {
                value = valueStr.toCharArray()[0];
            } else if (Enum.class.isAssignableFrom(returnType)) {
                value = ClassUtils.searchForEnum((Class<Enum>) returnType, valueStr);
            } else {
                throw new SettingsException("Unknown type to parse: " + returnType.getName());
            }
        } catch (RuntimeException e) {
            throw new SettingsException("Can't parse value " + valueStr + " to type " + returnType.getName());
        }
        return value;
    }
}
