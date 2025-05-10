package org.stapledon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.stapledon.api.service.AuthService;
import org.stapledon.api.service.ComicsService;
import org.stapledon.api.service.DailyRunner;
import org.stapledon.api.service.PreferenceService;
import org.stapledon.api.service.StartupReconciler;
import org.stapledon.api.service.UpdateService;
import org.stapledon.api.service.UserService;
import org.stapledon.caching.ImageCacheStatsUpdater;
import org.stapledon.config.properties.CacheProperties;
import org.stapledon.config.properties.DailyRunnerProperties;
import org.stapledon.config.properties.JwtProperties;
import org.stapledon.config.properties.StartupReconcilerProperties;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.security.JwtAuthenticationEntryPoint;
import org.stapledon.security.JwtTokenFilter;
import org.stapledon.security.JwtTokenUtil;
import org.stapledon.security.JwtUserDetailsService;
import org.stapledon.web.WebInspector;

/**
 * Central configuration class for test beans
 * Provides mock implementations for all required beans in tests
 */
@Configuration
@Profile("test")
public class TestApplicationConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @Primary
    public JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-should-be-very-long-and-secure-for-testing-purposes-only");
        properties.setExpiration(300000); // 5 minutes
        properties.setRefreshExpiration(600000); // 10 minutes
        return properties;
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(jwtProperties());
    }

    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    // Mock service beans for tests

    @Bean
    @Primary
    public ComicsService comicsService() {
        return Mockito.mock(ComicsService.class);
    }

    @Bean
    @Primary
    public UpdateService updateService() {
        return Mockito.mock(UpdateService.class);
    }

    @Bean
    @Primary
    public StartupReconciler startupReconciler() {
        return Mockito.mock(StartupReconciler.class);
    }

    @Bean
    @Primary
    public DailyRunner dailyRunner() {
        return Mockito.mock(DailyRunner.class);
    }

    @Bean
    @Primary
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    @Primary
    public AuthService authService() {
        return Mockito.mock(AuthService.class);
    }

    @Bean
    @Primary
    public PreferenceService preferenceService() {
        return Mockito.mock(PreferenceService.class);
    }

    @Bean
    @Primary
    public JwtUserDetailsService jwtUserDetailsService() {
        return Mockito.mock(JwtUserDetailsService.class);
    }

    @Bean
    @Primary
    public JwtTokenFilter jwtTokenFilter() {
        return Mockito.mock(JwtTokenFilter.class);
    }

    // Mock configuration beans

    @Bean
    @Primary
    public CacheProperties cacheProperties() {
        CacheProperties properties = new CacheProperties();
        properties.setLocation("./test-cache");
        properties.setConfig("./test-comics.json");
        properties.setUsersConfig("./test-users.json");
        properties.setPreferencesConfig("./test-preferences.json");
        return properties;
    }

    @Bean
    @Primary
    public StartupReconcilerProperties startupReconcilerProperties() {
        return Mockito.mock(StartupReconcilerProperties.class);
    }

    @Bean
    @Primary
    public DailyRunnerProperties dailyRunnerProperties() {
        return Mockito.mock(DailyRunnerProperties.class);
    }

    @Bean
    @Primary
    public WebInspector webInspector() {
        return Mockito.mock(WebInspector.class);
    }

    @Bean
    @Primary
    public ComicCacher comicCacher() {
        return Mockito.mock(ComicCacher.class);
    }

    @Bean
    @Primary
    public ImageCacheStatsUpdater imageCacheStatsUpdater() {
        return Mockito.mock(ImageCacheStatsUpdater.class);
    }
}