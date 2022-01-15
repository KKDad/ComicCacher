package org.stapledon.downloader;

import com.google.common.base.Preconditions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.IWebInspector;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class KingFeatures extends DailyComic
{
    private final String website;
    private static final Logger logger = LoggerFactory.getLogger(KingFeatures.class);


    private static final String KING_FEATURES_WEB = "http://kingfeatures.com/comics/comics-a-z/?id=%s";

    /** Need to produce somethnig like
     * https://safr.kingfeatures.com/api/img.php?e=gif&s=c&file=QmFieUJsdWVzLzIwMTkvMTEvQmFieV9CbHVlcy4yMDE5MTEwN18xNTM2LmdpZg=='
     *  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36'
     *  -H 'DNT: 1'
     *  -H 'Accept: image/webp,image/apng,image/*'
     */

    public KingFeatures(IWebInspector inspector, String website) {
        super(inspector, "meta");
        Preconditions.checkNotNull(website, "website cannot be null");

        this.website = website;
    }

    @Override
    protected String generateSiteURL() {
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
     * @return Mst recent date we can get a api for
     */
    public LocalDate getLastStripOn()
    {
        return LocalDate.now();
    }


    @Override
    public void updateComicMetadata(ComicItem comicItem) {
        try {
            var url = String.format(KING_FEATURES_WEB, this.comicName.replace(' ', '_'));

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("DNT", "1")
                    .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    .timeout(TIMEOUT)
                    .get();

            Optional<Element> author = doc.select("div").stream().filter(p -> p.attributes().get("id").contains("fmw_header")).findFirst();
            author.ifPresent(element -> comicItem.author = element.text());


            // Cache the Avatar if we don't already have it
            var avatarCached = new File(String.format("%s/avatar.png", this.cacheLocation()));
            if (!avatarCached.exists())
            {
                Element featureAvatars = doc.select("img[src^=https://api.kingdigital.com/img/features/]").last();
                if (featureAvatars == null)
                    logger.error("Unable to determine site avatar");
                else {
                    cacheImage(featureAvatars, avatarCached.getAbsolutePath());
                    logger.trace("Avatar has been cached ");
                }
            }



        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        }
    }

}