package com.project.watchmate.Services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiscoverySyncCoordinatorTest {

    @Mock
    private CuratedContentSyncService curatedContentSyncService;

    @Mock
    private TaskExecutor taskExecutor;

    @InjectMocks
    private DiscoverySyncCoordinator discoverySyncCoordinator;

    @Test
    void onApplicationReady_WhenCacheEmpty_RunsBlockingSync() {
        ReflectionTestUtils.setField(discoverySyncCoordinator, "startupSyncEnabled", true);
        when(curatedContentSyncService.hasCachedContent()).thenReturn(false);

        discoverySyncCoordinator.onApplicationReady();

        verify(curatedContentSyncService).syncDiscoveryContent("startup-empty-cache");
    }

    @Test
    void onApplicationReady_WhenCacheExists_QueuesAsyncRefresh() {
        ReflectionTestUtils.setField(discoverySyncCoordinator, "startupSyncEnabled", true);
        when(curatedContentSyncService.hasCachedContent()).thenReturn(true);

        discoverySyncCoordinator.onApplicationReady();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();
        verify(curatedContentSyncService).syncDiscoveryContent("startup-refresh");
    }

    @Test
    void scheduledRefresh_TriggersScheduledSync() {
        discoverySyncCoordinator.scheduledRefresh();

        verify(curatedContentSyncService).syncDiscoveryContent("scheduled");
    }
}
