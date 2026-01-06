package org.stapledon.api.dto.auth;

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
public class AuthResponse {
    private String token;
    private String refreshToken;

    @ToString.Include
    private String username;

    @ToString.Include
    private String displayName;
}