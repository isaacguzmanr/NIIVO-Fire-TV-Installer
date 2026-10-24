package com.nivo.installer;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final int BG = Color.rgb(5, 11, 20);
    private static final int PANEL = Color.rgb(14, 27, 42);
    private static final int CYAN = Color.rgb(36, 215, 211);
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

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(24), dp(24), dp(28));
        scroll.addView(page);

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.nivo.installer.R.drawable.nivo_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout logoRow = new LinearLayout(this);
        logoRow.setGravity(Gravity.CENTER);
        logoRow.addView(logo, new LinearLayout.LayoutParams(dp(112), dp(112)));
        page.addView(logoRow);

        TextView title = text("Instalador NIVO", 27, WHITE, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(8);
        page.addView(title, titleLp);

        TextView intro = text("Instala o actualiza NIVO en tu Fire TV desde este teléfono.", 15, MUTED, false);
        intro.setGravity(Gravity.CENTER);
        intro.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(7);
        introLp.bottomMargin = dp(18);
        page.addView(intro, introLp);

        LinearLayout help = new LinearLayout(this);
        help.setOrientation(LinearLayout.VERTICAL);
        help.setPadding(dp(16), dp(15), dp(16), dp(15));
        help.setBackground(round(PANEL, dp(16), Color.rgb(29, 56, 75), dp(1)));
        help.addView(text("ANTES DE EMPEZAR", 12, CYAN, true));
        TextView steps = text("1. En Fire TV, activa Depuración ADB.\n2. Conecta teléfono y TV a la misma Wi‑Fi.\n3. Si aparece el aviso, permite la depuración ADB en la TV.", 14, WHITE, false);
        steps.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams stepsLp = new LinearLayout.LayoutParams(-1, -2);
        stepsLp.topMargin = dp(8);
        help.addView(steps, stepsLp);
        page.addView(help);

        searchButton = button("Buscar Fire TV en mi Wi‑Fi", true);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(54));
        searchLp.topMargin = dp(18);
        page.addView(searchButton, searchLp);
        searchButton.setOnClickListener(v -> scanNetwork());

        devices = text("Todavía no se ha buscado ningún dispositivo.", 14, MUTED, false);
        devices.setPadding(dp(4), dp(12), dp(4), dp(5));
        page.addView(devices);

        TextView manual = text("También puedes escribir la IP que aparece en Fire TV > Mi Fire TV > Acerca de > Red.", 13, MUTED, false);
        LinearLayout.LayoutParams manualLp = new LinearLayout.LayoutParams(-1, -2);
        manualLp.topMargin = dp(11);
        page.addView(manual, manualLp);

        ipInput = new EditText(this);
        ipInput.setSingleLine(true);
        ipInput.setHint("IP del Fire TV, por ejemplo 192.168.1.7");
        ipInput.setHintTextColor(Color.rgb(116, 134, 151));
        ipInput.setTextColor(WHITE);
        ipInput.setTextSize(14);
        ipInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        ipInput.setPadding(dp(14), 0, dp(14), 0);
        ipInput.setBackground(round(Color.rgb(16, 30, 44), dp(12), Color.rgb(45, 68, 84), dp(1)));
        LinearLayout.LayoutParams ipLp = new LinearLayout.LayoutParams(-1, dp(52));
        ipLp.topMargin = dp(8);
        page.addView(ipInput, ipLp);

        installButton = button("Instalar NIVO", false);
        LinearLayout.LayoutParams installLp = new LinearLayout.LayoutParams(-1, dp(56));
        installLp.topMargin = dp(14);
        page.addView(installButton, installLp);
        ipInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                installButton.setEnabled(searchButton.isEnabled() && isValidIp(s.toString().trim()));
                installButton.setAlpha(installButton.isEnabled() ? 1f : 0.48f);
            }
            @Override public void afterTextChanged(Editable e) {}
        });        installButton.setOnClickListener(v -> installSelected());

        status = text("El APK de NIVO va incluido en esta app; no hace falta descargarlo aparte.", 13, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(4), dp(12), dp(4), 0);
        page.addView(status);
        setContentView(scroll);
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
        String host = ipInput.getText().toString().trim();
        if (!isValidIp(host)) {
            Toast.makeText(this, "Escribe una dirección IP válida de la TV.", Toast.LENGTH_LONG).show();
            return;
        }
        installButton.setEnabled(false);
        searchButton.setEnabled(false);
        status.setText("Conectando con " + host + ". Mira la TV y acepta el aviso de depuración ADB si aparece…");
        WORK.execute(() -> {
            AdbConnection connection = null;
            try {
                connection = connect(host);
                AdbConnection active = connection;
                String deviceInfo = runShell(active, "getprop ro.product.manufacturer; getprop ro.product.model");
                String deviceLower = deviceInfo.toLowerCase(java.util.Locale.ROOT);
                if (!deviceLower.contains("amazon") && !deviceLower.contains("aft")) {
                    throw new IOException("El dispositivo " + host + " no se identificó como Fire TV. No instalé nada. " + clean(deviceInfo));
                }
                runOnUiThread(() -> status.setText("Fire TV reconocido. Copiando NIVO… no cierres la app."));
                pushApk(active);
                runOnUiThread(() -> status.setText("APK copiado. Instalando NIVO y preparando el botón Inicio…"));
                String install = runShell(active, "pm install -r -g /data/local/tmp/nivo.apk");
                if (!install.contains("Success")) {
                    throw new IOException("La TV no aceptó la instalación. " + clean(install));
                }
                runShell(active, "settings put secure enabled_accessibility_services " + ACCESSIBILITY
                        + "; settings put secure accessibility_enabled 1"
                        + "; am start -n " + FIRE_PACKAGE + "/.HomeActivity");
                runOnUiThread(() -> {
                    status.setText("¡NIVO quedó instalado! El dock, la imagen y la configuración se conservan si ya estaban en esa TV.");
                    devices.setText("Instalación terminada en " + host + ". Ya puedes volver al Inicio del Fire TV.");
                    searchButton.setEnabled(true);
                });
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                runOnUiThread(() -> {
                    status.setText("No se completó: " + message + "\n\nVerifica que ADB esté activado, que ambas estén en la misma Wi‑Fi y que hayas aceptado el aviso en la TV.");
                    searchButton.setEnabled(true);
                    installButton.setEnabled(true);
                });
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
            }
        });
    }

    private AdbConnection connect(String host) throws Exception {
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
        socket.connect(new InetSocketAddress(host, 5555), 10000);
        socket.setSoTimeout(120000);
        AdbConnection conn = AdbConnection.create(socket, crypto);
        boolean ok = conn.connect(120, TimeUnit.SECONDS, false);
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
        AdbStream stream = conn.open("sync:");
        try {
            byte[] spec = "/data/local/tmp/nivo.apk,420".getBytes(StandardCharsets.UTF_8);
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
            if (!"OKAY".equals(id)) throw new IOException("Respuesta inesperada al copiar NIVO: " + id);
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

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view=new TextView(this);
        view.setText(value); view.setTextSize(sp); view.setTextColor(color);
        if(bold) view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }

    private Button button(String label, boolean primary) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(primary?BG:WHITE);
        b.setBackground(round(primary?CYAN:Color.rgb(24,42,57),dp(14),primary?CYAN:Color.rgb(51,77,95),dp(1)));
        b.setEnabled(primary);
        b.setAlpha(primary?1f:0.48f);
        b.setOnTouchListener((v,e)->{ if(!b.isEnabled())return false; if(e.getAction()==android.view.MotionEvent.ACTION_DOWN)b.setAlpha(0.78f); if(e.getAction()==android.view.MotionEvent.ACTION_UP||e.getAction()==android.view.MotionEvent.ACTION_CANCEL)b.setAlpha(1f); return false;});
        return b;
    }

    private GradientDrawable round(int color,int radius,int stroke,int strokeWidth) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(strokeWidth,stroke); return d;
    }
    private int dp(float value) { return (int)(value*getResources().getDisplayMetrics().density+0.5f); }
    @Override protected void onDestroy() { super.onDestroy(); /* executor is intentionally process-scoped for pending ADB I/O */ }
}
