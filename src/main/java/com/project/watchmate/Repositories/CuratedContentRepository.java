package com.project.watchmate.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.CuratedContent;
import com.project.watchmate.Models.CuratedContentCategory;

public interface CuratedContentRepository extends JpaRepository<CuratedContent, Long> {

    @Query("""
        select curatedContent
        from CuratedContent curatedContent
        join fetch curatedContent.media media
        where curatedContent.categoryKey = :categoryKey
        order by curatedContent.rankPosition asc
        """)
    List<CuratedContent> findByCategoryKeyWithMediaOrderByRankPositionAsc(@Param("categoryKey") CuratedContentCategory categoryKey);

    boolean existsByCategoryKey(CuratedContentCategory categoryKey);

    long countByCategoryKey(CuratedContentCategory categoryKey);

    @Modifying
    void deleteByCategoryKey(CuratedContentCategory categoryKey);
}
