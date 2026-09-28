package org.stapledon.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application clock, in {@code batch.timezone}. Anything that asks "what day is it" (today's strip, retention
 * cutoffs, batch log file names) reads it from this clock, so the answer doesn't depend on the JVM's default zone
 * (UTC in the containers).
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    public Clock clock(@Value("${batch.timezone}") String batchTimezone) {
        return Clock.system(ZoneId.of(batchTimezone));
    }
}
