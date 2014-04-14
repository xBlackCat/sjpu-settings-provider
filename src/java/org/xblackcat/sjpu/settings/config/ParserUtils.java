package org.xblackcat.sjpu.settings.config;

import org.apache.commons.lang3.BooleanUtils;
import org.xblackcat.sjpu.settings.SettingsException;

import java.lang.reflect.Array;

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
        } else if (int.class == targetType) {
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
        } else if (long.class == targetType) {
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
        } else if (short.class == targetType) {
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
        } else if (byte.class == targetType) {
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
        } else if (boolean.class == targetType) {
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
        } else if (char.class == targetType) {
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

    @SuppressWarnings({"unchecked"})
    static ValueParser getToObjectConverter(Class<?> targetType) throws SettingsException {
        if (String.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return valueStr;
                }
            };
        } else if (Integer.class == targetType || int.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return Integer.parseInt(valueStr);
                }
            };
        } else if (Long.class == targetType || long.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return Long.parseLong(valueStr);
                }
            };
        } else if (Short.class == targetType || short.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return Short.parseShort(valueStr);
                }
            };
        } else if (Byte.class == targetType || byte.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return Byte.parseByte(valueStr);
                }
            };
        } else if (Boolean.class == targetType || boolean.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return BooleanUtils.toBoolean(valueStr);
                }
            };
        } else if (Character.class == targetType || char.class == targetType) {
            return new ValueParser() {
                public Object parse(String valueStr) {
                    return valueStr.toCharArray()[0];
                }
            };
        } else if (Enum.class.isAssignableFrom(targetType)) {
            return new EnumValueParser((Class<Enum>) targetType);
        } else {
            throw new SettingsException("Unknown type to parse: " + targetType.getName());
        }
    }

    static interface ValueParser {
        Object parse(String valueStr);
    }

    static interface ArraySetter {
        void set(Object array, int index, String value);
    }

    private static class EnumValueParser implements ValueParser {
        private final Class<Enum> targetType;

        private EnumValueParser(Class<Enum> targetType) {
            this.targetType = targetType;
        }

        @SuppressWarnings({"unchecked"})
        public Object parse(String valueStr) {
            return ClassUtils.searchForEnum(targetType, valueStr);
        }
    }

    private static class EnumArraySetter implements ArraySetter {
        private final Class<Enum> targetType;

        private EnumArraySetter(Class<Enum> targetType) {
            this.targetType = targetType;
        }

        @SuppressWarnings({"unchecked"})
        public void set(Object o, int i, String valueStr) {
            Array.set(o, i, ClassUtils.searchForEnum(targetType, valueStr));
        }
    }
}
