package org.stapledon.api.dto.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class UserRegistrationDto {
    @ToString.Include private String username;

    private String password;

    @ToString.Include private String email;

    @ToString.Include private String displayName;
}