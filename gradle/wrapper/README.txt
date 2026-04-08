This project is prepared for GitHub Codespaces builds.

Why gradle-wrapper.jar is not included here:
- The Codespaces devcontainer installs Gradle 8.2 directly.
- The provided ./gradlew script prefers the installed Gradle binary.
- If you later generate a full wrapper on a desktop machine, place gradle-wrapper.jar in this folder.

For Codespaces, after setup finishes:
  ./gradlew assembleDebug
