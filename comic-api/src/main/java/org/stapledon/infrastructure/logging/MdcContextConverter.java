package org.stapledon.infrastructure.logging;

import org.stapledon.common.util.LogContext;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback converter ({@code %ctx}) that prints only the MDC keys that are set, as {@code [req=ab12cd34 user=alice comic=Garfield date=2026-09-24] },
 * and nothing when none are. Keeps request, batch and download lines searchable without padding every other line with empty fields.
 */
public class MdcContextConverter extends ClassicConverter {

    private static final List<Map.Entry<String, String>> KEYS = List.of(
            Map.entry(LogContext.REQUEST_ID, "req"),
            Map.entry(LogContext.USER, "user"),
            Map.entry(LogContext.GRAPHQL_OPERATION, "op"),
            Map.entry("batchJobExecutionId", "job"),
            Map.entry(LogContext.COMIC, "comic"),
            Map.entry(LogContext.DATE, "date"),
            Map.entry(LogContext.STRIP, "strip"));

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> key : KEYS) {
            String value = mdc.get(key.getKey());
            if (value != null && !value.isEmpty()) {
                out.append(out.isEmpty() ? "[" : " ").append(key.getValue()).append('=').append(value);
            }
        }
        return out.isEmpty() ? "" : out.append("] ").toString();
    }
}
