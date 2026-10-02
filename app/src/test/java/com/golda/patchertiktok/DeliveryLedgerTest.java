package com.golda.patchertiktok;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class DeliveryLedgerTest {
    private static class MemoryStore implements DeliveryLedger.Store {
        final Map<String, DeliveryLedger.Record> values = new HashMap<>();
        boolean writable = true;
        public DeliveryLedger.Record read(String key) { return values.get(key); }
        public boolean write(String key, DeliveryLedger.Record value) {
            if (!writable) return false;
            values.put(key, value);
            return true;
        }
    }

    @Test public void reservationSurvivesRestartWithoutAck() {
        MemoryStore store = new MemoryStore();
        DeliveryLedger ledger = new DeliveryLedger(store);
        assertNotNull(ledger.reserve("a", "2026-09-19", "1:2", 100));
        assertNull(new DeliveryLedger(store).reserve("a", "2026-09-19", "1:2", 200));
        assertNull(new DeliveryLedger(store).reserve("a", "2026-09-20", "1:2", 300));
    }

    @Test public void blocksSameDayAndSameWindowButAllowsNextWindowTomorrow() {
        DeliveryLedger ledger = new DeliveryLedger(new MemoryStore());
        assertNotNull(ledger.reserve("a", "2026-09-19", "1:2", 100));
        assertNull(ledger.reserve("a", "2026-09-19", "2:3", 200));
        assertNull(ledger.reserve("a", "2026-09-18", "2:3", 200));
        assertNotNull(ledger.reserve("a", "2026-09-20", "2:3", 300));
        assertNotNull(ledger.reserve("other-account", "2026-09-19", "1:2", 300));
    }

    @Test public void storageFailurePreventsDispatch() {
        MemoryStore store = new MemoryStore();
        store.writable = false;
        assertNull(new DeliveryLedger(store).reserve("a", "2026-09-19", "1:2", 100));
    }

    @Test public void unknownOutcomeCannotRetry() {
        MemoryStore store = new MemoryStore();
        DeliveryLedger ledger = new DeliveryLedger(store);
        DeliveryLedger.Record attempt = ledger.reserve("a", "2026-09-19", "1:2", 100);
        ledger.finish("a", attempt, DeliveryLedger.State.UNKNOWN, 200);
        ledger.finish("a", attempt, DeliveryLedger.State.RETRY, 201);
        assertNull(ledger.reserve("a", "2026-09-19", "1:2", 900_000));
        ledger.finish("a", attempt, DeliveryLedger.State.CONFIRMED, 900_001);
        ledger.finish("a", attempt, DeliveryLedger.State.UNKNOWN, 900_002);
        assertEquals(DeliveryLedger.State.CONFIRMED, store.read("a").state);
    }

    @Test public void retriesOnlyPreDispatchFailureWithBoundedBackoff() {
        MemoryStore store = new MemoryStore();
        DeliveryLedger ledger = new DeliveryLedger(store);
        for (int index = 0; index < 3; index++) {
            long now = index * 300_000L;
            DeliveryLedger.Record attempt = ledger.reserve("a", "2026-09-19", "1:2", now);
            assertNotNull(attempt);
            ledger.finish("a", attempt, DeliveryLedger.State.RETRY, now);
            assertNull(ledger.reserve("a", "2026-09-19", "1:2", now + 1));
        }
        assertNull(ledger.reserve("a", "2026-09-19", "1:2", 1_000_000));
    }

    @Test public void oldCallbackCannotOverwriteNewAttempt() {
        MemoryStore store = new MemoryStore();
        DeliveryLedger ledger = new DeliveryLedger(store);
        DeliveryLedger.Record old = ledger.reserve("a", "2026-09-19", "1:2", 100);
        DeliveryLedger.Record next = ledger.reserve("a", "2026-09-20", "2:3", 200);
        ledger.finish("a", old, DeliveryLedger.State.CONFIRMED, 300);
        assertEquals(next.token, store.read("a").token);
        assertEquals(DeliveryLedger.State.RESERVED, store.read("a").state);
    }

    @Test public void stableKeysSeparateAccountsAndNormalizeWindowUnits() {
        assertEquals(StreakKeys.window(1_800_000_000L, 1_800_001_000L),
                StreakKeys.window(1_800_000_000_000L, 1_800_001_000_000L));
        assertNotEquals(StreakKeys.hash("window_v1_", "1", "23", "4:5"),
                StreakKeys.hash("window_v1_", "12", "3", "4:5"));
        assertNotEquals(StreakKeys.hash("window_v1_", "1", "2", "4:5"),
                StreakKeys.hash("window_v1_", "3", "2", "4:5"));
    }
}
