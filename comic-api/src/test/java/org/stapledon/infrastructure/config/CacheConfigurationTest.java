package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CacheProperties;

import java.io.File;
import java.nio.file.Paths;

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

    // --- Windows Tests ---

    @Test
    void windows_shouldUseUserHomeWhenNoDriverLetter() {
        // Given - Use temp directory to avoid creating C: folder on macOS
        String tempDir = System.getProperty("java.io.tmpdir");
        String testPath = Paths.get(tempDir, "win-test-comics-" + System.currentTimeMillis()).toString();
        System.setProperty("os.name", "Windows 10");
        when(cacheProperties.getLocation()).thenReturn(testPath);

        // When
        String result = subject.cacheLocation();

        // Then - Windows without drive letter should use user home
        String expectedPath = Paths.get(System.getProperty("user.home"), "comics").toString();
        assertThat(result).isEqualTo(expectedPath);

        // Clean up
        new File(result).delete();
    }

    // --- macOS Tests ---

    @Test
    void macOS_shouldConvertWindowsPathToUserHome() {
        // Given
        System.setProperty("os.name", "Mac OS X");
        when(cacheProperties.getLocation()).thenReturn("C:/comics");

        // When
        String result = subject.cacheLocation();

        // Then - Windows-style path should be converted to user home on macOS
        String expectedPath = Paths.get(System.getProperty("user.home"), "comics").toString();
        assertThat(result).isEqualTo(expectedPath);
    }

    @Test
    void macOS_shouldUseConfiguredUnixPath() {
        // Given - Use temp directory path to avoid side effects
        String tempDir = System.getProperty("java.io.tmpdir");
        String testPath = Paths.get(tempDir, "macos-test-comics-" + System.currentTimeMillis()).toString();
        System.setProperty("os.name", "Mac OS X");
        when(cacheProperties.getLocation()).thenReturn(testPath);

        // When
        String result = subject.cacheLocation();

        // Then - Valid Unix path should be used as-is on macOS
        assertThat(result).isEqualTo(testPath);

        // Clean up
        new File(result).delete();
    }

    // --- Linux Tests ---

    @Test
    void linux_shouldConvertWindowsPathToUserHome() {
        // Given
        System.setProperty("os.name", "Linux");
        when(cacheProperties.getLocation()).thenReturn("C:/comics");

        // When
        String result = subject.cacheLocation();

        // Then - Windows-style path should be converted to user home on Linux
        String expectedPath = Paths.get(System.getProperty("user.home"), "comics").toString();
        assertThat(result).isEqualTo(expectedPath);
    }

    @Test
    void linux_shouldUseConfiguredUnixPath() {
        // Given - Use temp directory path to avoid side effects
        String tempDir = System.getProperty("java.io.tmpdir");
        String testPath = Paths.get(tempDir, "linux-test-comics-" + System.currentTimeMillis()).toString();
        System.setProperty("os.name", "Linux");
        when(cacheProperties.getLocation()).thenReturn(testPath);

        // When
        String result = subject.cacheLocation();

        // Then - Valid Unix path should be used as-is on Linux
        assertThat(result).isEqualTo(testPath);

        // Clean up
        new File(result).delete();
    }

    // --- Directory Creation Tests ---

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
        assertThat(directory.exists()).isTrue();
        assertThat(directory.isDirectory()).isTrue();

        // Clean up
        directory.delete();
    }
}
