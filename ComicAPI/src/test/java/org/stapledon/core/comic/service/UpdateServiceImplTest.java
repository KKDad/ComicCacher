package org.stapledon.core.comic.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.core.comic.downloader.ComicCacher;
import org.stapledon.api.dto.comic.ComicItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateServiceImplTest {

    @Mock
    private ComicCacher comicCacher;

    @InjectMocks
    UpdateServiceImpl subject;

    @Test
    void updateAll() {
        when(comicCacher.cacheAll()).thenReturn(true);
        assertThat(subject.updateAll()).isTrue();
    }

    @Test
    void updateComic() {
        ComicsServiceImpl.getComics().add(ComicItem.builder().id(42).build());

        when(comicCacher.cacheSingle(any())).thenReturn(true);

        assertThat(subject.updateComic(42)).isTrue();
        assertThat(subject.updateComic(50)).isFalse();
    }
}