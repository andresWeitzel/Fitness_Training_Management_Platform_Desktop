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
copy /Y "install\client-dist\Iniciar.bat" "%DIST%\"
copy /Y "install\client-dist\configurar-primera-vez.bat" "%DIST%\"
copy /Y "install\client-dist\LEEME.txt" "%DIST%\"
copy /Y "install\client-dist\scripts\start-db.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\start-db-silent.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\stop-db.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\backup-db.bat" "%DIST%\scripts\"
copy /Y "install\CLIENTE.md" "%DIST%\docs\"
copy /Y "install\CONFIGURAR-JAVA-POSTGRES.md" "%DIST%\docs\"

set "ZIP=target\FitnessTraining-client-win64.zip"
if exist "%ZIP%" del /f /q "%ZIP%"

powershell -NoProfile -Command "Compress-Archive -Path '%DIST%\*' -DestinationPath '%ZIP%' -Force"
if errorlevel 1 (
    echo [AVISO] No se pudo crear el zip. Entregue la carpeta %DIST%
) else (
    echo Zip listo: %ZIP%
)

echo.
echo === Entrega al cliente ===
echo   Carpeta: %DIST%
echo   Zip:     %ZIP%
echo.
echo   Requisitos en la PC del cliente: Java 21 + PostgreSQL 16
echo   Docker opcional ^(si esta instalado, Iniciar.bat lo usa^)
echo.
echo   Iniciar.bat              - abre la app ^(+ Docker si hay^)
echo   app\FitnessTraining.bat  - solo aplicacion
echo   docs\CLIENTE.md          - guia de instalacion
echo.
pause
exit /b 0
