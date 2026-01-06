package org.stapledon.api.dto.user;

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
public class UserRegistrationDto {
    @ToString.Include
    private String username;

    private String password;

    @ToString.Include
    private String email;

    @ToString.Include
    private String displayName;
}