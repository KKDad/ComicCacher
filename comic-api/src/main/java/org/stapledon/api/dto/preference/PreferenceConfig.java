package org.stapledon.api.dto.preference;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PreferenceConfig {
    @Builder.Default
    private Map<String, UserPreference> preferences = new ConcurrentHashMap<>();
}