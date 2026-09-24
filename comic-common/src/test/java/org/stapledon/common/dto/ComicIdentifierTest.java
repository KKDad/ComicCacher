package org.stapledon.common.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ComicIdentifierTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Mother Goose & Grimm | MotherGoose&Grimm",
            "Sherman's Lagoon     | Sherman'sLagoon",
            "Frank-And-Ernest     | Frank-And-Ernest"
    })
    void getDirectoryName_removesSpacesAndKeepsOtherCharacters(String name, String expected) {
        assertThat(new ComicIdentifier(7, name).getDirectoryName()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", ".", "..", " . . ", "../etc", "a/b", "a\\b"})
    void getDirectoryName_fallsBackToIdForUnsafeNames(String name) {
        assertThat(new ComicIdentifier(7, name).getDirectoryName()).isEqualTo("comic_7");
    }

    @Test
    void getDirectoryName_fallsBackToIdForNullName() {
        assertThat(new ComicIdentifier(7, null).getDirectoryName()).isEqualTo("comic_7");
    }
}
