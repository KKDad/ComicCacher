package org.stapledon.downloader;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.IWebInspector;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ComicsKingdom extends DailyComic {

    private static final String ABOUT_SITE_STRING = "https://comicskingdom.com/%s/about";
    private final String website;

    public ComicsKingdom(IWebInspector inspector, String website) {
        super(inspector, "meta");
        Preconditions.checkNotNull(website, "website cannot be null");

        this.website = website;
    }

    @Override
    protected String generateSiteURL() {
        // https://comicskingdom.com//daddy-daze/2022-09-11
        return String.format("%s/%s", this.website, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    @Override
    protected Elements pickImages(Elements media) {
        var elements = new Elements();
        for (Element src : media) {
            if (src.tagName().equals("meta") && src.attr("property").contains("og:image"))
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

    /**
     * Determines when the latest published image it. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     *
     * @return Mst recent date we can get a api for
     */
    @Override
    public LocalDate getLastStripOn() {
        return LocalDate.now();
    }

    @Override
    public void updateComicMetadata(ComicItem comicItem) {
        try {
            var url = String.format(ABOUT_SITE_STRING, this.comicName.replace(' ', '-'));

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("DNT", "1")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .timeout(TIMEOUT)
                    .get();

            var author = doc.select("title").text();
            if (!author.isEmpty()) {
                comicItem.author = author.substring(author.indexOf("|")).replace("|", "").trim();
                log.info("Author={}", comicItem.author);
            }


            // Cache the Avatar if we don't already have it
            var avatarCached = new File(String.format("%s/avatar.png", this.cacheLocation()));
            if (!avatarCached.exists()) {
                Element featureAvatars = doc.select("img[src^=https://api.kingdigital.com/img/features/]").last();
                if (featureAvatars == null)
                    log.error("Unable to determine site avatar");
                else {
                    cacheImage(featureAvatars, avatarCached.getAbsolutePath());
                    log.trace("Avatar has been cached ");
                }
            }


        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
    }


}
