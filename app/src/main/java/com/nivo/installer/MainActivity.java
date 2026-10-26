package com.nivo.installer;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.RadialGradient;
import android.graphics.SweepGradient;
import android.graphics.PixelFormat;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.animation.ValueAnimator;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;
import com.tananaev.adblib.AdbBase64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 26, 43);
    private static final int PANEL = Color.rgb(13, 24, 48);
    private static final int CYAN = Color.rgb(29, 190, 255);
    private static final int BLUE = Color.rgb(18, 111, 255);
    private static final int PURPLE = Color.rgb(126, 28, 255);
    private static final int WHITE = Color.rgb(244, 248, 252);
    private static final int MUTED = Color.rgb(171, 187, 203);
    private static final String FIRE_PACKAGE = "com.ivantrj.glasslauncher";
    private static final String ACCESSIBILITY = FIRE_PACKAGE + "/com.ivantrj.glasslauncher.HomeButtonService";
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private final List<String> found = new ArrayList<>();
    private EditText ipInput;
    private TextView status;
    private TextView devices;
    private Button searchButton;
    private Button installButton;
    private boolean uninstallMode;
    private boolean settingsScreenVisible;
    private boolean settingsDetailVisible;
    private boolean installerScreenVisible;
    private boolean updatesScreenVisible;
    private volatile boolean updateOperationRunning;
    private volatile long lastUpdateProgressUi;
    private UpdateManager.Release pendingUpdate;
    private String updateHost = "";
    private String installedTvVersion = "";
    private String latestReleaseUrl = "";
    private String lastUpdateCheckLabel = "";
    private android.widget.ProgressBar updateProgressBar;
    private TextView updateProgressLabel;
    private TextView updateProgressSize;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildUi();
    }

    private android.widget.FrameLayout progressTrack;
    private IndeterminateProgressView indeterminateProgress;
    private android.widget.ProgressBar operationProgressBar;
    private TextView operationProgressPercent;
    private TextView operationProgressTitle;
    private TextView operationProgressSubtitle;
    private final View[] operationStepStatuses = new View[4];
    private android.widget.ProgressBar operationActiveSpinner;
    private final TextView[] operationStepTitles = new TextView[4];
    private final TextView[] operationStepSubtitles = new TextView[4];
    private final View[] operationStepIcons = new View[4];
    private volatile boolean cancelRequested;
    private volatile boolean operationRunning;
    private volatile Socket activeSocket;
    private volatile AdbConnection activeConnection;
    private String activeHost = "";
    private boolean keepDataOnUninstall = true;

    private void buildUi() {
        getWindow().setStatusBarColor(Color.rgb(3, 10, 23));
        getWindow().setNavigationBarColor(Color.rgb(3, 10, 23));
        showSplash();
    }

    private void showSplash() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(34), dp(34), dp(34), dp(34));
        root.setBackground(homeGradient());
        View logo = new NivoMarkView(this);
        root.addView(logo, new LinearLayout.LayoutParams(dp(126), dp(126)));
        TextView name = text("NIIVO", 28, WHITE, true);
        name.setLetterSpacing(0.08f);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-1, -2);
        nameLp.topMargin = dp(17);
        root.addView(name, nameLp);
        TextView launcher = text("L A U N C H E R", 10, MUTED, false);
        launcher.setLetterSpacing(0.09f);
        launcher.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams launcherLp = new LinearLayout.LayoutParams(-1, -2);
        launcherLp.topMargin = dp(3);
        root.addView(launcher, launcherLp);
        TextView tag = text("Tu Fire TV, a tu manera.", 16, MUTED, false);
        tag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(-1, -2);
        tagLp.topMargin = dp(25);
        root.addView(tag, tagLp);
        UpdateRingView ring = new UpdateRingView(this);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        ringLp.topMargin = dp(68);
        root.addView(ring, ringLp);
        TextView loading = text("Iniciando NIIVO…", 15, MUTED, false);
        loading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams loadingLp = new LinearLayout.LayoutParams(-1, -2);
        loadingLp.topMargin = dp(15);
        root.addView(loading, loadingLp);
        setContentView(root);
        root.postDelayed(() -> { if (!isFinishing()) showHome(); }, 2300);
    }

    private LinearLayout newPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(screenGradient());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(25), dp(22), dp(28));
        scroll.addView(page);
        setContentView(scroll);
        return page;
    }

    private void addBack(LinearLayout page, String label) {
        TextView back = text("‹   " + label, 16, WHITE, true);
        back.setPadding(0, dp(3), 0, dp(10));
        back.setOnClickListener(v -> { if (updateOperationRunning) confirmCancelUpdate(); else showHome(); });
        page.addView(back);
    }

    private void showHome() {
        updatesScreenVisible = false;
        settingsScreenVisible = false;
        settingsDetailVisible = false;
        installerScreenVisible = false;
        LinearLayout page = newPage();
        page.setBackground(homeGradient());
        page.setPadding(dp(18), dp(78), dp(16), dp(24));
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        View mark = new NivoMarkView(this);
        brand.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout brandWords = new LinearLayout(this);
        brandWords.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandWordsLp = new LinearLayout.LayoutParams(-2, -2);
        brandWordsLp.leftMargin = dp(17);
        brand.addView(brandWords, brandWordsLp);
        TextView name = text("NIIVO", 32, WHITE, true);
        name.setLetterSpacing(0.15f);
        brandWords.addView(name);
        TextView launcher = text("L A U N C H E R", 11, MUTED, false);
        launcher.setLetterSpacing(0.12f);
        brandWords.addView(launcher);
        View spacer = new View(this);
        brand.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView version = text(getEmbeddedLauncherVersion().replace("Versión ", "v"), 12, MUTED, false);
        version.setGravity(Gravity.CENTER);
        version.setSingleLine(true);
        version.setPadding(dp(10), 0, dp(10), 0);
        version.setBackground(round(0xD0071429, dp(22), 0x80344BC6, dp(1)));
        LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(-2, dp(34));
        brand.addView(version, versionLp);
        FrameLayout settings = new FrameLayout(this);
        settings.setForegroundGravity(Gravity.CENTER);
        settings.setContentDescription("Configuración");
        settings.setBackgroundColor(Color.TRANSPARENT);
        settings.setFocusable(true);
        settings.setClickable(true);
        settings.setOnClickListener(v -> showSettings());
        ActionIconView settingsGlyph = new ActionIconView(this, 3);
        View settingsDisc = new View(this);
        settingsDisc.setBackground(round(0x55200D45, dp(20), 0x222D7CE6, dp(1)));
        settings.addView(settingsDisc, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        FrameLayout.LayoutParams settingsGlyphLp = new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER);
        settings.addView(settingsGlyph, settingsGlyphLp);
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        settingsLp.leftMargin = dp(4);
        brand.addView(settings, settingsLp);
        page.addView(brand);

        TextView tagline = text("Tu Fire TV, a tu manera.", 16, MUTED, false);
        LinearLayout.LayoutParams taglineLp = new LinearLayout.LayoutParams(-1, -2);
        taglineLp.topMargin = dp(5);
        taglineLp.bottomMargin = dp(33);
        tagline.setPadding(dp(10), 0, 0, 0);
        tagline.setTextSize(15);
        tagline.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        page.addView(tagline, taglineLp);
        page.addView(mainMenuCard(9, "Instalar", "Instala la última versión de NIIVO\nen tu dispositivo.", true,
                v -> { uninstallMode = false; buildInstallerPage(); }));
        page.addView(mainMenuCard(1, "Actualizar", "Comprueba si tienes una nueva\nversión disponible.", false,
                v -> showUpdates()));
        page.addView(mainMenuCard(3, "Configuración", "Personaliza NIIVO a tu gusto.", false,
                v -> showSettings()));
        page.addView(mainMenuCard(2, "Desinstalar", "Elimina NIIVO de tu dispositivo.", false,
                v -> { uninstallMode = true; buildInstallerPage(); }));
    }

    private View mainMenuCard(int icon, String title, String subtitle, boolean featured,
                              View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(8), dp(11), dp(8));
        row.setBackground(featured ? installerCardPanel(dp(16)) : glassPanel(dp(15)));
        int cardHeight = featured ? 102 : title.equals("Actualizar") ? 94 : 90;
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(cardHeight));
        rowLp.bottomMargin = featured ? dp(18) : title.equals("Desinstalar") ? 0 : dp(10);
        row.setLayoutParams(rowLp);

        int iconSize = icon == 9 ? 56 : icon == 1 ? 56 : icon == 2 ? 60 : 46;
        ActionIconView iconView = new ActionIconView(this, icon);
        iconView.setContentDescription(title);
        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        iconBox.addView(iconView, new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
        row.addView(iconBox, new LinearLayout.LayoutParams(dp(70), dp(66)));
        View divider = new View(this);
        divider.setBackgroundColor(featured ? 0xB5228BFF : 0x55337BB8);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(1), dp(featured ? 62 : 54));
        dividerLp.leftMargin = dp(8);
        dividerLp.rightMargin = dp(18);
        row.addView(divider, dividerLp);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(labels, labelsLp);
        labels.addView(text(title, 18, WHITE, true));
        TextView sub = text(subtitle, 14, MUTED, false);
        sub.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(4);
        labels.addView(sub, subLp);

        TextView arrow = text("›", 30, 0xFFD5D8F2, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(28), -1));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(click);
        row.setOnFocusChangeListener((view, focused) -> {
            if (!featured) row.setBackground(focused ? installerCardPanel(dp(18)) : glassPanel(dp(18)));
            float scale = focused ? 1.015f : 1f;
            row.animate().scaleX(scale).scaleY(scale).setDuration(110).start();
        });
        return row;
    }
    private LinearLayout updatesPage(String subtitle) {
        LinearLayout page = newPage();
        page.setPadding(dp(18), dp(10), dp(18), dp(26));
        TextView back = text("‹", 30, WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Regresar");
        back.setOnClickListener(v -> { updatesScreenVisible = false; showHome(); });
        page.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text("Actualizaciones", 30, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(8);
        page.addView(title, titleLp);
        TextView intro = text(subtitle, 16, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        intro.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(7);
        introLp.bottomMargin = dp(18);
        page.addView(intro, introLp);
        return page;
    }

    private void showUpdates() {
        settingsScreenVisible = false;
        settingsDetailVisible = false;
        updatesScreenVisible = true;
        updateOperationRunning = false;
        pendingUpdate = null;
        String host = getPreferences(MODE_PRIVATE).getString("last_fire_tv_host", "");
        if (host == null || host.trim().isEmpty()) {
            showUpdateNeedDevice();
            return;
        }
        checkUpdates(host.trim());
    }

    private void showUpdateNeedDevice() {
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(updateVersionCard("No conectada", "Conecta Fire TV para leer su versión", "—", "—", "—", "ADB", 0xFF174A84), updateCardLayout(dp(188), 16));
        page.addView(updateStatusCard("Fire TV no conectado", "Conecta tu televisor para comprobar su versión instalada y buscar actualizaciones reales.", CYAN, "↔"), updateCardLayout(dp(104), 18));
        Button connect = button("Conectar Fire TV", true);
        page.addView(connect, updateCardLayout(dp(58), 0));
        connect.setOnClickListener(v -> showUpdateHostDialog());
        connect.requestFocus();
    }

    private void showUpdateHostDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("IP del Fire TV, por ejemplo 192.168.1.7");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        String saved = getPreferences(MODE_PRIVATE).getString("last_fire_tv_host", "");
        if (saved != null) input.setText(saved);
        new android.app.AlertDialog.Builder(this).setTitle("Conectar Fire TV")
                .setMessage("Escribe la IP que aparece en Mi Fire TV > Acerca de > Red.")
                .setView(input).setNegativeButton("Cancelar", null)
                .setPositiveButton("Comprobar", (d, w) -> {
                    String host = input.getText().toString().trim();
                    if (!isValidIp(host)) {
                        Toast.makeText(this, "Escribe una IP válida.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    getPreferences(MODE_PRIVATE).edit().putString("last_fire_tv_host", host).apply();
                    checkUpdates(host);
                }).show();
    }

    private void confirmCancelUpdate() {
        new android.app.AlertDialog.Builder(this).setTitle("Cancelar actualización")
                .setMessage("La descarga o instalación se detendrá. ¿Quieres cancelar?")
                .setNegativeButton("Continuar", null)
                .setPositiveButton("Cancelar", (d, w) -> cancelCurrentOperation()).show();
    }

    private void checkUpdates(String host) {
        updatesScreenVisible = true;
        updateHost = host;
        pendingUpdate = null;
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(updateVersionCard("Comprobando…", "Leyendo la versión del Fire TV", "—", "GitHub", "—", "BUSCANDO", 0xFF174A84), updateCardLayout(dp(188), 30));
        UpdateRingView spinner = new UpdateRingView(this);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(dp(58), dp(58));
        spinLp.gravity = Gravity.CENTER_HORIZONTAL;
        spinLp.topMargin = dp(10);
        page.addView(spinner, spinLp);
        TextView message = text("Comprobando actualizaciones…", 17, WHITE, true);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(-1, -2);
        msgLp.topMargin = dp(18);
        page.addView(message, msgLp);
        TextView note = text("Esto puede tardar unos segundos.", 14, MUTED, false);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(7);
        page.addView(note, noteLp);
        page.getChildAt(0).requestFocus();

        cancelRequested = false;
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                String details = runShell(connection, "dumpsys package " + FIRE_PACKAGE);
                java.util.regex.Matcher version = java.util.regex.Pattern.compile("versionName=([^\\s]+)").matcher(details == null ? "" : details);
                if (!version.find()) throw new IOException("No se encontró NIIVO instalado en el Fire TV conectado.");
                String installed = UpdateManager.normalizeVersion(version.group(1));
                if (installed == null) throw new IOException("El Fire TV devolvió una versión de NIIVO no válida.");
                installedTvVersion = installed;
            } catch (Exception ex) {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
                showUpdateError("No se pudo comprobar", "No se pudo leer la versión instalada en el Fire TV. Comprueba ADB y vuelve a intentarlo.", host, true);
                return;
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
            }
            try {
                UpdateManager.ReleaseCheck check = UpdateManager.checkLatest(installedTvVersion);
                runOnUiThread(() -> presentUpdateCheck(check, host));
            } catch (Exception ex) {
                String detail = updateFailureMessage(ex);
                runOnUiThread(() -> showUpdateError("No se pudo comprobar", detail, host, false));
            }
        });
    }

    private String updateFailureMessage(Exception error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.ConnectException || cause instanceof java.net.NoRouteToHostException)
                return "No hay conexión con GitHub. Comprueba que el teléfono tenga Internet e inténtalo nuevamente.";
            if (cause instanceof IOException && cause.getMessage() != null && !cause.getMessage().trim().isEmpty())
                return cause.getMessage();
            cause = cause.getCause();
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "No fue posible consultar las actualizaciones de NIIVO en GitHub."
                : "No fue posible consultar GitHub: " + message;
    }

    private void presentUpdateCheck(UpdateManager.ReleaseCheck check, String host) {
        if (!updatesScreenVisible) return;
        pendingUpdate = check.asset;
        latestReleaseUrl = check.htmlUrl;
        lastUpdateCheckLabel = "Hoy, " + new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
        if (UpdateManager.compareVersions(installedTvVersion, check.latestVersion) >= 0) {
            showUpdateCurrent(host, check.latestVersion);
        } else if (check.asset == null) {
            showUpdateUnavailable(host, installedTvVersion, check.latestVersion);
        } else {
            showUpdateAvailable(host, installedTvVersion, check.asset);
        }
    }

    private void showUpdateCurrent(String host, String latest) {
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(updateVersionCard(installedTvVersion, "Launcher para Fire TV", lastUpdateCheckLabel, "GitHub", updateChannel(latest), "AL DÍA", 0xFF0BA66E), updateCardLayout(dp(188), 14));
        page.addView(updateStatusCard("NIIVO está actualizado", "Ya tienes la última versión disponible de NIIVO (" + latest + ").", 0xFF13C982, "✓"), updateCardLayout(dp(94), 12));
        Button retry = button("⟳   Buscar actualizaciones", true);
        page.addView(retry, updateCardLayout(dp(58), 10));
        retry.setOnClickListener(v -> checkUpdates(host));
        Button changes = button("▤   Ver cambios recientes     ›", false);
        page.addView(changes, updateCardLayout(dp(54), 0));
        changes.setOnClickListener(v -> openLatestRelease());
        retry.requestFocus();
    }

    private void showUpdateAvailable(String host, String installed, UpdateManager.Release release) {
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(updateVersionCard(installed, "Launcher para Fire TV", lastUpdateCheckLabel, "GitHub", updateChannel(release.version), "NUEVA", 0xFF5420D9), updateCardLayout(dp(188), 12));
        page.addView(updateStatusCard("Nueva versión disponible", "Versión " + release.version + "\nMejoras de rendimiento, correcciones y nuevas funciones.", PURPLE, "↑"), updateCardLayout(dp(112), 12));
        Button update = button("⬇   Actualizar ahora", true);
        page.addView(update, updateCardLayout(dp(58), 10));
        update.setOnClickListener(v -> startRemoteUpdate(host, release));
        Button later = button("◷   Más tarde     ›", false);
        page.addView(later, updateCardLayout(dp(54), 10));
        later.setOnClickListener(v -> { updatesScreenVisible = false; showHome(); });
        Button changes = button("▤   Ver cambios recientes     ›", false);
        page.addView(changes, updateCardLayout(dp(54), 0));
        changes.setOnClickListener(v -> openLatestRelease());
        update.requestFocus();
    }

    private void showUpdateUnavailable(String host, String installed, String latest) {
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(updateVersionCard(installed, "Launcher para Fire TV", lastUpdateCheckLabel, "GitHub", updateChannel(latest), "PUBLICADA", 0xFF5420D9), updateCardLayout(dp(188), 14));
        page.addView(updateStatusCard("APK no disponible", "La versión " + latest + " está publicada, pero no contiene un APK de NIIVO válido.", 0xFFEEA62A, "!"), updateCardLayout(dp(104), 16));
        Button retry = button("⟳   Buscar actualizaciones", true);
        page.addView(retry, updateCardLayout(dp(58), 0));
        retry.setOnClickListener(v -> checkUpdates(host)); retry.requestFocus();
    }

    private void showUpdateError(String title, String detail, String host, boolean fireTvConnectionProblem) {
        runOnUiThread(() -> {
            if (!updatesScreenVisible) return;
            LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
            page.addView(updateVersionCard("No comprobada", "No se pudo leer la versión instalada", "—", "GitHub", "—", "ERROR", 0xFF81254C), updateCardLayout(dp(188), 14));
            page.addView(updateStatusCard(title, detail, 0xFFEF6676, "!"), updateCardLayout(dp(112), 14));
            if (fireTvConnectionProblem) {
                TextView help = text("Comprueba que Fire TV y el teléfono estén en la misma Wi‑Fi, activa Depuración ADB en la TV y acepta el aviso de autorización.", 14, MUTED, false);
                help.setLineSpacing(dp(3), 1f);
                help.setPadding(dp(8), 0, dp(8), dp(14));
                page.addView(help);
            }
            Button retry = button("⟳   Reintentar", true);
            page.addView(retry, updateCardLayout(dp(58), fireTvConnectionProblem ? 8 : 0));
            retry.setOnClickListener(v -> checkUpdates(host));
            if (fireTvConnectionProblem) {
                Button reconnect = button("Conectar o cambiar IP     ›", false);
                page.addView(reconnect, updateCardLayout(dp(58), 0));
                reconnect.setOnClickListener(v -> showUpdateHostDialog());
                reconnect.requestFocus();
            } else {
                retry.requestFocus();
            }
        });
    }

    private String updateChannel(String version) {
        return version != null && version.toLowerCase(java.util.Locale.ROOT).contains("beta") ? "Beta" : "Estable";
    }

    private void openLatestRelease() {
        String url = latestReleaseUrl;
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "No hay una publicación reciente para mostrar.", Toast.LENGTH_SHORT).show();
            return;
        }
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
        catch (Exception ex) { Toast.makeText(this, "No se pudo abrir GitHub.", Toast.LENGTH_SHORT).show(); }
    }

    private LinearLayout updateVersionCard(String version, String description, String checkedAt,
                                           String repository, String channel, String badge, int badgeColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(12));
        card.setBackground(successOutline(dp(19)));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.FrameLayout logoTile = new android.widget.FrameLayout(this);
        logoTile.setBackground(round(0xB807152A, dp(16), 0x334780C0, dp(1)));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.nivo_update_mark);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackgroundColor(Color.TRANSPARENT);
        android.widget.FrameLayout.LayoutParams logoLp = new android.widget.FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER);
        logoTile.addView(logo, logoLp);
        top.addView(logoTile, new LinearLayout.LayoutParams(dp(68), dp(68)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(5), 0);
        TextView label = text("Versión instalada", 13, MUTED, false);
        info.addView(label);
        TextView value = text(version, 18, WHITE, true);
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(-1, -2);
        valueLp.topMargin = dp(3);
        info.addView(value, valueLp);
        TextView caption = text(description, 12, MUTED, false);
        LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(-1, -2);
        captionLp.topMargin = dp(3);
        info.addView(caption, captionLp);
        top.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView chip = text(badge, 10, WHITE, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(8), dp(7), dp(8), dp(7));
        chip.setBackground(round(badgeColor, dp(14), 0, 0));
        top.addView(chip, new LinearLayout.LayoutParams(-2, -2));
        card.addView(top);

        View divider = new View(this);
        divider.setBackgroundColor(0x334A70AA);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.topMargin = dp(12);
        dividerLp.bottomMargin = dp(9);
        card.addView(divider, dividerLp);

        LinearLayout metadata = new LinearLayout(this);
        metadata.setGravity(Gravity.CENTER_VERTICAL);
        addUpdateMeta(metadata, "Última comprobación", checkedAt);
        addUpdateMetaDivider(metadata);
        addUpdateMeta(metadata, "Repositorio", repository);
        addUpdateMetaDivider(metadata);
        addUpdateMeta(metadata, "Canal", channel);
        card.addView(metadata, new LinearLayout.LayoutParams(-1, dp(43)));
        return card;
    }

    private void addUpdateMeta(LinearLayout row, String title, String value) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(title, 9, MUTED, false);
        label.setMaxLines(1);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.addView(label);
        TextView content = text(value, 12, WHITE, false);
        content.setMaxLines(1);
        content.setSingleLine(true);
        content.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(-1, -2);
        contentLp.topMargin = dp(2);
        cell.addView(content, contentLp);
        row.addView(cell, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    private void addUpdateMetaDivider(LinearLayout row) {
        View divider = new View(this);
        divider.setBackgroundColor(0x443F6193);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(34));
        lp.setMargins(dp(7), 0, dp(7), 0);
        row.addView(divider, lp);
    }

    private LinearLayout updateStatusCard(String title, String detail, int accent, String symbol) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(glassPanel(dp(18)));
        TextView icon = text(symbol, 27, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(accent, dp(28), 0, 0));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        iconLp.rightMargin = dp(12);
        card.addView(icon, iconLp);
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView heading = text(title, 16, WHITE, true);
        copy.addView(heading);
        TextView body = text(detail, 13, MUTED, false);
        body.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, -2);
        bodyLp.topMargin = dp(4);
        copy.addView(body, bodyLp);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        return card;
    }

    private LinearLayout.LayoutParams updateCardLayout(int height, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, height);
        lp.bottomMargin = dp(bottom);
        return lp;
    }

    private void startRemoteUpdate(String host, UpdateManager.Release release) {
        if (host == null || host.trim().isEmpty()) { showUpdateHostDialog(); return; }
        updatesScreenVisible = true;
        updateOperationRunning = true;
        cancelRequested = false;
        File downloadedApk = new File(getCacheDir(), "niivo-update-" + release.version + ".apk");
        showUpdateDownloading(release, 0, release.size);
        WORK.execute(() -> {
            try {
                UpdateManager.download(release, downloadedApk, (downloaded, total) -> {
                    if (cancelRequested) throw new RuntimeException("Actualización cancelada.");
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (downloaded == total || now - lastUpdateProgressUi >= 120) {
                        lastUpdateProgressUi = now;
                        runOnUiThread(() -> updateDownloadProgress(downloaded, total));
                    }
                });
                if (cancelRequested) throw new IOException("Actualización cancelada.");
                android.content.pm.PackageInfo info = getPackageManager().getPackageArchiveInfo(downloadedApk.getAbsolutePath(), 0);
                if (info == null || !FIRE_PACKAGE.equals(info.packageName)) throw new IOException("El archivo descargado no es el APK de NIIVO.");
                String fileVersion = UpdateManager.normalizeVersion(info.versionName);
                if (fileVersion == null || UpdateManager.compareVersions(fileVersion, release.version) != 0) throw new IOException("La versión del APK no coincide con el release esperado.");
                runOnUiThread(() -> showUpdateInstalling(release.version));
                AdbConnection connection = connect(host);
                try {
                    pushApk(connection, downloadedApk, "/data/local/tmp/niivo-update.apk");
                    String result = runShell(connection, "pm install -r -g /data/local/tmp/niivo-update.apk");
                    if (result == null || !result.contains("Success")) throw new IOException("Fire TV no confirmó la instalación: " + clean(result));
                    String details = runShell(connection, "dumpsys package " + FIRE_PACKAGE);
                    java.util.regex.Matcher installed = java.util.regex.Pattern.compile("versionName=([^\\s]+)").matcher(details == null ? "" : details);
                    if (!installed.find() || UpdateManager.compareVersions(installed.group(1), release.version) != 0)
                        throw new IOException("La instalación terminó, pero Fire TV no confirmó la versión " + release.version + ".");
                } finally { try { connection.close(); } catch (Exception ignored) {} }
                updateOperationRunning = false;
                runOnUiThread(() -> showUpdateDone(release.version));
            } catch (Exception ex) {
                updateOperationRunning = false;
                if (cancelRequested) runOnUiThread(() -> showUpdateError("Actualización cancelada", "No se completó la actualización.", host, false));
                else {
                    String detail = ex.getMessage() == null ? "No se pudo completar la actualización." : ex.getMessage();
                    runOnUiThread(() -> showUpdateError("No se pudo actualizar", detail, host, true));
                }
            }
        });
    }

    private void showUpdateDownloading(UpdateManager.Release release, long downloaded, long total) {
        LinearLayout page = updatesPage("Descargando la versión " + release.version + ".");
        page.addView(updateVersionCard(installedTvVersion, "Launcher para Fire TV", lastUpdateCheckLabel, "GitHub", updateChannel(release.version), "ACTUALIZANDO", 0xFF5420D9), updateCardLayout(dp(188), 14));
        page.addView(updateStatusCard("Actualizando NIIVO", "Descargando la versión " + release.version + "…", BLUE, "⟳"), updateCardLayout(dp(96), 2));
        updateProgressBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        updateProgressBar.setMax(1000);
        updateProgressBar.setProgressDrawable(updateProgressDrawable());
        updateProgressBar.setIndeterminate(total <= 0);
        updateProgressLabel = text("0%", 15, MUTED, false);
        updateProgressSize = text("0 MB / " + formatMegabytes(total), 14, MUTED, false);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(8));
        barLp.topMargin = dp(16); page.addView(updateProgressBar, barLp);
        LinearLayout stats = new LinearLayout(this); stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.addView(updateProgressLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        updateProgressSize.setGravity(Gravity.RIGHT); stats.addView(updateProgressSize, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(-1, -2); statsLp.topMargin = dp(8); page.addView(stats, statsLp);
        updateDownloadProgress(downloaded, total);
    }

    private Drawable updateProgressDrawable() {
        GradientDrawable track = new GradientDrawable();
        track.setColor(0x55334A78); track.setCornerRadius(dp(5));
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{CYAN, BLUE, PURPLE});
        fill.setCornerRadius(dp(5));
        android.graphics.drawable.ClipDrawable clipped = new android.graphics.drawable.ClipDrawable(fill, Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL);
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(new Drawable[]{track, clipped});
        layers.setId(0, android.R.id.background); layers.setId(1, android.R.id.progress);
        return layers;
    }

    private void updateDownloadProgress(long downloaded, long total) {
        if (!updatesScreenVisible || updateProgressBar == null) return;
        updateProgressBar.setIndeterminate(total <= 0);
        if (total > 0) {
            int progress = (int)Math.min(1000, Math.max(0, downloaded * 1000L / total));
            updateProgressBar.setProgress(progress);
            updateProgressLabel.setText((downloaded >= total ? 100 : downloaded * 100 / total) + "%");
        } else updateProgressLabel.setText(formatMegabytes(downloaded));
        updateProgressSize.setText(formatMegabytes(downloaded) + " / " + (total > 0 ? formatMegabytes(total) : "tamaño desconocido"));
    }

    private String formatMegabytes(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024f * 1024f));
    }

    private void showUpdateInstalling(String version) {
        runOnUiThread(() -> {
            if (!updatesScreenVisible) return;
            LinearLayout page = updatesPage("Instalando la versión " + version + " en Fire TV.");
            page.addView(updateVersionCard(installedTvVersion, "Launcher para Fire TV", lastUpdateCheckLabel, "GitHub", updateChannel(version), "INSTALANDO", 0xFF5420D9), updateCardLayout(dp(188), 20));
            page.addView(updateStatusCard("Instalando NIIVO", "Fire TV está aplicando la actualización…", BLUE, "⟳"), updateCardLayout(dp(98), 20));
            UpdateRingView spinner = new UpdateRingView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(54), dp(54)); lp.gravity = Gravity.CENTER_HORIZONTAL; page.addView(spinner, lp);
        });
    }

    private void showUpdateDone(String version) {
        if (!updatesScreenVisible) return;
        installedTvVersion = version;
        LinearLayout page = updatesPage("NIIVO se actualizó correctamente en el Fire TV.");
        page.addView(updateVersionCard(version, "Launcher para Fire TV", lastUpdateCheckLabel, "GitHub", updateChannel(version), "AL DÍA", 0xFF0BA66E), updateCardLayout(dp(188), 18));
        SuccessRingView success = new SuccessRingView(this);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(86), dp(86)); ringLp.gravity = Gravity.CENTER_HORIZONTAL; ringLp.topMargin = dp(12); ringLp.bottomMargin = dp(12);
        page.addView(success, ringLp);
        page.addView(updateStatusCard("¡Actualizado!", "Fire TV confirmó que NIIVO " + version + " quedó instalado.", 0xFF13C982, "✓"), updateCardLayout(dp(96), 18));
        Button done = button("Listo", true); page.addView(done, updateCardLayout(dp(58), 0));
        done.setOnClickListener(v -> { updatesScreenVisible = false; showHome(); }); done.requestFocus();
    }
    private void showSettings() {
        updatesScreenVisible = false;
        settingsScreenVisible = true;
        settingsDetailVisible = false;
        installerScreenVisible = false;
        keepDataOnUninstall = getPreferences(MODE_PRIVATE).getBoolean("keep_data", true);
        LinearLayout page = newPage();
        page.setBackground(homeGradient());
        page.setPadding(dp(18), dp(18), dp(18), dp(26));
        addSettingsHeader(page, "Configuración", "Personaliza NIIVO a tu gusto.", this::showHome);

        LinearLayout adbCard = settingsCard(11, "Mantener ADB",
                "Mantiene activo el acceso ADB después de reiniciar.", true, v -> showAdbSettings());
        page.addView(adbCard, settingsOptionLayout());
        LinearLayout privacyCard = settingsCard(25, "Privacidad",
                "Gestiona tus datos y preferencias de privacidad.", false, v -> showPrivacySettings());
        page.addView(privacyCard, settingsOptionLayout());
        LinearLayout supportCard = settingsCard(22, "Soporte",
                "Obtén ayuda, reporta un problema o contáctanos.", false, v -> showSupportSettings());
        page.addView(supportCard, settingsOptionLayout());
        LinearLayout aboutCard = settingsCard(7, "Acerca de",
                "Versión, información legal y créditos.", false, v -> showAboutSettings());
        page.addView(aboutCard, settingsOptionLayout());
        adbCard.requestFocus();
    }

    private LinearLayout.LayoutParams settingsOptionLayout() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(86));
        lp.bottomMargin = dp(10);
        return lp;
    }

    private final LinearLayout[] launcherCardHolder = new LinearLayout[1];
    private final TextView[] launcherStatusHolder = new TextView[1];

    private LinearLayout settingsCard(int icon, String title, String subtitle, boolean highlighted,
                                      View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(9), dp(8), dp(11), dp(8));
        row.setBackground(highlighted ? installerCardPanel(dp(19)) : glassPanel(dp(19)));

        ActionIconView iconView = new ActionIconView(this, icon);
        row.addView(iconView, new LinearLayout.LayoutParams(dp(58), dp(58)));
        View divider = new View(this);
        divider.setBackgroundColor(highlighted ? 0x8A238DFF : 0x55236AC1);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(1), dp(54));
        dividerLp.setMargins(dp(6), 0, dp(13), 0);
        row.addView(divider, dividerLp);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(labels, labelsLp);
        labels.addView(text(title, 16, WHITE, true));
        TextView sub = text(subtitle, 13, MUTED, false);
        sub.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(3);
        labels.addView(sub, subLp);

        TextView arrow = text("›", 27, 0xFFD5D8F2, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(23), -1));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(click);
        row.setOnFocusChangeListener((view, focused) -> {
            if (!highlighted) row.setBackground(focused ? installerCardPanel(dp(19)) : glassPanel(dp(19)));
            float scale = focused ? 1.015f : 1f;
            row.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
        });
        return row;
    }

    private void refreshLauncherStatus(String host, LinearLayout card, TextView label) {
        label.setText("Comprobando Fire TV…");
        card.setTag(null);
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                String enabledServices = runShell(connection, "settings get secure enabled_accessibility_services");
                boolean enabled = enabledServices != null
                        && enabledServices.toLowerCase(java.util.Locale.ROOT).contains(ACCESSIBILITY.toLowerCase(java.util.Locale.ROOT));
                final boolean isEnabled = enabled;
                runOnUiThread(() -> {
                    if (card.getParent() == null) return;
                    card.setTag(isEnabled);
                    label.setText(isEnabled
                            ? "NIIVO está configurado como pantalla de inicio"
                            : "Establecer NIIVO como pantalla de inicio");
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if (card.getParent() == null) return;
                    card.setTag(null);
                    label.setText("No se pudo comprobar Fire TV");
                });
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void onLauncherCardSelected(LinearLayout card, TextView label) {
        String host = getPreferences(MODE_PRIVATE).getString("last_fire_tv_host", "");
        if (host.isEmpty()) {
            buildInstallerPage();
            return;
        }
        Object state = card.getTag();
        if (Boolean.TRUE.equals(state)) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Launcher predeterminado")
                    .setMessage("NIIVO está configurado para abrirse al pulsar el botón Inicio en este Fire TV.")
                    .setPositiveButton("Aceptar", null)
                    .show();
        } else if (Boolean.FALSE.equals(state)) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Establecer NIIVO")
                    .setMessage("Se conectará al Fire TV guardado para activar NIIVO como pantalla de inicio. Asegúrate de que esté encendido y conectado a la misma Wi‑Fi.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Configurar", (dialog, which) -> configureLauncher(host, card, label))
                    .show();
        } else {
            refreshLauncherStatus(host, card, label);
        }
    }

    private void configureLauncher(String host, LinearLayout card, TextView label) {
        card.setTag(null);
        label.setText("Configurando NIIVO en Fire TV…");
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                enableLauncher(connection);
                String enabledServices = runShell(connection, "settings get secure enabled_accessibility_services");
                boolean enabled = enabledServices != null
                        && enabledServices.toLowerCase(java.util.Locale.ROOT).contains(ACCESSIBILITY.toLowerCase(java.util.Locale.ROOT));
                if (!enabled) throw new IOException("Fire TV no confirmó la activación del servicio de NIIVO.");
                runOnUiThread(() -> {
                    card.setTag(Boolean.TRUE);
                    label.setText("NIIVO está configurado como pantalla de inicio");
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    card.setTag(null);
                    label.setText("No se pudo activar NIIVO");
                    Toast.makeText(this, clean(ex.getMessage()), Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void enableLauncher(AdbConnection connection) throws Exception {
        String currentlyEnabled = runShell(connection, "settings get secure enabled_accessibility_services");
        String services = currentlyEnabled == null || currentlyEnabled.trim().isEmpty() || "null".equalsIgnoreCase(currentlyEnabled.trim())
                ? ACCESSIBILITY
                : (currentlyEnabled.toLowerCase(java.util.Locale.ROOT).contains(ACCESSIBILITY.toLowerCase(java.util.Locale.ROOT))
                    ? currentlyEnabled.trim() : currentlyEnabled.trim() + ":" + ACCESSIBILITY);
        runShell(connection, "settings put secure enabled_accessibility_services " + services
                + "; settings put secure accessibility_enabled 1"
                + "; am start -n " + FIRE_PACKAGE + "/.HomeActivity");
    }

    private String getEmbeddedLauncherVersion() {
        try {
            File apk = new File(getCacheDir(), "niivo-version.apk");
            {
                try (InputStream input = getAssets().open("nivo.apk");
                     FileOutputStream output = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[32768];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
            }
            android.content.pm.PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            if (info != null && info.versionName != null && !info.versionName.isEmpty()) {
                return "Versión " + info.versionName;
            }
        } catch (Exception ignored) {}
        return "Versión no disponible";
    }

    private String getInstallerVersion() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null && !info.versionName.isEmpty()) return "Versión " + info.versionName;
        } catch (Exception ignored) {}
        return "Versión no disponible";
    }

    private void addSettingsHeader(LinearLayout page, String title, String subtitle) {
        addSettingsHeader(page, title, subtitle, this::showSettings);
    }

    private void addSettingsHeader(LinearLayout page, String title, String subtitle, Runnable backAction) {
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 31, WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Regresar");
        back.setBackground(round(0x55204068, dp(26), 0x334C84C0, dp(1)));
        back.setFocusable(true);
        back.setClickable(true);
        back.setOnClickListener(v -> backAction.run());
        top.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));

        View mark = new NivoMarkView(this);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(39), dp(39));
        markLp.leftMargin = dp(15);
        top.addView(mark, markLp);
        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandLp = new LinearLayout.LayoutParams(-2, -2);
        brandLp.leftMargin = dp(8);
        top.addView(brandText, brandLp);
        brandText.addView(text("NIIVO", 17, WHITE, true));
        TextView launcher = text("L A U N C H E R", 8, MUTED, false);
        launcher.setLetterSpacing(0.08f);
        brandText.addView(launcher);
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView heading = text(title, 28, WHITE, true);
        LinearLayout.LayoutParams headingLp = new LinearLayout.LayoutParams(-1, -2);
        headingLp.topMargin = dp(23);
        page.addView(heading, headingLp);
        TextView description = text(subtitle, 15, MUTED, false);
        description.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams descriptionLp = new LinearLayout.LayoutParams(-1, -2);
        descriptionLp.topMargin = dp(6);
        descriptionLp.bottomMargin = dp(19);
        page.addView(description, descriptionLp);
    }

    private LinearLayout submenuCard(int icon, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(glassPanel(dp(17)));
        ActionIconView image = new ActionIconView(this, icon);
        row.addView(image, new LinearLayout.LayoutParams(dp(48), dp(48)));
        View divider = new View(this);
        divider.setBackgroundColor(0x663879B7);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(1), dp(43));
        dividerLp.leftMargin = dp(10);
        dividerLp.rightMargin = dp(12);
        row.addView(divider, dividerLp);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(labels, labelsLp);
        labels.addView(text(title, 16, WHITE, true));
        TextView detail = text(subtitle, 13, MUTED, false);
        detail.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(3);
        labels.addView(detail, detailLp);
        TextView arrow = text("›", 28, 0xFFD4DDF4, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(25), -1));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(82));
        rowLp.bottomMargin = dp(9);
        row.setLayoutParams(rowLp);
        row.setFocusable(true);
        row.setClickable(true);
        row.setOnClickListener(click);
        row.setOnFocusChangeListener((view, focused) -> {
            row.setBackground(focused ? installerCardPanel(dp(17)) : glassPanel(dp(17)));
            float scale = focused ? 1.015f : 1f;
            row.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
        });
        return row;
    }

    private void showVersionSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsHeader(page, "Versión", "Información de la versión incluida en este instalador.");
        page.addView(submenuCard(6, "NIIVO Launcher", getEmbeddedLauncherVersion(), v ->
                showInfoDialog("Versión de NIIVO", "Esta es la versión real del launcher incluida en el instalador.")));
        page.addView(submenuCard(7, "Instalador NIIVO", getInstallerVersion(), v ->
                showInfoDialog("Instalador NIIVO", getInstallerVersion())));
    }

    private void showAboutSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsHeader(page, "Acerca de", "Versión, información legal y créditos.");

        LinearLayout brandCard = new LinearLayout(this);
        brandCard.setGravity(Gravity.CENTER_VERTICAL);
        brandCard.setPadding(dp(13), dp(12), dp(13), dp(12));
        brandCard.setBackground(installerCardPanel(dp(17)));
        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.drawable.installer_icon_foreground);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brandCard.addView(appIcon, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout brandLabels = new LinearLayout(this);
        brandLabels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandLabelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        brandLabelsLp.leftMargin = dp(12);
        brandCard.addView(brandLabels, brandLabelsLp);
        brandLabels.addView(text("NIIVO Launcher", 16, WHITE, true));
        brandLabels.addView(text(getEmbeddedLauncherVersion(), 13, MUTED, false));
        brandLabels.addView(text("Tu Fire TV, a tu manera.", 13, MUTED, false));
        LinearLayout.LayoutParams brandCardLp = new LinearLayout.LayoutParams(-1, dp(88));
        brandCardLp.bottomMargin = dp(10);
        page.addView(brandCard, brandCardLp);

        page.addView(submenuCard(17, "Licencias", "Software de terceros", v -> showThirdPartyLicenses()));
        page.addView(submenuCard(18, "Política de privacidad", "Lee nuestra política", v -> showPrivacyNotice()));
        page.addView(submenuCard(17, "Términos de uso", "Condiciones de uso", v -> showTermsNotice()));
        page.addView(submenuCard(19, "Créditos", "Agradecimientos", v ->
                showInfoDialog("Créditos", "NIIVO Launcher\nDesarrollado por isaacguzmanr.\n\nGracias a las bibliotecas de código abierto incluidas en este proyecto.")));
    }

    private void showPrivacySettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsHeader(page, "Privacidad", "Gestiona tus datos y preferencias de privacidad.");
        page.addView(submenuCard(14, "Permisos de la app", "Revisa los permisos otorgados", v -> openAppPermissions()));
        page.addView(submenuCard(15, "Recolección de datos", "Controla qué datos se recopilan", v ->
                showInfoDialog("Recolección de datos", "NIIVO Installer no integra analítica ni publicidad. Guarda localmente la IP del Fire TV y la clave de autorización ADB para facilitar futuras conexiones. La búsqueda de dispositivos y la instalación se realizan desde el teléfono hacia el Fire TV por la red local. Al comprobar versiones o descargar actualizaciones, la app se conecta a GitHub.")));
        page.addView(submenuCard(2, "Eliminar datos", "Borra datos y configuraciones", v -> confirmClearInstallerData(true)));
        page.addView(submenuCard(20, "Restablecer aplicación", "Vuelve al estado inicial", v -> confirmClearInstallerData(false)));
    }

    private void showKeepDataSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsHeader(page, "Mantener datos", "Conserva las preferencias de NIIVO Installer.");
        TextView explanation = text("Las actualizaciones de NIIVO se instalan sobre la versión existente y conservan los datos del launcher. Esta preferencia se aplica al proceso de desinstalación iniciado desde el instalador.", 15, MUTED, false);
        explanation.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams explanationLp = new LinearLayout.LayoutParams(-1, -2);
        explanationLp.bottomMargin = dp(16);
        page.addView(explanation, explanationLp);
        android.widget.Switch keepSwitch = new android.widget.Switch(this);
        keepSwitch.setText("Conservar datos al desinstalar");
        keepSwitch.setTextColor(WHITE);
        keepSwitch.setTextSize(15);
        keepSwitch.setChecked(keepDataOnUninstall);
        keepSwitch.setOnCheckedChangeListener((button, checked) -> {
            keepDataOnUninstall = checked;
            getPreferences(MODE_PRIVATE).edit().putBoolean("keep_data", checked).apply();
        });
        page.addView(keepSwitch);
    }

    private void showAdbSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsHeader(page, "Mantener ADB", "Mantiene activo el acceso ADB después de reiniciar.");
        String host = getPreferences(MODE_PRIVATE).getString("last_fire_tv_host", "").trim();
        TextView adbStatus = text(host.isEmpty() ? "Conecta un Fire TV para comprobar el estado." : "Comprobando Fire TV…", 12, MUTED, false);
        android.widget.Switch adbSwitch = new android.widget.Switch(this);
        adbSwitch.setText("Mantener ADB");
        adbSwitch.setTextColor(WHITE);
        adbSwitch.setTextSize(16);
        adbSwitch.setEnabled(false);
        adbSwitch.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFF168ED7));
        adbSwitch.setThumbTintList(android.content.res.ColorStateList.valueOf(WHITE));

        LinearLayout toggleCard = new LinearLayout(this);
        toggleCard.setGravity(Gravity.CENTER_VERTICAL);
        toggleCard.setPadding(dp(13), dp(10), dp(12), dp(10));
        toggleCard.setBackground(installerCardPanel(dp(17)));
        ActionIconView usb = new ActionIconView(this, 11);
        toggleCard.addView(usb, new LinearLayout.LayoutParams(dp(47), dp(47)));
        View divider = new View(this);
        divider.setBackgroundColor(0x663879B7);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(1), dp(42));
        dividerLp.leftMargin = dp(10);
        dividerLp.rightMargin = dp(12);
        toggleCard.addView(divider, dividerLp);
        LinearLayout toggleText = new LinearLayout(this);
        toggleText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams toggleTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        toggleCard.addView(toggleText, toggleTextLp);
        toggleText.addView(text("Mantener ADB", 15, WHITE, true));
        adbStatus.setText(host.isEmpty() ? "Conecta un Fire TV para comprobar" : "Activar después de reiniciar");
        LinearLayout.LayoutParams toggleSubLp = new LinearLayout.LayoutParams(-1, -2);
        toggleSubLp.topMargin = dp(3);
        toggleText.addView(adbStatus, toggleSubLp);
        toggleCard.addView(adbSwitch, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams toggleCardLp = new LinearLayout.LayoutParams(-1, dp(80));
        toggleCardLp.bottomMargin = dp(9);
        page.addView(toggleCard, toggleCardLp);

        page.addView(submenuCard(12, "Estado actual", "Comprobando Fire TV…", v -> refreshAdbState(host, adbSwitch, adbStatus, (TextView) ((LinearLayout) v).findViewWithTag("adb-subtitle"))));
        LinearLayout stateCard = (LinearLayout) page.getChildAt(page.getChildCount() - 1);
        TextView stateLabel = (TextView) ((LinearLayout) stateCard.getChildAt(2)).getChildAt(1);
        stateLabel.setTag("adb-subtitle");
        stateLabel.setText(host.isEmpty() ? "Conecta Fire TV para ver su estado" : "Comprobando Fire TV…");
        page.addView(submenuCard(12, "Aplicar ahora", "Activa la depuración ADB", v -> setFireAdbEnabled(host, true, adbSwitch, adbStatus)));
        adbStatus.setLineSpacing(dp(3), 1f);
        LinearLayout note = new LinearLayout(this);
        note.setPadding(dp(14), dp(12), dp(14), dp(12));
        note.setBackground(glassPanel(dp(15)));
        note.addView(text("i", 17, MUTED, true));
        TextView noteText = text("El ajuste se comprueba directamente en el Fire TV. El sistema puede requerir que vuelvas a autorizar la conexión ADB.", 12, MUTED, false);
        noteText.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        noteTextLp.leftMargin = dp(10);
        note.addView(noteText, noteTextLp);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(5);
        page.addView(note, noteLp);

        adbSwitch.setOnCheckedChangeListener((button, checked) -> setFireAdbEnabled(host, checked, adbSwitch, adbStatus));
        if (host.isEmpty()) adbStatus.setText("Conecta un Fire TV para comprobar el estado.");
        else refreshAdbState(host, adbSwitch, adbStatus, stateLabel);
    }

    private void refreshAdbState(String host, android.widget.Switch toggle, TextView statusLabel, TextView stateLabel) {
        if (host == null || host.isEmpty()) {
            statusLabel.setText("Conecta un Fire TV para comprobar el estado.");
            if (stateLabel != null) stateLabel.setText("Conecta Fire TV para ver su estado");
            return;
        }
        if (stateLabel != null) stateLabel.setText("Comprobando Fire TV…");
        toggle.setEnabled(false);
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                String value = runShell(connection, "settings get global adb_enabled").trim();
                boolean enabled = value.endsWith("1");
                runOnUiThread(() -> {
                    if (statusLabel.getParent() == null) return;
                    statusLabel.setText(enabled ? "ADB activo en Fire TV" : "ADB desactivado en Fire TV");
                    if (stateLabel != null) stateLabel.setText(enabled ? "ADB activo" : "ADB desactivado");
                    toggle.setOnCheckedChangeListener(null);
                    toggle.setChecked(enabled);
                    toggle.setEnabled(true);
                    toggle.setOnCheckedChangeListener((button, checked) -> setFireAdbEnabled(host, checked, toggle, statusLabel));
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if (statusLabel.getParent() == null) return;
                    statusLabel.setText("No se pudo comprobar ADB: " + clean(ex.getMessage()));
                    if (stateLabel != null) stateLabel.setText("No se pudo comprobar el estado");
                    toggle.setEnabled(false);
                });
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void setFireAdbEnabled(String host, boolean enabled, android.widget.Switch toggle, TextView statusLabel) {
        if (host == null || host.isEmpty()) {
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(false);
            toggle.setEnabled(false);
            statusLabel.setText("Conecta un Fire TV para cambiar ADB.");
            Toast.makeText(this, "Conecta el Fire TV desde Instalar para administrar ADB.", Toast.LENGTH_LONG).show();
            return;
        }
        toggle.setEnabled(false);
        statusLabel.setText(enabled ? "Activando ADB en Fire TV…" : "Desactivando ADB en Fire TV…");
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                String expected = enabled ? "1" : "0";
                String result = runShell(connection, "settings put global adb_enabled " + expected + "; settings get global adb_enabled").trim();
                if (!result.endsWith(expected)) throw new IOException("Fire TV no confirmó el cambio de ADB.");
                runOnUiThread(() -> {
                    if (statusLabel.getParent() == null) return;
                    statusLabel.setText(enabled ? "ADB activo en Fire TV" : "ADB desactivado en Fire TV");
                    toggle.setOnCheckedChangeListener(null);
                    toggle.setChecked(enabled);
                    toggle.setEnabled(true);
                    toggle.setOnCheckedChangeListener((button, checked) -> setFireAdbEnabled(host, checked, toggle, statusLabel));
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if (statusLabel.getParent() == null) return;
                    toggle.setOnCheckedChangeListener(null);
                    if (enabled) {
                        statusLabel.setText("No se confirmó el cambio de ADB.");
                        toggle.setChecked(false);
                        toggle.setEnabled(true);
                        toggle.setOnCheckedChangeListener((button, checked) -> setFireAdbEnabled(host, checked, toggle, statusLabel));
                    } else {
                        statusLabel.setText("No se pudo confirmar el nuevo estado. Vuelve a conectar el Fire TV.");
                        toggle.setChecked(false);
                        toggle.setEnabled(false);
                    }
                    Toast.makeText(this, clean(ex.getMessage()), Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
            }
        });
    }

    private void showSupportSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsHeader(page, "Soporte", "Obtén ayuda, reporta un problema o contáctanos.");
        page.addView(submenuCard(13, "Centro de ayuda", "Guías y preguntas frecuentes", v -> openWeb("https://github.com/isaacguzmanr/NIIVO-Fire-TV-Installer#readme")));
        page.addView(submenuCard(21, "Reportar un problema", "Envíanos un reporte", v -> openWeb("https://github.com/isaacguzmanr/NIIVO-Fire-TV-Installer/issues")));
        page.addView(submenuCard(14, "Contáctanos", "Perfil de contacto", v -> openWeb("https://github.com/isaacguzmanr")));
        page.addView(submenuCard(15, "Visitar sitio web", "niivo.app", v -> openWeb("https://niivo.app")));
    }

    private void openAppPermissions() {
        try {
            android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "No se pudo abrir la información de la app.", Toast.LENGTH_LONG).show();
        }
    }

    private void openEmailSupport() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO,
                android.net.Uri.parse("mailto:"));
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Ayuda con NIIVO Installer");
        try { startActivity(android.content.Intent.createChooser(intent, "Enviar correo a soporte")); }
        catch (Exception ex) { showInfoDialog("Contáctanos", "No hay una app de correo configurada. Puedes reportar el problema en GitHub: github.com/isaacguzmanr/NIIVO-Fire-TV-Installer/issues"); }
    }

    private void openWeb(String url) {
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
        catch (Exception ex) { Toast.makeText(this, "No se pudo abrir el enlace.", Toast.LENGTH_LONG).show(); }
    }

    private void showThirdPartyLicenses() {
        try (InputStream input = getAssets().open("THIRD_PARTY_NOTICES.txt")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            showInfoDialog("Licencias", output.toString("UTF-8"));
        } catch (Exception ex) {
            showInfoDialog("Licencias", "La biblioteca ADB incluida utiliza la licencia BSD 3-Clause.");
        }
    }

    private void showPrivacyNotice() {
        showInfoDialog("Política de privacidad", "NIIVO Installer guarda en el dispositivo la dirección IP del Fire TV y las claves ADB necesarias para la conexión. La búsqueda y transferencia del launcher ocurren por la red local. La app no incluye analítica ni publicidad. Para comprobar versiones y descargar APK, se conecta a GitHub.");
    }

    private void showTermsNotice() {
        showInfoDialog("Términos de uso", "NIIVO Installer permite instalar NIIVO en dispositivos Fire TV compatibles. Las marcas y aplicaciones de terceros pertenecen a sus respectivos titulares. NIIVO es un proyecto independiente.");
    }

    private void showInfoDialog(String title, String message) {
        new android.app.AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("Aceptar", null).show();
    }

    private void confirmClearInstallerData(boolean removeAdbKey) {
        String action = removeAdbKey ? "Se borrarán la IP guardada, las preferencias y la clave ADB local." : "Se restablecerán las preferencias de NIIVO Installer.";
        new android.app.AlertDialog.Builder(this).setTitle(removeAdbKey ? "Eliminar datos" : "Restablecer aplicación")
                .setMessage(action + " Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (dialog, which) -> {
                    getPreferences(MODE_PRIVATE).edit().clear().apply();
                    if (removeAdbKey) {
                        new File(getFilesDir(), "nivo-adbkey").delete();
                        new File(getFilesDir(), "nivo-adbkey.pub").delete();
                        clearDirectory(getCacheDir());
                    }
                    keepDataOnUninstall = true;
                    Toast.makeText(this, "Los datos locales se restablecieron.", Toast.LENGTH_LONG).show();
                    showSettings();
                }).show();
    }

    private void clearDirectory(File directory) {
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) clearDirectory(child);
            child.delete();
        }
    }
    private void buildInstallerPage() {
        if (uninstallMode) {
            buildUninstallPage();
            return;
        }
        buildConnectionPage();
    }

    private void buildConnectionPage() {
        updatesScreenVisible = false;
        settingsScreenVisible = false;
        settingsDetailVisible = false;
        installerScreenVisible = true;
        LinearLayout page = newPage();
        page.setBackground(homeGradient());
        addSettingsHeader(page, "Antes de instalar",
                "Realiza estos pasos en tu Fire TV\npara una instalación correcta.", () -> {
                    uninstallMode = false;
                    showHome();
                });
        page.addView(connectionStepCard("1", 3, "Antes de depuración ADB",
                "Ve a Ajustes > Mi Fire TV > Opciones para desarrolladores y activa Depuración ADB."));
        page.addView(connectionStepCard("2", 23, "Conecta a la misma Wi‑Fi",
                "Tu teléfono y Fire TV deben estar en la misma red."));
        page.addView(connectionStepCard("3", 24, "Acepta el aviso de depuración",
                "En tu Fire TV, acepta la solicitud de depuración cuando aparezca."));
        addConnectionControls(page, false);
    }

    private void buildUninstallPage() {
        updatesScreenVisible = false;
        settingsScreenVisible = false;
        settingsDetailVisible = false;
        installerScreenVisible = true;
        keepDataOnUninstall = getPreferences(MODE_PRIVATE).getBoolean("keep_data", true);
        LinearLayout page = newPage();
        page.setBackground(homeGradient());
        addSettingsHeader(page, "Antes de desinstalar",
                "Revisa la siguiente información antes\nde continuar.", () -> {
                    uninstallMode = false;
                    showHome();
                });
        page.addView(connectionInfoCard(2, "Se eliminará NIIVO",
                "El launcher actual se desinstalará de tu Fire TV."));
        page.addView(connectionInfoCard(4, "Se restaurará el launcher original",
                "Tu Fire TV volverá al launcher de Amazon al finalizar."));
        page.addView(connectionInfoCard(3, "Tus datos se mantendrán",
                keepDataOnUninstall
                        ? "Esta acción no afecta tus aplicaciones o configuraciones del Fire TV."
                        : "La configuración local de NIIVO se eliminará al desinstalar."));
        page.addView(connectionInfoCard(7, "Se requiere conexión ADB",
                "Tu teléfono y Fire TV deben estar en la misma red y tener ADB habilitado."));
        addConnectionControls(page, true);
    }

    private View connectionStepCard(String number, int icon, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(9), dp(12), dp(9));
        row.setBackground(glassPanel(dp(17)));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(82));
        rowLp.bottomMargin = dp(9);
        row.setLayoutParams(rowLp);

        TextView badge = text(number, 14, WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(0xFF075079, dp(22), 0x6646BFFF, dp(1)));
        row.addView(badge, new LinearLayout.LayoutParams(dp(29), dp(29)));
        ActionIconView stepIcon = new ActionIconView(this, icon);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        iconLp.leftMargin = dp(9);
        iconLp.rightMargin = dp(10);
        row.addView(stepIcon, iconLp);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        labels.addView(text(title, 14, WHITE, true));
        TextView detail = text(subtitle, 11, MUTED, false);
        detail.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(3);
        labels.addView(detail, detailLp);
        return row;
    }

    private View connectionInfoCard(int icon, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(7), dp(10), dp(7));
        row.setBackground(glassPanel(dp(16)));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(68));
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);
        ActionIconView iconView = new ActionIconView(this, icon);
        row.addView(iconView, new LinearLayout.LayoutParams(dp(40), dp(40)));
        View divider = new View(this);
        divider.setBackgroundColor(0x663879B7);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(1), dp(44));
        dividerLp.setMargins(dp(10), 0, dp(12), 0);
        row.addView(divider, dividerLp);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        labels.addView(text(title, 14, WHITE, true));
        TextView sub = text(subtitle, 11, MUTED, false);
        sub.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(2);
        labels.addView(sub, subLp);
        return row;
    }

    private void addConnectionControls(LinearLayout page, boolean removing) {
        searchButton = button("Buscar Fire TV en mi Wi‑Fi", true);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(52));
        searchLp.topMargin = dp(7);
        searchLp.bottomMargin = dp(7);
        page.addView(searchButton, searchLp);
        searchButton.setOnClickListener(v -> scanNetwork());

        LinearLayout note = new LinearLayout(this);
        note.setGravity(Gravity.TOP | Gravity.START);
        note.setPadding(dp(3), dp(4), dp(2), dp(4));
        ActionIconView infoIcon = new ActionIconView(this, 7);
        note.addView(infoIcon, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView hint = text("Si no aparece tu Fire TV, puedes escribir la IP manualmente.", 12, MUTED, false);
        hint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(0, -2, 1f);
        hintLp.leftMargin = dp(10);
        note.addView(hint, hintLp);
        page.addView(note);

        LinearLayout ipBox = new LinearLayout(this);
        ipBox.setGravity(Gravity.CENTER_VERTICAL);
        ipBox.setPadding(dp(12), 0, dp(12), 0);
        ipBox.setBackground(glassPanel(dp(15)));
        ActionIconView wifi = new ActionIconView(this, 23);
        ipBox.addView(wifi, new LinearLayout.LayoutParams(dp(30), dp(30)));
        View divider = new View(this);
        divider.setBackgroundColor(0x663879B7);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(dp(1), dp(32));
        dividerLp.setMargins(dp(8), 0, dp(12), 0);
        ipBox.addView(divider, dividerLp);
        ipInput = new EditText(this);
        ipInput.setSingleLine(true);
        ipInput.setHint("IP del Fire TV (ej. 192.168.1.7)");
        ipInput.setHintTextColor(0xFF91A6C2);
        ipInput.setTextColor(WHITE);
        ipInput.setTextSize(14);
        ipInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        ipInput.setPadding(0, 0, 0, 0);
        ipInput.setBackgroundColor(Color.TRANSPARENT);
        ipBox.addView(ipInput, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams ipLp = new LinearLayout.LayoutParams(-1, dp(50));
        ipLp.topMargin = dp(4);
        page.addView(ipBox, ipLp);

        installButton = button(removing ? "Desinstalar NIIVO" : "Continuar", true);
        installButton.setEnabled(false);
        installButton.setAlpha(0.5f);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(-1, dp(54));
        actionLp.topMargin = dp(12);
        page.addView(installButton, actionLp);
        ipInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                installButton.setEnabled(searchButton.isEnabled() && isValidIp(s.toString().trim()));
                installButton.setAlpha(installButton.isEnabled() ? 1f : 0.5f);
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        installButton.setOnClickListener(v -> installSelected());

        devices = text("", 12, MUTED, false);
        devices.setVisibility(View.GONE);
        LinearLayout.LayoutParams devicesLp = new LinearLayout.LayoutParams(-1, -2);
        devicesLp.topMargin = dp(7);
        page.addView(devices, devicesLp);
        status = text("", 12, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setVisibility(View.GONE);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(6);
        page.addView(status, statusLp);
    }

    private void showProgressScreen(boolean removing) {
        LinearLayout page = newPage();
        page.setBackground(homeGradient());
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(18), dp(2), dp(18), dp(22));

        android.widget.FrameLayout topBar = new android.widget.FrameLayout(this);
        TextView back = text("‹", 31, WHITE, false);
        back.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams backLp = new android.widget.FrameLayout.LayoutParams(dp(44), dp(46), Gravity.START | Gravity.CENTER_VERTICAL);
        topBar.addView(back, backLp);
        back.setContentDescription("Cancelar operación");
        back.setOnClickListener(v -> confirmCancelOperation(removing));
        page.addView(topBar, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView title = text(removing ? "Desinstalación" : "Instalación", 29, WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(13);
        page.addView(title, titleLp);
        TextView intro = text(removing ? "Desinstala NIIVO de tu Fire TV." : "Instala la última versión de NIIVO\nen tu dispositivo.", 15, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(7);
        introLp.bottomMargin = dp(17);
        page.addView(intro, introLp);

        LinearLayout appCard = new LinearLayout(this);
        appCard.setGravity(Gravity.CENTER_VERTICAL);
        appCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        appCard.setBackground(installerCardPanel(dp(20)));
        android.widget.ImageView appIcon = new android.widget.ImageView(this);
        appIcon.setImageResource(R.drawable.nivo_update_mark);
        appIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        appIcon.setBackground(round(0xFF061020, dp(18), 0x334A72BA, dp(1)));
        appIcon.setPadding(dp(9), dp(9), dp(9), dp(9));
        appCard.addView(appIcon, new LinearLayout.LayoutParams(dp(68), dp(68)));
        LinearLayout appCopy = new LinearLayout(this);
        appCopy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams appCopyLp = new LinearLayout.LayoutParams(0, -2, 1f);
        appCopyLp.leftMargin = dp(14);
        appCard.addView(appCopy, appCopyLp);
        appCopy.addView(text("NIIVO Launcher", 18, WHITE, true));
        TextView appVersion = text(getEmbeddedLauncherVersion().replace("Versión ", ""), 14, MUTED, false);
        LinearLayout.LayoutParams appVersionLp = new LinearLayout.LayoutParams(-1, -2);
        appVersionLp.topMargin = dp(3);
        appCopy.addView(appVersion, appVersionLp);
        TextView appKind = text("Launcher para Fire TV", 13, MUTED, false);
        LinearLayout.LayoutParams appKindLp = new LinearLayout.LayoutParams(-1, -2);
        appKindLp.topMargin = dp(3);
        appCopy.addView(appKind, appKindLp);
        LinearLayout.LayoutParams appCardLp = new LinearLayout.LayoutParams(-1, -2);
        appCardLp.bottomMargin = dp(10);
        page.addView(appCard, appCardLp);

        LinearLayout progressCard = new LinearLayout(this);
        progressCard.setOrientation(LinearLayout.VERTICAL);
        progressCard.setPadding(dp(15), dp(13), dp(15), dp(13));
        progressCard.setBackground(glassPanel(dp(19)));
        LinearLayout progressHeading = new LinearLayout(this);
        progressHeading.setGravity(Gravity.CENTER_VERTICAL);
        operationProgressTitle = text(removing ? "Desinstalando…" : "Instalando…", 17, WHITE, true);
        progressHeading.addView(operationProgressTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        operationProgressPercent = text("0%", 17, CYAN, true);
        operationProgressPercent.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        progressHeading.addView(operationProgressPercent);
        progressCard.addView(progressHeading);
        operationProgressSubtitle = text("Esto puede tardar unos segundos.", 13, MUTED, false);
        LinearLayout.LayoutParams progressSubLp = new LinearLayout.LayoutParams(-1, -2);
        progressSubLp.topMargin = dp(3);
        progressCard.addView(operationProgressSubtitle, progressSubLp);
        operationProgressBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        operationProgressBar.setMax(100);
        operationProgressBar.setProgress(0);
        operationProgressBar.setProgressDrawable(operationProgressDrawable());
        LinearLayout.LayoutParams progressBarLp = new LinearLayout.LayoutParams(-1, dp(8));
        progressBarLp.topMargin = dp(13);
        progressCard.addView(operationProgressBar, progressBarLp);
        LinearLayout.LayoutParams progressCardLp = new LinearLayout.LayoutParams(-1, -2);
        progressCardLp.bottomMargin = dp(10);
        page.addView(progressCard, progressCardLp);

        LinearLayout stepsCard = new LinearLayout(this);
        stepsCard.setOrientation(LinearLayout.VERTICAL);
        stepsCard.setPadding(dp(14), dp(7), dp(14), dp(7));
        stepsCard.setBackground(glassPanel(dp(19)));
        String[] stepTitles = removing
                ? new String[]{"Conectando con Fire TV", "Desinstalando NIIVO", "Restaurando inicio", "Finalizando"}
                : new String[]{"Copiando paquete", "Verificando archivo", "Instalando aplicación", "Finalizando"};
        String[] stepSubtitles = removing
                ? new String[]{"Esperando autorización ADB", "Quitando NIIVO del dispositivo", "Launcher predeterminado de Fire TV", "Proceso completado"}
                : new String[]{getEmbeddedLauncherVersion(), "Comprobando integridad del paquete", "En tu dispositivo", "Configurando launcher"};
        int[] stepIcons = removing ? new int[]{1, 2, 4, 10} : new int[]{9, 7, 3, 10};
        for (int i = 0; i < stepTitles.length; i++) {
            LinearLayout row = operationStepRow(i, stepIcons[i], stepTitles[i], stepSubtitles[i]);
            stepsCard.addView(row, new LinearLayout.LayoutParams(-1, dp(58)));
            if (i < stepTitles.length - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0x332F5684);
                LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
                dividerLp.leftMargin = dp(55);
                dividerLp.rightMargin = dp(3);
                stepsCard.addView(divider, dividerLp);
            }
        }
        LinearLayout.LayoutParams stepsCardLp = new LinearLayout.LayoutParams(-1, -2);
        stepsCardLp.bottomMargin = dp(12);
        page.addView(stepsCard, stepsCardLp);

        TextView installing = text(removing ? "Desinstalando…" : "Instalando…", 15, 0xB8FFFFFF, true);
        installing.setGravity(Gravity.CENTER);
        installing.setBackground(gradientButton(dp(22)));
        installing.setEnabled(false);
        page.addView(installing, new LinearLayout.LayoutParams(-1, dp(58)));
        updateOperationProgress(0, 0, removing ? "Conectando con Fire TV…" : "Conectando con Fire TV…", "Acepta la solicitud de depuración ADB en la televisión.");
    }

    private void setProgressActivity(boolean moving) {
        runOnUiThread(() -> {
            if (operationActiveSpinner != null) operationActiveSpinner.setVisibility(moving ? View.VISIBLE : View.INVISIBLE);
        });
    }

    private Drawable operationProgressDrawable() {
        GradientDrawable track = new GradientDrawable();
        track.setColor(0xAA172543);
        track.setCornerRadius(dp(8));
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{CYAN, BLUE, PURPLE});
        fill.setCornerRadius(dp(8));
        android.graphics.drawable.ClipDrawable clipped = new android.graphics.drawable.ClipDrawable(
                fill, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(
                new Drawable[]{track, clipped});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        return layers;
    }

    private LinearLayout operationStepRow(int index, int iconKind, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout iconHolder = new FrameLayout(this);
        iconHolder.setBackground(round(0xD1271CE8, dp(28), 0x443F6BFF, dp(1)));
        ActionIconView icon = new ActionIconView(this, iconKind);
        iconHolder.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconLp.rightMargin = dp(12);
        row.addView(iconHolder, iconLp);
        operationStepIcons[index] = iconHolder;

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        operationStepTitles[index] = text(title, 14, WHITE, false);
        copy.addView(operationStepTitles[index]);
        operationStepSubtitles[index] = text(subtitle, 12, MUTED, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(2);
        copy.addView(operationStepSubtitles[index], subLp);

        FrameLayout badge = new FrameLayout(this);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        badgeLp.leftMargin = dp(8);
        row.addView(badge, badgeLp);
        TextView check = text("✓", 16, 0xFF20E6B0, true);
        check.setGravity(Gravity.CENTER);
        check.setBackground(round(0x4420DDAA, dp(16), 0, 0));
        badge.addView(check, new FrameLayout.LayoutParams(-1, -1));
        check.setVisibility(View.GONE);
        android.widget.ProgressBar spinner = new android.widget.ProgressBar(this);
        if (android.os.Build.VERSION.SDK_INT >= 21) spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(CYAN));
        badge.addView(spinner, new FrameLayout.LayoutParams(-1, -1));
        spinner.setVisibility(View.GONE);
        TextView pending = text("", 12, 0xFF465575, false);
        pending.setBackground(round(0x00304060, dp(16), 0x99435578, dp(2)));
        badge.addView(pending, new FrameLayout.LayoutParams(-1, -1));
        operationStepStatuses[index] = badge;
        return row;
    }

    private void updateOperationProgress(int percent, int activeStep, String title, String subtitle) {
        runOnUiThread(() -> {
            if (operationProgressBar == null) return;
            int safe = Math.max(0, Math.min(100, percent));
            operationProgressBar.setProgress(safe);
            operationProgressPercent.setText(safe + "%");
            operationProgressTitle.setText(title);
            operationProgressSubtitle.setText(subtitle);
            operationActiveSpinner = null;
            for (int i = 0; i < operationStepStatuses.length; i++) {
                FrameLayout badge = (FrameLayout) operationStepStatuses[i];
                TextView check = (TextView) badge.getChildAt(0);
                android.widget.ProgressBar spinner = (android.widget.ProgressBar) badge.getChildAt(1);
                TextView pending = (TextView) badge.getChildAt(2);
                if (i < activeStep) {
                    check.setVisibility(View.VISIBLE);
                    spinner.setVisibility(View.GONE);
                    pending.setVisibility(View.GONE);
                    operationStepTitles[i].setTextColor(WHITE);
                    operationStepIcons[i].setAlpha(1f);
                } else if (i == activeStep) {
                    check.setVisibility(View.GONE);
                    spinner.setVisibility(View.VISIBLE);
                    pending.setVisibility(View.GONE);
                    operationActiveSpinner = spinner;
                    operationStepTitles[i].setTextColor(WHITE);
                    operationStepIcons[i].setAlpha(1f);
                } else {
                    check.setVisibility(View.GONE);
                    spinner.setVisibility(View.GONE);
                    pending.setVisibility(View.VISIBLE);
                    operationStepTitles[i].setTextColor(0xFF8290AB);
                    operationStepSubtitles[i].setTextColor(0xFF75829A);
                    operationStepIcons[i].setAlpha(0.48f);
                }
            }
        });
    }

    private void confirmCancelOperation(boolean removing) {
        if (!operationRunning) {
            buildInstallerPage();
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancelar operación")
                .setMessage(removing ? "¿Quieres detener la desinstalación?" : "¿Quieres detener la instalación?")
                .setNegativeButton("Continuar", null)
                .setPositiveButton("Cancelar", (dialog, which) -> cancelCurrentOperation())
                .show();
    }

    private void cancelCurrentOperation() {
        cancelRequested = true;
        setProgressActivity(false);
        AdbConnection connection = activeConnection;
        Socket socket = activeSocket;
        if (connection != null) try { connection.close(); } catch (Exception ignored) {}
        if (socket != null) try { socket.close(); } catch (Exception ignored) {}
    }
    private void showSuccess(boolean removed, String host) {
        LinearLayout page = newPage();
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(18), 0, dp(18), 0);

        View topSpace = new View(this);
        page.addView(topSpace, new LinearLayout.LayoutParams(-1, 0, 2.4f));

        View check = new SuccessRingView(this);
        page.addView(check, new LinearLayout.LayoutParams(dp(156), dp(156)));

        TextView title = text(removed ? "Desinstalado" : "¡Listo!", 24, WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(19);
        page.addView(title, titleLp);

        TextView message = text(removed ? "NIIVO se quitó correctamente." : "NIIVO se instaló correctamente.", 14, 0xFFE3EAF2, false);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageLp = new LinearLayout.LayoutParams(-1, -2);
        messageLp.topMargin = dp(8);
        page.addView(message, messageLp);

        if (!removed) {
            View open = successButton(true, "Abrir Launcher", v -> launchNiivo(host));
            LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(-1, dp(64));
            openLp.topMargin = dp(40);
            page.addView(open, openLp);
        }

        View close = successButton(false, "Cerrar", v -> showHome());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(-1, dp(64));
        closeLp.topMargin = dp(18);
        page.addView(close, closeLp);

        View bottomSpace = new View(this);
        page.addView(bottomSpace, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    private View successButton(boolean primary, String label, View.OnClickListener click) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setBackground(primary ? gradientButton(dp(24)) : successOutline(dp(24)));
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(click);
        button.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) button.setAlpha(0.78f);
            if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) button.setAlpha(1f);
            return false;
        });
        if (primary) {
            View play = new View(this) {
                private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                @Override protected void onDraw(Canvas canvas) {
                    super.onDraw(canvas);
                    paint.setColor(WHITE);
                    Path triangle = new Path();
                    float cx = getWidth() / 2f, cy = getHeight() / 2f;
                    triangle.moveTo(cx - dp(5), cy - dp(8));
                    triangle.lineTo(cx + dp(8), cy);
                    triangle.lineTo(cx - dp(5), cy + dp(8));
                    triangle.close();
                    canvas.drawPath(triangle, paint);
                }
            };
            button.addView(play, new LinearLayout.LayoutParams(dp(20), dp(24)));
            TextView labelView = text(label, 16, WHITE, false);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(-2, -2);
            labelLp.leftMargin = dp(10);
            button.addView(labelView, labelLp);
        } else {
            button.addView(text(label, 16, WHITE, false));
        }
        return button;
    }
    private void showFailure(String message) {
        LinearLayout page = newPage();
        addBack(page, "NIIVO");
        TextView title = text("No se completó", 25, WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(34);
        page.addView(title, titleLp);
        TextView detail = text(message + "\n\nComprueba que ADB esté activado, que teléfono y TV estén en la misma Wi‑Fi y que aceptaste el aviso en el televisor.", 14, MUTED, false);
        detail.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(12);
        page.addView(detail, detailLp);
        Button retry = button("Intentar de nuevo", true);
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(-1, dp(54));
        retryLp.topMargin = dp(24);
        page.addView(retry, retryLp);
        retry.setOnClickListener(v -> buildInstallerPage());
    }

    private void launchNiivo(String host) {
        Toast.makeText(this, "Abriendo NIIVO en Fire TV…", Toast.LENGTH_SHORT).show();
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                runShell(connection, "am start -n " + FIRE_PACKAGE + "/.HomeActivity");
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this, "No pude abrir el launcher: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            } finally { if (connection != null) try { connection.close(); } catch (Exception ignored) {} }
        });
    }
    private void scanNetwork() {
        searchButton.setEnabled(false);
        installButton.setEnabled(false);
        devices.setVisibility(View.VISIBLE);
        status.setVisibility(View.VISIBLE);
        status.setText("Buscando dispositivos ADB en esta red…");
        devices.setText("Buscando Fire TV en la red Wi‑Fi…");
        found.clear();
        WORK.execute(() -> {
            List<String> candidates = new ArrayList<>();
            try {
                List<InetAddress> addresses = getScanAddresses();
                int total = addresses.size();
                java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(48);
                List<java.util.concurrent.Future<String>> futures = new ArrayList<>();
                for (InetAddress address : addresses) {
                    futures.add(pool.submit(() -> hasAdbPort(address) ? address.getHostAddress() : null));
                }
                int done = 0;
                for (java.util.concurrent.Future<String> future : futures) {
                    String host = future.get();
                    if (host != null) candidates.add(host);
                    done++;
                    final int progress = done;
                    runOnUiThread(() -> status.setText("Buscando ADB… " + progress + " de " + total));
                }
                pool.shutdownNow();
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("No pude recorrer esta red. Escribe la IP de la TV abajo."));
            }
            Collections.sort(candidates);
            runOnUiThread(() -> {
                found.addAll(candidates);
                searchButton.setEnabled(true);
                if (found.isEmpty()) {
                    devices.setText("No encontré Fire TV con ADB en el puerto 5555. Comprueba la Wi‑Fi o escribe su IP manualmente.");
                    status.setText("La búsqueda terminó.");
                    installButton.setEnabled(isValidIp(ipInput.getText().toString().trim()));
                } else {
                    StringBuilder labels = new StringBuilder();
                    for (int i = 0; i < found.size(); i++) {
                        if (i > 0) labels.append("\n");
                        labels.append("• Fire TV disponible: ").append(found.get(i));
                    }
                    devices.setText(labels.toString());
                    if (found.size() == 1) {
                        ipInput.setText(found.get(0));
                        status.setText("Encontré un dispositivo ADB. Voy a comprobar que sea un Fire TV antes de instalar.");
                        installButton.setEnabled(true);
                    } else {
                        ipInput.setText("");
                        status.setText("Encontré varios dispositivos ADB. Escribe la IP del Fire TV que quieres actualizar.");
                        installButton.setEnabled(false);
                    }
                }
            });
        });
    }

    private List<InetAddress> getScanAddresses() throws Exception {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network network = manager == null ? null : manager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null ? null : manager.getNetworkCapabilities(network);
        if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            throw new IOException("Conecta el teléfono a la misma red Wi‑Fi del Fire TV.");
        }
        LinkProperties properties = manager.getLinkProperties(network);
        if (properties == null) throw new IOException("No pude leer la red Wi‑Fi.");
        List<InetAddress> result = new ArrayList<>();
        for (LinkAddress entry : properties.getLinkAddresses()) {
            InetAddress addr = entry.getAddress();
            if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
            byte[] b = addr.getAddress();
            int ip = ((b[0]&255)<<24)|((b[1]&255)<<16)|((b[2]&255)<<8)|(b[3]&255);
            int prefix = entry.getPrefixLength();
            if (prefix < 1 || prefix > 30) prefix = 24;
            if (prefix < 24) prefix = 24; // limit wide networks to this phone's /24
            int mask = (int)(0xffffffffL << (32-prefix));
            int base = ip & mask;
            int count = 1 << (32-prefix);
            for (int i=1; i<count-1; i++) {
                int host = base | i;
                if (host == ip) continue;
                byte[] out = new byte[]{(byte)(host>>>24),(byte)(host>>>16),(byte)(host>>>8),(byte)host};
                result.add(InetAddress.getByAddress(out));
            }
        }
        if (result.isEmpty()) throw new IOException("No pude obtener una dirección IPv4 de la Wi‑Fi.");
        return result;
    }
    private boolean hasAdbPort(InetAddress address) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, 5555), 350);
            return true;
        } catch (Exception ignored) { return false; }
    }

    private void installSelected() {
        keepDataOnUninstall = getPreferences(MODE_PRIVATE).getBoolean("keep_data", true);
        String host = ipInput.getText().toString().trim();
        if (!isValidIp(host)) {
            Toast.makeText(this, "Escribe una dirección IP válida de la TV.", Toast.LENGTH_LONG).show();
            return;
        }
        activeHost = host;
        getPreferences(MODE_PRIVATE).edit().putString("last_fire_tv_host", host).apply();
        cancelRequested = false;
        operationRunning = true;
        activeSocket = null;
        activeConnection = null;
        showProgressScreen(uninstallMode);
        setProgressActivity(true);
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                AdbConnection active = connection;
                if (cancelRequested) throw new IOException("Operación cancelada.");
                updateOperationProgress(4, 0, "Conectando con Fire TV…", "Fire TV conectado; comprobando el dispositivo.");

                String deviceInfo = runShell(active, "getprop ro.product.manufacturer; getprop ro.product.model");
                String deviceLower = deviceInfo.toLowerCase(java.util.Locale.ROOT);
                if (!deviceLower.contains("amazon") && !deviceLower.contains("aft")) {
                    throw new IOException("El dispositivo " + host + " no se identificó como Fire TV. No instalé nada. " + clean(deviceInfo));
                }
                if (uninstallMode) {
                    updateOperationProgress(35, 1, "Desinstalando aplicación…", "Quitando NIIVO del Fire TV.");
                    String command = keepDataOnUninstall ? "pm uninstall -k " : "pm uninstall ";
                    String result = runShell(active, command + FIRE_PACKAGE);
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    if (!result.contains("Success")) throw new IOException("No se pudo desinstalar NIIVO. " + clean(result));
                    updateOperationProgress(88, 3, "Finalizando…", "Fire TV volverá a su launcher predeterminado.");
                } else {
                    File apk = prepareBundledApk();
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    updateOperationProgress(8, 0, "Enviando paquete…", "Copiando 0.0 MB de " + formatMegabytes(apk.length()) + " al Fire TV.");
                    pushApk(active, apk, "/data/local/tmp/nivo.apk", (sent, total) -> {
                        int transfer = total <= 0 ? 0 : (int) Math.min(1_000L, sent * 1_000L / total);
                        int progress = 8 + transfer * 57 / 1_000;
                        updateOperationProgress(progress, 0, "Enviando paquete…",
                                "Copiando " + formatMegabytes(sent) + " de " + formatMegabytes(total) + " al Fire TV.");
                    });
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    updateOperationProgress(65, 1, "Verificando archivo…", "Comprobando el paquete transferido al Fire TV.");
                    String remoteSizeText = runShell(active, "wc -c < /data/local/tmp/nivo.apk").trim();
                    java.util.regex.Matcher remoteSizeMatch = java.util.regex.Pattern.compile("(\\d+)").matcher(remoteSizeText);
                    if (!remoteSizeMatch.find() || Long.parseLong(remoteSizeMatch.group(1)) != apk.length()) {
                        throw new IOException("La comprobación del paquete copiado al Fire TV no coincide con el tamaño original.");
                    }
                    updateOperationProgress(68, 2, "Instalando aplicación…", "Fire TV está aplicando el paquete.");

                    String install = runShell(active, "pm install -r -g /data/local/tmp/nivo.apk");
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    if (!install.contains("Success")) throw new IOException("La TV no aceptó la instalación. " + clean(install));

                    updateOperationProgress(88, 3, "Finalizando…", "Configurando NIIVO como launcher.");
                    enableLauncher(active);
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                }
                updateOperationProgress(100, 4, uninstallMode ? "Desinstalación completada" : "Instalación completada", "Proceso terminado correctamente.");
                runOnUiThread(() -> {
                    operationRunning = false;
                    showSuccess(uninstallMode, host);
                });
            } catch (Exception ex) {
                if (cancelRequested) {
                    runOnUiThread(() -> {
                        operationRunning = false;
                        buildInstallerPage();
                        Toast.makeText(this, "Operación cancelada.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    runOnUiThread(() -> {
                        operationRunning = false;
                        showFailure(message);
                    });
                }
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
                activeConnection = null;
                activeSocket = null;
                operationRunning = false;
            }
        });
    }    private AdbConnection connect(String host) throws Exception {
        File privateKey = new File(getFilesDir(), "nivo-adbkey");
        File publicKey = new File(getFilesDir(), "nivo-adbkey.pub");
        AdbBase64 encoder = data -> Base64.encodeToString(data, Base64.NO_WRAP);
        AdbCrypto crypto;
        if (privateKey.exists() && publicKey.exists()) {
            crypto = AdbCrypto.loadAdbKeyPair(encoder, privateKey, publicKey);
        } else {
            crypto = AdbCrypto.generateAdbKeyPair(encoder);
            crypto.saveAdbKeyPair(privateKey, publicKey);
        }
        Socket socket = new Socket();
        activeSocket = socket;
        if (cancelRequested) { socket.close(); throw new IOException("Operación cancelada."); }
        socket.connect(new InetSocketAddress(host, 5555), 10000);
        socket.setSoTimeout(120000);
        AdbConnection conn = AdbConnection.create(socket, crypto);
        activeConnection = conn;
        if (operationRunning) setProgressActivity(false);
        boolean ok = conn.connect(120, TimeUnit.SECONDS, false);
        if (operationRunning) setProgressActivity(true);
        if (cancelRequested) { conn.close(); throw new IOException("Operación cancelada."); }
        if (!ok) {
            conn.close();
            throw new IOException("Tiempo agotado esperando la autorización ADB en la pantalla del Fire TV.");
        }
        return conn;
    }

    private void pushApk(AdbConnection conn) throws Exception {
        File apk = prepareBundledApk();
        pushApk(conn, apk, "/data/local/tmp/nivo.apk");
    }

    private File prepareBundledApk() throws Exception {
        File apk = new File(getCacheDir(), "nivo.apk");
        long totalBytes;
        try (InputStream sizeStream = getAssets().open("nivo.apk")) { totalBytes = sizeStream.available(); }
        try (InputStream in = getAssets().open("nivo.apk"); FileOutputStream out = new FileOutputStream(apk)) {
            byte[] buffer = new byte[32768];
            int n;
            long copied = 0;
            while ((n = in.read(buffer)) != -1) {
                if (cancelRequested) throw new IOException("Operación cancelada.");
                out.write(buffer, 0, n);
                copied += n;
                int preparation = (int) Math.min(4, copied * 4 / Math.max(1, totalBytes));
                updateOperationProgress(preparation, 0, "Preparando paquete…", "Leyendo el APK incluido en NIIVO Installer.");
            }
        }
        if (!apk.isFile() || apk.length() == 0) throw new IOException("El paquete de NIIVO está vacío.");
        boolean hasManifest = false;
        boolean hasDex = false;
        try (ZipFile zip = new ZipFile(apk)) {
            hasManifest = zip.getEntry("AndroidManifest.xml") != null;
            for (java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
                String name = entries.nextElement().getName();
                if (name.matches("classes[0-9]*\\.dex")) { hasDex = true; break; }
            }
        } catch (Exception invalid) {
            throw new IOException("No se pudo verificar el APK incluido.", invalid);
        }
        if (!hasManifest || !hasDex) throw new IOException("El APK incluido no pasó la verificación de integridad.");
        android.content.pm.PackageInfo info = getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (info == null || !FIRE_PACKAGE.equals(info.packageName)) throw new IOException("El APK incluido no corresponde al launcher NIIVO.");
        updateOperationProgress(8, 0, "Verificando archivo…", "APK válido, versión " + info.versionName + ".");
        return apk;
    }

    private void pushApk(AdbConnection conn, File apk, String remotePath) throws Exception {
        pushApk(conn, apk, remotePath, null);
    }

    private interface TransferProgressListener { void onProgress(long transferred, long total); }

    private void pushApk(AdbConnection conn, File apk, String remotePath, TransferProgressListener progress) throws Exception {
        AdbStream stream = conn.open("sync:");
        try {
            byte[] spec = (remotePath + ",420").getBytes(StandardCharsets.UTF_8);
            ByteBuffer send = ByteBuffer.allocate(8 + spec.length).order(ByteOrder.LITTLE_ENDIAN);
            send.put(new byte[]{'S','E','N','D'}).putInt(spec.length).put(spec);
            writeChunked(stream, send.array(), 4000);
            try (InputStream in = new java.io.FileInputStream(apk)) {
                byte[] fileChunk = new byte[3900];
                int n;
                long transferred = 0;
                long total = apk.length();
                long lastProgress = 0;
                while ((n = in.read(fileChunk)) != -1) {
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    ByteBuffer packet = ByteBuffer.allocate(8+n).order(ByteOrder.LITTLE_ENDIAN);
                    packet.put(new byte[]{'D','A','T','A'}).putInt(n).put(fileChunk,0,n);
                    writeChunked(stream, packet.array(), 4000);
                    transferred += n;
                    if (progress != null && (transferred - lastProgress >= 65536 || transferred == total)) {
                        progress.onProgress(transferred, total);
                        lastProgress = transferred;
                    }
                }
            }
            ByteBuffer done = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            done.put(new byte[]{'D','O','N','E'}).putInt((int)(System.currentTimeMillis()/1000));
            writeChunked(stream, done.array(), 4000);
            SyncReader reader = new SyncReader(stream);
            String id = new String(reader.read(4), StandardCharsets.US_ASCII);
            int length = reader.readInt();
            if ("FAIL".equals(id)) throw new IOException("No se pudo copiar el APK: " + new String(reader.read(length), StandardCharsets.UTF_8));
            if (!"OKAY".equals(id)) throw new IOException("Respuesta inesperada al copiar NIIVO: " + id);
        } finally { try { stream.close(); } catch (Exception ignored) {} }
    }

    private static void writeChunked(AdbStream stream, byte[] bytes, int max) throws Exception {
        for (int offset=0; offset<bytes.length; offset+=max) {
            int size=Math.min(max, bytes.length-offset);
            byte[] part=new byte[size];
            System.arraycopy(bytes,offset,part,0,size);
            stream.write(part);
        }
    }

    private String runShell(AdbConnection conn, String command) throws Exception {
        AdbStream stream = conn.open("shell:" + command);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long until=System.currentTimeMillis()+180000;
        try {
            while (System.currentTimeMillis()<until) {
                try {
                    byte[] part=stream.read();
                    if (part != null) output.write(part);
                } catch (IOException closed) { break; }
                if (output.size()>128*1024) break;
            }
        } finally { try { stream.close(); } catch (Exception ignored) {} }
        return output.toString("UTF-8");
    }

    private static final class SyncReader {
        private final AdbStream stream;
        private byte[] pending = new byte[0];
        private int offset;
        SyncReader(AdbStream s) { stream=s; }
        byte[] read(int count) throws Exception {
            ByteArrayOutputStream out=new ByteArrayOutputStream(count);
            while(out.size()<count) {
                if(offset>=pending.length) { pending=stream.read(); offset=0; }
                int n=Math.min(count-out.size(),pending.length-offset);
                out.write(pending,offset,n); offset+=n;
            }
            return out.toByteArray();
        }
        int readInt() throws Exception { return ByteBuffer.wrap(read(4)).order(ByteOrder.LITTLE_ENDIAN).getInt(); }
    }

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) return "sin detalle devuelto por el televisor";
        return value.replace('\n',' ').replace('\r',' ').trim();
    }

    private static boolean isValidIp(String value) {
        if (value == null || !value.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) return false;
        try { for(String piece:value.split("\\.")) if(Integer.parseInt(piece)>255) return false; return true; }
        catch(Exception e){return false;}
    }

    @Override public void onBackPressed() {
        if (operationRunning) confirmCancelOperation(uninstallMode);
        else if (updateOperationRunning) confirmCancelUpdate();
        else if (updatesScreenVisible) showHome();
        else if (settingsDetailVisible) showSettings();
        else if (settingsScreenVisible) showHome();
        else if (installerScreenVisible) { uninstallMode = false; showHome(); }
        else super.onBackPressed();
    }
    private final class IndeterminateProgressView extends View {
        private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private ValueAnimator motion;
        private float phase;
        private boolean moving = true;

        IndeterminateProgressView(android.content.Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setMoving(boolean value) {
            moving = value;
            if (!moving && motion != null) {
                motion.cancel();
                motion = null;
            }
            if (moving) startMotion();
            invalidate();
        }
        private void startMotion() {
            if (!moving || motion != null && motion.isStarted()) return;
            motion = ValueAnimator.ofFloat(0f, 1f);
            motion.setDuration(1150);
            motion.setRepeatCount(ValueAnimator.INFINITE);
            motion.setInterpolator(new android.view.animation.LinearInterpolator());
            motion.addUpdateListener(animator -> {
                phase = (float) animator.getAnimatedValue();
                invalidate();
            });
            motion.start();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startMotion();
        }

        @Override protected void onDetachedFromWindow() {
            if (motion != null) motion.cancel();
            motion = null;
            super.onDetachedFromWindow();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth(), height = getHeight();
            if (width <= 0 || height <= 0) return;
            float segmentWidth = width * 0.32f;
            float left = -segmentWidth + phase * (width + segmentWidth);
            Path clip = new Path();
            clip.addRoundRect(0, 0, width, height, height / 2f, height / 2f, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            segmentPaint.setShader(new android.graphics.LinearGradient(left, 0, left + segmentWidth, 0,
                    new int[]{CYAN, BLUE, PURPLE}, null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(left, 0, left + segmentWidth, height, height / 2f, height / 2f, segmentPaint);
            segmentPaint.setShader(null);
            canvas.restore();
        }
    }
    private final class ActionIconView extends View {
        private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int kind;
        ActionIconView(android.content.Context context, int kind) {
            super(context);
            this.kind = kind;
            icon.setColor(WHITE);
            icon.setStyle(Paint.Style.STROKE);
            icon.setStrokeWidth(5.5f);
            icon.setStrokeCap(Paint.Cap.ROUND);
            icon.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth(), getHeight()) / 100f;
            canvas.save();
            canvas.translate((getWidth() - 100 * scale) / 2f, (getHeight() - 100 * scale) / 2f);
            canvas.scale(scale, scale);
            if (kind == 11) drawUsb(canvas);
            else if (kind == 12) drawClock(canvas);
            else if (kind == 13 || kind == 16) drawHelp(canvas);
            else if (kind == 14) drawMail(canvas);
            else if (kind == 15) drawExternal(canvas);
            else if (kind == 17) drawDocument(canvas);
            else if (kind == 18) drawShield(canvas);
            else if (kind == 19) drawPeople(canvas);
            else if (kind == 20) drawRefresh(canvas);
            else if (kind == 21) drawChat(canvas);
            else if (kind == 22) drawHeadset(canvas);
            else if (kind == 23) drawWifi(canvas);
            else if (kind == 24) drawCheckSquare(canvas);
            else if (kind == 25) drawLock(canvas);
            else if (kind == 9) drawDownload(canvas);
            else if (kind == 0) drawCube(canvas);
            else if (kind == 1) drawMenuRefresh(canvas);
            else if (kind == 2) drawTrash(canvas);
            else if (kind == 3) drawGear(canvas);
            else if (kind == 4) drawHome(canvas);
            else if (kind == 5) drawDatabase(canvas);
            else if (kind == 6) drawTag(canvas);
            else if (kind == 7) drawInfo(canvas);
            else if (kind == 8) drawCancel(canvas);
            else if (kind == 10) drawRocket(canvas);
            else drawShield(canvas);
            canvas.restore();
        }
        private void drawHome(Canvas canvas) {
            Path roof = new Path(); roof.moveTo(18, 48); roof.lineTo(50, 20); roof.lineTo(82, 48); canvas.drawPath(roof, icon);
            canvas.drawRoundRect(new android.graphics.RectF(27, 44, 73, 82), 4, 4, icon);
            canvas.drawRoundRect(new android.graphics.RectF(44, 61, 56, 82), 3, 3, icon);
        }
        private void drawDatabase(Canvas canvas) {
            canvas.drawOval(new android.graphics.RectF(24, 18, 76, 42), icon);
            canvas.drawLine(24, 30, 24, 70, icon); canvas.drawLine(76, 30, 76, 70, icon);
            canvas.drawArc(new android.graphics.RectF(24, 38, 76, 60), 0, 180, false, icon);
            canvas.drawArc(new android.graphics.RectF(24, 58, 76, 82), 0, 180, false, icon);
            canvas.drawArc(new android.graphics.RectF(24, 58, 76, 82), 180, 180, false, icon);
        }
        private void drawTag(Canvas canvas) {
            Path tag = new Path(); tag.moveTo(17, 49); tag.lineTo(47, 19); tag.lineTo(75, 19); tag.quadTo(82, 19, 82, 26);
            tag.lineTo(82, 54); tag.lineTo(51, 84); tag.close(); canvas.drawPath(tag, icon); canvas.drawCircle(64, 35, 4, icon);
        }
        private void drawInfo(Canvas canvas) {
            canvas.drawCircle(50, 50, 34, icon); canvas.drawCircle(50, 32, 2.5f, icon); canvas.drawLine(50, 44, 50, 69, icon);
        }
        private void drawShield(Canvas canvas) {
            Path shield = new Path(); shield.moveTo(50, 15); shield.lineTo(79, 26); shield.lineTo(77, 55);
            shield.quadTo(74, 73, 50, 86); shield.quadTo(26, 73, 23, 55); shield.lineTo(21, 26); shield.close(); canvas.drawPath(shield, icon);
            canvas.drawCircle(50, 43, 4, icon); canvas.drawRoundRect(new android.graphics.RectF(45, 51, 55, 65), 4, 4, icon);
        }
        private void drawDownload(Canvas canvas) {
            canvas.drawLine(50, 18, 50, 60, icon);
            Path arrow = new Path(); arrow.moveTo(32, 45); arrow.lineTo(50, 63); arrow.lineTo(68, 45); canvas.drawPath(arrow, icon);
            canvas.drawLine(22, 80, 78, 80, icon);
        }
        private void drawCancel(Canvas canvas) {
            canvas.drawLine(28, 28, 72, 72, icon);
            canvas.drawLine(72, 28, 28, 72, icon);
        }
        private void drawRocket(Canvas canvas) {
            Path body = new Path();
            body.moveTo(50, 16); body.cubicTo(68, 28, 72, 47, 65, 64);
            body.lineTo(50, 76); body.lineTo(35, 64); body.cubicTo(28, 47, 32, 28, 50, 16);
            canvas.drawPath(body, icon);
            canvas.drawCircle(50, 43, 7, icon);
            Path fins = new Path();
            fins.moveTo(35, 55); fins.lineTo(23, 62); fins.lineTo(28, 76); fins.lineTo(42, 69);
            fins.moveTo(65, 55); fins.lineTo(77, 62); fins.lineTo(72, 76); fins.lineTo(58, 69);
            canvas.drawPath(fins, icon);
            canvas.drawLine(45, 79, 50, 88, icon);
            canvas.drawLine(55, 79, 50, 88, icon);
        }
        private void drawUsb(Canvas canvas) {
            canvas.drawLine(50, 79, 50, 29, icon);
            canvas.drawLine(50, 52, 29, 35, icon);
            canvas.drawLine(50, 61, 72, 43, icon);
            canvas.drawLine(50, 69, 50, 84, icon);
            canvas.drawCircle(50, 85, 7, icon);
            canvas.drawRect(24, 26, 34, 37, icon);
            Path arrow = new Path(); arrow.moveTo(68, 37); arrow.lineTo(72, 25); arrow.lineTo(81, 33); arrow.close(); canvas.drawPath(arrow, icon);
        }
        private void drawClock(Canvas canvas) {
            canvas.drawCircle(50, 50, 33, icon);
            canvas.drawLine(50, 30, 50, 51, icon);
            canvas.drawLine(50, 51, 66, 60, icon);
        }
        private void drawHelp(Canvas canvas) {
            canvas.drawCircle(50, 50, 34, icon);
            Path question = new Path();
            question.moveTo(38, 39); question.cubicTo(39, 25, 62, 25, 63, 40);
            question.cubicTo(63, 49, 50, 49, 50, 60); canvas.drawPath(question, icon);
            canvas.drawCircle(50, 73, 2.5f, icon);
        }
        private void drawMail(Canvas canvas) {
            android.graphics.RectF box = new android.graphics.RectF(19, 27, 81, 74);
            canvas.drawRoundRect(box, 5, 5, icon);
            canvas.drawLine(21, 31, 50, 54, icon); canvas.drawLine(79, 31, 50, 54, icon);
        }
        private void drawExternal(Canvas canvas) {
            android.graphics.RectF box = new android.graphics.RectF(19, 39, 61, 81);
            canvas.drawRoundRect(box, 5, 5, icon);
            canvas.drawLine(44, 56, 80, 20, icon); canvas.drawLine(58, 20, 80, 20, icon); canvas.drawLine(80, 20, 80, 42, icon);
        }
        private void drawDocument(Canvas canvas) {
            Path sheet = new Path();
            sheet.moveTo(27, 17); sheet.lineTo(58, 17); sheet.lineTo(75, 34); sheet.lineTo(75, 83); sheet.lineTo(27, 83); sheet.close(); canvas.drawPath(sheet, icon);
            canvas.drawLine(58, 18, 58, 34, icon); canvas.drawLine(58, 34, 74, 34, icon);
            canvas.drawLine(37, 49, 64, 49, icon); canvas.drawLine(37, 61, 64, 61, icon); canvas.drawLine(37, 72, 58, 72, icon);
        }
        private void drawPeople(Canvas canvas) {
            canvas.drawCircle(39, 36, 12, icon); canvas.drawCircle(65, 40, 10, icon);
            canvas.drawArc(new android.graphics.RectF(17, 48, 61, 83), 180, 180, false, icon);
            canvas.drawArc(new android.graphics.RectF(48, 53, 83, 83), 180, 180, false, icon);
        }
        private void drawChat(Canvas canvas) {
            android.graphics.RectF bubble = new android.graphics.RectF(18, 20, 82, 69);
            canvas.drawRoundRect(bubble, 7, 7, icon);
            Path tail = new Path(); tail.moveTo(32, 69); tail.lineTo(26, 83); tail.lineTo(46, 69); canvas.drawPath(tail, icon);
            canvas.drawLine(31, 37, 69, 37, icon); canvas.drawLine(31, 49, 61, 49, icon);
        }
        private void drawCube(Canvas canvas) {
            Path p = new Path();
            p.moveTo(50, 18); p.lineTo(78, 33); p.lineTo(78, 67); p.lineTo(50, 82);
            p.lineTo(22, 67); p.lineTo(22, 33); p.close(); canvas.drawPath(p, icon);
            canvas.drawLine(22, 33, 50, 49, icon); canvas.drawLine(78, 33, 50, 49, icon);
            canvas.drawLine(50, 49, 50, 82, icon);
        }
        private void drawRefresh(Canvas canvas) {
            android.graphics.RectF loop = new android.graphics.RectF(20, 20, 80, 80);
            canvas.drawArc(loop, 205, 125, false, icon);
            canvas.drawArc(loop, 25, 125, false, icon);
            Path upperArrow = new Path();
            upperArrow.moveTo(79, 44); upperArrow.lineTo(77, 34); upperArrow.lineTo(68, 38);
            canvas.drawPath(upperArrow, icon);
            Path lowerArrow = new Path();
            lowerArrow.moveTo(21, 56); lowerArrow.lineTo(23, 66); lowerArrow.lineTo(32, 62);
            canvas.drawPath(lowerArrow, icon);
        }
        private void drawMenuRefresh(Canvas canvas) {
            android.graphics.RectF loop = new android.graphics.RectF(17, 17, 83, 83);
            canvas.drawArc(loop, 195, 135, false, icon);
            canvas.drawArc(loop, 15, 125, false, icon);

            Path upperArrow = new Path();
            upperArrow.moveTo(68, 31);
            upperArrow.lineTo(82, 40);
            upperArrow.lineTo(80, 24);
            canvas.drawPath(upperArrow, icon);

            Path lowerArrow = new Path();
            lowerArrow.moveTo(32, 65);
            lowerArrow.lineTo(18, 61);
            lowerArrow.lineTo(22, 77);
            canvas.drawPath(lowerArrow, icon);
        }
        private void drawTrash(Canvas canvas) {
            Path body = new Path(); body.moveTo(30, 32); body.lineTo(34, 78); body.quadTo(35, 82, 40, 82);
            body.lineTo(60, 82); body.quadTo(65, 82, 66, 78); body.lineTo(70, 32); canvas.drawPath(body, icon);
            canvas.drawLine(25, 27, 75, 27, icon); canvas.drawLine(40, 20, 60, 20, icon);
            canvas.drawLine(44, 39, 44, 70, icon); canvas.drawLine(56, 39, 56, 70, icon);
        }
        private void drawGear(Canvas canvas) {
            Path p = new Path();
            for (int i = 0; i < 32; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 16;
                float radius = (i % 4 == 0 || i % 4 == 1) ? 38 : 30;
                float x = 50 + (float) Math.cos(angle) * radius;
                float y = 50 + (float) Math.sin(angle) * radius;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.close(); canvas.drawPath(p, icon); canvas.drawCircle(50, 50, 12, icon);
        }
        private void drawHeadset(Canvas canvas) {
            canvas.drawArc(new android.graphics.RectF(22, 17, 78, 75), 185, 170, false, icon);
            canvas.drawRoundRect(new android.graphics.RectF(17, 47, 31, 72), 5, 5, icon);
            canvas.drawRoundRect(new android.graphics.RectF(69, 47, 83, 72), 5, 5, icon);
            canvas.drawLine(76, 72, 69, 78, icon);
            canvas.drawRoundRect(new android.graphics.RectF(58, 76, 70, 84), 4, 4, icon);
        }
        private void drawWifi(Canvas canvas) {
            Path outer = new Path();
            outer.moveTo(15, 39); outer.cubicTo(35, 18, 65, 18, 85, 39); canvas.drawPath(outer, icon);
            Path middle = new Path();
            middle.moveTo(27, 53); middle.cubicTo(41, 39, 59, 39, 73, 53); canvas.drawPath(middle, icon);
            Path inner = new Path();
            inner.moveTo(40, 66); inner.cubicTo(46, 60, 54, 60, 60, 66); canvas.drawPath(inner, icon);
            canvas.drawCircle(50, 79, 3.5f, icon);
        }
        private void drawCheckSquare(Canvas canvas) {
            canvas.drawRoundRect(new android.graphics.RectF(18, 18, 82, 82), 8, 8, icon);
            Path check = new Path();
            check.moveTo(31, 49); check.lineTo(45, 63); check.lineTo(70, 36); canvas.drawPath(check, icon);
        }
        private void drawLock(Canvas canvas) {
            canvas.drawRoundRect(new android.graphics.RectF(23, 43, 77, 82), 6, 6, icon);
            canvas.drawArc(new android.graphics.RectF(33, 17, 67, 57), 180, 180, false, icon);
            canvas.drawLine(33, 37, 33, 45, icon);
            canvas.drawLine(67, 37, 67, 45, icon);
            canvas.drawCircle(50, 58, 4, icon);
            canvas.drawLine(50, 62, 50, 71, icon);
        }
    }
    private final class NivoMarkView extends android.widget.ImageView {
        NivoMarkView(android.content.Context context) {
            super(context);
            setImageResource(R.drawable.nivo_update_mark);
            setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            setContentDescription("NIIVO");
        }
    }
    private final class UpdateRingView extends View {
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float rotation;
        private ValueAnimator animator;
        UpdateRingView(android.content.Context context) {
            super(context);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth(), getHeight());
            float inset = dp(5);
            android.graphics.RectF oval = new android.graphics.RectF((getWidth()-side)/2f+inset, (getHeight()-side)/2f+inset,
                    (getWidth()+side)/2f-inset, (getHeight()+side)/2f-inset);
            ring.setStrokeWidth(dp(4));
            ring.setShader(new SweepGradient(getWidth()/2f, getHeight()/2f, new int[]{0x00335CFF, CYAN, BLUE, PURPLE, 0x00335CFF}, null));
            canvas.save();
            canvas.rotate(rotation, getWidth()/2f, getHeight()/2f);
            canvas.drawArc(oval, 0f, 300f, false, ring);
            canvas.restore();
            ring.setShader(null);
        }
        private void start() {
            if (animator != null || !isAttachedToWindow()) return;
            animator = ValueAnimator.ofFloat(0f, 360f);
            animator.setDuration(1250);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(a -> { rotation = (float)a.getAnimatedValue(); invalidate(); });
            animator.start();
        }
        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); start(); }
        @Override protected void onDetachedFromWindow() { if (animator != null) { animator.cancel(); animator = null; } super.onDetachedFromWindow(); }
    }
    private final class SuccessRingView extends View {
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        SuccessRingView(android.content.Context context) {
            super(context); ring.setStyle(Paint.Style.STROKE); ring.setStrokeCap(Paint.Cap.ROUND);
            tick.setColor(WHITE); tick.setStyle(Paint.Style.STROKE); tick.setStrokeCap(Paint.Cap.ROUND); tick.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.42f;
            ring.setStrokeWidth(Math.max(dp(5), radius * 0.08f)); ring.setShader(new SweepGradient(cx, cy, new int[]{CYAN, BLUE, PURPLE, CYAN}, null));
            canvas.drawCircle(cx, cy, radius, ring); ring.setShader(null);
            tick.setStrokeWidth(Math.max(dp(5), radius * 0.09f)); Path path = new Path();
            path.moveTo(cx - radius * 0.30f, cy + radius * 0.02f); path.lineTo(cx - radius * 0.05f, cy + radius * 0.27f);
            path.lineTo(cx + radius * 0.36f, cy - radius * 0.22f); canvas.drawPath(path, tick);
        }
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view=new TextView(this);
        view.setText(value); view.setTextSize(sp); view.setTextColor(color);
        if(bold) view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }

    private Button button(String label, boolean primary) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(WHITE);
        b.setBackground(primary?gradientButton(dp(16)):glassPanel(dp(14)));
        b.setEnabled(true);
        b.setAlpha(1f);
        b.setOnTouchListener((v,e)->{ if(!b.isEnabled())return false; if(e.getAction()==android.view.MotionEvent.ACTION_DOWN)b.setAlpha(0.78f); if(e.getAction()==android.view.MotionEvent.ACTION_UP||e.getAction()==android.view.MotionEvent.ACTION_CANCEL)b.setAlpha(1f); return false;});
        return b;
    }

    private GradientDrawable round(int color,int radius,int stroke,int strokeWidth) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(strokeWidth,stroke); return d;
    }
    private Drawable screenGradient() {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                Rect bounds = getBounds();
                float w = bounds.width(), h = bounds.height();
                if (w <= 0 || h <= 0) return;
                canvas.save(); canvas.translate(bounds.left, bounds.top);
                paint.setShader(null); paint.setColor(0xFF030711); canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.08f, h * 0.27f, Math.max(w, h) * 0.58f,
                        new int[]{0xB51A59B8, 0x50123A82, 0x00123A82}, new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.94f, h * 0.88f, Math.max(w, h) * 0.68f,
                        new int[]{0x842B0BA1, 0x3A1D0D69, 0x001D0D69}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint); paint.setShader(null); canvas.restore();
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter colorFilter) {}
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }
    private Drawable menuBackground() {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                Rect bounds = getBounds();
                float w = bounds.width(), h = bounds.height();
                if (w <= 0 || h <= 0) return;
                canvas.save();
                canvas.translate(bounds.left, bounds.top);
                Path clip = new Path();
                clip.addRoundRect(new android.graphics.RectF(0, 0, w, h), dp(17), dp(17), Path.Direction.CW);
                canvas.clipPath(clip);
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(null);
                paint.setColor(0xFF020812);
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.03f, h * 0.12f, Math.max(w, h) * 0.58f,
                        new int[]{0x35113A70, 0x1A102B5B, 0x00102B5B},
                        new float[]{0f, 0.56f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 1.02f, h * 0.98f, Math.max(w, h) * 0.62f,
                        new int[]{0x7A3B087C, 0x301E105C, 0x001E105C},
                        new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setShader(new android.graphics.LinearGradient(0, 0, w, h,
                        new int[]{0x995179BE, 0x775D49C8, 0x994821A5}, null, Shader.TileMode.CLAMP));
                android.graphics.RectF frame = new android.graphics.RectF(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f));
                canvas.drawRoundRect(frame, dp(17), dp(17), paint);
                paint.setShader(null);
                paint.setStyle(Paint.Style.FILL);
                canvas.restore();
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter colorFilter) {}
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }
    private Drawable homeGradient() {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                Rect bounds = getBounds();
                float w = bounds.width(), h = bounds.height();
                if (w <= 0 || h <= 0) return;
                canvas.save();
                canvas.translate(bounds.left, bounds.top);
                paint.setShader(null);
                paint.setColor(0xFF020711);
                canvas.drawRect(0, 0, w, h, paint);

                paint.setShader(new RadialGradient(w * 0.50f, w * 0.02f, w * 0.68f,
                        new int[]{0xA51C4F9A, 0x4A10336E, 0x0010336E},
                        new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(new RadialGradient(w * 0.94f, w * 1.62f, w * 0.94f,
                        new int[]{0x9C4B08B7, 0x4A291064, 0x00291064},
                        new float[]{0f, 0.50f, 1f}, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, paint);

                Path topArc = new Path();
                topArc.moveTo(w * 0.44f, -w * 0.035f);
                topArc.cubicTo(w * 0.59f, w * 0.09f, w * 0.82f, w * 0.27f, w * 1.02f, w * 0.42f);
                Path bottomArc = new Path();
                bottomArc.moveTo(-w * 0.04f, w * 1.44f);
                bottomArc.cubicTo(w * 0.28f, w * 1.63f, w * 0.65f, w * 1.76f, w * 1.04f, w * 1.72f);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(2.2f));
                paint.setShader(new android.graphics.LinearGradient(0, 0, w, w * 0.42f,
                        new int[]{0xFF0879FF, 0xFF174DFF, 0xFF9A16FF}, null, Shader.TileMode.CLAMP));
                canvas.drawPath(topArc, paint);
                paint.setStrokeWidth(dp(1.5f));
                paint.setShader(new android.graphics.LinearGradient(0, w * 1.44f, w, w * 1.72f,
                        new int[]{0xFF087BFF, 0xFF244CFF, 0xFF9A16FF}, null, Shader.TileMode.CLAMP));
                canvas.drawPath(bottomArc, paint);
                paint.setShader(null);
                paint.setStyle(Paint.Style.FILL);
                canvas.restore();
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter colorFilter) {}
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }
    private Drawable successOutline(int radius) {
        return new Drawable() {
            private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                android.graphics.Rect bounds = getBounds();
                android.graphics.RectF rect = new android.graphics.RectF(bounds.left, bounds.top, bounds.right, bounds.bottom);
                fill.setStyle(Paint.Style.FILL);
                fill.setColor(0xB8081020);
                canvas.drawRoundRect(rect, radius, radius, fill);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(dp(1));
                stroke.setShader(new android.graphics.LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        new int[]{CYAN, BLUE, PURPLE}, null, Shader.TileMode.CLAMP));
                float half = dp(1) / 2f;
                rect.inset(half, half);
                canvas.drawRoundRect(rect, radius, radius, stroke);
                stroke.setShader(null);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter colorFilter) {}
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }
    private Drawable installerCardPanel(int radius) {
        return new Drawable() {
            private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                android.graphics.Rect bounds = getBounds();
                android.graphics.RectF rect = new android.graphics.RectF(bounds.left, bounds.top, bounds.right, bounds.bottom);
                fill.setStyle(Paint.Style.FILL);
                fill.setShader(new android.graphics.LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        new int[]{0xE81C5790, 0xE80B1930, 0xE62C0B58}, null, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, radius, radius, fill);
                fill.setShader(null);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(dp(1.8f));
                stroke.setShader(new android.graphics.LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                        new int[]{0xCC22B9FF, 0xA83362FF, 0xCCB52AFF}, null, Shader.TileMode.CLAMP));
                rect.inset(dp(0.75f), dp(0.75f));
                canvas.drawRoundRect(rect, radius, radius, stroke);
                stroke.setShader(null);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter colorFilter) {}
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }    private GradientDrawable glassPanel(int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xE808111F, 0xE80C1224, 0xE91A1233});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), 0x454A6081);
        return d;
    }
    private GradientDrawable gradientButton(int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{CYAN, BLUE, PURPLE});
        d.setCornerRadius(radius);
        d.setStroke(dp(1), 0x6664D9FF);
        return d;
    }    private int dp(float value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
    @Override protected void onDestroy() { super.onDestroy(); /* executor is intentionally process-scoped for pending ADB I/O */ }
}
