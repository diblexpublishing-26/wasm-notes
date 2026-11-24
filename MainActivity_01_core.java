أهلاً بك، ومعك كل الحق في كل ملاحظة ذكرتها؛ نقدك في مكانه تماماً.

دعني أوضح لك **الأسباب التقنية المباشرة** لما حدث في النسخة السابقة، وكيف تم حلها جذرياً الآن:

1. **لماذا ظهر أنه تحديث لتطبيق `Mazwd` وفشل التثبيت؟**
   - لأن المعرّف البرمجي (`Package Name`) بقي `com.mazwd.app`، وبما أن المستودع الجديد على GitHub أنشأ مفتاح توقيع جديد، رفض نظام أندرويد تثبيته فوق القديم.
   - **الحل الجذري:** تم تغيير المعرّف البرمجي بالكامل إلى **`com.wasm.app`**، ليصبح تطبيق **«وسم»** مستقلاً 100% بتوقيعه وأيقونته وبياناته، ويثبّت بجانب تطبيقك القديم دون أي تعارض أو طلب حذف.

2. **لماذا ظهرت الخطوط في الإعدادات بخط واحد؟**
   - لسببين: الأول أن دالة تعميم خط التطبيق (`applyAppFont`) كانت تطغى على أسماء الخطوط وتجبرها على الظهور بالخط الحالي، والثاني أن بعض روابط GitHub المباشرة للخطوط تحولت في مستودع جوجل إلى خطوط متغيرة (Variable Fonts) فكان السكربت ينسخ خط `Tajawal` مكانها احتياطياً!
   - **الحل الجذري:**
     - في ملف البناء (`main.yml`)، تم استبدال التحميل العشوائي بسكربت Python يجلب **12 خطاً عربياً أصلياً ومختلفاً كلياً** مباشرة من خوادم Google Fonts (`gstatic.com`) ويتحقق من بصمة كل ملف `.ttf`.
     - في شاشة الإعدادات، **تم إلغاء صندوق المعاينة نهائياً** (كما طلبت)، وعُرضت الخطوط الـ 12 مباشرة داخل شبكة أنيقة في الإعدادات بحيث **يُكتب اسم كل خط بخطه الحقيقي نفسه** مع قفل خاص (`KEEP_OWN_FONT`) يمنع تغييره.

3. **لماذا كانت شاشة الإعدادات تقفز للأعلى عند اختيار أي خيار؟**
   - لأن دالة `showSettingsScreen()` كانت تعيد بناء الشاشة من نقطة الصفر (`ScrollY = 0`).
   - **الحل الجذري:** تمت إضافة حفظ فوري لموضع التمرير (`savedSettingsScrollY`) واستعادته قبل رسم الإطار (`OnPreDrawListener`)، بحيث عندما تختار ثيماً أو خطاً أو تغير الحجم أو تفعّل أي زر **تبقى في مكانك تماماً دون أي قفزة**.

4. **لماذا كانت الأزرار (مثل التكبير والتصغير) كبيرة والواجهة شبيهة بالقديمة؟**
   - التزمتُ سابقاً بمقاسات الأزرار القديمة (`46dp` و`36dp`). الآن قمت بإعادة تصميم الواجهة بالكامل لتكون **واجهة فاخرة عصرية على طراز تطبيقات النخبة في iOS**:
     - كبسولة التكبير والتصغير `[ ＋ | 100% | － ]` أصبحت دقيقة ومجوهراتية بارتفاع **`26dp` فقط** وخط **`10.5sp`**.
     - أزرار شريط الأدوات والتنقل أصبحت رشيقة وانسيابية (`30dp` – `38dp`).
     - الشاشة الرئيسية صُممت بهوية فاخرة (بطاقة صدارة ملكية مع شريط إحصائيات زجاجي مدمج، فلاتر سريعة `[ الكل | 📌 المثبتة | 🕒 الأحدث | 🔤 أبجدياً ]`، وبطاقات ملاحظات تعرض مقتطفاً حياً من سطرين من داخل الملاحظة مع شريط جانبي مذهّب للملاحظات المثبتة).

---

### طريقة التحديث في مستودعك (6 ملفات):
استبدل محتوى الملفات الخمسة في جذر المستودع + ملف `.github/workflows/main.yml`:

---

