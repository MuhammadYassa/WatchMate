package com.project.watchmate.discovery.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.discovery.domain.ContentSyncStatus;

public interface ContentSyncStatusRepository extends JpaRepository<ContentSyncStatus, String> {

}


