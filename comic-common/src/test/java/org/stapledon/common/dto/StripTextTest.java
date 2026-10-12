package org.stapledon.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StripTextTest {

    @Test
    void trimsTheTextAndTurnsBlankValuesIntoNull() {
        StripText text = new StripText("  Jon: Hi  ", " ");

        assertThat(text.transcript()).isEqualTo("Jon: Hi");
        assertThat(text.altText()).isNull();
    }

    @Test
    void isEmptyOnlyWithNeitherValue() {
        assertThat(StripText.NONE.isEmpty()).isTrue();
        assertThat(new StripText("", null).isEmpty()).isTrue();
        assertThat(StripText.ofAltText("Caption").isEmpty()).isFalse();
        assertThat(StripText.ofTranscript("Jon: Hi").isEmpty()).isFalse();
    }

    @Test
    void orNoneReplacesNull() {
        assertThat(StripText.orNone(null)).isEqualTo(StripText.NONE);
        assertThat(StripText.orNone(StripText.ofAltText("Caption"))).isEqualTo(StripText.ofAltText("Caption"));
    }

    @Test
    void saveDataAndDownloadResultsNeverHoldNullText() {
        assertThat(ComicSaveData.builder().imageData(new byte[] {1}).build().text()).isEqualTo(StripText.NONE);
        assertThat(ComicDownloadResult.success(null, new byte[] {1}).getText()).isEqualTo(StripText.NONE);
        assertThat(ComicDownloadResult.failure(null, "boom").getText()).isEqualTo(StripText.NONE);
    }
}
