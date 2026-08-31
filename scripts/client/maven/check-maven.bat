@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM Buscar Maven en rutas tipicas de winget
if not where mvn >nul 2>&1 (
    for /d %%d in ("%ProgramFiles%\Apache\maven\*") do (
        if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
    )
    for /d %%d in ("%ProgramFiles%\Maven\*") do (
        if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
    )
)

where mvn >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=*" %%v in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do echo [OK] %%v
    for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 0
)

echo [INFO] Maven no encontrado en PATH.

where winget >nul 2>&1
if errorlevel 1 (
    echo [ERROR] winget no disponible. Instale Maven manualmente: https://maven.apache.org/
    endlocal
    exit /b 1
)

if /i not "%FT_AUTO_INSTALL%"=="1" (
    echo.
    echo ¿Instalar Maven con winget?
    set /p INSTALL_MVN="Escriba S para instalar, N para cancelar: "
    if /i not "!INSTALL_MVN!"=="S" (
        echo [ERROR] Maven es requerido para compilar desde GitHub.
        endlocal
        exit /b 1
    )
)

echo.
echo Instalando Maven con winget ^(puede tardar varios minutos^)...
winget install -e --id Apache.Maven --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo [ERROR] No se pudo instalar Maven automaticamente.
    endlocal
    exit /b 1
)

REM Reintentar PATH
for /d %%d in ("%ProgramFiles%\Apache\maven\*") do (
    if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
)
for /d %%d in ("%ProgramFiles%\Maven\*") do (
    if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Maven instalado. Cierre esta ventana, abra una nueva y ejecute Iniciar.bat de nuevo.
    endlocal
    exit /b 1
)

echo [OK] Maven listo.
for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 0
