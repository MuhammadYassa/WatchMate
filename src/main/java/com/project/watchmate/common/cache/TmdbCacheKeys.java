package com.project.watchmate.common.cache;

import java.util.Locale;

import com.project.watchmate.media.catalog.domain.MediaType;

public final class TmdbCacheKeys {

    public static final String UPCOMING_MOVIES = "upcomingMovies";
    public static final String AIRING_TODAY = "airingToday";
    public static final String ON_THE_AIR = "onTheAir";

    private TmdbCacheKeys() {
    }

    public static String genre(String type) {
        return normalize(type);
    }

    public static String listByType(String type) {
        return normalize(type);
    }

    public static String media(MediaType type, Long tmdbId) {
        return normalize(type == null ? null : type.name()) + ":" + tmdbId;
    }

    public static String credits(MediaType type, Long tmdbId) {
        return media(type, tmdbId);
    }

    public static String videos(MediaType type, Long tmdbId) {
        return media(type, tmdbId);
    }

    public static String watchProviders(MediaType type, Long tmdbId) {
        return media(type, tmdbId);
    }

    public static String show(Long tmdbId) {
        return String.valueOf(tmdbId);
    }

    public static String season(Long tmdbId, Integer seasonNumber) {
        return tmdbId + ":" + seasonNumber;
    }

    public static String search(String query, int page) {
        return normalize(query) + ":" + page;
    }

    public static String discoverByGenre(String type, Long genreId, int page) {
        return normalize(type) + ":" + genreId + ":" + page;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
