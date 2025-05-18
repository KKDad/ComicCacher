package org.stapledon.api.dto.user;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Component
public class UserConfig {
    public UserConfig() {
        this.users = new ConcurrentHashMap<>();
    }

    private Map<String, User> users;
}