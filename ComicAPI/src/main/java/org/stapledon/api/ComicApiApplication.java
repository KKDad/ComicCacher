package org.stapledon.api;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.config.ApiConfig;
import org.stapledon.config.ApiConfigLoader;
import org.stapledon.downloader.DailyDownloader;
import org.stapledon.dto.ComicConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;

@SpringBootApplication
public class ComicApiApplication
{
	private static final Logger logger = LoggerFactory.getLogger(ComicApiApplication.class);

	private static ApiConfig config;

	public static ApiConfig getConfig()
	{
		if (ComicApiApplication.config == null)
			ComicApiApplication.config = new ApiConfigLoader().load();
		return ComicApiApplication.config;
	}

	public static void setConfig(ApiConfig config)
	{
		logger.warn("Configuration manually set");
		ComicApiApplication.config = config;
	}


	public ComicApiApplication() {
		logger.info("ComicApiApplication starting...");
		ComicApiApplication.getConfig();

		File directory=new File(ComicApiApplication.config.cacheDirectory);
		ComicsService.cacheLocation = directory.exists() ? ComicApiApplication.config.cacheDirectory : ComicApiApplication.config.cacheDirectoryAlternate;
		logger.warn("Serving from {}", ComicApiApplication.config.cacheDirectory);

		try {
			File config = new File(ComicsService.cacheLocation + "/comics.json");
			if (!config.exists())
				logger.warn("File {} does not exist", config);
			else {
				InputStream is = new FileInputStream(config);
				Reader reader = new InputStreamReader(is);
				ComicConfig comicConfig = new Gson().fromJson(reader, ComicConfig.class);
				ComicsService.getComics().addAll(comicConfig.items.values());
				logger.info("Loaded: {} comics.", ComicsService.getComics().size());
				reader.close();
				is.close();
			}

		} catch (IOException fne) {
			logger.error("Cannot load ComicList", fne);
		}

		// Ensure we cache comics once a day
		DailyDownloader.EnsureDailyCaching();
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ComicApiApplication.class, args);
	}

}

