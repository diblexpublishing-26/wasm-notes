// ===== MAZWD PART 02/05: STORAGE — v2.7.1 =====

    // ===== إدارة صلاحيات المجلد الحقيقي المباشر (SAF) =====
    public boolean hasValidFolderPermission() {
        if (treeUri == null) return false;
        try {
            List< android.content.UriPermission > perms = getContentResolver().getPersistedUriPermissions();
            for (android.content.UriPermission p : perms) {
                if (p.getUri().equals(treeUri) && p.isReadPermission() && p.isWritePermission()) {
                    String docId = DocumentsContract.getTreeDocumentId(treeUri);
                    Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                    Cursor c = getContentResolver().query(
                            docUri,
                            new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
                            null, null, null
                    );
                    if (c != null) {
                        boolean ok = c.moveToFirst();
                        c.close();
                        return ok;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public void pickFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );
            startActivityForResult(intent, REQ_OPEN_FOLDER);
        } catch (Exception e) {
            showToast("تعذر فتح منتقي المجلدات في هذا الجهاز");
        }
    }

    public Uri getRootDocumentUri() {
        if (treeUri == null) return null;
        String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId);
    }

    // ===== القراءة والكتابة المباشرة بترميز UTF-8 القياسي =====
    public String readTextFromUri(Uri uri) {
        if (uri == null) return "";
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = getContentResolver().openInputStream(uri);
            if (is == null) return "";
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, n);
            }
            // إزالة علامة BOM إن وجدت في بداية الملف
            if (sb.length() > 0 && sb.charAt(0) == '\uFEFF') {
                sb.deleteCharAt(0);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        } finally {
            try {
                if (reader != null) reader.close();
                if (is != null) is.close();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean writeTextToUri(Uri uri, String content) {
        if (uri == null) return false;
        String safeContent = content != null ? content : "";
        ParcelFileDescriptor pfd = null;
        FileOutputStream fos = null;
        OutputStreamWriter writer = null;
        try {
            pfd = getContentResolver().openFileDescriptor(uri, "rw");
            if (pfd != null) {
                fos = new FileOutputStream(pfd.getFileDescriptor());
                // تفريغ محتوى الملف القديم بالكامل قبل كتابة النص الجديد لمنع بقايا الأحرف
                fos.getChannel().truncate(0);
                writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                writer.write(safeContent);
                writer.flush();
                fos.getFD().sync();
                return true;
            }
        } catch (Exception e) {
            // محاولة بديلة عبر openOutputStream المباشر
            OutputStream os = null;
            try {
                os = getContentResolver().openOutputStream(uri, "wt");
                if (os == null) {
                    os = getContentResolver().openOutputStream(uri);
                }
                if (os != null) {
                    byte[] bytes = safeContent.getBytes(StandardCharsets.UTF_8);
                    os.write(bytes);
                    os.flush();
                    return true;
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    if (os != null) os.close();
                } catch (Exception ignored) {
                }
            }
        } finally {
            try {
                if (writer != null) writer.close();
                if (fos != null) fos.close();
                if (pfd != null) pfd.close();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public void saveCurrentNoteImmediatelyIfNeeded() {
        mainHandler.removeCallbacks(autoSaveRunnable);
        if (!isInDocumentScreen || currentNoteUri == null || !isDirty) {
            return;
        }
        if (isEditMode && noteEditText != null) {
            currentNoteContent = noteEditText.getText().toString();
        }
        boolean ok = writeTextToUri(currentNoteUri, currentNoteContent);
        if (ok) {
            isDirty = false;
            // تحديث النسخة المخبأة في الذاكرة لتسريع العودة للقائمة الرئيسية
            for (NoteItem item : cachedNotesList) {
                if (item.uri != null && item.uri.equals(currentNoteUri)) {
                    item.content = currentNoteContent;
                    item.lastModified = System.currentTimeMillis();
                    break;
                }
            }
        }
    }

    // ===== دوال البحث والإنشاء داخل مجلدات SAF =====
    public Uri findChildDocumentByName(String parentDocId, String targetName) {
        if (treeUri == null || parentDocId == null || targetName == null) return null;
        Cursor cursor = null;
        try {
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId);
            cursor = getContentResolver().query(
                    childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    },
                    null, null, null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    if (name != null && name.equalsIgnoreCase(targetName)) {
                        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public String generateUniqueTxtFileName(String parentDocId, String baseDesiredName) {
        String cleanBase = stripTxtExtension(baseDesiredName != null ? baseDesiredName.trim() : "");
        cleanBase = sanitizeFileName(cleanBase);
        if (cleanBase.isEmpty()) {
            cleanBase = "ملاحظة جديدة";
        }
        String candidate = cleanBase + ".txt";
        int counter = 2;
        while (findChildDocumentByName(parentDocId, candidate) != null) {
            candidate = cleanBase + " (" + counter + ").txt";
            counter++;
        }
        return candidate;
    }

    public String sanitizeFileName(String raw) {
        if (raw == null) return "";
        // إزالة المحارف الممنوعة في أنظمة ملفات Android و iOS
        return raw.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", " ").replaceAll("\\s+", " ").trim();
    }

    public List<NoteItem> loadAllNotesFromFolder() {
        List<NoteItem> list = new ArrayList<>();
        if (!hasValidFolderPermission()) {
            cachedNotesList = list;
            return list;
        }

        String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId);
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(
                    childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    },
                    null, null, null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    String mime = cursor.getString(2);
                    long lastMod = cursor.isNull(3) ? 0L : cursor.getLong(3);

                    if (name == null || name.startsWith(".")) continue;
                    if (VAULT_FOLDER_NAME.equals(name)) continue;
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;

                    if (name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                        NoteItem item = new NoteItem();
                        item.uri = fileUri;
                        item.fileName = name;
                        item.displayName = stripTxtExtension(name);
                        item.lastModified = lastMod;
                        item.content = readTextFromUri(fileUri);
                        item.isPinned = pinnedFileNames.contains(name) || pinnedFileNames.contains(item.displayName);
                        list.add(item);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        Collections.sort(list, new Comparator<NoteItem>() {
            @Override
            public int compare(NoteItem a, NoteItem b) {
                if (a.isPinned != b.isPinned) {
                    return a.isPinned ? -1 : 1;
                }
                return Long.compare(b.lastModified, a.lastModified);
            }
        });

        cachedNotesList = list;
        return list;
    }

    // ===== نظام خزنة النسخ الآمنة (خزنة_النسخ_الآمنة + .nomedia) =====
    public Uri getOrCreateVaultDir() {
        if (!hasValidFolderPermission()) return null;
        try {
            String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri existing = findChildDocumentByName(rootDocId, VAULT_FOLDER_NAME);
            if (existing != null) {
                return existing;
            }
            Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId);
            return DocumentsContract.createDocument(
                    getContentResolver(),
                    rootDocUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    VAULT_FOLDER_NAME
            );
        } catch (Exception e) {
            return null;
        }
    }

    public void ensureVaultNoMedia() {
        if (!hasValidFolderPermission()) return;
        try {
            Uri vaultUri = getOrCreateVaultDir();
            if (vaultUri == null) return;
            String vaultDocId = DocumentsContract.getDocumentId(vaultUri);
            Uri existingNoMedia = findChildDocumentByName(vaultDocId, ".nomedia");
            if (existingNoMedia == null) {
                DocumentsContract.createDocument(
                        getContentResolver(),
                        vaultUri,
                        "application/octet-stream",
                        ".nomedia"
                );
            }
        } catch (Exception ignored) {
        }
    }

    public boolean createSafeBackup(String noteFileName, String content, boolean showFeedback) {
        if (!hasValidFolderPermission()) {
            if (showFeedback) showToast("يرجى اختيار مجلد التخزين أولاً");
            return false;
        }
        try {
            Uri vaultUri = getOrCreateVaultDir();
            if (vaultUri == null) {
                if (showFeedback) showToast("تعذر الوصول إلى خزنة النسخ الآمنة");
                return false;
            }
            ensureVaultNoMedia();

            String baseName = stripTxtExtension(noteFileName != null ? noteFileName : "ملاحظة");
            baseName = sanitizeFileName(baseName);
            if (baseName.isEmpty()) baseName = "ملاحظة";

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String timestamp = sdf.format(new Date());
            String backupFileName = baseName + SAFE_BACKUP_MARKER + timestamp + ".txt";

            String vaultDocId = DocumentsContract.getDocumentId(vaultUri);
            int suffix = 2;
            while (findChildDocumentByName(vaultDocId, backupFileName) != null) {
                backupFileName = baseName + SAFE_BACKUP_MARKER + timestamp + "_" + suffix + ".txt";
                suffix++;
            }

            Uri backupUri = DocumentsContract.createDocument(
                    getContentResolver(),
                    vaultUri,
                    "text/plain",
                    backupFileName
            );
            if (backupUri != null) {
                boolean written = writeTextToUri(backupUri, content != null ? content : "");
                if (written && showFeedback) {
                    showToast("تم حفظ نسخة آمنة في «" + VAULT_FOLDER_NAME + "»");
                }
                return written;
            }
        } catch (Exception ignored) {
        }
        if (showFeedback) {
            showToast("تعذر إنشاء النسخة الآمنة");
        }
        return false;
    }

    public List<BackupItem> listSafeBackupsForFile(String noteFileName) {
        List<BackupItem> results = new ArrayList<>();
        if (!hasValidFolderPermission() || noteFileName == null) return results;

        String baseName = stripTxtExtension(noteFileName).trim();
        String expectedPrefix = baseName + SAFE_BACKUP_MARKER;

        Cursor cursor = null;
        try {
            Uri vaultUri = getOrCreateVaultDir();
            if (vaultUri == null) return results;
            String vaultDocId = DocumentsContract.getDocumentId(vaultUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, vaultDocId);

            cursor = getContentResolver().query(
                    childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    },
                    null, null, null
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    long lastMod = cursor.isNull(2) ? 0L : cursor.getLong(2);

                    if (name != null && name.startsWith(expectedPrefix) && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                        BackupItem b = new BackupItem();
                        b.uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                        b.fileName = name;
                        b.lastModified = lastMod;

                        String stampPart = stripTxtExtension(name.substring(expectedPrefix.length()));
                        b.displayDate = formatBackupStamp(stampPart, lastMod);
                        results.add(b);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        Collections.sort(results, new Comparator<BackupItem>() {
            @Override
            public int compare(BackupItem a, BackupItem b) {
                int cmp = Long.compare(b.lastModified, a.lastModified);
                if (cmp != 0) return cmp;
                return b.fileName.compareTo(a.fileName);
            }
        });

        return results;
    }

    private String formatBackupStamp(String rawStamp, long fallbackMillis) {
        try {
            String clean = rawStamp;
            if (clean.contains("_")) {
                String[] parts = clean.split("_");
                if (parts.length >= 2) {
                    String datePart = parts[0].replace('-', '/');
                    String timePart = parts[1].replace('-', ':');
                    return datePart + "  •  " + timePart;
                }
            }
        } catch (Exception ignored) {
        }
        return formatTimestamp(fallbackMillis);
    }

    public void renameSafeBackupsForFile(String oldNoteFileName, String newNoteFileName) {
        if (!hasValidFolderPermission() || oldNoteFileName == null || newNoteFileName == null) return;
        String oldBase = stripTxtExtension(oldNoteFileName).trim();
        String newBase = stripTxtExtension(newNoteFileName).trim();
        if (oldBase.isEmpty() || newBase.isEmpty() || oldBase.equals(newBase)) return;

        String oldPrefix = oldBase + SAFE_BACKUP_MARKER;
        String newPrefix = newBase + SAFE_BACKUP_MARKER;

        List<BackupItem> backups = listSafeBackupsForFile(oldNoteFileName);
        Uri vaultUri = getOrCreateVaultDir();
        if (vaultUri == null) return;

        for (BackupItem b : backups) {
            if (b.fileName != null && b.fileName.startsWith(oldPrefix)) {
                String targetBackupName = newPrefix + b.fileName.substring(oldPrefix.length());
                try {
                    DocumentsContract.renameDocument(getContentResolver(), b.uri, targetBackupName);
                } catch (Exception e) {
                    // بديل آمن إذا لم يدعم النظام إعادة التسمية المباشرة
                    try {
                        String content = readTextFromUri(b.uri);
                        Uri created = DocumentsContract.createDocument(
                                getContentResolver(), vaultUri, "text/plain", targetBackupName
                        );
                        if (created != null && writeTextToUri(created, content)) {
                            DocumentsContract.deleteDocument(getContentResolver(), b.uri);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    // ===== الاستعادة الذكية الصامتة وتصفح النسخ الآمنة =====
    public void performSilentSmartRestore(NoteItem targetNote, BackupItem chosenBackup) {
        if (targetNote == null || chosenBackup == null || chosenBackup.uri == null) return;

        String backupContent = readTextFromUri(chosenBackup.uri);
        String currentContent = readTextFromUri(targetNote.uri);

        // فحص صامت: هل يحتوي النص الحالي على تعديلات جديدة غير محفوظة في أحدث نسخة آمنة؟
        List<BackupItem> allBackups = listSafeBackupsForFile(targetNote.fileName);
        String latestBackupContent = null;
        if (!allBackups.isEmpty()) {
            latestBackupContent = readTextFromUri(allBackups.get(0).uri);
        }

        if (currentContent != null && !currentContent.trim().isEmpty()) {
            boolean differsFromLatest = (latestBackupContent == null) || !currentContent.equals(latestBackupContent);
            boolean differsFromChosen = !currentContent.equals(backupContent);
            if (differsFromLatest && differsFromChosen) {
                // إنشاء نسخة آمنة صامتة للنص الحالي قبل الاستعادة مباشرة دون إزعاج المستخدم
                createSafeBackup(targetNote.fileName, currentContent, false);
            }
        }

        boolean restored = writeTextToUri(targetNote.uri, backupContent);
        if (restored) {
            targetNote.content = backupContent;
            targetNote.lastModified = System.currentTimeMillis();
            if (isInDocumentScreen && currentNoteUri != null && currentNoteUri.equals(targetNote.uri)) {
                currentNoteContent = backupContent;
                isDirty = false;
                renderDocumentScreen();
            } else {
                refreshHomeFilesList();
            }
            showToast("تمت استعادة النسخة الآمنة بنجاح");
        } else {
            showToast("تعذر استعادة النسخة الآمنة");
        }
    }

    public void showSafeBackupsDialog(final NoteItem item) {
        if (item == null) return;
        final List<BackupItem> backups = listSafeBackupsForFile(item.fileName);
        if (backups.isEmpty()) {
            showStyledDialog(
                    "تصفح النسخ الآمنة",
                    "لا توجد حتى الآن أي نسخ آمنة محفوظة للملف «" + item.displayName + "» داخل خزنة النسخ الآمنة.",
                    null,
                    "إنشاء نسخة آمنة الآن",
                    getThemePrimaryColor(),
                    new Runnable() {
                        @Override
                        public void run() {
                            String liveContent = readTextFromUri(item.uri);
                            createSafeBackup(item.fileName, liveContent, true);
                        }
                    },
                    "إغلاق",
                    null
            );
            return;
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listContainer.setPadding(dp(2), dp(4), dp(2), dp(8));
        scroll.addView(listContainer);

        final Dialog[] parentDialogHolder = new Dialog[1];

        for (int i = 0; i < backups.size(); i++) {
            final BackupItem b = backups.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            int rowBg = isDarkMode ? 0xFF0F172A : 0xFFF8FAFC;
            row.setBackground(createInteractiveCardDrawable(rowBg, getSubtleBorderColor(), 1, 14));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowLp.bottomMargin = dp(8);

            TextView titleTv = new TextView(this);
            titleTv.setText("نسخة آمنة #" + (backups.size() - i));
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            titleTv.setTypeface(getAppTypeface(true));
            titleTv.setTextColor(getPrimaryTextColor());
            row.addView(titleTv);

            TextView dateTv = new TextView(this);
            dateTv.setText(b.displayDate);
            dateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            dateTv.setTypeface(getAppTypeface(false));
            dateTv.setTextColor(getSecondaryTextColor());
            dateTv.setPadding(0, dp(3), 0, dp(8));
            row.addView(dateTv);

            LinearLayout actionsRow = new LinearLayout(this);
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            actionsRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            actionsRow.setClipChildren(false);
            actionsRow.setClipToPadding(false);

            Button previewBtn = new Button(this);
            previewBtn.setTag("btn");
            previewBtn.setAllCaps(false);
            previewBtn.setText("معاينة النسخة");
            previewBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            previewBtn.setTypeface(getAppTypeface(true));
            previewBtn.setTextColor(getPrimaryTextColor());
            previewBtn.setBackground(createInteractiveCardDrawable(
                    isDarkMode ? 0xFF1E293B : 0xFFE2E8F0, getSubtleBorderColor(), 1, 10
            ));
            applySpringTouch(previewBtn);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            pLp.leftMargin = dp(6);
            pLp.bottomMargin = dp(3);
            previewBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (parentDialogHolder[0] != null) parentDialogHolder[0].dismiss();
                    showBackupPreviewDialog(item, b);
                }
            });

            Button restoreBtn = new Button(this);
            restoreBtn.setTag("btn");
            restoreBtn.setAllCaps(false);
            restoreBtn.setText("استعادة هذه النسخة");
            restoreBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            restoreBtn.setTypeface(getAppTypeface(true));
            restoreBtn.setTextColor(0xFFFFFFFF);
            restoreBtn.setBackground(createLuxuryButtonDrawable(getThemePrimaryColor(), 10));
            applySpringTouch(restoreBtn);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            rLp.rightMargin = dp(6);
            rLp.bottomMargin = dp(3);
            restoreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (parentDialogHolder[0] != null) parentDialogHolder[0].dismiss();
                    performSilentSmartRestore(item, b);
                }
            });

            actionsRow.addView(restoreBtn, rLp);
            actionsRow.addView(previewBtn, pLp);
            row.addView(actionsRow);

            listContainer.addView(row, rowLp);
        }

        parentDialogHolder[0] = showStyledDialog(
                "النسخ الآمنة — " + item.displayName,
                "عدد النسخ المحفوظة: " + backups.size() + " (الاستعادة الذكية تحفظ تعديلاتك الحالية تلقائياً)",
                scroll,
                null, 0, null,
                "إغلاق", null
        );
    }

    public void showBackupPreviewDialog(final NoteItem item, final BackupItem backup) {
        final String text = readTextFromUri(backup.uri);

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(createRoundedDrawable(getPaperBgColor(), getSubtleBorderColor(), 1, 14));

        TextView contentTv = new TextView(this);
        contentTv.setText(text.isEmpty() ? "(نسخة فارغة)" : text);
        contentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        contentTv.setTypeface(getAppTypeface(false));
        contentTv.setTextColor(getPrimaryTextColor());
        contentTv.setLineSpacing(0f, 1.35f);
        contentTv.setTextIsSelectable(true);
        box.addView(contentTv);
        sv.addView(box);

        showStyledDialog(
                "معاينة نسخة آمنة",
                backup.displayDate,
                sv,
                "استعادة هذه النسخة",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        performSilentSmartRestore(item, backup);
                    }
                },
                "رجوع",
                new Runnable() {
                    @Override
                    public void run() {
                        showSafeBackupsDialog(item);
                    }
                }
        );
    }

    // ===== إنشاء ملف جديد وإدارة العمليات السبع على الملفات =====
    public void showCreateNewNoteDialog() {
        if (!hasValidFolderPermission()) {
            showToast("يرجى اختيار مجلد لحفظ الملفات أولاً");
            pickFolder();
            return;
        }

        final EditText nameInput = createStyledEditText("", "اكتب اسم الملف الجديد...");
        nameInput.setSingleLine(true);

        showStyledDialog(
                "إنشاء ملف نصي جديد",
                "سيتم إنشاء ملف (.txt) حقيقي بترميز UTF-8 داخل مجلدك المختار",
                nameInput,
                "إنشاء وفتح",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        String desired = nameInput.getText().toString().trim();
                        if (desired.isEmpty()) {
                            desired = "ملاحظة جديدة";
                        }
                        createNewNoteAndOpen(desired);
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
        showKeyboard(nameInput);
    }

    public void createNewNoteAndOpen(String desiredName) {
        try {
            String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId);
            String finalFileName = generateUniqueTxtFileName(rootDocId, desiredName);

            Uri createdUri = DocumentsContract.createDocument(
                    getContentResolver(),
                    rootDocUri,
                    "text/plain",
                    finalFileName
            );
            if (createdUri != null) {
                writeTextToUri(createdUri, "");
                isEditMode = true;
                openNoteDocument(createdUri, finalFileName, "");
            } else {
                showToast("تعذر إنشاء الملف الجديد");
            }
        } catch (Exception e) {
            showToast("حدث خطأ أثناء إنشاء الملف");
        }
    }

    // ===== قائمة الثلاث نقاط (⋮) بالخيارات الـ 7 بالترتيب الحرفي المعتمد =====
    public void showFileOptionsMenu(final NoteItem item) {
        if (item == null) return;

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(18), dp(18), dp(18), dp(22));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getSubtleBorderColor(), 1, 24));
        card.setElevation(dp(12));

        // رأس القائمة باسم الملف
        TextView headerTitle = new TextView(this);
        headerTitle.setText(item.displayName);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        headerTitle.setTypeface(getAppTypeface(true));
        headerTitle.setTextColor(getPrimaryTextColor());
        headerTitle.setGravity(Gravity.RIGHT);
        card.addView(headerTitle);

        TextView headerSub = new TextView(this);
        headerSub.setText("إدارة الملف والنسخ الآمنة");
        headerSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        headerSub.setTypeface(getAppTypeface(false));
        headerSub.setTextColor(getSecondaryTextColor());
        headerSub.setGravity(Gravity.RIGHT);
        headerSub.setPadding(0, dp(2), 0, dp(12));
        card.addView(headerSub);

        final boolean currentlyPinned = pinnedFileNames.contains(item.fileName)
                || pinnedFileNames.contains(item.displayName);

        String[] optionLabels = new String[]{
                currentlyPinned ? "إلغاء تثبيت الملف" : "تثبيت الملف",
                "إعادة تسمية الملف",
                "إنشاء نسخة مكررة من الملف",
                "إنشاء نسخة آمنة",
                "تصفح النسخ الآمنة",
                "مشاركة الملف",
                "حذف الملف"
        };

        String[] optionIcons = new String[]{
                "📌",
                "✎",
                "❐",
                "🛡",
                "🗂",
                "↗",
                "🗑"
        };

        for (int i = 0; i < optionLabels.length; i++) {
            final int index = i;
            boolean isDeleteOption = (i == 6);

            LinearLayout optRow = new LinearLayout(this);
            optRow.setOrientation(LinearLayout.HORIZONTAL);
            optRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            optRow.setGravity(Gravity.CENTER_VERTICAL);
            optRow.setPadding(dp(14), dp(12), dp(14), dp(12));

            int rowBg = isDeleteOption
                    ? (isDarkMode ? 0x33DC2626 : 0xFFFEE2E2)
                    : (isDarkMode ? 0xFF0F172A : 0xFFF8FAFC);
            int rowBorder = isDeleteOption ? 0x55DC2626 : getSubtleBorderColor();
            optRow.setBackground(createInteractiveCardDrawable(rowBg, rowBorder, 1, 14));
            applySpringTouch(optRow);

            LinearLayout.LayoutParams optLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            optLp.bottomMargin = dp(7);

            TextView iconTv = new TextView(this);
            iconTv.setText(optionIcons[i]);
            iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            iconTv.setGravity(Gravity.CENTER);
            iconTv.setTextColor(isDeleteOption ? 0xFFDC2626 : getThemePrimaryColor());
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT);
            iconLp.leftMargin = dp(10);
            optRow.addView(iconTv, iconLp);

            TextView labelTv = new TextView(this);
            labelTv.setText(optionLabels[i]);
            labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            labelTv.setTypeface(getAppTypeface(true));
            labelTv.setTextColor(isDeleteOption ? 0xFFDC2626 : getPrimaryTextColor());
            labelTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            );
            optRow.addView(labelTv, labelLp);

            optRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    handleFileOptionSelection(item, index);
                }
            });

            card.addView(optRow, optLp);
        }

        applyFontToViewTree(card);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        wrapper.setPadding(dp(16), dp(16), dp(16), dp(16));
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

    private void handleFileOptionSelection(NoteItem item, int optionIndex) {
        switch (optionIndex) {
            case 0:
                togglePinFile(item);
                break;
            case 1:
                showRenameFileDialog(item, null);
                break;
            case 2:
                duplicateFile(item);
                break;
            case 3:
                String liveText = readTextFromUri(item.uri);
                createSafeBackup(item.fileName, liveText, true);
                break;
            case 4:
                showSafeBackupsDialog(item);
                break;
            case 5:
                showShareOptionsDialog(item.displayName, readTextFromUri(item.uri), item.uri);
                break;
            case 6:
                showDeleteFileConfirmationDialog(item);
                break;
        }
    }

    public void togglePinFile(NoteItem item) {
        if (item == null) return;
        boolean wasPinned = pinnedFileNames.contains(item.fileName) || pinnedFileNames.contains(item.displayName);
        pinnedFileNames.remove(item.fileName);
        pinnedFileNames.remove(item.displayName);
        if (!wasPinned) {
            pinnedFileNames.add(item.fileName);
            showToast("تم تثبيت الملف في أعلى القائمة 📌");
        } else {
            showToast("تم إلغاء تثبيت الملف");
        }
        savePinnedFiles();
        refreshHomeFilesList();
    }

    public void showRenameFileDialog(final NoteItem item, final Runnable onRenamedCallback) {
        if (item == null) return;
        final String currentCleanName = stripTxtExtension(item.fileName);
        final EditText input = createStyledEditText(currentCleanName, "اسم الملف الجديد...");
        input.setSingleLine(true);

        showStyledDialog(
                "إعادة تسمية الملف",
                "سيتم تحديث اسم الملف وجميع نسخه الآمنة المرتبطة به تلقائياً",
                input,
                "حفظ الاسم",
                getThemePrimaryColor(),
                new Runnable() {
                    @Override
                    public void run() {
                        hideKeyboard();
                        String newRaw = input.getText().toString().trim();
                        performRenameFile(item, newRaw, onRenamedCallback);
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
        showKeyboard(input);
    }

    public void performRenameFile(NoteItem item, String desiredRawName, Runnable onRenamedCallback) {
        if (item == null || !hasValidFolderPermission()) return;
        String cleanNewBase = sanitizeFileName(stripTxtExtension(desiredRawName));
        if (cleanNewBase.isEmpty()) {
            showToast("يرجى إدخال اسم صالح للملف");
            return;
        }

        String oldFileName = item.fileName;
        String oldDisplayName = item.displayName;
        String newFileName = cleanNewBase + ".txt";

        if (newFileName.equalsIgnoreCase(oldFileName)) {
            return;
        }

        String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
        if (findChildDocumentByName(rootDocId, newFileName) != null) {
            showToast("يوجد ملف آخر بهذا الاسم بالفعل");
            return;
        }

        boolean wasPinned = pinnedFileNames.contains(oldFileName) || pinnedFileNames.contains(oldDisplayName);
        Uri newUri = null;

        try {
            newUri = DocumentsContract.renameDocument(getContentResolver(), item.uri, newFileName);
        } catch (Exception ignored) {
        }

        // خطة بديلة مضمونة إذا فشل renameDocument في بعض الهواتف
        if (newUri == null) {
            try {
                String existingContent = readTextFromUri(item.uri);
                Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId);
                Uri created = DocumentsContract.createDocument(
                        getContentResolver(), rootDocUri, "text/plain", newFileName
                );
                if (created != null && writeTextToUri(created, existingContent)) {
                    DocumentsContract.deleteDocument(getContentResolver(), item.uri);
                    newUri = created;
                }
            } catch (Exception ignored) {
            }
        }

        if (newUri != null) {
            // الحفاظ على حالة التثبيت حتى بعد تغير الـ URI والاسم
            if (wasPinned) {
                pinnedFileNames.remove(oldFileName);
                pinnedFileNames.remove(oldDisplayName);
                pinnedFileNames.add(newFileName);
                savePinnedFiles();
            }

            // إعادة تسمية جميع النسخ الآمنة المرتبطة بهذا الملف داخل خزنة_النسخ_الآمنة
            renameSafeBackupsForFile(oldFileName, newFileName);

            item.uri = newUri;
            item.fileName = newFileName;
            item.displayName = cleanNewBase;

            if (isInDocumentScreen && currentNoteUri != null) {
                currentNoteUri = newUri;
                currentNoteName = newFileName;
                if (docTitleTextView != null) {
                    docTitleTextView.setText(cleanNewBase);
                }
            } else {
                refreshHomeFilesList();
            }

            if (onRenamedCallback != null) {
                onRenamedCallback.run();
            }
            showToast("تم تغيير اسم الملف بنجاح");
        } else {
            showToast("تعذر إعادة تسمية الملف");
        }
    }

    public void duplicateFile(NoteItem item) {
        if (item == null || !hasValidFolderPermission()) return;
        try {
            String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId);
            String desiredCopyName = item.displayName + " - نسخة";
            String uniqueName = generateUniqueTxtFileName(rootDocId, desiredCopyName);

            Uri copyUri = DocumentsContract.createDocument(
                    getContentResolver(), rootDocUri, "text/plain", uniqueName
            );
            if (copyUri != null) {
                String content = readTextFromUri(item.uri);
                writeTextToUri(copyUri, content);
                refreshHomeFilesList();
                showToast("تم إنشاء نسخة مكررة من الملف");
            } else {
                showToast("تعذر تكرار الملف");
            }
        } catch (Exception e) {
            showToast("حدث خطأ أثناء تكرار الملف");
        }
    }

    // ===== حذف الملف (باللون الأحمر الصريح #DC2626 + نسخة آمنة تلقائية قبل الحذف) =====
    public void showDeleteFileConfirmationDialog(final NoteItem item) {
        if (item == null) return;
        showStyledDialog(
                "تأكيد حذف الملف",
                "هل أنت متأكد من حذف الملف «" + item.displayName + "»؟\n(سيقوم التطبيق تلقائياً بحفظ نسخة آمنة منه في الخزنة قبل الحذف لحمايته من الضياع).",
                null,
                "حذف الملف",
                0xFFDC2626, // لون أحمر صريح بغض النظر عن الثيم
                new Runnable() {
                    @Override
                    public void run() {
                        performDeleteFileWithSafetyBackup(item);
                    }
                },
                "إلغاء",
                null
        );
    }

    public void performDeleteFileWithSafetyBackup(NoteItem item) {
        if (item == null || item.uri == null) return;
        try {
            // إنشاء نسخة آمنة صامتة قبل الحذف الفعلي
            String contentBeforeDelete = readTextFromUri(item.uri);
            createSafeBackup(item.fileName, contentBeforeDelete, false);

            boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), item.uri);
            if (deleted) {
                pinnedFileNames.remove(item.fileName);
                pinnedFileNames.remove(item.displayName);
                savePinnedFiles();
                refreshHomeFilesList();
                showToast("تم حذف الملف (وحُفظت نسخة وقائية في الخزنة)");
            } else {
                showToast("تعذر حذف الملف");
            }
        } catch (Exception e) {
            showToast("حدث خطأ أثناء حذف الملف");
        }
    }

    // ===== مشاركة الملف (نص مباشر / ملف TXT / بطاقة اقتباس وسم الفاخرة) =====
    public void showShareOptionsDialog(final String title, final String content, final Uri fileUri) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setClipChildren(false);
        box.setClipToPadding(false);

        final Dialog[] holder = new Dialog[1];

        String[] labels = new String[]{
                "مشاركة كنص مكتوب",
                "مشاركة كملف نصي (.txt)",
                "مشاركة كبطاقة اقتباس فاخرة (صورة)"
        };
        String[] descriptions = new String[]{
                "إرسال محتوى المستند مباشرة عبر واتساب أو البريد أو الملاحظات",
                "إرفاق ملف .txt الأصلي للمشاركة أو الحفظ السحابي",
                "توليد بطاقة بصرية فاخرة بتصميم الثيم النشط جاهزة للنشر"
        };

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.VERTICAL);
            itemRow.setPadding(dp(14), dp(12), dp(14), dp(12));
            itemRow.setBackground(createInteractiveCardDrawable(
                    isDarkMode ? 0xFF0F172A : 0xFFF8FAFC, getSubtleBorderColor(), 1, 14
            ));
            applySpringTouch(itemRow);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = dp(8);

            TextView t1 = new TextView(this);
            t1.setText(labels[i]);
            t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            t1.setTypeface(getAppTypeface(true));
            t1.setTextColor(getPrimaryTextColor());
            itemRow.addView(t1);

            TextView t2 = new TextView(this);
            t2.setText(descriptions[i]);
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            t2.setTypeface(getAppTypeface(false));
            t2.setTextColor(getSecondaryTextColor());
            t2.setPadding(0, dp(3), 0, 0);
            itemRow.addView(t2);

            itemRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder[0] != null) holder[0].dismiss();
                    if (idx == 0) {
                        sharePlainText(title, content);
                    } else if (idx == 1) {
                        shareTxtDocumentUri(title, fileUri, content);
                    } else {
                        shareLuxuryQuoteCard(title, content);
                    }
                }
            });

            box.addView(itemRow, lp);
        }

        holder[0] = showStyledDialog(
                "مشاركة «" + title + "»",
                "اختر صيغة المشاركة المناسبة:",
                box,
                null, 0, null,
                "إغلاق", null
        );
    }

    public void sharePlainText(String title, String content) {
        try {
            String body = (content != null && !content.isEmpty()) ? content : title;
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            sendIntent.putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(sendIntent, "مشاركة النص عبر"));
        } catch (Exception e) {
            showToast("تعذر فتح نافذة المشاركة");
        }
    }

    public void shareTxtDocumentUri(String title, Uri fileUri, String fallbackContent) {
        try {
            if (fileUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "مشاركة ملف .txt عبر"));
            } else {
                sharePlainText(title, fallbackContent);
            }
        } catch (Exception e) {
            sharePlainText(title, fallbackContent);
        }
    }

    // ===== النسخ الاحتياطي الشامل بصيغة (ZIP) =====
    public void startFullZipBackup() {
        if (!hasValidFolderPermission()) {
            showToast("يرجى ربط مجلد الملفات أولاً");
            pickFolder();
            return;
        }
        List<NoteItem> notes = loadAllNotesFromFolder();
        if (notes.isEmpty()) {
            showToast("لا توجد ملفات نصية لضغطها في النسخة الاحتياطية");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);
            String zipName = BRAND_NAME + "_Backup_" + sdf.format(new Date()) + ".zip";
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, zipName);
            startActivityForResult(intent, REQ_CREATE_ZIP);
        } catch (Exception e) {
            // في حال عدم توفر منتقي حفظ خارجي، نحفظ ملف الـ ZIP مباشرة داخل المجلد المختار
            createZipDirectlyInsideFolder();
        }
    }

    public void performZipExportToUri(Uri targetZipUri) {
        if (targetZipUri == null) return;
        OutputStream os = null;
        ZipOutputStream zos = null;
        try {
            List<NoteItem> notes = loadAllNotesFromFolder();
            os = getContentResolver().openOutputStream(targetZipUri);
            if (os == null) {
                showToast("تعذر فتح ملف الـ ZIP للكتابة");
                return;
            }
            zos = new ZipOutputStream(new BufferedOutputStream(os));
            int count = 0;

            for (NoteItem note : notes) {
                String text = readTextFromUri(note.uri);
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                ZipEntry entry = new ZipEntry(note.fileName);
                zos.putNextEntry(entry);
                zos.write(bytes);
                zos.closeEntry();
                count++;
            }

            zos.finish();
            zos.flush();
            showToast("تم تصدير النسخة الاحتياطية الشاملة (" + count + " ملفات) بنجاح");
        } catch (Exception e) {
            showToast("حدث خطأ أثناء إنشاء ملف ZIP");
        } finally {
            try {
                if (zos != null) zos.close();
                if (os != null) os.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void createZipDirectlyInsideFolder() {
        try {
            String rootDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String zipName = BRAND_NAME + "_Backup_" + sdf.format(new Date()) + ".zip";
            Uri zipUri = DocumentsContract.createDocument(
                    getContentResolver(), rootDocUri, "application/zip", zipName
            );
            if (zipUri != null) {
                performZipExportToUri(zipUri);
            }
        } catch (Exception e) {
            showToast("تعذر إنشاء ملف ZIP");
        }
    }
// ===== END PART 02/05 =====
