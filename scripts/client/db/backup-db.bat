@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "ROOT=%~dp0..\..\"
set "BACKUP_DIR=%ROOT%backups"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=1-3 delims=/ " %%a in ("%date%") do set "STAMP=%%c%%b%%a"
for /f "tokens=1-2 delims=:." %%a in ("%time%") do set "STAMP=!STAMP!_%%a%%b"
set "STAMP=!STAMP: =0!"

set "DB=fitness_training"
set "USER=postgres"
set "PG_PORT=5432"
set "ENV_FILE=%ROOT%db\.env"

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

where docker >nul 2>&1
if not errorlevel 1 (
    cd /d "%ROOT%db"
    echo Backup Docker de %DB%...
    docker compose exec -T postgres pg_dump -U %USER% %DB% > "%FILE%"
    if errorlevel 1 goto :fail
    echo Listo: %FILE%
    pause
    exit /b 0
)

where pg_dump >nul 2>&1
if not errorlevel 1 (
    pg_dump -h localhost -p %PG_PORT% -U %USER% %DB% > "%FILE%"
    if errorlevel 1 goto :fail
    echo Listo: %FILE%
    pause
    exit /b 0
)

echo [ERROR] Necesita Docker en ejecucion o pg_dump en PATH.
pause
exit /b 1

:fail
echo [ERROR] Fallo el backup.
pause
exit /b 1
