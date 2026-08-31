@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

if not exist "target\client-dist\app\FitnessTraining.bat" (
    echo [ERROR] Ejecute scripts\dev\build\package.bat primero.
    pause
    exit /b 1
)

call "target\client-dist\app\FitnessTraining.bat"
