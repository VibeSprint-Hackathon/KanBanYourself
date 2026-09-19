package com.vibesprint.backend.raid;

import com.vibesprint.backend.integration.DemoResetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RaidActivationConcurrencyTests {

    @Autowired
    private RaidService raidService;

    @Autowired
    private RaidRepository raidRepository;

    @Autowired
    private DemoResetService resetService;

    @BeforeEach
    void prepare() {
        resetService.reset();
    }

    @AfterEach
    void restore() {
        resetService.reset();
    }

    @Test
    void allowsOnlyOneOfTwoConcurrentActivations() throws Exception {
        raidService.cancel(201L);
        long firstId = raidService.create(request("First Draft")).id();
        long secondId = raidService.create(request("Second Draft")).id();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(() -> activateAfterSignal(firstId, ready, start));
            Future<Boolean> second = executor.submit(() -> activateAfterSignal(secondId, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<Boolean> results = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            );
            assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
            assertEquals(1, raidRepository.findAll().stream()
                    .filter(raid -> raid.getStatus() == RaidStatus.ACTIVE)
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean activateAfterSignal(long raidId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent activation did not start in time");
        }
        try {
            raidService.activate(raidId);
            return true;
        } catch (RaidConflictException exception) {
            return false;
        }
    }

    private CreateRaidRequest request(String name) {
        return new CreateRaidRequest(name, "Concurrent activation candidate.", 500, null);
    }
}
