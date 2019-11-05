package org.stapledon.dto;

import java.util.List;
import java.util.Objects;

public class ImageCacheStats
{
    public ImageCacheStats()
    {
        // No-argument constructor required for gson
    }
    public String oldestImage;
    public String newestImage;

    public List<String> years;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageCacheStats that = (ImageCacheStats) o;
        return  Objects.equals(oldestImage, that.oldestImage) &&
                Objects.equals(newestImage, that.newestImage) &&
                Objects.equals(years, that.years);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldestImage, newestImage, years);
    }
}
