package org.xblackcat.sjpu.settings.util;

import javassist.*;
import javassist.Modifier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.builder.BuilderUtils;
import org.xblackcat.sjpu.settings.NotImplementedException;
import org.xblackcat.sjpu.settings.NotLoadedException;
import org.xblackcat.sjpu.settings.SettingsException;
import org.xblackcat.sjpu.settings.ann.*;
import org.xblackcat.sjpu.settings.ann.Optional;
import org.xblackcat.sjpu.settings.config.ISettingsWrapper;
import org.xblackcat.sjpu.settings.converter.IParser;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 12.02.13 16:40
 *
 * @author xBlackCat
 */
public class ClassUtils {
    private static final Log log = LogFactory.getLog(ClassUtils.class);

    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_SPLITTER = ":";
    private static final String NOT_LOADED_EXCEPTION_CLASS = BuilderUtils.getName(NotLoadedException.class);

    public static String buildPropertyName(Method m) {
        return buildPropertyName(null, m);
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

    public static IParser<?> getCustomConverter(Method method) throws SettingsException {
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

    public static String getDelimiter(Method method) {
        Delimiter delimiterAnn = method.getAnnotation(Delimiter.class);

        if (delimiterAnn == null) {
            return DEFAULT_DELIMITER;
        } else {
            return delimiterAnn.value();
        }
    }

    public static String getSplitter(Method method) {
        Splitter splitterAnn = method.getAnnotation(Splitter.class);

        if (splitterAnn == null) {
            return DEFAULT_SPLITTER;
        } else {
            return splitterAnn.value();
        }
    }

    public static synchronized <T> Constructor<T> getSettingsConstructor(Class<T> clazz, ClassPool pool) throws SettingsException {
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
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"}) final Constructor<T> constructor = (Constructor<T>) aClass.getConstructors()[0];
        return constructor;
    }

    public static synchronized <T> Constructor<ISettingsWrapper<T>> getSettingsWrapperConstructor(
            Class<T> clazz,
            ClassPool pool
    ) throws SettingsException {
        final String implName = clazz.getName() + "$Wrapper";
        Class<?> aClass;
        try {
            aClass = Class.forName(implName, true, pool.getClassLoader());
        } catch (ClassNotFoundException e) {
            try {
                CtClass settingsClass = buildSettingsWrapperClass(clazz, pool);
                aClass = settingsClass.toClass();
                settingsClass.detach();
            } catch (CannotCompileException ee) {
                throw new SettingsException("Can't initialize a constructor for generated class " + clazz.getName(), ee);
            }
        }

        // A class with a single constructor has been generated
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"}) final Constructor<ISettingsWrapper<T>> constructor = (Constructor<ISettingsWrapper<T>>) aClass.getConstructors()[0];
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

        List<String> fieldNames = new ArrayList<>();

        StringBuilder toStringBody = new StringBuilder();
        StringBuilder equalsBody = new StringBuilder();

        StringBuilder constructorBody = new StringBuilder();
        List<CtClass> constructorParameters = new ArrayList<>();

        constructorBody.append("{\n");

        toStringBody.append("{\nreturn \"");
        toStringBody.append(clazz.getSimpleName());
        toStringBody.append(" [\"");

        final String className = settingsClass.getName();
        equalsBody.append(
                "{\n" +
                        "if (this == $1) return true;\n" +
                        "if ($1 == null || getClass() != $1.getClass()) return false;\n" +
                        "final "
        );
        equalsBody.append(className);
        equalsBody.append(" that = (");
        equalsBody.append(className);
        equalsBody.append(") $1;\n return true");

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
                addIgnoredImplementation(pool, settingsClass, m, mName, returnType);
                continue;
            }

            if (m.getParameterTypes().length > 0) {
                throw new SettingsException("Method " + m.toString() + " has parameters - can't be processed as getter");
            }

            String fieldName = BuilderUtils.makeFieldName(mName);

            if (log.isTraceEnabled()) {
                log.trace("Generate a property " + fieldName + " for class " + clazz.getName() + " of type " + returnType.getName());
            }

            final CtClass retType;
            try {
                retType = pool.get(returnType.getName());
            } catch (NotFoundException e) {
                throw new SettingsException("Somehow a class " + returnType.getName() + " can't be found", e);
            }

            final boolean returnTypeArray = returnType.isArray();
            try {
                CtField f = new CtField(retType, "__" + fieldName, settingsClass);
                f.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
                settingsClass.addField(f);

                final String body;
                if (returnTypeArray) {
                    body = "{ return ($r) this.__" + fieldName + ".clone()" + "; }";
                } else {
                    body = "{ return this.__" + fieldName + "; }";
                }

                if (log.isTraceEnabled()) {
                    log.trace("Implement method " + clazz.getName() + "#" + mName + "() " + body);
                }

                final CtMethod getter = CtNewMethod.make(
                        Modifier.FINAL | Modifier.PUBLIC,
                        retType,
                        mName,
                        BuilderUtils.EMPTY_LIST,
                        BuilderUtils.EMPTY_LIST,
                        body,
                        settingsClass
                );
                settingsClass.addMethod(getter);
            } catch (CannotCompileException e) {
                throw new SettingsException("Can't add a field __" + fieldName + " to generated class", e);
            }

            if (returnTypeArray) {
                constructorBody.append("if ($");
                constructorBody.append(idx);
                constructorBody.append(" != null) {\n");
                constructorBody.append("this.__");
                constructorBody.append(fieldName);
                constructorBody.append(" = (");
                constructorBody.append(BuilderUtils.getName(returnType));
                constructorBody.append(")$");
                constructorBody.append(idx);
                constructorBody.append(".clone();\n");
                constructorBody.append("} else {\n");
                constructorBody.append("this.__");
                constructorBody.append(fieldName);
                constructorBody.append(" = null;\n}\n");
            } else {
                constructorBody.append("this.__");
                constructorBody.append(fieldName);
                constructorBody.append(" = $");
                constructorBody.append(idx);
                constructorBody.append(";\n");
            }

            equalsBody.append(" &&\n");
            if (returnTypeArray) {
                equalsBody.append("java.util.Arrays.equals(__");
                equalsBody.append(fieldName);
                equalsBody.append(", that.__");
                equalsBody.append(fieldName);
                equalsBody.append(")");
            } else if (returnType.isPrimitive() || returnType.isEnum()) {
                equalsBody.append("__");
                equalsBody.append(fieldName);
                equalsBody.append(" == that.__");
                equalsBody.append(fieldName);
                equalsBody.append("");
            } else {
                equalsBody.append("java.util.Objects.equals(__");
                equalsBody.append(fieldName);
                equalsBody.append(", that.__");
                equalsBody.append(fieldName);
                equalsBody.append(")");
            }
            fieldNames.add("($w) __" + fieldName);

            toStringBody.append(" + \"");
            toStringBody.append(fieldName);
            toStringBody.append(" (");
            toStringBody.append(buildPropertyName(null, m));
            toStringBody.append(") = \\\"\" + java.lang.String.valueOf(this.__");
            toStringBody.append(fieldName);
            toStringBody.append(") + \"\\\"; \"");

            constructorParameters.add(retType);

            idx++;
        }

        if (idx == 1) {
            throw new SettingsException("Can't load settings to a class without properties. Class " + className);
        }

        constructorBody.append("}");
        toStringBody.setLength(toStringBody.length() - 3);
        toStringBody.append("]\";\n}");
        equalsBody.append(";\n}");

        try {
            if (log.isTraceEnabled()) {
                log.trace("Generated method " + clazz.getName() + "#equals() " + equalsBody.toString());
            }

            final CtMethod equals = CtNewMethod.make(
                    Modifier.FINAL | Modifier.PUBLIC,
                    pool.get(boolean.class.getName()),
                    "equals",
                    pool.get(new String[]{"java.lang.Object"}),
                    BuilderUtils.EMPTY_LIST,
                    equalsBody.toString(),
                    settingsClass
            );

            settingsClass.addMethod(equals);

            String hashCodeBody = "{\n" +
                    "return java.util.Objects.hash(new java.lang.Object[]{\n" +
                    String.join(",\n", fieldNames) +
                    "\n});\n}";
            if (log.isTraceEnabled()) {
                log.trace("Generated method " + clazz.getName() + "#hashCode() " + hashCodeBody);
            }

            final CtMethod hashCode = CtNewMethod.make(
                    Modifier.FINAL | Modifier.PUBLIC,
                    pool.get(int.class.getName()),
                    "hashCode",
                    BuilderUtils.EMPTY_LIST,
                    BuilderUtils.EMPTY_LIST,
                    hashCodeBody,
                    settingsClass
            );

            settingsClass.addMethod(hashCode);

            if (log.isTraceEnabled()) {
                log.trace("Generated method " + clazz.getName() + "#toString() " + toStringBody.toString());
            }

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

            if (log.isTraceEnabled()) {
                log.trace("Generated constructor " + clazz.getName() + "() " + constructorBody.toString());
            }

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

    private static <T> CtClass buildSettingsWrapperClass(Class<T> clazz, ClassPool pool) throws SettingsException {
        if (!clazz.isInterface()) {
            throw new SettingsException("Only annotated interfaces are supported. " + clazz.getName() + " is a class.");
        }

        final CtClass settingsClass;
        final CtClass settingsInterface;
        try {
            settingsInterface = pool.get(clazz.getName());
            settingsClass = settingsInterface.makeNestedClass("Wrapper", true);
            settingsClass.addInterface(settingsInterface);
            settingsClass.addInterface(BuilderUtils.toCtClass(pool, ISettingsWrapper.class));
        } catch (NotFoundException e) {
            throw new SettingsException("Can't generate class for settings", e);
        }

        try {
            {
                CtField f = new CtField(pool.get(ReadWriteLock.class.getName()), "__lock", settingsClass);
                f.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
                settingsClass.addField(f);
            }

            {
                CtField f = new CtField(settingsInterface, "__config", settingsClass);
                f.setModifiers(Modifier.PRIVATE | Modifier.VOLATILE);
                settingsClass.addField(f);
            }
        } catch (CannotCompileException | NotFoundException e) {
            throw new SettingsException("Can't initialize fields in class for settings", e);
        }

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
                addIgnoredImplementation(pool, settingsClass, m, mName, returnType);
                continue;
            }

            if (m.getParameterTypes().length > 0) {
                throw new SettingsException("Method " + m.toString() + " has parameters - can't be processed as getter");
            }

            final CtClass retType;
            try {
                retType = pool.get(returnType.getName());
            } catch (NotFoundException e) {
                throw new SettingsException("Somehow a class " + returnType.getName() + " can't be found", e);
            }

            try {
                final String body = "{\n" +
                        "this.__lock.readLock().lock();\n" +
                        "try {\n" +
                        "if (this.__config == null) {\n" +
                        "throw new " + NOT_LOADED_EXCEPTION_CLASS + "(\"Optional config " + clazz.getName() + " is not loaded\");\n" +
                        "}\n" +
                        "return ($r) this.__config." + mName + "()" + ";\n" +
                        "} finally {\n" +
                        "this.__lock.readLock().unlock();\n" +
                        "}\n" +
                        "}";

                if (log.isTraceEnabled()) {
                    log.trace("Implement method " + clazz.getName() + "#" + mName + "() " + body);
                }

                final CtMethod proxy = CtNewMethod.make(
                        Modifier.FINAL | Modifier.PUBLIC,
                        retType,
                        mName,
                        BuilderUtils.EMPTY_LIST,
                        BuilderUtils.EMPTY_LIST,
                        body,
                        settingsClass
                );
                settingsClass.addMethod(proxy);
            } catch (CannotCompileException e) {
                throw new SettingsException("Can't add a proxy method " + mName + " to generated class", e);
            }
        }


        String toStringBody = "{\n" +
                "this.__lock.readLock().lock();\n" +
                "try {\n" +
                "return \"" + clazz.getSimpleName() + " wrapper of \" + String.valueOf(this.__config);\n" +
                "} finally {\n" +
                "this.__lock.readLock().unlock();\n" +
                "}\n" +
                "}";
        String constructorBody = "{\n" +
                "this.__config = $1;\n" +
                "this.__lock = new " + BuilderUtils.getName(ReentrantReadWriteLock.class) + "();\n" +
                "}";
        String getterBody = "{\n" +
                "this.__lock.readLock().lock();\n" +
                "try {\n" +
                "return ($r) this.__config;\n" +
                "} finally {\n" +
                "this.__lock.readLock().unlock();\n" +
                "}\n" +
                "}";
        String setterBody = "{\n" +
                "this.__lock.writeLock().lock();\n" +
                "try {\n" +
                "this.__config = $1;\n" +
                "} finally {\n" +
                "this.__lock.writeLock().unlock();\n" +
                "}\n" +
                "}";

        try {
            if (log.isTraceEnabled()) {
                log.trace("Generated setter " + clazz.getName() + "#setConfig() " + setterBody);
            }

            final CtMethod setter = CtNewMethod.make(
                    Modifier.FINAL | Modifier.PUBLIC,
                    pool.get(void.class.getName()),
                    "setConfig",
                    new CtClass[]{pool.get(Object.class.getName())},
                    BuilderUtils.EMPTY_LIST,
                    setterBody,
                    settingsClass
            );

            settingsClass.addMethod(setter);

            if (log.isTraceEnabled()) {
                log.trace("Generated getter " + clazz.getName() + "#getConfig() " + getterBody);
            }

            final CtMethod getter = CtNewMethod.make(
                    Modifier.FINAL | Modifier.PUBLIC,
                    pool.get(Object.class.getName()),
                    "getConfig",
                    BuilderUtils.EMPTY_LIST,
                    BuilderUtils.EMPTY_LIST,
                    getterBody,
                    settingsClass
            );

            settingsClass.addMethod(getter);

            if (log.isTraceEnabled()) {
                log.trace("Generated method " + clazz.getName() + "#toString() " + toStringBody);
            }

            final CtMethod toString = CtNewMethod.make(
                    Modifier.FINAL | Modifier.PUBLIC,
                    pool.get(String.class.getName()),
                    "toString",
                    BuilderUtils.EMPTY_LIST,
                    BuilderUtils.EMPTY_LIST,
                    toStringBody,
                    settingsClass
            );

            settingsClass.addMethod(toString);

            if (log.isTraceEnabled()) {
                log.trace("Generated constructor " + clazz.getName() + "() " + constructorBody);
            }

            final CtConstructor constructor = CtNewConstructor.make(
                    new CtClass[]{settingsInterface},
                    BuilderUtils.EMPTY_LIST,
                    constructorBody,
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

    private static void addIgnoredImplementation(
            ClassPool pool,
            CtClass settingsClass,
            Method m,
            String mName,
            Class<?> returnType
    ) throws SettingsException {
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
    }

    public static <T> T initialize(Constructor<T> c, List<Object> values) throws SettingsException {
        final Object[] array = values.toArray();
        return initialize(c, array);
    }

    public static <T> T initialize(Constructor<T> c, Object... values) throws SettingsException {
        try {
            return c.newInstance(values);
        } catch (InstantiationException e) {
            throw new SettingsException("Can't make a new instance of my own class :(", e);
        } catch (IllegalAccessException e) {
            throw new SettingsException("Can't get access to my own class :(", e);
        } catch (InvocationTargetException e) {
            throw new SettingsException("My class produces an exception :(", e);
        }
    }

    public static <T> boolean allMethodsHaveDefaults(Class<T> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (ignoreMethod(m)) {
                continue;
            }
            if (!m.isAnnotationPresent(DefaultValue.class) && !m.isAnnotationPresent(Optional.class)) {
                return false;
            }
        }

        return true;
    }

    public static boolean ignoreMethod(Method method) {
        return method.isDefault() || method.isAnnotationPresent(Ignore.class);
    }
}
