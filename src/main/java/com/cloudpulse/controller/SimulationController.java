package com.cloudpulse.controller;

import com.cloudpulse.repository.ServiceRepository;
import com.cloudpulse.service.SimulationService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Buttons for breaking and fixing things on purpose. Each call only changes the mode;
 * the next MonitoringService tick (within 5 seconds) generates the matching telemetry.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;
    private final ServiceRepository serviceRepository;
    private final MeterRegistry meterRegistry;

    public SimulationController(SimulationService simulationService, ServiceRepository serviceRepository,
                                MeterRegistry meterRegistry) {
        this.simulationService = simulationService;
        this.serviceRepository = serviceRepository;
        this.meterRegistry = meterRegistry;
    }

    // GET /api/simulation -> which services are currently in a failure mode
    @GetMapping
    public Map<String, String> getModes() {
        return simulationService.getAllModes();
    }

    // POST /api/simulation/latency-spike?service=Order%20API  (service defaults to Payment API)
    @PostMapping("/latency-spike")
    public Map<String, String> latencySpike(@RequestParam(defaultValue = "Payment API") String service) {
        return changeMode(service, SimulationService.LATENCY_SPIKE);
    }

    @PostMapping("/error-spike")
    public Map<String, String> errorSpike(@RequestParam(defaultValue = "Payment API") String service) {
        return changeMode(service, SimulationService.ERROR_SPIKE);
    }

    @PostMapping("/outage")
    public Map<String, String> outage(@RequestParam(defaultValue = "Payment API") String service) {
        return changeMode(service, SimulationService.OUTAGE);
    }

    // POST /api/simulation/restore -> every service back to NORMAL
    @PostMapping("/restore")
    public Map<String, String> restore() {
        simulationService.restoreAll();
        countSimulation("restore");
        return Map.of("result", "All services restored to NORMAL");
    }

    private Map<String, String> changeMode(String serviceName, String mode) {
        // Reject unknown names with 404 instead of silently storing a typo
        if (serviceRepository.findByName(serviceName).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service: " + serviceName);
        }
        simulationService.setMode(serviceName, mode);
        countSimulation(mode.toLowerCase());
        return Map.of("service", serviceName, "mode", mode);
    }

    // Counter = a number that only goes up. Appears in Prometheus as cloudpulse_simulations_total{type="..."}
    private void countSimulation(String type) {
        meterRegistry.counter("cloudpulse.simulations", "type", type).increment();
    }
}
