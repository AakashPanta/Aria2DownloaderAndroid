@ECHO OFF
REM Lightweight Gradle launcher for browser/Codespaces workflow.
WHERE gradle >NUL 2>&1
IF %ERRORLEVEL% EQU 0 (
  gradle %*
  EXIT /B %ERRORLEVEL%
)

IF EXIST "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  java -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
  EXIT /B %ERRORLEVEL%
)

ECHO ERROR: Gradle is not installed and gradle-wrapper.jar is missing.
ECHO For Codespaces, let the devcontainer finish setup, then run again.
EXIT /B 1
