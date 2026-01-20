package org.stapledon.api.dto.auth;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class AuthResponse {
    private String token;
    private String refreshToken;

    @ToString.Include private String username;

    @ToString.Include private String displayName;
}