package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CleanLinksTest {
    @Test
    public void removesShareTracking() {
        assertEquals("https://www.tiktok.com/@user/video/123",
                CleanLinks.cleanUrl("https://www.tiktok.com/@user/video/123?_r=1&_t=ZM-8abc&u_code=xyz&share_link_id=1"));
    }

    @Test
    public void keepsUnrelatedParametersAndFragment() {
        assertEquals("https://www.tiktok.com/@user/video/123?lang=de#top",
                CleanLinks.cleanUrl("https://www.tiktok.com/@user/video/123?lang=de&utm_source=copy#top"));
    }

    @Test
    public void leavesOtherSitesAndTextAlone() {
        String other = "https://example.com/?utm_source=x";
        assertEquals(other, CleanLinks.cleanUrl(other));
        assertEquals("Look https://vm.tiktok.com/ZM123/ wow",
                CleanLinks.cleanText("Look https://vm.tiktok.com/ZM123/?_r=1 wow"));
    }
}
