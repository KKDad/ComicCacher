package org.stapledon;

import java.lang.reflect.Field;
import java.util.Map;

public class TestUtils
{

    /**
     * Set an Environment variable, only for testing purposes. This will only work under linux
     *
     * @param key Environment variable
     * @param value Value to set
     */
    public static void setEnv(String key, String value)
    {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }
}
