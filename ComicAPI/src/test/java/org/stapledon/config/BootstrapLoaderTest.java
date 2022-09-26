package org.stapledon.config;

import com.google.common.io.CharSource;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.stapledon.dto.Bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapLoaderTest {

    @Test
    void loadComicLocalDateTest() throws IOException {

        // Arrange
        String initialString = "{'dailyComics':[{'name':'Adam At Home','startDate':{'year':2008,'month':1,'day':9}}]}\n";
        InputStream targetStream = CharSource.wrap(initialString.replace('\'', '\"')).asByteSource(StandardCharsets.UTF_8).openStream();

        // Act
        Bootstrap results = new CacherConfigLoader(new GsonProvider().gson()).load(targetStream);

        // Assert
        assertThat(results.getDailyComics()).hasSize(1);
        assertThat(results.getDailyComics().get(0).name).isEqualTo("Adam At Home");
        assertThat(results.getDailyComics().get(0).startDate).isEqualTo(LocalDate.of(2008, 1, 9));
    }

    @Test
    void loadComicCompactDateTest() throws IOException {

        // Arrange
        String initialString = "{'dailyComics':[{'name': 'Adam At Home','startDate': '2019-01-21'}]}";
        InputStream targetStream = CharSource.wrap(initialString.replace('\'', '\"')).asByteSource(StandardCharsets.UTF_8).openStream();

        // Act
        Bootstrap results = new CacherConfigLoader(new GsonProvider().gson()).load(targetStream);

        // Assert
        assertThat(results.getDailyComics()).hasSize(1);
        assertThat(results.getDailyComics().get(0).name).isEqualTo("Adam At Home");
        assertThat(results.getDailyComics().get(0).startDate).isEqualTo(LocalDate.of(2019, 1, 21));
    }


    @Test
    void saveSimpleTest() {
        // Arrange
        Bootstrap config = Bootstrap.builder()
                .dailyComics(List.of(GoComicsBootstrap.builder()
                        .name("Adam At Home")
                        .startDate(LocalDate.of(2019, Month.JANUARY, 21))
                        .build())).build();

        // Act
        Gson gson = new GsonProvider().gson();
        String serialized = gson.toJson(config);

        assertThat(serialized).isNotNull().contains("2019-01-21");
    }
}