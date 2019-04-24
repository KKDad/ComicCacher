package org.stapledon.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class WebUtils
{
    private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    private HttpServletRequest request;

    @Autowired
    public void setRequest(HttpServletRequest request)
    {
        this.request = request;
        logger.warn("Request from {} for method {}.", this.getClientIp(), this.request.getMethod());
    }

    private String getClientIp()
    {
        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr))
                remoteAddr = request.getRemoteAddr();
        }

        return remoteAddr;
    }

}