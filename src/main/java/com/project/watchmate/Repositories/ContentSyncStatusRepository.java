package com.project.watchmate.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.ContentSyncStatus;

public interface ContentSyncStatusRepository extends JpaRepository<ContentSyncStatus, String> {

}
