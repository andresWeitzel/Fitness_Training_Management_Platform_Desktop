@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

set "BACKUP_DIR=%~dp0..\backups"
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=1-3 delims=/ " %%a in ("%date%") do set "STAMP=%%c%%b%%a"
for /f "tokens=1-2 delims=:." %%a in ("%time%") do set "STAMP=%STAMP%_%%a%%b"
set "STAMP=%STAMP: =0%"

set "DB=%POSTGRES_DB%"
if not defined DB set "DB=fitness_training"
set "USER=%POSTGRES_USER%"
if not defined USER set "USER=postgres"

if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        if "%%a"=="POSTGRES_DB" set "DB=%%b"
        if "%%a"=="POSTGRES_USER" set "USER=%%b"
    )
)

set "FILE=%BACKUP_DIR%\%DB%_%STAMP%.sql"

echo Backup de %DB% en %FILE% ...
docker compose exec -T postgres pg_dump -U %USER% %DB% > "%FILE%"
if errorlevel 1 (
    echo [ERROR] Fallo el backup. ¿Esta levantada la base? scripts\start-db.bat
    pause
    exit /b 1
)

echo Listo: %FILE%
pause
