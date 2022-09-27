package org.stapledon.downloader;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.IWebInspector;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
public class GoComics extends DailyComic {
    public GoComics(IWebInspector inspector) {
        super(inspector, "[src]");
    }


    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     *
     * @return Mst recent date we can get a api for
     */
    public LocalDate getLastStripOn() {
        return LocalDate.now();
    }

    /**
     * Generate the URL for a specific date.
     */
    @Override
    protected String generateSiteURL() {
        return String.format("http://www.gocomics.com/%s/%s/", this.comicNameParsed, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))).toLowerCase();
    }

    /**
     * Link to the About the api page
     *
     * @return URL where we can get the about information for this strip
     */
    private String generateAboutUTL() {
        return String.format("https://www.gocomics.com/%s/about", this.comicNameParsed);
    }

    public void updateComicMetadata(ComicItem comicItem) {
        try {
            String url = this.generateAboutUTL();
            log.info("Getting Comic Description from {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("DNT", "1")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .timeout(TIMEOUT)
                    .get();
            // Fragile, however there appears to only be one "section" class and the description seems to be the
            // first div inside it.
            comicItem.description = doc.select("section").select("div").get(0).text();


            Optional<Element> author = doc.select("span").stream().filter(p -> p.attributes().get("class").contains("media-subheading")).findFirst();
            author.ifPresent(element -> comicItem.author = element.text());

            // Cache the Avatar if we don't already have it
            var avatarCached = new File(String.format("%s/avatar.png", this.cacheLocation()));
            if (!avatarCached.exists()) {
                Element featureAvatars = doc.select("img[src^=https://avatar.amuniversal.com/feature_avatars]").last();
                comicItem.avatarAvailable = cacheImage(featureAvatars, avatarCached.getAbsolutePath());
                log.trace("Avatar has been cached ");
            } else {
                comicItem.avatarAvailable = true;
            }


        } catch (IOException | Selector.SelectorParseException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Determines which links represent the api image that we should utils
     *
     * @param media list of image links to choose from
     */
    @Override
    protected Elements pickImages(Elements media) {
        var elements = new Elements();
        for (Element src : media) {
            if (src.tagName().equals("img") && src.attr("abs:src").contains("assets.amuniversal.com"))
                elements.add(src);
        }
        webInspector.dumpMedia(elements);
        // We get back 2-3 images. The 2nd image is the hi-res version - we'll select it.
        if (elements.size() > 1) {
            var e = new Elements();
            e.add(elements.get(1));
            return e;

        }
        return elements;
    }
}
