package com.project.watchmate.favourite.application;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.cache.WatchMateCacheNames;
import com.project.watchmate.user.persistence.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserFavoriteMediaIdsCacheService {

    private final UsersRepository usersRepository;

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = WatchMateCacheNames.USER_FAVORITE_MEDIA_IDS,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#userId)",
        unless = "#result == null"
    )
    public Set<Long> getFavoriteMediaIds(Long userId) {
        return new LinkedHashSet<>(usersRepository.findFavoriteMediaIds(userId));
    }
}
