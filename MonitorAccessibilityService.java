package com.phoneanalyst.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class MonitorAccessibilityService extends AccessibilityService {
    private EventStore store;

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        store = new EventStore(this);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || store == null) return;
        int type = event.getEventType();
        String label;
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) label = "APPLICATION_OUVERTE";
        else if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) label = "CLIC";
        else return;

        // Confidentialité : uniquement le nom du paquet, le type et l'heure.
        // Aucun texte, contenu de champ, capture d'écran ou mot de passe n'est lu.
        CharSequence packageName = event.getPackageName();
        String pkg = packageName == null ? "inconnu" : packageName.toString();
        store.addEvent(System.currentTimeMillis(), pkg, label);
    }

    @Override public void onInterrupt() { }
}
