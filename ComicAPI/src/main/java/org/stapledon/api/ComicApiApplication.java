package org.stapledon.api;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.config.ApiConfig;
import org.stapledon.config.ApiConfigLoader;
import org.stapledon.dto.ComicConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;

@SpringBootApplication
public class ComicApiApplication
{
	private static final Logger logger = LoggerFactory.getLogger(ComicApiApplication.class);

	public static ApiConfig config;

	public ComicApiApplication() {
		logger.info("ComicApiApplication starting...");
		ComicApiApplication.config = new ApiConfigLoader().load();
		File initialFile = new File(ComicApiApplication.config.cacheDirectory + "/comics.json");
		if (!initialFile.exists()) {
			logger.info("Cache Directory={} does not appear to be valid. Trying Cache Directory={} instead.", ComicApiApplication.config.cacheDirectory, ComicApiApplication.config.cacheDirectoryAlternate);
			initialFile = new File(ComicApiApplication.config.cacheDirectoryAlternate + "/comics.json");
			ComicsService.cacheLocation = ComicApiApplication.config.cacheDirectoryAlternate;
		} else {
			logger.info("Cache Directory: {}", ComicApiApplication.config.cacheDirectory);
			ComicsService.cacheLocation = ComicApiApplication.config.cacheDirectory;
		}


		try {
			InputStream inputStream = new FileInputStream(initialFile);
			Reader reader = new InputStreamReader(inputStream);
			ComicConfig comicConfig = new Gson().fromJson(reader, ComicConfig.class);
			ComicsService.getComics().addAll(comicConfig.items.values());

			logger.info("Loaded: {} comics.", ComicsService.getComics().size());

		} catch (FileNotFoundException fne) {
			logger.error("Cannot load ComicList", fne);
		}
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ComicApiApplication.class, args);
	}

}

