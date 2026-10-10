package com.cloudpulse.controller;

// Anotation imports
// This one lets a class return an HTTP response in JSON
import org.springframework.web.bind.annotation.RestController;
// This one tells the class where in the URL to look
import org.springframework.web.bind.annotation.RequestMapping;
// This one tells the class which URL attribute to respond for
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api")
public class HealthController {

    // This will map the method below to respond to the /health part of the url
    @GetMapping("/health")
    public HealthResponse getHealth(){
        // We're returning an instance of the HealthResponse class to the GET call as JSON
        return new HealthResponse("UP", "CloudPulse");
    }

}