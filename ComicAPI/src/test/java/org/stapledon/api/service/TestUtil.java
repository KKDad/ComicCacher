package org.stapledon.api.service;

import org.springframework.http.MediaType;
import org.stapledon.infrastructure.config.GsonProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TestUtil {

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        var gson = new GsonProvider().gson();
        return gson.toJson(object).getBytes();
    }

    public static String createStringWithLength(int length) {
        return "a".repeat(Math.max(0, length));
    }
}
