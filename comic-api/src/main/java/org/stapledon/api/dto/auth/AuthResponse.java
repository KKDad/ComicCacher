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
@ToString(exclude = {"token", "refreshToken"})
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String displayName;
}