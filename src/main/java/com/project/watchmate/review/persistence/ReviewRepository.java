package com.project.watchmate.review.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.user.domain.Users;

public interface ReviewRepository extends JpaRepository<Review, Long>{

    List<Review> findByMedia(Media media);

    Page<Review> findByMedia(Media media, Pageable pageable);

    boolean existsByUserAndMedia(Users user, Media media);

    Page<Review> findAllByUser(Users user, Pageable pageable);

    @Query("""
        select review
        from Review review
        join fetch review.user
        join fetch review.media media
        where media.id in :mediaIds
        """)
    List<Review> findAllByMediaIdInWithUserAndMedia(@Param("mediaIds") Collection<Long> mediaIds);

}




