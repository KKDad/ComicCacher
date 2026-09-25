package org.stapledon.infrastructure.logging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.stapledon.common.util.LogContext;
import org.stapledon.common.util.MdcTaskDecorator;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;

/**
 * Keeps the request's MDC keys (request id, user, GraphQL operation) on the threads that do the work.
 * <ul>
 * <li>The {@link TaskDecorator} bean is picked up by Spring Boot's {@code @Async} executor (virtual threads).</li>
 * <li>The context-propagation accessor lets Spring GraphQL carry the MDC into data fetchers.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class LoggingContextConfiguration {

    static {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                new Slf4jThreadLocalAccessor(LogContext.REQUEST_ID, LogContext.USER, LogContext.GRAPHQL_OPERATION));
    }

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
