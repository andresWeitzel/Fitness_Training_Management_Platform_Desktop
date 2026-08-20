@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

if not exist "target\client\FitnessTraining.bat" (
    echo [ERROR] No hay build empaquetado. Ejecute package.bat primero.
    pause
    exit /b 1
)

echo Iniciando aplicacion empaquetada ^(target\client^)...
call "target\client\FitnessTraining.bat"
