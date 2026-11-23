#!/usr/bin/env python3
"""Dependency-free structural checks for the Mixezy Mobile Android project."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
NS = {"a": "http://schemas.android.com/apk/res/android"}
errors = []

def require(condition, message):
    if not condition:
        errors.append(message)

manifest_path = ANDROID / "app/src/main/AndroidManifest.xml"
try:
    manifest = ET.parse(manifest_path).getroot()
except Exception as exc:
    errors.append(f"AndroidManifest.xml illisible: {exc}")
    manifest = None

if manifest is not None:
    app = manifest.find("application")
    require(app is not None, "Balise application absente")
    if app is not None:
        require(app.get(f"{{{NS['a']}}}usesCleartextTraffic") == "false", "Le trafic HTTP non chiffré doit être désactivé")
        require(app.get(f"{{{NS['a']}}}allowBackup") == "false", "allowBackup doit être false")
    activities = manifest.findall("application/activity") if app is not None else []
    require(any(a.get(f"{{{NS['a']}}}name") == ".MainActivity" for a in activities), "MainActivity absente du manifeste")
    require(any(
        a.get(f"{{{NS['a']}}}name") == ".MainActivity" and a.get(f"{{{NS['a']}}}exported") == "true"
        for a in activities
    ), "MainActivity doit être exported=true pour le launcher")

strings_path = ANDROID / "app/src/main/res/values/strings.xml"
try:
    strings = ET.parse(strings_path).getroot()
    app_name = strings.find("string[@name='app_name']")
    require(app_name is not None and (app_name.text or "").strip() == "Mixezy Mobile", "Nom affiché incorrect")
except Exception as exc:
    errors.append(f"strings.xml illisible: {exc}")

build_file = ANDROID / "app/build.gradle"
text = build_file.read_text() if build_file.exists() else ""
require("applicationId 'com.mixezy.mobile'" in text, "Application ID incorrect")
require("versionName '0.9.3'" in text, "versionName incorrect")
require("targetSdk 35" in text, "targetSdk attendu: 35")
require("buildConfigField" in text and "DEFAULT_SERVER_URL" in text, "Configuration serveur pré-définie manquante")

activity = ANDROID / "app/src/main/java/com/mixezy/mobile/MainActivity.java"
java = activity.read_text() if activity.exists() else ""
require("isTrustedServerUri(uri)" in java, "Contrôle d’origine HTTPS absent")
require("candidate.equals(serverOrigin)" in java, "Comparaison d’origine complète absente")
require("onReceivedSslError" in java and "handler.cancel()" in java, "Les erreurs de certificat doivent être bloquées")
require("setAllowFileAccess(false)" in java, "Accès fichiers WebView doit être désactivé")
require("setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)" in java, "Le contenu mixte doit être bloqué")

for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
    icon = ANDROID / f"app/src/main/res/mipmap-{density}/ic_launcher.png"
    require(icon.is_file() and icon.stat().st_size > 0, f"Icône manquante: {density}")

workflow = ROOT / ".github/workflows/android-apk.yml"
wf = workflow.read_text() if workflow.exists() else ""
require("workflow_dispatch:" in wf, "Déclenchement manuel du workflow absent")
require("pull_request:" in wf, "Build de vérification des pull requests absent")
require("actions/checkout@v7" in wf and "gradle/actions/setup-gradle@v6" in wf, "Actions de build non actualisées")
require("actions/setup-java@v6" in wf, "Setup Java absent ou obsolète")
require("android-actions/setup-android@v4" in wf, "Setup Android SDK absent ou obsolète")
require("apksigner" in wf and "sha256sum" in wf, "Vérification APK ou SHA-256 absente")
require("github.event.inputs.server_url" in wf, "Expression du champ URL compatible push absente")

if errors:
    print("ÉCHEC - Validation Mixezy Mobile Android")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("OK - Structure Android valide")
print("OK - Manifest et ressources XML")
print("OK - Identité Mixezy Mobile / com.mixezy.mobile")
print("OK - Contrôles HTTPS et sécurité WebView")
print("OK - Workflow APK, signature et SHA-256")
