package org.stapledon.api.dto.preference;

import java.time.LocalDate;
import java.util.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class UserPreference {
    @ToString.Include private String username;

    @Builder.Default private List<Integer> favoriteComics = new ArrayList<>();

    @Builder.Default private Map<Integer, LocalDate> lastReadDates = new HashMap<>();

    @Builder.Default private Map<String, Object> displaySettings = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserPreference that = (UserPreference) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}