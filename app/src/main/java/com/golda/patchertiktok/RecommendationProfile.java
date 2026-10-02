package com.golda.patchertiktok;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class RecommendationProfile {
    final String localeTag;
    final String language;
    final String region;
    final String carrierRegion;
    final String operator;

    RecommendationProfile(Locale locale, String simCountry, String simOperator) {
        Locale resolved = locale == null || locale.getLanguage().isEmpty()
                ? Locale.ENGLISH : locale;
        localeTag = resolved.toLanguageTag();
        language = resolved.getLanguage();
        String country = countryCode(resolved.getCountry());
        String carrier = countryCode(simCountry);
        region = country.isEmpty() ? carrier : country;
        carrierRegion = carrier.isEmpty() ? region : carrier;
        operator = simOperator != null && simOperator.matches("[0-9]{5,6}")
                && !simOperator.startsWith("000") ? simOperator : "";
    }

    Map<String, String> parameters() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : new String[]{"region", "sys_region", "current_region",
                "residence", "op_region", "store_region"}) values.put(key, region);
        values.put("carrier_region", carrierRegion);
        values.put("mcc_mnc", operator);
        values.put("carrier_region_v2", operator.isEmpty() ? "" : operator.substring(0, 3));
        values.put("language", language);
        values.put("app_language", language);
        values.put("locale", localeTag);
        return values;
    }

    private static String countryCode(String value) {
        return value != null && value.matches("[a-zA-Z]{2}")
                ? value.toUpperCase(Locale.ROOT) : "";
    }
}
