package com.project.watchmate.common.cache;

import java.util.Locale;

import com.project.watchmate.media.catalog.domain.MediaType;

public final class WatchMateCacheKeys {

    public static final String HOME_DEFAULT = "home:default";

    private WatchMateCacheKeys() {
    }

    public static String curatedCategory(Enum<?> category) {
        return "category:" + normalize(category == null ? null : category.name());
    }

    public static String media(MediaType type, Long tmdbId) {
        return "media:" + normalize(type == null ? null : type.name()) + ":" + tmdbId;
    }

    public static String show(Long tmdbId) {
        return "show:" + tmdbId;
    }

    public static String season(Long tmdbId, Integer seasonNumber) {
        return "show:" + tmdbId + ":season:" + seasonNumber;
    }

    public static String user(Long userId) {
        return "user:" + userId;
    }

    public static String watchlistPage(Long userId, int page, int size) {
        return "user:" + userId + ":page:" + page + ":size:" + size + ":sort:id_asc";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
