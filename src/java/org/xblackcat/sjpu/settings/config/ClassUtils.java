package org.xblackcat.sjpu.settings.config;

import javassist.*;
import javassist.Modifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.NoPropertyException;
import org.xblackcat.sjpu.settings.NotImplementedException;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.*;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.converter.IParser;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 12.02.13 16:40
 *
 * @author xBlackCat
 */
public class ClassUtils {
    private static final Log log = LogFactory.getLog(ClassUtils.class);

    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_SPLITTER = ":";

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
            final String fieldName = BuilderUtils.makeFieldName(m.getName());
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
            IValueGetter properties,
            String prefixName
    ) throws SettingsException {
        List<Object> values = new ArrayList<>();

        for (Method method : clazz.getMethods()) {
            if (ignoreMethod(method)) {
                continue;
            }

            final GroupField groupField = method.getAnnotation(GroupField.class);

            try {
                final Object value;
                if (groupField != null) {
                    value = getGroupFieldValue(pool, groupField.value(), properties, prefixName, method);
                } else {
                    final Class<?> returnType = method.getReturnType();
                    final String delimiter;
                    delimiter = getDelimiter(method);

                    final IParser<?> parser = getCustomConverter(method);
                    if (parser != null && returnType.isAssignableFrom(parser.getReturnType())) {
                        String valueStr = getStringValue(properties, prefixName, method);

                        try {
                            value = parser.apply(valueStr);
                        } catch (RuntimeException e) {
                            throw new SettingsException("Can't parse value " + valueStr + " to type " + returnType.getName(), e);
                        }
                    } else if (returnType.isArray()) {
                        value = getArrayFieldValue(properties, prefixName, method, delimiter, parser);
                    } else if (Collection.class.isAssignableFrom(returnType)) {
                        value = getCollectionFieldValue(properties, prefixName, method, delimiter, parser);
                    } else if (Map.class.isAssignableFrom(returnType)) {
                        value = getMapFieldValue(properties, prefixName, method, delimiter);
                    } else if (returnType.isInterface()) {
                        final String propertyName = buildPropertyName(prefixName, method);

                        @SuppressWarnings("unchecked") final Constructor<?> c = getSettingsConstructor(returnType, pool);

                        value = initialize(c, buildConstructorParameters(pool, returnType, properties, propertyName));
                    } else {
                        String valueStr = getStringValue(properties, prefixName, method);

                        try {
                            value = ParserUtils.getToObjectConverter(returnType).apply(valueStr);
                        } catch (RuntimeException e) {
                            throw new SettingsException("Can't parse value " + valueStr + " to type " + returnType.getName(), e);
                        }
                    }
                }

                values.add(value);
            } catch (NoPropertyException e) {
                if (method.getReturnType().isPrimitive() || method.getAnnotation(org.xblackcat.sjpu.settings.ann.Optional.class) == null) {
                    throw e;
                }
                // Optional values could be omitted
                values.add(null);
            }
        }
        return values;
    }

    private static IParser<?> getCustomConverter(Method method) throws SettingsException {
        final ParseWith parseWith = method.getAnnotation(ParseWith.class);
        if (parseWith == null) {
            return null;
        }

        final IParser<?> parser;
        final Class<? extends IParser<?>> aClass = parseWith.value();
        try {
            // Check for default constructor
            aClass.getConstructor();
            parser = aClass.newInstance();
        } catch (InstantiationException e) {
            throw new SettingsException("Failed to instantiate converter class " + aClass, e);
        } catch (IllegalAccessException e) {
            throw new SettingsException("Failed to initialize converter class " + aClass, e);
        } catch (NoSuchMethodException e) {
            throw new SettingsException("Converter class " + aClass + " should have default public constructor", e);
        }
        return parser;
    }

    private static String getDelimiter(Method method) {
        Delimiter delimiterAnn = method.getAnnotation(Delimiter.class);

        if (delimiterAnn == null) {
            return DEFAULT_DELIMITER;
        } else {
            return delimiterAnn.value();
        }
    }

    private static String getSplitter(Method method) {
        Splitter splitterAnn = method.getAnnotation(Splitter.class);

        if (splitterAnn == null) {
            return DEFAULT_SPLITTER;
        } else {
            return splitterAnn.value();
        }
    }

    static synchronized <T> Constructor<T> getSettingsConstructor(Class<T> clazz, ClassPool pool) throws SettingsException {
        final String implName = clazz.getName() + "$Impl";
        Class<?> aClass;
        try {
            aClass = Class.forName(implName, true, pool.getClassLoader());
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
            final Class<?> returnType = m.getReturnType();

            if (m.isDefault()) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignore default method " + m + " in interface " + clazz.getName());
                }
                continue;
            }

            if (m.isAnnotationPresent(Ignore.class)) {
                final CtClass retType;
                try {
                    retType = pool.get(returnType.getName());
                } catch (NotFoundException e) {
                    throw new SettingsException("Somehow a class " + returnType.getName() + " can't be found", e);
                }

                try {
                    final CtMethod dumbMethod = CtNewMethod.make(
                            Modifier.FINAL | Modifier.PUBLIC,
                            retType,
                            mName,
                            BuilderUtils.toCtClasses(pool, m.getParameterTypes()),
                            BuilderUtils.toCtClasses(pool, m.getExceptionTypes()),
                            "{ throw new " + NotImplementedException.class.getName() + "(\"Method " + mName +
                                    " is excluded from generation\"); }",
                            settingsClass
                    );
                    settingsClass.addMethod(dumbMethod);
                } catch (NotFoundException | CannotCompileException e) {
                    throw new SettingsException("Can't add a dumb method " + mName + " to generated class", e);
                }
                continue;
            }

            if (m.getParameterTypes().length > 0) {
                throw new SettingsException("Method " + m.toString() + " has parameters - can't be processed as getter");
            }

            final String fieldName = BuilderUtils.makeFieldName(mName);

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
                f.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
                settingsClass.addField(f);

                final CtMethod getter = CtNewMethod.make(
                        Modifier.FINAL | Modifier.PUBLIC,
                        retType,
                        mName,
                        BuilderUtils.EMPTY_LIST,
                        BuilderUtils.EMPTY_LIST,
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
            toStringBody.append(buildPropertyName(null, m));
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
                    BuilderUtils.EMPTY_LIST,
                    BuilderUtils.EMPTY_LIST,
                    toStringBody.toString(),
                    settingsClass
            );

            settingsClass.addMethod(toString);

            final CtConstructor constructor = CtNewConstructor.make(
                    constructorParameters.toArray(new CtClass[constructorParameters.size()]),
                    BuilderUtils.EMPTY_LIST,
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

    static String getStringValue(IValueGetter properties, String prefixName, Method m) throws SettingsException {
        final Class<?> returnType = m.getReturnType();

        final String propertyName = buildPropertyName(prefixName, m);

        String valueStr = properties.get(propertyName);
        if (log.isTraceEnabled()) {
            log.trace("Property " + propertyName + " for method " + m.getName() + " is " + valueStr);
        }

        if (valueStr == null) {
            // Check for default value

            final DefaultValue field = m.getAnnotation(DefaultValue.class);

            final boolean optional = m.isAnnotationPresent(Optional.class);
            final String defValue = field == null ? null : field.value();
            if (StringUtils.isEmpty(defValue)) {
                if (returnType.isPrimitive() || !optional && defValue == null) {
                    throw new NoPropertyException(propertyName, m);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Using default value " + defValue + " for property " + propertyName);
                }
            }
            valueStr = defValue;
        }

        return valueStr;
    }

    @SuppressWarnings("unchecked")
    private static Object getArrayFieldValue(
            IValueGetter properties,
            String prefixName,
            Method method,
            String delimiter,
            IParser<?> parser
    ) throws SettingsException {
        final Class<?> returnType = method.getReturnType();
        Class<?> targetType = returnType.getComponentType();
        if (parser != null) {
            if (!targetType.isAssignableFrom(parser.getReturnType())) {
                throw new SettingsException(
                        "Converter return type " + parser.getReturnType().getName() + " can't be assigned to array component type" +
                                returnType.getName()
                );
            }
        }

        String arrayString = getStringValue(properties, prefixName, method);

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);
        if (targetType == null) {
            throw new IllegalStateException("Array component type is null? " + returnType.getName());
        }

        Object o = Array.newInstance(targetType, values.length);
        final ParserUtils.ArraySetter setter;
        if (parser == null) {
            setter = ParserUtils.getArraySetter(targetType);
        } else {
            setter = ParserUtils.getArraySetter(parser);
        }

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
            IValueGetter properties,
            String prefixName,
            Method method,
            String delimiter,
            IParser<?> parser
    ) throws SettingsException {
        String arrayString = getStringValue(properties, prefixName, method);
        if (arrayString == null) {
            return null;
        }
        final Class<?> returnRawType;
        final Class<?> proposalReturnClass;
        if (method.getGenericReturnType() instanceof ParameterizedType) {
            final ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
            if (!(returnType.getRawType() instanceof Class)) {
                throw new SettingsException("Raw type is not a class " + returnType + " in method " + method.toString());
            }
            returnRawType = (Class) returnType.getRawType();
            proposalReturnClass = BuilderUtils.detectTypeArgClass(returnType);
        } else {
            returnRawType = (Class<?>) method.getGenericReturnType();
            proposalReturnClass = null;
        }

        final Class<?> targetType;
        CollectionOf collectionOf = method.getAnnotation(CollectionOf.class);
        if (collectionOf != null) {
            targetType = collectionOf.value();
        } else {
            targetType = proposalReturnClass;
        }

        if (proposalReturnClass != null) {
            if (!targetType.isAssignableFrom(proposalReturnClass)) {
                throw new SettingsException(
                        "Specified return object " + targetType.getName() + " cannot be casted to " + proposalReturnClass.getName()
                );
            }
        }

        if (targetType == null) {
            throw new SettingsException(
                    "Cannot detect component type of list. Please, use @CollectionOf annotation for method " + method.toString()
            );
        }

        if (parser != null) {
            if (!targetType.isAssignableFrom(parser.getReturnType())) {
                throw new SettingsException(
                        "Converter return type " + parser.getReturnType().getName() + " can't be assigned to array component type" +
                                targetType.getName() + " for method " + method.getName()
                );
            }
        }

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);
        final Collection collection;
        final boolean isList;
        if (returnRawType.equals(Set.class)) {
            isList = false;

            if (Enum.class.isAssignableFrom(targetType)) {
                collection = EnumSet.noneOf((Class<Enum>) targetType);
            } else {
                collection = new LinkedHashSet<>(values.length);
            }
        } else if (returnRawType.equals(List.class) || returnRawType.equals(List.class)) {
            isList = true;

            collection = new ArrayList<>(values.length);
        } else {
            throw new SettingsException(
                    "Please, specify container by interface " + Collection.class.getName() + ", " + List.class.getName() + " or "
                            + Set.class.getName() + " as return type for collections."
            );
        }

        if (targetType.isInterface() || java.lang.reflect.Modifier.isAbstract(targetType.getModifiers())) {
            throw new SettingsException("Only non-abstract classes could be specified as collection elements");
        }

        final Function<String, ?> converter;
        if (parser == null) {
            converter = ParserUtils.getToObjectConverter(targetType);
        } else {
            converter = parser;
        }

        for (String valueStr : values) {
            try {
                if (valueStr == null) {
                    collection.add(null);
                } else {
                    collection.add(converter.apply(valueStr));
                }
            } catch (RuntimeException e) {
                throw new SettingsException("Can't parse value " + valueStr + " to type " + targetType.getName(), e);
            }
        }

        if (isList) {
            return Collections.unmodifiableList((List<?>) collection);
        } else {
            return Collections.unmodifiableSet((Set<?>) collection);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map getMapFieldValue(
            IValueGetter properties,
            String prefixName,
            Method method,
            String delimiter
    ) throws SettingsException {
        String arrayString = getStringValue(properties, prefixName, method);
        if (arrayString == null) {
            return null;
        }

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);

        final Class<?> returnRawType;
        final Class<?> proposalKeyClass;
        final Class<?> proposalValueClass;
        if (method.getGenericReturnType() instanceof ParameterizedType) {
            final ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
            if (!(returnType.getRawType() instanceof Class)) {
                throw new SettingsException("Raw type is not a class " + returnType + " in method " + method.toString());
            }
            returnRawType = (Class) returnType.getRawType();
            Class<?>[] detectTypeArgsClass = BuilderUtils.detectTypeArgsClass(returnType, 2);
            proposalKeyClass = detectTypeArgsClass[0];
            proposalValueClass = detectTypeArgsClass[1];
        } else {
            returnRawType = (Class<?>) method.getGenericReturnType();
            proposalKeyClass = null;
            proposalValueClass = null;
        }

        if (!Map.class.equals(returnRawType)) {
            throw new SettingsException("Please, specify general interface for maps as return type for method " + method.toString());
        }

        final Class<?> targetKeyType;
        MapKey mapKey = method.getAnnotation(MapKey.class);
        if (mapKey != null) {
            targetKeyType = mapKey.value();
        } else {
            targetKeyType = proposalKeyClass;
        }

        if (proposalKeyClass != null) {
            if (!targetKeyType.isAssignableFrom(proposalKeyClass)) {
                throw new SettingsException(
                        "Specified return object " + targetKeyType.getName() + " cannot be casted to " + proposalKeyClass.getName()
                );
            }
        }

        if (targetKeyType == null) {
            throw new SettingsException(
                    "Cannot detect key component type of map. Please, use @MapKey annotation for method " + method.toString()
            );
        }

        final Class<?> targetValueType;
        MapValue collectionOf = method.getAnnotation(MapValue.class);
        if (collectionOf != null) {
            targetValueType = collectionOf.value();
        } else {
            targetValueType = proposalValueClass;
        }

        if (proposalValueClass != null) {
            if (!targetValueType.isAssignableFrom(proposalValueClass)) {
                throw new SettingsException(
                        "Specified return object " + targetValueType.getName() + " cannot be casted to " + proposalValueClass.getName()
                );
            }
        }

        if (targetValueType == null) {
            throw new SettingsException(
                    "Cannot detect value component type of map. Please, use @MapValue annotation for method " + method.toString()
            );
        }


        final Map map;
        if (Enum.class.isAssignableFrom(targetKeyType)) {
            map = new EnumMap(targetKeyType);
        } else {
            map = new LinkedHashMap(values.length);
        }

        Function<String, ?> keyParser = ParserUtils.getToObjectConverter(targetKeyType);
        Function<String, ?> valueParser = ParserUtils.getToObjectConverter(targetValueType);

        final String splitter = getSplitter(method);

        for (String part : values) {
            String[] parts = StringUtils.splitByWholeSeparator(part, splitter, 2);
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
                Object key = keyParser.apply(keyString);

                if (valueString == null) {
                    map.put(key, null);
                } else {
                    map.put(key, valueParser.apply(valueString));
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
            IValueGetter properties,
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

        Set<String> propertyNames = properties.keySet().stream().filter(name -> name.startsWith(propertyNameDot)).collect(Collectors.toSet());

        // Search for possible prefixes
        Set<String> prefixes = new HashSet<>();
        for (Method mm : clazz.getMethods()) {
            if (ignoreMethod(mm)) {
                continue;
            }

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

        boolean required = method.getAnnotation(org.xblackcat.sjpu.settings.ann.Optional.class) == null;
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
            final Object[] array = values.toArray(new Object[values.size()]);
            return c.newInstance(array);
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
            if (ignoreMethod(m)) {
                continue;
            }
            if (!m.isAnnotationPresent(DefaultValue.class)) {
                return false;
            }
        }

        return true;
    }

    static boolean ignoreMethod(Method method) {
        return method.isDefault() || method.isAnnotationPresent(Ignore.class);
    }
}
