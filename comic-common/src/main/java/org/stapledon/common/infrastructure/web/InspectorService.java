package org.stapledon.common.infrastructure.web;

import org.jsoup.select.Elements;

public interface InspectorService {
    void dumpMedia(Elements media);
}
