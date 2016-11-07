package org.xblackcat.sjpu.settings.config;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 03.11.2016 15:12
 *
 * @author xBlackCat
 */
public interface IConfigListener {
    static <T> IConfigListener listen(Class<T> clazz, String prefix, Consumer<T> listener) {
        return (clazz1, prefix1, newConfig) -> {
            if (Objects.equals(clazz, clazz1) && Objects.equals(prefix, prefix1)) {
                @SuppressWarnings("unchecked")
                T config = (T) newConfig;
                listener.accept(config);
            }
        };
    }

    void onConfigChanged(Class<?> clazz, String prefix, Object newConfig);
}
