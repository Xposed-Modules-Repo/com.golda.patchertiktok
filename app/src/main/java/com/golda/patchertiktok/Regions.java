package com.golda.patchertiktok;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Country → representative carrier, used for the optional region spoof. */
final class Regions {
    static final class Region {
        final String iso;
        final String operator;
        final String carrier;

        Region(String iso, String operator, String carrier) {
            this.iso = iso;
            this.operator = operator;
            this.carrier = carrier;
        }

        String mcc() { return operator.substring(0, 3); }

        String mnc() { return operator.substring(3); }

        String name(Locale locale) { return new Locale("", iso).getDisplayCountry(locale); }
    }

    // iso:mccmnc:carrier
    private static final String TABLE =
            "ad:21303:Andorra Telecom;" +
            "ae:42402:Etisalat;" +
            "ag:344920:Flow;" +
            "al:27601:ONE;" +
            "am:28305:Team Telecom;" +
            "ao:63102:Unitel;" +
            "ar:722310:Claro;" +
            "at:23201:A1 Telekom;" +
            "au:50501:Telstra;" +
            "az:40001:Azercell;" +
            "ba:21805:m:tel;" +
            "bb:342600:Flow;" +
            "bd:47001:Grameenphone;" +
            "be:20601:Proximus;" +
            "bf:61301:Onatel;" +
            "bg:28401:A1 Bulgaria;" +
            "bi:64201:Econet;" +
            "bj:61603:MTN;" +
            "bm:35000:One Communications;" +
            "bn:52811:DST;" +
            "bo:73602:Entel;" +
            "br:72406:Vivo;" +
            "bs:364030:BTC;" +
            "bt:40211:B-Mobile;" +
            "bw:65201:Mascom;" +
            "by:25701:A1;" +
            "bz:70267:DigiCell;" +
            "ca:302720:Rogers;" +
            "cd:63001:Vodacom;" +
            "cf:62301:Telecel;" +
            "cg:62910:MTN;" +
            "ch:22801:Swisscom;" +
            "ci:61203:Orange;" +
            "cl:73001:Entel;" +
            "cm:62401:MTN;" +
            "co:732101:Claro;" +
            "cr:71201:ICE;" +
            "cv:62501:CV Móvel;" +
            "cy:28001:Cytamobile-Vodafone;" +
            "cz:23001:T-Mobile;" +
            "de:26201:Telekom;" +
            "dj:63801:Evatis;" +
            "dk:23801:TDC;" +
            "dm:366110:Flow;" +
            "do:37001:Claro;" +
            "dz:60301:Mobilis;" +
            "ec:74001:Claro;" +
            "ee:24801:Telia;" +
            "eg:60202:Vodafone;" +
            "er:65701:EriTel;" +
            "es:21401:Movistar;" +
            "et:63601:Ethio Telecom;" +
            "fi:24405:Elisa;" +
            "fj:54201:Vodafone;" +
            "fm:55001:FSMTC;" +
            "fr:20801:Orange;" +
            "ga:62801:Libertis;" +
            "gb:23410:O2;" +
            "gd:352110:Flow;" +
            "ge:28202:MagtiCom;" +
            "gf:34001:Orange;" +
            "gh:62001:MTN;" +
            "gl:29001:Tele Greenland;" +
            "gm:60702:Africell;" +
            "gn:61101:Orange;" +
            "gp:34001:Orange;" +
            "gq:62701:Getesa;" +
            "gr:20201:Cosmote;" +
            "gt:70402:Tigo;" +
            "gw:63203:Orange;" +
            "gy:73801:GTT;" +
            "hk:45400:CSL;" +
            "hn:708002:Tigo;" +
            "hr:21901:Hrvatski Telekom;" +
            "ht:37203:Natcom;" +
            "hu:21630:Telekom;" +
            "id:51010:Telkomsel;" +
            "ie:27201:Vodafone;" +
            "il:42501:Partner;" +
            "iq:41805:Asiacell;" +
            "is:27401:Síminn;" +
            "it:22201:TIM;" +
            "jm:338050:Digicel;" +
            "jo:41601:Zain;" +
            "jp:44010:NTT docomo;" +
            "ke:63902:Safaricom;" +
            "kh:45601:Cellcard;" +
            "ki:54501:ATHKL;" +
            "km:65401:Comores Telecom;" +
            "kn:356110:Flow;" +
            "kr:45005:SK Telecom;" +
            "kw:41902:Zain;" +
            "kz:40101:Beeline;" +
            "la:45701:Lao Telecom;" +
            "lb:41501:Alfa;" +
            "lc:358110:Flow;" +
            "li:29501:Swisscom;" +
            "lk:41301:Mobitel;" +
            "lr:61801:Lonestar;" +
            "ls:65101:Vodacom;" +
            "lt:24601:Telia;" +
            "lu:27001:POST;" +
            "lv:24701:LMT;" +
            "ma:60401:Maroc Telecom;" +
            "mc:21201:Monaco Telecom;" +
            "md:25901:Orange;" +
            "me:29703:m:tel;" +
            "mg:64604:Telma;" +
            "mh:55101:NTA;" +
            "mk:29401:Makedonski Telekom;" +
            "ml:61001:Malitel;" +
            "mm:41401:MPT;" +
            "mn:42899:Mobicom;" +
            "mo:45501:CTM;" +
            "mq:34001:Orange;" +
            "mr:60901:Mauritel;" +
            "mt:27801:Epic;" +
            "mu:61701:my.t;" +
            "mv:47201:Dhiraagu;" +
            "mw:65001:TNM;" +
            "mx:334020:Telcel;" +
            "my:50212:Maxis;" +
            "mz:64301:mcel;" +
            "na:64901:MTC;" +
            "nc:54601:Mobilis;" +
            "ne:61402:Airtel;" +
            "ng:62130:MTN;" +
            "ni:71021:Claro;" +
            "nl:20404:Vodafone;" +
            "no:24201:Telenor;" +
            "np:42901:NTC;" +
            "nr:53602:Digicel;" +
            "nz:53001:One NZ;" +
            "om:42202:Omantel;" +
            "pa:71401:Cable & Wireless;" +
            "pe:71610:Movistar;" +
            "pf:54720:Vini;" +
            "pg:53701:Digicel;" +
            "ph:51502:Globe;" +
            "pk:41001:Jazz;" +
            "pl:26003:Orange;" +
            "pr:330110:Claro;" +
            "pt:26801:MEO;" +
            "pw:55201:PNCC;" +
            "py:74404:Tigo;" +
            "qa:42701:Ooredoo;" +
            "re:64700:Orange;" +
            "ro:22610:Orange;" +
            "rs:22003:mts;" +
            "rw:63510:MTN;" +
            "sa:42001:STC;" +
            "sb:54001:Our Telekom;" +
            "sc:63301:Cable & Wireless;" +
            "sd:63401:Zain;" +
            "se:24001:Telia;" +
            "sg:52501:Singtel;" +
            "si:29341:Telekom Slovenije;" +
            "sk:23101:Orange;" +
            "sl:61901:Orange;" +
            "sm:29201:TMS;" +
            "sn:60801:Orange;" +
            "sr:74602:Telesur;" +
            "ss:65902:MTN;" +
            "st:62601:CST;" +
            "sv:70603:Tigo;" +
            "sz:65310:MTN;" +
            "td:62201:Airtel;" +
            "tg:61501:Togocel;" +
            "th:52001:AIS;" +
            "tj:43601:Tcell;" +
            "tl:51402:Telemor;" +
            "tn:60502:Tunisie Telecom;" +
            "to:53943:Digicel;" +
            "tr:28601:Turkcell;" +
            "tt:374130:Digicel;" +
            "tv:55301:TTC;" +
            "tz:64004:Vodacom;" +
            "ua:25503:Kyivstar;" +
            "ug:64110:MTN;" +
            "us:310260:T-Mobile;" +
            "uy:74801:Antel;" +
            "uz:43404:Beeline;" +
            "vc:360110:Flow;" +
            "ve:73404:Movistar;" +
            "vn:45204:Viettel;" +
            "vu:54105:Digicel;" +
            "ws:54901:Digicel;" +
            "yt:64710:SFR;" +
            "za:65501:Vodacom;" +
            "zm:64501:Airtel;" +
            "zw:64804:Econet;";

    private static volatile List<Region> all;

    private Regions() { }

    static List<Region> all() {
        List<Region> cached = all;
        if (cached != null) return cached;
        List<Region> parsed = new ArrayList<>();
        for (String entry : TABLE.split(";")) {
            String[] parts = entry.split(":", 3);
            if (parts.length == 3) parsed.add(new Region(parts[0], parts[1], parts[2]));
        }
        all = Collections.unmodifiableList(parsed);
        return all;
    }

    static List<Region> sorted(Locale locale) {
        List<Region> result = new ArrayList<>(all());
        Collator collator = Collator.getInstance(locale);
        result.sort((a, b) -> collator.compare(a.name(locale), b.name(locale)));
        return result;
    }

    static Region find(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        for (Region region : all()) if (region.iso.equals(iso)) return region;
        return null;
    }

    static String normalize(String iso) {
        String value = iso == null ? "" : iso.trim().toLowerCase(Locale.ROOT);
        return find(value) == null ? "" : value;
    }
}
