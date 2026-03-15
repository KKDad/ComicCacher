package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.model.ComicImageNotFoundException;
import org.stapledon.engine.management.ManagementFacade;
import org.stapledon.metrics.collector.AccessMetricsCollector;

import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for ComicController access tracking wiring.
 */
@ExtendWith(MockitoExtension.class)
class ComicControllerTest {

    @Mock
    private ManagementFacade comicManagementFacade;

    @Mock
    private AccessMetricsCollector accessMetricsCollector;

    @InjectMocks
    private ComicController controller;

    private static final int COMIC_ID = 42;
    private static final String COMIC_NAME = "Garfield";
    private static final String IMAGE_DATA = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

    // =========================================================================
    // Avatar endpoint — access tracking
    // =========================================================================

    record AvatarCase(String label, boolean avatarExists) {
    }

    static Stream<AvatarCase> avatarCases() {
        return Stream.of(
                new AvatarCase("avatar found — tracks access", true),
                new AvatarCase("avatar not found — no tracking", false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("avatarCases")
    void retrieveAvatar_tracksAccessOnlyOnSuccess(AvatarCase tc) {
        if (tc.avatarExists) {
            var imageDto = ImageDto.builder().mimeType("image/png").imageData(IMAGE_DATA).build();
            when(comicManagementFacade.getAvatar(COMIC_ID)).thenReturn(Optional.of(imageDto));
            when(comicManagementFacade.getComic(COMIC_ID)).thenReturn(
                    Optional.of(ComicItem.builder().id(COMIC_ID).name(COMIC_NAME).build()));

            ResponseEntity<byte[]> response = controller.retrieveAvatar(COMIC_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(accessMetricsCollector).trackAccess(eq(COMIC_NAME), eq(true), anyLong());
        } else {
            when(comicManagementFacade.getAvatar(COMIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.retrieveAvatar(COMIC_ID))
                    .isInstanceOf(ComicImageNotFoundException.class);

            verify(accessMetricsCollector, never()).trackAccess(eq(COMIC_NAME), eq(true), anyLong());
        }
    }

    // =========================================================================
    // Strip endpoint — access tracking
    // =========================================================================

    record StripCase(String label, boolean stripExists) {
    }

    static Stream<StripCase> stripCases() {
        return Stream.of(
                new StripCase("strip found — tracks access", true),
                new StripCase("strip not found — no tracking", false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stripCases")
    void retrieveStrip_tracksAccessOnlyOnSuccess(StripCase tc) {
        var date = LocalDate.of(2024, 1, 15);

        if (tc.stripExists) {
            var imageDto = ImageDto.builder().mimeType("image/jpeg").imageData(IMAGE_DATA).build();
            when(comicManagementFacade.getComicStripOnDate(COMIC_ID, date)).thenReturn(Optional.of(imageDto));
            when(comicManagementFacade.getComic(COMIC_ID)).thenReturn(
                    Optional.of(ComicItem.builder().id(COMIC_ID).name(COMIC_NAME).build()));

            ResponseEntity<byte[]> response = controller.retrieveStrip(COMIC_ID, date);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(accessMetricsCollector).trackAccess(eq(COMIC_NAME), eq(true), anyLong());
        } else {
            when(comicManagementFacade.getComicStripOnDate(COMIC_ID, date)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> controller.retrieveStrip(COMIC_ID, date))
                    .isInstanceOf(ComicImageNotFoundException.class);

            verify(accessMetricsCollector, never()).trackAccess(eq(COMIC_NAME), eq(true), anyLong());
        }
    }

    // =========================================================================
    // Verify comic name resolution
    // =========================================================================

    record NameResolutionCase(String label, String comicName) {
    }

    static Stream<NameResolutionCase> nameResolutionCases() {
        return Stream.of(
                new NameResolutionCase("simple name", "Garfield"),
                new NameResolutionCase("name with spaces", "Calvin and Hobbes")
        );
    }

    @ParameterizedTest(name = "resolves {0}")
    @MethodSource("nameResolutionCases")
    void retrieveAvatar_resolvesComicNameForTracking(NameResolutionCase tc) {
        var imageDto = ImageDto.builder().mimeType("image/png").imageData(IMAGE_DATA).build();
        when(comicManagementFacade.getAvatar(COMIC_ID)).thenReturn(Optional.of(imageDto));
        when(comicManagementFacade.getComic(COMIC_ID)).thenReturn(
                Optional.of(ComicItem.builder().id(COMIC_ID).name(tc.comicName).build()));

        controller.retrieveAvatar(COMIC_ID);

        verify(accessMetricsCollector).trackAccess(eq(tc.comicName), eq(true), anyLong());
    }
}
