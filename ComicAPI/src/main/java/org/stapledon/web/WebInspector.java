package org.stapledon.web;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public interface WebInspector {
    void dumpLinks(Document doc);

    void dumpLinks(Elements links);

    void dumpImports(Elements imports);

    void dumpMedia(Elements media);
}
