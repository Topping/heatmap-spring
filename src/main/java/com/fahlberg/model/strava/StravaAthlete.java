package com.fahlberg.model.strava;

public class StravaAthlete {
    public int id;
    private String username;
    public int resource_state;
    public String firstname;
    public String lastname;
    public String city;
    public String state;
    public String country;
    public String sex;
    public boolean premium;
    public boolean summit;
    public String created_at;
    public String updated_at;
    public int badge_type_id;
    public String profile_medium;
    public String profile;
    public String friend = null;
    public String follower = null;

    public StravaAthlete() {
    }
}