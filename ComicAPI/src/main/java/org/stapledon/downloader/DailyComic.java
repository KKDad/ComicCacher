package org.stapledon.downloader;

import com.google.common.base.Preconditions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
    final String elementSelector;

    String comicName;
    String comicNameParsed;
    LocalDate currentDate;

    protected abstract String generateSiteURL();


    public DailyComic(IWebInspector inspector, String elementSelector)
    {
        this.webInspector = (inspector == null) ? new WebInspector() : inspector;
        this.elementSelector = elementSelector;
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
    public String cacheLocation()
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

        // Always ensure that the destination directory exists before continuing
        ensureCacheDirectory();

        OutputStream os = null;
        try {
            URL urlImage;
            switch (sourceImageElement.tagName())
            {
                case "src":
                case "img":
                    urlImage = new URL(sourceImageElement.attr(WebInspector.ABS_SRC));
                    break;
                case "meta":
                    urlImage = new URL(sourceImageElement.attr(WebInspector.CONTENT));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
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

    /**
     * Ensure that the api is cached for the current date
     * @return true if the api for the current day has been successfully cached.
     */
    @Override
    public boolean ensureCache()
    {
        File f = new File(generateCachedName());
        if (f.exists()) {
            if (logger.isDebugEnabled())
                logger.debug("Image has already been cached as: {}", f.getAbsolutePath());
            return true;
        }

        if (logger.isDebugEnabled())
            logger.debug("Caching image to: {}", f.getAbsolutePath());


        try {
            String url = this.generateSiteURL();

            Document doc = Jsoup.connect(url)
                                .userAgent(USER_AGENT)
                                .header("DNT", "1")
                                .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                                .timeout(TIMEOUT)
                                .get();
            Elements media = doc.select(elementSelector);

            Elements image = this.pickImages(media);
            return cacheImage(image.first(), f.getAbsolutePath());

        } catch (IOException ioe) {
            return false;
        }
    }

    protected abstract Elements pickImages(Elements media);

    private String generateCachedName()
    {
        ensureCacheDirectory();
        // TODO: Autodetect image type. Perhaps https://stackoverflow.com/questions/12531797/how-to-get-an-image-type-without-knowing-its-file-extension?
        String extension = "png";
        return String.format("%s/%s.%s", this.cacheLocation(), this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd")), extension);
    }

    /**
     * Check if the destination directory for the image exists or create it if it doesn't exist.
     */
    private void ensureCacheDirectory()
    {
        Preconditions.checkNotNull(this.comicName, "Must call setComic() before ensureCacheDirectory()");

        String directoryName = String.format("%s/%s", this.cacheLocation(), this.currentDate.format(DateTimeFormatter.ofPattern("yyyy")));
        File directory = new File(directoryName);
        if (!directory.exists()) {
            if (logger.isDebugEnabled())
                logger.debug("Creating utils directory to: {}", directoryName);
            if (!directory.mkdirs())
                throw new IllegalStateException("Cannot create cache location");
        }
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

    public LocalDate getDate()
    {
        return this.currentDate;
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

    public String getComic()
    {
        return this.comicName;
    }

    public LocalDate advance() {
        if (this.currentDate.isBefore(this.getLastStripOn()))
            this.currentDate = this.currentDate.plusDays(1);
        return this.currentDate;
    }
}