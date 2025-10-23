package org.stapledon.api.dto.preference;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreferenceConfig {
    public PreferenceConfig() {
        this.preferences = new ConcurrentHashMap<>();
    }

    private Map<String, UserPreference> preferences;
}