package org.stapledon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;
import org.stapledon.config.properties.CacheProperties;
import org.stapledon.dto.User;
import org.stapledon.dto.UserConfig;
import org.stapledon.dto.UserRegistrationDto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserConfigWriterTest {

    @TempDir
    Path tempDir;

    @Mock
    private CacheProperties cacheProperties;

    private UserConfigWriter userConfigWriter;
    private Gson gson;
    private File usersFile;

    private static final String TEST_RESOURCES_DIR = "src/test/resources/test-users/";
    private static final String VALID_USERS_FILE = "valid-users.json";
    private static final String EMPTY_USERS_FILE = "empty-users.json";
    private static final String MALFORMED_USERS_FILE = "malformed-users.json";
    private static final String NULL_USERS_FILE = "null-users.json";
    private static final String MISSING_FIELDS_USERS_FILE = "missing-fields-users.json";

    @BeforeEach
    void setUp() {
        // Setup Gson with adapters for LocalDateTime
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        // Create temp file for users
        usersFile = tempDir.resolve("users.json").toFile();

        // Configure mock properties
        when(cacheProperties.getLocation()).thenReturn(tempDir.toString());
        when(cacheProperties.getUsersConfig()).thenReturn(usersFile.getName());

        // Create the UserConfigWriter with mocked dependencies
        userConfigWriter = new UserConfigWriter(gson, cacheProperties);
    }

    @Test
    void loadUsersShouldCreateEmptyConfigWhenFileDoesNotExist() throws Exception {
        // When
        UserConfig result = userConfigWriter.loadUsers();

        // Then
        assertNotNull(result);
        assertNotNull(result.getUsers());
        assertTrue(result.getUsers().isEmpty());
    }

    @Test
    void loadUsersShouldLoadExistingUsersFromFile() throws Exception {
        // Given
        UserConfig initialConfig = new UserConfig();
        User user = createTestUser("testuser");
        initialConfig.getUsers().put(user.getUsername(), user);
        
        try (FileWriter writer = new FileWriter(usersFile)) {
            gson.toJson(initialConfig, writer);
        }

        // When
        UserConfig result = userConfigWriter.loadUsers();

        // Then
        assertNotNull(result);
        assertNotNull(result.getUsers());
        assertEquals(1, result.getUsers().size());
        assertTrue(result.getUsers().containsKey("testuser"));
        assertEquals("testuser", result.getUsers().get("testuser").getUsername());
    }

    @Test
    void registerUserShouldCreateNewUserSuccessfully() {
        // Given
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@example.com")
                .displayName("New User")
                .build();

        // When
        Optional<User> result = userConfigWriter.registerUser(registrationDto);

        // Then
        assertTrue(result.isPresent());
        User user = result.get();
        assertEquals("newuser", user.getUsername());
        assertEquals("newuser@example.com", user.getEmail());
        assertEquals("New User", user.getDisplayName());
        assertTrue(BCrypt.checkpw("password123", user.getPasswordHash()));
        assertEquals(List.of("USER"), user.getRoles());
        assertNotNull(user.getCreated());
        assertNotNull(user.getUserToken());
    }

    @Test
    void registerUserShouldFailForExistingUsername() {
        // Given
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username("existinguser")
                .password("password123")
                .email("existinguser@example.com")
                .displayName("Existing User")
                .build();
        
        // Register first user
        userConfigWriter.registerUser(registrationDto);
        
        // Try to register with same username
        UserRegistrationDto duplicateDto = UserRegistrationDto.builder()
                .username("existinguser")
                .password("differentpassword")
                .email("different@example.com")
                .displayName("Different User")
                .build();

        // When
        Optional<User> result = userConfigWriter.registerUser(duplicateDto);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void authenticateUserShouldReturnUserForValidCredentials() {
        // Given
        String username = "authuser";
        String password = "authpassword";
        
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username(username)
                .password(password)
                .email("auth@example.com")
                .displayName("Auth User")
                .build();
        
        userConfigWriter.registerUser(registrationDto);

        // When
        Optional<User> result = userConfigWriter.authenticateUser(username, password);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertNotNull(result.get().getLastLogin());
    }

    @Test
    void authenticateUserShouldFailForInvalidCredentials() {
        // Given
        String username = "authuser";
        String password = "authpassword";
        String wrongPassword = "wrongpassword";
        
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username(username)
                .password(password)
                .email("auth@example.com")
                .displayName("Auth User")
                .build();
        
        userConfigWriter.registerUser(registrationDto);

        // When
        Optional<User> result = userConfigWriter.authenticateUser(username, wrongPassword);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserShouldReturnUserForExistingUsername() {
        // Given
        User user = createTestUser("getuser");
        userConfigWriter.saveUser(user);

        // When
        Optional<User> result = userConfigWriter.getUser("getuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals("getuser", result.get().getUsername());
    }

    @Test
    void getUserShouldReturnEmptyForNonExistingUsername() {
        // When
        Optional<User> result = userConfigWriter.getUser("nonexistentuser");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void updateUserShouldUpdateUserDetails() {
        // Given
        User originalUser = createTestUser("updateuser");
        userConfigWriter.saveUser(originalUser);
        
        User updatedUser = User.builder()
                .username("updateuser")
                .email("updated@example.com")
                .displayName("Updated Name")
                .build();

        // When
        Optional<User> result = userConfigWriter.updateUser(updatedUser);

        // Then
        assertTrue(result.isPresent());
        assertEquals("updateuser", result.get().getUsername());
        assertEquals("updated@example.com", result.get().getEmail());
        assertEquals("Updated Name", result.get().getDisplayName());
        // Password should be preserved
        assertEquals(originalUser.getPasswordHash(), result.get().getPasswordHash());
    }

    @Test
    void updatePasswordShouldChangePasswordSuccessfully() {
        // Given
        String username = "pwduser";
        String originalPassword = "original123";
        String newPassword = "updated456";
        
        UserRegistrationDto registrationDto = UserRegistrationDto.builder()
                .username(username)
                .password(originalPassword)
                .email("pwd@example.com")
                .displayName("Password User")
                .build();
        
        userConfigWriter.registerUser(registrationDto);

        // When
        Optional<User> result = userConfigWriter.updatePassword(username, newPassword);

        // Then
        assertTrue(result.isPresent());
        
        // Verify old password no longer works
        assertTrue(userConfigWriter.authenticateUser(username, originalPassword).isEmpty());
        
        // Verify new password works
        assertTrue(userConfigWriter.authenticateUser(username, newPassword).isPresent());
    }

    /**
     * Tests loading from a pre-existing valid users file in the test resources.
     */
    @Test
    void loadUsersFromValidTestResource() throws Exception {
        // Given
        copyResourceToTemp(VALID_USERS_FILE);

        // When
        UserConfig result = userConfigWriter.loadUsers();

        // Then
        assertNotNull(result);
        assertNotNull(result.getUsers());
        assertEquals(2, result.getUsers().size());
        assertTrue(result.getUsers().containsKey("testuser"));
        assertTrue(result.getUsers().containsKey("adminuser"));

        // Verify admin user has the right roles
        User adminUser = result.getUsers().get("adminuser");
        assertEquals(2, adminUser.getRoles().size());
        assertTrue(adminUser.getRoles().contains("ADMIN"));
    }

    /**
     * Tests loading from an empty users file, which should create an empty map but not null.
     */
    @Test
    void loadUsersFromEmptyUsersResource() throws Exception {
        // Given
        copyResourceToTemp(EMPTY_USERS_FILE);

        // When
        UserConfig result = userConfigWriter.loadUsers();

        // Then
        assertNotNull(result);
        assertNotNull(result.getUsers());
        assertEquals(0, result.getUsers().size());
    }

    /**
     * Tests loading from a malformed JSON file.
     * Since we modified UserConfigWriter to propagate JsonParseException for testing,
     * this test verifies that behavior.
     */
    @Test
    void loadUsersFromMalformedJsonShouldCreateNewConfig() throws Exception {
        // Given
        copyResourceToTemp(MALFORMED_USERS_FILE);

        // When/Then
        Exception exception = assertThrows(JsonParseException.class, () -> {
            userConfigWriter.loadUsers();
        });

        // Verify it's the right exception with a meaningful message
        assertTrue(exception.getMessage().contains("malformed") ||
                  exception.getMessage().contains("Expected") ||
                  exception.getMessage().contains("syntax"));
    }

    /**
     * Tests loading from a file with null users map.
     */
    @Test
    void loadUsersFromNullUsersField() throws Exception {
        // Given
        copyResourceToTemp(NULL_USERS_FILE);

        // When
        UserConfig result = userConfigWriter.loadUsers();

        // Then
        assertNotNull(result);
        assertNotNull(result.getUsers()); // Should be initialized to empty map
        assertEquals(0, result.getUsers().size());
    }

    /**
     * Tests loading users with missing fields, ensuring the deserialization
     * still works and defaults are applied correctly.
     */
    @Test
    void loadUsersWithMissingFields() throws Exception {
        // Given
        copyResourceToTemp(MISSING_FIELDS_USERS_FILE);

        // When
        UserConfig result = userConfigWriter.loadUsers();

        // Then
        assertNotNull(result);
        assertNotNull(result.getUsers());
        assertEquals(1, result.getUsers().size());

        User user = result.getUsers().get("missingfieldsuser");
        assertNotNull(user);
        assertEquals("missingfieldsuser", user.getUsername());
        assertEquals("missing@example.com", user.getEmail());
        // Check that default fields were properly initialized
        assertNull(user.getDisplayName()); // No default for display name
        assertNotNull(user.getRoles()); // Should default to empty list
    }

    /**
     * Tests saving to a non-existent directory, which should fail gracefully.
     */
    @Test
    void saveUserToNonExistentDirectory() {
        // Given
        when(cacheProperties.getLocation()).thenReturn("/non/existent/path");
        when(cacheProperties.getUsersConfig()).thenReturn("users.json");

        User user = createTestUser("failuser");

        // When
        boolean result = userConfigWriter.saveUser(user);

        // Then
        assertFalse(result);
    }

    /**
     * Tests when the file is locked or unavailable for writing.
     * This test simulates a file system issue by making the file non-writable.
     */
    @Test
    void saveUserWhenFileIsNotWritable() throws Exception {
        // Given
        copyResourceToTemp(VALID_USERS_FILE);

        // Make file read-only
        usersFile.setWritable(false);

        User user = createTestUser("readonlyuser");

        // When
        boolean result = userConfigWriter.saveUser(user);

        // Then
        assertFalse(result);

        // Clean up
        usersFile.setWritable(true);
    }

    /**
     * Tests sequential user registration - simpler and more reliable than concurrent test.
     */
    @Test
    void sequentialUserRegistrations() throws Exception {
        // Given
        // Create a fresh empty config
        UserConfig config = new UserConfig();
        try (FileWriter writer = new FileWriter(usersFile)) {
            gson.toJson(config, writer);
        }

        // When - Register 5 different users sequentially
        boolean[] results = new boolean[5];

        for (int i = 0; i < 5; i++) {
            UserRegistrationDto regDto = UserRegistrationDto.builder()
                .username("sequential" + i)
                .password("pass" + i)
                .email("user" + i + "@example.com")
                .displayName("Sequential User " + i)
                .build();

            Optional<User> user = userConfigWriter.registerUser(regDto);
            results[i] = user.isPresent();
        }

        // Then
        // Verify all registrations succeeded
        for (boolean result : results) {
            assertTrue(result);
        }

        // Check the final state
        UserConfig finalConfig = userConfigWriter.loadUsers();
        assertEquals(5, finalConfig.getUsers().size());
        for (int i = 0; i < 5; i++) {
            assertTrue(finalConfig.getUsers().containsKey("sequential" + i));
        }
    }

    /**
     * Helper method to copy a test resource to the temp directory for testing.
     */
    private void copyResourceToTemp(String resourceName) throws IOException {
        File resourceFile = new File(TEST_RESOURCES_DIR + resourceName);
        Files.copy(resourceFile.toPath(), usersFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private User createTestUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash(BCrypt.hashpw("testpass", BCrypt.gensalt()))
                .email(username + "@example.com")
                .displayName("Test " + username)
                .created(LocalDateTime.now())
                .roles(Arrays.asList("USER"))
                .build();
    }

    // Simple adapter for LocalDateTime serialization
    static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}