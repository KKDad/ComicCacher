package org.stapledon.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logback filter that accepts events only when a specified MDC key is present and not equal to a default value.
 * Replaces JaninoEventEvaluator which was removed in Logback 1.5.
 */
public class MdcPresentFilter extends Filter<ILoggingEvent> {

    private String key;
    private String defaultValue = "NONE";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String value = event.getMDCPropertyMap().get(key);
        if (value != null && !value.equals(defaultValue)) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
