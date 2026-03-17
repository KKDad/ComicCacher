package org.stapledon.common.util;

import org.stapledon.common.config.IComicsBootstrap;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bootstrap {
    private List<IComicsBootstrap> dailyComics;
    private List<IComicsBootstrap> kingComics;
}
