package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.NoPropertyException;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.*;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.converter.IParser;
import org.xblackcat.sjpu.settings.util.ClassUtils;
import org.xblackcat.sjpu.settings.util.IValueGetter;
import org.xblackcat.sjpu.util.function.SupplierEx;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 25.06.2018 11:49
 *
 * @author xBlackCat
 */
public abstract class AConfig {
    private final static Pattern VAR_EXPR = Pattern.compile("\\$\\{([\\w-.]+)}");

    @SuppressWarnings("unchecked")
    private static ArraySetter getArraySetter(Class<?> targetType) throws SettingsException {
        if (Object.class.isAssignableFrom(targetType)) {
            final Function<String, ?> toObjectConverter = getToObjectConverter(targetType);
            return getArraySetter(toObjectConverter);
        } else if (int.class.equals(targetType)) {
            return (o, i, valueStr) -> Array.setInt(o, i, Integer.parseInt(valueStr));
        } else if (long.class.equals(targetType)) {
            return (o, i, valueStr) -> Array.setLong(o, i, Long.parseLong(valueStr));
        } else if (short.class.equals(targetType)) {
            return (o, i, valueStr) -> Array.setShort(o, i, Short.parseShort(valueStr));
        } else if (byte.class.equals(targetType)) {
            return (o, i, valueStr) -> Array.setByte(o, i, Byte.parseByte(valueStr));
        } else if (boolean.class.equals(targetType)) {
            return (o, i, valueStr) -> Array.setBoolean(o, i, BooleanUtils.toBoolean(valueStr));
        } else if (char.class.equals(targetType)) {
            return (o, i, valueStr) -> Array.setChar(o, i, valueStr.toCharArray()[0]);
        } else {
            throw new SettingsException("Unknown type to parse: " + targetType.getName());
        }
    }

    private static ArraySetter getArraySetter(Function<String, ?> toObjectConverter) {
        return (array, index, value) -> Array.set(array, index, toObjectConverter.apply(value));
    }

