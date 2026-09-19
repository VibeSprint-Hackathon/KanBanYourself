package com.vibesprint.backend.progression;

import com.vibesprint.backend.integration.DemoResetService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ProgressionConcurrencyTests {

    @Autowired
    private ProgressionService progressionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DemoResetService resetService;

    @BeforeEach
    void restoreSeedBeforeTest() {
        restoreSeed();
    }

    @AfterEach
    void restoreSeedAfterTest() {
        restoreSeed();
    }

    @Test
    void appliesOnlyOneOfTwoConcurrentCompletions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ProgressionResult> first = executor.submit(() -> completeAfterSignal("parallel-1", ready, start));
            Future<ProgressionResult> second = executor.submit(() -> completeAfterSignal("parallel-2", ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<ProgressionResult> results = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS)
            );

            assertEquals(1, results.stream().filter(ProgressionResult::applied).count());
            assertEquals(1, results.stream().filter(result -> !result.applied()).count());
            assertEquals(180, results.stream().mapToInt(ProgressionResult::xpGained).sum());
            assertEquals(1100, jdbcTemplate.queryForObject(
                    "select total_xp from player where id = 1",
                    Integer.class
            ));
            assertEquals("DONE", jdbcTemplate.queryForObject(
                    "select status from quest where id = 101",
                    String.class
            ));
            assertEquals(0, jdbcTemplate.queryForObject(
                    "select current_hp from raid where id = 201",
                    Integer.class
            ));
        } finally {
            executor.shutdownNow();
        }
    }

    private ProgressionResult completeAfterSignal(
            String eventId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent completion did not start in time");
        }
        return progressionService.completeQuest(new CompleteQuestCommand(101L, eventId, ProgressionSource.DEMO));
    }

    private void restoreSeed() {
        resetService.reset();
    }
}
