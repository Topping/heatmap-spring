package com.fahlberg.model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity(name = "Heatmap")
@Table(name = "heatmap")
public class Heatmap {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long heatmapID;
    private String name;
    private Date lastModified;
    private Date rangeStart;
    private Date rangeEnd;


    @Lob
    @ElementCollection
    private List<String> polylines;


    public Heatmap() {
    }

    public Heatmap(String name, Date lastModified, Date rangeStart, Date rangeEnd, List<String> polylines) {
        this.name = name;
        this.lastModified = lastModified;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.polylines = polylines;
    }

    public long getHeatmapID() {
        return heatmapID;
    }

    public void setHeatmapID(long heatmapID) {
        this.heatmapID = heatmapID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPolylines() {
        return polylines;
    }

    public void setPolylines(List<String> data) {
        this.polylines = data;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(Date rangeStart) {
        this.rangeStart = rangeStart;
    }

    public Date getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(Date rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Heatmap heatmap = (Heatmap) o;
        return heatmapID == heatmap.heatmapID &&
                Objects.equals(name, heatmap.name) &&
                Objects.equals(lastModified, heatmap.lastModified) &&
                Objects.equals(polylines, heatmap.polylines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heatmapID, name, lastModified, polylines);
    }
}
