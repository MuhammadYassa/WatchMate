package com.project.watchmate.media.integration;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MediaResolutionConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MediaResolutionService mediaResolutionService;

    @Test
    void resolveMediaByTmdbId_whenTwoThreadsImportSameMedia_createsOneRowAndBothReturnMedia() throws Exception {
        Long tmdbId = 8201L;
        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch bothFetching = new CountDownLatch(2);
        AtomicInteger fetchCount = new AtomicInteger();

        when(tmdbClient.fetchMediaById(eq(tmdbId), eq(MediaType.MOVIE))).thenAnswer(invocation -> {
            fetchCount.incrementAndGet();
            bothFetching.countDown();
            assertThat(bothFetching.await(5, TimeUnit.SECONDS)).isTrue();
            return tmdbMovie(tmdbId);
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Media> first = executor.submit(() -> resolveAfterStart(startTogether, tmdbId));
            Future<Media> second = executor.submit(() -> resolveAfterStart(startTogether, tmdbId));

            startTogether.countDown();

            Media firstResult = first.get(10, TimeUnit.SECONDS);
            Media secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResult.getTmdbId()).isEqualTo(tmdbId);
            assertThat(secondResult.getTmdbId()).isEqualTo(tmdbId);
            assertThat(firstResult.getType()).isEqualTo(MediaType.MOVIE);
            assertThat(secondResult.getType()).isEqualTo(MediaType.MOVIE);

            List<Media> rows = mediaRepository.findAllByTmdbId(tmdbId);
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().getType()).isEqualTo(MediaType.MOVIE);
            assertThat(fetchCount.get()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private Media resolveAfterStart(CountDownLatch startTogether, Long tmdbId) throws Exception {
        assertThat(startTogether.await(5, TimeUnit.SECONDS)).isTrue();
        return mediaResolutionService.resolveMediaByTmdbId(tmdbId, MediaType.MOVIE);
    }

    private TmdbMovieDTO tmdbMovie(Long tmdbId) {
        return TmdbMovieDTO.builder()
            .id(tmdbId)
            .title("Concurrent Movie")
            .overview("Imported concurrently")
            .posterPath("/concurrent.jpg")
            .releaseDate("2024-01-01")
            .voteAverage(7.2)
            .genres(List.of())
            .build();
    }
}
