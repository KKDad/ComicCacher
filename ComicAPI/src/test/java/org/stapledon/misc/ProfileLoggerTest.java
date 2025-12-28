package org.stapledon.misc;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileLoggerTest {

    private ProfileLogger profileLogger;
    private MockLogger mockLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        profileLogger = new ProfileLogger();

        // Set up MockLogger
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.stapledon.misc");
        mockLogger = new MockLogger();
        mockLogger.setContext(loggerContext);
        mockLogger.start();
        rootLogger.addAppender(mockLogger);
    }

    @Test
    void testSetupWithDefaultProfile() {
        System.setProperty("spring.profiles.active", "default");
        profileLogger.setup();

        // Verify the log output
        assertTrue(mockLogger.getLogEvents().stream()
                .anyMatch(event -> event.getFormattedMessage().contains("Using application-default.properties")));
    }

    @Test
    void testSetupWithProductionProfile() {
        System.setProperty("spring.profiles.active", "production");
        profileLogger.setup();

        // Verify the log output
        assertTrue(mockLogger.getLogEvents().stream()
                .anyMatch(event -> event.getFormattedMessage().contains("Using application-production.properties")));
    }
}