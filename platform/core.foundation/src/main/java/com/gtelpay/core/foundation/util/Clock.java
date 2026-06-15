package com.gtelpay.core.foundation.util;

import java.time.Instant;

@FunctionalInterface
public interface Clock {

    Instant now();

    Clock systemUtc = Instant::now;
}
