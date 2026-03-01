package org.stapledon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.infrastructure.config.UserConfigWriter;
import org.stapledon.infrastructure.security.JwtTokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StapledonAccountGivens implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private final AtomicInteger counter = new AtomicInteger(1000);

    @Autowired private UserConfigWriter userConfigWriter;

    private static JwtTokenUtil jwtTokenUtil() {
        return applicationContext.getBean(JwtTokenUtil.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        StapledonAccountGivens.applicationContext = applicationContext;
    }

    public GivenAccountContext givenAdministrator() {
        return givenUser(AccountInfoParameters.builder()
                .username("admin")
                .firstName("Administrator")
                .password("test_password")
                .email("admin@stapledon.ca")
                .build());
    }

    public GivenAccountContext givenUser() {
        return givenUser(AccountInfoParameters.builder()
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .password("test_password")
                .email("testuser@stapledon.ca")
                .build());
    }

    public GivenAccountContext givenUser(AccountInfoParameters parameters) {
        if (userConfigWriter.existsByUsername(parameters.username)) {
            userConfigWriter.updatePassword(parameters.getUsername(), parameters.getPassword());
        } else {
            userConfigWriter.registerUser(UserRegistrationDto.builder()
                    .username(parameters.getUsername())
                    .password(parameters.getPassword())
                    .email(parameters.getEmail())
                    .displayName(parameters.getFirstName() + " " + parameters.getLastName())
                    .build());
        }
        return new GivenAccountContext()
                .username(parameters.getUsername())
                .password(parameters.getPassword())
                .firstName(parameters.getFirstName())
                .lastName(parameters.getLastName())
                .email(parameters.getEmail());
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AccountInfoParameters {
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String email;
        private List<String> roles;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class GivenAccountContext {
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String email;
        private List<String> roles;

        public GivenAccountContext username(String username) {
            this.username = username;
            return this;
        }

        public GivenAccountContext password(String password) {
            this.password = password;
            return this;
        }

        public GivenAccountContext firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public GivenAccountContext lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public GivenAccountContext email(String email) {
            this.email = email;
            return this;
        }

        public GivenAccountContext roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public String authenticate() {
            // Collect non-null, non-blank name parts into a single string
            String displayName = Stream.of(firstName, lastName)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .collect(Collectors.joining(" "));

            return jwtTokenUtil().generateToken(User.builder()
                    .username(this.username)
                    .email(this.email)
                    .displayName(displayName.isEmpty() ? "Anonymous" : displayName)
                    .roles(roles == null ? Collections.singletonList("USER") : roles)
                    .build());
        }
    }
}
