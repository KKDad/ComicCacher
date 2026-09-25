package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void workerThreadSeesTheSubmittersMdc() throws Exception {
        MDC.put("batchLogPath", "ComicDownloadJob/42");
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable task = decorator.decorate(() -> seen.set(MDC.get("batchLogPath")));

        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            pool.submit(task).get();
        }

        assertThat(seen.get()).isEqualTo("ComicDownloadJob/42");
    }

    @Test
    void restoresTheWorkersOwnMdcAfterTheTask() {
        MDC.put("requestId", "caller");
        Runnable task = decorator.decorate(() -> MDC.put("comic", "garfield"));

        // Run on this thread, as CallerRunsPolicy does, with a different context in place
        MDC.clear();
        MDC.put("requestId", "worker");
        task.run();

        assertThat(MDC.get("requestId")).isEqualTo("worker");
        assertThat(MDC.get("comic")).isNull();
    }

    @Test
    void emptyCallerContextClearsTheWorkerDuringTheTask() {
        Runnable task = decorator.decorate(() -> assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty());

        MDC.put("requestId", "stale");
        task.run();

        assertThat(MDC.get("requestId")).isEqualTo("stale");
    }
}
