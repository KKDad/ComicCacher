package com.stapledon.comic;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;
import com.stapledon.interop.ComicConfig;
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
    public List<ComicItem> list(@RequestParam(value="filter", defaultValue="") String filter) throws FileNotFoundException {

        File initialFile = new File(ComicApiApplication.config.cacheDirectory + "/comics.json");
        InputStream inputStream = new FileInputStream(initialFile);
        Reader reader = new InputStreamReader(inputStream);
        ComicConfig comics = new Gson().fromJson(reader, ComicConfig.class);

        List list = new ArrayList(comics.items.values());
        //Collections.sort(list);
        return list;
    }
}