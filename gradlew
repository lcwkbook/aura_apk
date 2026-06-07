#!/usr/bin/env sh
set -e
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=7.2
DIST_NAME=gradle-${GRADLE_VERSION}-bin
DIST_URL=https://services.gradle.org/distributions/${DIST_NAME}.zip
CACHE_DIR=${HOME}/.gradle/wrapper/dists/${DIST_NAME}/manual
GRADLE_HOME=${CACHE_DIR}/gradle-${GRADLE_VERSION}
ZIP_FILE=${CACHE_DIR}/${DIST_NAME}.zip

if [ ! -x "${GRADLE_HOME}/bin/gradle" ]; then
  mkdir -p "${CACHE_DIR}"
  if [ ! -s "${ZIP_FILE}" ]; then
    echo "Downloading ${DIST_URL}"
    if command -v curl >/dev/null 2>&1; then
      curl -L -o "${ZIP_FILE}" "${DIST_URL}"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "${ZIP_FILE}" "${DIST_URL}"
    else
      echo "curl or wget is required to download Gradle" >&2
      exit 1
    fi
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "${ZIP_FILE}" -d "${CACHE_DIR}"
  else
    echo "unzip is required to unpack Gradle" >&2
    exit 1
  fi
  chmod +x "${GRADLE_HOME}/bin/gradle" || true
fi

cd "${APP_HOME}"
exec "${GRADLE_HOME}/bin/gradle" "$@"
