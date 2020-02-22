package com.fahlberg.security;

import com.fahlberg.model.strava.StravaAthlete;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static StravaAthlete getAthleteDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof StravaAuthentication) {
            if (authentication.getPrincipal() instanceof StravaAthlete) {
                return (StravaAthlete) authentication.getPrincipal();
            }
        }

        throw new RuntimeException("Shit happened, is bad. What"); // TODO: Throw some auth exception
    }
}
