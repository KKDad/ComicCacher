package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.qos.logback.classic.Level;
import graphql.GraphQLContext;
import graphql.Scalars;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

class TimingInstrumentationTest {

    private final RequestTimings timings = new RequestTimings();

    @Test
    void recordsTheSlowestFieldAndWarnsPastTheThreshold() throws Exception {
        TimingInstrumentation instrumentation = new TimingInstrumentation(new TimingProperties(1000, 0, 200));
        DataFetcher<?> fetcher = instrumentation.instrumentDataFetcher(env -> "strip", parameters(false), null);

        try (LogCapture logs = new LogCapture(TimingInstrumentation.class)) {
            assertThat(fetcher.get(environment())).isEqualTo("strip");

            assertThat(logs.at(Level.WARN)).singleElement()
                    .extracting(e -> e.getFormattedMessage())
                    .asString()
                    .matches("Slow GraphQL field Comic\\.strip took \\d+ms");
        }
        assertThat(timings.summary()).matches(" \\(slowest=Comic\\.strip:\\d+ms\\)");
    }

    @Test
    void recordsAsyncFieldsWhenTheyComplete() throws Exception {
        TimingInstrumentation instrumentation = new TimingInstrumentation(TimingProperties.defaults());
        CompletableFuture<String> pending = new CompletableFuture<>();
        DataFetcher<?> fetcher = instrumentation.instrumentDataFetcher(env -> pending, parameters(false), null);

        CompletableFuture<?> result = ((java.util.concurrent.CompletionStage<?>) fetcher.get(environment())).toCompletableFuture();
        assertThat(timings.summary()).isEmpty();

        pending.complete("strip");
        assertThat(result.get()).isEqualTo("strip");
        assertThat(timings.summary()).startsWith(" (slowest=Comic.strip:");
    }

    @Test
    void leavesTrivialFieldsAlone() {
        TimingInstrumentation instrumentation = new TimingInstrumentation(TimingProperties.defaults());
        DataFetcher<?> original = env -> "name";

        assertThat(instrumentation.instrumentDataFetcher(original, parameters(true), null)).isSameAs(original);
    }

    private static InstrumentationFieldFetchParameters parameters(boolean trivial) {
        InstrumentationFieldFetchParameters parameters = mock(InstrumentationFieldFetchParameters.class);
        when(parameters.isTrivialDataFetcher()).thenReturn(trivial);
        return parameters;
    }

    private DataFetchingEnvironment environment() {
        GraphQLFieldDefinition field = GraphQLFieldDefinition.newFieldDefinition().name("strip").type(Scalars.GraphQLString).build();
        GraphQLObjectType comic = GraphQLObjectType.newObject().name("Comic").field(field).build();
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                .parentType(comic)
                .fieldDefinition(field)
                .graphQLContext(GraphQLContext.of(Map.of(RequestTimings.ATTRIBUTE, timings)))
                .build();
    }
}
