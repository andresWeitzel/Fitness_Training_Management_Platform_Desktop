@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

if exist "target\client\FitnessTraining.bat" (
    echo Iniciando aplicacion empaquetada...
    call "target\client\FitnessTraining.bat"
    exit /b %errorlevel%
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] No hay build empaquetado ni Maven.
    echo Ejecute package.bat primero, o instale Maven para desarrollo.
    pause
    exit /b 1
)

echo Modo desarrollo ^(Maven^)...
call mvn -q javafx:run
