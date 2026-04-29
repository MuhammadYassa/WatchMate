package com.project.watchmate.Repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.PopularMedia;

public interface PopularMediaRepository extends JpaRepository<PopularMedia, Long>{

    List<PopularMedia> findAllByOrderByPopularityRankAsc();

}
