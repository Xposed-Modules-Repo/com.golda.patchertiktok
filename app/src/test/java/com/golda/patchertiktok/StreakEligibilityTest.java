package com.golda.patchertiktok;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreakEligibilityTest {
    @Test
    public void includesRiskStartButExcludesExpiration() {
        long start = 1_800_000_000L;
        long before = start + 86_400L;
        long end = before + 86_400L;
        assertFalse(StreakEligibility.isAtRisk(true, true, start, before, end, before * 1000L - 1));
        assertTrue(StreakEligibility.isAtRisk(true, true, start, before, end, before * 1000L));
        assertFalse(StreakEligibility.isAtRisk(true, true, start, before, end, end * 1000L));
    }

    @Test
    public void rejectsMissingOrEmptyRiskWindow() {
        long now = 1_800_000_000L;
        assertFalse(StreakEligibility.isAtRisk(true, true, 0, now - 10, now + 10, now * 1000));
        assertFalse(StreakEligibility.isAtRisk(true, true, now - 20, 0, now + 10, now * 1000));
        assertFalse(StreakEligibility.isAtRisk(true, true, now - 20, now, now, now * 1000));
        assertFalse(StreakEligibility.isAtRisk(true, true, now - 20, now, now - 1, now * 1000));
    }

    @Test
    public void sendsOnlyInsideAtRiskWindowInSeconds() {
        long nowMillis = System.currentTimeMillis();
        long now = nowMillis / 1000L;

        assertTrue(StreakEligibility.isAtRisk(
                true, true, now - 3600L, now - 30L, now + 3600L, nowMillis));
        assertFalse(StreakEligibility.isAtRisk(
                true, true, now - 3600L, now + 60L, now + 3600L, nowMillis));
        assertFalse(StreakEligibility.isAtRisk(
                true, true, now - 3600L, now - 60L, now - 1L, nowMillis));
    }

    @Test
    public void supportsMillisecondTimestamps() {
        long now = System.currentTimeMillis();
        assertTrue(StreakEligibility.isAtRisk(
                true, true, now - 3_600_000L, now - 30_000L, now + 3_600_000L, now));
    }

    @Test
    public void rejectsMissingStreakOrPeer() {
        long nowMillis = System.currentTimeMillis();
        long now = nowMillis / 1000L;

        assertFalse(StreakEligibility.isAtRisk(
                false, true, now - 3600L, now - 30L, now + 3600L, nowMillis));
        assertFalse(StreakEligibility.isAtRisk(
                true, false, now - 3600L, now - 30L, now + 3600L, nowMillis));
    }
}
