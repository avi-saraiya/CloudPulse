package com.cloudpulse.controller;

import com.cloudpulse.entity.ServiceEntity;
import com.cloudpulse.repository.ServiceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/services")
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }
}