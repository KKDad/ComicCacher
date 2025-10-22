package org.stapledon.engine.downloader;

import com.google.common.base.Preconditions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stapledon.common.model.ComicCachingException;
import org.stapledon.common.infrastructure.caching.ICachable;
import org.stapledon.common.infrastructure.web.WebInspector;
import org.stapledon.common.infrastructure.web.WebInspectorImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for all ComicCachers.
 */
@Slf4j
public abstract class DailyComic implements IDailyComic, ICachable {
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36";
    static final int TIMEOUT = 5 * 1000;

    private Path cacheDirectory;

    final WebInspector webInspector;
    final String elementSelector;

    String comicName;
    String comicNameParsed;
    LocalDate currentDate;

    protected abstract String generateSiteURL();

    protected abstract Optional<String> extractComicImage(String comicUrl);


    DailyComic(WebInspector inspector, String elementSelector) {
        this.webInspector = (inspector == null) ? new WebInspectorImpl() : inspector;
        this.elementSelector = elementSelector;
    }

    /**
     * Set the root path for this comic. The path will be later augmented with the name of the comic that is
     * being cached.
     *
     * @param path Root Path to set.
     * @return this
     */
    public IDailyComic setCacheRoot(String path) {

        this.cacheDirectory = Paths.get(Objects.requireNonNull(path, "path cannot be null"));
        return this;
    }

    /**
     * Get the path full path where this comic has been cached. Include any augmentation
     *
     * @return Path
     */
    public String cacheLocation() {
        return String.format("%s/%s", cacheDirectory, comicNameParsed);
    }


    /**
     * Download the image to the specified location.
     *
     * @param sourceImageElement - HTML element for the image to download
     * @param destinationFile    - Fully qualified name of the file to save
     * @return True if successful
     */
    boolean cacheImage(String imageUrl, String destinationFile) throws IOException {
        Preconditions.checkNotNull(imageUrl, "imageUrl cannot be null");
        Preconditions.checkNotNull(destinationFile, "destinationFile cannot be null");

        // Always ensure that the destination directory exists before continuing
        ensureCacheDirectory();

        try {
            URL urlImage = new URL(imageUrl);

            log.info("Downloading Image from: {}", urlImage);

            try (InputStream in = urlImage.openStream();
                 OutputStream os = new FileOutputStream(destinationFile)) {
                var buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    os.write(buffer, 0, n);
                }
                log.trace("Image saved to: {}", destinationFile);
                return true;
            }
        } catch (FileNotFoundException e) {
            log.error("Failed to save Image:", e);
        }
        return false;
    }

    /**
     * Ensure that the api is cached for the current date
     *
     * @return true if the api for the current day has been successfully cached.
     */
    @Override
    public boolean ensureCache() {
        var f = new File(generateCachedName());
        if (f.exists()) {
            if (log.isTraceEnabled())
                log.trace("Image has already been cached as: {}", f.getAbsolutePath());
            return true;
        }

        if (log.isDebugEnabled())
            log.debug("Caching image to: {}", f.getAbsolutePath());

        try {
            String url = this.generateSiteURL();
            log.debug("Fetching {}", url);

            Optional<String> imageUrl = extractComicImage(url);

            if (imageUrl.isEmpty()) {
                log.error("No comic image found for {}", url);
                return false;
            }
            return cacheImage(imageUrl.get(), f.getAbsolutePath());

        } catch (IOException ioe) {
            log.error("Failed to cache comic {}: {}", getComic(), ioe.getMessage());
            throw ComicCachingException.forComic(getComic(), ioe);
        }
    }



    private String generateCachedName() {
        ensureCacheDirectory();
        // TODO: Autodetect image type. Perhaps https://stackoverflow.com/questions/12531797/how-to-get-an-image-type-without-knowing-its-file-extension?
        var extension = "png";
        return String.format("%s/%s.%s", this.cacheLocation(), this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd")), extension);
    }

    /**
     * Check if the destination directory for the image exists or create it if it doesn't exist.
     */
    private void ensureCacheDirectory() {
        Preconditions.checkNotNull(this.comicName, "Must call setComic() before ensureCacheDirectory()");

        var directoryName = String.format("%s/%s", this.cacheLocation(), this.currentDate.format(DateTimeFormatter.ofPattern("yyyy")));
        var directory = new File(directoryName);
        if (!directory.exists()) {
            if (log.isDebugEnabled())
                log.debug("Creating utils directory to: {}", directoryName);
            if (!directory.mkdirs())
                throw new IllegalStateException("Cannot create cache location");
        }
    }

    /**
     * Set the date for the retrieval
     *
     * @param date date to set
     * @return this
     */
    @Override
    public IDailyComic setDate(LocalDate date) {
        this.currentDate = date;
        if (log.isInfoEnabled())
            log.info("Date set to: {}", this.currentDate);

        return this;
    }

    public LocalDate getDate() {
        return this.currentDate;
    }

    /**
     * Set the GoComic that to caching
     *
     * @param comicName Name of the api to process
     * @return this
     */
    @Override
    public IDailyComic setComic(String comicName) {
        this.comicName = comicName;
        this.comicNameParsed = comicName.replace(" ", "");
        if (log.isInfoEnabled())
            log.info("Comic: {}", this.comicName);

        return this;
    }

    public String getComic() {
        return this.comicName;
    }

    public LocalDate advance() {
        if (this.currentDate.isBefore(this.getLastStripOn()))
            this.currentDate = this.currentDate.plusDays(1);
        return this.currentDate;
    }
}