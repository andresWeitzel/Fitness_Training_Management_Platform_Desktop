@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no encontrado. Instale Docker Desktop o use PostgreSQL nativo.
    echo Ver install\CLIENTE.md
    pause
    exit /b 1
)

if not exist ".env" (
    if exist ".env.example" (
        echo [AVISO] No hay .env. Copie .env.example a .env y cambie la contraseña.
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
echo PostgreSQL listo en localhost:%POSTGRES_PORT%
echo Si no definio .env, el puerto por defecto es 5432.
docker compose ps
echo.
pause
