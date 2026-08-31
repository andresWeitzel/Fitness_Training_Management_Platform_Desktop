@echo off
setlocal EnableExtensions
set "ROOT=%~dp0..\..\"
set "DB_DIR=%ROOT%db"

if not exist "%DB_DIR%" (
    echo [ERROR] No existe la carpeta db\
    endlocal
    exit /b 1
)

cd /d "%DB_DIR%"

where docker >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Docker no encontrado en PATH.
    endlocal
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Docker instalado pero Docker Desktop no esta en ejecucion.
    echo         Abra Docker Desktop y espere a que el engine este listo.
    endlocal
    exit /b 1
)

netstat -ano 2>nul | findstr /r /c:":5432 .*LISTENING" >nul
if not errorlevel 1 (
    echo [INFO] Puerto 5432 ya en uso. Si falla el contenedor, detenga otro PostgreSQL.
)

if not exist "docker-compose.yml" (
    echo [ERROR] No se encuentra docker-compose.yml en db\
    endlocal
    exit /b 1
)

if not exist ".env" (
    if exist ".env.example" (
        copy /Y ".env.example" ".env" >nul
        echo [INFO] Creado db\.env desde plantilla. Revise POSTGRES_PASSWORD.
    ) else (
        echo [ERROR] Falta db\.env y db\.env.example
        endlocal
        exit /b 1
    )
)

echo Levantando PostgreSQL con docker compose...
docker compose up -d
if errorlevel 1 (
    echo [ERROR] docker compose up -d fallo.
    endlocal
    exit /b 1
)

docker compose ps
endlocal
exit /b 0
