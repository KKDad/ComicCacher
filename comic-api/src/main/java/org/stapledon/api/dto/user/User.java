package org.stapledon.api.dto.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class User {
    @ToString.Include
    private String username;

    private String passwordHash;

    @ToString.Include
    private String email;

    @ToString.Include
    private String displayName;

    @Builder.Default
    private LocalDateTime created = LocalDateTime.now();

    private LocalDateTime lastLogin;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Builder.Default
    private UUID userToken = UUID.randomUUID();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}