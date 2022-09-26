package org.stapledon.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bootstrap {
    private List<GoComicsBootstrap> dailyComics;
    private List<KingComicsBootStrap> kingComics;
}
