package com.fahlberg.demo.controller;

import com.fahlberg.demo.model.Athlete;
import com.fahlberg.demo.model.Heatmap;
import com.fahlberg.demo.repository.AthleteRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.util.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/")
public class HeatmapController {

    @Autowired
    private AthleteRepository athleteRepository;

    private final String API_URL = "https://www.strava.com/api/v3";
    private final Gson GSON = new Gson();

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String check() {
        return "Heatmap works?";
    }

    @RequestMapping(value = "heatmaps", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getHeatmaps(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Athlete athlete = null;
        try {
            athlete = athleteRepository.findById(user.id).get();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(loginResponse.getStatusCode()).body(athlete.getHeatmaps());
    }

    @RequestMapping(value = "heatmaps/{id}", method = RequestMethod.GET)
    public ResponseEntity getHeatmap(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Athlete athlete = null;
        try {
            athlete = athleteRepository.findById(user.id).get();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.getHeatmaps().stream().filter(val -> val.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(heatmap);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @RequestMapping(value = "heatmaps/create", method = RequestMethod.POST)
    public ResponseEntity create(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestBody StringPair pair) {
        Pair<Date, Date> range = null;
        try {
            range = new Pair(new Date(pair.getKey()), new Date(pair.getValue()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Athlete athlete = null;
        try {
            athlete = athleteRepository.findById(user.id).get();
        } catch (NoSuchElementException e) {
            athlete = new Athlete(user.id, user.firstname, user.lastname, new ArrayList<Heatmap>());
        }

        List<String> heatmapData = this.getPolylines(range.getKey(), range.getValue(), authorization);
        final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Heatmap heatmap = new Heatmap(
                formatter.format(range.getKey()) + " ⟶ " + formatter.format(range.getValue()),
                Calendar.getInstance().getTime(),
                heatmapData);

        athlete.getHeatmaps().add(heatmap);
        Athlete flushedObj = this.athleteRepository.saveAndFlush(athlete);
        return ResponseEntity.status(HttpStatus.CREATED).body(flushedObj); // TODO 201 CREATED link heatmaps/{id}
    }

    @RequestMapping(value = "heatmaps/{id}/delete", method = RequestMethod.DELETE)
    public ResponseEntity delete(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ResponseEntity loginResponse = checkCredentials(authorization);
        if (loginResponse.getStatusCode().value() != 200) {
            return loginResponse;
        }
        final StravaAthlete user = GSON.fromJson(loginResponse.getBody().toString(), StravaAthlete.class);
        Athlete athlete = null;
        try {
            athlete = athleteRepository.findById(user.id).get();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<Heatmap> heatmap = athlete.getHeatmaps().stream().filter(val -> val.getHeatmapID() == id).findFirst();
        if (heatmap.isPresent()) {
            athlete.getHeatmaps().removeIf(e -> e.getHeatmapID() == id);
            Athlete flushedObj = this.athleteRepository.saveAndFlush(athlete);
            return ResponseEntity.status(HttpStatus.OK).body(flushedObj);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

//     TODO need to preserve startDate and endDate for this feature
    @RequestMapping(value = "heatmaps/{id}/refresh", method = RequestMethod.POST)
    public ResponseEntity refresh(@PathVariable Integer id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private ResponseEntity checkCredentials(String authorization) {
        Map<String, String> headers = new HashMap();
        headers.put(HttpHeaders.AUTHORIZATION, authorization);
        HttpResponse response = null;
        try {
            response = httpGet(API_URL + "/athlete/", headers);
            return ResponseEntity.status(response.statusCode).body(response.body);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public List<String> getPolylines(Date startDate, Date endDate, String authorization) {
        Map<String, String> headers = new HashMap();
        headers.put(HttpHeaders.AUTHORIZATION, authorization);
        List<String> heatmapComponentIDs = new ArrayList<>();
        try {
            int index = 0;
            HttpResponse response = null;
            do {
                URIBuilder builder = new URIBuilder(API_URL + "/athlete/activities");
                builder.addParameter("page", String.valueOf(++index));
                builder.addParameter("per_page", String.valueOf(50));
                builder.addParameter("after", String.valueOf((int) (startDate.getTime() / 1000)));
                builder.addParameter("before", String.valueOf((int) (endDate.getTime() / 1000)));
                response = httpGet(builder.toString(), headers); // TODO handle HTTP codes
                JsonArray jsonObject = GSON.fromJson(response.body, JsonArray.class);
                jsonObject.forEach(item -> {
                    JsonObject obj = (JsonObject) item;
                    if (!obj.get("map").getAsJsonObject().get("summary_polyline").isJsonNull()) {
                        heatmapComponentIDs.add(obj.get("id").toString());
                    }
                });
            } while (response.statusCode == 200 && !response.body.equals("[]"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        List<String> polylines = heatmapComponentIDs.stream().parallel()
                .map(id -> {
                    try {
                        JsonObject jsonObject = GSON.fromJson(httpGet(API_URL + "/activities/" + id, headers).body, JsonObject.class); // TODO handle errors
                        return jsonObject.get("map").getAsJsonObject().get("polyline").getAsString();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());
        return polylines;
    }

    private HttpResponse httpGet(String url, Map<String, String> headers) throws IOException {
        StringBuilder result = new StringBuilder();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        headers.entrySet().forEach(header -> {
            connection.setRequestProperty(header.getKey(), header.getValue());
        });

        InputStream input;
        try {
            input = connection.getInputStream();
        } catch (IOException e) {
            return new HttpResponse(connection.getResponseCode(), connection.getResponseMessage(), connection.getHeaderFields());
        }

        BufferedReader rd = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return new HttpResponse(connection.getResponseCode(), result.toString(), connection.getHeaderFields());
    }

    private class HttpResponse {
        public int statusCode;
        public String body;
        public Map<String, List<String>> headers;

        public HttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
    }

    private class StravaAthlete {
        public int id;
        public String username;
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


}

// Spring isn't fond of generics, so this class now exists
class StringPair extends Pair<String, String> {
    public StringPair(String key, String value) {
        super(key, value);
    }
}