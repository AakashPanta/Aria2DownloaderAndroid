#!/bin/sh

# Lightweight Gradle launcher for Codespaces/browser builds.
# 1) Prefer an installed Gradle binary (Codespaces/devcontainer path)
# 2) Fall back to the standard Gradle wrapper jar if it exists

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
  exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "ERROR: Gradle is not installed and gradle-wrapper.jar is missing." >&2
echo "For GitHub Codespaces, let the devcontainer finish setup, then run this again." >&2
echo "Expected wrapper jar path: $WRAPPER_JAR" >&2
exit 1
