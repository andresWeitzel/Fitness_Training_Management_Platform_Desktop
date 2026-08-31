@echo off
setlocal EnableExtensions EnableDelayedExpansion

title Fitness Training - Iniciando...
set "FT_AUTO_INSTALL=1"
set "ERR=0"

echo.
echo  Iniciando Fitness Training...
echo  Carpeta script: %~dp0
echo.

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
echo [INFO] Primera ejecucion: instalando herramientas si faltan...
echo.

call "!REPO!scripts\client\java\check-java.bat"
if errorlevel 1 goto :show_help

call "!REPO!scripts\client\maven\check-maven.bat"
if errorlevel 1 goto :show_help

echo [INFO] Compilando aplicacion ^(puede tardar varios minutos^)...
call "!REPO!scripts\dev\build\assemble-client.bat"
if errorlevel 1 goto :show_help

if exist "!REPO!target\client-dist\app\FitnessTraining.bat" (
    set "ROOT=!REPO!target\client-dist\"
    goto :root_ok
)
goto :show_help

:root_ok
cd /d "!ROOT!"
echo [INFO] Usando carpeta: !ROOT!
echo.

:deliver
echo  ============================================
echo   Fitness Training Management Platform
echo  ============================================
echo.

if not exist "%ROOT%scripts\setup\validate-package.bat" (
    echo [ERROR] Paquete incompleto. Falta scripts\setup\validate-package.bat
    set "ERR=1"
    goto :fin
)

call "%ROOT%scripts\setup\validate-package.bat"
if errorlevel 1 (
    set "ERR=1"
    goto :fin
)

call "%ROOT%scripts\setup\setup-first-run.bat"
if errorlevel 1 (
    set "ERR=1"
    goto :fin
)
echo [OK] Configuracion inicial.
echo.

echo [1/5] Verificando Java 21...
call "%ROOT%scripts\java\check-java.bat"
if errorlevel 1 (
    set "ERR=1"
    goto :fin
)
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
if errorlevel 1 set "ERR=1"

goto :fin

:show_help
echo.
echo  No se pudo preparar la aplicacion.
echo.
echo  - Ejecute desde la carpeta descomprimida de GitHub
echo  - Verifique internet ^(winget instala Java, Maven y Docker^)
echo  - Acepte permisos de administrador si Windows los pide
echo  - Vuelva a ejecutar Iniciar.bat
echo.
set "ERR=1"
goto :fin

:fin
endlocal & set "ERR=%ERR%"
exit /b %ERR%
