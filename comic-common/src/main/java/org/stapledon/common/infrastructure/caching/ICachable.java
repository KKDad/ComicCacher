package org.stapledon.common.infrastructure.caching;


public interface ICachable {
    /**
     * Get the path full path where this comic has been cached. Include any augmentation
     *
     * @return Path
     */
    String cacheLocation();
}
