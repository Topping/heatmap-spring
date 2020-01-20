package com.fahlberg.demo.controller;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class HeatmapControllerTest {

    @Test
    void generateHeatmap() {
        HeatmapController hc = new HeatmapController();
//        assertEquals(hc.getPolylines(new Date(new Date().getTime() - (7 * 86400000 )), new Date(), "Bearer 21feb185c97910603056aadab256a2b30db26694"), null);
        // 1577818641 // 1 week ago
        // 1578426841
    }
}