package com.stapledon.comic;

import com.google.gson.Gson;
import com.stapledon.config.ApiConfig;
import com.stapledon.config.JsonConfigLoader;
import com.stapledon.interop.ComicConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
public class ComicApiApplication
{
	private static final Logger logger = Logger.getLogger(ComicApiApplication.class.getName());
	public static ApiConfig config;

	public ComicApiApplication() {
		logger.info("ComicApiApplication starting...");
		ComicApiApplication.config = new JsonConfigLoader().load();
		logger.info(String.format("Cache Directory: %s", ComicApiApplication.config.cacheDirectory));

		try {
			File initialFile = new File(ComicApiApplication.config.cacheDirectory + "/comics.json");
			InputStream inputStream = new FileInputStream(initialFile);
			Reader reader = new InputStreamReader(inputStream);
			ComicConfig comicConfig = new Gson().fromJson(reader, ComicConfig.class);
			ComicsService.comics.addAll(comicConfig.items.values());

			logger.info(String.format("Loaded: %d comics.", ComicsService.comics.size()));

		} catch (FileNotFoundException fne) {
			logger.log(Level.SEVERE, "Cannot load ComicList: " + fne.getMessage());
		}
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ComicApiApplication.class, args);
	}

}

