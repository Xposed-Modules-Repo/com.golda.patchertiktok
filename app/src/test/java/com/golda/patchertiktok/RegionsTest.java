package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RegionsTest {
    @Test
    public void tableIsWellFormed() {
        assertTrue(Regions.all().size() > 150);
        for (Regions.Region region : Regions.all()) {
            assertTrue(region.iso, region.iso.matches("[a-z]{2}"));
            assertTrue(region.iso, region.operator.matches("[0-9]{5,6}"));
        }
    }

    @Test
    public void germanyUsesTelekom() {
        Regions.Region germany = Regions.find("de");
        assertNotNull(germany);
        assertEquals("262", germany.mcc());
        assertEquals("01", germany.mnc());
    }

    @Test
    public void unknownRegionIsDisabled() {
        assertEquals("", Regions.normalize("zz"));
        assertEquals("us", Regions.normalize(" US "));
    }
}
