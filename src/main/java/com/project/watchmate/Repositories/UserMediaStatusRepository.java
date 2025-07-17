package com.project.watchmate.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;

public interface UserMediaStatusRepository extends JpaRepository<UserMediaStatus, Long>{

    Optional<UserMediaStatus> findByUserAndMedia(Users user, Media media);

}
