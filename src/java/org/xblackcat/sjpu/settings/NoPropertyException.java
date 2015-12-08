package org.xblackcat.sjpu.settings;

import java.lang.reflect.Method;

/**
 * 11.08.2014 11:50
 *
 * @author xBlackCat
 */
public class NoPropertyException extends SettingsException {
    private final String propertyName;
    private final Method method;

    public NoPropertyException(String propertyName, Method method) {
        super("Property '" + propertyName + "' is not set for method " + method.getDeclaringClass().getName() + "#" + method.getName() +
                      "(). Use @DefaultValue annotation to specify default value for the property");
        this.propertyName = propertyName;
        this.method = method;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Method getMethod() {
        return method;
    }
}
