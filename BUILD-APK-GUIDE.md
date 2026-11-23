# Générer l’APK Android de Mixezy Mobile

Le projet Android autonome se trouve dans `android/`. Le workflow GitHub Actions se trouve dans `.github/workflows/android-apk.yml`.

## Méthode simple : GitHub Actions

1. Crée un dépôt GitHub vide, par exemple `mixezy-mobile-android`.
2. Décompresse le paquet Android autonome et envoie **son contenu** à la racine du dépôt. Les dossiers `.github/` et `android/` doivent donc se trouver directement à la racine.

   Depuis un terminal ouvert dans le dossier extrait, tu peux publier le projet avec Git (remplace `<TON_COMPTE>` par ton nom GitHub) :

   ```bash
   git init
   git add .
   git commit -m "Initial Mixezy Mobile Android project"
   git branch -M main
   git remote add origin https://github.com/<TON_COMPTE>/mixezy-mobile-android.git
   git push -u origin main
   ```

3. Ouvre l’onglet **Actions** et sélectionne **Build Mixezy Mobile Android APK**.
4. Clique sur **Run workflow**. Tu peux saisir l’URL HTTPS publique du backend dans le champ `server_url`. Laisse-le vide si tu préfères saisir l’adresse sur le téléphone au premier lancement.
5. À la fin, télécharge l’artefact `mixezy-mobile-android-debug-apk`.
6. Extrais-le : il contient `Mixezy-Mobile-v0.9.3-debug.apk` et son fichier `.sha256`.

Autre possibilité : créer la variable de dépôt `MIXEZY_SERVER_URL` dans **Settings → Secrets and variables → Actions → Variables**. Elle est utilisée si le champ du workflow est vide.

## Compilation locale

Pré-requis : JDK 17, Gradle 8.9, Android SDK Platform 35 et Build Tools 35.0.0.

Depuis la racine du projet :

```bash
gradle -p android --no-daemon assembleDebug
```

Avec une URL de serveur :

```bash
gradle -p android -PMIXEZY_SERVER_URL=https://app.ton-domaine.com assembleDebug
```

Le fichier sera généré dans `android/app/build/outputs/apk/debug/app-debug.apk`.

## Installer sur Android

1. Télécharge le fichier APK sur le téléphone.
2. Ouvre-le depuis le gestionnaire de fichiers.
3. Autorise l’installation depuis cette source lorsque Android le demande.
4. Lance **Mixezy Mobile**.
5. Si aucune URL n’a été préconfigurée, saisis l’adresse HTTPS publique du backend.

## Sécurité et limites

- Cette build debug est destinée aux tests. Pour Google Play ou la distribution commerciale, une build release signée avec une clé de publication privée est requise.
- Ne publie jamais la clé de signature ni les clés API Stripe, Replicate ou Resend dans le dépôt.
- L’APK est le client Android. Le serveur SaaS, les comptes, les paiements, la bibliothèque média et les traitements IA doivent être hébergés séparément sur un domaine HTTPS opérationnel.
- Le workflow vérifie que l’APK est signé et produit une empreinte SHA-256 pour vérifier l’intégrité du fichier téléchargé.

## Validation locale et compilation en une commande

Avant la compilation, vérifie la structure sans dépendance supplémentaire :

```bash
python3 tools/validate_android_project.py
```

Sur une machine configurée avec JDK 17, Gradle 8.9 et Android SDK Platform 35 :

```bash
./build-apk.sh
```

Pour préconfigurer l’URL du serveur :

```bash
MIXEZY_SERVER_URL=https://app.ton-domaine.com ./build-apk.sh
```
