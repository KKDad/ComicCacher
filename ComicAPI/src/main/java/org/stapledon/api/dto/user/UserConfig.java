package org.stapledon.api.dto.user;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class UserConfig {
    public UserConfig() {
        this.users = new ConcurrentHashMap<>();
    }

    private Map<String, User> users;
}