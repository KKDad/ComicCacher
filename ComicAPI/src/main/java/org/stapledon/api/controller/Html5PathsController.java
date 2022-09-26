package org.stapledon.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * https://stackoverflow.com/questions/44692781/configure-spring-boot-to-redirect-404-to-a-single-page-app
 * <p>
 * The configuration below will match all paths that do not contain a period and are not already mapped to
 * another controller and redirect them to the index page.
 * <p>
 * This fixes the browser refresh issue so that we don't serve up a 404 error
 */
@Controller
public class Html5PathsController {

    @RequestMapping(value = "/{[path:[^\\.]*}")
    public String redirect() {
        return "forward:/index.html";
    }
}