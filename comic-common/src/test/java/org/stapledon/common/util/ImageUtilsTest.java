package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageUtilsTest {

    @Test
    void stripExtensionRemovesTheLastExtension() {
        assertThat(ImageUtils.stripExtension("2026-09-28.png")).isEqualTo("2026-09-28");
        assertThat(ImageUtils.stripExtension("avatar.png")).isEqualTo("avatar");
        assertThat(ImageUtils.stripExtension("a.b.png")).isEqualTo("a.b");
    }

    @Test
    void stripExtensionLeavesANameWithoutADotUnchanged() {
        assertThat(ImageUtils.stripExtension("noext")).isEqualTo("noext");
    }
}
