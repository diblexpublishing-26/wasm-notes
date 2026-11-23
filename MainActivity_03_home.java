// ===== MAZWD PART 03/05: HOME — v2.7.1 =====

    // ===== بناء الشاشة الرئيسية الفاخرة (3 مناطق: علوية ثابتة + وسطى متحركة + سفلية ثابتة) =====
    public void showHomeScreen() {
        isInDocumentScreen = false;
        isInSettingsScreen = false;
        updateSystemBars();

        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getScreenBgColor());

        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mainRoot.setClipChildren(false);
        mainRoot.setClipToPadding(false);
        mainRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 1) البطاقة العلوية الثابتة (Hero Header Card)
        View heroCard = buildHeroHeaderCard();
        mainRoot.addView(heroCard);

        // 2) المنطقة الوسطى القابلة للتمرير (تضم العنوان + شريط البحث الموحد + قائمة الملفات)
        homeScrollView = new ScrollView(this);
        homeScrollView.setFillViewport(true);
        homeScrollView.setClipChildren(false);
        homeScrollView.setClipToPadding(false);
        homeScrollView.setVerticalScrollBarEnabled(false);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        mainRoot.addView(homeScrollView, scrollLp);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scrollContent.setClipChildren(false);
        scrollContent.setClipToPadding(false);
        scrollContent.setPadding(dp(16), dp(14), dp(16), dp(20));
        homeScrollView.addView(scrollContent);

        // سطر عنوان القسم (ملفاتي النصية يميناً + شارة TXT يساراً)
        LinearLayout sectionHeaderRow = new LinearLayout(this);
        sectionHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeaderRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sectionHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        sectionHeaderRow.setPadding(dp(4), dp(2), dp(4), dp(10));

        TextView sectionTitleTv = new TextView(this);
        sectionTitleTv.setText("ملفاتي النصية");
        sectionTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        sectionTitleTv.setTypeface(getAppTypeface(true));
        sectionTitleTv.setTextColor(getPrimaryTextColor());
        sectionTitleTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        sectionHeaderRow.addView(sectionTitleTv, stLp);

        TextView txtBadge = new TextView(this);
        txtBadge.setTag("badge");
        txtBadge.setText("TXT • UTF-8");
        txtBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        txtBadge.setTypeface(getAppTypeface(true));
        txtBadge.setTextColor(isDarkMode ? getThemeLightAccentColor() : getThemePrimaryColor());
        int badgeBg = isDarkMode ? 0xFF1E293B : getThemeLightAccentColor();
        txtBadge.setBackground(createRoundedDrawable(badgeBg, getLuxuryAccentBorderColor(), 1, 10));
        txtBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        sectionHeaderRow.addView(txtBadge);

        scrollContent.addView(sectionHeaderRow);

        // شريط البحث الموحد المتزامن (يتحرك مع القائمة للأعلى لتوفير أكبر مساحة للملفات)
        View searchBarView = buildUnifiedSearchBar();
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchLp.bottomMargin = dp(14);
        scrollContent.addView(searchBarView, searchLp);

        // حاوية بطاقات الملفات
        homeFilesContainer = new LinearLayout(this);
        homeFilesContainer.setOrientation(LinearLayout.VERTICAL);
        homeFilesContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        homeFilesContainer.setClipChildren(false);
        homeFilesContainer.setClipToPadding(false);
        scrollContent.addView(homeFilesContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 3) الشريط السفلي الثابت (Bottom Bar) بدون أي اقتصاص للظلال أو الحواف
        View bottomBar = buildHomeBottomBar();
        mainRoot.addView(bottomBar);

        rootContainer.addView(mainRoot);
        refreshHomeFilesList();
        applyFontToViewTree(rootContainer);

        // استعادة موضع التمرير المحفوظ بدقة
        if (savedHomeScrollY > 0 && homeScrollView != null) {
            final int targetY = savedHomeScrollY;
            homeScrollView.post(new Runnable() {
                @Override
                public void run() {
                    if (homeScrollView != null) {
                        homeScrollView.scrollTo(0, targetY);
                    }
                }
            });
        }
    }

    // ===== 1) بناء البطاقة الملكية العلوية (Hero Header Card) =====
    private View buildHeroHeaderCard() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setClipChildren(false);
        hero.setClipToPadding(false);
        hero.setPadding(dp(20), dp(22), dp(20), dp(20));
        hero.setBackground(createHeroHeaderDrawable());
        hero.setElevation(dp(6));

        String titleStr = (customHeaderTitle != null && !customHeaderTitle.trim().isEmpty())
                ? customHeaderTitle.trim() : DEFAULT_HERO_TITLE;
        String subStr = (customHeaderSubtitle != null && !customHeaderSubtitle.trim().isEmpty())
                ? customHeaderSubtitle.trim() : DEFAULT_HERO_SUBTITLE;

        TextView mainTitle = new TextView(this);
        mainTitle.setText(titleStr);
        mainTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
        mainTitle.setTypeface(getAppTypeface(true));
        mainTitle.setTextColor(0xFFFFFFFF);
        mainTitle.setGravity(Gravity.CENTER);
        mainTitle.setIncludeFontPadding(true);
        hero.addView(mainTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView subTitle = new TextView(this);
        subTitle.setText(subStr);
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subTitle.setTypeface(getAppTypeface(false));
        subTitle.setTextColor(0xE6FFFFFF);
        subTitle.setGravity(Gravity.CENTER);
        subTitle.setIncludeFontPadding(true);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(16);
        hero.addView(subTitle, subLp);

        // زر ＋ ملف جديد بارتفاع 46dp
        Button newFileBtn = new Button(this);
        newFileBtn.setTag("btn");
        newFileBtn.setAllCaps(false);
        newFileBtn.setText("＋  ملف جديد");
        newFileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        newFileBtn.setTypeface(getAppTypeface(true));
        newFileBtn.setTextColor(getThemeDarkColor());
        newFileBtn.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.RECTANGLE);
        btnBg.setColor(0xFFFFFFFF);
        btnBg.setCornerRadius(dp(14));
        RippleDrawable btnRipple = new RippleDrawable(
                ColorStateList.valueOf(0x33000000), btnBg, null
        );
        newFileBtn.setBackground(btnRipple);
        newFileBtn.setElevation(dp(3));
        applySpringTouch(newFileBtn);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        btnLp.topMargin = dp(2);
        btnLp.bottomMargin = dp(3);
        newFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateNewNoteDialog();
            }
        });

        hero.addView(newFileBtn, btnLp);
        return hero;
    }

    // ===== 2) بناء شريط البحث العربي الذكي الموحد =====
    private View buildUnifiedSearchBar() {
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(14), dp(4), dp(10), dp(4));
        searchBox.setBackground(createRoundedDrawable(getCardBgColor(), getSubtleBorderColor(), 1, 16));
        searchBox.setElevation(dp(2));

        TextView searchIcon = new TextView(this);
        searchIcon.setText("🔍");
        searchIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchIcon.setPadding(dp(2), 0, dp(8), 0);
        searchBox.addView(searchIcon);

        homeSearchInput = new EditText(this);
        homeSearchInput.setHint("ابحث في العناوين أو داخل النصوص (بحث ذكي)...");
        homeSearchInput.setHintTextColor(getSecondaryTextColor());
        homeSearchInput.setTextColor(getPrimaryTextColor());
        homeSearchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        homeSearchInput.setTypeface(getAppTypeface(false));
        homeSearchInput.setSingleLine(true);
        homeSearchInput.setBackgroundColor(Color.TRANSPARENT);
        homeSearchInput.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        homeSearchInput.setTextDirection(View.TEXT_DIRECTION_RTL);
        homeSearchInput.setPadding(dp(6), dp(10), dp(6), dp(10));
        homeSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        if (currentHomeSearchQuery != null && !currentHomeSearchQuery.isEmpty()) {
            homeSearchInput.setText(currentHomeSearchQuery);
            homeSearchInput.setSelection(currentHomeSearchQuery.length());
        }

        final TextView clearBtn = new TextView(this);
        clearBtn.setTag("btn");
        clearBtn.setText("✕");
        clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        clearBtn.setTypeface(getAppTypeface(true));
        clearBtn.setTextColor(getSecondaryTextColor());
        clearBtn.setGravity(Gravity.CENTER);
        clearBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        clearBtn.setVisibility(
                (currentHomeSearchQuery != null && !currentHomeSearchQuery.isEmpty())
                        ? View.VISIBLE : View.GONE
        );

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performHaptic(v);
                currentHomeSearchQuery = "";
                homeSearchInput.setText("");
                clearBtn.setVisibility(View.GONE);
                hideKeyboard();
                renderFilteredNotesCards();
            }
        });

        homeSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentHomeSearchQuery = s != null ? s.toString() : "";
                clearBtn.setVisibility(currentHomeSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                renderFilteredNotesCards();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        searchBox.addView(homeSearchInput, inputLp);
        searchBox.addView(clearBtn);

        return searchBox;
    }

    // ===== 3) بناء الشريط السفلي الثابت (الإعدادات + نسخ احتياطي ZIP) =====
    private View buildHomeBottomBar() {
        LinearLayout bottomWrapper = new LinearLayout(this);
        bottomWrapper.setOrientation(LinearLayout.VERTICAL);
        bottomWrapper.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bottomWrapper.setClipChildren(false);
        bottomWrapper.setClipToPadding(false);
        bottomWrapper.setBackgroundColor(getCardBgColor());
        bottomWrapper.setElevation(dp(12));

        View topDivider = new View(this);
        topDivider.setBackgroundColor(getSubtleBorderColor());
        bottomWrapper.addView(topDivider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        // حشوة سفلية كافية لضمان ظهور الحواف الدائرية والظلال بنسبة 100% دون أي اقتصاص
        row.setPadding(dp(16), dp(10), dp(16), dp(14));

        Button settingsBtn = new Button(this);
        settingsBtn.setTag("btn");
        settingsBtn.setAllCaps(false);
        settingsBtn.setText("⚙  الإعدادات");
        settingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        settingsBtn.setTypeface(getAppTypeface(true));
        settingsBtn.setTextColor(0xFFFFFFFF);
        settingsBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        settingsBtn.setElevation(dp(3));
        applySpringTouch(settingsBtn);

        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        setLp.leftMargin = dp(6);
        setLp.topMargin = dp(2);
        setLp.bottomMargin = dp(4);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (homeScrollView != null) {
                    savedHomeScrollY = homeScrollView.getScrollY();
                }
                showSettingsScreen();
            }
        });

        Button zipBtn = new Button(this);
        zipBtn.setTag("btn");
        zipBtn.setAllCaps(false);
        zipBtn.setText("نسخ احتياطي (ZIP)");
        zipBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        zipBtn.setTypeface(getAppTypeface(true));
        zipBtn.setTextColor(isDarkMode ? 0xFFF8FAFC : getThemeDarkColor());
        int zipBg = isDarkMode ? 0xFF334155 : getThemeLightAccentColor();
        zipBtn.setBackground(createInteractiveCardDrawable(zipBg, getLuxuryAccentBorderColor(), 1, 14));
        zipBtn.setElevation(dp(2));
        applySpringTouch(zipBtn);

        LinearLayout.LayoutParams zipLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        zipLp.rightMargin = dp(6);
        zipLp.topMargin = dp(2);
        zipLp.bottomMargin = dp(4);
        zipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFullZipBackup();
            }
        });

        row.addView(settingsBtn, setLp);
        row.addView(zipBtn, zipLp);
        bottomWrapper.addView(row);

        return bottomWrapper;
    }

    // ===== تحديث وعرض بطاقات الملفات مع التظليل الذكي =====
    public void refreshHomeFilesList() {
        if (homeFilesContainer == null) return;
        loadAllNotesFromFolder();
        renderFilteredNotesCards();
    }

    public void renderFilteredNotesCards() {
        if (homeFilesContainer == null) return;
        homeFilesContainer.removeAllViews();

        if (!hasValidFolderPermission()) {
            homeFilesContainer.addView(buildFolderSetupCard());
            applyFontToViewTree(homeFilesContainer);
            return;
        }

        String query = currentHomeSearchQuery != null ? currentHomeSearchQuery.trim() : "";
        List<NoteItem> matchedNotes = new ArrayList<>();

        if (query.isEmpty()) {
            matchedNotes.addAll(cachedNotesList);
        } else {
            for (NoteItem item : cachedNotesList) {
                List<int[]> nameMatches = findSmartMatchRanges(item.displayName, query);
                List<int[]> contentMatches = findSmartMatchRanges(item.content, query);
                if (!nameMatches.isEmpty() || !contentMatches.isEmpty()) {
                    matchedNotes.add(item);
                }
            }
        }

        if (matchedNotes.isEmpty()) {
            homeFilesContainer.addView(buildEmptyStateCard(!query.isEmpty()));
            applyFontToViewTree(homeFilesContainer);
            return;
        }

        for (NoteItem item : matchedNotes) {
            View card = createFileCardView(item, query);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardLp.bottomMargin = dp(11);
            homeFilesContainer.addView(card, cardLp);
        }

        applyFontToViewTree(homeFilesContainer);
    }

    private View buildFolderSetupCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(22), dp(26), dp(22), dp(26));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getLuxuryAccentBorderColor(), 1, 20));
        card.setElevation(dp(3));

        TextView iconTv = new TextView(this);
        iconTv.setText("📂");
        iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 38);
        iconTv.setGravity(Gravity.CENTER);
        card.addView(iconTv);

        TextView titleTv = new TextView(this);
        titleTv.setText("ربط مجلد حفظ الملفات النصية");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleTv.setTypeface(getAppTypeface(true));
        titleTv.setTextColor(getPrimaryTextColor());
        titleTv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        tLp.topMargin = dp(10);
        card.addView(titleTv, tLp);

        TextView descTv = new TextView(this);
        descTv.setText("يعمل تطبيق «" + BRAND_NAME + "» بنظام التخزين الحقيقي المباشر؛ حيث تُحفظ جميع ملاحظاتك كملفات (.txt) مستقلة في هاتفك مع خزنة نسخ آمنة تلقائية.");
        descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        descTv.setTypeface(getAppTypeface(false));
        descTv.setTextColor(getSecondaryTextColor());
        descTv.setGravity(Gravity.CENTER);
        descTv.setLineSpacing(0f, 1.3f);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dLp.topMargin = dp(8);
        dLp.bottomMargin = dp(18);
        card.addView(descTv, dLp);

        Button chooseBtn = new Button(this);
        chooseBtn.setTag("btn");
        chooseBtn.setAllCaps(false);
        chooseBtn.setText("اختيار مجلد الملفات الآن");
        chooseBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        chooseBtn.setTypeface(getAppTypeface(true));
        chooseBtn.setTextColor(0xFFFFFFFF);
        chooseBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        chooseBtn.setElevation(dp(3));
        applySpringTouch(chooseBtn);

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
        );
        bLp.bottomMargin = dp(4);
        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFolder();
            }
        });
        card.addView(chooseBtn, bLp);

        return card;
    }

    private View buildEmptyStateCard(boolean isSearchEmpty) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(20), dp(32), dp(20), dp(32));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getSubtleBorderColor(), 1, 18));

        TextView iconTv = new TextView(this);
        iconTv.setText(isSearchEmpty ? "🔍" : "📝");
        iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        iconTv.setGravity(Gravity.CENTER);
        card.addView(iconTv);

        TextView msgTv = new TextView(this);
        msgTv.setText(isSearchEmpty
                ? "لا توجد نتائج مطابقة لعبارة البحث"
                : "لا توجد ملفات نصية بعد في هذا المجلد");
        msgTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        msgTv.setTypeface(getAppTypeface(true));
        msgTv.setTextColor(getPrimaryTextColor());
        msgTv.setGravity(Gravity.CENTER);
        msgTv.setPadding(0, dp(10), 0, dp(4));
        card.addView(msgTv);

        TextView subTv = new TextView(this);
        subTv.setText(isSearchEmpty
                ? "جرّب البحث بكلمة أخرى؛ المحرك يتجاهل التشكيل والهمزات تلقائياً"
                : "اضغط على زر «＋ ملف جديد» في الأعلى للبدء بتدوين أفكارك");
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        subTv.setTypeface(getAppTypeface(false));
        subTv.setTextColor(getSecondaryTextColor());
        subTv.setGravity(Gravity.CENTER);
        card.addView(subTv);

        return card;
    }

    // ===== بناء بطاقة الملف الفردية (العنوان + شارة التثبيت + التاريخ + المقتطف المظلل + زر ⋮) =====
    public View createFileCardView(final NoteItem item, String searchQuery) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(10), dp(14));

        int borderCol = item.isPinned ? getLuxuryAccentBorderColor() : getSubtleBorderColor();
        int strokeW = item.isPinned ? 2 : 1;
        card.setBackground(createInteractiveCardDrawable(getCardBgColor(), borderCol, strokeW, 18));
        card.setElevation(dp(2));
        applySpringTouch(card);

        // العمود الأيمن: اسم الملف + التاريخ + المقتطف المظلل
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );

        // سطر العنوان وشارة التثبيت 📌
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        if (item.isPinned) {
            TextView pinBadge = new TextView(this);
            pinBadge.setText("📌");
            pinBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            pinBadge.setPadding(0, 0, dp(6), 0);
            titleRow.addView(pinBadge);
        }

        TextView nameTv = new TextView(this);
        if (searchQuery != null && !searchQuery.isEmpty()) {
            nameTv.setText(buildHighlightedSpannable(item.displayName, searchQuery));
        } else {
            nameTv.setText(item.displayName);
        }
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        nameTv.setTypeface(getAppTypeface(true));
        nameTv.setTextColor(getPrimaryTextColor());
        nameTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        nameTv.setMaxLines(1);
        nameTv.setEllipsize(TextUtils.TruncateAt.END);
        titleRow.addView(nameTv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        infoCol.addView(titleRow);

        // سطر التاريخ والوقت
        TextView dateTv = new TextView(this);
        dateTv.setText(formatTimestamp(item.lastModified));
        dateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        dateTv.setTypeface(getAppTypeface(false));
        dateTv.setTextColor(getSecondaryTextColor());
        dateTv.setGravity(Gravity.RIGHT);
        dateTv.setPadding(0, dp(3), 0, 0);
        infoCol.addView(dateTv);

        // سطر المقتطف المظلل عند البحث (أو سطر معاينة أنيق عند عدم البحث)
        CharSequence snippetSequence = buildSmartSnippet(item.content, searchQuery);
        if (snippetSequence != null && snippetSequence.length() > 0) {
            TextView snippetTv = new TextView(this);
            snippetTv.setText(snippetSequence);
            snippetTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            snippetTv.setTypeface(getAppTypeface(false));
            snippetTv.setTextColor(isDarkMode ? 0xFFCBD5E1 : 0xFF475569);
            snippetTv.setGravity(Gravity.RIGHT);
            snippetTv.setMaxLines(2);
            snippetTv.setEllipsize(TextUtils.TruncateAt.END);
            snippetTv.setLineSpacing(0f, 1.2f);
            snippetTv.setPadding(0, dp(6), 0, 0);
            infoCol.addView(snippetTv);
        }

        card.addView(infoCol, infoLp);

        // الزر الأيسر: زر الثلاث نقاط (⋮)
        TextView moreBtn = new TextView(this);
        moreBtn.setTag("btn");
        moreBtn.setText("⋮");
        moreBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        moreBtn.setTypeface(getAppTypeface(true));
        moreBtn.setTextColor(getSecondaryTextColor());
        moreBtn.setGravity(Gravity.CENTER);
        moreBtn.setBackground(createInteractiveCardDrawable(
                Color.TRANSPARENT, 0, 0, 12
        ));
        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        moreLp.rightMargin = dp(6);

        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performHaptic(v);
                showFileOptionsMenu(item);
            }
        });

        card.addView(moreBtn, moreLp);

        // عند النقر على البطاقة: حفظ موضع التمرير وفتح المستند في وضع القراءة
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (homeScrollView != null) {
                    savedHomeScrollY = homeScrollView.getScrollY();
                }
                hideKeyboard();
                isEditMode = false;
                prevScrollFraction = 0f;
                String freshContent = readTextFromUri(item.uri);
                openNoteDocument(item.uri, item.fileName, freshContent);
            }
        });

        return card;
    }

    private CharSequence buildSmartSnippet(String content, String query) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String singleLine = content.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (query == null || query.trim().isEmpty()) {
            if (singleLine.length() > 110) {
                return singleLine.substring(0, 110) + "...";
            }
            return singleLine;
        }

        List<int[]> matches = findSmartMatchRanges(singleLine, query);
        if (matches.isEmpty()) {
            if (singleLine.length() > 110) {
                return singleLine.substring(0, 110) + "...";
            }
            return singleLine;
        }

        int matchStart = matches.get(0)[0];
        int matchEnd = matches.get(0)[1];
        int snippetStart = Math.max(0, matchStart - 35);
        int snippetEnd = Math.min(singleLine.length(), matchEnd + 55);

        String prefix = snippetStart > 0 ? "..." : "";
        String suffix = snippetEnd < singleLine.length() ? "..." : "";
        String extracted = prefix + singleLine.substring(snippetStart, snippetEnd) + suffix;

        return buildHighlightedSpannable(extracted, query);
    }
// ===== END PART 03/05 =====
