package org.xblackcat.sjpu.settings.config;

import javassist.ClassPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.settings.APrefixHandler;
import org.xblackcat.sjpu.settings.util.IValueGetter;

import java.util.List;
import java.util.Map;

/**
 * 25.06.2018 11:49
 *
 * @author xBlackCat
 */
public abstract class AConfig {
    protected final Log log = LogFactory.getLog(getClass());
    protected final ClassPool pool;
    protected final Map<String, APrefixHandler> prefixHandlers;
    protected final List<IValueGetter> substitutions;

    public AConfig(
            ClassPool pool,
            Map<String, APrefixHandler> prefixHandlers,
            List<IValueGetter> substitutions
    ) {
        this.pool = pool;
        this.prefixHandlers = prefixHandlers;
        this.substitutions = substitutions;
    }

    public abstract IValueGetter getValueGetter();
}
