package org.stapledon.infrastructure.logging;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLNamedType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Times every non-trivial GraphQL field (resolver / data fetcher). Each field is logged at DEBUG, a field at or above
 * {@code comics.timing.slow-fetcher-ms} at WARN, and the slowest one goes into the request's {@link RequestTimings}.
 * Plain property reads are skipped: they cost nothing and would flood the log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimingInstrumentation extends SimplePerformantInstrumentation {

    private final TimingProperties properties;

    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters,
            InstrumentationState state) {
        if (parameters.isTrivialDataFetcher()) {
            return dataFetcher;
        }
        return environment -> {
            long start = System.nanoTime();
            Object result = dataFetcher.get(environment);
            if (result instanceof CompletionStage<?> stage) {
                return stage.whenComplete((value, error) -> record(environment, start));
            }
            record(environment, start);
            return result;
        };
    }

    private void record(DataFetchingEnvironment environment, long start) {
        long nanos = System.nanoTime() - start;
        String field = fieldName(environment);
        long ms = RequestTimings.millis(nanos);
        if (ms >= properties.slowFetcherMs()) {
            log.warn("Slow GraphQL field {} took {}ms", field, ms);
        } else {
            log.debug("GraphQL field {} took {}ms", field, ms);
        }
        RequestTimings timings = environment.getGraphQlContext().get(RequestTimings.ATTRIBUTE);
        if (timings != null) {
            timings.recordField(field, nanos);
        }
    }

    static String fieldName(DataFetchingEnvironment environment) {
        String parent = environment.getParentType() instanceof GraphQLNamedType named ? named.getName() : "?";
        return parent + "." + environment.getFieldDefinition().getName();
    }
}
