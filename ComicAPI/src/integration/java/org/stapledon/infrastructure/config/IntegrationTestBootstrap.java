package org.stapledon.infrastructure.config;

import org.stapledon.core.comic.downloader.IDailyComic;

import java.time.LocalDate;

/**
 * Implementation of IComicsBootstrap for integration tests
 * This concrete class avoids deserialization issues with the interface
 */
public class IntegrationTestBootstrap implements IComicsBootstrap {
    
    private String stripName;
    private LocalDate startDate;
    private String source;
    private String sourceIdentifier;
    private transient IDailyComic downloader;
    
    /**
     * Default constructor for deserialization
     */
    public IntegrationTestBootstrap() {
        this.stripName = "Test Comic";
        this.startDate = LocalDate.now().minusDays(30);
        this.source = "gocomics";
        this.sourceIdentifier = "testcomic";
        this.downloader = createTestDownloader();
    }
    
    /**
     * Constructor with all fields
     */
    public IntegrationTestBootstrap(String stripName, LocalDate startDate, String source, String sourceIdentifier) {
        this.stripName = stripName;
        this.startDate = startDate;
        this.source = source;
        this.sourceIdentifier = sourceIdentifier;
        this.downloader = createTestDownloader();
    }
    
    @Override
    public String stripName() {
        return stripName;
    }
    
    @Override
    public LocalDate startDate() {
        return startDate;
    }
    
    @Override
    public IDailyComic getDownloader() {
        if (downloader == null) {
            downloader = createTestDownloader();
        }
        return downloader;
    }
    
    @Override
    public String getSource() {
        return source;
    }
    
    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }
    
    /**
     * Creates a minimal test implementation of IDailyComic
     */
    private IDailyComic createTestDownloader() {
        return new IDailyComic() {
            @Override
            public IDailyComic setDate(LocalDate date) {
                return this;
            }
            
            @Override
            public IDailyComic setComic(String comicName) {
                return this;
            }
            
            @Override
            public boolean ensureCache() {
                return true;
            }
            
            @Override
            public LocalDate advance() {
                return LocalDate.now();
            }
            
            @Override
            public LocalDate getLastStripOn() {
                return LocalDate.now();
            }
            
            @Override
            public void updateComicMetadata(org.stapledon.api.dto.comic.ComicItem comicItem) {
                // No-op for testing
            }
            
            @Override
            public IDailyComic setCacheRoot(String cacheDirectory) {
                return this;
            }
        };
    }
    
    // Setters for deserialization
    
    public void setStripName(String stripName) {
        this.stripName = stripName;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }
}