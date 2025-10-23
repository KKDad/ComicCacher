package org.stapledon.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.stapledon.core.auth.service.AuthService;
import org.stapledon.engine.downloader.ComicCacher;
import org.stapledon.core.comic.service.UpdateService;
import org.stapledon.core.preference.service.PreferenceService;
import org.stapledon.core.user.service.UserService;
import org.stapledon.metrics.collector.StorageMetricsCollector;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.config.properties.DailyRunnerProperties;
import org.stapledon.infrastructure.config.properties.JwtProperties;
import org.stapledon.common.config.properties.StartupReconcilerProperties;
import org.stapledon.infrastructure.scheduling.DailyRunner;
import org.stapledon.infrastructure.scheduling.StartupReconciler;
import org.stapledon.infrastructure.security.JwtAuthenticationEntryPoint;
import org.stapledon.infrastructure.security.JwtTokenFilter;
import org.stapledon.infrastructure.security.JwtTokenUtil;
import org.stapledon.infrastructure.security.JwtUserDetailsService;
import org.stapledon.common.infrastructure.web.WebInspector;

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
    public StorageMetricsCollector storageMetricsCollector() {
        return Mockito.mock(StorageMetricsCollector.class);
    }
}