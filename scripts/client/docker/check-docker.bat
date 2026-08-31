@echo off
REM Solo informa estado de Docker. No instala automaticamente.
where docker >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Docker no encontrado.
    echo         https://www.docker.com/products/docker-desktop/
    exit /b 0
)
docker info >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Docker instalado pero no en ejecucion. Abra Docker Desktop.
    exit /b 0
)
echo [OK] Docker listo.
exit /b 0
