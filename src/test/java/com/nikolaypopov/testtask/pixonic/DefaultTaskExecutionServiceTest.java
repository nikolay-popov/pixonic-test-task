package com.nikolaypopov.testtask.pixonic;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTaskExecutionServiceTest {

    final SetValueLocalDateTimeSupplier setValueLocalDateTimeSupplier = new SetValueLocalDateTimeSupplier();

    TaskExecutionService taskExecutionService = new DefaultTaskExecutionService(setValueLocalDateTimeSupplier);

    private LocalDateTime localDateTime;
    private String expectedTaskResult;
    private String expectedTaskResult2;
    private String unexpectedTaskResult;

    @BeforeEach
    void setUp() {
        localDateTime = LocalDateTime.now();
        setValueLocalDateTimeSupplier.setLocalDateTime(localDateTime);
        expectedTaskResult = RandomStringUtils.randomAlphanumeric(9);
        expectedTaskResult2 = RandomStringUtils.randomAlphanumeric(8);
        unexpectedTaskResult = RandomStringUtils.randomAlphanumeric(7);
    }

    @Test
    void executesTaskWhenTimeComes() throws Exception {
        CompletableFuture completableFuture =
                taskExecutionService.submit(Task.of(localDateTime.plus(20, ChronoUnit.MILLIS), () -> expectedTaskResult));

        waitABit();

        assertThat(completableFuture).isNotDone();

        setValueLocalDateTimeSupplier.setLocalDateTime(localDateTime.plus(10, ChronoUnit.MILLIS));

        waitABit();

        assertThat(completableFuture).isNotDone();

        setValueLocalDateTimeSupplier.setLocalDateTime(localDateTime.plus(30, ChronoUnit.MILLIS));

        waitABit();

        assertThat(completableFuture).isCompleted();
        assertThat(completableFuture.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
    }

    private void waitABit() throws InterruptedException {
        Thread.sleep(50);
    }

    @Test
    void executesTaskIfTimeIsCurrentTime() throws Exception {
        CompletableFuture completableFuture =
                taskExecutionService.submit(Task.of(localDateTime, () -> expectedTaskResult));

        waitABit();

        assertThat(completableFuture).isCompleted();
        assertThat(completableFuture.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
    }

    @Test
    void propogatesExceptions() throws Exception {
        CompletableFuture completableFuture =
                taskExecutionService.submit(Task.of(localDateTime, () -> { throw new RuntimeException(expectedTaskResult); }));

        waitABit();

        assertThat(completableFuture).isCompletedExceptionally();
        assertThat(
                completableFuture
                        .exceptionally(e -> ((Exception) e).getMessage())
                        .getNow(unexpectedTaskResult))
                .isEqualTo(expectedTaskResult);
    }

    @Test
    void addEarlierTaskLater() throws Exception {
        CompletableFuture completableFuture1 =
                taskExecutionService.submit(Task.of(localDateTime.plus(50, ChronoUnit.MILLIS), () -> expectedTaskResult2));

        waitABit();

        CompletableFuture completableFuture2 =
                taskExecutionService.submit(Task.of(localDateTime.plus(25, ChronoUnit.MILLIS), () -> expectedTaskResult));

        waitABit();

        assertThat(completableFuture1).isNotDone();
        assertThat(completableFuture2).isNotDone();

        setValueLocalDateTimeSupplier.setLocalDateTime(localDateTime.plus(25, ChronoUnit.MILLIS));

        waitABit();

        assertThat(completableFuture2).isCompleted();
        assertThat(completableFuture2.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
        assertThat(completableFuture1).isNotDone();

        setValueLocalDateTimeSupplier.setLocalDateTime(localDateTime.plus(51, ChronoUnit.MILLIS));

        waitABit();

        assertThat(completableFuture1).isCompleted();
        assertThat(completableFuture1.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult2);
    }

    @Test
    void executesTaskWhenTimeInThePast() throws Exception {
        CompletableFuture completableFuture =
                taskExecutionService.submit(Task.of(localDateTime.minus(20, ChronoUnit.MILLIS), () -> expectedTaskResult));

        waitABit();

        assertThat(completableFuture).isCompleted();
        assertThat(completableFuture.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
    }

    @Test
    void tasksOrderWithSameTime() throws Exception {
        CompletableFuture completableFuture1 =
                taskExecutionService.submit(Task.of(localDateTime, () -> {
                    Thread.sleep(500);

                    return expectedTaskResult;
                }));

        waitABit();

        CompletableFuture completableFuture2 =
                taskExecutionService.submit(Task.of(localDateTime, () -> {
                    Thread.sleep(500);

                    return expectedTaskResult2;
                }));

        waitABit();

        assertThat(completableFuture1).isNotDone();
        assertThat(completableFuture2).isNotDone();

        Thread.sleep(500);

        assertThat(completableFuture1).isCompleted();
        assertThat(completableFuture1.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
        assertThat(completableFuture2).isNotDone();

        Thread.sleep(500);

        assertThat(completableFuture2).isCompleted();
        assertThat(completableFuture2.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult2);
    }

    @Test
    void tasksOrderWithTimeInThePast() throws Exception {
        CompletableFuture completableFuture1 =
                taskExecutionService.submit(Task.of(localDateTime, () -> {
                    Thread.sleep(500);

                    return expectedTaskResult;
                }));

        waitABit();

        CompletableFuture completableFuture2 =
                taskExecutionService.submit(Task.of(localDateTime.minus(1, ChronoUnit.DAYS), () -> {
                    Thread.sleep(500);

                    return expectedTaskResult2;
                }));

        waitABit();

        assertThat(completableFuture1).isNotDone();
        assertThat(completableFuture2).isNotDone();

        Thread.sleep(500);

        assertThat(completableFuture1).isCompleted();
        assertThat(completableFuture1.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
        assertThat(completableFuture2).isNotDone();

        Thread.sleep(500);

        assertThat(completableFuture2).isCompleted();
        assertThat(completableFuture2.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult2);
    }

    @Test
    void submitFromDifferentThreads() throws Exception {
        List<CompletableFuture<String>> resultCompletableFutures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                CompletableFuture completableFuture = taskExecutionService.submit(Task.of(localDateTime, () -> expectedTaskResult));
                resultCompletableFutures.add(completableFuture);
            }).run();
        }

        Thread.sleep(1000);

        resultCompletableFutures.stream()
                .forEach(completableFuture -> {
                    assertThat(completableFuture).isCompleted();
                    assertThat(completableFuture.getNow(unexpectedTaskResult)).isEqualTo(expectedTaskResult);
                });
    }

}
