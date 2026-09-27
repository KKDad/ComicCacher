package org.stapledon.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stapledon.common.util.GsonUtils;

import com.google.gson.Gson;

@Configuration(proxyBeanMethods = false)
public class GsonProvider {

    @Bean(name = "gsonWithLocalDate")
    public Gson gson() {
        return GsonUtils.createGsonBuilder().create();
    }
}
