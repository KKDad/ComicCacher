package org.stapledon.downloader;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.stapledon.dto.ComicItem;
import org.stapledon.web.IWebInspector;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class KingFeatures extends DailyComic
{
    private String website;

    /** Need to produce somethnig like
     * https://safr.kingfeatures.com/api/img.php?e=gif&s=c&file=QmFieUJsdWVzLzIwMTkvMTEvQmFieV9CbHVlcy4yMDE5MTEwN18xNTM2LmdpZg=='
     *  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36'
     *  -H 'DNT: 1'
     *  -H 'Accept: image/webp,image/apng,image/*'
     */

    public KingFeatures(IWebInspector inspector) {
        super(inspector, "meta");
    }

    @Override
    protected String generateSiteURL() {
        return String.format("%s/%s", this.website, this.currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    @Override
    protected Elements pickImages(Elements media) {
        Elements elements = new Elements();
        for (Element src : media) {
            if (src.tagName().equals("meta") && src.attr("property").contains("og:image"))
                elements.add(src);
        }
        webInspector.dumpMedia(elements);
        // We get back 2-3 images. The 2nd image is the hi-res version - we'll select it.
        if (elements.size() > 1) {
            Elements e = new Elements();
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

    /**
     * Each KingFeatures is accessed via a different site
     */
    public void setWebsite(String website)
    {
        this.website = website;
    }

    @Override
    public void updateComicMetadata(ComicItem comicItem) {
        // TODO: Finish this

    }
}
