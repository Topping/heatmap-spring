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
