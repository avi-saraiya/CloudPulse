package com.cloudpulse.controller;

import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.entity.MetricEntity;
import com.cloudpulse.entity.ServiceEntity;
import com.cloudpulse.repository.IncidentRepository;
import com.cloudpulse.repository.MetricRepository;
import com.cloudpulse.repository.ServiceRepository;
import com.cloudpulse.service.MonitoringService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

// Every endpoint the UI uses. Return values are turned into JSON automatically.
@RestController
@RequestMapping("/api")
public class ApiController {

    private final ServiceRepository serviceRepository;
    private final MetricRepository metricRepository;
    private final IncidentRepository incidentRepository;
    private final MonitoringService monitoringService;

    public ApiController(ServiceRepository serviceRepository, MetricRepository metricRepository,
                         IncidentRepository incidentRepository, MonitoringService monitoringService) {
        this.serviceRepository = serviceRepository;
        this.metricRepository = metricRepository;
        this.incidentRepository = incidentRepository;
        this.monitoringService = monitoringService;
    }

    @GetMapping("/services")
    public List<ServiceEntity> services() {
        return serviceRepository.findAll(Sort.by("id"));
    }

    // The newest reading for one service
    @GetMapping("/services/{id}/metrics")
    public MetricEntity latestMetric(@PathVariable Long id) {
        return metricRepository.findFirstByServiceIdOrderByIdDesc(id);
    }

    @GetMapping("/incidents")
    public List<IncidentEntity> incidents() {
        return incidentRepository.findAll(Sort.by("id").descending());
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public IncidentEntity acknowledge(@PathVariable Long id) {
        IncidentEntity incident = incidentRepository.findById(id).orElseThrow();
        incident.status = "ACKNOWLEDGED";
        incident.acknowledgedAt = OffsetDateTime.now();
        return incidentRepository.save(incident);
    }

    @PostMapping("/incidents/{id}/resolve")
    public IncidentEntity resolve(@PathVariable Long id) {
        IncidentEntity incident = incidentRepository.findById(id).orElseThrow();
        incident.status = "RESOLVED";
        incident.resolvedAt = OffsetDateTime.now();
        return incidentRepository.save(incident);
    }

    // Which services are in a failure mode, e.g. {"Payment API": "latency-spike"}
    @GetMapping("/simulation")
    public Map<String, String> modes() {
        return monitoringService.modes;
    }

    // POST /api/simulation/latency-spike?service=Payment API (also error-spike, outage).
    // POST /api/simulation/restore puts every service back to normal.
    @PostMapping("/simulation/{mode}")
    public Map<String, String> simulate(@PathVariable String mode, @RequestParam String service) {
        if (mode.equals("restore")) {
            monitoringService.modes.clear();
        } else {
            monitoringService.modes.put(service, mode);
        }
        return monitoringService.modes;
    }
}
