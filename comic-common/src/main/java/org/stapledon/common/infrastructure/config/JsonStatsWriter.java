package org.stapledon.common.infrastructure.config;

import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.stapledon.common.dto.ImageCacheStats;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON implementation of StatsWriter for persisting statistics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonStatsWriter implements StatsWriter {
    @Qualifier("gsonWithLocalDate")
    private final Gson gson;

    @Override
    public boolean save(ImageCacheStats stats, String targetDirectory) {
        try {
            Writer writer = new FileWriter(targetDirectory + "/stats.db");
            gson.toJson(stats, writer);
            writer.flush();
            writer.close();
            return true;
        } catch (IOException ioe) {
            log.error("Failed to save image cache stats: {}", ioe.getMessage(), ioe);
        }
        return false;
    }
}
