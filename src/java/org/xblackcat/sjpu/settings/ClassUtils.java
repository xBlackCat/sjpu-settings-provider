package org.xblackcat.sjpu.settings;

import javassist.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 12.02.13 16:40
 *
 * @author xBlackCat
 */
public class ClassUtils {
    private static final Log log = LogFactory.getLog(ClassUtils.class);

    public static final CtClass[] EMPTY_LIST = new CtClass[]{};
    private static final String DEFAULT_DELIMITER = ",";

    public static <T extends Enum<T>> T searchForEnum(Class<T> clazz, String name) throws IllegalArgumentException {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException e) {
            // Try to search case-insensitive
            for (T c : clazz.getEnumConstants()) {
                if (name.equalsIgnoreCase(c.name())) {
                    return c;
                }
            }

            throw e;
        }
    }

    /**
     * Generate a field name by getter method name: trims 'is' or 'get' at the beginning and convert to lower case the first letter.
     *
     * @param mName getter method name
     * @return field name related to the getter.
     */
    public static String makeFieldName(String mName) {
        if (mName.startsWith("get") && mName.length() > 3) {
            final char[] fn = mName.toCharArray();
            fn[3] = Character.toLowerCase(fn[3]);
            return new String(fn, 3, fn.length - 3);
        }

        if (mName.startsWith("is") && mName.length() > 2) {
            final char[] fn = mName.toCharArray();
            fn[2] = Character.toLowerCase(fn[2]);
            return new String(fn, 2, fn.length - 2);
        }

        return mName;
    }

    public static String buildPropertyName(String prefixName, Method m) {
        StringBuilder propertyNameBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(prefixName)) {
            propertyNameBuilder.append(prefixName);
            propertyNameBuilder.append('.');
        }

        final PropertyName field = m.getAnnotation(PropertyName.class);
        if (field != null && StringUtils.isNotBlank(field.value())) {
            propertyNameBuilder.append(field.value());
        } else {
            final String fieldName = makeFieldName(m.getName());
            boolean onHump = true;
            // Generate a property name from field name
            for (char c : fieldName.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    if (!onHump) {
                        propertyNameBuilder.append('.');
                        onHump = true;
                    }
                } else {
                    onHump = false;
                }

                propertyNameBuilder.append(Character.toLowerCase(c));
            }
        }

        return propertyNameBuilder.toString();
    }

    static <T> List<Object> buildConstructorParameters(
            ClassPool pool,
            Class<T> clazz,
            Map<String, String> properties,
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
                final String delimiter;
                Delimiter delimiterAnn = method.getAnnotation(Delimiter.class);
                if (delimiterAnn == null) {
                    delimiter = DEFAULT_DELIMITER;
                } else {
                    delimiter = delimiterAnn.value();
                }

                if (returnType.isArray()) {
                    value = getArrayFieldValue(properties, prefixName, method, delimiter);
                } else if (Collection.class.isAssignableFrom(returnType)) {
                    value = getCollectionFieldValue(properties, prefixName, method, delimiter);
                } else if (Map.class.isAssignableFrom(returnType)) {
                    value = getMapFieldValue(properties, prefixName, method, delimiter);
                } else if (returnType.isInterface()) {
                    final String propertyName = buildPropertyName(prefixName, method);

                    @SuppressWarnings("unchecked") final Constructor<?> c = getSettingsConstructor(returnType, pool);

                    value = initialize(c, buildConstructorParameters(pool, returnType, properties, propertyName));
                } else {
                    String valueStr = getStringValue(properties, prefixName, method);

                    value = ParserUtils.getToObjectConverter(method.getReturnType()).parse(valueStr);
                }
            }

            values.add(value);
        }
        return values;
    }

    static synchronized <T> Constructor<T> getSettingsConstructor(Class<T> clazz, ClassPool pool) throws SettingsException {
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

        // A class with a single constructor has been generated
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        final Constructor<T> constructor = (Constructor<T>) aClass.getConstructors()[0];
        return constructor;
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

            final String fieldName = makeFieldName(mName);

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
                        EMPTY_LIST,
                        EMPTY_LIST,
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
            toStringBody.append(" (");
            toStringBody.append(makeFieldName(fieldName));
            toStringBody.append(") = \\\"\" + java.lang.String.valueOf(this.");
            toStringBody.append(fieldName);
            toStringBody.append(") + \"\\\"; \"");

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
                    EMPTY_LIST,
                    EMPTY_LIST,
                    toStringBody.toString(),
                    settingsClass
            );

            settingsClass.addMethod(toString);

            final CtConstructor constructor = CtNewConstructor.make(
                    constructorParameters.toArray(new CtClass[constructorParameters.size()]),
                    EMPTY_LIST,
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

    static String getStringValue(Map<String, String> properties, String prefixName, Method m) throws SettingsException {
        final Class<?> returnType = m.getReturnType();

        final String propertyName = buildPropertyName(prefixName, m);

        String valueStr = properties.get(propertyName);
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

    @SuppressWarnings("unchecked")
    private static Object getArrayFieldValue(
            Map<String, String> properties,
            String prefixName,
            Method method,
            String delimiter
    ) throws SettingsException {
        final Class<?> returnType = method.getReturnType();
        String arrayString = getStringValue(properties, prefixName, method);

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);
        Class<?> targetType = returnType.getComponentType();
        if (targetType == null) {
            throw new IllegalStateException("Array component type is null? " + returnType.getName());
        }

        Object o = Array.newInstance(targetType, values.length);
        ParserUtils.ArraySetter setter = ParserUtils.getArraySetter(targetType);

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
    private static Object getCollectionFieldValue(
            Map<String, String> properties,
            String prefixName,
            Method method,
            String delimiter
    ) throws SettingsException {
        String arrayString = getStringValue(properties, prefixName, method);
        Class<?> returnType = method.getReturnType();

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);

        final Class<?> targetType;
        final Collection collection;
        ListOf listOf = method.getAnnotation(ListOf.class);
        SetOf setOf = method.getAnnotation(SetOf.class);
        if (listOf != null) {
            if (returnType != List.class) {
                throw new SettingsException("Annotation @ListOf allowed only for java.util.List return type.");
            }
            targetType = listOf.value();
            collection = new ArrayList<>(values.length);
        } else if (setOf != null) {
            if (returnType != Set.class) {
                throw new SettingsException("Annotation @SetOf allowed only for java.util.Set return type.");
            }
            targetType = setOf.value();

            if (Enum.class.isAssignableFrom(targetType)) {
                collection = new LinkedHashSet<>(values.length);
            } else {
                collection = EnumSet.noneOf((Class<Enum>) targetType);
            }
        } else {
            throw new SettingsException("Neither @ListOf nor @SetOf annotations is set for declaring class of collection element.");
        }

        ParserUtils.ValueParser parser = ParserUtils.getToObjectConverter(targetType);

        for (String valueStr : values) {
            try {
                if (valueStr == null) {
                    collection.add(null);
                } else {
                    collection.add(parser.parse(valueStr));
                }
            } catch (RuntimeException e) {
                throw new SettingsException("Can't parse value " + valueStr + " to type " + targetType.getName(), e);
            }
        }

        if (listOf != null) {
            return Collections.unmodifiableList((List<?>) collection);
        } else {
            return Collections.unmodifiableSet((Set<?>) collection);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map getMapFieldValue(
            Map<String, String> properties,
            String prefixName,
            Method method,
            String delimiter
    ) throws SettingsException {
        String arrayString = getStringValue(properties, prefixName, method);

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);

        final Class<?> targetKeyType;
        final Map map;
        MapOf mapOf = method.getAnnotation(MapOf.class);
        if (mapOf != null) {
            targetKeyType = mapOf.key();

            if (Enum.class.isAssignableFrom(targetKeyType)) {
                map = new EnumMap(targetKeyType);
            } else {
                map = new LinkedHashMap(values.length);
            }
        } else {
            throw new SettingsException("@MapOf annotation is not set for declaring target map elements.");
        }

        ParserUtils.ValueParser keyParser = ParserUtils.getToObjectConverter(targetKeyType);
        ParserUtils.ValueParser valueParser = ParserUtils.getToObjectConverter(mapOf.value());

        for (String part : values) {
            String[] parts = StringUtils.splitByWholeSeparator(part, mapOf.splitter(), 2);
            final String keyString;
            final String valueString;
            if (parts.length < 2) {
                keyString = parts[0];
                valueString = null;
            } else {
                keyString = parts[0];
                valueString = parts[1];
            }

            try {
                Object key = keyParser.parse(keyString);

                if (valueString == null) {
                    map.put(key, null);
                } else {
                    map.put(key, valueParser.parse(valueString));
                }
            } catch (RuntimeException e) {
                throw new SettingsException("Can't parse value " + valueString + " to type " + targetKeyType.getName(), e);
            }
        }

        return Collections.unmodifiableMap(map);
    }

    private static <T> Map<String, T> getGroupFieldValue(
            ClassPool pool,
            Class<T> clazz,
            Map<String, String> properties,
            String prefixName,
            Method method
    ) throws SettingsException {
        final Class<?> returnType = method.getReturnType();
        if (Map.class != returnType) {
            throw new SettingsException("Group field should have Map return type");
        }
        @SuppressWarnings("unchecked") final Constructor<T> c = getSettingsConstructor(clazz, pool);

        final String propertyName = buildPropertyName(prefixName, method);
        final String propertyNameDot = propertyName + ".";

        Set<String> propertyNames = new HashSet<>();

        for (String name : properties.keySet()) {
            if (name.startsWith(propertyNameDot)) {
                propertyNames.add(name);
            }
        }

        // Search for possible prefixes
        Set<String> prefixes = new HashSet<>();
        for (Method mm : clazz.getMethods()) {
            final String suffix = "." + buildPropertyName(null, mm);

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

    static <T> boolean allMethodsHaveDefaults(Class<T> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(DefaultValue.class)) {
                return false;
            }
        }

        return true;
    }
}
