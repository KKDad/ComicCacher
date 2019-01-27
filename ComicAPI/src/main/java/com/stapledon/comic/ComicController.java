package com.stapledon.comic;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.stapledon.interop.ComicItem;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ComicController {

    private final AtomicLong counter = new AtomicLong();

    //@RequestMapping(method=GET, path = "/comics/v1/list")
    @RequestMapping("/comics/v1/list")
    public List<ComicItem> list(@RequestParam(value="filter", defaultValue="") String filter)
    {
        List<ComicItem> results = new ArrayList<>();

        return results;
    }
}