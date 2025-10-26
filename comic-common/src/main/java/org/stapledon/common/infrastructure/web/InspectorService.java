package org.stapledon.common.infrastructure.web;

import org.jsoup.select.Elements;

public interface InspectorService {
    void dumpLinks(Elements links);

    void dumpImports(Elements imports);

    void dumpMedia(Elements media);
}
