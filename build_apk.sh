#!/bin/bash
# Сборка APK приложения "Роуз Фит"
set -e
export JAVA_HOME='C:\Games\COSClaude\_tools\jdk-dl\extracted\jdk-17.0.20.1+1'
GRADLE="/c/Games/COSClaude/_tools/gradle-dl/extracted/gradle-8.7/bin/gradle.bat"
cd /c/Games/COSClaude/FitnessTracker
"$GRADLE" assembleRelease --no-daemon -g 'C:\Games\COSClaude\_tools\gradle-home'
echo "BUILD_OK"
ls -la app/build/outputs/apk/release/
