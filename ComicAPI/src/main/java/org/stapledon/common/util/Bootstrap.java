package org.stapledon.common.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stapledon.infrastructure.config.GoComicsBootstrap;
import org.stapledon.infrastructure.config.KingComicsBootStrap;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bootstrap {
    private List<GoComicsBootstrap> dailyComics;
    private List<KingComicsBootStrap> kingComics;
}
