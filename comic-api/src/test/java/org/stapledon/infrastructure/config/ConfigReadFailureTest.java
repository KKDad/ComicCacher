package org.stapledon.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.util.GsonUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * A config file that exists but cannot be read must never be treated as empty: the next save would overwrite every user, preference or comic in it.
 */
class ConfigReadFailureTest {

    @TempDir
    Path tempDir;

    private final Gson gson = GsonUtils.createGsonBuilder().create();
    private ApplicationConfigurationFacade facade;
    private UserConfigWriter userConfigWriter;

    @BeforeEach
    void setUp() {
        CacheProperties properties = CacheProperties.builder()
                .location(tempDir.toString())
                .config("comics.json")
                .usersConfig("users.json")
                .preferencesConfig("preferences.json")
                .build();
        facade = new ApplicationConfigurationFacade(gson, properties, tempDir.toString());
        userConfigWriter = new UserConfigWriter(gson, properties, facade);
    }

    @Test
    void unreadableUsersFileThrowsInsteadOfLookingEmpty() throws Exception {
        // A directory where the file should be: exists, but reading it fails with an IOException
        Files.createDirectory(tempDir.resolve("users.json"));

        // Gson reports an I/O error during parsing as a JsonSyntaxException; opening failures are UncheckedIOException. Either way it throws.
        assertThatThrownBy(() -> facade.loadUserConfig())
                .isInstanceOfAny(UncheckedIOException.class, JsonParseException.class);
    }

    @Test
    void registrationDuringAReadFailureDoesNotOverwriteExistingUsers() throws Exception {
        Path users = tempDir.resolve("users.json");
        String existing = gson.toJson(Map.of("users", Map.of("alice", User.builder().username("alice").build())));
        // Unparseable content stands in for a partial NFS read
        Files.writeString(users, "{\"users\": {\"alice\": ");

        Optional<User> registered = userConfigWriter.registerUser(UserRegistrationDto.builder().username("bob").password("secret").build());

        assertThat(registered).isEmpty();
        assertThat(Files.readString(users)).isEqualTo("{\"users\": {\"alice\": ");

        // Once the file reads again, nothing was lost and the writer did not cache an empty config
        Files.writeString(users, existing);
        assertThat(userConfigWriter.registerUser(UserRegistrationDto.builder().username("bob").password("secret").build())).isPresent();
        assertThat(facade.loadUserConfig().getUsers()).containsKeys("alice", "bob");
    }

    @Test
    void malformedComicsFileThrows() throws Exception {
        Files.writeString(tempDir.resolve("comics.json"), "{ not json");

        assertThatThrownBy(() -> facade.loadComicConfig()).isInstanceOf(JsonParseException.class);
    }

    @Test
    void missingFilesStillStartEmpty() {
        assertThat(facade.loadUserConfig().getUsers()).isEmpty();
        assertThat(facade.loadPreferenceConfig().getPreferences()).isEmpty();
        assertThat(facade.loadComicConfig().getItems()).isEmpty();
    }

    @Test
    void saveWritesAtomicallyAndLeavesNoTempFiles() throws Exception {
        assertThat(userConfigWriter.registerUser(UserRegistrationDto.builder().username("carol").password("pw").build())).isPresent();

        try (var files = Files.list(tempDir)) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("users.json");
        }
    }
}
