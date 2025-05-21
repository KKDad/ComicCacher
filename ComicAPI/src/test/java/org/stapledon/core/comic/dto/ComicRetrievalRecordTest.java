package org.stapledon.core.comic.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ComicRetrievalRecordTest {
    
    @Test
    void successFactoryMethodCreatesCorrectRecord() {
        // Arrange
        String comicName = "TestComic";
        LocalDate comicDate = LocalDate.of(2023, 1, 1);
        String source = "gocomics";
        long durationMs = 500;
        long imageSize = 10240;
        
        // Act
        ComicRetrievalRecord record = ComicRetrievalRecord.success(
                comicName, comicDate, source, durationMs, imageSize);
        
        // Assert
        assertNotNull(record);
        assertEquals(comicName, record.getComicName());
        assertEquals(comicDate, record.getComicDate());
        assertEquals(source, record.getSource());
        assertEquals(ComicRetrievalStatus.SUCCESS, record.getStatus());
        assertEquals(durationMs, record.getRetrievalDurationMs());
        assertEquals(imageSize, record.getImageSize());
        assertNull(record.getErrorMessage());
        assertNull(record.getHttpStatusCode());
        assertEquals("TestComic_2023-01-01", record.getId());
    }
    
    @Test
    void failureFactoryMethodCreatesCorrectRecord() {
        // Arrange
        String comicName = "TestComic";
        LocalDate comicDate = LocalDate.of(2023, 1, 1);
        String source = "gocomics";
        ComicRetrievalStatus status = ComicRetrievalStatus.NETWORK_ERROR;
        String errorMessage = "Connection timeout";
        long durationMs = 2000;
        Integer httpStatusCode = 500;
        
        // Act
        ComicRetrievalRecord record = ComicRetrievalRecord.failure(
                comicName, comicDate, source, status, errorMessage, durationMs, httpStatusCode);
        
        // Assert
        assertNotNull(record);
        assertEquals(comicName, record.getComicName());
        assertEquals(comicDate, record.getComicDate());
        assertEquals(source, record.getSource());
        assertEquals(status, record.getStatus());
        assertEquals(errorMessage, record.getErrorMessage());
        assertEquals(durationMs, record.getRetrievalDurationMs());
        assertEquals(httpStatusCode, record.getHttpStatusCode());
        assertNull(record.getImageSize());
        assertEquals("TestComic_2023-01-01", record.getId());
    }
    
    @Test
    void generateIdCreatesConsistentIdForSameComic() {
        // Arrange
        String comicName = "TestComic";
        LocalDate comicDate = LocalDate.of(2023, 1, 1);
        String source = "gocomics";
        long durationMs = 500;
        long imageSize = 10240;
        
        // Act
        ComicRetrievalRecord record1 = ComicRetrievalRecord.success(
                comicName, comicDate, source, durationMs, imageSize);
        
        ComicRetrievalRecord record2 = ComicRetrievalRecord.success(
                comicName, comicDate, source, durationMs, imageSize);
        
        // Assert
        assertEquals(record1.getId(), record2.getId());
        assertEquals("TestComic_2023-01-01", record1.getId());
    }
}