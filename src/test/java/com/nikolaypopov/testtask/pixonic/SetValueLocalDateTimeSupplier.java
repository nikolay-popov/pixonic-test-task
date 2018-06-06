package com.nikolaypopov.testtask.pixonic;

import lombok.Setter;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Setter
public class SetValueLocalDateTimeSupplier implements Supplier<LocalDateTime> {

    private LocalDateTime localDateTime;

    @Override
    public LocalDateTime get() {
        return localDateTime;
    }

}
