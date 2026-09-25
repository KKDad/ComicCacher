package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

class MdcContextConverterTest {

    private final MdcContextConverter converter = new MdcContextConverter();

    @Test
    void printsNothingWhenNoKeysAreSet() {
        assertThat(converter.convert(event(Map.of()))).isEmpty();
    }

    @Test
    void printsOnlyThePresentKeysInAFixedOrder() {
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("date", "2026-09-24");
        mdc.put("comic", "Garfield");
        mdc.put("requestId", "ab12cd34");
        mdc.put("unrelated", "ignored");

        assertThat(converter.convert(event(mdc))).isEqualTo("[req=ab12cd34 comic=Garfield date=2026-09-24] ");
    }

    @Test
    void includesUserOperationAndBatchJob() {
        Map<String, String> mdc = Map.of("user", "alice", "gqlOp", "GetComics", "batchJobExecutionId", "42");

        assertThat(converter.convert(event(mdc))).isEqualTo("[user=alice op=GetComics job=42] ");
    }

    private static LoggingEvent event(Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(new LoggerContext());
        event.setLevel(Level.INFO);
        event.setMessage("message");
        event.setMDCPropertyMap(mdc);
        return event;
    }
}
