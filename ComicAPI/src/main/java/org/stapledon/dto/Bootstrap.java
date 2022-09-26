package org.stapledon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stapledon.config.GoComicsBootstrap;
import org.stapledon.config.KingComicsBootStrap;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bootstrap {
    private List<GoComicsBootstrap> dailyComics;
    private List<KingComicsBootStrap> kingComics;
}
