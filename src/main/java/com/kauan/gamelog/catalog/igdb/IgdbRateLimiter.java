package com.kauan.gamelog.catalog.igdb;

import java.util.concurrent.TimeUnit;

/** Espaça as requisições para não passar do limite do IGDB (4 por segundo por credencial). */
final class IgdbRateLimiter {
    private final long intervalNanos;
    private long nextSlot = System.nanoTime();

    IgdbRateLimiter(int requestsPerSecond) {
        this.intervalNanos = TimeUnit.SECONDS.toNanos(1) / requestsPerSecond;
    }

    void acquire() {
        long wait;
        synchronized (this) {
            long now = System.nanoTime();
            long slot = Math.max(now, nextSlot);
            nextSlot = slot + intervalNanos;
            wait = slot - now;
        }
        if (wait > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrompido esperando o limite do IGDB", e);
            }
        }
    }
}
