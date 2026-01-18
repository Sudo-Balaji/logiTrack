package com.valkyrie.logitrack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class SimulationService {

    private final Random random = new Random();

    // Static memory leak storage
    public static final List<byte[]> memoryLeakStorage = new ArrayList<>();

    // Configurable CPU burn intensity (10-70%)
    @Value("${chaos.cpu.intensity:30}")
    private int cpuIntensity;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private final String[] normalMessages = {
            "Order #%d processed successfully",
            "User authentication completed for session %s",
            "Inventory check completed - %d items in stock",
            "Payment transaction #%d verified",
            "Shipping label generated for order #%d",
            "Customer notification sent successfully",
            "Database backup completed",
            "Cache refresh completed in %dms",
            "API request processed successfully",
            "Session %s created for user"
    };

    @Scheduled(fixedRate = 3000)
    public void runChaosSimulation() {
        int randomValue = random.nextInt(100);

        if (randomValue < 60) {
            // Pattern A: Normal Traffic (60%)
            executeNormalTraffic();
        } else if (randomValue < 70) {
            // Pattern B: High Latency (10%)
            executeHighLatency();
        } else if (randomValue < 80) {
            // Pattern C: CPU Burn (10%)
            executeCpuBurn();
        } else if (randomValue < 90) {
            // Pattern D: Memory Leak (10%)
            executeMemoryLeak();
        } else {
            // Pattern E: Exceptions (10%)
            executeException();
        }
    }

    private void executeNormalTraffic() {
        String message = String.format(
                normalMessages[random.nextInt(normalMessages.length)],
                random.nextInt(10000),
                generateSessionId());
        log.info("NORMAL_TRAFFIC: {}", message);
    }

    private void executeHighLatency() {
        long startTime = System.currentTimeMillis();
        int sleepTime = 500 + random.nextInt(1500); // 500-2000ms

        try {
            Thread.sleep(sleepTime);
            long duration = System.currentTimeMillis() - startTime;
            log.warn("HIGH_LATENCY: Slow API response detected - Duration: {}ms, Threshold exceeded", duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HIGH_LATENCY: Thread interrupted during latency simulation", e);
        }
    }

    private void executeCpuBurn() {
        long startTime = System.currentTimeMillis();

        // Calculate number of parallel jobs based on CPU intensity (10-70%)
        int parallelJobs = Math.max(1, (cpuIntensity / 10));

        log.info("CPU_BURN: Starting CPU intensive operations with {}% intensity ({} parallel jobs)",
                cpuIntensity, parallelJobs);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < parallelJobs; i++) {
            final int jobId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                switch (jobId % 3) {
                    case 0 -> {
                        // Fibonacci calculation
                        int fibResult = fibonacci(35);
                        log.debug("CPU_BURN_JOB_{}: Fibonacci(35) = {}", jobId, fibResult);
                    }
                    case 1 -> {
                        // Prime number generation
                        int primeCount = generatePrimes(100000);
                        log.debug("CPU_BURN_JOB_{}: Generated {} primes", jobId, primeCount);
                    }
                    case 2 -> {
                        // Matrix multiplication
                        double result = matrixMultiplication(100);
                        log.debug("CPU_BURN_JOB_{}: Matrix result = {}", jobId, result);
                    }
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all jobs to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - startTime;
        log.warn("CPU_BURN: Completed {} parallel CPU intensive jobs in {}ms with {}% intensity",
                parallelJobs, duration, cpuIntensity);
    }

    private int fibonacci(int n) {
        if (n <= 1)
            return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    private int generatePrimes(int limit) {
        int count = 0;
        for (int num = 2; num <= limit; num++) {
            boolean isPrime = true;
            for (int i = 2; i <= Math.sqrt(num); i++) {
                if (num % i == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime)
                count++;
        }
        return count;
    }

    private double matrixMultiplication(int size) {
        double[][] matrixA = new double[size][size];
        double[][] matrixB = new double[size][size];
        double[][] result = new double[size][size];

        // Initialize matrices
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrixA[i][j] = random.nextDouble();
                matrixB[i][j] = random.nextDouble();
            }
        }

        // Multiply matrices
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }

        return result[0][0];
    }

    private void executeMemoryLeak() {
        // Allocate 5MB byte array
        byte[] leak = new byte[5 * 1024 * 1024];
        random.nextBytes(leak); // Fill with random data to prevent optimization

        memoryLeakStorage.add(leak);

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        double memoryUsagePercent = (usedMemory * 100.0) / maxMemory;

        log.error("MEMORY_LEAK: Allocated 5MB - Total leaked: {}MB, Memory usage: {}MB/{}MB ({}%)",
                memoryLeakStorage.size() * 5, usedMemory, maxMemory,
                String.format("%.2f", memoryUsagePercent));

        if (memoryUsagePercent > 80) {
            log.error("MEMORY_LEAK: CRITICAL - OutOfMemory warning! Memory usage above 80%");
        }
    }

    private void executeException() {
        try {
            // Simulate database connection failure
            throw new RuntimeException(
                    "Database Connection Failed: Connection timeout after 30s to db-primary.prod.internal:5432");
        } catch (RuntimeException e) {
            log.error("EXCEPTION: Critical error occurred in transaction processing", e);
        }
    }

    private String generateSessionId() {
        return String.format("sess_%d_%s",
                System.currentTimeMillis(),
                Integer.toHexString(random.nextInt()));
    }

    public int getMemoryLeakSize() {
        return memoryLeakStorage.size();
    }

    public void clearMemoryLeak() {
        int size = memoryLeakStorage.size();
        memoryLeakStorage.clear();
        System.gc(); // Suggest garbage collection
        log.info("MEMORY_RESET: Cleared {} memory leak entries ({}MB)", size, size * 5);
    }
}
