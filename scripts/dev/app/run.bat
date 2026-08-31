@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

echo Fitness Training - modo desarrollo
echo Compilando y ejecutando ultimos cambios...
echo.

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven no encontrado. Instale Maven.
    pause
    exit /b 1
)

call mvn -q clean compile javafx:run
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion o el inicio.
    pause
    exit /b 1
)
