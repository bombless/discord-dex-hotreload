#!/usr/bin/env sh
# Minimal Gradle 9.1.0 bootstrap. Run with: sh ./gradlew <tasks>
set -eu
VERSION=9.1.0
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/manual-wrapper"
DIST="$BASE/gradle-$VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BASE"
  ARCHIVE="$BASE/gradle-$VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ARCHIVE"
  else
    echo "curl is required to download Gradle $VERSION" >&2
    exit 1
  fi
  unzip -q -o "$ARCHIVE" -d "$BASE"
fi
exec "$DIST/bin/gradle" "$@"