    @SuppressWarnings({"unchecked"})
    private static Function<String, ?> getToObjectConverter(Class<?> targetType) throws SettingsException {
        if (String.class.equals(targetType)) {
            return valueStr -> valueStr;
        } else if (Integer.class.equals(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : Integer.parseInt(valueStr);
        } else if (int.class.equals(targetType)) {
            return Integer::parseInt;
        } else if (Long.class.equals(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : Long.parseLong(valueStr);
        } else if (long.class.equals(targetType)) {
            return Long::parseLong;
        } else if (Short.class.equals(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : Short.parseShort(valueStr);
        } else if (short.class.equals(targetType)) {
            return Short::parseShort;
        } else if (Byte.class.equals(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : Byte.parseByte(valueStr);
        } else if (byte.class.equals(targetType)) {
            return Byte::parseByte;
        } else if (Boolean.class.equals(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : BooleanUtils.toBoolean(valueStr);
        } else if (boolean.class.equals(targetType)) {
            return BooleanUtils::toBoolean;
        } else if (Character.class.equals(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : valueStr.toCharArray()[0];
        } else if (char.class.equals(targetType)) {
            return valueStr -> valueStr.toCharArray()[0];
        } else if (Enum.class.isAssignableFrom(targetType)) {
            return valueStr -> StringUtils.isBlank(valueStr) ? null : BuilderUtils.searchForEnum((Class<Enum>) targetType, valueStr);
        } else {
            throw new SettingsException("Unknown type to parse: " + targetType.getName());
        }
    }

    protected final Log log = LogFactory.getLog(getClass());
    protected final ClassPool pool;
    protected final Map<String, UnaryOperator<String>> prefixHandlers;
    protected final List<SupplierEx<IValueGetter, SettingsException>> substitutions;

    public AConfig(
            ClassPool pool,
            Map<String, UnaryOperator<String>> prefixHandlers,
            List<SupplierEx<IValueGetter, SettingsException>> substitutions
    ) {
        this.pool = pool;
        this.prefixHandlers = prefixHandlers;
        this.substitutions = substitutions;
    }

    public abstract IValueGetter getValueGetter() throws SettingsException;

    private String getStringValue(IValueGetter properties, String prefixName, Method m) throws SettingsException {
        final Class<?> returnType = m.getReturnType();

        final String propertyName = ClassUtils.buildPropertyName(prefixName, m);

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

        if (StringUtils.isNotBlank(valueStr)) {
            // Substitute variables if exists
            Set<String> invalidVars = new HashSet<>();

            boolean replaced;
            do {
                replaced = false;
                Matcher matcher = VAR_EXPR.matcher(valueStr);

                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                    String varName = matcher.group(1);
                    if (Objects.equals(varName, propertyName)) {
                        throw new SettingsException("Recurrent reference to a property " + propertyName);
                    }
                    if (!invalidVars.contains(varName)) {
                        String substitution = properties.get(varName);
                        if (substitution == null) {
                            substitution = getSubstitution(varName);
                        }
                        if (substitution != null) {
                            matcher.appendReplacement(result, Matcher.quoteReplacement(substitution));
                            replaced = true;
                            continue;
                        }
                    }

                    // Store invalid var
                    invalidVars.add(varName);
                    matcher.appendReplacement(result, Matcher.quoteReplacement("${" + varName + "}"));
                }
                matcher.appendTail(result);

                if (replaced) {
                    valueStr = result.toString();
                }
            } while (replaced);
        }

        if (StringUtils.isNotBlank(valueStr)) {
            // Process prefixed values parsers
            boolean replaced;
            do {
                replaced = false;
                for (Map.Entry<String, UnaryOperator<String>> h: prefixHandlers.entrySet()) {
                    String prefix = h.getKey();
                    if (valueStr.startsWith(prefix)) {
                        valueStr = h.getValue().apply(valueStr.substring(prefix.length()));
                        replaced = true;
                        break;
                    }
                }
            } while (replaced);
        }

        return valueStr;
    }

    private String getSubstitution(String varName) throws SettingsException {
        if (StringUtils.isBlank(varName)) {
            return null;
        }
        for (SupplierEx<IValueGetter, SettingsException> vg: substitutions) {
            String val = vg.get().get(varName);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    protected <T> List<Object> buildConstructorParameters(
            ClassPool pool,
            Class<T> clazz,
            String prefixName,
            IValueGetter properties
    ) throws SettingsException {
        List<Object> values = new ArrayList<>();

        for (Method method: clazz.getMethods()) {
            if (ClassUtils.ignoreMethod(method)) {
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
                    delimiter = ClassUtils.getDelimiter(method);

                    final IParser<?> parser = ClassUtils.getCustomConverter(method);
                    if (parser != null && returnType.isAssignableFrom(parser.getReturnType())) {
                        String valueStr = getStringValue(properties, prefixName, method);

                        if (valueStr != null) {
                            try {
                                value = parser.apply(valueStr);
                            } catch (RuntimeException e) {
                                throw new SettingsException("Can't parse value " + valueStr + " to type " + returnType.getName(), e);
                            }
                        } else {
                            value = null;
                        }
                    } else if (returnType.isArray()) {
                        value = getArrayFieldValue(properties, prefixName, method, delimiter, parser);
                    } else if (Collection.class.isAssignableFrom(returnType)) {
                        value = getCollectionFieldValue(properties, prefixName, method, delimiter, parser);
                    } else if (Map.class.isAssignableFrom(returnType)) {
                        value = getMapFieldValue(properties, prefixName, method, delimiter);
                    } else if (returnType.isInterface()) {
                        final String propertyName = ClassUtils.buildPropertyName(prefixName, method);

                        @SuppressWarnings("unchecked") final Constructor<?> c = ClassUtils.getSettingsConstructor(returnType, pool);

                        value = ClassUtils.initialize(c, buildConstructorParameters(pool, returnType, propertyName, properties));
                    } else {
                        String valueStr = getStringValue(properties, prefixName, method);

                        if (valueStr != null) {
                            try {
                                value = getToObjectConverter(returnType).apply(valueStr);
                            } catch (RuntimeException e) {
                                throw new SettingsException("Can't parse value " + valueStr + " to type " + returnType.getName(), e);
                            }
                        } else {
                            value = null;
                        }
                    }
                }

                values.add(value);
            } catch (NoPropertyException e) {
                if (method.getReturnType().isPrimitive() || method.getAnnotation(Optional.class) == null) {
                    throw e;
                }
                // Optional values could be omitted
                values.add(null);
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private Object getArrayFieldValue(
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

        if (targetType == null) {
            throw new IllegalStateException("Array component type is null? " + returnType.getName());
        }

        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);
        final int arrayLength;
        if (values != null) {
            arrayLength = values.length;
        } else {
            arrayLength = 0;
        }
        Object o = Array.newInstance(targetType, arrayLength);
        if (arrayLength == 0) {
            return o;
        }

        final ArraySetter setter;
        if (parser == null) {
            setter = getArraySetter(targetType);
        } else {
            setter = getArraySetter(parser);
        }

        int i = 0;
        while (i < arrayLength) {
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
    private Object getCollectionFieldValue(
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
        final boolean isSet;
        final boolean isList;
        if (returnRawType.equals(Set.class)) {
            if (values == null || values.length == 0) {
                return Collections.emptySet();
            }

            isList = false;
            isSet = true;

            if (Enum.class.isAssignableFrom(targetType)) {
                collection = EnumSet.noneOf((Class<Enum>) targetType);
            } else {
                collection = new LinkedHashSet<>(values.length);
            }
        } else if (returnRawType.equals(List.class) || returnRawType.equals(Collection.class)) {
            if (values == null || values.length == 0) {
                return Collections.emptyList();
            }

            isList = returnRawType.equals(List.class);
            isSet = false;

            collection = new ArrayList<>(values.length);
        } else {
            throw new SettingsException(
                    "Please, specify container by interface " + Collection.class.getName() + ", " + List.class.getName() + " or " +
                            Set.class.getName() + " as return type for collections."
            );
        }

        if (targetType.isInterface() || java.lang.reflect.Modifier.isAbstract(targetType.getModifiers())) {
            throw new SettingsException("Only non-abstract classes could be specified as collection elements");
        }

        final Function<String, ?> converter;
        if (parser == null) {
            converter = getToObjectConverter(targetType);
        } else {
            converter = parser;
        }

        for (String valueStr: values) {
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
        } else if (isSet) {
            return Collections.unmodifiableSet((Set<?>) collection);
        } else {
            return Collections.unmodifiableCollection((Collection<?>) collection);
        }
    }

    @SuppressWarnings("unchecked")
    private Map getMapFieldValue(
            IValueGetter properties,
            String prefixName,
            Method method,
            String delimiter
    ) throws SettingsException {
        String arrayString = getStringValue(properties, prefixName, method);
        if (arrayString == null) {
            return null;
        }

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


        String[] values = StringUtils.splitByWholeSeparator(arrayString, delimiter);

        if (values == null || values.length == 0) {
            return Collections.emptyMap();
        }

        final Map map;
        if (Enum.class.isAssignableFrom(targetKeyType)) {
            map = new EnumMap(targetKeyType);
        } else {
            map = new LinkedHashMap(values.length);
        }

        Function<String, ?> keyParser = getToObjectConverter(targetKeyType);
        Function<String, ?> valueParser = getToObjectConverter(targetValueType);

        final String splitter = ClassUtils.getSplitter(method);

        for (String part: values) {
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

    private <T> Map<String, T> getGroupFieldValue(
            ClassPool pool,
            Class<T> clazz,
            IValueGetter properties,
            String prefixName,
            Method method
    ) throws SettingsException {
        final Class<?> returnType = method.getReturnType();
        if (!Map.class.equals(returnType)) {
            throw new SettingsException("Group field should have java.util.Map return type only");
        }
        @SuppressWarnings("unchecked") final Constructor<T> c = ClassUtils.getSettingsConstructor(clazz, pool);

        final String propertyName = ClassUtils.buildPropertyName(prefixName, method);
        final String propertyNameDot = propertyName + ".";

        Set<String> propertyNames = properties.keySet().stream().filter(name -> name.startsWith(propertyNameDot)).collect(Collectors.toSet());

        // Search for possible prefixes
        Set<String> prefixes = new HashSet<>();
        for (Method mm: clazz.getMethods()) {
            if (ClassUtils.ignoreMethod(mm)) {
                continue;
            }

            final String suffix = "." + ClassUtils.buildPropertyName(null, mm);

            for (String name: propertyNames) {
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

        for (String p: prefixes) {
            final String realPrefix;
            if (StringUtils.isNotBlank(p)) {
                realPrefix = propertyNameDot + p;
            } else {
                realPrefix = propertyName;
            }

            result.put(p, ClassUtils.initialize(c, buildConstructorParameters(pool, clazz, realPrefix, properties)));
        }

        return Collections.unmodifiableMap(result);
    }

}
