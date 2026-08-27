@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === Preparar runtime portable (Java + PostgreSQL) ===
echo.

set "RUNTIME=%~dp0runtime"
set "JDK_DIR=%RUNTIME%\jdk"
set "PG_DIR=%RUNTIME%\postgresql"
set "CACHE=%RUNTIME%\_cache"

if not exist "%RUNTIME%" mkdir "%RUNTIME%"
if not exist "%CACHE%" mkdir "%CACHE%"

REM --- Java 21 JRE (Temurin / Adoptium) ---
if exist "%JDK_DIR%\bin\java.exe" (
    echo [OK] Java ya existe en runtime\jdk
) else (
    echo [1/2] Descargando Java 21 JRE ^(Temurin^)...
    set "JRE_ZIP=%CACHE%\temurin-jre21-win.zip"
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$ProgressPreference='SilentlyContinue';" ^
        "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk' -OutFile '%JRE_ZIP%' -UseBasicParsing"
    if errorlevel 1 (
        echo [ERROR] No se pudo descargar Java. Verifique internet.
        pause
        exit /b 1
    )
    echo Extrayendo Java...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$dest='%RUNTIME%\_jre_extract'; if (Test-Path $dest) { Remove-Item $dest -Recurse -Force };" ^
        "Expand-Archive -Path '%JRE_ZIP%' -DestinationPath $dest -Force;" ^
        "$folder = Get-ChildItem $dest -Directory | Select-Object -First 1;" ^
        "if (-not $folder) { throw 'Zip JRE vacio' };" ^
        "if (Test-Path '%JDK_DIR%') { Remove-Item '%JDK_DIR%' -Recurse -Force };" ^
        "Move-Item $folder.FullName '%JDK_DIR%'"
    if errorlevel 1 (
        echo [ERROR] Fallo al extraer Java.
        pause
        exit /b 1
    )
    if exist "%RUNTIME%\_jre_extract" rmdir /s /q "%RUNTIME%\_jre_extract"
    echo [OK] Java listo en runtime\jdk
)

REM --- PostgreSQL 16 binaries (Windows x64) ---
if exist "%PG_DIR%\bin\pg_ctl.exe" (
    echo [OK] PostgreSQL ya existe en runtime\postgresql
) else (
    echo [2/2] Descargando PostgreSQL 16 binaries...
    set "PG_ZIP=%CACHE%\postgresql-16-binaries-win.zip"
    REM URL EDB - version 16.4; si falla, ver install\ENTREGA-PORTABLE.md
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$ProgressPreference='SilentlyContinue';" ^
        "Invoke-WebRequest -Uri 'https://get.enterprisedb.com/postgresql/postgresql-16.4-1-windows-x64-binaries.zip' -OutFile '%PG_ZIP%' -UseBasicParsing"
    if errorlevel 1 (
        echo [ERROR] No se pudo descargar PostgreSQL.
        echo Descargue manualmente desde:
        echo   https://www.enterprisedb.com/download-postgresql-binaries
        echo y extraiga en runtime\postgresql
        pause
        exit /b 1
    )
    echo Extrayendo PostgreSQL...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "$dest='%RUNTIME%\_pg_extract'; if (Test-Path $dest) { Remove-Item $dest -Recurse -Force };" ^
        "Expand-Archive -Path '%PG_ZIP%' -DestinationPath $dest -Force;" ^
        "$pgsql = Join-Path $dest 'pgsql'; if (-not (Test-Path $pgsql)) { $pgsql = Get-ChildItem $dest -Directory | Select-Object -First 1 | ForEach-Object { $_.FullName } };" ^
        "if (-not (Test-Path $pgsql)) { throw 'No se encontro carpeta pgsql' };" ^
        "if (Test-Path '%PG_DIR%') { Remove-Item '%PG_DIR%' -Recurse -Force };" ^
        "Move-Item $pgsql '%PG_DIR%'"
    if errorlevel 1 (
        echo [ERROR] Fallo al extraer PostgreSQL.
        pause
        exit /b 1
    )
    if exist "%RUNTIME%\_pg_extract" rmdir /s /q "%RUNTIME%\_pg_extract"
    echo [OK] PostgreSQL listo en runtime\postgresql
)

echo.
echo === Runtime portable listo ===
echo   runtime\jdk\bin\java.exe
echo   runtime\postgresql\bin\pg_ctl.exe
echo.
echo Siguiente paso: package-full.bat o package.bat
echo.
exit /b 0
