# TODO — Move sources, verify build, test, and release (v1.1.0)

This file records the implementation plan and acceptance checklist. Follow each step and mark items complete.

Summary
-------
Move Kotlin sources under `com.shell.epubtoaudiobook`, verify imports/package declarations, run a clean build and deploy to a connected device, run an end-to-end EPUB → audiobook conversion + playback verification, capture logs and DB state, update `README.md`, and create a PR + release.

Acceptance checklist
--------------------
- [ ] Create working branch `chore/refactor/move-packages`
- [ ] Move all Kotlin sources to `app/src/main/java/com/shell/epubtoaudiobook`
- [ ] Fix package declarations and imports (no `com.example` remains)
- [ ] `./gradlew clean assembleDebug` completes successfully
- [ ] `./build_and_install.sh` installs and launches the app on connected device
- [ ] E2E conversion test: generated audiobook exists in `/sdcard/Music/EpubToAudiobook`
- [ ] Conversion notifications observed: start / progress / finished
- [ ] Playback controls verified: skip ±10s, prev/next chapter, start-of-chapter, speed control
- [ ] Playback position persistence verified after app restart
- [ ] Logs and DB snapshot captured and attached to PR
- [ ] `README.md` updated with v1.1.0, new package name, and notification behaviour
- [ ] PR opened, merged to `main`, tag `v1.1.0` created and pushed, release created with APK attached

Commands and steps (copy / run as needed)
----------------------------------------
# 1. Create branch
git checkout -b chore/refactor/move-packages

# 2. Move sources (example - run only if sources still under com/example)
mkdir -p app/src/main/java/com/shell
git mv app/src/main/java/com/example/epubtoaudiobook app/src/main/java/com/shell/epubtoaudiobook
# similarly for test/androidTest if present

# 3. Fix package declarations (use sed to replace any remaining com.example occurrences)
grep -R --line-number "package com\.example" app || echo "no package com.example found"
find app/src -type f -name '*.kt' -exec sed -i 's/^package com\.example\.epubtoaudiobook/package com.shell.epubtoaudiobook/' {} +

# 4. Build and install
./gradlew clean assembleDebug
chmod +x ./build_and_install.sh
./build_and_install.sh

# 5. Capture logs during E2E test
adb logcat -c
adb logcat -v time | tee conversion_playback.log
# then in app: import EPUB, trigger conversion; observe logs & notifications

# 6. Pull DB and media files
adb exec-out run-as com.shell.epubtoaudiobook cat databases/epubtoaudiobook.db > epubtoaudiobook.db || echo "run-as failed: use adb pull if not debuggable"
adb shell ls -l /sdcard/Music/EpubToAudiobook
adb pull /sdcard/Music/EpubToAudiobook .

# 7. Commit, push, PR, merge, tag, release
git add -A
git commit -m "refactor: move Kotlin sources under com.shell; update docs and tasks"
git push --set-upstream origin chore/refactor/move-packages
# create PR with gh or web UI
# after merge:
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk EpubToAudiobook-1.1.0.apk
git tag -a v1.1.0 -m "v1.1.0"
git push origin v1.1.0
# create GH release with gh: gh release create v1.1.0 EpubToAudiobook-1.1.0.apk --title "v1.1.0" --notes "Release notes..."

Risk & assumptions
------------------
- Assumes `adb` and a connected Android device are available and visible via `adb devices`.
- Assumes `gh` (GitHub CLI) is available if you want to automate PR/release creation; otherwise use GitHub web UI.
- For signed release you will need a keystore and signing config.

Notes
-----
- If your repo already contains sources under `com.shell.epubtoaudiobook`, skip the move step and just verify no `com.example` occurrences remain.
- Keep backups/branches before large refactors.
