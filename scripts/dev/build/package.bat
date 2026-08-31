@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

echo === Fitness Training - Empaquetado para cliente ===
echo.

call "%~dp0assemble-client.bat"
if errorlevel 1 (
    echo [ERROR] Fallo el empaquetado.
    pause
    exit /b 1
)

set "DIST=target\client-dist"
set "ZIP=target\FitnessTraining.zip"
if exist "%ZIP%" del /f /q "%ZIP%"

powershell -NoProfile -Command "Compress-Archive -Path '%DIST%\*' -DestinationPath '%ZIP%' -Force"
if errorlevel 1 (
    echo [AVISO] No se pudo crear el zip. Entregue la carpeta %DIST%
) else (
    echo Zip listo: %ZIP%
)

echo.
echo   Carpeta: %DIST%
echo   Zip:     %ZIP%
echo.
pause
exit /b 0
