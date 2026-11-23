// ===== MAZWD PART 05/05: SETTINGS — v2.7.1 =====

    // ===== شاشة الإعدادات والمظهر الفاخر =====
    public void showSettingsScreen() {
        saveCurrentNoteImmediatelyIfNeeded();
        isInSettingsScreen = true;
        isInDocumentScreen = false;
        updateSystemBars();

        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getScreenBgColor());

        LinearLayout settingsRoot = new LinearLayout(this);
        settingsRoot.setOrientation(LinearLayout.VERTICAL);
        settingsRoot.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        settingsRoot.setClipChildren(false);
        settingsRoot.setClipToPadding(false);
        settingsRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // الشريط العلوي لشاشة الإعدادات
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(14), dp(12), dp(14), dp(12));
        topBar.setBackgroundColor(getThemeDarkColor());
        topBar.setElevation(dp(6));

        TextView backBtn = new TextView(this);
        backBtn.setTag("btn");
        backBtn.setText("→  رجوع");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        backBtn.setTypeface(getAppTypeface(true));
        backBtn.setTextColor(0xFFFFFFFF);
        backBtn.setGravity(Gravity.CENTER);
        backBtn.setPadding(dp(12), dp(7), dp(12), dp(7));
        backBtn.setBackground(createInteractiveCardDrawable(0x26FFFFFF, 0x33FFFFFF, 1, 12));
        applySpringTouch(backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isInSettingsScreen = false;
                showHomeScreen();
            }
        });
        topBar.addView(backBtn);

        TextView titleTv = new TextView(this);
        titleTv.setText("إعدادات «" + BRAND_NAME + "» والمظهر");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.5f);
        titleTv.setTypeface(getAppTypeface(true));
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        topBar.addView(titleTv, titleLp);

        TextView verBadge = new TextView(this);
        verBadge.setTag("badge");
        verBadge.setText(APP_VERSION_NAME);
        verBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        verBadge.setTypeface(getAppTypeface(true));
        verBadge.setTextColor(0xE6FFFFFF);
        verBadge.setPadding(dp(10), dp(5), dp(10), dp(5));
        verBadge.setBackground(createRoundedDrawable(0x22FFFFFF, 0x33FFFFFF, 1, 10));
        topBar.addView(verBadge);

        settingsRoot.addView(topBar);

        // محتوى الإعدادات القابل للتمرير
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);
        sv.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        sv.addView(content);

        // 1) بطاقة الثيمات الخليجية والملكية الـ 8 + الوضع الليلي
        content.addView(buildThemesAndDarkModeSectionCard());

        // 2) بطاقة الخطوط الـ 12 وحجم النص وتباعد الأسطر
        content.addView(buildFontsAndTypographySectionCard());

        // 3) بطاقة أدوات التحرير والتوقيع والتنسيق الآلي
        content.addView(buildToolbarAndEditorSettingsCard());

        // 4) بطاقة تخصيص الهوية ومجلد التخزين والنسخ الآمنة
        content.addView(buildIdentityAndStorageSettingsCard());

        settingsRoot.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        rootContainer.addView(settingsRoot);
        applyFontToViewTree(rootContainer);
    }

    // ===== الأزرار المنزلقة المضيئة (Sliding Switches) على يمين النص (بداية السطر العربي RTL) =====
    public View createSlidingSwitchView(final boolean initialChecked, final Runnable onToggle) {
        final boolean[] state = new boolean[]{initialChecked};

        final FrameLayout track = new FrameLayout(this);
        track.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        track.setClipChildren(false);
        track.setClipToPadding(false);

        final View thumb = new View(this);
        GradientDrawable thumbDrawable = new GradientDrawable();
        thumbDrawable.setShape(GradientDrawable.OVAL);
        thumbDrawable.setColor(0xFFFFFFFF);
        thumb.setBackground(thumbDrawable);
        thumb.setElevation(dp(3));

        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(dp(20), dp(20));
        thumbLp.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
        thumbLp.leftMargin = dp(3);
        track.addView(thumb, thumbLp);

        final Runnable updateVisuals = new Runnable() {
            @Override
            public void run() {
                int trackColor = state[0]
                        ? getThemePrimaryColor()
                        : (isDarkMode ? 0xFF334155 : 0xFFCBD5E1);
                int borderColor = state[0]
                        ? 0x44FFFFFF
                        : getSubtleBorderColor();
                track.setBackground(createRoundedDrawable(trackColor, borderColor, 1, 14));
                float targetX = state[0] ? dp(20) : 0f;
                thumb.animate()
                        .translationX(targetX)
                        .setDuration(160)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        };
        updateVisuals.run();

        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state[0] = !state[0];
                performHaptic(v);
                updateVisuals.run();
                if (onToggle != null) {
                    onToggle.run();
                }
            }
        });

        return track;
    }

    private View buildRightAlignedSwitchRow(
            String title,
            String subtitle,
            boolean currentVal,
            final Runnable onToggle
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        int bg = isDarkMode ? 0xFF0F172A : 0xFFF8FAFC;
        row.setBackground(createInteractiveCardDrawable(bg, getSubtleBorderColor(), 1, 14));

        // المفتاح المنزلق يقع على يمين النص (أول عنصر في السطر العربي RTL)
        final View switchView = createSlidingSwitchView(currentVal, onToggle);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(46), dp(26));
        swLp.leftMargin = dp(12);
        row.addView(switchView, swLp);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams txtLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        t1.setTypeface(getAppTypeface(true));
        t1.setTextColor(getPrimaryTextColor());
        t1.setGravity(Gravity.RIGHT);
        textCol.addView(t1);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView t2 = new TextView(this);
            t2.setText(subtitle);
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            t2.setTypeface(getAppTypeface(false));
            t2.setTextColor(getSecondaryTextColor());
            t2.setGravity(Gravity.RIGHT);
            t2.setPadding(0, dp(2), 0, 0);
            textCol.addView(t2);
        }

        row.addView(textCol, txtLp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView.performClick();
            }
        });

        return row;
    }

    // ===== 1) قسم الثيمات الـ 8 والوضع الليلي =====
    private View buildThemesAndDarkModeSectionCard() {
        LinearLayout card = createSettingsSectionCard(
                "المظهر والثيمات الملكية (8 ثيمات)",
                "اختر الهوية اللونية المفضلة أو فعّل الوضع الليلي المريح للعين"
        );

        // مفتاح الوضع الليلي على يمين النص
        View darkSwitchRow = buildRightAlignedSwitchRow(
                "الوضع الليلي المريح للعين (Dark Mode)",
                "خلفيات كحلية داكنة تحمي العين أثناء القراءة والكتابة الليلية",
                isDarkMode,
                new Runnable() {
                    @Override
                    public void run() {
                        isDarkMode = !isDarkMode;
                        prefs.edit().putBoolean("dark_mode", isDarkMode).apply();
                        showSettingsScreen();
                    }
                }
        );
        LinearLayout.LayoutParams dmLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dmLp.bottomMargin = dp(12);
        card.addView(darkSwitchRow, dmLp);

        // شبكة الثيمات الـ 8 (صفوف ثنائية أنيقة)
        for (int i = 0; i < THEME_NAMES.length; i += 2) {
            LinearLayout pairRow = new LinearLayout(this);
            pairRow.setOrientation(LinearLayout.HORIZONTAL);
            pairRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            pairRow.setClipChildren(false);
            pairRow.setClipToPadding(false);

            LinearLayout.LayoutParams pairLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            pairLp.bottomMargin = dp(8);

            View item1 = createThemeChoiceChip(i);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp1.leftMargin = dp(5);
            pairRow.addView(item1, lp1);

            if (i + 1 < THEME_NAMES.length) {
                View item2 = createThemeChoiceChip(i + 1);
                LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp2.rightMargin = dp(5);
                pairRow.addView(item2, lp2);
            }

            card.addView(pairRow, pairLp);
        }

        return card;
    }

    private View createThemeChoiceChip(final int themeIdx) {
        boolean isSelected = (currentThemeIndex == themeIdx);
        int primaryCol = THEME_PALETTES[themeIdx][0];
        int darkCol = THEME_PALETTES[themeIdx][1];

        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(10), dp(10), dp(10), dp(10));

        int chipBg = isSelected
                ? (isDarkMode ? 0xFF0F172A : THEME_PALETTES[themeIdx][2])
                : (isDarkMode ? 0xFF0F172A : 0xFFF8FAFC);
        int borderCol = isSelected ? primaryCol : getSubtleBorderColor();
        int borderW = isSelected ? 2 : 1;

        chip.setBackground(createInteractiveCardDrawable(chipBg, borderCol, borderW, 14));
        applySpringTouch(chip);

        // دائرة اللوحة اللونية المزدوجة
        View swatch = new View(this);
        GradientDrawable swGd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{darkCol, primaryCol}
        );
        swGd.setShape(GradientDrawable.OVAL);
        swGd.setStroke(dp(1), 0x55FFFFFF);
        swatch.setBackground(swGd);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        swLp.leftMargin = dp(8);
        chip.addView(swatch, swLp);

        TextView nameTv = new TextView(this);
        String shortName = THEME_NAMES[themeIdx].split("\\(")[0].trim();
        nameTv.setText(isSelected ? ("✓ " + shortName) : shortName);
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        nameTv.setTypeface(getAppTypeface(isSelected));
        nameTv.setTextColor(getPrimaryTextColor());
        nameTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        nameTv.setMaxLines(1);
        nameTv.setEllipsize(TextUtils.TruncateAt.END);
        chip.addView(nameTv, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentThemeIndex = themeIdx;
                prefs.edit().putInt("theme_index", currentThemeIndex).apply();
                showSettingsScreen();
            }
        });

        return chip;
    }

    // ===== 2) قسم الخطوط الـ 12 وحجم النص وتباعد الأسطر =====
    private View buildFontsAndTypographySectionCard() {
        LinearLayout card = createSettingsSectionCard(
                "الخطوط وتنسيق القراءة (12 خطاً)",
                "اختر الخط العربي أو الفني المناسب، واضبط حجم الحروف وتباعد الأسطر"
        );

        // بطاقة معاينة حية للخط المختار (تختبر نقاط حرف الياء والتشكيل)
        LinearLayout previewBox = new LinearLayout(this);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        previewBox.setBackground(createRoundedDrawable(getPaperBgColor(), getLuxuryAccentBorderColor(), 1, 14));

        TextView previewLabel = new TextView(this);
        previewLabel.setText("معاينة حية — " + FONT_NAMES[currentFontIndex]);
        previewLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        previewLabel.setTypeface(getAppTypeface(true));
        previewLabel.setTextColor(getThemePrimaryColor());
        previewLabel.setGravity(Gravity.RIGHT);
        previewBox.addView(previewLabel);

        TextView sampleTv = new TextView(this);
        sampleTv.setText("العربيّةُ لُغةُ البيانِ والجمالِ — يَسمو الفِكرُ بالتدوينِ الرّاقي.");
        sampleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseFontSizeSp);
        sampleTv.setLineSpacing(0f, lineSpacingMultiplier);
        sampleTv.setTypeface(getAppTypeface(false));
        sampleTv.setTextColor(getPrimaryTextColor());
        sampleTv.setGravity(Gravity.RIGHT);
        sampleTv.setPadding(0, dp(6), 0, dp(2));
        previewBox.addView(sampleTv);

        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        prevLp.bottomMargin = dp(12);
        card.addView(previewBox, prevLp);

        // زر اختيار الخط من القائمة الكاملة (12 خطاً)
        Button chooseFontBtn = new Button(this);
        chooseFontBtn.setTag("btn");
        chooseFontBtn.setAllCaps(false);
        chooseFontBtn.setText("تغيير نوع الخط (" + FONT_NAMES[currentFontIndex].split("—")[0].trim() + ")");
        chooseFontBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        chooseFontBtn.setTypeface(getAppTypeface(true));
        chooseFontBtn.setTextColor(0xFFFFFFFF);
        chooseFontBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        applySpringTouch(chooseFontBtn);

        LinearLayout.LayoutParams cfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        cfLp.bottomMargin = dp(12);
        chooseFontBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFontPickerDialog();
            }
        });
        card.addView(chooseFontBtn, cfLp);

        // التحكم بحجم الخط الأساسي
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sizeRow.setGravity(Gravity.CENTER_VERTICAL);
        sizeRow.setPadding(dp(12), dp(8), dp(12), dp(8));
        sizeRow.setBackground(createRoundedDrawable(
                isDarkMode ? 0xFF0F172A : 0xFFF8FAFC, getSubtleBorderColor(), 1, 14
        ));

        TextView sizeTitle = new TextView(this);
        sizeTitle.setText("حجم الخط الأساسي: " + baseFontSizeSp + "sp");
        sizeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        sizeTitle.setTypeface(getAppTypeface(true));
        sizeTitle.setTextColor(getPrimaryTextColor());
        sizeRow.addView(sizeTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        Button plusSizeBtn = createMiniControlButton("＋", new Runnable() {
            @Override
            public void run() {
                if (baseFontSizeSp < 32) {
                    baseFontSizeSp++;
                    prefs.edit().putInt("base_font_size_sp", baseFontSizeSp).apply();
                    showSettingsScreen();
                }
            }
        });
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(dp(42), dp(38));
        pLp.leftMargin = dp(6);
        sizeRow.addView(plusSizeBtn, pLp);

        Button minusSizeBtn = createMiniControlButton("－", new Runnable() {
            @Override
            public void run() {
                if (baseFontSizeSp > 13) {
                    baseFontSizeSp--;
                    prefs.edit().putInt("base_font_size_sp", baseFontSizeSp).apply();
                    showSettingsScreen();
                }
            }
        });
        sizeRow.addView(minusSizeBtn, new LinearLayout.LayoutParams(dp(42), dp(38)));

        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        srLp.bottomMargin = dp(10);
        card.addView(sizeRow, srLp);

        // التحكم بتباعد الأسطر
        LinearLayout spacingRow = new LinearLayout(this);
        spacingRow.setOrientation(LinearLayout.HORIZONTAL);
        spacingRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        spacingRow.setGravity(Gravity.CENTER_VERTICAL);

        float[] spacingValues = new float[]{1.25f, 1.45f, 1.70f};
        String[] spacingLabels = new String[]{"تباعد متقارب", "تباعد متوازن", "تباعد مريح"};

        for (int i = 0; i < spacingValues.length; i++) {
            final float val = spacingValues[i];
            boolean active = Math.abs(lineSpacingMultiplier - val) < 0.08f;

            Button spBtn = new Button(this);
            spBtn.setTag("btn");
            spBtn.setAllCaps(false);
            spBtn.setText(spacingLabels[i]);
            spBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            spBtn.setTypeface(getAppTypeface(true));
            spBtn.setTextColor(active ? 0xFFFFFFFF : getPrimaryTextColor());
            spBtn.setBackground(active
                    ? createLuxuryButtonDrawable(getThemePrimaryColor(), 12)
                    : createInteractiveCardDrawable(isDarkMode ? 0xFF0F172A : 0xFFF8FAFC, getSubtleBorderColor(), 1, 12));
            applySpringTouch(spBtn);

            LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (i < spacingValues.length - 1) spLp.leftMargin = dp(6);
            spLp.bottomMargin = dp(3);

            spBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lineSpacingMultiplier = val;
                    prefs.edit().putFloat("line_spacing_mult", lineSpacingMultiplier).apply();
                    showSettingsScreen();
                }
            });
            spacingRow.addView(spBtn, spLp);
        }

        card.addView(spacingRow);
        return card;
    }

    private Button createMiniControlButton(String symbol, final Runnable onClick) {
        Button b = new Button(this);
        b.setTag("btn");
        b.setAllCaps(false);
        b.setText(symbol);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        b.setTypeface(getAppTypeface(true));
        b.setTextColor(0xFFFFFFFF);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 10));
        applySpringTouch(b);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClick != null) onClick.run();
            }
        });
        return b;
    }

    public void showFontPickerDialog() {
        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setPadding(dp(2), dp(4), dp(2), dp(8));
        sv.addView(list);

        final Dialog[] holder = new Dialog[1];

        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            boolean selected = (currentFontIndex == i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));

            int bg = selected
                    ? (isDarkMode ? 0xFF0F172A : getThemeLightAccentColor())
                    : (isDarkMode ? 0xFF0F172A : 0xFFF8FAFC);
            int border = selected ? getThemePrimaryColor() : getSubtleBorderColor();
            row.setBackground(createInteractiveCardDrawable(bg, border, selected ? 2 : 1, 14));
            applySpringTouch(row);

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rLp.bottomMargin = dp(7);

            TextView tv = new TextView(this);
            tv.setText((selected ? "✓  " : "") + FONT_NAMES[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            tv.setTextColor(getPrimaryTextColor());
            tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            row.addView(tv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentFontIndex = idx;
                    prefs.edit().putInt("font_index", currentFontIndex).apply();
                    if (holder[0] != null) holder[0].dismiss();
                    showSettingsScreen();
                }
            });

            list.addView(row, rLp);
        }

        holder[0] = showStyledDialog(
                "اختيار خط التطبيق والمستندات",
                "جميع الخطوط الـ 12 مدمجة داخل التطبيق وتعمل بدون إنترنت:",
                sv,
                null, 0, null,
                "إغلاق", null
        );
    }

    // ===== 3) قسم أدوات التحرير والتوقيع (خيار [ نصوص | رموز ] + زر «تخصيص أدوات التحرير») =====
    private View buildToolbarAndEditorSettingsCard() {
        LinearLayout card = createSettingsSectionCard(
                "شريط أدوات التحرير والتوقيع",
                "تحكم في نمط عرض شريط التحرير وترتيب الأدوات الـ 12 والتوقيع"
        );

        // خيار [ نصوص | رموز ] المعتمد حصرياً داخل شاشة الإعدادات
        TextView modeLabel = new TextView(this);
        modeLabel.setText("نمط عرض أزرار شريط التحرير:");
        modeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        modeLabel.setTypeface(getAppTypeface(true));
        modeLabel.setTextColor(getPrimaryTextColor());
        modeLabel.setGravity(Gravity.RIGHT);
        modeLabel.setPadding(0, 0, 0, dp(6));
        card.addView(modeLabel);

        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        modeRow.setClipChildren(false);
        modeRow.setClipToPadding(false);

        Button textAndIconsBtn = new Button(this);
        textAndIconsBtn.setTag("btn");
        textAndIconsBtn.setAllCaps(false);
        textAndIconsBtn.setText("نصوص ورموز");
        textAndIconsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        textAndIconsBtn.setTypeface(getAppTypeface(true));
        textAndIconsBtn.setTextColor(!toolbarIconsOnly ? 0xFFFFFFFF : getPrimaryTextColor());
        textAndIconsBtn.setBackground(!toolbarIconsOnly
                ? createLuxuryButtonDrawable(getThemePrimaryColor(), 12)
                : createInteractiveCardDrawable(isDarkMode ? 0xFF0F172A : 0xFFF8FAFC, getSubtleBorderColor(), 1, 12));
        applySpringTouch(textAndIconsBtn);

        LinearLayout.LayoutParams m1Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        m1Lp.leftMargin = dp(6);
        m1Lp.bottomMargin = dp(4);
        textAndIconsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarIconsOnly = false;
                prefs.edit().putBoolean("toolbar_icons_only", false).apply();
                showSettingsScreen();
            }
        });

        Button iconsOnlyBtn = new Button(this);
        iconsOnlyBtn.setTag("btn");
        iconsOnlyBtn.setAllCaps(false);
        iconsOnlyBtn.setText("رموز فقط");
        iconsOnlyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        iconsOnlyBtn.setTypeface(getAppTypeface(true));
        iconsOnlyBtn.setTextColor(toolbarIconsOnly ? 0xFFFFFFFF : getPrimaryTextColor());
        iconsOnlyBtn.setBackground(toolbarIconsOnly
                ? createLuxuryButtonDrawable(getThemePrimaryColor(), 12)
                : createInteractiveCardDrawable(isDarkMode ? 0xFF0F172A : 0xFFF8FAFC, getSubtleBorderColor(), 1, 12));
        applySpringTouch(iconsOnlyBtn);

        LinearLayout.LayoutParams m2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        m2Lp.rightMargin = dp(6);
        m2Lp.bottomMargin = dp(4);
        iconsOnlyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbarIconsOnly = true;
                prefs.edit().putBoolean("toolbar_icons_only", true).apply();
                showSettingsScreen();
            }
        });

        modeRow.addView(textAndIconsBtn, m1Lp);
        modeRow.addView(iconsOnlyBtn, m2Lp);

        LinearLayout.LayoutParams mrWrapLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        mrWrapLp.bottomMargin = dp(10);
        card.addView(modeRow, mrWrapLp);

        // زر «تخصيص أدوات التحرير» بالاسم المعتمد حرفياً
        Button customizeToolbarBtn = new Button(this);
        customizeToolbarBtn.setTag("btn");
        customizeToolbarBtn.setAllCaps(false);
        customizeToolbarBtn.setText("تخصيص أدوات التحرير");
        customizeToolbarBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        customizeToolbarBtn.setTypeface(getAppTypeface(true));
        customizeToolbarBtn.setTextColor(0xFFFFFFFF);
        customizeToolbarBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        applySpringTouch(customizeToolbarBtn);

        LinearLayout.LayoutParams ctLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        ctLp.bottomMargin = dp(10);
        customizeToolbarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomizeToolbarDialog(null);
            }
        });
        card.addView(customizeToolbarBtn, ctLp);

        // زر إعداد التوقيع الشخصي
        Button sigBtn = new Button(this);
        sigBtn.setTag("btn");
        sigBtn.setAllCaps(false);
        String sigStatus = (userSignature != null && !userSignature.trim().isEmpty())
                ? "تعديل التوقيع المحفوظ («" + userSignature.trim() + "»)"
                : "إعداد التوقيع الشخصي";
        sigBtn.setText(sigStatus);
        sigBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        sigBtn.setTypeface(getAppTypeface(true));
        sigBtn.setTextColor(isDarkMode ? 0xFFF8FAFC : getThemeDarkColor());
        sigBtn.setBackground(createInteractiveCardDrawable(
                isDarkMode ? 0xFF334155 : getThemeLightAccentColor(), getLuxuryAccentBorderColor(), 1, 14
        ));
        applySpringTouch(sigBtn);

        LinearLayout.LayoutParams sigLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        sigLp.bottomMargin = dp(10);
        sigBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignatureSetupDialog(false);
            }
        });
        card.addView(sigBtn, sigLp);

        // مفتاح المسافة البادئة للفقرات في التنسيق الآلي (على يمين النص)
        View indentSwitch = buildRightAlignedSwitchRow(
                "مسافة بادئة للفقرات عند التنسيق الآلي",
                "إضافة مسافة أنيقة في بداية كل فقرة عند الضغط على أداة «تنسيق آلي»",
                autoFormatIndent,
                new Runnable() {
                    @Override
                    public void run() {
                        autoFormatIndent = !autoFormatIndent;
                        prefs.edit().putBoolean("auto_format_indent", autoFormatIndent).apply();
                    }
                }
        );
        LinearLayout.LayoutParams indLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        indLp.bottomMargin = dp(8);
        card.addView(indentSwitch, indLp);

        // مفتاح الاهتزاز اللمسي الفاخر (على يمين النص)
        View hapticSwitch = buildRightAlignedSwitchRow(
                "الاستجابة اللمسية الناعمة (Haptic Feedback)",
                "نبضة لمسية خفيفة عند الضغط على الأزرار أو سحب الأدوات",
                hapticEnabled,
                new Runnable() {
                    @Override
                    public void run() {
                        hapticEnabled = !hapticEnabled;
                        prefs.edit().putBoolean("haptic_enabled", hapticEnabled).apply();
                    }
                }
        );
        card.addView(hapticSwitch);

        return card;
    }

    // ===== 4) قسم تخصيص الهوية ومجلد التخزين =====
    private View buildIdentityAndStorageSettingsCard() {
        LinearLayout card = createSettingsSectionCard(
                "تخصيص الواجهة ومجلد التخزين",
                "تخصيص اسم صاحب المفكرة في البطاقة العلوية أو تغيير مجلد الحفظ"
        );

        Button headerBtn = new Button(this);
        headerBtn.setTag("btn");
        headerBtn.setAllCaps(false);
        headerBtn.setText("تخصيص عنوان ووصف البطاقة العلوية");
        headerBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        headerBtn.setTypeface(getAppTypeface(true));
        headerBtn.setTextColor(0xFFFFFFFF);
        headerBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        applySpringTouch(headerBtn);

        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        hLp.bottomMargin = dp(10);
        headerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHeaderCustomizationDialog();
            }
        });
        card.addView(headerBtn, hLp);

        Button folderBtn = new Button(this);
        folderBtn.setTag("btn");
        folderBtn.setAllCaps(false);
        folderBtn.setText("تغيير مجلد حفظ الملفات النصية (SAF)");
        folderBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        folderBtn.setTypeface(getAppTypeface(true));
        folderBtn.setTextColor(isDarkMode ? 0xFFF8FAFC : getThemeDarkColor());
        folderBtn.setBackground(createInteractiveCardDrawable(
                isDarkMode ? 0xFF334155 : getThemeLightAccentColor(), getLuxuryAccentBorderColor(), 1, 14
        ));
        applySpringTouch(folderBtn);

        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        fLp.bottomMargin = dp(4);
        folderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFolder();
            }
        });
        card.addView(folderBtn, fLp);

        return card;
    }

    private LinearLayout createSettingsSectionCard(String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(16), dp(16), dp(16), dp(18));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getSubtleBorderColor(), 1, 20));
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.bottomMargin = dp(14);
        card.setLayoutParams(cardLp);

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        t1.setTypeface(getAppTypeface(true));
        t1.setTextColor(getPrimaryTextColor());
        t1.setGravity(Gravity.RIGHT);
        card.addView(t1);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView t2 = new TextView(this);
            t2.setText(subtitle);
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            t2.setTypeface(getAppTypeface(false));
            t2.setTextColor(getSecondaryTextColor());
            t2.setGravity(Gravity.RIGHT);
            t2.setPadding(0, dp(3), 0, dp(12));
            card.addView(t2);
        }

        return card;
    }

    // ===== نافذة إعداد التوقيع الشخصي =====
    public void showSignatureSetupDialog(final boolean insertAfterSave) {
        final EditText sigInput = createStyledEditText(
                userSignature,
                "مثال: بقلم / اسم الكاتب أو الباحث..."
        );

        showStyledDialog(
                "إعداد التوقيع الشخصي",
                "سيتم إدراج هذا التوقيع بضغطة واحدة عند استخدام أداة «التوقيع» في المحرر",
                sigInput,
                "حفظ التوقيع",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        userSignature = sigInput.getText().toString().trim();
                        prefs.edit().putString("user_signature", userSignature).apply();
                        if (insertAfterSave && isInDocumentScreen && isEditMode && noteEditText != null && !userSignature.isEmpty()) {
                            int start = Math.max(0, noteEditText.getSelectionStart());
                            int end = Math.max(0, noteEditText.getSelectionEnd());
                            noteEditText.getText().replace(
                                    Math.min(start, end),
                                    Math.max(start, end),
                                    "\n" + userSignature + "\n"
                            );
                        } else if (isInSettingsScreen) {
                            showSettingsScreen();
                        }
                        showToast("تم حفظ التوقيع بنجاح");
                    }
                },
                "إلغاء",
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                    }
                }
        );
        showKeyboard(sigInput);
    }

    // ===== نافذة تخصيص عنوان ووصف البطاقة العلوية في الشاشة الرئيسية =====
    public void showHeaderCustomizationDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        final EditText titleEt = createStyledEditText(customHeaderTitle, DEFAULT_HERO_TITLE);
        titleEt.setSingleLine(true);

        final EditText subEt = createStyledEditText(customHeaderSubtitle, DEFAULT_HERO_SUBTITLE);
        subEt.setSingleLine(true);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(10);

        box.addView(titleEt);
        box.addView(subEt, subLp);

        showStyledDialog(
                "تخصيص البطاقة الملكية العلوية",
                "يمكنك كتابة اسمك أو لقبك ليظهر في أعلى الشاشة الرئيسية، أو استعادة عنوان «" + BRAND_NAME + "» الافتراضي",
                box,
                "حفظ التخصيص",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        String t = titleEt.getText().toString().trim();
                        String s = subEt.getText().toString().trim();
                        customHeaderTitle = t.isEmpty() ? DEFAULT_HERO_TITLE : t;
                        customHeaderSubtitle = s.isEmpty() ? DEFAULT_HERO_SUBTITLE : s;
                        prefs.edit()
                                .putString("custom_header_title", customHeaderTitle)
                                .putString("custom_header_subtitle", customHeaderSubtitle)
                                .apply();
                        showToast("تم تحديث عنوان البطاقة العلوية");
                    }
                },
                "استعادة الافتراضي",
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        customHeaderTitle = DEFAULT_HERO_TITLE;
                        customHeaderSubtitle = DEFAULT_HERO_SUBTITLE;
                        prefs.edit()
                                .putString("custom_header_title", customHeaderTitle)
                                .putString("custom_header_subtitle", customHeaderSubtitle)
                                .apply();
                        showToast("تمت استعادة العنوان الافتراضي");
                    }
                }
        );
    }
}
// ===== END PART 05/05 =====
