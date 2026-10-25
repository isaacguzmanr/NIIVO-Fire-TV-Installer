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
    private boolean updatesScreenVisible;
    private volatile boolean updateOperationRunning;
    private volatile long lastUpdateProgressUi;
    private UpdateManager.Release pendingUpdate;
    private String updateHost = "";
    private String installedTvVersion = "";
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
        root.setPadding(dp(34), dp(28), dp(34), dp(28));
        root.setBackground(screenGradient());
        View logo = new NivoMarkView(this);
root.addView(logo, new LinearLayout.LayoutParams(dp(126), dp(126)));
        ValueAnimator logoPulse = ValueAnimator.ofFloat(0.96f, 1.035f);
        logoPulse.setDuration(760);
        logoPulse.setRepeatMode(ValueAnimator.REVERSE);
        logoPulse.setRepeatCount(ValueAnimator.INFINITE);
        logoPulse.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            logo.setScaleX(scale);
            logo.setScaleY(scale);
        });
        logoPulse.start();
        TextView name = text("NIIVO", 28, WHITE, true);
        name.setLetterSpacing(0.12f);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-1, -2);
        nameLp.topMargin = dp(17);
        root.addView(name, nameLp);
        TextView tag = text("T V   S I M P L I F I E D", 11, MUTED, false);
        tag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(-1, -2);
        tagLp.topMargin = dp(8);
        root.addView(tag, tagLp);
        android.widget.FrameLayout track = new android.widget.FrameLayout(this);
        track.setBackground(round(0x553B4B79, dp(4), 0x003B4B79, 0));
        android.view.View fill = new android.view.View(this);
        fill.setBackground(gradientButton(dp(4)));
        android.widget.FrameLayout.LayoutParams fillLp = new android.widget.FrameLayout.LayoutParams(0, -1);
        track.addView(fill, fillLp);
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(dp(190), dp(5));
        trackLp.topMargin = dp(68);
        root.addView(track, trackLp);
        TextView loading = text("Iniciando…", 13, MUTED, false);
        loading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams loadingLp = new LinearLayout.LayoutParams(-1, -2);
        loadingLp.topMargin = dp(12);
        root.addView(loading, loadingLp);
        setContentView(root);
        track.post(() -> {
            int fullWidth = track.getWidth();
            if (fullWidth <= 0 || isFinishing()) return;
            ValueAnimator progress = ValueAnimator.ofInt(0, fullWidth);
            progress.setDuration(1750);
            progress.setInterpolator(new android.view.animation.DecelerateInterpolator(1.15f));
            progress.addUpdateListener(animation -> {
                android.widget.FrameLayout.LayoutParams params =
                        (android.widget.FrameLayout.LayoutParams) fill.getLayoutParams();
                params.width = (int) animation.getAnimatedValue();
                fill.setLayoutParams(params);
            });
            progress.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    logoPulse.cancel();
                    showHome();
                }
            });
            progress.start();
        });
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
        LinearLayout page = newPage();
        page.setPadding(dp(16), dp(25), dp(16), dp(24));
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setPadding(dp(8), 0, dp(8), 0);
        View mark = new NivoMarkView(this);
        brand.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(38)));
        View spacer = new View(this);
        brand.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        TextView more = text("⋮", 24, WHITE, false);
        more.setGravity(Gravity.CENTER);
        more.setContentDescription("Más opciones");
        brand.addView(more, new LinearLayout.LayoutParams(dp(32), dp(38)));
        more.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, more);
            popup.getMenu().add("Configuración").setOnMenuItemClickListener(item -> { showSettings(); return true; });
            popup.getMenu().add("Acerca de NIIVO").setOnMenuItemClickListener(item -> {
                Toast.makeText(this, "NIIVO Installer 1.5.0-beta.1", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });
        page.addView(brand);

        TextView title = text("NIIVO", 31, WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        title.setTextSize(31);
        title.setPadding(dp(8), 0, 0, 0);
        titleLp.topMargin = dp(34);
        page.addView(title, titleLp);
        TextView intro = text("Tu Fire TV, a tu manera.", 16, MUTED, false);
        intro.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        intro.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(7);
        introLp.bottomMargin = dp(27);
        page.addView(intro, introLp);
        page.addView(actionCard(0, "Instalar", getEmbeddedLauncherVersion(), true,
                v -> { uninstallMode = false; buildInstallerPage(); }));
        page.addView(actionCard(1, "Actualizar", "Verificar nueva versión", false,
                v -> showUpdates()));
        page.addView(actionCard(2, "Desinstalar", "Remover NIIVO", false,
                v -> { uninstallMode = true; buildInstallerPage(); }));
        page.addView(actionCard(3, "Configuración", "Ajustes y preferencias", false,
                v -> showSettings()));
    }

    private View actionCard(int icon, String title, String subtitle, boolean featured,
                            View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(9), dp(13), dp(9));
        row.setBackground(featured ? installerCardPanel(dp(19)) : glassPanel(dp(19)));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(92));
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        ActionIconView iconView = new ActionIconView(this, icon);
        iconView.setBackground(round(0x4421385D, dp(30), 0x333D7EBE, dp(1)));
        row.addView(iconView, new LinearLayout.LayoutParams(dp(60), dp(60)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        labelsLp.leftMargin = dp(12);
        row.addView(labels, labelsLp);
        labels.addView(text(title, 16, WHITE, true));
        TextView sub = text(subtitle, 13, MUTED, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(3);
        labels.addView(sub, subLp);

        TextView arrow = text("›", 27, MUTED, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(24), -1));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(click);
        return row;
    }
    private LinearLayout updatesPage(String subtitle) {
        LinearLayout page = newPage();
        page.setPadding(dp(18), dp(10), dp(18), dp(24));
        TextView back = text("‹", 32, WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Regresar");

        page.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text("Actualizaciones", 30, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(12);
        page.addView(title, titleLp);
        TextView intro = text(subtitle, 16, MUTED, false);
        intro.setLineSpacing(dp(3), 1f);
        intro.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(7);
        introLp.bottomMargin = dp(22);
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
        page.addView(settingsCard(1, "Versión instalada", "Conecta un Fire TV para comprobar", true, v -> showUpdateHostDialog()));
        TextView note = text("La versión se consulta directamente en el Fire TV para mostrar el estado real.", 14, MUTED, false);
        note.setPadding(dp(8), dp(14), dp(8), dp(10));
        page.addView(note);
        Button connect = button("Conectar Fire TV", true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
        lp.topMargin = dp(12);
        page.addView(connect, lp);
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
        page.addView(settingsCard(1, "Versión instalada", "Consultando Fire TV…", true, v -> {}));
        android.widget.ProgressBar spinner = new android.widget.ProgressBar(this);
        spinner.setIndeterminate(true);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        spinLp.gravity = Gravity.CENTER_HORIZONTAL;
        spinLp.topMargin = dp(28);
        page.addView(spinner, spinLp);
        TextView message = text("Buscando actualizaciones…", 16, MUTED, false);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(-1, -2);
        msgLp.topMargin = dp(14);
        page.addView(message, msgLp);
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
                UpdateManager.ReleaseCheck check = UpdateManager.checkLatest();
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
        page.addView(settingsCard(1, "Versión instalada", installedTvVersion, true, v -> {}), updateCardLayout(dp(92), 10));
        TextView current = text("✓  NIIVO está actualizado", 19, 0xFF2BE7BB, true);
        current.setPadding(dp(10), dp(14), dp(8), 0);
        page.addView(current);
        TextView detail = text("Ya tienes la última versión disponible (" + latest + ").", 15, MUTED, false);
        detail.setPadding(dp(10), dp(8), dp(8), dp(20));
        page.addView(detail);
        Button retry = button("Buscar nuevamente", true);
        page.addView(retry, updateCardLayout(dp(58), 0));
        retry.setOnClickListener(v -> checkUpdates(host));
        retry.requestFocus();
    }

    private void showUpdateAvailable(String host, String installed, UpdateManager.Release release) {
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(settingsCard(1, "Versión instalada", installed, true, v -> {}), updateCardLayout(dp(92), 10));
        page.addView(settingsCard(9, "Nueva versión disponible", "Hay una nueva versión de NIIVO: " + release.version, false, v -> {}), updateCardLayout(dp(92), 12));
        TextView versions = text("Instalada  " + installed + "                      Nueva  " + release.version, 14, MUTED, false);
        versions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(-1, -2);
        vlp.topMargin = dp(4); vlp.bottomMargin = dp(14); page.addView(versions, vlp);
        Button update = button("Actualizar ahora", true);
        page.addView(update, updateCardLayout(dp(60), 0));
        update.setOnClickListener(v -> startRemoteUpdate(host, release));
        update.requestFocus();
    }

    private void showUpdateUnavailable(String host, String installed, String latest) {
        LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
        page.addView(settingsCard(1, "Versión instalada", installed, true, v -> {}), updateCardLayout(dp(92), 12));
        page.addView(settingsCard(9, "Actualización no disponible", "La versión " + latest + " está publicada, pero todavía no hay un APK instalador válido.", false, v -> {}), updateCardLayout(dp(104), 16));
        Button retry = button("Buscar nuevamente", true);
        page.addView(retry, updateCardLayout(dp(58), 0));
        retry.setOnClickListener(v -> checkUpdates(host)); retry.requestFocus();
    }

    private void showUpdateError(String title, String detail, String host, boolean fireTvConnectionProblem) {
        runOnUiThread(() -> {
            if (!updatesScreenVisible) return;
            LinearLayout page = updatesPage("Comprueba si tienes la última versión de NIIVO.");
            page.addView(settingsCard(7, title, detail, false, v -> {}), updateCardLayout(dp(112), 18));
            if (fireTvConnectionProblem) {
                TextView help = text("Comprueba que Fire TV y el teléfono estén en la misma Wi‑Fi, activa Depuración ADB en la TV y acepta el aviso de autorización.", 14, MUTED, false);
                help.setPadding(dp(10), 0, dp(8), dp(14));
                page.addView(help);
            }
            Button retry = button("Reintentar", true);
            page.addView(retry, updateCardLayout(dp(58), fireTvConnectionProblem ? 8 : 0));
            retry.setOnClickListener(v -> checkUpdates(host));
            if (fireTvConnectionProblem) {
                Button reconnect = button("Conectar o cambiar IP", false);
                page.addView(reconnect, updateCardLayout(dp(58), 0));
                reconnect.setOnClickListener(v -> showUpdateHostDialog());
                reconnect.requestFocus();
            } else {
                retry.requestFocus();
            }
        });
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
        page.addView(settingsCard(9, "Actualizando NIIVO", "Descargando nueva versión " + release.version + "…", true, v -> {}), updateCardLayout(dp(94), 18));
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
            page.addView(settingsCard(9, "Instalando NIIVO", "Fire TV está aplicando la actualización…", true, v -> {}), updateCardLayout(dp(94), 16));
            android.widget.ProgressBar spinner = new android.widget.ProgressBar(this); spinner.setIndeterminate(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40)); lp.gravity = Gravity.CENTER_HORIZONTAL; lp.topMargin = dp(30); page.addView(spinner, lp);
        });
    }

    private void showUpdateDone(String version) {
        if (!updatesScreenVisible) return;
        installedTvVersion = version;
        LinearLayout page = updatesPage("NIIVO se actualizó correctamente en el Fire TV.");
        page.addView(settingsCard(1, "NIIVO está actualizado", "Versión " + version, true, v -> {}), updateCardLayout(dp(94), 20));
        Button done = button("Listo", true); page.addView(done, updateCardLayout(dp(58), 0));
        done.setOnClickListener(v -> showHome()); done.requestFocus();
    }
    private void showSettings() {
        updatesScreenVisible = false;
        settingsScreenVisible = true;
        settingsDetailVisible = false;
        keepDataOnUninstall = getPreferences(MODE_PRIVATE).getBoolean("keep_data", true);
        LinearLayout page = newPage();
        page.setPadding(dp(16), dp(8), dp(16), dp(24));

        TextView back = text("‹", 32, WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Regresar");
        back.setOnClickListener(v -> showHome());
        page.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text("Configuración", 31, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(9);
        page.addView(title, titleLp);

        TextView subtitle = text("Ajustes y preferencias de NIIVO.", 16, MUTED, false);
        subtitle.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(-1, -2);
        subtitleLp.topMargin = dp(5);
        subtitleLp.bottomMargin = dp(20);
        page.addView(subtitle, subtitleLp);

        LinearLayout launcherCard = settingsCard(4, "Launcher predeterminado",
                "Comprobando Fire TV…", true, v -> onLauncherCardSelected(launcherCardHolder[0], launcherStatusHolder[0]));
        launcherCardHolder[0] = launcherCard;
        TextView launcherStatus = (TextView) ((LinearLayout) launcherCard.getChildAt(1)).getChildAt(1);
        launcherStatusHolder[0] = launcherStatus;
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, dp(80));
        cardLp.bottomMargin = dp(8);
        page.addView(launcherCard, cardLp);

        LinearLayout keepCard = settingsCard(5, "Mantener datos",
                "Conservar configuración y preferencias", false,
                v -> showKeepDataSettings());
        cardLp = new LinearLayout.LayoutParams(-1, dp(80));
        cardLp.bottomMargin = dp(8);
        page.addView(keepCard, cardLp);

        LinearLayout versionCard = settingsCard(6, "Versión",
                getEmbeddedLauncherVersion(), false, v -> showVersionSettings());
        cardLp = new LinearLayout.LayoutParams(-1, dp(80));
        cardLp.bottomMargin = dp(8);
        page.addView(versionCard, cardLp);

        LinearLayout aboutCard = settingsCard(7, "Acerca de",
                "Información sobre NIIVO", false, v -> showAboutSettings());
        cardLp = new LinearLayout.LayoutParams(-1, dp(80));
        cardLp.bottomMargin = dp(8);
        page.addView(aboutCard, cardLp);

        LinearLayout privacyCard = settingsCard(8, "Privacidad",
                "Privacidad y permisos", false, v -> showPrivacySettings());
        page.addView(privacyCard, new LinearLayout.LayoutParams(-1, dp(80)));

        String host = getPreferences(MODE_PRIVATE).getString("last_fire_tv_host", "");
        if (host.isEmpty()) {
            launcherStatus.setText("Conecta un Fire TV para comprobar");
            launcherCard.setTag(Boolean.FALSE);
        } else {
            refreshLauncherStatus(host, launcherCard, launcherStatus);
        }
        launcherCard.requestFocus();
    }

    private final LinearLayout[] launcherCardHolder = new LinearLayout[1];
    private final TextView[] launcherStatusHolder = new TextView[1];

    private LinearLayout settingsCard(int icon, String title, String subtitle, boolean highlighted,
                                      View.OnClickListener click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(11), dp(8), dp(11), dp(8));
        row.setBackground(highlighted ? installerCardPanel(dp(19)) : glassPanel(dp(19)));

        ActionIconView iconView = new ActionIconView(this, icon);
        iconView.setBackground(round(0x4421385D, dp(30), 0x333D7EBE, dp(1)));
        row.addView(iconView, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        labelsLp.leftMargin = dp(12);
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

    private void addSettingsBack(LinearLayout page) {
        TextView back = text("‹", 32, WHITE, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Regresar");
        back.setOnClickListener(v -> showSettings());
        page.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
    }

    private void showVersionSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsBack(page);
        TextView title = text("Versión", 28, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(18);
        page.addView(title, titleLp);
        TextView version = text(getEmbeddedLauncherVersion(), 17, MUTED, false);
        version.setPadding(dp(8), dp(20), 0, 0);
        page.addView(version);
    }

    private void showAboutSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsBack(page);
        TextView title = text("Acerca de NIIVO", 28, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(18);
        page.addView(title, titleLp);
        TextView name = text("NIIVO", 18, WHITE, true);
        name.setPadding(dp(8), dp(24), 0, 0);
        page.addView(name);
        TextView version = text(getEmbeddedLauncherVersion(), 15, MUTED, false);
        version.setPadding(dp(8), dp(8), 0, 0);
        page.addView(version);
        TextView about = text("Launcher para Amazon Fire TV. NIIVO se instala y actualiza desde tu teléfono mediante una conexión ADB local.", 15, MUTED, false);
        about.setLineSpacing(dp(3), 1f);
        about.setPadding(dp(8), dp(18), 0, 0);
        page.addView(about);
    }

    private void showPrivacySettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsBack(page);
        TextView title = text("Privacidad y permisos", 27, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(18);
        page.addView(title, titleLp);
        TextView details = text("NIIVO utiliza la red local para encontrar el Fire TV y conectarse mediante ADB cuando instalas, actualizas o abres el launcher. La clave ADB se guarda en el almacenamiento privado de esta app. " +
                "Permisos usados: acceso a Internet y estado de Wi‑Fi/red.", 15, MUTED, false);
        details.setLineSpacing(dp(4), 1f);
        details.setPadding(dp(8), dp(20), 0, 0);
        page.addView(details);
    }

    private void showKeepDataSettings() {
        settingsDetailVisible = true;
        LinearLayout page = newPage();
        addSettingsBack(page);
        TextView title = text("Mantener datos", 28, WHITE, true);
        title.setPadding(dp(8), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(18);
        page.addView(title, titleLp);
        TextView explanation = text("Conserva configuración y preferencias al desinstalar NIIVO. Las actualizaciones con ADB mantienen los datos de la aplicación.", 15, MUTED, false);
        explanation.setLineSpacing(dp(4), 1f);
        explanation.setPadding(dp(8), dp(12), 0, dp(16));
        page.addView(explanation);

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
    private void buildInstallerPage() {
        updatesScreenVisible = false;
        settingsScreenVisible = false;
        settingsDetailVisible = false;
        LinearLayout page = newPage();
        addBack(page, "NIIVO");
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        View mark = new NivoMarkView(this);
        brand.addView(mark, new LinearLayout.LayoutParams(dp(37), dp(37)));
        TextView wordmark = text("NIIVO", 15, WHITE, true);
        wordmark.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams wordmarkLp = new LinearLayout.LayoutParams(-2, -2);
        wordmarkLp.leftMargin = dp(9);
        brand.addView(wordmark, wordmarkLp);
        page.addView(brand);
        TextView title = text(uninstallMode ? "Desinstalar NIIVO" : "Launcher listo\npara instalar", 27, WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(23);
        page.addView(title, titleLp);
        TextView intro = text(uninstallMode ? "Conecta con tu Fire TV para quitar NIIVO." : "Un Android TV más limpio y rápido.", 15, MUTED, false);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(7);
        introLp.bottomMargin = dp(16);
        page.addView(intro, introLp);
        LinearLayout help = new LinearLayout(this);
        help.setOrientation(LinearLayout.VERTICAL);
        help.setPadding(dp(16), dp(14), dp(16), dp(14));
        help.setBackground(glassPanel(dp(17)));
        help.addView(text("ANTES DE EMPEZAR", 11, CYAN, true));
        TextView steps = text("1. Activa Depuración ADB en Fire TV.\n2. Conecta teléfono y TV a la misma Wi‑Fi.\n3. Acepta el aviso de depuración en la TV.", 13, WHITE, false);
        steps.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams stepsLp = new LinearLayout.LayoutParams(-1, -2);
        stepsLp.topMargin = dp(7);
        help.addView(steps, stepsLp);
        page.addView(help);
        searchButton = button("Buscar Fire TV en mi Wi‑Fi", true);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(52));
        searchLp.topMargin = dp(15);
        page.addView(searchButton, searchLp);
        searchButton.setOnClickListener(v -> scanNetwork());
        devices = text("Todavía no se ha buscado ningún dispositivo.", 13, MUTED, false);
        devices.setPadding(dp(3), dp(9), dp(3), dp(3));
        page.addView(devices);
        TextView manual = text("También puedes escribir la IP de Fire TV > Mi Fire TV > Acerca de > Red.", 12, MUTED, false);
        LinearLayout.LayoutParams manualLp = new LinearLayout.LayoutParams(-1, -2);
        manualLp.topMargin = dp(8);
        page.addView(manual, manualLp);
        ipInput = new EditText(this);
        ipInput.setSingleLine(true);
        ipInput.setHint("IP del Fire TV, por ejemplo 192.168.1.7");
        ipInput.setHintTextColor(Color.rgb(139, 155, 175));
        ipInput.setTextColor(WHITE);
        ipInput.setTextSize(14);
        ipInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        ipInput.setPadding(dp(14), 0, dp(14), 0);
        ipInput.setBackground(glassPanel(dp(15)));
        LinearLayout.LayoutParams ipLp = new LinearLayout.LayoutParams(-1, dp(50));
        ipLp.topMargin = dp(8);
        page.addView(ipInput, ipLp);
        installButton = button(uninstallMode ? "Desinstalar NIIVO" : "Instalar NIIVO", true);
        installButton.setEnabled(false);
        installButton.setAlpha(0.5f);
        LinearLayout.LayoutParams installLp = new LinearLayout.LayoutParams(-1, dp(54));
        installLp.topMargin = dp(13);
        page.addView(installButton, installLp);
        ipInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                installButton.setEnabled(searchButton.isEnabled() && isValidIp(s.toString().trim()));
                installButton.setAlpha(installButton.isEnabled() ? 1f : 0.5f);
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        installButton.setOnClickListener(v -> installSelected());
        status = text("El APK de NIIVO está incluido; no tienes que descargarlo aparte.", 12, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(4), dp(11), dp(4), 0);
        page.addView(status);
    }

    private void showProgressScreen(boolean removing) {
        LinearLayout page = newPage();
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(22), 0, dp(22), 0);

        android.widget.FrameLayout topBar = new android.widget.FrameLayout(this);
        TextView back = text("‹", 32, WHITE, false);
        back.setGravity(Gravity.CENTER);
        android.widget.FrameLayout.LayoutParams backLp = new android.widget.FrameLayout.LayoutParams(dp(48), dp(48), Gravity.START | Gravity.CENTER_VERTICAL);
        topBar.addView(back, backLp);
        back.setContentDescription("Cancelar operación");
        back.setOnClickListener(v -> confirmCancelOperation(removing));
        page.addView(topBar, new LinearLayout.LayoutParams(-1, dp(48)));

        View topSpace = new View(this);
        page.addView(topSpace, new LinearLayout.LayoutParams(-1, 0, 0.95f));

        View logo = new NivoMarkView(this);
        page.addView(logo, new LinearLayout.LayoutParams(dp(88), dp(88)));
        TextView title = text(removing ? "Desinstalando NIIVO" : "Instalando NIIVO", 23, WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(19);
        page.addView(title, titleLp);

        status = text("Esto puede tomar unos segundos.", 14, MUTED, false);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.topMargin = dp(8);
        page.addView(status, statusLp);

        progressTrack = new android.widget.FrameLayout(this);
        progressTrack.setBackground(round(0x55435783, dp(5), 0x00435783, 0));
        indeterminateProgress = new IndeterminateProgressView(this);
        progressTrack.addView(indeterminateProgress, new android.widget.FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(-1, dp(6));
        trackLp.leftMargin = dp(3);
        trackLp.rightMargin = dp(3);
        trackLp.topMargin = dp(46);
        page.addView(progressTrack, trackLp);

        View bottomSpace = new View(this);
        page.addView(bottomSpace, new LinearLayout.LayoutParams(-1, 0, 1.1f));
    }

    private void setProgressActivity(boolean moving) {
        runOnUiThread(() -> {
            if (indeterminateProgress != null) indeterminateProgress.setMoving(moving);
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
        status.setText("Buscando dispositivos ADB en esta red…");
        devices.setText("");
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

                String deviceInfo = runShell(active, "getprop ro.product.manufacturer; getprop ro.product.model");
                String deviceLower = deviceInfo.toLowerCase(java.util.Locale.ROOT);
                if (!deviceLower.contains("amazon") && !deviceLower.contains("aft")) {
                    throw new IOException("El dispositivo " + host + " no se identificó como Fire TV. No instalé nada. " + clean(deviceInfo));
                }
                if (uninstallMode) {

                    String command = keepDataOnUninstall ? "pm uninstall -k " : "pm uninstall ";
                    String result = runShell(active, command + FIRE_PACKAGE);
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    if (!result.contains("Success")) throw new IOException("No se pudo desinstalar NIIVO. " + clean(result));
                } else {

                    pushApk(active);
                    if (cancelRequested) throw new IOException("Operación cancelada.");

                    String install = runShell(active, "pm install -r -g /data/local/tmp/nivo.apk");
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                    if (!install.contains("Success")) throw new IOException("La TV no aceptó la instalación. " + clean(install));

                    enableLauncher(active);
                    if (cancelRequested) throw new IOException("Operación cancelada.");
                }
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
        File apk = new File(getCacheDir(), "nivo.apk");
        try (InputStream in = getAssets().open("nivo.apk"); FileOutputStream out = new FileOutputStream(apk)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        pushApk(conn, apk, "/data/local/tmp/nivo.apk");
    }

    private void pushApk(AdbConnection conn, File apk, String remotePath) throws Exception {
        AdbStream stream = conn.open("sync:");
        try {
            byte[] spec = (remotePath + ",420").getBytes(StandardCharsets.UTF_8);
            ByteBuffer send = ByteBuffer.allocate(8 + spec.length).order(ByteOrder.LITTLE_ENDIAN);
            send.put(new byte[]{'S','E','N','D'}).putInt(spec.length).put(spec);
            writeChunked(stream, send.array(), 4000);
            try (InputStream in = new java.io.FileInputStream(apk)) {
                byte[] fileChunk = new byte[3900];
                int n;
                while ((n = in.read(fileChunk)) != -1) {
                    ByteBuffer packet = ByteBuffer.allocate(8+n).order(ByteOrder.LITTLE_ENDIAN);
                    packet.put(new byte[]{'D','A','T','A'}).putInt(n).put(fileChunk,0,n);
                    writeChunked(stream, packet.array(), 4000);
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
            if (kind == 9) drawDownload(canvas);
            else if (kind == 0) drawCube(canvas);
            else if (kind == 1) drawRefresh(canvas);
            else if (kind == 2) drawTrash(canvas);
            else if (kind == 3) drawGear(canvas);
            else if (kind == 4) drawHome(canvas);
            else if (kind == 5) drawDatabase(canvas);
            else if (kind == 6) drawTag(canvas);
            else if (kind == 7) drawInfo(canvas);
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
            Path tray = new Path(); tray.moveTo(22, 64); tray.lineTo(22, 77); tray.quadTo(22, 82, 28, 82); tray.lineTo(72, 82); tray.quadTo(78, 82, 78, 77); tray.lineTo(78, 64); canvas.drawPath(tray, icon);
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
    }
    private final class NivoMarkView extends android.widget.ImageView {
        NivoMarkView(android.content.Context context) {
            super(context);
            setImageResource(R.drawable.nivo_mark);
            setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            setContentDescription("NIIVO");
        }
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
    }    private Drawable successOutline(int radius) {
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
                stroke.setStrokeWidth(dp(1.5f));
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
