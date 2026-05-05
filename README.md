# 📚 EpubToAudiobook

Application Android qui convertit vos fichiers EPUB en audiobooks en utilisant le moteur TTS (Text-to-Speech) local de votre appareil, sans connexion internet requise.

## ✨ Fonctionnalités

### 📖 Bibliothèque EPUB
- Import de fichiers EPUB depuis le stockage de l'appareil
- Affichage de la couverture, titre et auteur
- Conversion en arrière-plan avec barre de progression par chapitre
- Suppression avec confirmation

### 🎧 Bibliothèque Audiobooks
- Liste des audiobooks générés avec couverture
- Affichage de la durée totale et de la progression de lecture
- Reprise automatique à la position exacte de la dernière écoute

### ▶️ Lecteur (style Audible)
- **Couverture** du livre affichée en grand format
- **Contrôles complets** :
  - ⏮ Reculer de 10 secondes
  - ⏮ Revenir au début du chapitre actuel
  - ⏪ Chapitre précédent
  - ▶/⏸ Play / Pause
  - ⏩ Chapitre suivant
  - ⏭ Avancer de 10 secondes
- **Vitesse de lecture** : 0.75x · 1x · 1.25x · 1.5x · 2x
- **Liste des chapitres** déroulable avec chapitre actif surligné, tap pour sauter
- **Position sauvegardée** automatiquemet toutes les 5 secondes
- **Notification média** avec couverture et contrôles (accessible depuis l'écran verrouillé)

### ⚙️ Paramètres
- Sélecteur de voix TTS parmi toutes les voix installées sur l'appareil
- Test de la voix sélectionnée
- Conseil pour installer Google TTS pour de meilleures voix

## 🏗️ Architecture

```
app/src/main/
├── data/
│   ├── db/          # Room Database (AppDatabase, DAOs)
│   ├── model/       # Entités (EpubBookEntity, AudiobookEntity, PlaybackStateEntity, ChapterMark)
│   └── repository/  # Repositories (Epub, Audiobook, Playback)
├── service/
│   ├── TtsConversionWorker.kt     # WorkManager: EPUB → WAV via TTS
│   └── AudiobookPlayerService.kt  # MediaSessionService (ExoPlayer)
└── ui/
    ├── screens/     # EpubLibraryScreen, AudiobookLibraryScreen, PlayerScreen, SettingsScreen
    ├── viewmodel/   # ViewModels pour chaque écran
    └── theme/       # Material3 theme
```

## 🛠️ Stack technique

| Composant | Technologie |
|---|---|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| Lecture audio | Media3 / ExoPlayer + MediaSession |
| Conversion TTS | Android TextToSpeech (`synthesizeToFile`) |
| Parsing EPUB | epublib-core 3.1 |
| Base de données | Room |
| Tâches background | WorkManager |
| Chargement images | Coil |

## 📦 Stockage

Les audiobooks générés sont sauvegardés dans :
```
/sdcard/Music/EpubToAudiobook/<titre>.wav
```

## 🚀 Build & Installation

### Prérequis
- Android Studio ou SDK Android
- `adb` dans le PATH
- Appareil Android avec Débogage USB activé

### Script tout-en-un
```bash
./build_and_install.sh
```

Ce script compile, installe et lance automatiquement l'application sur l'appareil connecté.

### Manuel
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 📋 Permissions requises

- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO` — accès aux fichiers
- `WRITE_EXTERNAL_STORAGE` — sauvegarde des audiobooks dans Music/
- `POST_NOTIFICATIONS` — notification de lecture (Android 13+)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — lecture en arrière-plan

## 📱 Compatibilité

- **Android minimum** : API 24 (Android 7.0)
- **Android cible** : API 36

