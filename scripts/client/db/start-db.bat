@echo off
setlocal EnableExtensions
set "ROOT=%~dp0..\..\"
cd /d "%ROOT%db"

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no encontrado.
    pause
    exit /b 1
)

if not exist ".env" (
    if exist ".env.example" (
        copy /Y ".env.example" ".env" >nul
        echo Edite db\.env y cambie POSTGRES_PASSWORD.
    )
)

echo Levantando PostgreSQL...
docker compose up -d
if errorlevel 1 (
    echo [ERROR] No se pudo iniciar el contenedor.
    pause
    exit /b 1
)

docker compose ps
pause
endlocal
exit /b 0
