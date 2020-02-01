package com.fahlberg.http;

import java.util.List;
import java.util.Map;

public class HttpResponse {
    public int statusCode;
    public String body;
    public Map<String, List<String>> headers;
    public HttpResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }
}