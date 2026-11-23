// ===== MAZWD PART 04/05: DOCUMENT — v2.7.1 =====

    // ===== فتح المستند وإدارة التبديل السلس بين القراءة والتحرير =====
    public void openNoteDocument(Uri uri, String fileName, String content) {
        if (uri == null) return;
        isInDocumentScreen = true;
        isInSettingsScreen = false;
        currentNoteUri = uri;
        currentNoteName = fileName != null ? fileName : "ملاحظة.txt";
        currentNoteContent = content != null ? content : "";
        isDirty = false;

        undoStack.clear();
        redoStack.clear();
        undoStack.add(currentNoteContent);

        renderDocumentScreen();
    }

    public void captureCurrentScrollFraction() {
        ScrollView activeSv = isEditMode ? editScrollView : readScrollView;
        if (activeSv != null && activeSv.getChildCount() > 0) {
            int contentH = activeSv.getChildAt(0).getHeight();
            int viewH = activeSv.getHeight();
            int maxScroll = Math.max(0, contentH - viewH);
            if (maxScroll > 0) {
                prevScrollFraction = Math.max(0f, Math.min(1f, (float) activeSv.getScrollY() / (float) maxScroll));
            } else {
                prevScrollFraction = 0f;
            }
        }
    }

    public void restoreScrollFraction(final ScrollView targetSv) {
        if (targetSv == null) return;
        targetSv.post(new Runnable() {
            @Override
            public void run() {
                if (targetSv.getChildCount() == 0) return;
                int contentH = targetSv.getChildAt(0).getHeight();
                int viewH = targetSv.getHeight();
                int maxScroll = Math.max(0, contentH - viewH);
                if (maxScroll > 0 && prevScrollFraction > 0f) {
                    int targetY = Math.round(prevScrollFraction * maxScroll);
                    targetSv.scrollTo(0, targetY);
                }
                updateFastScrollbarPosition(targetSv);
            }
        });
    }

    // ===== بناء شاشة المستند الكاملة (وضعا القراءة والتحرير) =====
    public void renderDocumentScreen() {
        updateSystemBars();
        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getScreenBgColor());

        LinearLayout docRoot = new LinearLayout(this);
        docRoot.setOrientation(LinearLayout.VERTICAL);
        docRoot.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        docRoot.setClipChildren(false);
        docRoot.setClipToPadding(false);
        docRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 1) الشريط العلوي للمستند (يميناً: رجوع | وسطاً: العنوان | يساراً: كبسولة التكبير)
        docTopBar = buildDocumentTopBar();
        docRoot.addView(docTopBar);

        // 2) منطقة الورقة الوسطى + شريط التمرير السريع الجانبي + زر القفز العائم
        FrameLayout paperStage = new FrameLayout(this);
        paperStage.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        paperStage.setClipChildren(false);
        paperStage.setClipToPadding(false);
        paperStage.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        );
        docRoot.addView(paperStage, stageLp);

        FrameLayout paperCard = new FrameLayout(this);
        paperCard.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        paperCard.setBackground(createRoundedDrawable(getPaperBgColor(), getSubtleBorderColor(), 1, 20));
        paperCard.setElevation(dp(3));
        paperStage.addView(paperCard, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (isEditMode) {
            buildEditModePaperContent(paperCard);
        } else {
            buildReadModePaperContent(paperCard);
        }

        // إضافة شريط التمرير السريع الجانبي (أقصى يسار الورقة LAYOUT_DIRECTION_LTR)
        View fastScrollerOverlay = buildFastScrollerOverlay();
        paperCard.addView(fastScrollerOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // إضافة زر الانتقال السريع العائم (أسفل يسار الورقة بأسلوب TextPad)
        View floatingJumpBtn = buildFloatingJumpButton();
        FrameLayout.LayoutParams jumpLp = new FrameLayout.LayoutParams(dp(44), dp(44));
        jumpLp.gravity = Gravity.BOTTOM | Gravity.LEFT;
        jumpLp.leftMargin = dp(14);
        jumpLp.bottomMargin = dp(14);
        paperCard.addView(floatingJumpBtn, jumpLp);

        // 3) المنطقة السفلية (شريط الإحصائيات + شريط أزرار القراءة أو شريط أدوات التحرير الـ 12)
        docBottomArea = new LinearLayout(this);
        docBottomArea.setOrientation(LinearLayout.VERTICAL);
        docBottomArea.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        docBottomArea.setClipChildren(false);
        docBottomArea.setClipToPadding(false);
        docBottomArea.setBackgroundColor(getCardBgColor());
        docBottomArea.setElevation(dp(10));

        View statsBar = buildStatsBarView();
        docBottomArea.addView(statsBar);

        if (isEditMode) {
            View editToolbar = buildEditToolbarView();
            docBottomArea.addView(editToolbar);
        } else {
            View readActionsBar = buildReadModeBottomBar();
            docBottomArea.addView(readActionsBar);
        }

        docRoot.addView(docBottomArea);
        rootContainer.addView(docRoot);

        updateStatsBar(currentNoteContent);
        applyFontToViewTree(rootContainer);
        applyDocumentTextSizing();

        ScrollView activeSv = isEditMode ? editScrollView : readScrollView;
        restoreScrollFraction(activeSv);
    }

    // ===== الشريط العلوي للمستند + كبسولة التكبير [ ＋ | 100% | － ] =====
    private LinearLayout buildDocumentTopBar() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        topBar.setBackgroundColor(getThemeDarkColor());
        topBar.setElevation(dp(6));

        // يميناً: زر → رجوع
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
                onBackPressed();
            }
        });
        topBar.addView(backBtn);

        // وسطاً: عنوان الملف (الضغط عليه يفتح نافذة إعادة تسمية الملف)
        docTitleTextView = new TextView(this);
        docTitleTextView.setText(stripTxtExtension(currentNoteName));
        docTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        docTitleTextView.setTypeface(getAppTypeface(true));
        docTitleTextView.setTextColor(0xFFFFFFFF);
        docTitleTextView.setGravity(Gravity.CENTER);
        docTitleTextView.setSingleLine(true);
        docTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
        docTitleTextView.setPadding(dp(10), dp(4), dp(10), dp(4));
        applySpringTouch(docTitleTextView);
        docTitleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NoteItem temp = new NoteItem();
                temp.uri = currentNoteUri;
                temp.fileName = currentNoteName;
                temp.displayName = stripTxtExtension(currentNoteName);
                showRenameFileDialog(temp, null);
            }
        });

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        topBar.addView(docTitleTextView, titleLp);

        // يساراً: كبسولة التكبير والتصغير الموحدة [ ＋ | 100% | － ]
        LinearLayout zoomCapsule = new LinearLayout(this);
        zoomCapsule.setOrientation(LinearLayout.HORIZONTAL);
        zoomCapsule.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        zoomCapsule.setGravity(Gravity.CENTER_VERTICAL);
        zoomCapsule.setBackground(createRoundedDrawable(0x26FFFFFF, 0x40FFFFFF, 1, 12));
        zoomCapsule.setPadding(dp(2), dp(2), dp(2), dp(2));

        TextView zoomInBtn = new TextView(this);
        zoomInBtn.setTag("btn");
        zoomInBtn.setText("＋");
        zoomInBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        zoomInBtn.setTypeface(getAppTypeface(true));
        zoomInBtn.setTextColor(0xFFFFFFFF);
        zoomInBtn.setGravity(Gravity.CENTER);
        zoomInBtn.setPadding(dp(9), dp(4), dp(9), dp(4));
        applySpringTouch(zoomInBtn);
        zoomInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDocumentZoom(10);
            }
        });

        View div1 = new View(this);
        div1.setBackgroundColor(0x40FFFFFF);
        LinearLayout.LayoutParams divLp1 = new LinearLayout.LayoutParams(dp(1), dp(18));

        zoomPercentBtn = new TextView(this);
        zoomPercentBtn.setTag("btn");
        zoomPercentBtn.setText(docZoomPercent + "%");
        zoomPercentBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        zoomPercentBtn.setTypeface(getAppTypeface(true));
        zoomPercentBtn.setTextColor(0xFFFFFFFF);
        zoomPercentBtn.setGravity(Gravity.CENTER);
        zoomPercentBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        applySpringTouch(zoomPercentBtn);
        zoomPercentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetZoomDialog();
            }
        });

        View div2 = new View(this);
        div2.setBackgroundColor(0x40FFFFFF);
        LinearLayout.LayoutParams divLp2 = new LinearLayout.LayoutParams(dp(1), dp(18));

        TextView zoomOutBtn = new TextView(this);
        zoomOutBtn.setTag("btn");
        zoomOutBtn.setText("－");
        zoomOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        zoomOutBtn.setTypeface(getAppTypeface(true));
        zoomOutBtn.setTextColor(0xFFFFFFFF);
        zoomOutBtn.setGravity(Gravity.CENTER);
        zoomOutBtn.setPadding(dp(9), dp(4), dp(9), dp(4));
        applySpringTouch(zoomOutBtn);
        zoomOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeDocumentZoom(-10);
            }
        });

        zoomCapsule.addView(zoomInBtn);
        zoomCapsule.addView(div1, divLp1);
        zoomCapsule.addView(zoomPercentBtn);
        zoomCapsule.addView(div2, divLp2);
        zoomCapsule.addView(zoomOutBtn);

        topBar.addView(zoomCapsule);
        return topBar;
    }

    public void changeDocumentZoom(int deltaPercent) {
        int next = Math.max(50, Math.min(250, docZoomPercent + deltaPercent));
        if (next == docZoomPercent) return;
        captureCurrentScrollFraction();
        docZoomPercent = next;
        prefs.edit().putInt("doc_zoom_percent", docZoomPercent).apply();
        if (zoomPercentBtn != null) {
            zoomPercentBtn.setText(docZoomPercent + "%");
        }
        applyDocumentTextSizing();
        ScrollView activeSv = isEditMode ? editScrollView : readScrollView;
        restoreScrollFraction(activeSv);
    }

    public void showResetZoomDialog() {
        showStyledDialog(
                "حجم عرض المستند (" + docZoomPercent + "%)",
                "هل ترغب في إعادة نسبة التكبير والتصغير إلى الحجم القياسي الافتراضي (100%)؟",
                null,
                "إعادة إلى 100%",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        captureCurrentScrollFraction();
                        docZoomPercent = 100;
                        prefs.edit().putInt("doc_zoom_percent", 100).apply();
                        if (zoomPercentBtn != null) {
                            zoomPercentBtn.setText("100%");
                        }
                        applyDocumentTextSizing();
                        ScrollView activeSv = isEditMode ? editScrollView : readScrollView;
                        restoreScrollFraction(activeSv);
                    }
                },
                "إلغاء",
                null
        );
    }

    public void applyDocumentTextSizing() {
        float effectiveSizeSp = baseFontSizeSp * (docZoomPercent / 100f);
        if (readTextView != null) {
            readTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, effectiveSizeSp);
            readTextView.setLineSpacing(0f, lineSpacingMultiplier);
            readTextView.setTypeface(getAppTypeface(false));
            readTextView.setIncludeFontPadding(false);
        }
        if (noteEditText != null) {
            noteEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, effectiveSizeSp);
            noteEditText.setLineSpacing(0f, lineSpacingMultiplier);
            noteEditText.setTypeface(getAppTypeface(false));
        }
    }

    // ===== محتوى الورقة في وضعي القراءة والتحرير =====
    private void buildReadModePaperContent(FrameLayout paperCard) {
        readScrollView = new ScrollView(this);
        readScrollView.setFillViewport(true);
        readScrollView.setVerticalScrollBarEnabled(false);
        readScrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);

        readTextView = new TextView(this);
        // هامش أيسر 26dp لترك مساحة مريحة لشريط التمرير السريع الجانبي
        readTextView.setPadding(dp(26), dp(20), dp(20), dp(72));
        readTextView.setTextColor(getPrimaryTextColor());
        readTextView.setGravity(Gravity.RIGHT | Gravity.TOP);
        readTextView.setTextDirection(View.TEXT_DIRECTION_RTL);
        readTextView.setTextIsSelectable(true);

        if (currentHomeSearchQuery != null && !currentHomeSearchQuery.trim().isEmpty()) {
            readTextView.setText(buildHighlightedSpannable(currentNoteContent, currentHomeSearchQuery));
        } else {
            readTextView.setText(currentNoteContent);
        }

        readScrollView.addView(readTextView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        attachScrollListenerForFastScrollbar(readScrollView);
        paperCard.addView(readScrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private void buildEditModePaperContent(FrameLayout paperCard) {
        editScrollView = new ScrollView(this);
        editScrollView.setFillViewport(true);
        editScrollView.setVerticalScrollBarEnabled(false);
        editScrollView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);

        noteEditText = new EditText(this);
        noteEditText.setBackgroundColor(Color.TRANSPARENT);
        noteEditText.setPadding(dp(26), dp(20), dp(20), dp(90));
        noteEditText.setTextColor(getPrimaryTextColor());
        noteEditText.setHint("ابدأ بكتابة أفكارك هنا...");
        noteEditText.setHintTextColor(getSecondaryTextColor());
        noteEditText.setGravity(Gravity.RIGHT | Gravity.TOP);
        noteEditText.setTextDirection(View.TEXT_DIRECTION_RTL);
        noteEditText.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        isInternalEditChange = true;
        noteEditText.setText(currentNoteContent);
        isInternalEditChange = false;

        noteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String updated = s != null ? s.toString() : "";
                currentNoteContent = updated;
                isDirty = true;
                updateStatsBar(updated);

                if (!isInternalEditChange) {
                    if (undoStack.isEmpty() || !undoStack.get(undoStack.size() - 1).equals(updated)) {
                        undoStack.add(updated);
                        if (undoStack.size() > 80) {
                            undoStack.remove(0);
                        }
                        redoStack.clear();
                    }
                }

                // جدولة الحفظ التلقائي الفوري بعد 600ms من التوقف عن الكتابة
                mainHandler.removeCallbacks(autoSaveRunnable);
                mainHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY_MS);
            }
        });

        editScrollView.addView(noteEditText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        attachScrollListenerForFastScrollbar(editScrollView);
        paperCard.addView(editScrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    // ===== شريط التمرير السريع الجانبي (يسار الورقة) مع إيقاف الاندفاع الفوري (stopOngoingScrollFling) =====
    private View buildFastScrollerOverlay() {
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        overlay.setClipChildren(false);
        overlay.setClipToPadding(false);

        // مسار السحب في أقصى يسار الورقة
        final FrameLayout touchZone = new FrameLayout(this);
        touchZone.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        FrameLayout.LayoutParams zoneLp = new FrameLayout.LayoutParams(
                dp(30), ViewGroup.LayoutParams.MATCH_PARENT
        );
        zoneLp.gravity = Gravity.LEFT | Gravity.TOP;
        zoneLp.topMargin = dp(12);
        zoneLp.bottomMargin = dp(64);

        fastScrollTrack = new View(this);
        fastScrollTrack.setBackground(createRoundedDrawable(
                isDarkMode ? 0x22FFFFFF : 0x18000000, 0, 0, 4
        ));
        FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(
                dp(4), ViewGroup.LayoutParams.MATCH_PARENT
        );
        trackLp.gravity = Gravity.LEFT;
        trackLp.leftMargin = dp(8);
        touchZone.addView(fastScrollTrack, trackLp);

        fastScrollThumb = new View(this);
        fastScrollThumb.setBackground(createRoundedDrawable(getThemePrimaryColor(), 0x44FFFFFF, 1, 8));
        fastScrollThumb.setElevation(dp(3));
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(dp(10), dp(48));
        thumbLp.gravity = Gravity.LEFT | Gravity.TOP;
        thumbLp.leftMargin = dp(5);
        touchZone.addView(fastScrollThumb, thumbLp);

        // فقاعة عرض النسبة المئوية (0% - 100%)
        fastScrollBubble = new TextView(this);
        fastScrollBubble.setTag("badge");
        fastScrollBubble.setText("0%");
        fastScrollBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        fastScrollBubble.setTypeface(getAppTypeface(true));
        fastScrollBubble.setTextColor(0xFFFFFFFF);
        fastScrollBubble.setGravity(Gravity.CENTER);
        fastScrollBubble.setPadding(dp(12), dp(6), dp(12), dp(6));
        fastScrollBubble.setBackground(createRoundedDrawable(getThemeDarkColor(), 0x44FFFFFF, 1, 14));
        fastScrollBubble.setElevation(dp(6));
        fastScrollBubble.setVisibility(View.GONE);

        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bubbleLp.gravity = Gravity.LEFT | Gravity.TOP;
        bubbleLp.leftMargin = dp(38);
        overlay.addView(fastScrollBubble, bubbleLp);
        overlay.addView(touchZone, zoneLp);

        touchZone.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ScrollView activeSv = isEditMode ? editScrollView : readScrollView;
                if (activeSv == null || activeSv.getChildCount() == 0) return false;

                int contentH = activeSv.getChildAt(0).getHeight();
                int viewH = activeSv.getHeight();
                int maxScroll = contentH - viewH;
                if (maxScroll <= 0) return false;

                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    // إيقاف اندفاع الصفحة (Fling) فوراً من أول لمسة للمقبض
                    stopOngoingScrollFling(activeSv);
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    performHaptic(v);
                    fastScrollBubble.setVisibility(View.VISIBLE);
                }

                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    int trackH = Math.max(1, v.getHeight() - fastScrollThumb.getHeight());
                    float clampedY = Math.max(0f, Math.min(trackH, event.getY() - (fastScrollThumb.getHeight() / 2f)));
                    float fraction = clampedY / (float) trackH;

                    int targetScrollY = Math.round(fraction * maxScroll);
                    activeSv.scrollTo(0, targetScrollY);

                    fastScrollThumb.setTranslationY(clampedY);
                    int pct = Math.round(fraction * 100f);
                    fastScrollBubble.setText(pct + "%");
                    fastScrollBubble.setTranslationY(dp(12) + clampedY);
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    fastScrollBubble.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        });

        return overlay;
    }

    public void stopOngoingScrollFling(ScrollView sv) {
        if (sv == null) return;
        try {
            Field scrollerField = ScrollView.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            Object scroller = scrollerField.get(sv);
            if (scroller instanceof OverScroller) {
                ((OverScroller) scroller).abortAnimation();
            }
        } catch (Exception ignored) {
        }
        try {
            // إرسال حدث لمس صامت لإيقاف أي حركة متبقية في جميع إصدارات أندرويد
            long now = android.os.SystemClock.uptimeMillis();
            MotionEvent cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0);
            sv.dispatchTouchEvent(cancelEvent);
            cancelEvent.recycle();
        } catch (Exception ignored) {
        }
    }

    private void attachScrollListenerForFastScrollbar(final ScrollView sv) {
        if (sv == null) return;
        sv.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                updateFastScrollbarPosition(sv);
            }
        });
    }

    public void updateFastScrollbarPosition(ScrollView sv) {
        if (sv == null || sv.getChildCount() == 0 || fastScrollThumb == null || fastScrollTrack == null) {
            return;
        }
        int contentH = sv.getChildAt(0).getHeight();
        int viewH = sv.getHeight();
        int maxScroll = contentH - viewH;
        if (maxScroll <= dp(20)) {
            fastScrollTrack.setVisibility(View.INVISIBLE);
            fastScrollThumb.setVisibility(View.INVISIBLE);
            return;
        }
        fastScrollTrack.setVisibility(View.VISIBLE);
        fastScrollThumb.setVisibility(View.VISIBLE);

        float fraction = Math.max(0f, Math.min(1f, (float) sv.getScrollY() / (float) maxScroll));
        View parentZone = (View) fastScrollThumb.getParent();
        int trackH = Math.max(1, parentZone.getHeight() - fastScrollThumb.getHeight());
        float thumbY = fraction * trackH;
        fastScrollThumb.setTranslationY(thumbY);
    }

    // ===== زر الانتقال السريع العائم (أسفل يسار الورقة بأسلوب TextPad) =====
    private View buildFloatingJumpButton() {
        FrameLayout btn = new FrameLayout(this);
        int bgCol = isDarkMode ? 0xEE1E293B : 0xEEFFFFFF;
        btn.setBackground(createInteractiveCardDrawable(bgCol, getLuxuryAccentBorderColor(), 1, 22));
        btn.setElevation(dp(5));
        applySpringTouch(btn);

        ImageView iconView = new ImageView(this);
        iconView.setImageDrawable(getToolbarIconDrawable("ic_jump_vertical", getThemePrimaryColor()));
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(22), dp(22));
        iconLp.gravity = Gravity.CENTER;
        btn.addView(iconView, iconLp);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View anchor) {
                showJumpPopupCard(anchor);
            }
        });

        return btn;
    }

    private void showJumpPopupCard(View anchor) {
        if (jumpPopupWindow != null && jumpPopupWindow.isShowing()) {
            jumpPopupWindow.dismiss();
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getLuxuryAccentBorderColor(), 1, 16));
        card.setElevation(dp(8));

        TextView topJumpBtn = new TextView(this);
        topJumpBtn.setTag("btn");
        topJumpBtn.setText("↑  أول المستند");
        topJumpBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        topJumpBtn.setTypeface(getAppTypeface(true));
        topJumpBtn.setTextColor(getPrimaryTextColor());
        topJumpBtn.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        topJumpBtn.setPadding(dp(14), dp(10), dp(14), dp(10));
        topJumpBtn.setBackground(createInteractiveCardDrawable(Color.TRANSPARENT, 0, 0, 10));
        applySpringTouch(topJumpBtn);

        View divider = new View(this);
        divider.setBackgroundColor(getSubtleBorderColor());
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        );
        divLp.topMargin = dp(2);
        divLp.bottomMargin = dp(2);

        TextView bottomJumpBtn = new TextView(this);
        bottomJumpBtn.setTag("btn");
        bottomJumpBtn.setText("↓  آخر المستند");
        bottomJumpBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        bottomJumpBtn.setTypeface(getAppTypeface(true));
        bottomJumpBtn.setTextColor(getPrimaryTextColor());
        bottomJumpBtn.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        bottomJumpBtn.setPadding(dp(14), dp(10), dp(14), dp(10));
        bottomJumpBtn.setBackground(createInteractiveCardDrawable(Color.TRANSPARENT, 0, 0, 10));
        applySpringTouch(bottomJumpBtn);

        card.addView(topJumpBtn);
        card.addView(divider, divLp);
        card.addView(bottomJumpBtn);
        applyFontToViewTree(card);

        jumpPopupWindow = new PopupWindow(
                card,
                dp(165),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        jumpPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        jumpPopupWindow.setOutsideTouchable(true);
        jumpPopupWindow.setElevation(dp(10));

        topJumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpPopupWindow.dismiss();
                scrollToDocumentBoundary(true);
            }
        });

        bottomJumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpPopupWindow.dismiss();
                scrollToDocumentBoundary(false);
            }
        });

        card.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupH = card.getMeasuredHeight();
        jumpPopupWindow.showAsDropDown(anchor, 0, -(anchor.getHeight() + popupH + dp(8)));
    }

    public void scrollToDocumentBoundary(final boolean toTop) {
        final ScrollView activeSv = isEditMode ? editScrollView : readScrollView;
        if (activeSv == null || activeSv.getChildCount() == 0) return;
        stopOngoingScrollFling(activeSv);

        if (toTop) {
            activeSv.smoothScrollTo(0, 0);
            if (isEditMode && noteEditText != null) {
                noteEditText.setSelection(0);
            }
        } else {
            int bottomY = Math.max(0, activeSv.getChildAt(0).getHeight() - activeSv.getHeight());
            activeSv.smoothScrollTo(0, bottomY);
            if (isEditMode && noteEditText != null) {
                noteEditText.setSelection(noteEditText.getText().length());
            }
        }
    }

    // ===== شريط الإحصائيات السفلي + شريط أزرار وضع القراءة =====
    private View buildStatsBarView() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bar.setBackgroundColor(isDarkMode ? 0xFF0F172A : 0xFFF8FAFC);

        View topLine = new View(this);
        topLine.setBackgroundColor(getSubtleBorderColor());
        bar.addView(topLine, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ));

        statsBarTextView = new TextView(this);
        statsBarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        statsBarTextView.setTypeface(getAppTypeface(true));
        statsBarTextView.setTextColor(getSecondaryTextColor());
        statsBarTextView.setGravity(Gravity.CENTER);
        statsBarTextView.setPadding(dp(12), dp(6), dp(12), dp(6));
        bar.addView(statsBarTextView);

        View bottomLine = new View(this);
        bottomLine.setBackgroundColor(getSubtleBorderColor());
        bar.addView(bottomLine, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ));

        return bar;
    }

    public void updateStatsBar(String text) {
        if (statsBarTextView == null) return;
        String safe = text != null ? text : "";
        int charsCount = safe.length();
        int wordsCount = 0;
        String trimmed = safe.trim();
        if (!trimmed.isEmpty()) {
            wordsCount = trimmed.split("\\s+").length;
        }
        int linesCount = safe.isEmpty() ? 1 : (safe.split("\r\n|\r|\n", -1).length);

        statsBarTextView.setText(
                "الكلمات: " + wordsCount + "   •   الحروف: " + charsCount + "   •   الأسطر: " + linesCount
        );
    }

    private View buildReadModeBottomBar() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(16), dp(10), dp(16), dp(14));

        Button editBtn = new Button(this);
        editBtn.setTag("btn");
        editBtn.setAllCaps(false);
        editBtn.setText("تحرير الملف");
        editBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        editBtn.setTypeface(getAppTypeface(true));
        editBtn.setTextColor(0xFFFFFFFF);
        editBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        editBtn.setElevation(dp(3));
        applySpringTouch(editBtn);

        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        editLp.leftMargin = dp(6);
        editLp.topMargin = dp(2);
        editLp.bottomMargin = dp(4);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureCurrentScrollFraction();
                isEditMode = true;
                renderDocumentScreen();
            }
        });

        Button shareBtn = new Button(this);
        shareBtn.setTag("btn");
        shareBtn.setAllCaps(false);
        shareBtn.setText("مشاركة الملف");
        shareBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        shareBtn.setTypeface(getAppTypeface(true));
        shareBtn.setTextColor(isDarkMode ? 0xFFF8FAFC : getThemeDarkColor());
        int shareBg = isDarkMode ? 0xFF334155 : getThemeLightAccentColor();
        shareBtn.setBackground(createInteractiveCardDrawable(shareBg, getLuxuryAccentBorderColor(), 1, 14));
        shareBtn.setElevation(dp(2));
        applySpringTouch(shareBtn);

        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        shareLp.rightMargin = dp(6);
        shareLp.topMargin = dp(2);
        shareLp.bottomMargin = dp(4);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShareOptionsDialog(
                        stripTxtExtension(currentNoteName),
                        currentNoteContent,
                        currentNoteUri
                );
            }
        });

        row.addView(editBtn, editLp);
        row.addView(shareBtn, shareLp);
        return row;
    }

    // ===== شريط أدوات التحرير الـ 12 القابلة للتخصيص =====
    private View buildEditToolbarView() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setClipChildren(false);
        hsv.setClipToPadding(false);
        hsv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setClipChildren(false);
        strip.setClipToPadding(false);
        strip.setPadding(dp(10), dp(8), dp(10), dp(12));

        for (final String toolKey : activeToolbarTools) {
            if (disabledToolbarTools.contains(toolKey)) continue;

            LinearLayout toolChip = new LinearLayout(this);
            toolChip.setOrientation(LinearLayout.HORIZONTAL);
            toolChip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            toolChip.setGravity(Gravity.CENTER);
            int chipBg = isDarkMode ? 0xFF0F172A : 0xFFF1F5F9;
            toolChip.setBackground(createInteractiveCardDrawable(chipBg, getSubtleBorderColor(), 1, 12));
            toolChip.setPadding(dp(11), dp(8), dp(11), dp(8));
            applySpringTouch(toolChip);

            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)
            );
            chipLp.leftMargin = dp(6);
            chipLp.topMargin = dp(2);
            chipLp.bottomMargin = dp(3);

            ImageView iconIv = new ImageView(this);
            iconIv.setImageDrawable(getToolbarIconDrawable(
                    getToolIconResName(toolKey),
                    isDarkMode ? 0xFFF8FAFC : getThemePrimaryColor()
            ));
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(18), dp(18));
            toolChip.addView(iconIv, iconLp);

            if (!toolbarIconsOnly) {
                TextView labelTv = new TextView(this);
                labelTv.setTag("btn");
                labelTv.setText(getToolArabicLabel(toolKey));
                labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
                labelTv.setTypeface(getAppTypeface(true));
                labelTv.setTextColor(getPrimaryTextColor());
                labelTv.setPadding(dp(6), 0, 0, 0);
                toolChip.addView(labelTv);
            }

            toolChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    executeToolbarAction(toolKey);
                }
            });

            strip.addView(toolChip, chipLp);
        }

        hsv.addView(strip);
        return hsv;
    }

    public String getToolArabicLabel(String key) {
        if ("undo".equals(key)) return "تراجع";
        if ("redo".equals(key)) return "إعادة";
        if ("copy".equals(key)) return "نسخ";
        if ("paste".equals(key)) return "لصق";
        if ("select_all".equals(key)) return "تحديد الكل";
        if ("auto_format".equals(key)) return "تنسيق آلي";
        if ("signature".equals(key)) return "التوقيع";
        if ("cut".equals(key)) return "قص";
        if ("select_up".equals(key)) return "تحديد للأعلى";
        if ("select_down".equals(key)) return "تحديد للأسفل";
        if ("datetime".equals(key)) return "التاريخ والوقت";
        if ("find_replace".equals(key)) return "بحث واستبدال";
        return key;
    }

    public String getToolIconResName(String key) {
        if ("undo".equals(key)) return "ic_undo";
        if ("redo".equals(key)) return "ic_redo";
        if ("copy".equals(key)) return "ic_copy";
        if ("paste".equals(key)) return "ic_paste";
        if ("select_all".equals(key)) return "ic_select_all";
        if ("auto_format".equals(key)) return "ic_auto_format";
        if ("signature".equals(key)) return "ic_signature";
        if ("cut".equals(key)) return "ic_cut";
        if ("select_up".equals(key)) return "ic_select_up";
        if ("select_down".equals(key)) return "ic_select_down";
        if ("datetime".equals(key)) return "ic_datetime";
        if ("find_replace".equals(key)) return "ic_find_replace";
        return "ic_auto_format";
    }

    public Drawable getToolbarIconDrawable(final String iconResName, final int tintColor) {
        try {
            int resId = getResources().getIdentifier(iconResName, "drawable", getPackageName());
            if (resId != 0) {
                Drawable d = getResources().getDrawable(resId, getTheme());
                if (d != null) {
                    Drawable mutated = d.mutate();
                    mutated.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
                    return mutated;
                }
            }
        } catch (Exception ignored) {
        }
        // رسم متجهي برمجي فائق الدقة في حال عدم وجود ملف XML خارجي
        return new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(tintColor);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(1.8f));
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);

                Rect b = getBounds();
                float w = b.width();
                float h = b.height();
                float cx = b.left + w / 2f;
                float cy = b.top + h / 2f;

                if ("ic_jump_vertical".equals(iconResName)) {
                    canvas.drawLine(cx, b.top + h * 0.18f, cx, b.bottom - h * 0.18f, p);
                    canvas.drawLine(cx - w * 0.22f, b.top + h * 0.36f, cx, b.top + h * 0.16f, p);
                    canvas.drawLine(cx + w * 0.22f, b.top + h * 0.36f, cx, b.top + h * 0.16f, p);
                    canvas.drawLine(cx - w * 0.22f, b.bottom - h * 0.36f, cx, b.bottom - h * 0.16f, p);
                    canvas.drawLine(cx + w * 0.22f, b.bottom - h * 0.36f, cx, b.bottom - h * 0.16f, p);
                } else if ("ic_auto_format".equals(iconResName)) {
                    // عصا سحرية مع نجوم
                    canvas.drawLine(b.left + w * 0.2f, b.bottom - h * 0.2f, b.right - w * 0.22f, b.top + h * 0.22f, p);
                    p.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(b.right - w * 0.18f, b.top + h * 0.18f, dp(2.2f), p);
                    canvas.drawCircle(b.left + w * 0.28f, b.top + h * 0.25f, dp(1.6f), p);
                    canvas.drawCircle(b.right - w * 0.22f, b.bottom - h * 0.28f, dp(1.6f), p);
                } else {
                    canvas.drawRoundRect(new RectF(b.left + dp(2), b.top + dp(2), b.right - dp(2), b.bottom - dp(2)), dp(4), dp(4), p);
                }
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return android.graphics.PixelFormat.TRANSLUCENT;
            }
        };
    }

    // ===== تنفيذ أوامر الأدوات الـ 12 =====
    public void executeToolbarAction(String key) {
        if (noteEditText == null) return;
        Editable ed = noteEditText.getText();
        int selStart = Math.max(0, noteEditText.getSelectionStart());
        int selEnd = Math.max(0, noteEditText.getSelectionEnd());
        if (selStart > selEnd) {
            int tmp = selStart;
            selStart = selEnd;
            selEnd = tmp;
        }

        if ("undo".equals(key)) {
            if (undoStack.size() > 1) {
                String current = undoStack.remove(undoStack.size() - 1);
                redoStack.add(current);
                String prev = undoStack.get(undoStack.size() - 1);
                isInternalEditChange = true;
                noteEditText.setText(prev);
                noteEditText.setSelection(Math.min(prev.length(), selStart));
                isInternalEditChange = false;
                saveCurrentNoteImmediatelyIfNeeded();
            } else {
                showToast("لا توجد خطوات سابقة للتراجع");
            }
        } else if ("redo".equals(key)) {
            if (!redoStack.isEmpty()) {
                String next = redoStack.remove(redoStack.size() - 1);
                undoStack.add(next);
                isInternalEditChange = true;
                noteEditText.setText(next);
                noteEditText.setSelection(Math.min(next.length(), selStart));
                isInternalEditChange = false;
                saveCurrentNoteImmediatelyIfNeeded();
            } else {
                showToast("لا توجد خطوات للإعادة");
            }
        } else if ("copy".equals(key)) {
            String toCopy = (selEnd > selStart)
                    ? ed.subSequence(selStart, selEnd).toString()
                    : ed.toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("wasm_note", toCopy));
                showToast(selEnd > selStart ? "تم نسخ النص المحدد" : "تم نسخ كامل النص");
            }
        } else if ("cut".equals(key)) {
            if (selEnd > selStart) {
                String cutStr = ed.subSequence(selStart, selEnd).toString();
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("wasm_note", cutStr));
                }
                ed.delete(selStart, selEnd);
                showToast("تم قص النص المحدد");
            } else {
                showToast("حدد نصاً أولاً لقصه");
            }
        } else if ("paste".equals(key)) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence clipTxt = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (clipTxt != null) {
                    ed.replace(selStart, selEnd, clipTxt);
                }
            } else {
                showToast("الحافظة فارغة");
            }
        } else if ("select_all".equals(key)) {
            noteEditText.requestFocus();
            noteEditText.selectAll();
        } else if ("select_up".equals(key)) {
            noteEditText.requestFocus();
            noteEditText.setSelection(0, selEnd);
        } else if ("select_down".equals(key)) {
            noteEditText.requestFocus();
            noteEditText.setSelection(selStart, ed.length());
        } else if ("datetime".equals(key)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar"));
            String stamp = sdf.format(new Date());
            ed.replace(selStart, selEnd, stamp);
        } else if ("signature".equals(key)) {
            if (userSignature != null && !userSignature.trim().isEmpty()) {
                String sigToInsert = "\n" + userSignature.trim() + "\n";
                ed.replace(selStart, selEnd, sigToInsert);
            } else {
                showSignatureSetupDialog(true);
            }
        } else if ("auto_format".equals(key)) {
            performSmartAutoFormat();
        } else if ("find_replace".equals(key)) {
            showFindAndReplaceDialog();
        }
    }

    public void performSmartAutoFormat() {
        if (noteEditText == null) return;
        String raw = noteEditText.getText().toString();
        if (raw.trim().isEmpty()) {
            showToast("المستند فارغ");
            return;
        }

        String[] lines = raw.split("\r\n|\r|\n");
        StringBuilder formatted = new StringBuilder();
        int emptyStreak = 0;

        for (String line : lines) {
            String clean = line.trim().replaceAll("[ \\t]+", " ");
            // ضبط المسافات قبل وبعد علامات الترقيم العربية والإنجليزية
            clean = clean.replaceAll("\\s+([،؛؟!.,:])", "$1");
            clean = clean.replaceAll("([،؛؟!.,:])([^\\s\\d\"'»)])", "$1 $2");

            if (clean.isEmpty()) {
                emptyStreak++;
                if (emptyStreak == 1 && formatted.length() > 0) {
                    formatted.append("\n");
                }
            } else {
                emptyStreak = 0;
                if (formatted.length() > 0) {
                    formatted.append("\n");
                }
                if (autoFormatIndent && !clean.startsWith("•") && !clean.startsWith("-")) {
                    formatted.append("    ").append(clean);
                } else {
                    formatted.append(clean);
                }
            }
        }

        String result = formatted.toString().trim();
        noteEditText.setText(result);
        noteEditText.setSelection(result.length());
        saveCurrentNoteImmediatelyIfNeeded();
        showToast("تم تنسيق النص والفقرات تلقائياً ✨");
    }

    // ===== أداة بحث واستبدال الذكية داخل المحرر =====
    public void showFindAndReplaceDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        final EditText findEt = createStyledEditText("", "الكلمة المراد البحث عنها...");
        findEt.setSingleLine(true);

        final EditText replaceEt = createStyledEditText("", "استبدال بـ (اتركه فارغاً للبحث فقط)...");
        replaceEt.setSingleLine(true);
        LinearLayout.LayoutParams repLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        repLp.topMargin = dp(10);

        final TextView countInfoTv = new TextView(this);
        countInfoTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        countInfoTv.setTypeface(getAppTypeface(true));
        countInfoTv.setTextColor(getThemePrimaryColor());
        countInfoTv.setGravity(Gravity.RIGHT);
        countInfoTv.setPadding(dp(4), dp(8), dp(4), 0);
        countInfoTv.setText("اكتب كلمة للبحث الذكي (يتجاهل التشكيل والهمزات)");

        findEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString().trim() : "";
                if (q.isEmpty()) {
                    countInfoTv.setText("اكتب كلمة للبحث الذكي (يتجاهل التشكيل والهمزات)");
                } else {
                    List<int[]> matches = findSmartMatchRanges(currentNoteContent, q);
                    countInfoTv.setText("عدد المطابقات في المستند: " + matches.size());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        box.addView(findEt);
        box.addView(replaceEt, repLp);
        box.addView(countInfoTv);

        showStyledDialog(
                "بحث واستبدال ذكي",
                "يبحث ويستبدل بدقة حتى لو كانت الكلمات مشكّلة في النص الأصلي",
                box,
                "استبدال الكل",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        String q = findEt.getText().toString().trim();
                        String rep = replaceEt.getText().toString();
                        if (q.isEmpty()) {
                            showToast("يرجى كتابة كلمة البحث أولاً");
                            return;
                        }
                        List<int[]> ranges = findSmartMatchRanges(currentNoteContent, q);
                        if (ranges.isEmpty()) {
                            showToast("لا توجد مطابقات للكلمة المدخلة");
                            return;
                        }
                        StringBuilder sb = new StringBuilder(currentNoteContent);
                        for (int i = ranges.size() - 1; i >= 0; i--) {
                            int[] r = ranges.get(i);
                            sb.replace(r[0], r[1], rep);
                        }
                        String updated = sb.toString();
                        if (noteEditText != null) {
                            noteEditText.setText(updated);
                        }
                        saveCurrentNoteImmediatelyIfNeeded();
                        showToast("تم استبدال " + ranges.size() + " مطابقة بنجاح");
                    }
                },
                "تحديد أول مطابقة",
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        String q = findEt.getText().toString().trim();
                        if (q.isEmpty() || noteEditText == null) return;
                        List<int[]> ranges = findSmartMatchRanges(currentNoteContent, q);
                        if (!ranges.isEmpty()) {
                            int[] first = ranges.get(0);
                            noteEditText.requestFocus();
                            noteEditText.setSelection(first[0], first[1]);
                        } else {
                            showToast("لم يتم العثور على الكلمة");
                        }
                    }
                }
        );
    }

    // ===== نافذة «تخصيص أدوات التحرير» المعتمدة (زر استعادة الافتراضي في الأعلى + السحب المتصل ≡) =====
    public void showCustomizeToolbarDialog(final Runnable onSavedCallback) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final List<String> tempOrder = new ArrayList<>(activeToolbarTools);
        final Set<String> tempDisabled = new HashSet<>(disabledToolbarTools);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(18), dp(18), dp(18), dp(22));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getSubtleBorderColor(), 1, 24));
        card.setElevation(dp(12));

        // العنوان المعتمد حرفياً: «تخصيص أدوات التحرير» (بدون كلمة وترتيب)
        TextView titleTv = new TextView(this);
        titleTv.setText("تخصيص أدوات التحرير");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.5f);
        titleTv.setTypeface(getAppTypeface(true));
        titleTv.setTextColor(getPrimaryTextColor());
        titleTv.setGravity(Gravity.RIGHT);
        card.addView(titleTv);

        TextView descTv = new TextView(this);
        descTv.setText("فعّل الأدوات المطلوبة أو اسحب مقبض (≡) لترتيبها بحركة متصلة:");
        descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        descTv.setTypeface(getAppTypeface(false));
        descTv.setTextColor(getSecondaryTextColor());
        descTv.setGravity(Gravity.RIGHT);
        descTv.setPadding(0, dp(4), 0, dp(10));
        card.addView(descTv);

        // زر «استعادة الافتراضي» في أعلى النافذة (تحت العنوان والوصف مباشرة وقبل قائمة الأدوات)
        Button restoreDefBtn = new Button(this);
        restoreDefBtn.setTag("btn");
        restoreDefBtn.setAllCaps(false);
        restoreDefBtn.setText("استعادة الافتراضي");
        restoreDefBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        restoreDefBtn.setTypeface(getAppTypeface(true));
        restoreDefBtn.setTextColor(isDarkMode ? 0xFFF8FAFC : getThemeDarkColor());
        int defBtnBg = isDarkMode ? 0xFF334155 : getThemeLightAccentColor();
        restoreDefBtn.setBackground(createInteractiveCardDrawable(defBtnBg, getLuxuryAccentBorderColor(), 1, 12));
        applySpringTouch(restoreDefBtn);

        LinearLayout.LayoutParams defBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)
        );
        defBtnLp.bottomMargin = dp(12);
        card.addView(restoreDefBtn, defBtnLp);

        // قائمة الأدوات القابلة للسحب والإفلات المتصل السلس
        final ScrollView listScroll = new ScrollView(this);
        listScroll.setVerticalScrollBarEnabled(false);
        listScroll.setClipChildren(false);
        listScroll.setClipToPadding(false);

        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listContainer.setPadding(dp(2), dp(2), dp(2), dp(6));
        listScroll.addView(listContainer);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxListHeight = Math.round(dm.heightPixels * 0.48f);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxListHeight
        );
        card.addView(listScroll, scrollLp);

        final Runnable[] rebuildRowsRef = new Runnable[1];
        rebuildRowsRef[0] = new Runnable() {
            @Override
            public void run() {
                populateDraggableToolRows(listScroll, listContainer, tempOrder, tempDisabled);
            }
        };
        rebuildRowsRef[0].run();

        // نافذة التأكيد عند الضغط على «استعادة الافتراضي»
        restoreDefBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStyledDialog(
                        "استعادة الإعدادات الافتراضية",
                        "هل أنت متأكد أنك تريد استعادة الإعدادات الافتراضية؟",
                        null,
                        "نعم",
                        getThemePrimaryColor(),
                        new Runnable() {
                            @Override
                            public void run() {
                                tempOrder.clear();
                                tempOrder.addAll(Arrays.asList(DEFAULT_TOOLBAR_TOOLS));
                                tempDisabled.clear();
                                rebuildRowsRef[0].run();
                                showToast("تمت استعادة ترتيب وتفعيل الأدوات الافتراضية");
                            }
                        },
                        "لا",
                        null
                );
            }
        });

        // أسفل النافذة: زرا «حفظ» و«إلغاء» فقط
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bottomRow.setClipChildren(false);
        bottomRow.setClipToPadding(false);
        bottomRow.setPadding(dp(2), dp(12), dp(2), dp(4));

        Button saveBtn = new Button(this);
        saveBtn.setTag("btn");
        saveBtn.setAllCaps(false);
        saveBtn.setText("حفظ");
        saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        saveBtn.setTypeface(getAppTypeface(true));
        saveBtn.setTextColor(0xFFFFFFFF);
        saveBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 14));
        saveBtn.setElevation(dp(3));
        applySpringTouch(saveBtn);

        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        saveLp.leftMargin = dp(6);
        saveLp.topMargin = dp(2);
        saveLp.bottomMargin = dp(4);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeToolbarTools.clear();
                activeToolbarTools.addAll(tempOrder);
                disabledToolbarTools.clear();
                disabledToolbarTools.addAll(tempDisabled);
                saveToolbarConfiguration();
                dialog.dismiss();
                if (isInDocumentScreen && isEditMode) {
                    renderDocumentScreen();
                }
                if (onSavedCallback != null) {
                    onSavedCallback.run();
                }
                showToast("تم حفظ تخصيص أدوات التحرير");
            }
        });

        Button cancelBtn = new Button(this);
        cancelBtn.setTag("btn");
        cancelBtn.setAllCaps(false);
        cancelBtn.setText("إلغاء");
        cancelBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        cancelBtn.setTypeface(getAppTypeface(true));
        cancelBtn.setTextColor(getPrimaryTextColor());
        cancelBtn.setBackground(createInteractiveCardDrawable(
                isDarkMode ? 0xFF334155 : 0xFFE2E8F0, getSubtleBorderColor(), 1, 14
        ));
        applySpringTouch(cancelBtn);

        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cancelLp.rightMargin = dp(6);
        cancelLp.topMargin = dp(2);
        cancelLp.bottomMargin = dp(4);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        bottomRow.addView(saveBtn, saveLp);
        bottomRow.addView(cancelBtn, cancelLp);
        card.addView(bottomRow);

        applyFontToViewTree(card);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        wrapper.setPadding(dp(14), dp(14), dp(14), dp(14));
        wrapper.addView(card);

        dialog.setContentView(wrapper);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    // ===== محرك السحب والإفلات المتصل السلس (Continuous Drag & Auto-Scroll) =====
    private void populateDraggableToolRows(
            final ScrollView listScroll,
            final LinearLayout listContainer,
            final List<String> tempOrder,
            final Set<String> tempDisabled
    ) {
        listContainer.removeAllViews();

        for (int i = 0; i < tempOrder.size(); i++) {
            final String toolKey = tempOrder.get(i);
            final LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            int rowBg = isDarkMode ? 0xFF0F172A : 0xFFF8FAFC;
            row.setBackground(createRoundedDrawable(rowBg, getSubtleBorderColor(), 1, 14));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
            );
            rowLp.bottomMargin = dp(7);

            // مفتاح التفعيل المنزلق على اليمين
            final boolean isEnabled = !tempDisabled.contains(toolKey);
            final View toggleView = createSlidingSwitchView(isEnabled, new Runnable() {
                @Override
                public void run() {
                    if (tempDisabled.contains(toolKey)) {
                        tempDisabled.remove(toolKey);
                    } else {
                        tempDisabled.add(toolKey);
                    }
                }
            });
            LinearLayout.LayoutParams togLp = new LinearLayout.LayoutParams(dp(46), dp(26));
            togLp.leftMargin = dp(10);
            row.addView(toggleView, togLp);

            // أيقونة الأداة
            ImageView iconIv = new ImageView(this);
            iconIv.setImageDrawable(getToolbarIconDrawable(getToolIconResName(toolKey), getThemePrimaryColor()));
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(20), dp(20));
            icLp.leftMargin = dp(10);
            row.addView(iconIv, icLp);

            // اسم الأداة
            TextView nameTv = new TextView(this);
            nameTv.setText(getToolArabicLabel(toolKey));
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            nameTv.setTypeface(getAppTypeface(true));
            nameTv.setTextColor(getPrimaryTextColor());
            nameTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            );
            row.addView(nameTv, nameLp);

            // مقبض السحب المتصل السلس (≡) في اليسار
            TextView dragHandle = new TextView(this);
            dragHandle.setTag("btn");
            dragHandle.setText("≡");
            dragHandle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            dragHandle.setTypeface(getAppTypeface(true));
            dragHandle.setTextColor(getSecondaryTextColor());
            dragHandle.setGravity(Gravity.CENTER);
            dragHandle.setPadding(dp(12), dp(4), dp(8), dp(4));
            row.addView(dragHandle);

            attachContinuousDragListener(dragHandle, row, listScroll, listContainer, tempOrder);
            listContainer.addView(row, rowLp);
        }
        applyFontToViewTree(listContainer);
    }

    private void attachContinuousDragListener(
            View handle,
            final View draggedRow,
            final ScrollView scrollView,
            final LinearLayout container,
            final List<String> orderList
    ) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float startRawY = 0f;
            private int initialScrollY = 0;
            private int draggedIndex = -1;
            private final Runnable autoScrollTick = new Runnable() {
                @Override
                public void run() {
                    if (draggedIndex < 0) return;
                    // استمرار التمرير التلقائي السلس عند الاقتراب من الحواف
                    mainHandler.postDelayed(this, 16);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    draggedIndex = container.indexOfChild(draggedRow);
                    if (draggedIndex < 0) return false;

                    startRawY = event.getRawY();
                    initialScrollY = scrollView.getScrollY();

                    ViewParent parent = scrollView.getParent();
                    while (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                        parent = parent.getParent();
                    }
                    scrollView.requestDisallowInterceptTouchEvent(true);

                    performHaptic(draggedRow);
                    draggedRow.setElevation(dp(10));
                    draggedRow.setScaleX(1.02f);
                    draggedRow.setScaleY(1.02f);
                    draggedRow.setAlpha(0.93f);
                    return true;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (draggedIndex < 0) return false;

                    // تمرير تلقائي للقائمة عند سحب الصف قرب أعلى أو أسفل منطقة التمرير
                    int[] svLoc = new int[2];
                    scrollView.getLocationOnScreen(svLoc);
                    float rawY = event.getRawY();
                    int edgeZone = dp(48);
                    if (rawY < svLoc[1] + edgeZone) {
                        scrollView.scrollBy(0, -dp(8));
                    } else if (rawY > svLoc[1] + scrollView.getHeight() - edgeZone) {
                        scrollView.scrollBy(0, dp(8));
                    }

                    int scrollDelta = scrollView.getScrollY() - initialScrollY;
                    float dy = (rawY - startRawY) + scrollDelta;
                    draggedRow.setTranslationY(dy);

                    int rowFullHeight = draggedRow.getHeight() + dp(7);
                    int steps = Math.round(dy / (float) rowFullHeight);
                    int targetIndex = Math.max(0, Math.min(container.getChildCount() - 1, draggedIndex + steps));

                    if (targetIndex != draggedIndex) {
                        // تبديل موقع الأداة في القائمة مع الحفاظ على اتصال حركة الإصبع دون إفلات
                        String movedItem = orderList.remove(draggedIndex);
                        orderList.add(targetIndex, movedItem);

                        container.removeViewAt(draggedIndex);
                        container.addView(draggedRow, targetIndex);

                        int indexDiff = targetIndex - draggedIndex;
                        startRawY += indexDiff * rowFullHeight;
                        draggedIndex = targetIndex;
                        draggedRow.setTranslationY((rawY - startRawY) + (scrollView.getScrollY() - initialScrollY));
                        performHaptic(draggedRow);
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    mainHandler.removeCallbacks(autoScrollTick);
                    draggedIndex = -1;
                    draggedRow.animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                    draggedRow.setElevation(0f);
                    return true;
                }
                return false;
            }
        });
    }

    // ===== توليد ومشاركة بطاقة اقتباس «وسم» الفاخرة كصورة عالية الدقة =====
    public void shareLuxuryQuoteCard(String title, String content) {
        try {
            String cleanQuote = (content != null ? content.trim() : "");
            if (cleanQuote.isEmpty()) {
                cleanQuote = title != null ? title : BRAND_NAME;
            }
            if (cleanQuote.length() > 650) {
                cleanQuote = cleanQuote.substring(0, 650) + "...";
            }

            int width = 1080;
            int padding = 90;
            int textWidth = width - (padding * 2);

            TextPaint quotePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            quotePaint.setColor(0xFFFFFFFF);
            quotePaint.setTextSize(42f);
            quotePaint.setTypeface(getAppTypeface(false));

            StaticLayout quoteLayout = StaticLayout.Builder
                    .obtain(cleanQuote, 0, cleanQuote.length(), quotePaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(14f, 1.35f)
                    .build();

            int height = Math.max(760, quoteLayout.getHeight() + 420);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);

            // خلفية متدرجة ملكية بلون الثيم النشط
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setShader(new LinearGradient(
                    0, 0, width, height,
                    getThemeDarkColor(), getThemePrimaryColor(),
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRect(0, 0, width, height, bgPaint);

            // إطار داخلي ذهبي/زجاجي فاخر
            Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setStrokeWidth(3f);
            framePaint.setColor(0x44FFFFFF);
            canvas.drawRoundRect(new RectF(42, 42, width - 42, height - 42), 36, 36, framePaint);

            // عنوان الملاحظة في الأعلى
            TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(0xFFFDE68A);
            titlePaint.setTextSize(46f);
            titlePaint.setTypeface(getAppTypeface(true));
            titlePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(title != null ? title : BRAND_NAME, width - padding, 140, titlePaint);

            // رسم النص المقتبس
            canvas.save();
            canvas.translate(padding, 200);
            quoteLayout.draw(canvas);
            canvas.restore();

            // توقيع هوية «وسم» في الأسفل
            TextPaint footerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            footerPaint.setColor(0xCCFFFFFF);
            footerPaint.setTextSize(32f);
            footerPaint.setTypeface(getAppTypeface(true));
            footerPaint.setTextAlign(Paint.Align.CENTER);
            String footerText = (userSignature != null && !userSignature.trim().isEmpty())
                    ? userSignature.trim() + "  •  " + BRAND_NAME
                    : DEFAULT_HERO_TITLE;
            canvas.drawText(footerText, width / 2f, height - 85, footerPaint);

            // حفظ الصورة ومشاركتها
            if (hasValidFolderPermission()) {
                Uri vaultUri = getOrCreateVaultDir();
                if (vaultUri != null) {
                    String imgName = "Wasm_Card_" + System.currentTimeMillis() + ".png";
                    Uri imgUri = DocumentsContract.createDocument(
                            getContentResolver(), vaultUri, "image/png", imgName
                    );
                    if (imgUri != null) {
                        OutputStream os = getContentResolver().openOutputStream(imgUri);
                        if (os != null) {
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                            os.flush();
                            os.close();
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("image/png");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, imgUri);
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(shareIntent, "مشاركة بطاقة وسم الفاخرة"));
                            return;
                        }
                    }
                }
            }
            sharePlainText(title, content);
        } catch (Exception e) {
            sharePlainText(title, content);
        }
    }
// ===== END PART 04/05 =====
