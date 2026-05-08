package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.config.IComicsBootstrap;
import org.stapledon.common.util.GsonUtils;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

@Configuration(proxyBeanMethods = false)
public class GsonProvider {

    @Bean(name = "gsonWithLocalDate")
    public Gson gson() {
        return GsonUtils.createGsonBuilder()
                .registerTypeAdapter(IComicsBootstrap.class, new IComicsBootstrapDeserializer())
                .create();
    }

    /**
     * Custom deserializer for IComicsBootstrap interface.
     * Determines the concrete implementation based on fields in the JSON.
     */
    static class IComicsBootstrapDeserializer implements JsonDeserializer<IComicsBootstrap> {
        @Override
        public IComicsBootstrap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("website")) {
                return context.deserialize(json, KingComicsBootStrap.class);
            }
            return context.deserialize(json, GoComicsBootstrap.class);
        }
    }
}
