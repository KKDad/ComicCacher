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
@ToString(exclude = "password")
public class UserRegistrationDto {
    private String username;
    private String password;
    private String email;
    private String displayName;
}