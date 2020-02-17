package com.fahlberg.model;

public class CreateHeatmapRequest {
    private String fromDateUTC;
    private String toDateUTC;

    public CreateHeatmapRequest(String fromDateUTC, String toDateUTC) {
        this.fromDateUTC = fromDateUTC;
        this.toDateUTC = toDateUTC;
    }

    public String getFromDateUTC() {
        return fromDateUTC;
    }

    public void setFromDateUTC(String fromDateUTC) {
        this.fromDateUTC = fromDateUTC;
    }

    public String getToDateUTC() {
        return toDateUTC;
    }

    public void setToDateUTC(String toDateUTC) {
        this.toDateUTC = toDateUTC;
    }
}