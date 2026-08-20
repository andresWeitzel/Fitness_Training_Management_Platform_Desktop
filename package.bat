@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === Fitness Training - Empaquetado para cliente ===
echo.

call mvn -q -Pclient-package clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Fallo el build Maven.
    pause
    exit /b 1
)

set "DIST=target\client-dist"
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%\app"
mkdir "%DIST%\db"
mkdir "%DIST%\scripts"
mkdir "%DIST%\docs"

xcopy /E /I /Y "target\client\*" "%DIST%\app\"
copy /Y "docker-compose.yml" "%DIST%\db\"
copy /Y ".env.example" "%DIST%\db\"
copy /Y "scripts\start-db.bat" "%DIST%\scripts\"
copy /Y "scripts\stop-db.bat" "%DIST%\scripts\"
copy /Y "scripts\backup-db.bat" "%DIST%\scripts\"
copy /Y "install\CLIENTE.md" "%DIST%\docs\"

echo.
echo Carpeta lista para entregar: %DIST%
echo   app\FitnessTraining.bat  - aplicacion
echo   scripts\start-db.bat     - base Docker
echo   docs\CLIENTE.md          - guia de instalacion
echo.

where jpackage >nul 2>&1
if errorlevel 1 (
    echo jpackage no disponible. Entregue la carpeta client-dist ^(incluye app portable^).
    pause
    exit /b 0
)

echo.
echo Opcional: jpackage para instalador nativo ^(experimental^).
echo Por ahora se recomienda entregar target\client-dist como zip.
echo.
pause
exit /b 0
