package org.stapledon.api.dto.auth;

import java.time.Instant;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class JwtTokenDto {
    private String token;
    private String refreshToken;

    @ToString.Include private String username;

    @ToString.Include private Instant issuedAt;

    @ToString.Include private Instant expiresAt;
}
