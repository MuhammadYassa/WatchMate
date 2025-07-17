package com.project.watchmate.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Review;

public interface ReviewRepository extends JpaRepository<Review, Long>{

    List<Review> findByMedia(Media media);

}
