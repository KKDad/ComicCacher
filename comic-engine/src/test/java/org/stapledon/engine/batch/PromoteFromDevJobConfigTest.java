package org.stapledon.engine.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import org.stapledon.engine.batch.config.PromoteFromDevJobConfig;
import org.stapledon.engine.promotion.DevPromotionService;
import org.stapledon.engine.promotion.DevPromotionService.PromotionResult;
import org.stapledon.engine.source.SourceRegistry;

class PromoteFromDevJobConfigTest {

    private DevPromotionService promotionService;
    private PromoteFromDevJobConfig config;

    @BeforeEach
    void setUp() {
        promotionService = mock(DevPromotionService.class);
        config = new PromoteFromDevJobConfig(promotionService, mock(SourceRegistry.class));
        setField(config, "defaultDays", 1);
    }

    @Test
    void noParameters_promotesTheDefaultDays() throws Exception {
        when(promotionService.promote(anyInt(), any(), any())).thenReturn(new PromotionResult(true, 1, 1, 0, 0, 0, 0));

        Tasklet tasklet = config.promoteFromDevTasklet(null, null, null);

        assertThat(tasklet.execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
        verify(promotionService).promote(1, null, null);
    }

    @Test
    void parameters_arePassedOn() throws Exception {
        when(promotionService.promote(anyInt(), any(), any())).thenReturn(new PromotionResult(true, 0, 0, 0, 0, 0, 0));

        config.promoteFromDevTasklet("7", "gocomics", "42").execute(null, null);

        verify(promotionService).promote(7, "gocomics", 42);
    }

    @Test
    void failedStrips_failTheStep() {
        when(promotionService.promote(anyInt(), any(), any())).thenReturn(new PromotionResult(true, 1, 3, 0, 0, 2, 0));

        Tasklet tasklet = config.promoteFromDevTasklet("1", null, null);

        assertThatThrownBy(() -> tasklet.execute(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 strip(s)");
    }
}
