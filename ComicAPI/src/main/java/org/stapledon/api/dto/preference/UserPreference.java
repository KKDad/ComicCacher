package org.stapledon.api.dto.preference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPreference {
    private String username;
    
    @Builder.Default
    private List<Integer> favoriteComics = new ArrayList<>();
    
    @Builder.Default
    private Map<Integer, LocalDate> lastReadDates = new HashMap<>();
    
    @Builder.Default
    private Map<String, Object> displaySettings = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPreference that = (UserPreference) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}