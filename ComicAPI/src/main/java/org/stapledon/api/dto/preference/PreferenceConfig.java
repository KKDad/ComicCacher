package org.stapledon.api.dto.preference;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class PreferenceConfig {
    public PreferenceConfig() {
        this.preferences = new ConcurrentHashMap<>();
    }

    private Map<String, UserPreference> preferences;
}