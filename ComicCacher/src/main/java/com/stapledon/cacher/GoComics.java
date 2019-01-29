package com.stapledon.cacher;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GoComics extends DailyComic {

    private static final Logger logger = Logger.getLogger(GoComics.class);

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
            logger.info("Date set to: " + this.currentDate.toString());

        return this;
    }

    /**
     * Set the GoComic that to caching
     * @param comicName Name of the comic to process
     * @return this
     */
    @Override
    public IDailyComic setComic(String comicName)
    {
        this.comicName = comicName;
        this.comicNameParsed = comicName.replace(" ", "");
        if (logger.isInfoEnabled())
            logger.info("Comic: " + this.comicName);

        return this;
    }

    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     * @return Mst recent date we can get a comic for
     */
    public LocalDate getLastStripOn()
    {
        return LocalDate.now();
    }

    /**
     * Generate the URL for a specific date.
     */
    private String generateSiteURL() {
        return String.format("http://www.gocomics.com/%s/%s/", this.comicNameParsed, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))).toLowerCase();
    }

    /**
     * Link to the About the comic page
     * @return URL where we can get the about information for this strip
     */
    private String generateAboutUTL()
    {
        return String.format("https://www.gocomics.com/%s/about",  this.comicNameParsed);
    }

    public String getComicDescription()
    {
        try {
            String url = this.generateAboutUTL();
            logger.info("Getting Comic Description from " + url);

            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(TIMEOUT).get();
            // Fragile, however there appears to only be one "section" class and the description seems to be the
            // first div inside it.
            return doc.select("section").select("div").get(0).text();

        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Determines which links represent the comic image that we should cache
     * @param media list of image links to choose from
     */
    private Elements pickImages(Elements media)
    {
        Elements elements = new Elements();
        for (Element src : media) {
            if (src.tagName().equals("img") && src.attr("abs:src").contains("assets.amuniversal.com"))
                elements.add(src);
        }
        dumpMedia(elements);
        // We get back 2-3 images. The 2nd image is the hi-res version - we'll select it.
        if (elements.size() > 1) {
            Elements e = new Elements();
            e.add(elements.get(1));
            return e;

        }
        return elements;
    }

    private String generateCachedName()
    {
        ensureCacheDirectoryExists();
        // TODO: Autodetect image type. Perhaps https://stackoverflow.com/questions/12531797/how-to-get-an-image-type-without-knowing-its-file-extension?
        String extension = "png";
        return String.format("%s/%s/%s.%s", this.getCacheDirectory(), comicNameParsed, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd")), extension);
    }


    /**
     * Ensure that the comic is cached for the current date
     * @return true if the comic for the current day has been successfully cached.
     */
    @Override
    public boolean ensureCache() {

        File f = new File(generateCachedName());
        if (f.exists()) {
            if (logger.isDebugEnabled())
                logger.debug("Image has already been cached as : " + f.getAbsolutePath());
            return true;
        }

        if (logger.isDebugEnabled())
            logger.debug("Caching image to: " + f.getAbsolutePath());


        try {
            String url = this.generateSiteURL();

            Document doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(TIMEOUT).get();
            Elements media = doc.select("[src]");

            Elements image = this.pickImages(media);
            return cacheImage(image.first(), f.getAbsolutePath());

        } catch (IOException ioe) {
            return false;
        }
    }

    @Override
    public LocalDate advance() {
        if (this.currentDate.isBefore(this.getLastStripOn()))
            this.currentDate = this.currentDate.plusDays(1);
        return this.currentDate;
    }
}
