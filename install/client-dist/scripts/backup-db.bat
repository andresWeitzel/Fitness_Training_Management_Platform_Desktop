@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "DIST=%~dp0.."
set "BACKUP_DIR=%DIST%\backups"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=1-3 delims=/ " %%a in ("%date%") do set "STAMP=%%c%%b%%a"
for /f "tokens=1-2 delims=:." %%a in ("%time%") do set "STAMP=!STAMP!_%%a%%b"
set "STAMP=!STAMP: =0!"

set "DB=fitness_training"
set "USER=postgres"
set "PG_PORT=5432"
set "ENV_FILE=%DIST%\db\.env"

if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        set "key=%%a"
        set "val=%%b"
        if "!key!"=="POSTGRES_DB" set "DB=!val!"
        if "!key!"=="POSTGRES_USER" set "USER=!val!"
        if "!key!"=="POSTGRES_PORT" set "PG_PORT=!val!"
    )
)

set "FILE=%BACKUP_DIR%\%DB%_%STAMP%.sql"

if exist "%DIST%\runtime\postgresql\bin\pg_dump.exe" (
    set "PGROOT=%DIST%\runtime\postgresql"
    echo Backup portable de %DB% en %FILE% ...
    set "PGPASSWORD="
    if exist "%ENV_FILE%" (
        for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
            if "%%a"=="POSTGRES_PASSWORD" set "PGPASSWORD=%%b"
        )
    )
    "%PGROOT%\bin\pg_dump.exe" -h localhost -p %PG_PORT% -U %USER% %DB% > "%FILE%"
    if errorlevel 1 goto :fail
    echo Listo: %FILE%
    pause
    exit /b 0
)

cd /d "%DIST%\db"
echo Backup Docker de %DB% en %FILE% ...
docker compose exec -T postgres pg_dump -U %USER% %DB% > "%FILE%"
if errorlevel 1 goto :fail
echo Listo: %FILE%
pause
exit /b 0

:fail
echo [ERROR] Fallo el backup. ¿Esta levantada la base? Ejecute Iniciar.bat
pause
exit /b 1
