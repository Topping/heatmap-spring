package com.fahlberg.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpMethod {
    public HttpMethod() {
    }

    public HttpResponse get(String url, Map<String, String> headers) throws IOException {
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

        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return new HttpResponse(connection.getResponseCode(), result.toString(), connection.getHeaderFields());
    }
}
