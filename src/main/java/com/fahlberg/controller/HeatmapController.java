package com.fahlberg.controller;

import com.fahlberg.model.Athlete;
import com.fahlberg.model.CreateHeatmapRequest;
import com.fahlberg.model.Heatmap;
import com.fahlberg.model.strava.StravaAthlete;
import com.fahlberg.repository.AthleteRepository;
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

    private HttpClient http = HttpClientBuilder.create().build();

    private final String API_URL = "https://www.strava.com/api/v3";
    private final Gson GSON = new Gson();

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String check() {
        return "Heatmap works?";
    }

    @RequestMapping(value = "heatmaps", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Heatmap>> getHeatmaps(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if(!athlete.isPresent()) {
            this.athleteRepository.saveAndFlush(new Athlete(user.id, user.firstname, user.lastname, new ArrayList<Heatmap>()));
        }
        return ResponseEntity.status(loginResponse.getStatusCode()).body(athlete.get().getHeatmaps());
    }

    @RequestMapping(value = "heatmaps/{id}", method = RequestMethod.GET)
    public ResponseEntity getHeatmap(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if(!athlete.isPresent()) {
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
        AbstractMap.SimpleEntry<Date, Date> range = null; // TODO Java.Time instead of depricated utilDate
        try {
            range = new AbstractMap.SimpleEntry(new Date(request.getFromDateUTC()), new Date(request.getToDateUTC()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if(!athlete.isPresent()) {
            this.athleteRepository.saveAndFlush(new Athlete(user.id, user.firstname, user.lastname, new ArrayList<Heatmap>()));
        }
        List<String> heatmapData = this.getPolylines(range.getKey(), range.getValue(), authorization);
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
        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if(!athlete.isPresent()) {
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
        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Optional<Athlete> athlete = athleteRepository.findById(user.id);
        if(!athlete.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.get().getHeatmaps().stream().filter(ath -> ath.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            heatmap.get().setPolylines(this.getPolylines(heatmap.get().getRangeStart(), heatmap.get().getRangeEnd(), authorization));
            heatmap.get().setLastModified(new Date());
            Athlete flushedObj = this.athleteRepository.saveAndFlush(athlete.get());
            return ResponseEntity.status(HttpStatus.OK).body(flushedObj);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private ResponseEntity checkCredentials(String authorization) {
        try {
            HttpUriRequest request = RequestBuilder.get()
                    .setUri(API_URL + "/athlete/")
                    .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                    .build();
            HttpResponse response = http.execute(request);
            return ResponseEntity.status(response.getStatusLine().getStatusCode()).body(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @RequestMapping(value = "samples", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Heatmap>> getSampleHeatmaps() {
        Athlete athlete = null;
        try {
            athlete = athleteRepository.findById(0).get();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(athlete.getHeatmaps());
    }

    @RequestMapping(value = "samples/{id}", method = RequestMethod.GET)
    public ResponseEntity<Heatmap> getSample(@PathVariable Integer id) {
        Athlete athlete = null;
        try {
            athlete = athleteRepository.findById(0).get();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.getHeatmaps().stream().filter(val -> val.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(heatmap.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    public List<String> getPolylines(Date startDate, Date endDate, String authorization) {
        List<String> heatmapComponentIDs = new ArrayList<>();
        try {
            int index = 0;
            int httpCode;
            String httpBody;
            do {
                URIBuilder builder = new URIBuilder(API_URL + "/athlete/activities");
                builder.addParameter("page", String.valueOf(++index));
                builder.addParameter("per_page", String.valueOf(50));
                builder.addParameter("after", String.valueOf((int) (startDate.getTime() / 1000)));
                builder.addParameter("before", String.valueOf((int) (endDate.getTime() / 1000)));

                HttpUriRequest request = RequestBuilder.get()
                        .setUri(builder.toString())
                        .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                        .build();
                HttpResponse httpResponse = http.execute(request);
                httpBody = EntityUtils.toString(httpResponse.getEntity());
                httpCode = httpResponse.getStatusLine().getStatusCode();

                JsonArray jsonObject = GSON.fromJson(httpBody, JsonArray.class);
                jsonObject.forEach(item -> {
                    JsonObject obj = (JsonObject) item;
                    if (!obj.get("map").getAsJsonObject().get("summary_polyline").isJsonNull()) {
                        heatmapComponentIDs.add(obj.get("id").toString());
                    }
                });
            } while (httpCode == 200 && !httpBody.equals("[]"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return heatmapComponentIDs.stream().parallel()
                .map(id -> {
                    try {
                        HttpUriRequest request = RequestBuilder.get()
                                .setUri(API_URL + "/activities/" + id)
                                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                                .build();
                        HttpResponse response = http.execute(request);
                        JsonObject jsonObject = GSON.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class); // TODO handle errors
                        return jsonObject.get("map").getAsJsonObject().get("polyline").getAsString();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }
}