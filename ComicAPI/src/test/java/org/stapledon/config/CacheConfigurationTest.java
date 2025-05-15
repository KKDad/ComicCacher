package org.stapledon.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.config.properties.CacheProperties;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheConfigurationTest {

    @Mock
    CacheProperties cacheProperties;

    @InjectMocks
    CacheConfiguration subject;
    
    private String originalOsName;

    @BeforeEach
    void setUp() {
        // Save the original os.name
        originalOsName = System.getProperty("os.name");
    }
    
    @AfterEach
    void tearDown() {
        // Restore original OS name
        System.setProperty("os.name", originalOsName);
    }

    @Test
    void configNameTest() {
        when(cacheProperties.getConfig()).thenReturn("myfancyconfigname.json");
        assertThat(subject.configName()).isEqualTo("myfancyconfigname.json");
    }
    
    @Test
    void shouldReturnWindowsPathOnWindows() {
        // Given
        System.setProperty("os.name", "Windows 10");
        when(cacheProperties.getLocation()).thenReturn("C:/comics");

        // When
        String result = subject.cacheLocation();

        // Then
        assertThat(result).isEqualTo("C:/comics");
    }

    @Test
    void shouldConvertWindowsPathToUserHomeOnNonWindows() {
        // Given
        System.setProperty("os.name", "Mac OS X");
        when(cacheProperties.getLocation()).thenReturn("C:/comics");

        // When
        String result = subject.cacheLocation();

        // Then
        String expectedPath = Paths.get(System.getProperty("user.home"), "comics").toString();
        assertThat(result).isEqualTo(expectedPath);
    }

    @Test
    void shouldUseConfiguredPathWhenValid() {
        // Given
        System.setProperty("os.name", "Linux");
        when(cacheProperties.getLocation()).thenReturn("/var/comics");

        // When
        String result = subject.cacheLocation();

        // Then
        assertThat(result).isEqualTo("/var/comics");
    }

    @Test
    void shouldCreateDirectoryIfNotExists() {
        // Given
        String tempDir = System.getProperty("java.io.tmpdir");
        String testPath = Paths.get(tempDir, "comic-cacher-test-" + System.currentTimeMillis()).toString();
        when(cacheProperties.getLocation()).thenReturn(testPath);

        // When
        String result = subject.cacheLocation();

        // Then
        File directory = new File(result);
        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        
        // Clean up
        directory.delete();
    }
}