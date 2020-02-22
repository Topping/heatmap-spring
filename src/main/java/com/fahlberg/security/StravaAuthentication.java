package com.fahlberg.security;

import com.fahlberg.model.strava.StravaAthlete;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.security.auth.Subject;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public class StravaAuthentication implements Authentication, Serializable {
    private final String authToken;
    private final StravaAthlete stravaAthlete;
    private boolean isAuthenticated;

    public StravaAuthentication(String authToken, StravaAthlete stravaAthlete) {
        this.authToken = authToken;
        this.stravaAthlete = stravaAthlete;
        this.isAuthenticated = true;
    }

    // TODO: Roles
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public Object getCredentials() {
        return authToken;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return stravaAthlete;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean b) throws IllegalArgumentException {
        this.isAuthenticated = b;
    }

    @Override
    public String getName() {
        return stravaAthlete != null ? String.valueOf(stravaAthlete.id) : "";
    }
}
