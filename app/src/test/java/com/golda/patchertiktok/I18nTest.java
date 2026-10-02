package com.golda.patchertiktok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Locale;
import java.util.Map;

import org.junit.Test;

public class I18nTest {
    @Test
    public void everyTableHasEveryString() {
        for (Map.Entry<String, String[]> table : I18n.tables().entrySet()) {
            assertEquals(table.getKey(), I18n.S.values().length, table.getValue().length);
            for (String value : table.getValue()) assertFalse(table.getKey(), value.trim().isEmpty());
        }
    }

    @Test
    public void unknownLanguageFallsBackToEnglish() {
        I18n.use(new Locale("ja"));
        assertEquals("Block ads", I18n.get(I18n.S.ADS));
        I18n.use(new Locale("kk"));
        assertEquals("Блокировать рекламу", I18n.get(I18n.S.ADS));
        I18n.use(new Locale("in"));
        assertEquals("Blokir iklan", I18n.get(I18n.S.ADS));
        I18n.use(Locale.ENGLISH);
    }
}
