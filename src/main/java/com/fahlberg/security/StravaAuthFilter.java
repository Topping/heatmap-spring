package com.fahlberg.security;

import com.fahlberg.model.strava.StravaAthlete;
import com.fahlberg.service.StravaApiService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static java.util.Objects.isNull;

@Component
public class StravaAuthFilter extends OncePerRequestFilter {
    private final StravaApiService stravaService;

    @Autowired
    public StravaAuthFilter(StravaApiService stravaService) {
        this.stravaService = stravaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String authToken = getHeaderValue(httpServletRequest, HttpHeaders.AUTHORIZATION);
        if (!authToken.equals("")) {
            StravaAthlete athlete = stravaService.getCurrentAthlete(authToken);
            if (athlete != null) {
                StravaAuthentication authentication = new StravaAuthentication(authToken, athlete);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }

    private String getHeaderValue(HttpServletRequest request, String name) {
        Enumeration<String> headers = request.getHeaders(name);
        if (isNull(headers) || !headers.hasMoreElements()) {
            return "";
        }
        String value = headers.nextElement();
        if (headers.hasMoreElements()) {
            throw new RuntimeException("More than one " + name + " header!"); // TODO: Create some auth exception to handle
        }

        if (value == null || "".equals(value)) {
            return "";
        }

        return value;
    }
}
