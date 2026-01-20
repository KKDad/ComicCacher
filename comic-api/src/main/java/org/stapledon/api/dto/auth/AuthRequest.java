package org.stapledon.api.dto.auth;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class AuthRequest {
    @ToString.Include private String username;

    private String password;
}