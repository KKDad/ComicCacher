package org.stapledon.common.dto;

import lombok.Builder;

/**
 * Data transfer object for comic strip save operations.
 * Bundles image data with optional metadata so the save interface
 * remains stable as new metadata fields are added.
 */
@Builder
public record ComicSaveData(
        byte[] imageData,
        StripText text,
        Integer stripNumber
) {

    /**
     * A null {@code text} becomes {@link StripText#NONE}.
     */
    public ComicSaveData {
        text = StripText.orNone(text);
    }
}
