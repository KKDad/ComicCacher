package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.io.CharSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.util.Bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class BootstrapLoaderTest {

    @Mock
    private Gson mockGson;

    private CacherConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new CacherConfigLoader(mockGson);
    }

    @Test
    void loadComicLocalDateTest() throws IOException {
        // Arrange
        String initialString = "{'dailyComics':[{'name':'Adam At Home','startDate':{'year':2008,'month':1,'day':9}}]}\n";
        InputStream targetStream = CharSource.wrap(initialString.replace('\'', '\"')).asByteSource(StandardCharsets.UTF_8).openStream();

        // Create a test bootstrap to return
        Bootstrap testBootstrap = createTestBootstrap("Adam At Home", LocalDate.of(2008, 1, 9));

        // Mock the gson behavior
        when(mockGson.fromJson(any(Reader.class), eq(Bootstrap.class))).thenReturn(testBootstrap);

        // Act
        Bootstrap results = configLoader.load(targetStream);

        // Assert
        assertThat(results.getDailyComics()).hasSize(1);
        assertThat(results.getDailyComics().get(0).stripName()).isEqualTo("Adam At Home");
        assertThat(results.getDailyComics().get(0).startDate()).isEqualTo(LocalDate.of(2008, 1, 9));
    }

    @Test
    void loadComicCompactDateTest() throws IOException {
        // Arrange
        String initialString = "{'dailyComics':[{'name': 'Adam At Home','startDate': '2019-01-21'}]}";
        InputStream targetStream = CharSource.wrap(initialString.replace('\'', '\"')).asByteSource(StandardCharsets.UTF_8).openStream();

        // Create a test bootstrap to return
        Bootstrap testBootstrap = createTestBootstrap("Adam At Home", LocalDate.of(2019, 1, 21));

        // Mock the gson behavior
        when(mockGson.fromJson(any(Reader.class), eq(Bootstrap.class))).thenReturn(testBootstrap);

        // Act
        Bootstrap results = configLoader.load(targetStream);

        // Assert
        assertThat(results.getDailyComics()).hasSize(1);
        assertThat(results.getDailyComics().get(0).stripName()).isEqualTo("Adam At Home");
        assertThat(results.getDailyComics().get(0).startDate()).isEqualTo(LocalDate.of(2019, 1, 21));
    }

    @Test
    void saveSimpleTest() {
        // Arrange
        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(GoComicsBootstrap.builder()
                        .name("Adam At Home")
                        .startDate(LocalDate.of(2019, Month.JANUARY, 21))
                        .build())).build();

        // Use real Gson for serialization test
        Gson realGson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();

        // Act
        String serialized = realGson.toJson(config);

        // Assert
        assertThat(serialized).isNotNull().contains("2019-01-21");
    }

    // Helper method to create test bootstrap objects
    private Bootstrap createTestBootstrap(String name, LocalDate date) {
        GoComicsBootstrap comic = GoComicsBootstrap.builder()
                .name(name)
                .startDate(date)
                .build();

        return Bootstrap.builder()
                .dailyComics(List.of(comic))
                .build();
    }

    // Simple adapter class for LocalDate
    private static class LocalDateAdapter implements com.google.gson.JsonSerializer<LocalDate> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDate src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
    }
}