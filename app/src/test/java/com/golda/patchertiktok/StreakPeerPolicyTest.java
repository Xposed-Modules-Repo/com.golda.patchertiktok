package com.golda.patchertiktok;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class StreakPeerPolicyTest {
    @Test public void allowsOnlyOwnDirectConversation() {
        assertEquals("202", StreakPeerPolicy.directPeer(1, "101", Arrays.asList("202", "101")));
        assertEquals("202", StreakPeerPolicy.directPeer(1, "101", Arrays.asList("101", "202", "101")));
        assertNull(StreakPeerPolicy.directPeer(2, "101", Arrays.asList("101", "202")));
        assertNull(StreakPeerPolicy.directPeer(1, "999", Arrays.asList("101", "202")));
        assertNull(StreakPeerPolicy.directPeer(1, "101", Arrays.asList("101", "202", "303")));
        assertNull(StreakPeerPolicy.directPeer(1, "101", Arrays.asList("101")));
    }

    @Test public void rejectsInvalidIdentities() {
        for (String id : Arrays.asList(null, "", "0", "-1", "01", "abc", "9999999999999999999")) {
            assertNull(StreakPeerPolicy.directPeer(1, "101", Arrays.asList("101", id)));
            assertFalse(StreakPeerPolicy.validId(id));
        }
    }
}
