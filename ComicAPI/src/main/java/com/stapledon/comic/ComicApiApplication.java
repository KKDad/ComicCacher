package com.stapledon.comic;

import com.stapledon.config.ApiConfig;
import com.stapledon.config.JsonConfigLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class ComicApiApplication
{
	private static final Logger logger = Logger.getLogger(ComicApiApplication.class.getName());
	public static ApiConfig config;

	public ComicApiApplication()
	{
		logger.info("ComicApiApplication starting...");
		ComicApiApplication.config = new JsonConfigLoader().load();
		logger.info(String.format("Cache Directory: %s", ComicApiApplication.config.cacheDirectory));
	}


	public static void main(String[] args)
	{
		SpringApplication.run(ComicApiApplication.class, args);
	}

}

