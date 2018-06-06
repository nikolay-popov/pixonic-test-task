package com.nikolaypopov.testtask.pixonic;

import java.time.LocalDateTime;
import java.util.function.Supplier;

public class DefaultLocalDateTimeSupplier implements Supplier<LocalDateTime> {

    @Override
    public LocalDateTime get() {
        return LocalDateTime.now();
    }

}
