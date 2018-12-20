package org.stapledon.cacher;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDate;

public abstract class DailyComic
{

    static final String ABS_SRC = "abs:src";
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
    static final int TIMEOUT = 5 * 1000;

    static final String CACHE_DIRECTORY = "Z:/ComicCache";

    private final Logger logger = Logger.getLogger(DailyComic.class);


    void cacheImage(Element sourceImageElement, String destinationFile) throws IOException
    {
        OutputStream os = null;
        try {
            URL urlImage = new URL(sourceImageElement.attr(ABS_SRC));
            logger.info("Downloading Image from: " + urlImage);
            try (InputStream in = urlImage.openStream()) {
                byte[] buffer = new byte[4096];
                int n;
                os = new FileOutputStream(destinationFile);
                while ((n = in.read(buffer)) != -1) {
                    os.write(buffer, 0, n);
                }
            }
            logger.trace("Image saved");
        } catch (IOException e) {
            logger.error("Failed to cache Image:", e);
        } finally {
            if (os != null)
                os.close();
        }
    }

    // *********************************************************************************************************
    // Helper Methods for debugging new site retrievals
    // *********************************************************************************************************

    private void dumpLinks(Document doc) {
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");
        Elements imports = doc.select("link[href]");

        dumpMedia(media);
        dumpImports(imports);
        dumpLinks(links);
    }

    void dumpLinks(Elements links) {
        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            print(" * a: <%s>  (%s)", link.attr(ABS_SRC), trim(link.text(), 35));
        }
    }

    void dumpImports(Elements imports) {
        print("\nImports: (%d)", imports.size());
        for (Element link : imports) {
            print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"), link.attr("rel"));
        }
    }

    void dumpMedia(Elements media) {
        print("\nMedia: (%d)", media.size());
        for (Element src : media) {
            if (src.tagName().equals("img"))
                print(" * %s: <%s> %sx%s (%s)",
                        src.tagName(), src.attr(ABS_SRC), src.attr("width"), src.attr("height"),
                        trim(src.attr("alt"), 20));
            else
                print(" * %s: <%s>", src.tagName(), src.attr(ABS_SRC));
        }
    }

    /**
     * Utility method to log a sinlge line to Log4j is logging at debug is enabled.
     * @param msg Line to Log
     * @param args Parameter to Line
     */
    private void print(String msg, Object... args) {
        if (logger.isDebugEnabled())
            logger.debug(String.format(msg, args));
    }

    private String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }


    public abstract LocalDate advance();
}
