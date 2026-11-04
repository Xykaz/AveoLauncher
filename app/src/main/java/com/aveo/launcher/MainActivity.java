package com.aveo.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {
    private static final int RC_LOCATION = 201;
    private static final int RC_HOME = 101;

    private final int bg = Color.rgb(8, 15, 27);
    private final int panel = Color.rgb(20, 32, 50);
    private final int panelAlt = Color.rgb(12, 24, 40);
    private final int accent = Color.rgb(48, 164, 245);
    private final int muted = Color.rgb(169, 190, 211);

    private TextView batteryValue;
    private TextView gpsValue;
    private TextView systemValue;
    private TextView mediaValue;
    private TextView btValue;
    private TextView infoBar;
    private LocationManager locationManager;
    private BroadcastReceiver batteryReceiver;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), Color.rgb(43, 66, 91));
        return d;
    }

    private TextView label(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void add(LinearLayout p, View v, int w, int h, float weight) {
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h), weight);
        a.setMargins(dp(4), dp(4), dp(4), dp(4));
        p.addView(v, a);
    }

    private LinearLayout valueCard(String title, TextView valueView) {
        LinearLayout l = col();
        l.setBackground(shape(panel));
        l.setPadding(dp(13), dp(10), dp(13), dp(10));
        add(l, label(title, 12, muted), -1, -2, 0);
        valueView.setTextSize(17);
        valueView.setTextColor(Color.WHITE);
        add(l, valueView, -1, -2, 0);
        return l;
    }

    private Button btn(String text, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(shape(panel));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private List<ResolveInfo> apps() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(i, 0);
        Collections.sort(list, (a, b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        return list;
    }

    private void start(ResolveInfo a) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(a.activityInfo.packageName, a.activityInfo.name);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir l'application", Toast.LENGTH_SHORT).show();
        }
    }

    private void launch(String... names) {
        for (ResolveInfo a : apps()) {
            String n = a.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);
            for (String q : names) {
                if (n.contains(q.toLowerCase(Locale.ROOT))) {
                    start(a);
                    return;
                }
            }
        }
        Toast.makeText(this, "Application non trouvée", Toast.LENGTH_SHORT).show();
    }

    private void allApps() {
        List<ResolveInfo> list = apps();
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) names[i] = list.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this)
                .setTitle("Applications")
                .setItems(names, (d, index) -> start(list.get(index)))
                .setNegativeButton("Fermer", null)
                .show();
    }

    private void requestDefaultHome() {
        try {
            RoleManager rm = getSystemService(RoleManager.class);
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                    Toast.makeText(this, "Aveo Launcher est déjà l'accueil par défaut", Toast.LENGTH_LONG).show();
                    return;
                }
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), RC_HOME);
                return;
            }
        } catch (Exception ignored) {
        }
        openHomeSettings();
    }

    private void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Sélection de l'accueil indisponible")
                    .setMessage("Le firmware Diamond masque le sélecteur HOME. Nous pouvons basculer sur Aveo Launcher via ADB si nécessaire.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void ensureLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, RC_LOCATION);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) return;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500L, 0f, this);
            }
            gpsValue.setText("Recherche...");
        } catch (Exception e) {
            gpsValue.setText("Service indisponible");
        }
    }

    private void stopLocationUpdates() {
        try {
            if (locationManager != null) locationManager.removeUpdates(this);
        } catch (Exception ignored) {
        }
    }

    private void registerBattery() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int mv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                if (mv > 0) {
                    batteryValue.setText(String.format(Locale.US, "%.1f V", mv / 1000f));
                } else {
                    batteryValue.setText("Source à identifier");
                }
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void setStaticStatuses() {
        mediaValue.setText("Raccourcis prêts");
        btValue.setText("Bluetooth / Wi‑Fi disponibles");
        systemValue.setText("OBD à connecter");
        infoBar.setText("Rotation 3D au doigt • pince pour zoomer • accueil configurable");
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(5), dp(5), dp(5), dp(5));

        LinearLayout left = col();
        add(left, label("AVEO", 22, accent), -1, 44, 0);
        add(left, btn("Définir accueil", this::requestDefaultHome), -1, 52, 0);
        add(left, btn("TLink5", () -> launch("tlink")), -1, 52, 0);
        add(left, btn("Navigation", () -> launch("maps", "navigation")), -1, 52, 0);
        add(left, btn("Radio FM", () -> launch("radio")), -1, 52, 0);
        add(left, btn("Musique", () -> launch("musique", "music", "youtube")), -1, 52, 0);
        add(left, btn("Applications", this::allApps), -1, 52, 0);
        add(root, left, 128, -1, 0);

        LinearLayout middle = col();
        middle.setBackground(shape(panel));
        middle.setPadding(dp(15), dp(10), dp(15), dp(10));
        TextView head = label("CHEVROLET AVEO T250", 20, Color.WHITE);
        head.setTypeface(null, Typeface.BOLD);
        add(middle, head, -1, 42, 0);
        add(middle, label("Hatchback 5 portes  •  Blanc  •  Vue 3D légère", 12, muted), -1, 24, 0);

        LinearLayout sceneContainer = col();
        sceneContainer.setBackground(shape(panelAlt));
        sceneContainer.setPadding(dp(8), dp(8), dp(8), dp(8));

        AveoGLSurfaceView glView = new AveoGLSurfaceView(this);
        add(sceneContainer, glView, -1, 250, 0);
        infoBar = label("", 12, muted);
        add(sceneContainer, infoBar, -1, 24, 0);
        add(middle, sceneContainer, -1, 0, 1);
        add(root, middle, 0, -1, 1);

        LinearLayout right = col();
        LinearLayout clock = col();
        clock.setPadding(dp(12), dp(8), dp(12), dp(8));
        clock.setBackground(shape(panel));
        add(clock, label("TABLEAU DE BORD", 12, muted), -1, 28, 0);
        TextClock time = new TextClock(this);
        time.setFormat24Hour("HH:mm");
        time.setTextSize(34);
        time.setTextColor(Color.WHITE);
        time.setGravity(Gravity.CENTER);
        add(clock, time, -1, 58, 0);
        add(right, clock, -1, -2, 0);

        batteryValue = label("Lecture...", 17, Color.WHITE);
        gpsValue = label("À activer", 17, Color.WHITE);
        mediaValue = label("Lecture...", 17, Color.WHITE);
        systemValue = label("Lecture...", 17, Color.WHITE);
        btValue = label("Lecture...", 17, Color.WHITE);

        add(right, valueCard("Tension véhicule", batteryValue), -1, -2, 0);
        add(right, valueCard("Vitesse GPS", gpsValue), -1, -2, 0);
        add(right, valueCard("Multimédia", mediaValue), -1, -2, 0);
        add(right, valueCard("Informations moteur", systemValue), -1, -2, 0);
        add(right, valueCard("Connectivité", btValue), -1, -2, 0);

        add(right, btn("Activer GPS", this::ensureLocation), -1, 48, 0);
        add(right, btn("Android Auto / TLink5", () -> launch("tlink")), -1, 48, 0);
        add(right, btn("Toutes les applications", this::allApps), -1, 48, 0);
        add(right, btn("Choisir l'accueil par défaut", this::requestDefaultHome), -1, 48, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(right);
        add(root, scroll, 238, -1, 0);

        setContentView(root);
        setStaticStatuses();
        registerBattery();
        ensureLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        try {
            if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        float kmh = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;
        gpsValue.setText(String.format(Locale.US, "%.0f km/h", kmh));
    }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) {
        gpsValue.setText("GPS désactivé");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) granted = false;
            if (granted) startLocationUpdates();
            else gpsValue.setText("Permission refusée");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_HOME) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Aveo Launcher est maintenant l'accueil par défaut", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Accueil non modifié", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
