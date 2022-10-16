package com.example.otp;

import java.time.*;
import java.time.temporal.TemporalUnit;

public class TestClock extends Clock {

  private final Instant base = Instant.now();
  private Duration offset = Duration.ZERO;

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public Instant instant() {
    return base.plus(offset);
  }

  public void forward(long amount, TemporalUnit unit) {
    offset = offset.plus(amount, unit);
  }
}
