@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

echo === [OPCIONAL] Preparar runtime portable (Java + PostgreSQL) ===
echo Este script NO es necesario para la entrega normal.
echo Ver README.md en esta carpeta.
echo.

set "RUNTIME=%~dp0runtime"
set "JDK_DIR=%RUNTIME%\jdk"
set "PG_DIR=%RUNTIME%\postgresql"
set "CACHE=%RUNTIME%\_cache"
set "JRE_ZIP=%CACHE%\temurin-jre21-win.zip"
set "PG_ZIP=%CACHE%\postgresql-16-binaries-win.zip"
set "JRE_EXTRACT=%RUNTIME%\_jre_extract"
set "PG_EXTRACT=%RUNTIME%\_pg_extract"

if not exist "%RUNTIME%" mkdir "%RUNTIME%"
if not exist "%CACHE%" mkdir "%CACHE%"

if exist "%JDK_DIR%\bin\java.exe" (
    echo [OK] Java ya existe en install\optional\runtime\jdk
    goto :postgres
)

echo [1/2] Descargando Java 21 JRE ^(Temurin^)...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk' -OutFile '%JRE_ZIP%' -UseBasicParsing"
if errorlevel 1 (
    echo [ERROR] No se pudo descargar Java.
    pause
    exit /b 1
)

echo Extrayendo Java...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$zip='%JRE_ZIP%'; $dest='%JRE_EXTRACT%'; $jdk='%JDK_DIR%';" ^
    "if (Test-Path $dest) { Remove-Item $dest -Recurse -Force };" ^
    "Expand-Archive -LiteralPath $zip -DestinationPath $dest -Force;" ^
    "$folder = Get-ChildItem -LiteralPath $dest -Directory | Select-Object -First 1;" ^
    "if (-not $folder) { throw 'Zip JRE vacio' };" ^
    "if (Test-Path $jdk) { Remove-Item $jdk -Recurse -Force };" ^
    "Move-Item -LiteralPath $folder.FullName -Destination $jdk"
if errorlevel 1 (
    echo [ERROR] Fallo al extraer Java.
    pause
    exit /b 1
)

if exist "%JRE_EXTRACT%" rmdir /s /q "%JRE_EXTRACT%"
echo [OK] Java listo

:postgres
if exist "%PG_DIR%\bin\pg_ctl.exe" (
    echo [OK] PostgreSQL ya existe en install\optional\runtime\postgresql
    goto :done
)

echo [2/2] Descargando PostgreSQL 16 binaries...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -Uri 'https://get.enterprisedb.com/postgresql/postgresql-16.4-1-windows-x64-binaries.zip' -OutFile '%PG_ZIP%' -UseBasicParsing"
if errorlevel 1 (
    echo [ERROR] No se pudo descargar PostgreSQL.
    pause
    exit /b 1
)

echo Extrayendo PostgreSQL...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$zip='%PG_ZIP%'; $dest='%PG_EXTRACT%'; $pg='%PG_DIR%';" ^
    "if (Test-Path $dest) { Remove-Item $dest -Recurse -Force };" ^
    "Expand-Archive -LiteralPath $zip -DestinationPath $dest -Force;" ^
    "$pgsql = Join-Path $dest 'pgsql';" ^
    "if (-not (Test-Path $pgsql)) { $pgsql = (Get-ChildItem -LiteralPath $dest -Directory | Select-Object -First 1).FullName };" ^
    "if (-not (Test-Path $pgsql)) { throw 'No se encontro carpeta pgsql' };" ^
    "if (Test-Path $pg) { Remove-Item $pg -Recurse -Force };" ^
    "Move-Item -LiteralPath $pgsql -Destination $pg"
if errorlevel 1 (
    echo [ERROR] Fallo al extraer PostgreSQL.
    pause
    exit /b 1
)

if exist "%PG_EXTRACT%" rmdir /s /q "%PG_EXTRACT%"
echo [OK] PostgreSQL listo

:done
echo.
echo Runtime portable en install\optional\runtime\
echo.
exit /b 0
