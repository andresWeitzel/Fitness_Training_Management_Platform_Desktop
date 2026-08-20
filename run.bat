@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo Fitness Training - modo desarrollo
echo Compilando y ejecutando ultimos cambios...
echo.

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven no encontrado. Instale Maven o use target\client\FitnessTraining.bat
    pause
    exit /b 1
)

call mvn -q compile javafx:run
if errorlevel 1 (
    echo.
    echo [ERROR] Fallo la compilacion o el inicio.
    pause
    exit /b 1
)
