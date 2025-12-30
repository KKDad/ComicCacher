package org.stapledon.metrics;

import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.service.ComicStorageFacade;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mock implementation of ComicStorageFacade for testing CacheUtils
 */
public class MockComicStorageFacade implements ComicStorageFacade {

    private final Map<String, LocalDate> oldestDates = new HashMap<>();
    private final Map<String, LocalDate> newestDates = new HashMap<>();
    private final Map<String, Map<LocalDate, byte[]>> comicStrips = new HashMap<>();

    /**
     * Set up comic data for testing
     * @param comicId Comic ID
     * @param comicName Comic name
     * @param oldest Oldest date
     * @param newest Newest date
     * @param dates Additional dates between oldest and newest
     */
    public void setupComic(int comicId, String comicName, LocalDate oldest, LocalDate newest, LocalDate... dates) {
        String key = getComicKey(comicId, comicName);
        oldestDates.put(key, oldest);
        newestDates.put(key, newest);

        Map<LocalDate, byte[]> strips = new HashMap<>();
        strips.put(oldest, new byte[]{1});
        strips.put(newest, new byte[]{2});

        for (LocalDate date : dates) {
            strips.put(date, new byte[]{3});
        }

        comicStrips.put(key, strips);
    }

    private String getComicKey(int comicId, String comicName) {
        return comicId + ":" + comicName;
    }

    @Override
    public Optional<LocalDate> getOldestDateWithComic(int comicId, String comicName) {
        String key = getComicKey(comicId, comicName);
        return Optional.ofNullable(oldestDates.get(key));
    }

    @Override
    public Optional<LocalDate> getNewestDateWithComic(int comicId, String comicName) {
        String key = getComicKey(comicId, comicName);
        return Optional.ofNullable(newestDates.get(key));
    }

    @Override
    public Optional<LocalDate> getNextDateWithComic(int comicId, String comicName, LocalDate fromDate) {
        String key = getComicKey(comicId, comicName);
        Map<LocalDate, byte[]> strips = comicStrips.get(key);

        if (strips == null) {
            return Optional.empty();
        }

        LocalDate nextDate = null;
        for (LocalDate date : strips.keySet()) {
            if (date.isAfter(fromDate) && (nextDate == null || date.isBefore(nextDate))) {
                nextDate = date;
            }
        }

        return Optional.ofNullable(nextDate);
    }

    @Override
    public Optional<LocalDate> getPreviousDateWithComic(int comicId, String comicName, LocalDate fromDate) {
        String key = getComicKey(comicId, comicName);
        Map<LocalDate, byte[]> strips = comicStrips.get(key);

        if (strips == null) {
            return Optional.empty();
        }

        LocalDate prevDate = null;
        for (LocalDate date : strips.keySet()) {
            if (date.isBefore(fromDate) && (prevDate == null || date.isAfter(prevDate))) {
                prevDate = date;
            }
        }

        return Optional.ofNullable(prevDate);
    }

    @Override
    public boolean saveComicStrip(int comicId, String comicName, LocalDate date, byte[] imageData) {
        String key = getComicKey(comicId, comicName);
        Map<LocalDate, byte[]> strips = comicStrips.computeIfAbsent(key, k -> new HashMap<>());
        strips.put(date, imageData);

        // Update oldest/newest dates as needed
        if (!oldestDates.containsKey(key) || date.isBefore(oldestDates.get(key))) {
            oldestDates.put(key, date);
        }

        if (!newestDates.containsKey(key) || date.isAfter(newestDates.get(key))) {
            newestDates.put(key, date);
        }

        return true;
    }

    @Override
    public boolean saveAvatar(int comicId, String comicName, byte[] avatarData) {
        // Not needed for these tests
        return true;
    }

    @Override
    public long getStorageSize(int comicId, String comicName) {
        // Return a dummy value
        return 1024L;
    }

    @Override
    public List<String> getYearsWithContent(int comicId, String comicName) {
        String key = getComicKey(comicId, comicName);
        Map<LocalDate, byte[]> strips = comicStrips.get(key);

        if (strips == null) {
            return new ArrayList<>();
        }

        // Extract unique years from all dates in the comic strips and convert to strings
        return strips.keySet().stream()
                .map(LocalDate::getYear)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String getComicCacheRoot(int comicId, String comicName) {
        // Return a mock cache root path
        return "/mock/cache/root/" + comicName.replace(" ", "");
    }

    @Override
    public File getCacheRoot() {
        // Return the root cache directory as a File
        return new File("/mock/cache/root");
    }

    @Override
    public boolean purgeOldImages(int comicId, String comicName, int daysToKeep) {
        // In a mock implementation, return success
        return true;
    }

    @Override
    public boolean deleteComic(int comicId, String comicName) {
        // In a mock implementation, remove from the maps and return success
        String key = getComicKey(comicId, comicName);
        oldestDates.remove(key);
        newestDates.remove(key);
        comicStrips.remove(key);
        return true;
    }

    @Override
    public boolean comicStripExists(int comicId, String comicName, LocalDate date) {
        String key = getComicKey(comicId, comicName);
        Map<LocalDate, byte[]> strips = comicStrips.get(key);
        return strips != null && strips.containsKey(date);
    }

    @Override
    public Optional<ImageDto> getAvatar(int comicId, String comicName) {
        // Return a dummy avatar
        ImageDto imageDto = ImageDto.builder()
                .mimeType("image/png")
                .imageData("base64EncodedDummyData")
                .height(100)
                .width(100)
                .build();
        return Optional.of(imageDto);
    }

    @Override
    public Optional<ImageDto> getComicStrip(int comicId, String comicName, LocalDate date) {
        String key = getComicKey(comicId, comicName);
        Map<LocalDate, byte[]> strips = comicStrips.get(key);

        if (strips == null || !strips.containsKey(date)) {
            return Optional.empty();
        }

        // Return a dummy comic strip
        ImageDto imageDto = ImageDto.builder()
                .mimeType("image/png")
                .imageData("base64EncodedStripData")
                .height(400)
                .width(800)
                .imageDate(date)
                .build();
        return Optional.of(imageDto);
    }
}