package org.stapledon.core.comic.dto;

/**
 * Enumeration of possible comic retrieval status outcomes.
 */
public enum ComicRetrievalStatus {
    SUCCESS,           // Download completed successfully
    NETWORK_ERROR,     // Connection or network-related failure 
    PARSING_ERROR,     // Failed to parse website content
    COMIC_UNAVAILABLE, // Comic not available for the requested date
    AUTHENTICATION_ERROR, // Failed authentication with comic source
    STORAGE_ERROR,     // Failed to save the downloaded content
    UNKNOWN_ERROR      // Unclassified errors
}