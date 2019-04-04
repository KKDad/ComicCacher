package com.stapledon.config;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class JsonConfigLoader {

    public ApiConfig load()
    {
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("ApiConfig.json");

        return load(inputStream);
    }

    public ApiConfig load(InputStream inputStream)
    {
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(inputStream);
        return gson.fromJson(reader, ApiConfig.class);
    }




}
