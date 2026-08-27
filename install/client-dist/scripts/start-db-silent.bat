@echo off
setlocal EnableExtensions
cd /d "%~dp0..\db"

if not exist ".env" (
    if exist ".env.example" copy /Y ".env.example" ".env" >nul
)

docker compose up -d 2>nul
if errorlevel 1 (
    echo [AVISO] No se pudo levantar Docker. Verifique Docker Desktop o use PostgreSQL nativo.
)
endlocal
exit /b 0
