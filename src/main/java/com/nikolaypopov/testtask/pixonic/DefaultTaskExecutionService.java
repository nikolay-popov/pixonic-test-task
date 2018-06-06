package com.nikolaypopov.testtask.pixonic;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
public class DefaultTaskExecutionService<T> implements TaskExecutionService<T> {

    // Priority queue with sorting by Task's localDateTime
    private final BlockingQueue<ScheduledTask<T>> taskQueue =
            new PriorityBlockingQueue<>(
                    11,
                    (o1, o2) -> o1.getTask().getLocalDateTime().compareTo(o2.getTask().getLocalDateTime()));

    private final Lock waitLock = new ReentrantLock();
    private final Condition waitCondition = waitLock.newCondition();

    public DefaultTaskExecutionService() {
        this(new DefaultLocalDateTimeSupplier()); // for real life usage
    }

    public DefaultTaskExecutionService(Supplier<LocalDateTime> localDateTimeSupplier) {
        // Starting task executor thread.
        // In real application ScheduledThreadPoolExecutor could be started and stopped as needed.
        // For the test task I just run it here.
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
            // Taking next task from the queue, waiting as necessary
            getNextTask().ifPresent(scheduledTask -> {
                Task<T> task = scheduledTask.getTask();

                LocalDateTime localDateTimeNow = localDateTimeSupplier.get();
                LocalDateTime taskLocalDateTime = task.getLocalDateTime();

                // Checking if we need to wait before executing the task
                if (localDateTimeNow.isBefore(taskLocalDateTime)) {
                    taskQueue.add(scheduledTask); // returning task in the queue as another task with earlier execution time can arrive

                    long millisBeforeExecution = Duration.between(localDateTimeNow, taskLocalDateTime).toMillis();

                    // waiting until it's time to execute the task OR another task arrives (submit method will signal on the condition)
                    // OR wait interrupted or spectacular wake-up occurs
                    await(millisBeforeExecution);

                    // in any case starting the loop from the beginning to make sure the earliest task will be executed first
                    return;
                }

                // Executing the task and resolve completable future
                // Could also be done in a thread pool executor
                execute(scheduledTask);
            });
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    private Optional<ScheduledTask<T>> getNextTask() {
        ScheduledTask<T> scheduledTask = null;
        try {
             scheduledTask = taskQueue.poll(1, TimeUnit.SECONDS); // not blocking here forever to give user chance to stop executor
        } catch (InterruptedException e) {
            logger.debug("Waiting on task queue interrupted.", e);
        }

        return Optional.ofNullable(scheduledTask);
    }

    private void await(long millisBeforeExecution) {
        waitLock.lock();
        try {
            waitCondition.await(millisBeforeExecution, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.debug("Waiting on condition interrupted.", e);
        } finally {
            waitLock.unlock();
        }
    }

    private void execute(ScheduledTask<T> scheduledTask) {
        CompletableFuture<T> completableFuture = scheduledTask.getCompletableFuture();
        try {
            T result = scheduledTask.getTask().getCallable().call();

            completableFuture.complete(result);
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
    }

    @Override
    public CompletableFuture<T> submit(Task<T> task) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();

        taskQueue.add(ScheduledTask.of(task, completableFuture));

        // waking up possibly waiting task execution thread
        signal();

        return completableFuture;
    }

    private void signal() {
        waitLock.lock();
        try {
            waitCondition.signal();
        } finally {
            waitLock.unlock();
        }
    }

    @Value(staticConstructor = "of")
    private static class ScheduledTask<T> {

        @NonNull
        Task<T> task;
        @NonNull
        CompletableFuture<T> completableFuture;

    }

}
