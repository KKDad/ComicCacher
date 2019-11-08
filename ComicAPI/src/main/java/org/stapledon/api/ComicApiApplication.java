package org.stapledon.api;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.downloader.DailyRunner;
import org.stapledon.dto.ComicConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;

@SpringBootApplication
public class ComicApiApplication
{
	private static final Logger logger = LoggerFactory.getLogger(ComicApiApplication.class);

	public ComicApiApplication() {
		logger.info("ComicApiApplication starting...");

		String dir = System.getenv("CACHE_DIRECTORY");
		if (dir == null) {
			logger.error("CACHE_DIRECTORY not set. Defaulting to /comics");
			dir = "/comics";
		}

		File directory = new File(dir);
		if (!directory.exists() || directory.isDirectory()) {
			directory.mkdirs();
		}
		ComicsService.cacheLocation = dir;
		logger.warn("Serving from {}", dir);

		try {
			File cf = new File(ComicsService.cacheLocation + "/comics.json");
			if (!cf.exists())
				logger.warn("File {} does not exist", cf);
			else {
				InputStream is = new FileInputStream(cf);
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
		DailyRunner.ensureDailyCaching();
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ComicApiApplication.class, args);
	}

}

