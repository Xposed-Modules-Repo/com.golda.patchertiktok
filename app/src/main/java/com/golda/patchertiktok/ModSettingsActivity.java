package com.golda.patchertiktok;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.golda.patchertiktok.I18n.S;

/**
 * The module's settings screen. TikTok starts its own settings activity with
 * {@link #EXTRA}; {@link SettingsEntry} swaps in this class, so the screen lives in
 * TikTok's task and theme without a manifest entry of its own.
 */
public final class ModSettingsActivity extends Activity {
    static final String EXTRA = "com.golda.patchertiktok.SETTINGS";
    static final String EXTRA_DARK = "com.golda.patchertiktok.DARK";
    static final String SOURCE_URL = "https://github.com/hik0w/TiktokPatchXposed";

    private final ArrayDeque<View> pages = new ArrayDeque<>();
    private final Runnable prefsListener = this::onPrefsChanged;
    private Palette palette;
    private Locale locale;
    private FrameLayout stack;
    private View restartBar;
    private Object backCallback;
    private int topInset;
    private int bottomInset;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Prefs.load(getApplicationContext());
        locale = getResources().getConfiguration().getLocales().get(0);
        I18n.use(locale);
        boolean dark = getIntent().hasExtra(EXTRA_DARK)
                ? getIntent().getBooleanExtra(EXTRA_DARK, true)
                : (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        palette = Palette.of(dark);
        setupWindow();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(palette.background);
        stack = new FrameLayout(this);
        root.addView(stack, new FrameLayout.LayoutParams(-1, -1));
        View statusScrim = new View(this);
        statusScrim.setBackgroundColor(palette.statusBar);
        root.addView(statusScrim, new FrameLayout.LayoutParams(-1, 0, Gravity.TOP));
        restartBar = restartBar();
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(restartBar, barParams);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                topInset = bars.top;
                bottomInset = Math.max(bars.bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            for (int index = 0; index < stack.getChildCount(); index++) applyInsets(stack.getChildAt(index));
            statusScrim.getLayoutParams().height = topInset;
            statusScrim.requestLayout();
            restartBar.setPadding(dp(12), dp(8), dp(12), dp(12) + bottomInset);
            return insets;
        });
        setContentView(root);
        push(mainPage(), false);
        updateRestartBar(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Prefs.listen(prefsListener);
    }

    @Override
    protected void onStop() {
        Prefs.unlisten(prefsListener);
        super.onStop();
    }

