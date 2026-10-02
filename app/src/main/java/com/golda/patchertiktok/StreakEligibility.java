package com.golda.patchertiktok;

final class StreakEligibility {
    private static final long MILLIS_THRESHOLD = 10_000_000_000L;

    private StreakEligibility() {
    }

    static boolean isAtRisk(
            boolean hasStreak,
            boolean hasPeer,
            long activeStart,
            long activeBefore,
            long endAt,
            long nowMillis
    ) {
        if (!hasStreak || !hasPeer || activeStart <= 0 || activeBefore <= 0) return false;
        if (activeStart > activeBefore || endAt <= activeBefore) return false;

        long now = usesMilliseconds(activeStart, activeBefore, endAt)
                ? nowMillis
                : nowMillis / 1000L;
        return now >= activeBefore && now < endAt;
    }

    private static boolean usesMilliseconds(long... values) {
        for (long value : values) {
            if (value > MILLIS_THRESHOLD) return true;
        }
        return false;
    }
}
