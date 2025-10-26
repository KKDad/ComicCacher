package org.stapledon.engine.downloader;

import com.google.common.base.Preconditions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.InspectorService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
public class ComicsKingdom extends DailyComic {

    private static final String ABOUT_SITE_STRING = "https://comicskingdom.com/%s/about";
    private final String website;

    public ComicsKingdom(InspectorService inspector, String website) {
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
    protected Optional<String> extractComicImage(String comicUrl) {
        try {
            Document doc = Jsoup.connect(comicUrl)
                    .userAgent(USER_AGENT)
                    .header("DNT", "1")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .timeout(TIMEOUT)
                    .get();

            // ComicsKingdom often uses a meta tag for the image
            Elements metaTags = doc.select("meta[property=og:image]");
            if (!metaTags.isEmpty()) {
                // We get back 2-3 images. The 2nd image is the hi-res version - we'll select it.
                if (metaTags.size() > 1) {
                    return Optional.ofNullable(metaTags.get(1).attr("content"));
                }
                return Optional.ofNullable(metaTags.first().attr("content"));
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Error extracting comic image for {}: {}", comicUrl, e.getMessage());
            return Optional.empty();
        }
    }



    /**
     * Determines when the latest published image is. Some comics are only available on the web a couple days or
     * a week after they were published in print.
     *
     * @return Most recent date we can get an image for
     */
    @Override
    public LocalDate getLastStripOn() {
        return LocalDate.now();
    }

    @Override
    public void close() {
    }

    public void updateComicMetadata(ComicItem comicItem) {
        try {
            var url = String.format(ABOUT_SITE_STRING, this.comicName.replace(' ', '-'));

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("DNT", "1")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .timeout(TIMEOUT)
                    .get();

            var stripName = doc.select("div.feature-header h1.card-title span.card-title__link span").text();
            var author = doc.select("div.feature-header div.card-content").text();
            if (!stripName.isEmpty() && !author.isEmpty()) {
                comicItem.setAuthor(String.format("%s by %s", stripName, author.replace("By ", "").trim()));
                log.info("Author={}", comicItem.getAuthor());
            }

            // Cache the Avatar if we don't already have it
            var avatarCached = new File(String.format("%s/avatar.png", this.cacheLocation()));
            if (!avatarCached.exists()) {
                Element featureAvatars = doc.select("img[src^=https://api.kingdigital.com/img/features/]").last();
                if (featureAvatars == null)
                    log.error("Unable to determine site avatar");
                else {
                    cacheImage(featureAvatars.attr("abs:src"), avatarCached.getAbsolutePath());
                    log.trace("Avatar has been cached");
                }
            }


        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
    }


}
