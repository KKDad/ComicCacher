package org.stapledon.config;

import com.google.common.io.CharSource;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class CacherBootstrapConfigLoaderTest {

    @Test
    void loadSimpleTest() throws IOException
    {

        // Arrange
        String initialString = "{'dailyComics':[{'name':'Adam At Home','startDate':{'year':2008,'month':1,'day':9}}]}\n";
        InputStream targetStream =   CharSource.wrap(initialString.replace('\'', '\"')).asByteSource(StandardCharsets.UTF_8).openStream();

        // Act
        CacherBootstrapConfig results = new CacherConfigLoader().load(targetStream);

        // Assert
        assertThat(results.dailyComics).hasSize(1);
        assertThat(results.dailyComics.get(0).name).isEqualTo("Adam At Home");
        assertThat(results.dailyComics.get(0).startDate).isEqualTo(LocalDate.of(2008, 1, 9));
    }


    @Test
    void saveSimpleTest()
    {
        // Arrange
        CacherBootstrapConfig config = new CacherBootstrapConfig();
        CacherBootstrapConfig.GoComicsBootstrap comic = config.new GoComicsBootstrap();
        comic.name = "Adam At Home";
        comic.startDate = LocalDate.of(2019, Month.JANUARY, 1);
        config.dailyComics.add(comic);

        // Act
        Gson gson = new Gson();
        String serialized = gson.toJson(config);

        assertThat(serialized).isNotNull();
    }

}