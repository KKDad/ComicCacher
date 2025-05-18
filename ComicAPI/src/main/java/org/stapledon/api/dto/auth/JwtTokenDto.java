package org.stapledon.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtTokenDto {
    private String token;
    private String refreshToken;
    private String username;
    private Instant issuedAt;
    private Instant expiresAt;
}