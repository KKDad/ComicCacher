package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
@DisplayName("MdcPresentFilter")
class MdcPresentFilterTest {

    @Mock
    private ILoggingEvent event;

    private MdcPresentFilter filter;

    @BeforeEach
    void setUp() {
        filter = new MdcPresentFilter();
        filter.setKey("batchJobExecutionId");
        filter.setDefaultValue("NONE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "42", "999"})
    @DisplayName("should accept events with a valid MDC key")
    void shouldAcceptWhenMdcKeyPresent(String executionId) {
        when(event.getMDCPropertyMap()).thenReturn(Map.of("batchJobExecutionId", executionId));

        assertThat(filter.decide(event)).isEqualTo(FilterReply.ACCEPT);
    }

    @Test
    @DisplayName("should deny events when MDC key is absent")
    void shouldDenyWhenMdcKeyAbsent() {
        when(event.getMDCPropertyMap()).thenReturn(Map.of());

        assertThat(filter.decide(event)).isEqualTo(FilterReply.DENY);
    }

    @Test
    @DisplayName("should deny events when MDC key equals default value")
    void shouldDenyWhenMdcKeyIsDefault() {
        when(event.getMDCPropertyMap()).thenReturn(Map.of("batchJobExecutionId", "NONE"));

        assertThat(filter.decide(event)).isEqualTo(FilterReply.DENY);
    }
}
