package com.example.online_learning.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("health", "/health");
        links.put("swaggerUi", "/swagger-ui.html");
        links.put("apiDocs", "/v3/api-docs");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "online_learning API");
        body.put(
                "message",
                "This URL is the backend only. Open your Railway frontend service URL for the web UI.");
        body.put("links", links);
        return body;
    }
}
