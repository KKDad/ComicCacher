package org.stapledon.api.dto.comic;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ComicStorageMetricsTest {

    @Test
    void testComicStorageMetricsBuilderAndGetters() {
        // Setup test data
        String comicName = "TestComic";
        long storageBytes = 1024 * 1024 * 10; // 10MB
        int imageCount = 365;
        double averageImageSize = 28000.0;
        String mostRecentAccess = "2023-05-01T10:15:30";
        int accessCount = 42;
        double hitRatio = 0.85;
        Map<String, Long> storageByYear = new HashMap<>();
        storageByYear.put("2022", 5_242_880L); // 5MB
        storageByYear.put("2023", 5_242_880L); // 5MB
        long downloadTime = 1500L;

        // Create metrics using builder
        ComicStorageMetrics metrics = ComicStorageMetrics.builder()
                .comicName(comicName)
                .storageBytes(storageBytes)
                .imageCount(imageCount)
                .averageImageSize(averageImageSize)
                .mostRecentAccess(mostRecentAccess)
                .accessCount(accessCount)
                .hitRatio(hitRatio)
                .storageByYear(storageByYear)
                .downloadTime(downloadTime)
                .build();

        // Verify field values
        assertEquals(comicName, metrics.getComicName());
        assertEquals(storageBytes, metrics.getStorageBytes());
        assertEquals(imageCount, metrics.getImageCount());
        assertEquals(averageImageSize, metrics.getAverageImageSize());
        assertEquals(mostRecentAccess, metrics.getMostRecentAccess());
        assertEquals(accessCount, metrics.getAccessCount());
        assertEquals(hitRatio, metrics.getHitRatio());
        assertEquals(storageByYear, metrics.getStorageByYear());
        assertEquals(downloadTime, metrics.getDownloadTime());
    }

    @Test
    void testEqualsAndHashCode() {
        // Create two identical metrics
        Map<String, Long> storageByYear1 = new HashMap<>();
        storageByYear1.put("2022", 5_242_880L);
        
        Map<String, Long> storageByYear2 = new HashMap<>();
        storageByYear2.put("2022", 5_242_880L);
        
        ComicStorageMetrics metrics1 = ComicStorageMetrics.builder()
                .comicName("TestComic")
                .storageBytes(1024 * 1024 * 10)
                .imageCount(365)
                .averageImageSize(28000.0)
                .mostRecentAccess("2023-05-01T10:15:30")
                .accessCount(42)
                .hitRatio(0.85)
                .storageByYear(storageByYear1)
                .downloadTime(1500L)
                .build();
        
        ComicStorageMetrics metrics2 = ComicStorageMetrics.builder()
                .comicName("TestComic")
                .storageBytes(1024 * 1024 * 10)
                .imageCount(365)
                .averageImageSize(28000.0)
                .mostRecentAccess("2023-05-01T10:15:30")
                .accessCount(42)
                .hitRatio(0.85)
                .storageByYear(storageByYear2)
                .downloadTime(1500L)
                .build();
        
        // Create a different metric
        ComicStorageMetrics metrics3 = ComicStorageMetrics.builder()
                .comicName("DifferentComic")
                .storageBytes(1024 * 1024 * 5)
                .imageCount(180)
                .averageImageSize(28500.0)
                .mostRecentAccess("2023-05-02T10:15:30")
                .accessCount(21)
                .hitRatio(0.75)
                .storageByYear(new HashMap<>())
                .downloadTime(1200L)
                .build();
        
        // Test equals and hashCode
        assertEquals(metrics1, metrics2);
        assertEquals(metrics1.hashCode(), metrics2.hashCode());
        assertNotEquals(metrics1, metrics3);
        assertNotEquals(metrics1.hashCode(), metrics3.hashCode());
    }
    
    @Test
    void testSetters() {
        // Create initial metrics
        ComicStorageMetrics metrics = new ComicStorageMetrics();
        
        // Test setters
        metrics.setComicName("UpdatedComic");
        metrics.setStorageBytes(2048576); // 2MB
        metrics.setImageCount(100);
        metrics.setAverageImageSize(20485.76);
        metrics.setMostRecentAccess("2023-05-10T10:00:00");
        metrics.setAccessCount(50);
        metrics.setHitRatio(0.9);
        
        Map<String, Long> storageByYear = new HashMap<>();
        storageByYear.put("2023", 2048576L);
        metrics.setStorageByYear(storageByYear);
        
        metrics.setDownloadTime(800L);
        
        // Verify updated values
        assertEquals("UpdatedComic", metrics.getComicName());
        assertEquals(2048576, metrics.getStorageBytes());
        assertEquals(100, metrics.getImageCount());
        assertEquals(20485.76, metrics.getAverageImageSize());
        assertEquals("2023-05-10T10:00:00", metrics.getMostRecentAccess());
        assertEquals(50, metrics.getAccessCount());
        assertEquals(0.9, metrics.getHitRatio());
        assertEquals(storageByYear, metrics.getStorageByYear());
        assertEquals(800L, metrics.getDownloadTime());
    }
    
    @Test
    void testNoArgsConstructor() {
        // Create metrics using no-args constructor
        ComicStorageMetrics metrics = new ComicStorageMetrics();
        
        // Default values should be null or 0
        assertEquals(null, metrics.getComicName());
        assertEquals(0, metrics.getStorageBytes());
        assertEquals(0, metrics.getImageCount());
        assertEquals(0.0, metrics.getAverageImageSize());
        assertEquals(null, metrics.getMostRecentAccess());
        assertEquals(0, metrics.getAccessCount());
        assertEquals(0.0, metrics.getHitRatio());
        assertEquals(null, metrics.getStorageByYear());
        assertEquals(0, metrics.getDownloadTime());
    }
}