package org.stapledon.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.stapledon.AbstractIntegrationTest;
import org.stapledon.config.TaskExecutionTracker;
import org.stapledon.downloader.ComicCacher;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("integration")
class TaskExecutionIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private DailyRunner dailyRunner;
    
    @Autowired
    private StartupReconciler startupReconciler;
    
    @MockBean
    private TaskExecutionTracker taskExecutionTracker;
    
    @MockBean
    private ComicCacher comicCacher;

    @Test
    void verifyDailyRunnerOnlyRunsOncePerDay() throws Exception {
        // Setup tracker to say task hasn't run today
        when(taskExecutionTracker.canRunToday("DailyComicCacher")).thenReturn(true);
        
        // Run the dailyRunner
        dailyRunner.run(new String[0]);
        
        // Verify cacheAll was called
        verify(comicCacher, times(1)).cacheAll();
        
        // Verify task was marked as executed
        verify(taskExecutionTracker, times(1)).markTaskExecuted("DailyComicCacher");
        
        // Reset and set up tracker to say task has already run today
        reset(comicCacher, taskExecutionTracker);
        
        when(taskExecutionTracker.canRunToday("DailyComicCacher")).thenReturn(false);
        when(taskExecutionTracker.getLastExecutionDate("DailyComicCacher")).thenReturn(LocalDate.now());
        
        // Run dailyRunner again
        dailyRunner.run(new String[0]);
        
        // Verify cacheAll was NOT called again
        verify(comicCacher, never()).cacheAll();
    }
    
    @Test
    void verifyStartupReconcilerOnlyRunsOncePerDay() throws Exception {
        // Setup tracker to say task hasn't run today
        when(taskExecutionTracker.canRunToday("StartupReconciler")).thenReturn(true);
        
        // Run the startupReconciler
        startupReconciler.reconcile();
        
        // Verify task was marked as executed
        verify(taskExecutionTracker, times(1)).markTaskExecuted("StartupReconciler");
        
        // Reset and set up tracker to say task has already run today
        reset(taskExecutionTracker);
        
        when(taskExecutionTracker.canRunToday("StartupReconciler")).thenReturn(false);
        when(taskExecutionTracker.getLastExecutionDate("StartupReconciler")).thenReturn(LocalDate.now());
        
        // Run startupReconciler again
        startupReconciler.reconcile();
        
        // Verify markTaskExecuted was NOT called again
        verify(taskExecutionTracker, never()).markTaskExecuted(anyString());
    }
}