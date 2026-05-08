package org.stapledon.common.infrastructure.web;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.stapledon.common.config.properties.DownloaderProperties;


/**
 * Provides User-Agent strings for outbound HTTP requests, with optional per-source overrides.
 *
 * <p>Reads from {@link DownloaderProperties}:
 * <pre>
 * downloader.user-agent.default-value=&lt;UA string&gt;          # global fallback
 * downloader.sources.&lt;source&gt;.user-agent=&lt;UA string&gt;       # optional per-source override
 * </pre>
 *
 * <p>If no global default is configured, falls back to a current Chrome desktop UA baked into this class.
 */
@Slf4j
@ToString
@Service
public class UserAgentService {

    /**
     * Built-in fallback used when {@code downloader.user-agent.default-value} is not configured.
     * Refresh periodically as Chrome major versions advance.
     */
    static final String FALLBACK_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    private final DownloaderProperties properties;

    public UserAgentService(DownloaderProperties properties) {
        this.properties = properties;
        int sourceCount = properties.getSources() == null ? 0 : properties.getSources().size();
        log.info("UserAgentService initialized: default UA configured, {} source overrides", sourceCount);
    }

    /**
     * Returns the User-Agent string for the given source, or the default if the source has no override (or the source name is null/blank).
     */
    public String getUserAgent(String source) {
        String override = properties.userAgentFor(source);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return getDefault();
    }

    /**
     * Returns the global default User-Agent.
     */
    public String getDefault() {
        DownloaderProperties.UserAgent userAgent = properties.getUserAgent();
        String configured = userAgent == null ? null : userAgent.getDefaultValue();
        return configured != null && !configured.isBlank() ? configured : FALLBACK_USER_AGENT;
    }
}
