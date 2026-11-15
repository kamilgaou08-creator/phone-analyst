package com.phoneanalyst.app;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.app.ActivityManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private LinearLayout content;
    private EventStore store;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new EventStore(this);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 36, 36, 36);
        content.setBackgroundColor(0xFFF3F6FA);
        scroll.addView(content);
        setContentView(scroll);
        refresh();
    }

    private void addText(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(0xFF172033);
        if (bold) t.setTypeface(null, 1);
        t.setPadding(0, 12, 0, 12);
        content.addView(t);
    }

    private void addButton(String label, Runnable action) {
        Button b = new Button(this); b.setText(label);
        b.setOnClickListener(v -> action.run());
        content.addView(b);
    }

    private void refresh() {
        content.removeAllViews();
        addText("PHONE ANALYST", 27, true);
        addText("Tableau de bord • " + DateFormat.getDateTimeInstance().format(new Date()), 13, false);

        int battery = batteryLevel();
        addText("BATTERIE", 19, true);
        addText(battery >= 0 ? "Charge actuelle : " + battery + " %" : "Niveau de batterie indisponible", 17, false);
        addText(batteryRecommendation(battery), 14, false);

        addText("WI-FI ET RÉSEAU", 19, true);
        addText(wifiStatus(), 16, false);

        addText("SYSTÈME", 19, true);
        addText(memoryStatus(), 16, false);
        addText(storageStatus(), 16, false);

        long day = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
        long week = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        addText("ACTIVITÉ DES APPLICATIONS", 19, true);
        addText("Événements enregistrés sur 24 h : " + store.countSince(day), 16, false);
        addText("Événements enregistrés sur 7 jours : " + store.countSince(week), 16, false);
        addText("Applications les plus actives (événements) :", 15, true);
        for (String s : store.topAppsSince(day)) addText("• " + s, 14, false);
        if (!hasUsageAccess()) addText("Autorise « Accès aux données d'utilisation » pour consulter les durées d'utilisation fournies par Android.", 14, false);
        else addText(usageSummary(), 14, false);

        addText("RECOMMANDATIONS", 19, true);
        addText(recommendations(battery), 15, false);

        addButton("Actualiser le tableau de bord", this::refresh);
        addButton("Autoriser les statistiques d'utilisation", () ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        addButton("Activer le suivi avancé (Accessibilité)", () ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addButton("Générer le rapport journalier", () -> showReport(false));
        addButton("Générer le rapport hebdomadaire", () -> showReport(true));
        addButton("Partager le rapport", () -> shareReport());
        addText("Confidentialité : les données collectées restent dans la base locale de cette application. Le suivi avancé est facultatif et peut être désactivé dans les paramètres Android.", 13, false);
    }

    private int batteryLevel() {
        Intent i = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return -1;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (level < 0 || scale <= 0) ? -1 : Math.round(level * 100f / scale);
    }

    private String batteryRecommendation(int level) {
        if (level < 0) return "Vérifie les informations de batterie dans les paramètres.";
        if (level <= 20) return "Conseil : active l'économie d'énergie et réduis la luminosité.";
        if (level >= 80) return "Charge élevée. Tu peux débrancher lorsque la charge est suffisante.";
        return "Niveau de charge normal. Surveille les applications énergivores.";
    }

    private String wifiStatus() {
        try {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            if (n == null) return "Aucun réseau actif détecté.";
            NetworkCapabilities caps = cm.getNetworkCapabilities(n);
            if (caps == null) return "État du réseau indisponible.";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                int dbm = -999;
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    Object info = caps.getTransportInfo();
                    if (info instanceof WifiInfo) dbm = ((WifiInfo)info).getRssi();
                }
                return "Wi-Fi connecté • signal : " + (dbm == -999 ? "non communiqué" : dbm + " dBm") +
                        "\nLe débit réel dépend du routeur, de la distance et de la congestion.";
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Données mobiles actives.";
            return "Réseau actif (type non identifié).";
        } catch (Exception e) { return "Informations Wi-Fi indisponibles sur cette version Android."; }
    }

    private String memoryStatus() {
        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long total = mi.totalMem / (1024 * 1024);
        long available = mi.availMem / (1024 * 1024);
        return "RAM : " + available + " Mo disponibles sur " + total + " Mo.";
    }

    private String storageStatus() {
        StatFs s = new StatFs(Environment.getDataDirectory().getPath());
        long total = s.getTotalBytes() / (1024 * 1024 * 1024);
        long free = s.getAvailableBytes() / (1024 * 1024 * 1024);
        return "Stockage interne : " + free + " Go libres sur " + total + " Go.";
    }

    private boolean hasUsageAccess() {
        try {
            AppOpsManager a = (AppOpsManager)getSystemService(APP_OPS_SERVICE);
            int mode = a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) { return false; }
    }

    private String usageSummary() {
        try {
            UsageStatsManager usm = (UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
            Map<String, UsageStats> map = usm.queryAndAggregateUsageStats(
                    System.currentTimeMillis() - 24L*60*60*1000, System.currentTimeMillis());
            long total = 0;
            for (UsageStats u : map.values()) total += u.getTotalTimeInForeground();
            return "Temps total au premier plan (24 h, estimation Android) : " + (total / 3600000) +
                    " h " + ((total / 60000) % 60) + " min.";
        } catch (Exception e) { return "Statistiques d'utilisation non disponibles."; }
    }

    private String recommendations(int battery) {
        StringBuilder s = new StringBuilder();
        if (battery >= 0 && battery <= 20) s.append("• Batterie faible : active l'économiseur d'énergie.\n");
        s.append("• Si le Wi-Fi est faible, rapproche-toi du routeur ou vérifie son emplacement.\n");
        s.append("• Garde une marge de stockage libre pour les mises à jour.\n");
        s.append("• Consulte les applications les plus utilisées pour repérer celles qui tournent souvent.\n");
        s.append("• Les chiffres RAM et réseau sont des instantanés, pas un diagnostic complet.");
        return s.toString();
    }

    private String makeReport(boolean weekly) {
        long period = weekly ? 7L*24*60*60*1000 : 24L*60*60*1000;
        long since = System.currentTimeMillis() - period;
        return "PHONE ANALYST — RAPPORT " + (weekly ? "HEBDOMADAIRE" : "JOURNALIER") + "\n" +
                "Date : " + DateFormat.getDateTimeInstance().format(new Date()) + "\n\n" +
                "Événements d'interaction enregistrés : " + store.countSince(since) + "\n" +
                "Batterie : " + batteryLevel() + " %\n" + wifiStatus() + "\n" +
                memoryStatus() + "\n" + storageStatus() + "\n\n" +
                "Applications les plus actives (événements) :\n" +
                String.join("\n", store.topAppsSince(since)) + "\n\n" +
                "Conseils :\n" + recommendations(batteryLevel()) +
                "\n\nNote : les événements ne représentent pas chaque action réalisée. Les données dépendent des autorisations Android.";
    }

    private void showReport(boolean weekly) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(weekly ? "Rapport hebdomadaire" : "Rapport journalier")
                .setMessage(makeReport(weekly))
                .setPositiveButton("Fermer", null).show();
    }

    private void shareReport() {
        String report = makeReport(false) + "\n\n" + makeReport(true);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Rapports PHONE ANALYST");
        send.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(send, "Partager les rapports"));
    }
}
