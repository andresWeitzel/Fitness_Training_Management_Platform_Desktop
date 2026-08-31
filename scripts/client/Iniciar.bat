@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title Fitness Training - Iniciando...

echo.
echo  ============================================
echo   Fitness Training Management Platform
echo  ============================================
echo   Carpeta: %~dp0
echo.

if not exist "%~dp0app\FitnessTraining.bat" (
    echo [ERROR] No se encuentra app\FitnessTraining.bat
    echo.
    echo  Ejecute Iniciar.bat desde la carpeta del zip descomprimido
    echo  ^(Iniciar.bat, app\ y scripts\ en el mismo nivel^).
    echo.
    echo  Desarrollo: scripts\dev\client\start-client-dist.bat
    echo  Empaquetar: scripts\dev\build\package.bat
    goto :fin_error
)

if not exist "%~dp0scripts\setup\setup-first-run.bat" (
    echo [ERROR] No se encuentra scripts\setup\setup-first-run.bat
    echo  La carpeta scripts\ esta incompleta o corrupta.
    goto :fin_error
)

if not exist "%~dp0scripts\java\check-java.bat" (
    echo [ERROR] No se encuentra scripts\java\check-java.bat
    goto :fin_error
)

echo [Setup] Configuracion inicial...
call "%~dp0scripts\setup\setup-first-run.bat"
if errorlevel 1 (
    echo [ERROR] Fallo la configuracion inicial.
    goto :fin_error
)
echo [OK] Configuracion inicial.
echo.

echo [1/4] Verificando Java 21...
call "%~dp0scripts\java\check-java.bat"
if errorlevel 1 goto :fin_error
echo [OK] Java verificado.
echo.

echo [2/4] Verificando Docker ^(opcional^)...
where docker >nul 2>&1
if errorlevel 1 (
    echo [INFO] Docker no detectado en PATH.
    if exist "%~dp0scripts\docker\check-docker.bat" (
        call "%~dp0scripts\docker\check-docker.bat"
        REM Continuar aunque no se instale Docker (PostgreSQL nativo).
    )
) else (
    echo [OK] Docker detectado en PATH.
)
echo.

where docker >nul 2>&1
if not errorlevel 1 (
    echo [3/4] Levantando PostgreSQL ^(Docker^)...
    call "%~dp0scripts\db\start-db-silent.bat"
    if errorlevel 1 (
        echo [AVISO] No se levanto la base en Docker.
        echo         Use PostgreSQL del sistema o inicie Docker Desktop y reintente.
        echo.
    ) else (
        echo [OK] Base de datos Docker lista.
        echo.
    )
) else (
    echo [3/4] Sin Docker - PostgreSQL del sistema debe estar activo.
    echo.
)

echo [4/4] Abriendo aplicacion ^(Flyway migra al conectar^)...
echo.
call "%~dp0app\FitnessTraining.bat"
set "APP_ERR=%ERRORLEVEL%"

echo.
if %APP_ERR% neq 0 (
    echo [ERROR] La aplicacion termino con error ^(codigo %APP_ERR%^).
    goto :fin_error
)

echo  ============================================
echo   Proceso completado. La aplicacion se cerro.
echo  ============================================
echo.
pause
endlocal
exit /b 0

:fin_error
echo.
echo  ============================================
echo   El inicio no se completo. Revise los mensajes.
echo  ============================================
echo.
pause
endlocal
exit /b 1
