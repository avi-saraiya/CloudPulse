package com.cloudpulse.controller;

import com.cloudpulse.entity.ServiceEntity;
import com.cloudpulse.repository.ServiceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceController {

    // Simple reads go straight to the repository; anything with business rules lives in the service/ package.
    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    // GET /api/services -> all three simulated services with their current status
    @GetMapping("/services")
    public List<ServiceEntity> getAllServices() {
        // Sort by id so the list order never changes (PostgreSQL returns rows in no guaranteed order)
        return serviceRepository.findAll(Sort.by("id"));
    }

    // GET /api/services/1 -> one service, or 404 if the id does not exist
    @GetMapping("/services/{id}")
    public ServiceEntity getService(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service " + id + " not found"));
    }
}