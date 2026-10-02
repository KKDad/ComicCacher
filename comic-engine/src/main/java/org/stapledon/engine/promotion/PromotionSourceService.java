package org.stapledon.engine.promotion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stapledon.common.dto.ComicIdentifier;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.engine.management.ManagementFacade;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * The serving side of promotion: lists the strips this instance has for a date range and hands out each one, for another instance's
 * {@link DevPromotionService} to copy. Reads only; disabled comics are included, since their strips are still on disk.
 */
@Slf4j
@Service
public class PromotionSourceService {

    private final ManagementFacade managementFacade;
    private final ComicStorageFacade storageFacade;
    private final int maxDays;

    public PromotionSourceService(ManagementFacade managementFacade, ComicStorageFacade storageFacade,
            @Value("${comics.promotion.max-days:7}") int maxDays) {
        this.managementFacade = managementFacade;
        this.storageFacade = storageFacade;
        this.maxDays = maxDays;
    }

    /**
     * Lists every comic with a strip between {@code from} and {@code to} (inclusive), optionally only one source or one comic. Throws
     * IllegalArgumentException when the range is reversed or longer than {@code comics.promotion.max-days}.
     */
    public PromotionManifest manifest(LocalDate from, LocalDate to, String source, String sourceIdentifier) {
        checkRange(from, to, maxDays);
        List<PromotionManifest.Comic> comics = new ArrayList<>();
        for (ComicItem comic : managementFacade.getAllComics()) {
            if (comic.getSource() == null || comic.getSourceIdentifier() == null
                    || source != null && !source.equals(comic.getSource())
                    || sourceIdentifier != null && !sourceIdentifier.equals(comic.getSourceIdentifier())) {
                continue;
            }
            List<LocalDate> dates = storageFacade.getAvailableDates(ComicIdentifier.from(comic)).stream()
                    .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                    .toList();
            if (!dates.isEmpty()) {
                comics.add(new PromotionManifest.Comic(comic.getSource(), comic.getSourceIdentifier(), comic.getName(), dates));
            }
        }
        log.debug("Promotion manifest {} to {}: {} comics", from, to, comics.size());
        return new PromotionManifest(from, to, comics);
    }

    /**
     * The strip (with its transcript) of the comic with this source and identifier on this date, if it is on disk.
     */
    public Optional<ImageDto> strip(String source, String sourceIdentifier, LocalDate date) {
        return managementFacade.getAllComics().stream()
                .filter(comic -> Objects.equals(source, comic.getSource()) && Objects.equals(sourceIdentifier, comic.getSourceIdentifier()))
                .findFirst()
                .flatMap(comic -> storageFacade.getComicStrip(ComicIdentifier.from(comic), date));
    }

    /**
     * Rejects a reversed range or one covering more than {@code maxDays} days.
     */
    static void checkRange(LocalDate from, LocalDate to, int maxDays) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("from and to are required, and to may not be before from");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > maxDays) {
            throw new IllegalArgumentException("The range covers " + days + " days; at most " + maxDays + " are allowed");
        }
    }
}
