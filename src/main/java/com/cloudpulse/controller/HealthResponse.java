package com.cloudpulse.controller;

public class HealthResponse {

    private String status;
    private String application;

    // Constructor
    public HealthResponse(String status, String application){
        this.status = status;
        this.application = application;
    }

    // Getter methods
    public String getStatus(){
        return status;
    }

    public String getApplication(){
        return application;
    }

    // Setter methods
    public void setStatus(String status){
        this.status = status;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}