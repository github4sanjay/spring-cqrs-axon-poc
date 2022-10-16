package com.example.api.messaging;

public enum Priority {
  BULK(5d),

  HIGH(0d);

  public final double score;

  Priority(double score) {
    this.score = score;
  }
}
