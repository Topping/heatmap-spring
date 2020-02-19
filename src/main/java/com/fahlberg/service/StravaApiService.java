package com.fahlberg.service;

import com.fahlberg.model.strava.StravaAthlete;
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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StravaApiService {
    private HttpClient http;
    private final String API_URL = "https://www.strava.com/api/v3";
    private final Gson GSON;

    public StravaApiService() {
        http = HttpClientBuilder.create().build();
        GSON = new Gson();
    }

    public StravaAthlete getCurrentAthlete(String authorization) throws IOException {
        HttpUriRequest request = RequestBuilder.get()
                .setUri(API_URL + "/athlete/")
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        HttpResponse response = http.execute(request);

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 200) {
            final StravaAthlete user = GSON.fromJson(EntityUtils.toString(response.getEntity()), StravaAthlete.class);
            return user;
        } else {
            return null; // TODO handle 401 better
        }
    }

    public List<String> getPolylines(Date startDate, Date endDate, String authorization) throws IOException {
        List<String> activityIds = new ArrayList<>();
        try {
            int index = 0;
            List<String> activityPageIds = new ArrayList<>();
            do {
                activityPageIds = this.getActivityIds(++index, 50, startDate, endDate, authorization);
                activityIds.addAll(activityPageIds);
            } while (!activityPageIds.isEmpty());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return activityIds.stream().parallel()
                .map(id -> {
                    try {
                        return this.getPolylinesById(id, authorization);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> getActivityIds(int page, int perPage, Date startDate, Date endDate, String authorization) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(API_URL + "/athlete/activities")
                .addParameter("page", String.valueOf(page))
                .addParameter("per_page", String.valueOf(perPage))
                .addParameter("after", String.valueOf((int) (startDate.getTime() / 1000)))
                .addParameter("before", String.valueOf((int) (endDate.getTime() / 1000)));

        HttpUriRequest request = RequestBuilder.get()
                .setUri(uriBuilder.toString())
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        HttpResponse httpResponse = http.execute(request);
        String httpBody = EntityUtils.toString(httpResponse.getEntity());
        int httpCode = httpResponse.getStatusLine().getStatusCode();
        if (httpCode != 200) {
            throw new IOException(); // TODO varies, 401
        }

        JsonArray jsonObject = GSON.fromJson(httpBody, JsonArray.class);
        List<String> activityIds = new ArrayList<>();
        jsonObject.forEach(activityJson -> {
            JsonObject activityJsonObj = (JsonObject) activityJson;
            if (!activityJsonObj.get("map").getAsJsonObject().get("summary_polyline").isJsonNull()) {
                activityIds.add(activityJsonObj.get("id").toString());
            }
        });
        return activityIds;
    }

    private String getPolylinesById(String id, String authorization) throws IOException {
        HttpUriRequest request = RequestBuilder.get()
                .setUri(API_URL + "/activities/" + id)
                .setHeader(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        HttpResponse response = http.execute(request);
        JsonObject jsonObject = GSON.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
        return jsonObject.get("map").getAsJsonObject().get("polyline").getAsString();
    }
}
