@echo off
setlocal EnableExtensions EnableDelayedExpansion

title Fitness Training
set "FT_AUTO_INSTALL=1"

set "ROOT="
set "CUR=%~dp0"
if "%CUR:~-1%"=="\" set "CUR=%CUR:~0,-1%"

for /L %%n in (1,1,10) do (
    if exist "!CUR!\app\FitnessTraining.bat" if exist "!CUR!\scripts\setup\setup-first-run.bat" (
        set "ROOT=!CUR!\"
        goto :root_ok
    )
    if exist "!CUR!\target\client-dist\app\FitnessTraining.bat" (
        set "ROOT=!CUR!\target\client-dist\"
        goto :root_ok
    )
    for %%p in ("!CUR!\..") do set "NEXT=%%~fp"
    if "!NEXT!"=="!CUR!" goto :need_build
    set "CUR=!NEXT!"
)

:need_build
set "CUR=%~dp0"
if "%CUR:~-1%"=="\" set "CUR=%CUR:~0,-1%"
set "REPO="

for /L %%n in (1,1,10) do (
    if exist "!CUR!\pom.xml" (
        set "REPO=!CUR!\"
        goto :repo_found
    )
    for %%p in ("!CUR!\..") do set "NEXT=%%~fp"
    if "!NEXT!"=="!CUR!" goto :show_help
    set "CUR=!NEXT!"
)

:repo_found
echo [INFO] Primera ejecucion desde GitHub: instalando herramientas si faltan...
echo.

call "!REPO!scripts\client\java\check-java.bat"
if errorlevel 1 goto :show_help

call "!REPO!scripts\client\maven\check-maven.bat"
if errorlevel 1 goto :show_help

echo [INFO] Compilando aplicacion...
call "!REPO!scripts\dev\build\assemble-client.bat"
if errorlevel 1 goto :show_help

if exist "!REPO!target\client-dist\app\FitnessTraining.bat" (
    set "ROOT=!REPO!target\client-dist\"
    goto :root_ok
)
goto :show_help

:root_ok
cd /d "!ROOT!"

:deliver
echo  ============================================
echo   Fitness Training Management Platform
echo  ============================================
echo.

call "%ROOT%scripts\setup\validate-package.bat"
if errorlevel 1 goto :fail

call "%ROOT%scripts\setup\setup-first-run.bat"
if errorlevel 1 goto :fail
echo [OK] Configuracion inicial.
echo.

echo [1/5] Verificando Java 21...
call "%ROOT%scripts\java\check-java.bat"
if errorlevel 1 goto :fail
echo.

echo [2/5] Verificando Docker ^(PostgreSQL^)...
if exist "%ROOT%scripts\docker\check-docker.bat" call "%ROOT%scripts\docker\check-docker.bat"
echo.

where docker >nul 2>&1
if not errorlevel 1 (
    echo [3/5] Levantando PostgreSQL ^(Docker^)...
    call "%ROOT%scripts\db\start-db-silent.bat"
    if errorlevel 1 (
        echo [AVISO] Base no levantada. Abra Docker Desktop y vuelva a ejecutar Iniciar.bat.
    ) else (
        echo [OK] Base de datos lista.
    )
    echo.
) else (
    echo [3/5] Sin Docker - PostgreSQL del sistema debe estar activo.
    echo.
)

echo [4/5] Abriendo aplicacion...
call "%ROOT%app\FitnessTraining.bat"
if errorlevel 1 goto :fail

echo.
echo  Proceso completado.
pause
exit /b 0

:show_help
echo.
echo  No se pudo preparar la aplicacion.
echo  Verifique conexion a internet ^(winget^) y permisos de instalacion.
echo  Luego ejecute Iniciar.bat de nuevo.
echo.
pause
exit /b 1

:fail
echo.
echo  El inicio no se completo. Revise los mensajes arriba.
pause
exit /b 1
