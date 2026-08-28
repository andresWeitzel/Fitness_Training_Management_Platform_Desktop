@echo off
setlocal EnableExtensions
set "APP_DIR=%~dp0"
cd /d "%APP_DIR%"

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 21 no encontrado en el PATH.
    echo Instale JDK o JRE 21: https://adoptium.net/
    pause
    exit /b 1
)

for /f "tokens=*" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%v"
echo %JAVA_VER% | findstr /r "21\." >nul
if errorlevel 1 (
    echo [AVISO] Se recomienda Java 21. Verifique: java -version
)

set "JAR="
for %%f in ("%APP_DIR%fitness-training-desktop-*.jar") do set "JAR=%%f"
if not defined JAR (
    echo [ERROR] No se encontro fitness-training-desktop-*.jar en %APP_DIR%
    pause
    exit /b 1
)

set "MODULE_PATH=%APP_DIR%lib\javafx-base-win.jar;%APP_DIR%lib\javafx-graphics-win.jar;%APP_DIR%lib\javafx-controls-win.jar;%APP_DIR%lib\javafx-fxml-win.jar"

java --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml ^
    -cp "%JAR%;%APP_DIR%lib\*" com.fitnesstraining.FitnessApp

if errorlevel 1 pause
