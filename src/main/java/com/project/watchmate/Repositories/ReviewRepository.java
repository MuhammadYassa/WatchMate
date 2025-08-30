package com.project.watchmate.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.Users;

public interface ReviewRepository extends JpaRepository<Review, Long>{

    List<Review> findByMedia(Media media);

    boolean existsByUserAndMedia(Users user, Media media)  ;

}
