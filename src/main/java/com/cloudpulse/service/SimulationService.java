package com.cloudpulse.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which "failure mode" each simulated service is in.
 * The MonitoringService reads this every tick to decide what telemetry to generate.
 *
 * The state is kept in memory (not in the database) to keep the project simple.
 * That is why we run only ONE replica of the app: a second replica would have its own map
 * and its own scheduled tick, which would write duplicate metrics.
 */
@Service
public class SimulationService {

    // The four possible modes. Plain String constants, the same style as the entity status fields.
    public static final String NORMAL = "NORMAL";
    public static final String LATENCY_SPIKE = "LATENCY_SPIKE";
    public static final String ERROR_SPIKE = "ERROR_SPIKE";
    public static final String OUTAGE = "OUTAGE";

    // service name -> mode. ConcurrentHashMap because web requests and the scheduler run on different threads.
    private final Map<String, String> modes = new ConcurrentHashMap<>();

    // Any service we have never touched is NORMAL.
    public String getMode(String serviceName) {
        return modes.getOrDefault(serviceName, NORMAL);
    }

    public void setMode(String serviceName, String mode) {
        modes.put(serviceName, mode);
    }

    // Forgetting every entry puts all services back to NORMAL.
    public void restoreAll() {
        modes.clear();
    }

    public Map<String, String> getAllModes() {
        return Map.copyOf(modes);
    }
}
