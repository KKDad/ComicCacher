package org.stapledon.common.infrastructure.web;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.extern.slf4j.Slf4j;

/**
 * Methods for debugging new site retrievals
 */
@Slf4j
public class WebInspectorImpl implements WebInspector {
    public static final String ABS_SRC = "abs:src";
    public static final String CONTENT = "content";

    @Override
    public void dumpLinks(Document doc) {
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");
        Elements imports = doc.select("link[href]");

        dumpMedia(media);
        dumpImports(imports);
        dumpLinks(links);
    }

    @Override
    public void dumpLinks(Elements links) {
        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            print(" * a: <%s>  (%s)", link.attr(ABS_SRC), trim(link.text(), 35));
        }
    }

    @Override
    public void dumpImports(Elements imports) {
        print("\nImports: (%d)", imports.size());
        for (Element link : imports) {
            print(" * %s <%s> (%s)", link.tagName(), link.attr("abs:href"), link.attr("rel"));
        }
    }

    @Override
    public void dumpMedia(Elements media) {
        print("Media: (%d)", media.size());
        for (Element src : media) {
            if (src.tagName().equals("img"))
                print(" * %s: <%s> %sx%s (%s)",
                        src.tagName(), src.attr(ABS_SRC), src.attr("width"), src.attr("height"),
                        trim(src.attr("alt"), 20));
            else {
                print(" * %s: <%s>", src.tagName(), src.attr(CONTENT));
            }
        }
    }

    /**
     * Utility method to log a single line to Log4j if logging at debug is enabled.
     *
     * @param msg  Line to Log
     * @param args Parameter to Line
     */
    private void print(String msg, Object... args) {
        if (log.isDebugEnabled())
            log.debug(String.format(msg, args));
    }

    private String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }
}
