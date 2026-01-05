package org.stapledon.api.dto.auth;

import java.time.Instant;
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
@ToString(exclude = {"token", "refreshToken"})
public class JwtTokenDto {
    private String token;
    private String refreshToken;
    private String username;
    private Instant issuedAt;
    private Instant expiresAt;
}
