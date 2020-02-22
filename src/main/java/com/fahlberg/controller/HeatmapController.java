package com.fahlberg.controller;

import com.fahlberg.model.Athlete;
import com.fahlberg.model.CreateHeatmapRequest;
import com.fahlberg.model.Heatmap;
import com.fahlberg.model.strava.StravaAthlete;
import com.fahlberg.repository.AthleteRepository;
import com.fahlberg.security.SecurityUtils;
import com.fahlberg.service.StravaApiService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/")
public class HeatmapController {

    @Autowired
    private AthleteRepository athleteRepository;

    @Autowired
    private StravaApiService apiService;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String check() {
        return "Heatmap works?";
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "heatmaps", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Heatmap>> getHeatmaps(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        StravaAthlete user = SecurityUtils.getAthleteDetails();
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if (!athlete.isPresent()) {
            this.athleteRepository.saveAndFlush(new Athlete(user.id, user.firstname, user.lastname, new ArrayList<Heatmap>()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(athlete.get().getHeatmaps());
    }

    @RequestMapping(value = "heatmaps/{id}", method = RequestMethod.GET)
    public ResponseEntity getHeatmap(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        StravaAthlete user;
        try {
            user = apiService.getCurrentAthlete(authorization);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if (!athlete.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<Heatmap> heatmap = athlete.get().getHeatmaps().stream().filter(val -> val.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(heatmap);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(value = "heatmaps/create", method = RequestMethod.POST)
    public ResponseEntity create(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestBody CreateHeatmapRequest request) {
        AbstractMap.SimpleEntry<Date, Date> range;
        try {
            range = new AbstractMap.SimpleEntry(new Date(request.getFromDateUTC()), new Date(request.getToDateUTC()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        StravaAthlete user;
        try {
            user = apiService.getCurrentAthlete(authorization);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if (!athlete.isPresent()) {
            this.athleteRepository.saveAndFlush(new Athlete(user.id, user.firstname, user.lastname, new ArrayList<Heatmap>()));
        }
        List<String> heatmapData = null;
        try {
            heatmapData = apiService.getPolylines(range.getKey(), range.getValue(), authorization);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Heatmap heatmap = new Heatmap(
                formatter.format(range.getKey()) + " ⟶ " + formatter.format(range.getValue()),
                Calendar.getInstance().getTime(),
                range.getKey(),
                range.getValue(),
                heatmapData);

        athlete.get().getHeatmaps().add(heatmap);
        Athlete flushedObj = this.athleteRepository.saveAndFlush(athlete.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(flushedObj); // TODO 201 CREATED link heatmaps/{id}
    }

    @RequestMapping(value = "heatmaps/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Athlete> delete(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        StravaAthlete user;
        try {
            user = apiService.getCurrentAthlete(authorization);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if (!athlete.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.get().getHeatmaps().stream().filter(val -> val.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            athlete.get().getHeatmaps().removeIf(ath -> ath.getHeatmapID() == id);
            Athlete flushedObj = this.athleteRepository.saveAndFlush(athlete.get());
            return ResponseEntity.status(HttpStatus.OK).body(flushedObj);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(value = "heatmaps/{id}/refresh", method = RequestMethod.GET)
    public ResponseEntity<Athlete> refresh(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        StravaAthlete user;
        try {
            user = apiService.getCurrentAthlete(authorization);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if (!athlete.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.get().getHeatmaps().stream().filter(ath -> ath.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            try {
                heatmap.get().setPolylines(apiService.getPolylines(heatmap.get().getRangeStart(), heatmap.get().getRangeEnd(), authorization));
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            heatmap.get().setLastModified(new Date());
            Athlete flushedObj = this.athleteRepository.saveAndFlush(athlete.get());
            return ResponseEntity.status(HttpStatus.OK).body(flushedObj);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @RequestMapping(value = "samples", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Heatmap>> getSampleHeatmaps() {
        Optional<Athlete> athlete = athleteRepository.findById(0);
        if (!athlete.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(athlete.get().getHeatmaps());
    }

    @RequestMapping(value = "samples/{id}", method = RequestMethod.GET)
    public ResponseEntity<Heatmap> getSample(@PathVariable Integer id) {
        Optional<Athlete> athlete = athleteRepository.findById(0);
        if (!athlete.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.get().getHeatmaps().stream().filter(val -> val.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(heatmap.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}