package org.xblackcat.sjpu.settings;

import javassist.CtClass;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.MissingResourceException;

/**
 * 12.02.13 16:40
 *
 * @author xBlackCat
 */
public class ClassUtils {
    public static final CtClass[] EMPTY_LIST = new CtClass[]{};

    public static <T extends Enum<T>> T searchForEnum(Class<T> clazz, String name) throws IllegalArgumentException {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException e) {
            // Try to search case-insensetive
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
        final String fieldName;
        if (mName.startsWith("get") && mName.length() > 3) {
            final char[] fn = mName.toCharArray();
            fn[3] = Character.toLowerCase(fn[3]);
            fieldName = new String(fn, 3, fn.length - 3);
        } else if (mName.startsWith("is") && mName.length() > 2) {
            final char[] fn = mName.toCharArray();
            fn[2] = Character.toLowerCase(fn[2]);
            fieldName = new String(fn, 2, fn.length - 2);
        } else {
            fieldName = mName;
        }
        return fieldName;
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

    public static InputStream getInputStream(String propertiesFile) throws IOException {
        URL result;
        URL url = SettingsProvider.class.getResource(propertiesFile);
        if (url == null) {
            url = SettingsProvider.class.getClassLoader().getResource(propertiesFile);
        }
        if (url == null) {
            throw new MissingResourceException(
                    "Can not find resource " + propertiesFile,
                    SettingsProvider.class.getName(),
                    propertiesFile
            );
        } else {
            result = url;
        }
        return result.openStream();
    }
}
