package org.stapledon.downloader;

import com.google.common.base.Preconditions;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.caching.ICachable;
import org.stapledon.web.IWebInspector;
import org.stapledon.web.WebInspector;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Base class for all ComicCachers.
 */
public abstract class DailyComic implements IDailyComic, ICachable
{
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    static final int TIMEOUT = 5 * 1000;

    private Path cacheDirectory;

    private static final Logger logger = LoggerFactory.getLogger(DailyComic.class);

    final IWebInspector webInspector;

    String comicName;
    String comicNameParsed;
    LocalDate currentDate;

    public DailyComic(IWebInspector inspector)
    {
        this.webInspector = (inspector == null) ? new WebInspector() : inspector;
    }

    /**
     * Set the root path for this comic. The path will be later augmented with the name of the comic that is
     * being cached.
     * @param path Root Path to set.
     * @return this
     */
    IDailyComic setCacheRoot(String path)
    {

        this.cacheDirectory = Paths.get(Objects.requireNonNull(path, "path cannot be null"));
        return this;
    }

    /**
     * Get the path full path where this comic has been cached. Include any augmentation
     * @return Path
     */
    public String CacheLocation()
    {
        return String.format("%s/%s", cacheDirectory, comicNameParsed);
    }


    /**
     * Download the image to the specified location.
     * @param sourceImageElement - HTML element for the image to download
     * @param destinationFile - Fully qualified name of the file to save
     * @return True if successful
     * @throws IOException
     */
    boolean cacheImage(Element sourceImageElement, String destinationFile) throws IOException
    {
        Preconditions.checkNotNull(sourceImageElement, "sourceImageElement cannot be null");
        Preconditions.checkNotNull(destinationFile, "destinationFile cannot be null");

        OutputStream os = null;
        try {
            URL urlImage = new URL(sourceImageElement.attr(WebInspector.ABS_SRC));
            logger.info("Downloading Image from: {}", urlImage);
            try (InputStream in = urlImage.openStream()) {
                byte[] buffer = new byte[4096];
                int n;
                os = new FileOutputStream(destinationFile);
                while ((n = in.read(buffer)) != -1) {
                    os.write(buffer, 0, n);
                }
            }
            logger.trace("Image saved");
            return true;
        } catch (FileNotFoundException e) {
            logger.error("Failed to save Image:", e);

        } finally {
            if (os != null)
                os.close();
        }
        return false;
    }

    IDailyComic ensureCacheDirectoryExists()
    {
        Preconditions.checkNotNull(this.comicName, "Must call setComic() before ensureCacheDirectoryExists()");

        String directoryName = String.format("%s/%s", this.CacheLocation(), this.currentDate.format(DateTimeFormatter.ofPattern("yyyy")));
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (logger.isDebugEnabled())
                logger.debug("Creating utils directory to: {}", directoryName);
            directory.mkdirs();
        }
        return this;
    }

    /**
     * Set the date for the retrieval
     * @param date date to set
     * @return this
     */
    @Override
    public IDailyComic setDate(LocalDate date)
    {
        this.currentDate = date;
        if (logger.isInfoEnabled())
            logger.info("Date set to: {}", this.currentDate);

        return this;
    }

    /**
     * Set the GoComic that to caching
     * @param comicName Name of the api to process
     * @return this
     */
    @Override
    public IDailyComic setComic(String comicName)
    {
        this.comicName = comicName;
        this.comicNameParsed = comicName.replace(" ", "");
        if (logger.isInfoEnabled())
            logger.info("Comic: {}", this.comicName);

        return this;
    }

}