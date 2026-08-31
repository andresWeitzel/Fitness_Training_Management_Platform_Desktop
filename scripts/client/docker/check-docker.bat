@echo off
setlocal EnableExtensions

where docker >nul 2>&1
if not errorlevel 1 (
    echo [OK] Docker ya esta en PATH.
    endlocal
    exit /b 0
)

echo [INFO] Docker no detectado. Puede usar PostgreSQL instalado en Windows sin Docker.

where winget >nul 2>&1
if errorlevel 1 (
    echo [INFO] winget no disponible. Instale Docker manualmente o use PostgreSQL nativo.
    endlocal
    exit /b 0
)

echo.
echo ¿Instalar Docker Desktop con winget? ^(requiere permisos de administrador^)
set /p INSTALL_DOCKER="Escriba S para instalar, N para continuar sin Docker: "
if /i not "%INSTALL_DOCKER%"=="S" (
    echo [INFO] Continuando sin Docker.
    endlocal
    exit /b 0
)

echo.
echo Instalando Docker Desktop con winget ^(puede tardar varios minutos^)...
winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo [AVISO] No se pudo instalar Docker. Use PostgreSQL nativo o instale Docker manualmente.
    endlocal
    exit /b 0
)

echo.
echo [OK] Docker instalado. Inicie Docker Desktop desde el menu Inicio
echo      y vuelva a ejecutar Iniciar.bat.
endlocal
exit /b 0
