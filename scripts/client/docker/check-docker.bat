@echo off
setlocal EnableExtensions

where docker >nul 2>&1
if not errorlevel 1 (
    echo [OK] Docker ya esta en PATH.
    endlocal
    exit /b 0
)

echo [INFO] Docker no detectado ^(PostgreSQL en Docker^).

where winget >nul 2>&1
if errorlevel 1 (
    echo [AVISO] winget no disponible. Instale Docker o PostgreSQL manualmente.
    endlocal
    exit /b 0
)

if /i not "%FT_AUTO_INSTALL%"=="1" (
    echo.
    echo ¿Instalar Docker Desktop con winget?
    set /p INSTALL_DOCKER="Escriba S para instalar, N para continuar sin Docker: "
    if /i not "%INSTALL_DOCKER%"=="S" (
        echo [INFO] Continuando sin Docker.
        endlocal
        exit /b 0
    )
)

echo.
echo Instalando Docker Desktop con winget ^(puede tardar varios minutos^)...
winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo [AVISO] No se pudo instalar Docker. Use PostgreSQL nativo o instale Docker manualmente.
    endlocal
    exit /b 0
)

echo [OK] Docker instalado. Si no inicia, abra Docker Desktop desde el menu Inicio.
endlocal
exit /b 0
