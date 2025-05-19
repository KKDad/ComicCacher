package org.stapledon.infrastructure.storage;

import com.google.common.io.Files;

import org.springframework.stereotype.Component;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.common.util.Direction;
import org.stapledon.common.util.ImageUtils;
import org.stapledon.events.CacheMissEvent;
import org.stapledon.infrastructure.config.properties.CacheProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the Comic Storage Facade that abstracts all filesystem operations
 * related to comic storage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComicStorageFacadeImpl implements ComicStorageFacade {
    
    private static final String COMBINE_PATH = "%s/%s";
    private static final int WARNING_TIME_MS = 100;
    private static final String AVATAR_FILE = "avatar.png";
    
    private final CacheProperties cacheProperties;
    
    // Event handling for cache misses
    private final List<Consumer<CacheMissEvent>> cacheMissListeners = new CopyOnWriteArrayList<>();
    
    @Override
    public void addCacheMissListener(Consumer<CacheMissEvent> listener) {
        cacheMissListeners.add(listener);
    }
    
    private void fireCacheMissEvent(int comicId, String comicName, LocalDate date) {
        CacheMissEvent event = new CacheMissEvent(comicId, comicName, date);
        cacheMissListeners.forEach(listener -> listener.accept(event));
    }
    
    /**
     * Gets a directory name for a comic - uses the comic name if available, 
     * otherwise falls back to the comic ID. This ensures we can handle comics with null names.
     * 
     * @param comicId the comic ID
     * @param comicName the comic name (can be null)
     * @return a string to use as the directory name
     */
    private String getComicNameParsed(int comicId, String comicName) {
        if (comicName == null) {
            return "comic_" + comicId;
        }
        return comicName.replace(" ", "");
    }
    
    @Override
    public boolean saveComicStrip(int comicId, String comicName, LocalDate date, byte[] imageData) {
        // Only validate the essential parameters
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(imageData, "imageData cannot be null");
        
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        
        // Create directory structure if it doesn't exist
        String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
        File directory = new File(String.format("%s/%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed, yearPath));
        if (!directory.exists() && !directory.mkdirs()) {
            log.error("Failed to create directory: {}", directory.getAbsolutePath());
            return false;
        }
        
        // Create the file
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File file = new File(String.format("%s/%s.png", directory.getAbsolutePath(), filename));
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageData);
            return true;
        } catch (IOException e) {
            log.error("Failed to save comic strip for {} on {}: {}", comicName, date, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean saveAvatar(int comicId, String comicName, byte[] imageData) {
        Objects.requireNonNull(imageData, "imageData cannot be null");
        
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        
        // Create directory structure if it doesn't exist
        File directory = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        if (!directory.exists() && !directory.mkdirs()) {
            log.error("Failed to create directory: {}", directory.getAbsolutePath());
            return false;
        }
        
        // Create the file
        File file = new File(String.format("%s/%s", directory.getAbsolutePath(), AVATAR_FILE));
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageData);
            return true;
        } catch (IOException e) {
            log.error("Failed to save avatar for {}: {}", comicName, e.getMessage());
            return false;
        }
    }
    
    @Override
    public Optional<ImageDto> getComicStrip(int comicId, String comicName, LocalDate date) {
        Objects.requireNonNull(date, "date cannot be null");
        
        // First check if the image exists in cache
        if (!comicStripExists(comicId, comicName, date)) {
            // Notify listeners of cache miss
            fireCacheMissEvent(comicId, comicName, date);
            return Optional.empty();
        }
        
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File file = new File(String.format("%s/%s/%s/%s.png", getCacheRoot().getAbsolutePath(), comicNameParsed, yearPath, filename));
        
        try {
            return Optional.of(ImageUtils.getImageDto(file));
        } catch (IOException e) {
            log.error("Failed to read comic strip for {} on {}: {}", comicName, date, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<ImageDto> getAvatar(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File file = new File(String.format("%s/%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed, AVATAR_FILE));
        
        if (!file.exists()) {
            log.error("Avatar not found for {}", comicName);
            return Optional.empty();
        }
        
        try {
            return Optional.of(ImageUtils.getImageDto(file));
        } catch (IOException e) {
            log.error("Failed to read avatar for {}: {}", comicName, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<LocalDate> getNextDateWithComic(int comicId, String comicName, LocalDate fromDate) {
        Objects.requireNonNull(fromDate, "fromDate cannot be null");
        
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File root = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        
        if (!root.exists()) {
            return Optional.empty();
        }
        
        // Get the newest date first as a boundary
        Optional<LocalDate> newestOpt = getNewestDateWithComic(comicId, comicName);
        if (newestOpt.isEmpty()) {
            return Optional.empty();
        }
        
        LocalDate newest = newestOpt.get();
        LocalDate nextCandidate = fromDate.plusDays(1);
        
        while (nextCandidate.isBefore(newest) || nextCandidate.isEqual(newest)) {
            String yearPath = nextCandidate.format(DateTimeFormatter.ofPattern("yyyy"));
            String filename = nextCandidate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File file = new File(String.format("%s/%s/%s.png", root.getAbsolutePath(), yearPath, filename));
            
            if (file.exists()) {
                return Optional.of(nextCandidate);
            }
            
            nextCandidate = nextCandidate.plusDays(1);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<LocalDate> getPreviousDateWithComic(int comicId, String comicName, LocalDate fromDate) {
        Objects.requireNonNull(fromDate, "fromDate cannot be null");
        
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File root = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        
        if (!root.exists()) {
            return Optional.empty();
        }
        
        // Get the oldest date first as a boundary
        Optional<LocalDate> oldestOpt = getOldestDateWithComic(comicId, comicName);
        if (oldestOpt.isEmpty()) {
            return Optional.empty();
        }
        
        LocalDate oldest = oldestOpt.get();
        LocalDate prevCandidate = fromDate.minusDays(1);
        
        while (prevCandidate.isAfter(oldest) || prevCandidate.isEqual(oldest)) {
            String yearPath = prevCandidate.format(DateTimeFormatter.ofPattern("yyyy"));
            String filename = prevCandidate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File file = new File(String.format("%s/%s/%s.png", root.getAbsolutePath(), yearPath, filename));
            
            if (file.exists()) {
                return Optional.of(prevCandidate);
            }
            
            prevCandidate = prevCandidate.minusDays(1);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<LocalDate> getNewestDateWithComic(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        
        ComicItem comicItem = ComicItem.builder()
                .id(comicId)
                .name(comicNameParsed)
                .build();
        
        File newest = findNewest(comicItem);
        if (newest == null) {
            return Optional.empty();
        }
        
        try {
            String filename = Files.getNameWithoutExtension(newest.getName());
            return Optional.of(LocalDate.parse(filename, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            log.error("Failed to parse date from filename: {}", newest.getName());
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<LocalDate> getOldestDateWithComic(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        
        ComicItem comicItem = ComicItem.builder()
                .id(comicId)
                .name(comicNameParsed)
                .build();
        
        File oldest = findOldest(comicItem);
        if (oldest == null) {
            return Optional.empty();
        }
        
        try {
            String filename = Files.getNameWithoutExtension(oldest.getName());
            return Optional.of(LocalDate.parse(filename, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            log.error("Failed to parse date from filename: {}", oldest.getName());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean comicStripExists(int comicId, String comicName, LocalDate date) {
        Objects.requireNonNull(date, "date cannot be null");
        
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        String yearPath = date.format(DateTimeFormatter.ofPattern("yyyy"));
        String filename = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File file = new File(String.format("%s/%s/%s/%s.png", getCacheRoot().getAbsolutePath(), comicNameParsed, yearPath, filename));
        
        return file.exists();
    }
    
    @Override
    public boolean deleteComic(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File directory = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        
        if (!directory.exists()) {
            log.warn("Comic directory does not exist for {}", comicName);
            return false;
        }
        
        return deleteDirectory(directory);
    }
    
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        log.error("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        
        return directory.delete();
    }
    
    @Override
    public boolean purgeOldImages(int comicId, String comicName, int daysToKeep) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File comicRoot = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        
        if (!comicRoot.exists()) {
            log.warn("Comic directory does not exist for {}", comicName);
            return false;
        }
        
        // Get all year directories
        File[] yearDirs = comicRoot.listFiles(File::isDirectory);
        if (yearDirs == null) {
            return true;
        }
        
        boolean success = true;
        
        for (File yearDir : yearDirs) {
            if (yearDir.getName().equals("@eaDir")) {
                continue;
            }
            
            File[] comicFiles = yearDir.listFiles(file -> file.isFile() && file.getName().endsWith(".png"));
            if (comicFiles == null) {
                continue;
            }
            
            for (File comicFile : comicFiles) {
                try {
                    String filename = Files.getNameWithoutExtension(comicFile.getName());
                    LocalDate comicDate = LocalDate.parse(filename, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    
                    if (comicDate.isBefore(cutoffDate)) {
                        if (!comicFile.delete()) {
                            log.error("Failed to delete old comic file: {}", comicFile.getAbsolutePath());
                            success = false;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse date from filename: {}", comicFile.getName());
                }
            }
            
            // Delete year directory if it's empty
            File[] remainingFiles = yearDir.listFiles();
            if (remainingFiles != null && remainingFiles.length == 0) {
                if (!yearDir.delete()) {
                    log.error("Failed to delete empty year directory: {}", yearDir.getAbsolutePath());
                    success = false;
                }
            }
        }
        
        return success;
    }
    
    @Override
    public File getCacheRoot() {
        return new File(cacheProperties.getLocation());
    }
    
    @Override
    public String getComicCacheRoot(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        return String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed);
    }
    
    @Override
    public List<String> getYearsWithContent(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File comicRoot = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        
        if (!comicRoot.exists()) {
            return new ArrayList<>();
        }
        
        File[] yearDirs = comicRoot.listFiles(file -> file.isDirectory() && !file.getName().equals("@eaDir"));
        if (yearDirs == null) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(yearDirs)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }
    
    @Override
    public long getStorageSize(int comicId, String comicName) {
        String comicNameParsed = getComicNameParsed(comicId, comicName);
        File comicRoot = new File(String.format("%s/%s", getCacheRoot().getAbsolutePath(), comicNameParsed));
        
        if (!comicRoot.exists()) {
            return 0;
        }
        
        return calculateDirectorySize(comicRoot);
    }
    
    private long calculateDirectorySize(File directory) {
        long size = 0;
        
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        
        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
            } else if (file.isDirectory() && !file.getName().equals("@eaDir")) {
                size += calculateDirectorySize(file);
            }
        }
        
        return size;
    }
    
    // Helper methods migrated from CacheUtils
    
    private File findOldest(ComicItem comic) {
        return findFirst(comic, Direction.FORWARD);
    }
    
    private File findNewest(ComicItem comic) {
        return findFirst(comic, Direction.BACKWARD);
    }
    
    private File findFirst(ComicItem comic, Direction which) {
        // Make sure we're using the same naming convention
        String comicName = comic.getName();
        int comicId = comic.getId();
        
        File root = new File(getComicCacheRoot(comicId, comicName));
        
        if (!root.exists()) {
            log.debug("Comic directory doesn't exist: {}. This might be expected for comics with null names.", root.getAbsolutePath());
            return null;
        }
        
        // Comics are stored by year, find year folders, excluding any directory called @eaDir
        String[] yearFolders = root.list((dir, name) -> new File(dir, name).isDirectory() && !name.equals("@eaDir"));
        if (yearFolders == null || yearFolders.length == 0) {
            return null;
        }
        
        Arrays.sort(yearFolders, Comparator.comparing(Integer::valueOf));
        
        // Comics are stored with filename that is sortable.
        File folder = new File(String.format(COMBINE_PATH, root.getAbsolutePath(),
                which == Direction.FORWARD ? yearFolders[0] : yearFolders[yearFolders.length - 1]));
        
        String[] cachedStrips = folder.list((dir, name) -> new File(dir, name).isFile() && !name.equals("@eaDir"));
        if (cachedStrips == null || cachedStrips.length == 0) {
            return null;
        }
        
        Arrays.sort(cachedStrips, String::compareTo);
        
        return new File(String.format(COMBINE_PATH, folder.getAbsolutePath(),
                which == Direction.FORWARD ? cachedStrips[0] : cachedStrips[cachedStrips.length - 1]));
    }
}