package com.golda.patchertiktok;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecommendationUrlPolicyTest {
    private static final String FEED = "https://api.tiktokv.com/aweme/v2/feed/";
    private static final RecommendationProfile PROFILE = new RecommendationProfile(
            java.util.Locale.forLanguageTag("ru-RU"), "ru", "25001");

    @Test
    public void replacesRepeatedAndEncodedRegionKeys() {
        String result = RecommendationUrlPolicy.rewrite(FEED + "?region=DE&REGION=BY&%72egion=US", PROFILE);
        assertFalse(result.contains("DE"));
        assertFalse(result.contains("BY"));
        assertFalse(result.contains("US"));
        assertTrue(result.contains("?region=RU&"));
        assertTrue(result.contains("mcc_mnc=25001"));
        assertTrue(result.endsWith("locale=ru-RU"));
    }

    @Test
    public void preservesTimeAndUnrelatedEncodedParameters() {
        String original = FEED + "?cursor=a%2Bb%2fc&timezone_name=Europe%2FBerlin"
                + "&timezone_offset=7200&ts=1800000000&empty=&flag#part%2Fone";
        String result = RecommendationUrlPolicy.rewrite(original, PROFILE);
        assertTrue(result.startsWith(FEED + "?cursor=a%2Bb%2fc&timezone_name=Europe%2FBerlin"
                + "&timezone_offset=7200&ts=1800000000&empty=&flag&region=RU"));
        assertTrue(result.endsWith("#part%2Fone"));
    }

    @Test
    public void onlyRewritesExactRecommendationEndpoints() {
        String other = "https://api.tiktokv.com/search/?next=/aweme/v2/feed/";
        assertEquals(other, RecommendationUrlPolicy.rewrite(other, PROFILE));
        assertFalse(RecommendationUrlPolicy.isFeedUrl(FEED + "extra/"));
        assertFalse(RecommendationUrlPolicy.isFeedUrl("file:///aweme/v2/feed/"));
        assertTrue(RecommendationUrlPolicy.isFeedUrl(FEED.replace("v2", "v1")));
    }

    @Test
    public void repeatedApplicationIsIdempotent() {
        String result = RecommendationUrlPolicy.rewrite(FEED, PROFILE);
        assertEquals(result, RecommendationUrlPolicy.rewrite(result, PROFILE));
    }

    @Test
    public void leavesMalformedUrlsUnchanged() {
        assertEquals(null, RecommendationUrlPolicy.rewrite(null, PROFILE));
        String malformed = FEED + "?value=%zz";
        assertEquals(malformed, RecommendationUrlPolicy.rewrite(malformed, PROFILE));
    }

    @Test
    public void supportsInternationalDeviceWithDifferentSimCountry() {
        RecommendationProfile profile = new RecommendationProfile(
                java.util.Locale.forLanguageTag("pt-BR"), "pt", "26806");
        String result = RecommendationUrlPolicy.rewrite(FEED + "?language=ru&region=RU&mcc_mnc=25001", profile);
        assertTrue(result.contains("region=BR"));
        assertTrue(result.contains("carrier_region=PT"));
        assertTrue(result.contains("mcc_mnc=26806"));
        assertTrue(result.contains("language=pt"));
        assertFalse(result.contains("ru"));
        assertFalse(result.contains("25001"));
    }

    @Test
    public void missingSimDoesNotInventOperatorOrKeepSpoofedOperator() {
        RecommendationProfile profile = new RecommendationProfile(java.util.Locale.GERMANY, "", "");
        String result = RecommendationUrlPolicy.rewrite(FEED + "?mcc_mnc=26201&carrier_region_v2=262", profile);
        assertTrue(result.contains("locale=de-DE"));
        assertFalse(result.contains("mcc_mnc"));
        assertFalse(result.contains("carrier_region_v2"));
    }

    @Test
    public void noCountryAndInvalidSimRemainUnspecified() {
        RecommendationProfile profile = new RecommendationProfile(java.util.Locale.ENGLISH, "", "00000");
        String result = RecommendationUrlPolicy.rewrite(FEED + "?region=DE", profile);
        assertFalse(result.contains("region="));
        assertTrue(result.contains("locale=en"));
    }

    @Test
    public void acceptsFeedEndpointWithoutTrailingSlash() {
        assertTrue(RecommendationUrlPolicy.isFeedUrl("https://api.tiktokv.com/aweme/v2/feed?count=6"));
    }
}
