@echo off
setlocal EnableExtensions
cd /d "%~dp0..\db"

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no encontrado. Instale Docker Desktop o use PostgreSQL nativo.
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

echo Levantando PostgreSQL...
docker compose up -d
if errorlevel 1 (
    echo [ERROR] No se pudo iniciar el contenedor.
    pause
    exit /b 1
)

echo.
docker compose ps
echo.
echo PostgreSQL listo en localhost ^(puerto segun db\.env^).
pause
