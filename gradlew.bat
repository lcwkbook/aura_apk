@echo off
setlocal
set APP_HOME=%~dp0
set GRADLE_VERSION=8.9
set DIST_NAME=gradle-%GRADLE_VERSION%-bin
set DIST_URL=https://services.gradle.org/distributions/%DIST_NAME%.zip
set CACHE_DIR=%USERPROFILE%\.gradle\wrapper\dists\%DIST_NAME%\manual
set GRADLE_HOME=%CACHE_DIR%\gradle-%GRADLE_VERSION%
set ZIP_FILE=%CACHE_DIR%\%DIST_NAME%.zip

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
  if not exist "%ZIP_FILE%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_FILE%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%CACHE_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

cd /d "%APP_HOME%"
"%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
