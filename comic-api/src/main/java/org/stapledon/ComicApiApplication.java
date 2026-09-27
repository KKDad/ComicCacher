package org.stapledon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@SpringBootApplication
@ConfigurationPropertiesScan("org.stapledon")
@EnableScheduling
@EnableAsync
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class ComicApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComicApiApplication.class, args);
    }
}
