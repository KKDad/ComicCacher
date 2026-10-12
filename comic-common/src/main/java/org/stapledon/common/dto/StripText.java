package org.stapledon.common.dto;

/**
 * The text a source gives with a strip: its transcript and its alt (hover) text. Every downloader returns one, and it is carried unchanged
 * through the download result, the save and the metadata sidecar, so a source starts supplying a kind of text by filling it in here.
 * Blank values are kept as null.
 */
public record StripText(String transcript, String altText) {

    /** A strip with no text. */
    public static final StripText NONE = new StripText(null, null);

    /**
     * Trims both values and turns blank ones into null.
     */
    public StripText {
        transcript = clean(transcript);
        altText = clean(altText);
    }

    /**
     * Just a transcript.
     */
    public static StripText ofTranscript(String transcript) {
        return new StripText(transcript, null);
    }

    /**
     * Just alt text.
     */
    public static StripText ofAltText(String altText) {
        return new StripText(null, altText);
    }

    /**
     * {@code text}, or {@link #NONE} when it is null.
     */
    public static StripText orNone(StripText text) {
        return text != null ? text : NONE;
    }

    /**
     * True when the strip has neither a transcript nor alt text.
     */
    public boolean isEmpty() {
        return transcript == null && altText == null;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
