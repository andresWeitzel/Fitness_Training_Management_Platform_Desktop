@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0..\..\"
cd /d "%ROOT%"
set "ROOT=%CD%\"

set "ERR=0"
set "WARN=0"

echo [Validacion] Revisando paquete en %ROOT%
echo.

REM --- JAR principal ---
set "HAS_JAR=0"
if exist "%ROOT%app\FitnessTraining.jar" set "HAS_JAR=1"
for %%f in ("%ROOT%app\fitness-training-desktop-*.jar") do set "HAS_JAR=1"
if "!HAS_JAR!"=="0" (
    echo [ERROR] Falta el JAR en app\ ^(FitnessTraining.jar^)
    set "ERR=1"
)

REM --- JavaFX (minimo para arrancar) ---
for %%j in (javafx-base-win.jar javafx-graphics-win.jar javafx-controls-win.jar javafx-fxml-win.jar) do (
    if not exist "%ROOT%app\lib\%%j" (
        echo [ERROR] Falta app\lib\%%j - paquete incompleto
        set "ERR=1"
    )
)

REM --- Scripts requeridos ---
for %%s in (
    setup\setup-first-run.bat
    setup\validate-package.bat
    java\check-java.bat
    docker\check-docker.bat
    db\start-db-silent.bat
    db\start-db.bat
) do (
    if not exist "%ROOT%scripts\%%s" (
        echo [ERROR] Falta scripts\%%s
        set "ERR=1"
    )
)

if not exist "%ROOT%app\FitnessTraining.bat" (
    echo [ERROR] Falta app\FitnessTraining.bat
    set "ERR=1"
)

REM --- Docker ^(solo aviso^) ---
if not exist "%ROOT%db\docker-compose.yml" (
    echo [AVISO] Falta db\docker-compose.yml - no se podra usar Docker
    set "WARN=1"
)
if not exist "%ROOT%db\.env.example" (
    echo [AVISO] Falta db\.env.example - revise carpeta db\
    set "WARN=1"
)

REM --- Ruta larga ---
if not "%ROOT:~200%"=="" (
    echo [AVISO] Ruta larga ^(mas de 200 caracteres^). Si hay errores, use C:\FitnessTraining
    set "WARN=1"
)

REM --- Carpetas sincronizadas en la nube ---
echo %ROOT% | findstr /i "OneDrive Dropbox Google Drive iCloud" >nul
if not errorlevel 1 (
    echo [AVISO] Carpeta en sincronizacion cloud - puede causar lentitud o bloqueos
    set "WARN=1"
)

REM --- Permisos de escritura (config local) ---
set "TEST_DIR=%USERPROFILE%\.fitness-training"
if not exist "%TEST_DIR%" mkdir "%TEST_DIR%" 2>nul
echo test> "%TEST_DIR%\.write-test" 2>nul
if exist "%TEST_DIR%\.write-test" (
    del "%TEST_DIR%\.write-test" >nul 2>&1
) else (
    echo [ERROR] No se puede escribir en %TEST_DIR%
    set "ERR=1"
)

REM --- Espacio en disco ^(aviso si ^< 500 MB^) ---
set "DRIVE_LET=%ROOT:~0,1%"
for /f %%F in ('powershell -NoProfile -Command "try{(Get-PSDrive %DRIVE_LET%).Free}catch{999999999999}"') do set "FREE_BYTES=%%F"
if defined FREE_BYTES if !FREE_BYTES! LSS 524288000 (
    echo [AVISO] Poco espacio libre en disco ^(^< 500 MB^)
    set "WARN=1"
)

REM --- Version de Windows ---
ver | findstr /i "10\. 11\." >nul
if errorlevel 1 (
    echo [AVISO] Se recomienda Windows 10 u 11
    set "WARN=1"
)

echo.
if "!ERR!"=="1" (
    echo [ERROR] Paquete incompleto o corrupto. Descomprima de nuevo FitnessTraining.zip
    echo         en una ruta corta ^(ej. C:\FitnessTraining^).
    endlocal
    exit /b 1
)

if "!WARN!"=="1" (
    echo [OK] Paquete valido con advertencias ^(se puede continuar^).
) else (
    echo [OK] Paquete valido.
)
echo.
endlocal
exit /b 0
