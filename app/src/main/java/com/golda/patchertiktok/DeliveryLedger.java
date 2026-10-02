package com.golda.patchertiktok;

import java.util.UUID;

final class DeliveryLedger {
    enum State { RESERVED, UNKNOWN, CONFIRMED, RETRY }

    interface Store {
        Record read(String key);
        boolean write(String key, Record record);
    }

    static final class Record {
        final String day;
        final String window;
        final String token;
        final State state;
        final int attempts;
        final long retryAt;

        Record(String day, String window, String token, State state, int attempts, long retryAt) {
            this.day = day;
            this.window = window;
            this.token = token;
            this.state = state;
            this.attempts = attempts;
            this.retryAt = retryAt;
        }
    }

    private final Store store;

    DeliveryLedger(Store store) {
        this.store = store;
    }

    synchronized Record reserve(String key, String day, String window, long now) {
        if (key == null || day == null || !day.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                || window == null || now < 0) return null;
        Record previous = store.read(key);
        int attempts = 1;
        if (previous != null) {
            boolean sameWindow = previous.window.equals(window);
            boolean retry = sameWindow && previous.state == State.RETRY
                    && previous.attempts < 3 && now >= previous.retryAt;
            if (!retry && (sameWindow || previous.day.compareTo(day) >= 0)) return null;
            if (retry) attempts = previous.attempts + 1;
        }
        Record next = new Record(day, window, UUID.randomUUID().toString(), State.RESERVED, attempts, 0L);
        // The durable reservation must precede SDK dispatch, including after a process restart.
        return store.write(key, next) ? next : null;
    }

    synchronized void finish(String key, Record attempt, State state, long now) {
        Record current = store.read(key);
        if (current == null || !current.token.equals(attempt.token) || current.state == State.CONFIRMED) return;
        if (state == State.RETRY && current.state == State.UNKNOWN) return;
        long retryAt = state == State.RETRY ? now + 300_000L : 0L;
        if (!store.write(key, new Record(current.day, current.window, current.token,
                state, current.attempts, retryAt))) {
            throw new IllegalStateException("Delivery state was not persisted");
        }
    }
}
