// ===== MAZWD PART 01/05: CORE — v2.7.1 =====
package com.mazwd.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    // ===== ثوابت الهوية التجارية (قابلة للتعديل الفوري مستقبلاً) =====
    public static final String BRAND_NAME = "وسم";
    public static final String DEFAULT_HERO_TITLE = "وَسم — مفكرة النخبة";
    public static final String DEFAULT_HERO_SUBTITLE = "مساحتك الفاخرة للتدوين وحفظ الأفكار";
    public static final String APP_VERSION_NAME = "v2.7.1";

    // ===== ثوابت التخزين والخزنة الآمنة =====
    public static final String PREFS_NAME = "mazwd_notepad_prefs";
    public static final String VAULT_FOLDER_NAME = "خزنة_النسخ_الآمنة";
    public static final String SAFE_BACKUP_MARKER = "__نسخة_آمنة_";
    public static final int REQ_OPEN_FOLDER = 1001;
    public static final int REQ_CREATE_ZIP = 1002;
    public static final long AUTO_SAVE_DELAY_MS = 600L;

    // ===== الأدوات الـ 12 الافتراضية للتحرير =====
    public static final String[] DEFAULT_TOOLBAR_TOOLS = new String[]{
            "undo", "redo", "copy", "paste", "select_all", "auto_format",
            "signature", "cut", "select_up", "select_down", "datetime", "find_replace"
    };

    // ===== الثيمات الخليجية والملكية الـ 8 =====
    public static final String[] THEME_NAMES = new String[]{
            "الأخضر الزمردي (السعودي الملكي)",
            "العنابي الفاخر (القطري الأصيل)",
            "البني التراثي (العود الخليجي)",
            "الطيني العربي (الأصالة التراثية)",
            "البترولي العصري (لؤلؤ الخليج)",
            "البنفسجي الأنيق (الليلك الملكي)",
            "الرمادي الرسمي (الديوان الرسمي)",
            "الأسود الماسي (الفخامة المطلقة)"
    };

    // [0]=Primary, [1]=Dark, [2]=LightAccent
    public static final int[][] THEME_PALETTES = new int[][]{
            {0xFF065F46, 0xFF064E3B, 0xFFD1FAE5}, // 0: الأخضر الزمردي
            {0xFF881337, 0xFF4C0519, 0xFFFFE4E6}, // 1: العنابي الفاخر
            {0xFF78350F, 0xFF451A03, 0xFFFEF3C7}, // 2: البني التراثي
            {0xFF9A3412, 0xFF431407, 0xFFFFEDD5}, // 3: الطيني العربي
            {0xFF0F4C5C, 0xFF082F3A, 0xFFCCFBF1}, // 4: البترولي العصري
            {0xFF5B21B6, 0xFF2E1065, 0xFFEDE9FE}, // 5: البنفسجي الأنيق
            {0xFF334155, 0xFF0F172A, 0xFFE2E8F0}, // 6: الرمادي الرسمي
            {0xFF18181B, 0xFF09090B, 0xFFF4F4F5}  // 7: الأسود الماسي
    };

    // ===== الخطوط الـ 12 المدمجة =====
    public static final String[] FONT_NAMES = new String[]{
            "Sans — خط واضح وعصري (افتراضي)",
            "تجوال — خط حديث وأنيق للمقالات",
            "المراعي — خط عربي كلاسيكي مريح ومتوازن",
            "شهرزاد عريض — خط نسخي تراثي أصيل",
            "ريدكس برو — خط عصري فائق الوضوح",
            "نوتو سانس — خط جوجل القياسي المتوازن",
            "نوتو نسخ — خط نسخي مطبعي للأبحاث",
            "آي بي إم بليكس — خط تقني حديث وأنيق",
            "عارف رقعة — خط الرقعة العربي",
            "ريم كوفي — خط كوفي حديث ومميز",
            "Serif — خط إنجليزي رسمي ومريح",
            "Cursive — خط فني مائل"
    };

    // ===== المتغيرات والحقول المشتركة =====
    public SharedPreferences prefs;
    public Handler mainHandler = new Handler(Looper.getMainLooper());
    public Runnable autoSaveRunnable;
    public Uri treeUri;
    public FrameLayout rootContainer;

    // حالات الشاشات
    public boolean isInDocumentScreen = false;
    public boolean isInSettingsScreen = false;
    public boolean isEditMode = false;

    // متغيرات الشاشة الرئيسية
    public ScrollView homeScrollView;
    public LinearLayout homeFilesContainer;
    public EditText homeSearchInput;
    public String currentHomeSearchQuery = "";
    public int savedHomeScrollY = 0;
    public List<NoteItem> cachedNotesList = new ArrayList<>();

    // متغيرات شاشة المستند
    public Uri currentNoteUri;
    public String currentNoteName = "";
    public String currentNoteContent = "";
    public boolean isDirty = false;
    public float prevScrollFraction = 0f;
    public int docZoomPercent = 100;
    public ScrollView readScrollView;
    public ScrollView editScrollView;
    public TextView readTextView;
    public EditText noteEditText;
    public TextView docTitleTextView;
    public TextView zoomPercentBtn;
    public TextView statsBarTextView;
    public LinearLayout docTopBar;
    public LinearLayout docBottomArea;
    public View fastScrollTrack;
    public View fastScrollThumb;
    public TextView fastScrollBubble;
    public PopupWindow jumpPopupWindow;

    // سجل التراجع والإعادة وأدوات التحرير
    public ArrayList<String> undoStack = new ArrayList<>();
    public ArrayList<String> redoStack = new ArrayList<>();
    public boolean isInternalEditChange = false;
    public List<String> activeToolbarTools = new ArrayList<>();
    public Set<String> disabledToolbarTools = new HashSet<>();

    // إعدادات المظهر والتخصيص
    public int currentThemeIndex = 0;
    public boolean isDarkMode = false;
    public int currentFontIndex = 1; // تجوال افتراضياً لأناقة العرض
    public int baseFontSizeSp = 19;
    public float lineSpacingMultiplier = 1.45f;
    public boolean toolbarIconsOnly = false;
    public boolean autoFormatIndent = true;
    public boolean hapticEnabled = true;
    public boolean autoHideBarsOnScroll = false;
    public String userSignature = "";
    public String customHeaderTitle = "";
    public String customHeaderSubtitle = "";
    public Set<String> pinnedFileNames = new HashSet<>();

    // كاش الخطوط لتسريع الأداء (120fps)
    private final Map<String, Typeface> typefaceCache = new HashMap<>();

    // ===== نماذج البيانات الداخلية =====
    public static class NoteItem {
        public Uri uri;
        public String fileName;
        public String displayName;
        public long lastModified;
        public String content;
        public boolean isPinned;
    }

    public static class BackupItem {
        public Uri uri;
        public String fileName;
        public String displayDate;
        public long lastModified;
    }

    // ===== دورة حياة التطبيق (Lifecycle) =====
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadAllPreferences();

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        updateSystemBars();

        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootContainer.setBackgroundColor(getScreenBgColor());
        setContentView(rootContainer);

        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                saveCurrentNoteImmediatelyIfNeeded();
            }
        };

        showHomeScreen();
    }

    @Override
    protected void onPause() {
        saveCurrentNoteImmediatelyIfNeeded();
        if (!isInDocumentScreen && !isInSettingsScreen && homeScrollView != null) {
            savedHomeScrollY = homeScrollView.getScrollY();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        saveCurrentNoteImmediatelyIfNeeded();
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            saveCurrentNoteImmediatelyIfNeeded();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (jumpPopupWindow != null && jumpPopupWindow.isShowing()) {
            jumpPopupWindow.dismiss();
            return;
        }
        if (isInSettingsScreen) {
            isInSettingsScreen = false;
            showHomeScreen();
            return;
        }
        if (isInDocumentScreen) {
            saveCurrentNoteImmediatelyIfNeeded();
            if (isEditMode) {
                captureCurrentScrollFraction();
                hideKeyboard();
                isEditMode = false;
                renderDocumentScreen();
                return;
            }
            isInDocumentScreen = false;
            currentNoteUri = null;
            showHomeScreen();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri returnedUri = data.getData();
        if (requestCode == REQ_OPEN_FOLDER) {
            try {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(returnedUri, takeFlags);
            } catch (Exception ignored) {
            }
            treeUri = returnedUri;
            prefs.edit().putString("tree_uri", treeUri.toString()).apply();
            ensureVaultNoMedia();
            showToast("تم ربط مجلد الملفات بنجاح");
            showHomeScreen();
        } else if (requestCode == REQ_CREATE_ZIP) {
            performZipExportToUri(returnedUri);
        }
    }

    // ===== تحميل وحفظ الإعدادات =====
    public void loadAllPreferences() {
        currentThemeIndex = prefs.getInt("theme_index", 0);
        if (currentThemeIndex < 0 || currentThemeIndex >= THEME_PALETTES.length) {
            currentThemeIndex = 0;
        }
        isDarkMode = prefs.getBoolean("dark_mode", false);
        currentFontIndex = prefs.getInt("font_index", 1);
        if (currentFontIndex < 0 || currentFontIndex >= FONT_NAMES.length) {
            currentFontIndex = 0;
        }
        baseFontSizeSp = prefs.getInt("base_font_size_sp", 19);
        lineSpacingMultiplier = prefs.getFloat("line_spacing_mult", 1.45f);
        docZoomPercent = prefs.getInt("doc_zoom_percent", 100);
        toolbarIconsOnly = prefs.getBoolean("toolbar_icons_only", false);
        autoFormatIndent = prefs.getBoolean("auto_format_indent", true);
        hapticEnabled = prefs.getBoolean("haptic_enabled", true);
        autoHideBarsOnScroll = prefs.getBoolean("auto_hide_bars", false);
        userSignature = prefs.getString("user_signature", "");
        customHeaderTitle = prefs.getString("custom_header_title", DEFAULT_HERO_TITLE);
        customHeaderSubtitle = prefs.getString("custom_header_subtitle", DEFAULT_HERO_SUBTITLE);

        String uriStr = prefs.getString("tree_uri", null);
        if (uriStr != null && !uriStr.isEmpty()) {
            try {
                treeUri = Uri.parse(uriStr);
            } catch (Exception e) {
                treeUri = null;
            }
        }

        pinnedFileNames.clear();
        Set<String> savedPins = prefs.getStringSet("pinned_files", null);
        if (savedPins != null) {
            pinnedFileNames.addAll(savedPins);
        }

        loadToolbarConfiguration();
    }

    public void savePinnedFiles() {
        prefs.edit().putStringSet("pinned_files", new HashSet<>(pinnedFileNames)).apply();
    }

    public void loadToolbarConfiguration() {
        activeToolbarTools.clear();
        disabledToolbarTools.clear();

        String orderCsv = prefs.getString("toolbar_order_csv", null);
        if (orderCsv != null && !orderCsv.trim().isEmpty()) {
            String[] parts = orderCsv.split(",");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty() && !activeToolbarTools.contains(clean)) {
                    activeToolbarTools.add(clean);
                }
            }
        }
        for (String defTool : DEFAULT_TOOLBAR_TOOLS) {
            if (!activeToolbarTools.contains(defTool)) {
                activeToolbarTools.add(defTool);
            }
        }

        Set<String> savedDisabled = prefs.getStringSet("toolbar_disabled_set", null);
        if (savedDisabled != null) {
            disabledToolbarTools.addAll(savedDisabled);
        }
    }

    public void saveToolbarConfiguration() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < activeToolbarTools.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(activeToolbarTools.get(i));
        }
        prefs.edit()
                .putString("toolbar_order_csv", sb.toString())
                .putStringSet("toolbar_disabled_set", new HashSet<>(disabledToolbarTools))
                .apply();
    }

    // ===== محرك الثيمات والألوان الفاخرة =====
    public int getThemePrimaryColor() {
        return THEME_PALETTES[currentThemeIndex][0];
    }

    public int getThemeDarkColor() {
        return THEME_PALETTES[currentThemeIndex][1];
    }

    public int getThemeLightAccentColor() {
        return THEME_PALETTES[currentThemeIndex][2];
    }

    public int getScreenBgColor() {
        return isDarkMode ? 0xFF0F172A : 0xFFF3F5F8;
    }

    public int getCardBgColor() {
        return isDarkMode ? 0xFF1E293B : 0xFFFFFFFF;
    }

    public int getPaperBgColor() {
        return isDarkMode ? 0xFF131C2E : 0xFFFFFDF9;
    }

    public int getPrimaryTextColor() {
        return isDarkMode ? 0xFFF8FAFC : 0xFF0F172A;
    }

    public int getSecondaryTextColor() {
        return isDarkMode ? 0xFF94A3B8 : 0xFF64748B;
    }

    public int getSubtleBorderColor() {
        return isDarkMode ? 0xFF334155 : 0xFFE2E8F0;
    }

    public int getLuxuryAccentBorderColor() {
        int primary = getThemePrimaryColor();
        return (primary & 0x00FFFFFF) | (isDarkMode ? 0x44000000 : 0x22000000);
    }

    @SuppressWarnings("deprecation")
    public void updateSystemBars() {
        try {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getThemeDarkColor());
            window.setNavigationBarColor(isDarkMode ? 0xFF0F172A : 0xFFFFFFFF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View decor = window.getDecorView();
                int flags = decor.getSystemUiVisibility();
                if (!isDarkMode) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                decor.setSystemUiVisibility(flags);
            }
        } catch (Exception ignored) {
        }
    }

    // ===== محرك الخطوط الـ 12 + معالجة نقاط حرف الياء (ي) =====
    public Typeface getAppTypeface(boolean bold) {
        String key = currentFontIndex + "_" + (bold ? "b" : "r");
        if (typefaceCache.containsKey(key)) {
            return typefaceCache.get(key);
        }

        Typeface tf = null;
        try {
            switch (currentFontIndex) {
                case 0:
                    tf = Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
                    break;
                case 1:
                    tf = loadFontResource(bold ? "tajawal_bold" : "tajawal_regular", bold);
                    break;
                case 2:
                    tf = loadFontResource(bold ? "mazwd_arabic_bold" : "mazwd_arabic", bold);
                    break;
                case 3:
                    tf = loadFontResource("scheherazade_bold", bold);
                    break;
                case 4:
                    tf = loadFontResource(bold ? "readex_pro_bold" : "readex_pro", bold);
                    break;
                case 5:
                    tf = loadFontResource(bold ? "noto_sans_arabic_bold" : "noto_sans_arabic", bold);
                    break;
                case 6:
                    tf = loadFontResource(bold ? "noto_naskh_arabic_bold" : "noto_naskh_arabic", bold);
                    break;
                case 7:
                    tf = loadFontResource(bold ? "ibm_plex_arabic_bold" : "ibm_plex_arabic", bold);
                    break;
                case 8:
                    tf = loadFontResource(bold ? "aref_ruqaa_bold" : "aref_ruqaa", bold);
                    break;
                case 9:
                    tf = loadFontResource(bold ? "reem_kufi_bold" : "reem_kufi", bold);
                    break;
                case 10:
                    tf = Typeface.create("serif", bold ? Typeface.BOLD : Typeface.NORMAL);
                    break;
                case 11:
                    tf = Typeface.create("cursive", bold ? Typeface.BOLD : Typeface.NORMAL);
                    break;
                default:
                    tf = Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
                    break;
            }
        } catch (Exception ignored) {
        }

        if (tf == null) {
            tf = Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
        }
        typefaceCache.put(key, tf);
        return tf;
    }

    private Typeface loadFontResource(String fontResName, boolean bold) {
        try {
            int resId = getResources().getIdentifier(fontResName, "font", getPackageName());
            if (resId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Typeface base = getResources().getFont(resId);
                if (base != null) {
                    return Typeface.create(base, bold ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        } catch (Exception ignored) {
        }
        return Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    public void applyFontToViewTree(View view) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFontToViewTree(vg.getChildAt(i));
            }
        } else if (view instanceof TextView) {
            TextView tv = (TextView) view;
            boolean isBold = tv.getTypeface() != null && tv.getTypeface().isBold();
            tv.setTypeface(getAppTypeface(isBold));

            // قاعدة الدقة البصرية: إظهار نقطتي حرف الياء (ي) كاملة في العناوين والتسميات
            boolean isButtonOrBadge = (tv instanceof Button) || "badge".equals(tv.getTag()) || "btn".equals(tv.getTag());
            boolean isDocReadView = (tv == readTextView);
            if (!isButtonOrBadge && !isDocReadView && currentFontIndex != 3) {
                tv.setIncludeFontPadding(true);
            }
        }
    }

    // ===== محرك الفخامة البصرية والحركة الانسيابية (Luxury UI & Haptics) =====
    public int dp(float value) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, dm));
    }

    public void performHaptic(View view) {
        if (!hapticEnabled) return;
        try {
            if (view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } else {
                Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vib != null && vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
                    } else {
                        vib.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void applySpringTouch(final View view) {
        if (view == null) return;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(85)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(190)
                            .setInterpolator(new OvershootInterpolator(2.2f))
                            .start();
                    if (action == MotionEvent.ACTION_UP) {
                        performHaptic(v);
                    }
                }
                return false;
            }
        });
    }

    public GradientDrawable createRoundedDrawable(int fillColor, int strokeColor, int strokeWidthDp, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeWidthDp > 0 && strokeColor != 0) {
            gd.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return gd;
    }

    public Drawable createInteractiveCardDrawable(int fillColor, int strokeColor, int strokeWidthDp, float radiusDp) {
        GradientDrawable content = createRoundedDrawable(fillColor, strokeColor, strokeWidthDp, radiusDp);
        GradientDrawable mask = createRoundedDrawable(0xFFFFFFFF, 0, 0, radiusDp);
        int rippleColor = (getThemePrimaryColor() & 0x00FFFFFF) | 0x22000000;
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    public Drawable createLuxuryButtonDrawable(int bgColor, float radiusDp) {
        GradientDrawable base = new GradientDrawable();
        base.setShape(GradientDrawable.RECTANGLE);
        base.setColor(bgColor);
        base.setCornerRadius(dp(radiusDp));
        base.setStroke(dp(1), 0x22FFFFFF);

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setColor(0xFFFFFFFF);
        mask.setCornerRadius(dp(radiusDp));

        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), base, mask);
    }

    public GradientDrawable createHeroHeaderDrawable() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{getThemeDarkColor(), getThemePrimaryColor()}
        );
        float r = dp(24);
        gd.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        return gd;
    }

    public void showToast(String message) {
        try {
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            TextView tv = new TextView(this);
            tv.setText(message);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTypeface(getAppTypeface(true));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(20), dp(12), dp(20), dp(12));
            tv.setBackground(createRoundedDrawable(isDarkMode ? 0xEE1E293B : getThemeDarkColor(), 0x33FFFFFF, 1, 22));
            tv.setElevation(dp(6));
            toast.setView(tv);
            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dp(85));
            toast.show();
        } catch (Exception e) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v == null) v = rootContainer;
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && v != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    public void showKeyboard(final View target) {
        if (target == null) return;
        target.requestFocus();
        target.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception ignored) {
                }
            }
        }, 120);
    }

    // ===== النوافذ المنبثقة الفاخرة (بدون أي اقتصاص للأزرار) =====
    public Dialog showStyledDialog(
            String title,
            String subtitle,
            View customBody,
            String positiveText,
            int positiveBgColor,
            final Runnable onPositive,
            String negativeText,
            final Runnable onNegative
    ) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        // حشوة سفلية إضافية +4dp لمنع اقتصاص الحواف السفلية للأزرار نهائياً
        card.setPadding(dp(20), dp(20), dp(20), dp(22));
        card.setBackground(createRoundedDrawable(getCardBgColor(), getSubtleBorderColor(), 1, 24));
        card.setElevation(dp(10));

        if (title != null && !title.isEmpty()) {
            TextView titleTv = new TextView(this);
            titleTv.setText(title);
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            titleTv.setTypeface(getAppTypeface(true));
            titleTv.setTextColor(getPrimaryTextColor());
            titleTv.setGravity(Gravity.RIGHT);
            titleTv.setIncludeFontPadding(true);
            card.addView(titleTv);
        }

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subTv = new TextView(this);
            subTv.setText(subtitle);
            subTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            subTv.setTypeface(getAppTypeface(false));
            subTv.setTextColor(getSecondaryTextColor());
            subTv.setGravity(Gravity.RIGHT);
            subTv.setIncludeFontPadding(true);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            subLp.topMargin = dp(6);
            card.addView(subTv, subLp);
        }

        if (customBody != null) {
            LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            bodyLp.topMargin = dp(14);
            bodyLp.bottomMargin = dp(6);
            card.addView(customBody, bodyLp);
        }

        if ((positiveText != null && !positiveText.isEmpty()) || (negativeText != null && !negativeText.isEmpty())) {
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            btnRow.setClipChildren(false);
            btnRow.setClipToPadding(false);
            btnRow.setPadding(dp(2), dp(6), dp(2), dp(4));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowLp.topMargin = dp(14);

            if (positiveText != null && !positiveText.isEmpty()) {
                Button posBtn = new Button(this);
                posBtn.setTag("btn");
                posBtn.setAllCaps(false);
                posBtn.setText(positiveText);
                posBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                posBtn.setTypeface(getAppTypeface(true));
                posBtn.setTextColor(0xFFFFFFFF);
                int btnColor = positiveBgColor != 0 ? positiveBgColor : getThemePrimaryColor();
                posBtn.setBackground(createLuxuryButtonDrawable(btnColor, 14));
                posBtn.setElevation(dp(2));
                applySpringTouch(posBtn);

                LinearLayout.LayoutParams posLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
                posLp.topMargin = dp(2);
                posLp.bottomMargin = dp(4);
                if (negativeText != null && !negativeText.isEmpty()) {
                    posLp.leftMargin = dp(6);
                }
                posBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (onPositive != null) onPositive.run();
                    }
                });
                btnRow.addView(posBtn, posLp);
            }

            if (negativeText != null && !negativeText.isEmpty()) {
                Button negBtn = new Button(this);
                negBtn.setTag("btn");
                negBtn.setAllCaps(false);
                negBtn.setText(negativeText);
                negBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                negBtn.setTypeface(getAppTypeface(true));
                negBtn.setTextColor(getPrimaryTextColor());
                int negBg = isDarkMode ? 0xFF334155 : 0xFFE2E8F0;
                negBtn.setBackground(createInteractiveCardDrawable(negBg, getSubtleBorderColor(), 1, 14));
                negBtn.setElevation(dp(1));
                applySpringTouch(negBtn);

                LinearLayout.LayoutParams negLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
                negLp.topMargin = dp(2);
                negLp.bottomMargin = dp(4);
                if (positiveText != null && !positiveText.isEmpty()) {
                    negLp.rightMargin = dp(6);
                }
                negBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (onNegative != null) onNegative.run();
                    }
                });
                btnRow.addView(negBtn, negLp);
            }

            card.addView(btnRow, rowLp);
        }

        applyFontToViewTree(card);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        wrapper.setPadding(dp(16), dp(16), dp(16), dp(16));
        wrapper.addView(card, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        dialog.setContentView(wrapper);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
        return dialog;
    }

    public EditText createStyledEditText(String initialText, String hintText) {
        EditText et = new EditText(this);
        et.setText(initialText != null ? initialText : "");
        et.setHint(hintText != null ? hintText : "");
        et.setHintTextColor(getSecondaryTextColor());
        et.setTextColor(getPrimaryTextColor());
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        et.setTypeface(getAppTypeface(false));
        et.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        et.setTextDirection(View.TEXT_DIRECTION_RTL);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        int fieldBg = isDarkMode ? 0xFF0F172A : 0xFFF8FAFC;
        et.setBackground(createRoundedDrawable(fieldBg, getSubtleBorderColor(), 1, 14));
        if (initialText != null) {
            et.setSelection(initialText.length());
        }
        return et;
    }

    // ===== محرك البحث العربي الذكي (findSmartMatchRanges) =====
    public static boolean isIgnoredArabicMark(char c) {
        // التشكيل الكامل (الحركات، التنوين، الشدة، السكون، الألف الخنجرية، علامات الوقف، التطويل/الكشيدة)
        return (c >= '\u064B' && c <= '\u065F')
                || c == '\u0670'
                || c == '\u0640'
                || (c >= '\u0610' && c <= '\u061A')
                || (c >= '\u06D6' && c <= '\u06ED');
    }

    public static char normalizeArabicChar(char c) {
        switch (c) {
            case 'أ':
            case 'إ':
            case 'آ':
            case 'ٱ':
                return 'ا';
            case 'ة':
                return 'ه';
            case 'ى':
                return 'ي';
            case 'ؤ':
                return 'و';
            case 'ئ':
                return 'ي';
            default:
                return Character.toLowerCase(c);
        }
    }

    public List<int[]> findSmartMatchRanges(String sourceText, String rawQuery) {
        List<int[]> ranges = new ArrayList<>();
        if (sourceText == null || sourceText.isEmpty() || rawQuery == null || rawQuery.isEmpty()) {
            return ranges;
        }

        // تطبيع عبارة البحث
        StringBuilder normQuerySb = new StringBuilder();
        for (int i = 0; i < rawQuery.length(); i++) {
            char c = rawQuery.charAt(i);
            if (!isIgnoredArabicMark(c)) {
                normQuerySb.append(normalizeArabicChar(c));
            }
        }
        String normQuery = normQuerySb.toString().trim();
        if (normQuery.isEmpty()) {
            return ranges;
        }

        // تطبيع النص الأصلي مع الاحتفاظ بخريطة المواقع الحقيقية
        StringBuilder normTextSb = new StringBuilder(sourceText.length());
        int[] indexMap = new int[sourceText.length()];
        int normLen = 0;

        for (int i = 0; i < sourceText.length(); i++) {
            char c = sourceText.charAt(i);
            if (!isIgnoredArabicMark(c)) {
                normTextSb.append(normalizeArabicChar(c));
                indexMap[normLen] = i;
                normLen++;
            }
        }

        String normText = normTextSb.toString();
        int qLen = normQuery.length();
        int searchFrom = 0;

        while (searchFrom <= normLen - qLen) {
            int foundIdx = normText.indexOf(normQuery, searchFrom);
            if (foundIdx < 0) break;

            int origStart = indexMap[foundIdx];
            int origEnd = indexMap[foundIdx + qLen - 1] + 1;

            // تضمين أي حركات تشكيل تابعة للحرف الأخير من الكلمة المطابقة
            while (origEnd < sourceText.length() && isIgnoredArabicMark(sourceText.charAt(origEnd))) {
                origEnd++;
            }

            ranges.add(new int[]{origStart, origEnd});
            searchFrom = foundIdx + qLen;
        }

        return ranges;
    }

    public SpannableStringBuilder buildHighlightedSpannable(String text, String query) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text != null ? text : "");
        if (text == null || text.isEmpty() || query == null || query.trim().isEmpty()) {
            return ssb;
        }
        List<int[]> matches = findSmartMatchRanges(text, query);
        for (int[] range : matches) {
            int start = Math.max(0, Math.min(range[0], ssb.length()));
            int end = Math.max(start, Math.min(range[1], ssb.length()));
            if (end > start) {
                ssb.setSpan(
                        new BackgroundColorSpan(0xFFFDE047),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                ssb.setSpan(
                        new ForegroundColorSpan(0xFF0F172A),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        return ssb;
    }

    public String formatTimestamp(long millis) {
        if (millis <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  •  hh:mm a", new Locale("ar"));
        return sdf.format(new Date(millis));
    }

    public String stripTxtExtension(String name) {
        if (name == null) return "";
        if (name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }
// ===== END PART 01/05 =====
