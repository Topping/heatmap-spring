package com.fahlberg.demo.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "Athlete")
@Table(name = "athlete")
public class Athlete {
    @Id
    private Integer athleteID;
    private String firstname;
    private String lastname;

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Heatmap> heatmaps = new ArrayList<Heatmap>();

    public Athlete() {
    }

    public Athlete(int athleteID, String firstname, String lastname, List<Heatmap> heatmaps) {
        this.athleteID = athleteID;
        this.firstname = firstname;
        this.lastname = lastname;
        this.heatmaps = heatmaps;
    }

    public int getAthleteID() {
        return athleteID;
    }

    public void setAthleteID(int athleteID) {
        this.athleteID = athleteID;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public List<Heatmap> getHeatmaps() {
        return heatmaps;
    }

    public void setHeatmaps(List<Heatmap> heatmaps) {
        this.heatmaps = heatmaps;
    }
}
