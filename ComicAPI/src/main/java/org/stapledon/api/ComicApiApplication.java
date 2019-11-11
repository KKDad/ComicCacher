package org.stapledon.api;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.stapledon.config.CacherBootstrapConfig;
import org.stapledon.config.IComicsBootstrap;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.downloader.DailyRunner;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;

import java.io.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
			JsonConfigWriter jsonConfigWriter = new JsonConfigWriter(ComicsService.cacheLocation + "/comics.json");
			ComicConfig comicConfig = jsonConfigWriter.loadComics();
			reconcileBoostrapConfig(comicConfig);
			comicConfig = jsonConfigWriter.loadComics();

			ComicsService.getComics().addAll(comicConfig.items.values());
			logger.info("Loaded: {} comics.", ComicsService.getComics().size());

		} catch (IOException fne) {
			logger.error("Cannot load ComicList", fne);
		}

		// Ensure we cache comics once a day
		DailyRunner.ensureDailyCaching();
	}

	/**
	 * Reconcile all of the entries in the CacherBootstrapConfig with the ComicList
	 * - New entries found in the CacherBootstrapConfig will be added and immediately cached
	 * - Entries found in the ComicList, but not in the CacherBootstrapConfig will be removed
	 */
	public void reconcileBoostrapConfig(ComicConfig comicConfig)
	{
		logger.info("Begin Reconciliation of CacherBootstrapConfig and ComicConfig");
		try {
			ComicCacher cacher = new ComicCacher();
			CacherBootstrapConfig config = cacher.bootstrapConfig();

			// Check for New GoComics
			for (IComicsBootstrap daily : config.dailyComics) {
				ComicItem comic = findComicItem(comicConfig, daily);
				if (comic == null) {
					if (logger.isInfoEnabled())
						logger.info("Bootstrapping new DailyComic: {}", daily.stripName());
					cacher.cacheSingle(true,daily);
				}
			}

			// Check for New KingFeatures
			for (IComicsBootstrap king : config.kingComics) {
				ComicItem comic = findComicItem(comicConfig, king);
				if (comic == null) {
					if (logger.isInfoEnabled())
						logger.info("Bootstrapping new KingFeatures: {}", king.stripName());
					cacher.cacheSingle(true, king);
				}
			}

			// Removed entries found in ComicConfig, but not in the CacherBootstrapConfig
			comicConfig.items.entrySet().removeIf(integerComicItemEntry -> findBootstrapComic(config, integerComicItemEntry.getValue()) == null);


		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			logger.error(e.getMessage());
		}
		logger.info("Reconciliation complete");
	}

	/**
	 * Giving a ComicItem, Locate the corresponding IComicsBootstrap from the BootStrap configuration
	 * @param comic ComicItem to lookup
	 * @return IComicsBootstrap or null if none could be located
	 */
	IComicsBootstrap findBootstrapComic(CacherBootstrapConfig config, ComicItem comic)
	{
		if (!config.dailyComics.isEmpty()) {
			IComicsBootstrap dailyComics = config.dailyComics.stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
			if (dailyComics != null)
				return dailyComics;
		}
		if (!config.kingComics.isEmpty()) {
			IComicsBootstrap kingComics = config.kingComics.stream().filter(p -> p.name.equalsIgnoreCase(comic.name)).findFirst().orElse(null);
			if (kingComics != null)
				return kingComics;
		}
		if (logger.isWarnEnabled())
			logger.warn("{} was not found. Disabling", comic.name);
		return null;
	}

	/**
	 * Giving a IComicsBootstrap, Locate the corresponding ComicItem from the BootStrap configuration
	 * @param config List<ComicConfig> to search in
	 * @param comic IComicsBootstrap to lookup
	 * @return ComicItem or null if none could be located
	 */
	ComicItem findComicItem(ComicConfig config, IComicsBootstrap comic)
	{

		if (!config.items.isEmpty()) {
			Map.Entry<Integer, ComicItem> result = config.items.entrySet()
					                                           .stream()
															   .filter(p -> p.getValue().name.equalsIgnoreCase(comic.stripName()))
					                                           .findFirst()
					                                           .orElse(null);
			if (result != null)
				return result.getValue();
		}
		return null;
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ComicApiApplication.class, args);
	}

}

