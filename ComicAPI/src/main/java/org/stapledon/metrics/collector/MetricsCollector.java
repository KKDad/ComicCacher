package org.stapledon.metrics.collector;

/**
 * Interface for collecting metrics data.
 *
 * This interface defines the contract for metrics collection components.
 * Current implementations:
 * - StorageMetricsCollector: Scans filesystem for storage metrics
 * - AccessMetricsCollector: Tracks comic access patterns and timing
 *
 * Future implementations could support:
 * - Different collection strategies
 * - External metrics systems (Victoria Metrics, Prometheus, etc.)
 * - Custom metrics collectors
 *
 * Note: This is currently a documentation interface. Implementations
 * are concrete classes that may evolve to formally implement this interface
 * in future phases.
 */
public interface MetricsCollector {

    /**
     * Triggers metrics collection.
     * For storage metrics, this typically scans the filesystem.
     * For access metrics, this persists in-memory counters to disk.
     */
    void collectMetrics();

    /**
     * Returns the name of this collector for logging/debugging.
     */
    String getCollectorName();
}
