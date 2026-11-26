package freshvend.iot;

import java.util.concurrent.ThreadLocalRandom;

/** Lùi thời gian thử lại theo cấp số nhân có nhiễu ngẫu nhiên, tránh dồn dập khi backend vừa hồi phục. */
final class Backoff {
  private final long base, max;
  private int failures = 0;
  private long nextAt = 0;

  Backoff(long base, long max) { this.base = base; this.max = max; }

  synchronized boolean ready() { return System.currentTimeMillis() >= nextAt; }

  synchronized void success() { failures = 0; nextAt = 0; }

  synchronized long failure() {
    failures++;
    long d = Math.min(max, base * (1L << Math.min(failures - 1, 12)));
    d = d / 2 + ThreadLocalRandom.current().nextLong(d / 2 + 1);
    nextAt = System.currentTimeMillis() + d;
    return d;
  }

  synchronized int failures() { return failures; }
}
