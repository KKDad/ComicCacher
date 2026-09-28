package org.stapledon.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestTimingsTest {

    private static final long MS = 1_000_000;

    @Test
    void summaryIsEmptyWhenNothingWasRecorded() {
        assertThat(new RequestTimings().summary()).isEmpty();
    }

    @Test
    void summaryShowsGraphqlTimeSlowestFieldAndStorageReads() {
        RequestTimings timings = new RequestTimings();
        timings.recordGraphql(790 * MS);
        timings.recordField("Query.comics", 40 * MS);
        timings.recordField("Comic.strip", 640 * MS);
        timings.recordField("Comic.avatar", 12 * MS);
        timings.recordStorage(400 * MS);
        timings.recordStorage(210 * MS);

        assertThat(timings.summary()).isEqualTo(" (gql=790ms slowest=Comic.strip:640ms storage=2/610ms)");
    }

    @Test
    void summaryShowsStorageAloneForRestRequests() {
        RequestTimings timings = new RequestTimings();
        timings.recordStorage(15 * MS);

        assertThat(timings.summary()).isEqualTo(" (storage=1/15ms)");
    }
}
