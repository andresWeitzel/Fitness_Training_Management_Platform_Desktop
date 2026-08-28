@echo off
setlocal EnableExtensions
cd /d "%~dp0..\db"

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no encontrado.
    echo Use PostgreSQL instalado en Windows o instale Docker Desktop.
    echo Ver docs\CLIENTE.md
    pause
    exit /b 1
)

if not exist ".env" (
    if exist ".env.example" (
        echo [AVISO] Copiando .env.example a .env ...
        copy /Y ".env.example" ".env" >nul
        echo Cambie POSTGRES_PASSWORD en db\.env antes de produccion.
    )
)

echo Deteniendo PostgreSQL ^(Docker^)...
docker compose stop
docker compose ps
pause
