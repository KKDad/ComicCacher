package org.stapledon;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.stapledon.api.dto.user.User;
import org.stapledon.api.dto.user.UserRegistrationDto;
import org.stapledon.infrastructure.config.UserConfigWriter;
import org.stapledon.infrastructure.security.JwtTokenUtil;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StapledonAccountGivens implements ApplicationContextAware {

    private final AtomicInteger counter = new AtomicInteger(1000);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StapledonAccountGivens.applicationContext = applicationContext;
    }

    private static ApplicationContext applicationContext;

    protected static JwtTokenUtil jwtTokenUtil() {
        return applicationContext.getBean(JwtTokenUtil.class);
    }

    // User Givens
    public static class AccountInfoParameters {
        private String username = "user";
        private String password = "password";
        private String firstName = "John";
        private String lastName = "Doe";
        private String email = "test@stapledon.ca";

        public static AccountInfoParameters builder() {
            return new AccountInfoParameters();
        }

        public AccountInfoParameters username(String username) {
            this.username = username;
            return this;
        }

        public AccountInfoParameters password(String password) {
            this.password = password;
            return this;
        }

        public AccountInfoParameters firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public AccountInfoParameters lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public AccountInfoParameters email(String email) {
            this.email = email;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }

        public AccountInfoParameters build() {
            return this;
        }
    }

    public static class GivenAccountContext {
        private Long id;
        private String username;
        private String password;
        private String firstName;
        private String lastName;
        private String email;

        public static GivenAccountContext builder() {
            return new GivenAccountContext();
        }

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

        public GivenAccountContext id(Long id) {
            this.id = id;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }

        public Long getId() {
            return id;
        }

        public GivenAccountContext build() {
            return this;
        }

        public String authenticate() {
            // Skip logging to avoid Lombok issues
            return jwtTokenUtil().generateToken(User.builder().build());
        }
    }

    public GivenAccountContext givenAdministrator() {
        return givenUser(AccountInfoParameters.builder()
                .username("admin")
                .firstName("Administrator")
                .email("admin@stapledon.ca")
                .build());
    }

    public GivenAccountContext givenUser() {
        return givenUser(AccountInfoParameters
                .builder()
                .email("user@stapledon.ca")
                .build());
    }
    
    @Autowired
    private UserConfigWriter userConfigWriter;
    
    public GivenAccountContext givenUser(AccountInfoParameters parameters) {
        userConfigWriter.registerUser(UserRegistrationDto.builder()
                .username(parameters.getUsername())
                .password(parameters.getPassword())
                .email(parameters.getEmail())
                .displayName(parameters.getFirstName() + " " + parameters.getLastName())
                .build());

        return GivenAccountContext.builder()
                .username(parameters.getUsername())
                .password(parameters.getPassword())
                .firstName(parameters.getFirstName())
                .lastName(parameters.getLastName())
                .email(parameters.getEmail())
                .build();
    }
}