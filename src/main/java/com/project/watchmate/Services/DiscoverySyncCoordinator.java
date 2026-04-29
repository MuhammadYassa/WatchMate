package com.project.watchmate.Services;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DiscoverySyncCoordinator {

    private final CuratedContentSyncService curatedContentSyncService;

    private final TaskExecutor taskExecutor;

    public DiscoverySyncCoordinator(CuratedContentSyncService curatedContentSyncService, @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.curatedContentSyncService = curatedContentSyncService;
        this.taskExecutor = taskExecutor;
    }

    @Value("${watchmate.discovery.sync.startup-enabled:true}")
    private boolean startupSyncEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!startupSyncEnabled) {
            log.info("Discovery startup sync disabled");
            return;
        }

        if (!curatedContentSyncService.hasCachedContent()) {
            log.info("Discovery startup sync running reason=empty_cache");
            curatedContentSyncService.syncDiscoveryContent("startup-empty-cache");
            return;
        }

        log.info("Discovery startup sync queued reason=refresh_existing_cache");
        taskExecutor.execute(() -> curatedContentSyncService.syncDiscoveryContent("startup-refresh"));
    }

    @Scheduled(cron = "${watchmate.discovery.sync.cron:0 59 23 * * *}")
    public void scheduledRefresh() {
        curatedContentSyncService.syncDiscoveryContent("scheduled");
    }
}