    /** Used when TikTok has not opted into predictive back; otherwise {@link #syncBackCallback()} applies. */
    @Override
    @SuppressWarnings("deprecation")
    @android.annotation.SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        if (pages.size() > 1) pop();
        else super.onBackPressed();
    }

    // ---- window ----------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private void setupWindow() {
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(palette.background));
        window.setStatusBarColor(0);
        window.setNavigationBarColor(0);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(palette.dark ? 0 : light, light);
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!palette.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    // ---- navigation ------------------------------------------------------------------------

    private void push(View page, boolean animate) {
        View previous = pages.peek();
        pages.push(page);
        stack.addView(page, new FrameLayout.LayoutParams(-1, -1));
        applyInsets(page);
        if (animate && previous != null) {
            float width = getResources().getDisplayMetrics().widthPixels;
            page.setTranslationX(width);
            page.animate().translationX(0).setDuration(280)
                    .setInterpolator(new android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)).start();
            previous.animate().translationX(-width * 0.3f).setDuration(280)
                    .setInterpolator(new android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
                    .withEndAction(() -> previous.setVisibility(View.INVISIBLE)).start();
        }
        syncBackCallback();
    }

    private void pop() {
        View page = pages.pop();
        View previous = pages.peek();
        hideKeyboard(page);
        float width = getResources().getDisplayMetrics().widthPixels;
        if (previous != null) {
            previous.setVisibility(View.VISIBLE);
            previous.animate().translationX(0).setDuration(250).start();
        }
        page.animate().translationX(width).setDuration(250).withEndAction(() -> stack.removeView(page)).start();
        if (previous != null && previous.getTag() instanceof Runnable) ((Runnable) previous.getTag()).run();
        syncBackCallback();
    }

    /** API 33+ dispatches back through callbacks when the host app opts into predictive back. */
    private void syncBackCallback() {
        if (Build.VERSION.SDK_INT < 33) return;
        android.window.OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();
        if (pages.size() > 1 && backCallback == null) {
            android.window.OnBackInvokedCallback callback = this::pop;
            dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            backCallback = callback;
        } else if (pages.size() <= 1 && backCallback != null) {
            dispatcher.unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback) backCallback);
            backCallback = null;
        }
    }

    private void applyInsets(View page) {
        Object holder = page.getTag(R_ID_INSETS);
        if (holder instanceof Insettable) ((Insettable) holder).apply(topInset, bottomInset);
    }

    private interface Insettable { void apply(int top, int bottom); }

    private static final int R_ID_INSETS = 0x7f0b0001;

    // ---- pages -----------------------------------------------------------------------------

    private View mainPage() {
        Page page = new Page(I18n.get(S.APP_NAME), true);
        page.section(I18n.get(S.SECTION_FEED));
        LinearLayout feed = page.card();
        toggle(feed, S.ADS, S.ADS_DESC, Prefs.ADS);
        toggle(feed, S.LIVE, S.LIVE_DESC, Prefs.LIVE);
        toggle(feed, S.ACQUAINTANCES, S.ACQUAINTANCES_DESC, Prefs.ACQUAINTANCES);
        toggle(feed, S.SEEKBAR, S.SEEKBAR_DESC, Prefs.SEEKBAR);

        page.section(I18n.get(S.SECTION_SOCIAL));
        LinearLayout social = page.card();
        toggle(social, S.COMMENT_REPOSTS, S.COMMENT_REPOSTS_DESC, Prefs.COMMENT_REPOSTS);
        toggle(social, S.NEW_PROFILE, S.NEW_PROFILE_DESC, Prefs.NEW_PROFILE);
        toggle(social, S.EXTRA_FEATURES, S.EXTRA_FEATURES_DESC, Prefs.EXTRA_FEATURES);

        page.section(I18n.get(S.SECTION_SHARING));
        LinearLayout sharing = page.card();
        toggle(sharing, S.NO_WATERMARK, null, Prefs.NO_WATERMARK);
        toggle(sharing, S.DOWNLOAD_ANY, S.DOWNLOAD_ANY_DESC, Prefs.DOWNLOAD_ANY);
        toggle(sharing, S.SCREENSHOTS, S.SCREENSHOTS_DESC, Prefs.SCREENSHOTS);
        toggle(sharing, S.CLEAN_LINKS, S.CLEAN_LINKS_DESC, Prefs.CLEAN_LINKS);

        page.section(I18n.get(S.SECTION_REGION));
        LinearLayout regionCard = page.card();
        TextView regionValue = navigation(regionCard, I18n.get(S.REGION), regionLabel(),
                view -> push(regionPage(), true));
        page.footer(I18n.get(S.REGION_DESC));

        page.section(I18n.get(S.SECTION_STREAKS));
        LinearLayout streaks = page.card();
        toggle(streaks, S.AUTO_STREAK, S.AUTO_STREAK_DESC, Prefs.AUTO_STREAK);

        page.section(I18n.get(S.SECTION_ABOUT));
        LinearLayout about = page.card();
        navigation(about, I18n.get(S.VERSION), BuildConfig.VERSION_NAME, null);
        navigation(about, I18n.get(S.SOURCE_CODE), null, view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)));
            } catch (RuntimeException ignored) { }
        });
        page.root.setTag((Runnable) () -> regionValue.setText(regionLabel()));
        return page.root;
    }

    private View regionPage() {
        Page page = new Page(I18n.get(S.REGION), false);
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(12), 0, dp(12), 0);
        searchBox.setBackground(rounded(palette.field, dp(8)));
        ImageView glass = new ImageView(this);
        glass.setImageDrawable(new Glyph(Glyph.Kind.SEARCH, palette.textSecondary, dp(18)));
        searchBox.addView(glass, new LinearLayout.LayoutParams(dp(18), dp(18)));
        EditText search = new EditText(this);
        search.setBackground(null);
        search.setHint(I18n.get(S.SEARCH));
        search.setHintTextColor(palette.textSecondary);
        search.setTextColor(palette.textPrimary);
        search.setTextSize(16);
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setPadding(dp(8), 0, 0, 0);
        searchBox.addView(search, new LinearLayout.LayoutParams(0, dp(40), 1));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(-1, dp(40));
        boxParams.setMargins(dp(16), dp(8), dp(16), dp(12));
        page.content.addView(searchBox, boxParams);

        LinearLayout list = page.card();
        List<View> rows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        String selected = Prefs.region();
        rows.add(choice(list, I18n.get(S.REGION_OFF), selected.isEmpty(), () -> Prefs.setRegion("")));
        names.add("");
        for (Regions.Region region : Regions.sorted(locale)) {
            String name = region.name(locale);
            rows.add(choice(list, name, region.iso.equals(selected), () -> Prefs.setRegion(region.iso)));
            names.add(name.toLowerCase(locale) + " " + region.iso + " " + region.name(Locale.ENGLISH).toLowerCase(Locale.ROOT));
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase(locale);
                for (int index = 0; index < rows.size(); index++) {
                    boolean visible = query.isEmpty() ? true : index > 0 && names.get(index).contains(query);
                    rows.get(index).setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            }
        });
        return page.root;
    }

    private String regionLabel() {
        Regions.Region region = Regions.find(Prefs.region());
        return region == null ? I18n.get(S.OFF) : region.name(locale);
    }

    // ---- rows ------------------------------------------------------------------------------

    private void toggle(LinearLayout card, S title, S description, String key) {
        LinearLayout row = row(card);
        LinearLayout texts = texts(row, I18n.get(title), description == null ? null : I18n.get(description));
        Toggle toggle = new Toggle(this, palette);
        toggle.setChecked(Prefs.on(key), false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMarginStart(dp(12));
        row.addView(toggle, params);
        row.setContentDescription(texts.getContentDescription());
        texts.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        row.setOnClickListener(view -> {
            boolean value = !toggle.checked();
            toggle.setChecked(value, true);
            Prefs.set(key, value);
            row.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED);
        });
        row.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Switch");
                info.setCheckable(true);
                info.setChecked(toggle.checked());
            }
        });
    }

    private TextView navigation(LinearLayout card, String title, String value, View.OnClickListener click) {
        LinearLayout row = row(card);
        texts(row, title, null);
        TextView end = new TextView(this);
        end.setTextColor(palette.textSecondary);
        end.setTextSize(16);
        end.setTypeface(Fonts.regular(this));
        end.setSingleLine(true);
        end.setEllipsize(TextUtils.TruncateAt.END);
        end.setMaxWidth(dp(180));
        if (value != null) end.setText(value);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-2, -2);
        valueParams.setMarginStart(dp(12));
        row.addView(end, valueParams);
        if (click != null) {
            ImageView chevron = new ImageView(this);
            chevron.setImageDrawable(new Glyph(Glyph.Kind.CHEVRON, palette.chevron, dp(20)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(20), dp(20));
            params.setMarginStart(dp(4));
            row.addView(chevron, params);
            row.setOnClickListener(click);
        } else {
            row.setClickable(false);
            row.setBackground(null);
        }
        return end;
    }

    private View choice(LinearLayout card, String title, boolean selected, Runnable select) {
        LinearLayout row = row(card);
        row.setMinimumHeight(dp(52));
        texts(row, title, null);
        ImageView check = new ImageView(this);
        check.setImageDrawable(new Glyph(Glyph.Kind.CHECK, palette.accent, dp(22)));
        check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        row.addView(check, new LinearLayout.LayoutParams(dp(22), dp(22)));
        row.setSelected(selected);
        row.setOnClickListener(view -> {
            select.run();
            hideKeyboard(view);
            pop();
        });
        return row;
    }

    private LinearLayout row(LinearLayout card) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(palette.pressed), null,
                new ColorDrawable(0xFFFFFFFF)));
        row.setClickable(true);
        row.setFocusable(true);
        card.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private LinearLayout texts(LinearLayout row, String title, String description) {
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(palette.textPrimary);
        titleView.setTextSize(17);
        titleView.setTypeface(Fonts.semibold(this));
        texts.addView(titleView);
        if (description != null) {
            TextView descriptionView = new TextView(this);
            descriptionView.setText(description);
            descriptionView.setTextColor(palette.textSecondary);
            descriptionView.setTextSize(14);
            descriptionView.setTypeface(Fonts.regular(this));
            descriptionView.setPadding(0, dp(2), 0, 0);
            texts.addView(descriptionView);
        }
        texts.setContentDescription(description == null ? title : title + ". " + description);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        return texts;
    }

    // ---- restart ---------------------------------------------------------------------------

    private View restartBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout inner = new LinearLayout(this);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(16), dp(10), dp(10), dp(10));
        inner.setBackground(rounded(palette.dark ? 0xFF2B2B2B : 0xFF161823, dp(12)));
        inner.setElevation(dp(6));
        TextView hint = new TextView(this);
        hint.setText(I18n.get(S.RESTART_HINT));
        hint.setTextColor(0xFFFFFFFF);
        hint.setTextSize(14);
        hint.setTypeface(Fonts.regular(this));
        inner.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        TextView button = new TextView(this);
        button.setText(I18n.get(S.RESTART));
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(15);
        button.setTypeface(Fonts.semibold(this));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), 0, dp(16), 0);
        button.setMinHeight(dp(40));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),
                rounded(palette.accent, dp(6)), null));
        button.setOnClickListener(view -> AppRestart.restart(this));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, dp(40));
        buttonParams.setMarginStart(dp(12));
        inner.addView(button, buttonParams);
        bar.addView(inner, new LinearLayout.LayoutParams(-1, -2));
        bar.setVisibility(View.GONE);
        return bar;
    }

    private void onPrefsChanged() {
        runOnUiThread(() -> updateRestartBar(true));
    }

    private void updateRestartBar(boolean animate) {
        boolean show = Prefs.restartPending();
        if (show == (restartBar.getVisibility() == View.VISIBLE)) return;
        if (!show) {
            restartBar.setVisibility(View.GONE);
            return;
        }
        restartBar.setVisibility(View.VISIBLE);
        if (animate) {
            restartBar.setAlpha(0f);
            restartBar.setTranslationY(dp(24));
            restartBar.animate().alpha(1f).translationY(0).setDuration(220).start();
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void hideKeyboard(View view) {
        android.view.inputmethod.InputMethodManager manager = getSystemService(android.view.inputmethod.InputMethodManager.class);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    int dp(float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    /** A scrolling page with TikTok's collapsing large title and a toolbar. */
    private final class Page {
        final FrameLayout root = new FrameLayout(ModSettingsActivity.this);
        final LinearLayout content = new LinearLayout(ModSettingsActivity.this);
        private final ScrollView scroll = new ScrollView(ModSettingsActivity.this);
        private final FrameLayout toolbar = new FrameLayout(ModSettingsActivity.this);

        Page(String title, boolean root) {
            this.root.setBackgroundColor(palette.background);
            this.root.setClickable(true);
            content.setOrientation(LinearLayout.VERTICAL);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            scroll.setClipToPadding(false);
            scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
            this.root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

            toolbar.setBackgroundColor(palette.background);
            ImageView back = new ImageView(ModSettingsActivity.this);
            back.setImageDrawable(new Glyph(Glyph.Kind.BACK, palette.textPrimary, dp(24)));
            back.setScaleType(ImageView.ScaleType.CENTER);
            back.setContentDescription(I18n.get(S.BACK));
            back.setBackground(new RippleDrawable(ColorStateList.valueOf(palette.pressed), null, null));
            back.setOnClickListener(view -> {
                if (pages.size() > 1) pop();
                else finish();
            });
            FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.START | Gravity.BOTTOM);
            backParams.setMarginStart(dp(4));
            backParams.bottomMargin = dp(4);
            toolbar.addView(back, backParams);
            TextView small = new TextView(ModSettingsActivity.this);
            small.setText(title);
            small.setTextColor(palette.textPrimary);
            small.setTextSize(18);
            small.setTypeface(Fonts.bold(ModSettingsActivity.this));
            small.setSingleLine(true);
            small.setEllipsize(TextUtils.TruncateAt.END);
            small.setGravity(Gravity.CENTER_VERTICAL);
            FrameLayout.LayoutParams smallParams = new FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM);
            smallParams.setMarginStart(dp(60));
            smallParams.setMarginEnd(dp(16));
            toolbar.addView(small, smallParams);
            this.root.addView(toolbar, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

            TextView large = null;
            if (root) {
                large = new TextView(ModSettingsActivity.this);
                large.setText(title);
                large.setTextColor(palette.textPrimary);
                large.setTextSize(32);
                large.setTypeface(Fonts.bold(ModSettingsActivity.this));
                large.setPadding(dp(32), dp(8), dp(32), dp(12));
                content.addView(large);
                small.setAlpha(0f);
            }
            TextView largeTitle = large;
            scroll.setOnScrollChangeListener((view, x, y, oldX, oldY) -> {
                if (largeTitle == null) return;
                float fade = Math.max(0f, Math.min(1f, (y - largeTitle.getHeight() * 0.5f) / (largeTitle.getHeight() * 0.4f)));
                small.setAlpha(fade);
            });
            this.root.setTag(R_ID_INSETS, (Insettable) (top, bottom) -> {
                toolbar.setPadding(0, top, 0, 0);
                toolbar.getLayoutParams().height = top + dp(56);
                toolbar.requestLayout();
                scroll.setPadding(0, top + dp(56), 0, bottom + dp(96));
            });
        }

        void section(String title) {
            TextView header = new TextView(ModSettingsActivity.this);
            header.setText(title);
            header.setTextColor(palette.textSecondary);
            header.setTextSize(15);
            header.setTypeface(Fonts.semibold(ModSettingsActivity.this));
            header.setPadding(dp(32), dp(20), dp(32), dp(10));
            if (Build.VERSION.SDK_INT >= 28) header.setAccessibilityHeading(true);
            content.addView(header);
        }

        LinearLayout card() {
            LinearLayout card = new LinearLayout(ModSettingsActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(0, dp(4), 0, dp(4));
            GradientDrawable background = rounded(palette.card, dp(12));
            card.setBackground(background);
            card.setClipToOutline(true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(dp(8), 0, dp(8), 0);
            content.addView(card, params);
            return card;
        }

        void footer(String text) {
            TextView footer = new TextView(ModSettingsActivity.this);
            footer.setText(text);
            footer.setTextColor(palette.textTertiary);
            footer.setTextSize(13);
            footer.setTypeface(Fonts.regular(ModSettingsActivity.this));
            footer.setPadding(dp(24), dp(8), dp(24), 0);
            content.addView(footer);
        }
    }

    /** Builds the intent that opens this screen from inside TikTok. */
    static Intent intent(Context host, boolean dark) {
        Intent intent = new Intent();
        intent.setClassName(host.getPackageName(), SettingsEntry.HOST_ACTIVITY);
        intent.putExtra(EXTRA, true);
        intent.putExtra(EXTRA_DARK, dark);
        if (!(host instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
