package com.nikolaypopov.testtask.pixonic;

import java.util.concurrent.CompletableFuture;

public interface TaskExecutionService<T> {

    CompletableFuture<T> submit(Task<T> task);

}
