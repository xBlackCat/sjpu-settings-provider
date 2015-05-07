package org.xblackcat.sjpu.settings.config;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.xblackcat.sjpu.settings.SettingsException;

import java.lang.reflect.Array;
import java.util.function.Function;

/**
 * 03.01.14 15:03
 *
 * @author xBlackCat
 */
final class ParserUtils {
    private ParserUtils() {
    }

    @SuppressWarnings("unchecked")
    static ArraySetter getArraySetter(Class<?> targetType) throws SettingsException {
        if (Object.class.isAssignableFrom(targetType)) {
            final Function<String, Object> toObjectConverter = getToObjectConverter(targetType);
            return (array, index, value) -> Array.set(array, index, toObjectConverter.apply(value));
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

    @SuppressWarnings({"unchecked"})
    static Function<String, Object> getToObjectConverter(Class<?> targetType) throws SettingsException {
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
            return valueStr -> StringUtils.isBlank(valueStr) ? null : ClassUtils.searchForEnum((Class<Enum>) targetType, valueStr);
        } else {
            throw new SettingsException("Unknown type to parse: " + targetType.getName());
        }
    }

    interface ArraySetter {
        void set(Object array, int index, String value);
    }
}
