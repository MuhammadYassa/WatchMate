package com.project.watchmate.review.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.user.domain.Users;

public interface ReviewRepository extends JpaRepository<Review, Long>{

    List<Review> findByMedia(Media media);

    boolean existsByUserAndMedia(Users user, Media media);

    Page<Review> findAllByUser(Users user, Pageable pageable);

}




