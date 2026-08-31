@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no encontrado.
    pause
    exit /b 1
)

if not exist ".env" (
    if exist ".env.example" copy /Y ".env.example" ".env" >nul
)

echo Levantando PostgreSQL...
docker compose up -d
docker compose ps
pause
