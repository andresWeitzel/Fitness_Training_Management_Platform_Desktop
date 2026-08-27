@echo off
setlocal EnableExtensions
set "APP_DIR=%~dp0"
set "DIST_ROOT=%APP_DIR%..\"
cd /d "%APP_DIR%"

set "JAVA_EXE=java"
if exist "%DIST_ROOT%runtime\jdk\bin\java.exe" (
    set "JAVA_EXE=%DIST_ROOT%runtime\jdk\bin\java.exe"
) else if exist "%APP_DIR%runtime\jdk\bin\java.exe" (
    set "JAVA_EXE=%APP_DIR%runtime\jdk\bin\java.exe"
)

"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 21 no encontrado.
    echo.
    echo Opciones:
    echo   1. Instalar JDK 21 en Windows ^(java en PATH^)
    echo   2. Copiar un JRE 21 en: runtime\jdk\  ^(ver install\RUNTIME.md^)
  echo   3. Usar un instalador .exe futuro con Java incluido
    pause
    exit /b 1
)

for /f "tokens=*" %%v in ('"%JAVA_EXE%" -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%v"
echo %JAVA_VER% | findstr /r "21\." >nul
if errorlevel 1 (
    echo [AVISO] Se recomienda Java 21. Verifique: "%JAVA_EXE%" -version
)

set "JAR="
for %%f in ("%APP_DIR%fitness-training-desktop-*.jar") do set "JAR=%%f"
if not defined JAR (
    echo [ERROR] No se encontro fitness-training-desktop-*.jar en %APP_DIR%
    pause
    exit /b 1
)

set "MODULE_PATH=%APP_DIR%lib\javafx-base-win.jar;%APP_DIR%lib\javafx-graphics-win.jar;%APP_DIR%lib\javafx-controls-win.jar;%APP_DIR%lib\javafx-fxml-win.jar"

"%JAVA_EXE%" --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml ^
    -cp "%JAR%;%APP_DIR%lib\*" com.fitnesstraining.FitnessApp

if errorlevel 1 pause
