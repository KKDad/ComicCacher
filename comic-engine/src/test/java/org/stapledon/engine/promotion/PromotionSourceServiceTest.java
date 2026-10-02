package org.stapledon.engine.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.common.service.ComicStorageFacade;
import org.stapledon.engine.management.ManagementFacade;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class PromotionSourceServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    private ManagementFacade managementFacade;
    private ComicStorageFacade storageFacade;
    private PromotionSourceService service;

    @BeforeEach
    void setUp() {
        managementFacade = mock(ManagementFacade.class);
        storageFacade = mock(ComicStorageFacade.class);
        service = new PromotionSourceService(managementFacade, storageFacade, 7);

        ComicItem garfield = ComicItem.builder().id(1).name("Garfield").source("gocomics").sourceIdentifier("garfield").build();
        ComicItem hidden = ComicItem.builder().id(2).name("Hidden").source("comicskingdom").sourceIdentifier("hidden").enabled(false).build();
        ComicItem noSource = ComicItem.builder().id(3).name("Local").build();
        when(managementFacade.getAllComics()).thenReturn(List.of(garfield, hidden, noSource));
        when(storageFacade.getAvailableDates(argThat(id -> id != null && id.getId() == 1)))
                .thenReturn(List.of(DAY.minusDays(30), DAY.minusDays(1), DAY));
        when(storageFacade.getAvailableDates(argThat(id -> id != null && id.getId() == 2))).thenReturn(List.of(DAY));
    }

    @Test
    void manifest_listsDatesInRange_includingDisabledComics() {
        PromotionManifest manifest = service.manifest(DAY.minusDays(6), DAY, null, null);

        assertThat(manifest.comics()).containsExactly(
                new PromotionManifest.Comic("gocomics", "garfield", "Garfield", List.of(DAY.minusDays(1), DAY)),
                new PromotionManifest.Comic("comicskingdom", "hidden", "Hidden", List.of(DAY)));
    }

    @Test
    void manifest_filtersBySourceAndIdentifier() {
        assertThat(service.manifest(DAY, DAY, "comicskingdom", null).comics()).extracting(PromotionManifest.Comic::name).containsExactly("Hidden");
        assertThat(service.manifest(DAY, DAY, "gocomics", "garfield").comics()).extracting(PromotionManifest.Comic::name).containsExactly("Garfield");
        assertThat(service.manifest(DAY, DAY, "gocomics", "hidden").comics()).isEmpty();
    }

    @Test
    void manifest_refusesBadRanges() {
        assertThatThrownBy(() -> service.manifest(DAY.minusDays(7), DAY, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.manifest(DAY, DAY.minusDays(1), null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.manifest(DAY.minusDays(6), DAY, null, null).comics()).isNotEmpty();
    }

    @Test
    void strip_findsTheComicBySourceAndIdentifier() {
        ImageDto image = ImageDto.builder().mimeType("image/png").imageData("AQID").build();
        when(storageFacade.getComicStrip(argThat(id -> id != null && id.getId() == 1), eq(DAY))).thenReturn(Optional.of(image));

        assertThat(service.strip("gocomics", "garfield", DAY)).contains(image);
        assertThat(service.strip("comicskingdom", "garfield", DAY)).isEmpty();
        assertThat(service.strip("gocomics", "nobody", DAY)).isEmpty();
        when(storageFacade.getComicStrip(any(), eq(DAY.minusDays(1)))).thenReturn(Optional.empty());
        assertThat(service.strip("gocomics", "garfield", DAY.minusDays(1))).isEmpty();
    }
}
