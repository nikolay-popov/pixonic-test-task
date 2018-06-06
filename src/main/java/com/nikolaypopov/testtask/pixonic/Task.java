package com.nikolaypopov.testtask.pixonic;

import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

@Value(staticConstructor = "of")
public class Task<T> {

    @NonNull
    LocalDateTime localDateTime;
    @NonNull
    Callable<T> callable;

}
