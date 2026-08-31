@echo off
setlocal EnableExtensions EnableDelayedExpansion

title Fitness Training - Iniciando...
set "ERR=0"

echo.
echo  Fitness Training Management Platform
echo  Carpeta: %~dp0
echo.

set "ROOT="
set "CUR=%~dp0"
if "%CUR:~-1%"=="\" set "CUR=%CUR:~0,-1%"

for /L %%n in (1,1,10) do (
    if exist "!CUR!\app\FitnessTraining.bat" if exist "!CUR!\scripts\setup\setup-first-run.bat" (
        set "ROOT=!CUR!\"
        goto :have_root
    )
    if exist "!CUR!\target\client-dist\app\FitnessTraining.bat" (
        set "ROOT=!CUR!\target\client-dist\"
        goto :have_root
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
    if "!NEXT!"=="!CUR!" goto :bad_folder
    set "CUR=!NEXT!"
)

:bad_folder
echo [ERROR] No se reconoce la carpeta del proyecto.
echo         Descomprima el ZIP de GitHub y ejecute Iniciar.bat en la raiz.
set "ERR=1"
goto :fin

:repo_found
call "!REPO!scripts\client\setup\check-requirements.bat" build
if errorlevel 1 (
    set "ERR=1"
    goto :fin
)

echo [INFO] Compilando aplicacion ^(primera vez, puede tardar^)...
call "!REPO!scripts\dev\build\assemble-client.bat"
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion.
    set "ERR=1"
    goto :fin
)

if not exist "!REPO!target\client-dist\app\FitnessTraining.bat" (
    echo [ERROR] No se genero la aplicacion en target\client-dist\
    set "ERR=1"
    goto :fin
)
set "ROOT=!REPO!target\client-dist\"

:have_root
cd /d "!ROOT!"
echo [INFO] Carpeta de ejecucion: !ROOT!
echo.

call "!ROOT!scripts\setup\check-requirements.bat" run
if errorlevel 1 (
    set "ERR=1"
    goto :fin
)

if exist "!ROOT!scripts\setup\validate-package.bat" (
    call "!ROOT!scripts\setup\validate-package.bat"
    if errorlevel 1 (
        set "ERR=1"
        goto :fin
    )
)

call "!ROOT!scripts\setup\setup-first-run.bat"
if errorlevel 1 (
    set "ERR=1"
    goto :fin
)
echo [OK] Configuracion inicial.
echo.

where docker >nul 2>&1
if not errorlevel 1 (
    echo [INFO] Levantando PostgreSQL ^(Docker^)...
    call "!ROOT!scripts\db\start-db-silent.bat"
    echo.
)

echo [INFO] Abriendo aplicacion...
call "!ROOT!app\FitnessTraining.bat"
if errorlevel 1 set "ERR=1"

:fin
if not defined FT_SKIP_PAUSE (
    echo.
    if !ERR! neq 0 (
        echo  Finalizado con errores ^(codigo !ERR!^).
    ) else (
        echo  Finalizado.
    )
    echo.
    pause
)
exit /b !ERR!
