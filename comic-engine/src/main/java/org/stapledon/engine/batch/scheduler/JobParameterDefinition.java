package org.stapledon.engine.batch.scheduler;

import java.util.List;

/**
 * Declares an optional parameter that a batch job accepts for manual triggers.
 */
public record JobParameterDefinition(
    String name,
    String label,
    String type,
    boolean required,
    String defaultValue,
    List<Option> options
) {

    /**
     * A selectable option for ENUM-type parameters.
     */
    public record Option(String value, String label) {
    }
}
