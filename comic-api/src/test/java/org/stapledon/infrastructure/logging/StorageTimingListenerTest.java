package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.stapledon.common.util.StorageTimings;

import ch.qos.logback.classic.Level;

class StorageTimingListenerTest {

    private final StorageTimingListener listener = new StorageTimingListener(new TimingProperties(1000, 250, 5));

    @AfterEach
    void cleanUp() {
        listener.uninstall();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void addsReadsToTheCurrentRequestAndWarnsAboutSlowOnes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/comics/1/strip/2026-09-28");
        RequestTimings timings = new RequestTimings();
        request.setAttribute(RequestTimings.ATTRIBUTE, timings);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        listener.install();

        try (LogCapture logs = new LogCapture(StorageTimingListener.class)) {
            StorageTimings.record("/comics/Fast/2026/2026-09-28.png", System.nanoTime());
            listener.onRead("/comics/Slow/2026/2026-09-28.png", 7_000_000);

            assertThat(logs.at(Level.WARN)).singleElement()
                    .extracting(e -> e.getFormattedMessage())
                    .isEqualTo("Slow storage read /comics/Slow/2026/2026-09-28.png took 7ms");
        }
        assertThat(timings.summary()).matches(" \\(storage=2/\\d+ms\\)");
    }

    @Test
    void ignoresReadsOutsideARequest() {
        listener.onRead("/comics/comics.json", 1_000_000);
        // No request context: nothing to record, and no exception
        assertThat(RequestTimings.current()).isNull();
    }
}
