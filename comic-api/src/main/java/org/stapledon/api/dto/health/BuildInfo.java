package org.stapledon.api.dto.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DTO for build-related information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(onlyExplicitlyIncluded = true)
public class BuildInfo {

    /**
     * Application name
     */
    @ToString.Include
    private String name;

    /**
     * Application artifact
     */
    private String artifact;

    /**
     * Application group
     */
    private String group;

    /**
     * Application version
     */
    @ToString.Include
    private String version;

    /**
     * Build timestamp
     */
    private String buildTime;

    /**
     * Java version
     */
    private String javaVersion;
}