package org.stapledon.core.comic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.engine.management.ManagementFacade;

@ExtendWith(MockitoExtension.class)
class ComicUpdateServiceTest {

    @Mock
    private ManagementFacade comicManagementFacade;

    @InjectMocks
    ComicUpdateService subject;

    @Test
    void updateAll() {
        when(comicManagementFacade.updateAllComics()).thenReturn(true);
        assertThat(subject.updateAll()).isTrue();
    }

    @Test
    void updateComic() {
        when(comicManagementFacade.updateComic(42)).thenReturn(true);
        when(comicManagementFacade.updateComic(50)).thenReturn(false);

        assertThat(subject.updateComic(42)).isTrue();
        assertThat(subject.updateComic(50)).isFalse();
    }
}