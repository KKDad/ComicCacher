package org.stapledon.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class ngRefreshController
{
    @Controller
    public class WebController {
        @RequestMapping(value = "/comics", method = RequestMethod.GET)
        public String index() {
            return "/";
        }
        @RequestMapping(value = "/redirect", method = RequestMethod.GET)
        public String redirect() {
            return "redirect:finalPage";
        }
    }
}
