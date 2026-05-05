#!/bin/bash

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

echo "====================================="
echo "  EPUB → Audiobook — Build & Install"
echo "====================================="

# Vérification ADB
if ! command -v adb &> /dev/null; then
    echo "❌ adb introuvable. Installez Android SDK Platform-Tools et ajoutez-le au PATH."
    exit 1
fi

# Vérification appareil connecté
DEVICE=$(adb devices | grep -v "List of devices" | grep "device$" | head -1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo "❌ Aucun appareil Android connecté."
    echo "   → Branchez votre téléphone en USB et activez le Débogage USB."
    exit 1
fi

DEVICE_MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
echo "📱 Appareil détecté : $DEVICE_MODEL ($DEVICE)"

# Compilation
echo ""
echo "🔨 Compilation en cours..."
cd "$PROJECT_DIR"
./gradlew assembleDebug

if [ ! -f "$APK" ]; then
    echo "❌ APK non trouvé après la compilation."
    exit 1
fi

APK_SIZE=$(du -h "$APK" | awk '{print $1}')
echo "✅ Compilation réussie — APK : $APK_SIZE"

# Installation
echo ""
echo "📦 Installation sur $DEVICE_MODEL..."
adb -s "$DEVICE" install -r "$APK"

echo ""
echo "🚀 Lancement de l'application..."
adb -s "$DEVICE" shell am start -n "com.example.epubtoaudiobook/.MainActivity"

echo ""
echo "====================================="
echo "✅ Terminé ! L'app est lancée."
echo "====================================="

