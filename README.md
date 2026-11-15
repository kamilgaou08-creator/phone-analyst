# PHONE ANALYST — Android (Java)

Application Android de diagnostic personnel avec tableau de bord, rapports journaliers/hebdomadaires et suivi avancé facultatif.

## Fonctionnalités
- Niveau de batterie et recommandations simples.
- Type de réseau actif et RSSI Wi-Fi lorsque Android le communique.
- RAM et espace de stockage disponibles.
- Statistiques d'utilisation Android après autorisation spéciale.
- Service Accessibilité facultatif : enregistre localement le nom du paquet actif lors d'un changement de fenêtre et les clics signalés par Android. Il ne lit pas le texte affiché et ne récupère pas le contenu des champs.
- Rapports partageables en texte.

## Compiler l'APK avec GitHub depuis un téléphone
1. Crée un dépôt GitHub vide nommé `phone-analyst`.
2. Téléverse tous les fichiers et dossiers de ce projet en conservant leur arborescence.
3. Ouvre l'onglet **Actions** du dépôt.
4. Sélectionne **Build PHONE ANALYST APK**, puis **Run workflow**.
5. Attends la fin du workflow.
6. Ouvre l'exécution terminée, puis télécharge l'artefact `PHONE_ANALYST-debug-apk`.
7. Décompresse l'archive téléchargée et installe `app-debug.apk`. Autorise l'installation depuis la source utilisée si Android le demande.

Le dépôt doit contenir à sa racine `settings.gradle`, `build.gradle`, `gradle.properties`, le dossier `app/` et `.github/workflows/android.yml`.

## Autorisations à activer sur le téléphone
- **Accès aux données d'utilisation** : pour les durées agrégées fournies par Android.
- **Accessibilité > PHONE ANALYST** : facultatif, pour les événements généraux d'ouverture et de clic.

Les intitulés des menus peuvent varier selon MIUI/HyperOS. Le service Accessibilité est désactivable à tout moment.

## Limites importantes
Android ne permet pas à une application ordinaire de voir toutes les actions effectuées dans les autres applications. Les événements d'accessibilité sont partiels et dépendent de la version Android et de l'application observée. Cette version ne collecte ni contenu de messages, ni mots de passe, ni frappes clavier, ni captures d'écran. Les estimations de batterie et de réseau ne remplacent pas les mesures système détaillées. Le rapport est un instantané au moment de sa génération ; la collecte en continu dépend des restrictions d'arrière-plan d'Android.

## Sécurité et signature
L'APK `debug` généré par GitHub Actions est destiné aux essais et peut être installé directement. Pour une diffusion durable, il faudra créer une clé de signature privée et produire un APK release signé. Ne partage jamais cette clé ni son mot de passe.
