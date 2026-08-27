@echo off
setlocal EnableExtensions

set "DIST=%~dp0.."

if exist "%DIST%\runtime\postgresql\bin\pg_ctl.exe" (
    call "%~dp0stop-db-portable.bat"
    exit /b 0
)

cd /d "%DIST%\db"
echo Deteniendo PostgreSQL ^(Docker^)...
docker compose stop
docker compose ps
pause
