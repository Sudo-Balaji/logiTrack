package com.valkyrie.logitrack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chaos")
@RequiredArgsConstructor
@Slf4j
public class ChaosController {

    private final SimulationService simulationService;

    @GetMapping("/status")
    public String getStatus() {
        log.info("CHAOS_CONTROL: Status check requested");
        return "Chaos Engine Running";
    }

    @PostMapping("/reset-memory")
    public Map<String, Object> resetMemory() {
        int previousSize = simulationService.getMemoryLeakSize();
        simulationService.clearMemoryLeak();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Memory leak storage cleared");
        response.put("clearedEntries", previousSize);
        response.put("freedMemoryMB", previousSize * 5);

        log.info("CHAOS_CONTROL: Memory reset completed - Cleared {} entries ({}MB)",
                previousSize, previousSize * 5);

        return response;
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("memoryLeakEntries", simulationService.getMemoryLeakSize());
        metrics.put("leakedMemoryMB", simulationService.getMemoryLeakSize() * 5);
        metrics.put("usedMemoryMB", usedMemory);
        metrics.put("freeMemoryMB", freeMemory);
        metrics.put("maxMemoryMB", maxMemory);
        metrics.put("memoryUsagePercent", String.format("%.2f", (usedMemory * 100.0) / maxMemory));

        log.info("CHAOS_CONTROL: Metrics requested - Memory: {}MB/{}MB", usedMemory, maxMemory);

        return metrics;
    }
}
