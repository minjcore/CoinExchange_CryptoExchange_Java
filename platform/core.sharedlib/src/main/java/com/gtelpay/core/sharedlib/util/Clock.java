package com.gtelpay.core.sharedlib.util;

import java.time.Instant;

@FunctionalInterface
public interface Clock {

    Instant now();

    Clock systemUtc = Instant::now;
}