### 1. الملف الأول: `MainActivity_01_core.java`
*(يحتوي على المعرّف الجديد `com.wasm.app`، نظام الثيمات الفاخرة، محرك الـ 12 خطاً مع حماية خط كل عنصر `KEEP_OWN_FONT`، وحفظ مواضع التمرير)*

```java
package com.wasm.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

    // =========================================================================
    // 1. BRANDING & APP CONSTANTS (WASM LUXURY EDITION)
    // =========================================================================
    public static final String BRAND_NAME = "وسم";
    public static final String BRAND_BADGE_TEXT = "✦  وَسْـم  •  WASM";
    public static final String DEFAULT_HERO_TITLE = "وَسْـم — ديوان الأفكار";
    public static final String DEFAULT_HERO_SUBTITLE = "مساحتك الخاصة للتدوين الفاخر وحفظ النصوص بأعلى درجات الأمان";
    public static final String VAULT_FOLDER_NAME = "خزنة_النسخ_الآمنة";
    public static final String BACKUP_SEPARATOR = "__نسخة_آمنة_";

    public static final String PREFS_NAME = "wasm_luxury_prefs_v3";
    public static final String KEY_FOLDER_URI = "folder_uri";
    public static final String KEY_DARK_MODE = "dark_mode";
    public static final String KEY_THEME_INDEX = "theme_index";
    public static final String KEY_FONT_INDEX = "font_index";
    public static final String KEY_BASE_FONT_SIZE = "base_font_size";
    public static final String KEY_DOC_ZOOM_PERCENT = "doc_zoom_percent";
    public static final String KEY_LINE_SPACING = "line_spacing";
    public static final String KEY_PINNED_FILES = "pinned_files";
    public static final String KEY_TOOLBAR_ORDER = "toolbar_order";
    public static final String KEY_TOOLBAR_HIDDEN = "toolbar_hidden";
    public static final String KEY_TOOLBAR_ICON_ONLY = "toolbar_icon_only";
    public static final String KEY_SIGNATURE_TEXT = "signature_text";
    public static final String KEY_INDENT_PARAGRAPHS = "indent_paragraphs";
    public static final String KEY_HAPTIC_ENABLED = "haptic_enabled";
    public static final String KEY_HERO_TITLE = "hero_title";
    public static final String KEY_HERO_SUBTITLE = "hero_subtitle";
    public static final String KEY_HOME_FILTER_MODE = "home_filter_mode";

    public static final int REQ_PICK_FOLDER = 1001;
    public static final int REQ_CREATE_ZIP_BACKUP = 1002;

    public static final int SCREEN_HOME = 0;
    public static final int SCREEN_DOCUMENT = 1;
    public static final int SCREEN_SETTINGS = 2;

    public static final int FILTER_ALL = 0;
    public static final int FILTER_PINNED = 1;
    public static final int FILTER_RECENT = 2;
    public static final int FILTER_ALPHA = 3;

    public static final String TAG_KEEP_OWN_FONT = "KEEP_OWN_FONT";

    // =========================================================================
    // 2. LUXURY GCC THEMES & 12 AUTHENTIC ARABIC FONTS
    // =========================================================================
    public static final String[] THEME_NAMES = new String[]{
            "الزمرد الملكي",
            "العنابي القطري",
            "العود المعتّق",
            "الرمال الذهبية",
            "لؤلؤ الخليج",
            "الخزامى النجدي",
            "الديوان الرسمي",
            "الكربون الفاخر"
    };

    // [0]=Primary, [1]=DeepGradient, [2]=SoftTintLight, [3]=GoldChampagneAccent
    public static final int[][] THEME_PALETTES = new int[][]{
            {0xFF065F46, 0xFF032E22, 0xFFECFDF5, 0xFFD4AF37}, // 0: Royal Saudi Emerald
            {0xFF7F1D32, 0xFF420917, 0xFFFFF1F2, 0xFFE2B86B}, // 1: Doha Velvet Maroon
            {0xFF6B3E1E, 0xFF361D0A, 0xFFFEF9EE, 0xFFD9A05B}, // 2: Aged Arabian Oud
            {0xFF8C3B1A, 0xFF431807, 0xFFFFF7ED, 0xFFE0A96D}, // 3: Diriyah Golden Sand
            {0xFF0E4D5C, 0xFF06252E, 0xFFF0FDFA, 0xFF7DD3C8}, // 4: Gulf Pearl Teal
            {0xFF4C1D95, 0xFF230B4E, 0xFFF5F3FF, 0xFFC4B5FD}, // 5: Najd Royal Lavender
            {0xFF283548, 0xFF0F172A, 0xFFF1F5F9, 0xFF94A3B8}, // 6: Official Diwan Slate
            {0xFF1C1C21, 0xFF09090C, 0xFFF4F4F6, 0xFFD4AF37}  // 7: Obsidian & Champagne Gold
    };

    public static final String[] FONT_NAMES = new String[]{
            "خط تجوال — العصري المتوازن",
            "خط المراعي — الخليجي الأنيق",
            "خط القاهرة — الهندسي الحديث",
            "الخط الأميري — المصحفي الفاخر",
            "خط شهرزاد — التراثي الأصيل",
            "خط ريدكس برو — الحكومي الواضح",
            "خط نوتو سانس — الرقمي المريح",
            "خط نوتو نسخ — الأدبي الكلاسيكي",
            "خط آي بي إم — التقني الفخم",
            "خط عارف رقعة — الديواني الأصيل",
            "خط ريم كوفي — الكوفي المزخرف",
            "خط تشانجا — العناوين العريضة"
    };

    public static final String[] FONT_SHORT_NAMES = new String[]{
            "تجوال (Tajawal)",
            "المراعي (Almarai)",
            "القاهرة (Cairo)",
            "الأميري (Amiri)",
            "شهرزاد (Scheherazade)",
            "ريدكس برو (Readex)",
            "نوتو سانس (Noto Sans)",
            "نوتو نسخ (Noto Naskh)",
            "آي بي إم (IBM Plex)",
            "عارف رقعة (Aref Ruqaa)",
            "ريم كوفي (Reem Kufi)",
            "تشانجا (Changa)"
    };

    public static final String[] DEFAULT_TOOLS = new String[]{
            "undo", "redo", "copy", "paste", "select_all", "auto_format",
            "signature", "cut", "select_up", "select_down", "datetime", "find_replace"
    };

    // =========================================================================
    // 3. STATE FIELDS & UI REFERENCES
    // =========================================================================
    public SharedPreferences prefs;
    public final Handler mainHandler = new Handler(Looper.getMainLooper());

    public Uri currentFolderUri = null;
    public boolean isDarkMode = false;
    public int currentThemeIndex = 0;
    public int currentFontIndex = 0;
    public float baseFontSizeSp = 17f;
    public int docZoomPercent = 100;
    public float lineSpacingMultiplier = 1.40f;
    public boolean toolbarIconOnly = false;
    public String signatureText = "";
    public boolean indentParagraphsOnFormat = false;
    public boolean hapticEnabled = true;
    public String heroTitleText = DEFAULT_HERO_TITLE;
    public String heroSubtitleText = DEFAULT_HERO_SUBTITLE;
    public int homeFilterMode = FILTER_ALL;

    public final Set<String> pinnedFileNames = new HashSet<>();
    public final List<String> toolbarOrderList = new ArrayList<>();
    public final Set<String> toolbarHiddenSet = new HashSet<>();

    // Dynamic Theme Colors
    public int colorPrimary;
    public int colorPrimaryDark;
    public int colorAccentLight;
    public int colorGoldAccent;
    public int colorBackground;
    public int colorCardSurface;
    public int colorSubtleSurface;
    public int colorTextPrimary;
    public int colorTextSecondary;
    public int colorTextMuted;
    public int colorBorder;
    public int colorDivider;
    public final int colorDanger = 0xFFDC2626;
    public final int colorSuccess = 0xFF059669;

    // Navigation & Scroll State
    public int currentScreen = SCREEN_HOME;
    public int savedHomeScrollY = 0;
    public int savedSettingsScrollY = 0;
    public String currentSearchQuery = "";

    // Home UI References
    public ScrollView homeScrollView;
    public LinearLayout filesContainerLayout;
    public EditText homeSearchEditText;
    public View homeSearchClearBtn;
    public TextView homeFilesCountBadge;
    public final List<NoteFileItem> cachedFileItems = new ArrayList<>();

    // Settings UI References
    public ScrollView settingsScrollView;

    // Document Screen State
    public NoteFileItem currentOpenNote = null;
    public boolean isEditMode = false;
    public boolean isDocumentDirty = false;
    public String lastSavedContent = "";
    public float prevScrollFraction = 0f;

    public ScrollView docScrollView;
    public TextView readTextView;
    public EditText editNoteEditText;
    public TextView docTitleTextView;
    public TextView zoomPercentTextView;
    public TextView bottomStatsTextView;
    public View fastScrollThumb;
    public TextView fastScrollBubble;
    public boolean isFastScrollDragging = false;

    // Undo / Redo History
    public final List<String> undoStack = new ArrayList<>();
    public final List<String> redoStack = new ArrayList<>();
    public boolean isInternalTextChange = false;

    // Font Cache
    public static final Map<String, Typeface> FONT_CACHE = new HashMap<>();

    // Debounced Auto-Save
    public final Runnable debouncedSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveCurrentNoteImmediatelyIfNeeded(false);
        }
    };

    // Data Model for Note Files
    public static class NoteFileItem {
        public Uri uri;
        public String fileName;
        public String displayName;
        public long lastModified;
        public long sizeBytes;
        public String cachedContent;
        public boolean isPinned;
    }

    // =========================================================================
    // 4. ACTIVITY LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadPreferences();
        computeThemeColors();
        configureSystemBars();

        if (hasValidFolderPermission()) {
            performSilentSmartRestore();
        }
        showHomeScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentScreen == SCREEN_HOME && homeScrollView != null) {
            savedHomeScrollY = homeScrollView.getScrollY();
        } else if (currentScreen == SCREEN_SETTINGS && settingsScrollView != null) {
            savedSettingsScrollY = settingsScrollView.getScrollY();
        } else if (currentScreen == SCREEN_DOCUMENT) {
            saveCurrentNoteImmediatelyIfNeeded(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (currentScreen == SCREEN_DOCUMENT) {
            saveCurrentNoteImmediatelyIfNeeded(true);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && currentScreen == SCREEN_DOCUMENT) {
            saveCurrentNoteImmediatelyIfNeeded(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == SCREEN_DOCUMENT) {
            saveCurrentNoteImmediatelyIfNeeded(true);
            showHomeScreen();
        } else if (currentScreen == SCREEN_SETTINGS) {
            savedSettingsScrollY = 0;
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                } catch (Exception ignored) {
                }
                currentFolderUri = treeUri;
                prefs.edit().putString(KEY_FOLDER_URI, treeUri.toString()).apply();
                ensureVaultNoMedia();
                performSilentSmartRestore();
                showToast("تم ربط مجلد الحفظ بنجاح ✓");
                if (currentScreen == SCREEN_SETTINGS) {
                    showSettingsScreen();
                } else {
                    showHomeScreen();
                }
            }
        } else if (requestCode == REQ_CREATE_ZIP_BACKUP && resultCode == Activity.RESULT_OK && data != null) {
            Uri zipDestUri = data.getData();
            if (zipDestUri != null) {
                performZipExportToUri(zipDestUri);
            }
        }
    }

    // =========================================================================
    // 5. PREFERENCES & STATE PERSISTENCE
    // =========================================================================
    public void loadPreferences() {
        String uriStr = prefs.getString(KEY_FOLDER_URI, null);
        if (!TextUtils.isEmpty(uriStr)) {
            try {
                currentFolderUri = Uri.parse(uriStr);
            } catch (Exception e) {
                currentFolderUri = null;
            }
        }
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        currentThemeIndex = clampInt(prefs.getInt(KEY_THEME_INDEX, 0), 0, THEME_PALETTES.length - 1);
        currentFontIndex = clampInt(prefs.getInt(KEY_FONT_INDEX, 0), 0, FONT_NAMES.length - 1);
        baseFontSizeSp = clampFloat(prefs.getFloat(KEY_BASE_FONT_SIZE, 17f), 12f, 30f);
        docZoomPercent = clampInt(prefs.getInt(KEY_DOC_ZOOM_PERCENT, 100), 50, 250);
        lineSpacingMultiplier = clampFloat(prefs.getFloat(KEY_LINE_SPACING, 1.40f), 1.0f, 2.2f);
        toolbarIconOnly = prefs.getBoolean(KEY_TOOLBAR_ICON_ONLY, false);
        signatureText = prefs.getString(KEY_SIGNATURE_TEXT, "");
        indentParagraphsOnFormat = prefs.getBoolean(KEY_INDENT_PARAGRAPHS, false);
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true);
        heroTitleText = prefs.getString(KEY_HERO_TITLE, DEFAULT_HERO_TITLE);
        heroSubtitleText = prefs.getString(KEY_HERO_SUBTITLE, DEFAULT_HERO_SUBTITLE);
        homeFilterMode = clampInt(prefs.getInt(KEY_HOME_FILTER_MODE, FILTER_ALL), 0, 3);

        pinnedFileNames.clear();
        Set<String> savedPins = prefs.getStringSet(KEY_PINNED_FILES, null);
        if (savedPins != null) {
            pinnedFileNames.addAll(savedPins);
        }

        toolbarOrderList.clear();
        String savedOrder = prefs.getString(KEY_TOOLBAR_ORDER, null);
        if (!TextUtils.isEmpty(savedOrder)) {
            String[] parts = savedOrder.split(",");
            Set<String> seen = new LinkedHashSet<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty() && isValidToolId(trimmed)) {
                    seen.add(trimmed);
                }
            }
            for (String def : DEFAULT_TOOLS) {
                if (!seen.contains(def)) {
                    seen.add(def);
                }
            }
            toolbarOrderList.addAll(seen);
        } else {
            toolbarOrderList.addAll(Arrays.asList(DEFAULT_TOOLS));
        }

        toolbarHiddenSet.clear();
        Set<String> savedHidden = prefs.getStringSet(KEY_TOOLBAR_HIDDEN, null);
        if (savedHidden != null) {
            toolbarHiddenSet.addAll(savedHidden);
        }
    }

    public boolean isValidToolId(String id) {
        for (String d : DEFAULT_TOOLS) {
            if (d.equals(id)) return true;
        }
        return false;
    }

    public void savePinnedFiles() {
        prefs.edit().putStringSet(KEY_PINNED_FILES, new HashSet<>(pinnedFileNames)).apply();
    }

    public void saveToolbarConfiguration() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toolbarOrderList.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toolbarOrderList.get(i));
        }
        prefs.edit()
                .putString(KEY_TOOLBAR_ORDER, sb.toString())
                .putStringSet(KEY_TOOLBAR_HIDDEN, new HashSet<>(toolbarHiddenSet))
                .apply();
    }

    // =========================================================================
    // 6. LUXURY THEME & COLOR ENGINE
    // =========================================================================
    public void computeThemeColors() {
        int[] palette = THEME_PALETTES[clampInt(currentThemeIndex, 0, THEME_PALETTES.length - 1)];
        colorPrimary = palette[0];
        colorPrimaryDark = palette[1];
        colorAccentLight = palette[2];
        colorGoldAccent = palette[3];

        if (isDarkMode) {
            colorBackground = 0xFF0A0D12;
            colorCardSurface = 0xFF131822;
            colorSubtleSurface = 0xFF1C2331;
            colorTextPrimary = 0xFFF8FAFC;
            colorTextSecondary = 0xFFCBD5E1;
            colorTextMuted = 0xFF64748B;
            colorBorder = 0xFF242D3D;
            colorDivider = 0xFF1B2230;
        } else {
            colorBackground = 0xFFF6F5F0; // Warm luxury alabaster paper
            colorCardSurface = 0xFFFFFFFF;
            colorSubtleSurface = 0xFFEFEDE6;
            colorTextPrimary = 0xFF111827;
            colorTextSecondary = 0xFF475569;
            colorTextMuted = 0xFF94A3B8;
            colorBorder = 0xFFE4E1D8;
            colorDivider = 0xFFECEAE2;
        }
    }

    public void configureSystemBars() {
        try {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(isDarkMode ? 0xFF0A0D12 : colorPrimaryDark);
            window.setNavigationBarColor(isDarkMode ? 0xFF0A0D12 : colorCardSurface);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int flags = window.getDecorView().getSystemUiVisibility();
                if (!isDarkMode) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                window.getDecorView().setSystemUiVisibility(flags);
            }
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // 7. 12-FONT LOADER WITH INDIVIDUAL FONT LOCK & YA (ي) PADDING FIX
    // =========================================================================
    public Typeface getFontTypeface(int fontIdx, boolean bold) {
        int idx = clampInt(fontIdx, 0, FONT_NAMES.length - 1);
        String cacheKey = idx + "_" + (bold ? "b" : "r");
        if (FONT_CACHE.containsKey(cacheKey)) {
            return FONT_CACHE.get(cacheKey);
        }

        Typeface tf = loadRawFontTypeface(idx, bold);
        if (tf == null) {
            tf = Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        }
        FONT_CACHE.put(cacheKey, tf);
        return tf;
    }

    public Typeface loadRawFontTypeface(int idx, boolean bold) {
        try {
            String resName;
            switch (idx) {
                case 0: // Tajawal
                    resName = bold ? "font_00_tajawal_bold" : "font_00_tajawal_regular";
                    break;
                case 1: // Almarai
                    resName = bold ? "font_01_almarai_bold" : "font_01_almarai_regular";
                    break;
                case 2: // Cairo
                    resName = bold ? "font_02_cairo_bold" : "font_02_cairo_regular";
                    break;
                case 3: // Amiri
                    resName = bold ? "font_03_amiri_bold" : "font_03_amiri_regular";
                    break;
                case 4: // Scheherazade New
                    resName = bold ? "font_04_scheherazade_bold" : "font_04_scheherazade_regular";
                    break;
                case 5: // Readex Pro
                    resName = bold ? "font_05_readex_bold" : "font_05_readex_regular";
                    break;
                case 6: // Noto Sans Arabic
                    resName = bold ? "font_06_notosans_bold" : "font_06_notosans_regular";
                    break;
                case 7: // Noto Naskh Arabic
                    resName = bold ? "font_07_notonaskh_bold" : "font_07_notonaskh_regular";
                    break;
                case 8: // IBM Plex Sans Arabic
                    resName = bold ? "font_08_ibmplex_bold" : "font_08_ibmplex_regular";
                    break;
                case 9: // Aref Ruqaa
                    resName = bold ? "font_09_arefruqaa_bold" : "font_09_arefruqaa_regular";
                    break;
                case 10: // Reem Kufi
                    resName = bold ? "font_10_reemkufi_bold" : "font_10_reemkufi_regular";
                    break;
                case 11: // Changa
                default:
                    resName = bold ? "font_11_changa_bold" : "font_11_changa_regular";
                    break;
            }
            int resId = getResources().getIdentifier(resName, "font", getPackageName());
            if (resId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return getResources().getFont(resId);
            }
        } catch (Exception ignored) {
        }
        return Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    public void applyAppFont(View view, boolean bold) {
        if (view == null) return;
        // Respect views that intentionally display their own specific font (e.g. Font Cards in Settings)
        if (TAG_KEEP_OWN_FONT.equals(view.getTag())) {
            return;
        }
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            Typeface tf = getFontTypeface(currentFontIndex, bold);
            tv.setTypeface(tf, bold ? Typeface.BOLD : Typeface.NORMAL);
            // Fix Arabic letter Ya (ي) dots clipping on all buttons & labels
            if (tv != readTextView && currentFontIndex != 3) {
                tv.setIncludeFontPadding(true);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (TAG_KEEP_OWN_FONT.equals(child.getTag())) continue;
                boolean childBold = bold;
                if (child instanceof TextView) {
                    Typeface existing = ((TextView) child).getTypeface();
                    if (existing != null && existing.isBold()) {
                        childBold = true;
                    }
                }
                applyAppFont(child, childBold);
            }
        }
    }

    // =========================================================================
    // 8. UI HELPERS, FLUID SPRING PHYSICS & HAPTICS
    // =========================================================================
    public int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    public int clampInt(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public float clampFloat(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public void performHaptic(View view) {
        if (!hapticEnabled || view == null) return;
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
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
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90)
                            .setInterpolator(new DecelerateInterpolator()).start();
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(190)
                            .setInterpolator(new OvershootInterpolator(1.6f)).start();
                    if (action == MotionEvent.ACTION_UP) {
                        performHaptic(v);
                    }
                }
                return false;
            }
        });
    }

    public GradientDrawable createRoundedDrawable(int fillColor, int cornerRadiusDp, int strokeColor, int strokeWidthPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(cornerRadiusDp));
        if (strokeWidthPx > 0) {
            gd.setStroke(strokeWidthPx, strokeColor);
        }
        return gd;
    }

    public Drawable createVectorIcon(String iconType, int tintColor, int sizeDp) {
        final int px = dp(sizeDp);
        Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(tintColor);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, px * 0.095f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        if ("ic_jump_vertical".equals(iconType)) {
            float cx = px * 0.5f;
            canvas.drawLine(cx, px * 0.16f, cx, px * 0.45f, p);
            canvas.drawLine(cx, px * 0.16f, px * 0.30f, px * 0.32f, p);
            canvas.drawLine(cx, px * 0.16f, px * 0.70f, px * 0.32f, p);
            canvas.drawLine(cx, px * 0.84f, cx, px * 0.55f, p);
            canvas.drawLine(cx, px * 0.84f, px * 0.30f, px * 0.68f, p);
            canvas.drawLine(cx, px * 0.84f, px * 0.70f, px * 0.68f, p);
        }
        return new android.graphics.drawable.BitmapDrawable(getResources(), bmp);
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public AlertDialog showStyledDialog(View contentView, boolean cancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(cancelable);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        wrapper.setClipChildren(false);
        wrapper.setClipToPadding(false);
        wrapper.setPadding(dp(14), dp(14), dp(14), dp(14));

        if (contentView instanceof ViewGroup) {
            ((ViewGroup) contentView).setClipChildren(false);
            ((ViewGroup) contentView).setClipToPadding(false);
        }
        contentView.setBackground(createRoundedDrawable(colorCardSurface, 22, colorBorder, dp(1)));
        applyAppFont(contentView, false);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapper.addView(contentView, lp);

        builder.setView(wrapper);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        return dialog;
    }

    // =========================================================================
    // 9. SMART ARABIC SEARCH ENGINE (TASHKEEL & HAMZA INSENSITIVE)
    // =========================================================================
    public static boolean isArabicDiacritic(char c) {
        return (c >= '\u064B' && c <= '\u065F') || c == '\u0670' || c == '\u0640';
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
            case 'ئ':
                return 'ي';
            case 'ؤ':
                return 'و';
            default:
                return Character.toLowerCase(c);
        }
    }

    public static String normalizeArabicSimple(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!isArabicDiacritic(c)) {
                sb.append(normalizeArabicChar(c));
            }
        }
        return sb.toString();
    }

    public static List<int[]> findSmartMatchRanges(String rawText, String rawQuery) {
        List<int[]> ranges = new ArrayList<>();
        if (TextUtils.isEmpty(rawText) || TextUtils.isEmpty(rawQuery)) return ranges;

        String normQuery = normalizeArabicSimple(rawQuery.trim());
        if (normQuery.isEmpty()) return ranges;

        StringBuilder normText = new StringBuilder(rawText.length());
        int[] normToRawIndex = new int[rawText.length() + 1];
        int normIdx = 0;

        for (int rawIdx = 0; rawIdx < rawText.length(); rawIdx++) {
            char c = rawText.charAt(rawIdx);
            if (!isArabicDiacritic(c)) {
                normToRawIndex[normIdx] = rawIdx;
                normText.append(normalizeArabicChar(c));
                normIdx++;
            }
        }
        normToRawIndex[normIdx] = rawText.length();

        String haystack = normText.toString();
        int searchFrom = 0;
        while (searchFrom <= haystack.length() - normQuery.length()) {
            int found = haystack.indexOf(normQuery, searchFrom);
            if (found < 0) break;
            int rawStart = normToRawIndex[found];
            int rawEnd = normToRawIndex[found + normQuery.length() - 1] + 1;
            while (rawEnd < rawText.length() && isArabicDiacritic(rawText.charAt(rawEnd))) {
                rawEnd++;
            }
            ranges.add(new int[]{rawStart, rawEnd});
            searchFrom = found + Math.max(1, normQuery.length());
        }
        return ranges;
    }

    public SpannableString buildHighlightedSpannable(String text, String query) {
        SpannableString span = new SpannableString(text == null ? "" : text);
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(query)) return span;
        List<int[]> matches = findSmartMatchRanges(text, query);
        for (int[] range : matches) {
            int start = Math.max(0, Math.min(text.length(), range[0]));
            int end = Math.max(start, Math.min(text.length(), range[1]));
            if (end > start) {
                span.setSpan(new BackgroundColorSpan(0xFFFDE047), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(0xFF111827), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return span;
    }
