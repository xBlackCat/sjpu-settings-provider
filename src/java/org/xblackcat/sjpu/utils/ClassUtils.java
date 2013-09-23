package org.xblackcat.sjpu.utils;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * 12.02.13 16:40
 *
 * @author xBlackCat
 */
public class ClassUtils {
    public static final CtClass[] EMPTY_LIST = new CtClass[]{};

    /**
     * Convert array of Class objects to array of CtClass objects
     *
     * @param pool    javassist class pool
     * @param classes classes to convert to CtClass objects
     * @return array of CtClass objects
     * @throws NotFoundException thrown if some of specified classes can not be resolved by javassist class pool
     */
    public static CtClass[] toCtClasses(ClassPool pool, Class<?>... classes) throws NotFoundException {
        CtClass[] ctClasses = new CtClass[classes.length];

        int i = 0;
        int classesLength = classes.length;

        while (i < classesLength) {
            ctClasses[i] = pool.get(classes[i].getName());
            i++;
        }

        return ctClasses;
    }

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
}
