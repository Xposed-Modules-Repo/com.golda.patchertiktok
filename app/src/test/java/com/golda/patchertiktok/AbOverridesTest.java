package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AbOverridesTest {
    @Test
    public void convertsToGetterTypes() {
        assertEquals(Boolean.TRUE, AbOverrides.convert(1, boolean.class, null));
        assertEquals(Boolean.FALSE, AbOverrides.convert(false, Boolean.class, null));
        assertEquals(3, AbOverrides.convert(3, int.class, null));
        assertEquals(1, AbOverrides.convert(true, Integer.class, null));
        assertEquals(120L, AbOverrides.convert(120, long.class, null));
        assertEquals("v3", AbOverrides.convert("v3", String.class, null));
    }

    @Test
    public void objectGettersFollowTheCurrentValueType() {
        assertEquals(2, AbOverrides.convert(2, Object.class, 0));
        assertNull(AbOverrides.convert(2, Object.class, null));
    }
}
