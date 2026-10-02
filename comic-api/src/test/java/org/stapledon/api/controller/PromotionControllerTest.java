package org.stapledon.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.dto.PromotionManifest;
import org.stapledon.common.util.GsonUtils;
import org.stapledon.engine.promotion.PromotionSourceService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class PromotionControllerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    private final Gson gson = GsonUtils.createGsonBuilder().create();
    private PromotionSourceService sourceService;
    private PromotionController controller;

    @BeforeEach
    void setUp() {
        sourceService = mock(PromotionSourceService.class);
        controller = new PromotionController(sourceService, gson);
    }

    @Test
    void manifest_isWrittenWithGson() {
        PromotionManifest manifest = new PromotionManifest(DAY, DAY,
                List.of(new PromotionManifest.Comic("gocomics", "garfield", "Garfield", List.of(DAY))));
        when(sourceService.manifest(DAY, DAY, null, null)).thenReturn(manifest);

        ResponseEntity<String> response = controller.manifest(DAY, DAY, null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(gson.fromJson(response.getBody(), PromotionManifest.class)).isEqualTo(manifest);
    }

    @Test
    void manifest_badRangeIs400() {
        when(sourceService.manifest(DAY.minusDays(30), DAY, null, null)).thenThrow(new IllegalArgumentException("too long"));

        assertThat(controller.manifest(DAY.minusDays(30), DAY, null, null).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void strip_returnsTheBytesAndTranscript() {
        ImageDto image = ImageDto.builder().mimeType("image/png").imageData("AQID").transcript("Jon: Hi + bye").build();
        when(sourceService.strip("gocomics", "garfield", DAY)).thenReturn(Optional.of(image));

        ResponseEntity<byte[]> response = controller.strip("gocomics", "garfield", DAY);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(1, 2, 3);
        assertThat(URLDecoder.decode(response.getHeaders().getFirst("X-Transcript"), StandardCharsets.UTF_8)).isEqualTo("Jon: Hi + bye");
    }

    @Test
    void strip_missingIs404() {
        when(sourceService.strip("gocomics", "garfield", DAY)).thenReturn(Optional.empty());

        assertThat(controller.strip("gocomics", "garfield", DAY).getStatusCode().value()).isEqualTo(404);
    }
}
