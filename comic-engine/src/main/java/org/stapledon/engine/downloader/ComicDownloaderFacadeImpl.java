package org.stapledon.engine.downloader;

import org.springframework.stereotype.Component;
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.core.comic.dto.ComicDownloadRequest;
import org.stapledon.core.comic.dto.ComicDownloadResult;
import org.stapledon.core.comic.dto.ComicRetrievalRecord;
import org.stapledon.core.comic.dto.ComicRetrievalStatus;
import org.stapledon.core.comic.service.RetrievalStatusService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the ComicDownloaderFacade interface.
 * Coordinates comic downloading operations using registered downloader strategies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComicDownloaderFacadeImpl implements ComicDownloaderFacade {

    private final Map<String, ComicDownloaderStrategy> downloaderStrategies = new ConcurrentHashMap<>();
    private final RetrievalStatusService retrievalStatusService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadComic(ComicDownloadRequest request) {
        LocalDateTime startTime = LocalDateTime.now();
        
        // Attempt to find the appropriate strategy
        ComicDownloaderStrategy strategy = downloaderStrategies.get(request.getSource());
        if (strategy == null) {
            String errorMsg = String.format("No downloader strategy registered for source: %s", request.getSource());
            log.error(errorMsg);
            recordFailure(request, ComicRetrievalStatus.UNKNOWN_ERROR, errorMsg, startTime, null);
            return ComicDownloadResult.failure(request, errorMsg);
        }

        try {
            log.debug("Downloading comic {} for date {} from source {}", 
                    request.getComicName(), request.getDate(), request.getSource());
            ComicDownloadResult result = strategy.downloadComic(request);
            
            // Record the result
            if (result.isSuccessful()) {
                recordSuccess(request, startTime, result.getImageData().length);
            } else {
                recordFailure(request, ComicRetrievalStatus.COMIC_UNAVAILABLE, 
                        result.getErrorMessage(), startTime, null);
            }
            
            return result;
        } catch (Exception e) {
            String errorMessage = String.format("Error downloading comic %s for date %s: %s", 
                    request.getComicName(), request.getDate(), e.getMessage());
            log.error(errorMessage, e);
            ComicRetrievalStatus status = determineErrorStatus(e);
            recordFailure(request, status, errorMessage, startTime, null);
            return ComicDownloadResult.failure(request, errorMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<byte[]> downloadAvatar(int comicId, String comicName, String source, String sourceIdentifier) {
        ComicDownloaderStrategy strategy = downloaderStrategies.get(source);
        if (strategy == null) {
            log.error("No downloader strategy registered for source: {}", source);
            return Optional.empty();
        }

        try {
            log.debug("Downloading avatar for comic {} from source {}", comicName, source);
            return strategy.downloadAvatar(comicId, comicName, sourceIdentifier);
        } catch (Exception e) {
            log.error("Error downloading avatar for comic {}: {}", comicName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ComicDownloadResult> downloadLatestComics(ComicConfig config) {
        return downloadComicsForDate(config, LocalDate.now());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ComicDownloadResult> downloadComicsForDate(ComicConfig config, LocalDate date) {
        List<ComicDownloadResult> results = new ArrayList<>();
        
        if (config == null || config.getComics() == null) {
            log.warn("Comic configuration is null or empty");
            return results;
        }

        for (ComicItem comic : config.getComics()) {
            ComicDownloadRequest request = ComicDownloadRequest.builder()
                    .comicId(comic.getId())
                    .comicName(comic.getName())
                    .source(comic.getSource())
                    .sourceIdentifier(comic.getSourceIdentifier())
                    .date(date)
                    .build();
            
            ComicDownloadResult result = downloadComic(request);
            results.add(result);
            
            if (!result.isSuccessful()) {
                log.warn("Failed to download comic {}: {}", comic.getName(), result.getErrorMessage());
            }
        }
        
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerDownloaderStrategy(String source, ComicDownloaderStrategy strategy) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("Source cannot be null or empty");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        
        log.info("Registering downloader strategy for source: {}", source);
        downloaderStrategies.put(source, strategy);
    }
    
    private ComicRetrievalStatus determineErrorStatus(Exception e) {
        if (e instanceof java.net.ConnectException || 
            e instanceof java.net.SocketTimeoutException ||
            e instanceof java.io.IOException) {
            return ComicRetrievalStatus.NETWORK_ERROR;
        } else if (e instanceof org.jsoup.HttpStatusException) {
            return ComicRetrievalStatus.PARSING_ERROR;
        } else if (e instanceof java.nio.file.AccessDeniedException) {
            return ComicRetrievalStatus.STORAGE_ERROR;
        } else {
            return ComicRetrievalStatus.UNKNOWN_ERROR;
        }
    }
    
    private void recordSuccess(ComicDownloadRequest request, LocalDateTime startTime, long imageSize) {
        long durationMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        
        ComicRetrievalRecord record = ComicRetrievalRecord.success(
                request.getComicName(),
                request.getDate(),
                request.getSource(),
                durationMs,
                imageSize
        );
        
        retrievalStatusService.recordRetrievalResult(record);
    }
    
    private void recordFailure(
            ComicDownloadRequest request, 
            ComicRetrievalStatus status, 
            String errorMessage, 
            LocalDateTime startTime, 
            Integer httpStatusCode) {
        
        long durationMs = Duration.between(startTime, LocalDateTime.now()).toMillis();
        
        ComicRetrievalRecord record = ComicRetrievalRecord.failure(
                request.getComicName(),
                request.getDate(),
                request.getSource(),
                status,
                errorMessage,
                durationMs,
                httpStatusCode
        );
        
        retrievalStatusService.recordRetrievalResult(record);
    }
}