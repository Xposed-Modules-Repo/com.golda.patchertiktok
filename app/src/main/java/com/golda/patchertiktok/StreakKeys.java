package com.golda.patchertiktok;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class StreakKeys {
    private StreakKeys() {
    }

    static String hash(String prefix, String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            StringBuilder result = new StringBuilder(prefix);
            char[] hex = "0123456789abcdef".toCharArray();
            for (byte value : digest.digest()) {
                result.append(hex[(value & 255) >>> 4]).append(hex[value & 15]);
            }
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot form delivery identity", error);
        }
    }

    static String window(long activeBefore, long endAt) {
        long start = seconds(activeBefore);
        long end = seconds(endAt);
        return start > 0 && end > start ? start + ":" + end : null;
    }

    static long seconds(long timestamp) {
        return timestamp > 10_000_000_000L ? timestamp / 1000L : timestamp;
    }
}
