package org.stapledon.comics.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
@SpringBootApplication(excludeName = {
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
    "org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration"})
public class ComicsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicsServerApplication.class, args);
    }
}
