# TiktokPatchXposed

[Русский](README.md) | **English**

An LSPosed module for TikTok. It removes ads, unlocks features TikTok only gives to some users, and adds a few useful extras. All settings are inside TikTok itself.

Current version: 4.0.1. Tested on TikTok 47.1.4 from Google Play, Android 16.

## Features

**Feed**
- removes ads: in the feed, on the startup splash, inside videos and in other video lists
- hides LIVE streams and the LIVE button in the top-left corner
- hides videos marked "People you may know"
- shows the seek bar on every video, short ones included

**Comments & profile**
- comment reposts
- the new profile layout with banners
- hidden TikTok features: long-press speed-up, comments to Favorites, audio and photo comments, background playback and more

**Downloads & links**
- downloads without a watermark
- download any video, even if the author turned downloads off
- screenshots and screen recording work everywhere
- clean links: tracking parameters (`_r`, `_t`, `u_code`, `share_link_id` and so on) are removed from copied TikTok links

**Region**
- SIM country spoofing with around 190 countries. Germany is the default; you can pick another country or turn it off. Recommendations stay in your own language

**Streaks**
- auto streaks, off by default, see below

Comment reposts, the new profile and the hidden features are server-side TikTok experiments that the module switches on locally. These and the region take effect after TikTok restarts; the settings screen shows a restart button when needed.

## Where to find the settings

TikTok → Profile → ☰ → Settings and privacy → **TiktokPatchXposed** (the very first row).

The screen looks like TikTok's own settings and follows TikTok's theme (Display → light/dark) and app language. Translations: English, Russian, Ukrainian, German, Spanish, Portuguese, French, Italian, Polish, Turkish and Indonesian. Belarusian and Kazakh use Russian; any other language falls back to English.

The module has no app of its own. The settings button in LSPosed opens the same screen inside TikTok.

## Installation

1. Install the APK and enable the module for TikTok in LSPosed.
2. Force-stop TikTok and open it again.
3. Open TikTok's settings → TiktokPatchXposed.

Updating from an older version: just install over it. Auto streaks need to be turned on again after updating.

## Compatibility

Older versions had to be adjusted to new class names after every TikTok update. Now, on the first launch of a new TikTok build, the module finds the code it needs by itself (by strings and signatures), which takes a fraction of a second, and remembers the result. New TikTok versions should therefore keep working without a module update. This can't be guaranteed: if something can't be found, that feature is simply skipped.

Both `com.zhiliaoapp.musically` and `com.ss.android.ugc.trill` are supported (the latter hasn't been tested on a device).

The module uses LSPosed's modern API (libxposed 101–102), so newer LSPosed builds no longer mark it as "legacy". Older LSPosed 1.9.x is still supported: the APK keeps the old loading path for it.

## Auto streaks

Turned on manually. The module only renews existing streaks in one-to-one chats, and only when a streak is about to expire. It uses TikTok's `spark_v1` interaction, not a text message. It works while TikTok is open: no alarms and no background services. Every send is recorded first, so the same streak isn't renewed twice in a day. The module pauses a few seconds between chats, as if you were going through them by hand. If TikTok doesn't accept a streak, you'll get a notification so you can check that chat yourself.

## Building

Requires Java 17 and Android SDK 36.

```shell
./gradlew testDebugUnitTest assembleRelease
```

## Disclaimer

Modifying the client and automating actions can lead to account restrictions. Use at your own risk.
