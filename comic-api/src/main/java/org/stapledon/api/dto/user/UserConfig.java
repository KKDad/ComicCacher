package org.stapledon.api.dto.user;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Component
public class UserConfig {
    @ToString.Include
    private Map<String, User> users = new ConcurrentHashMap<>();
}
