@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

if not exist "target\client-dist\Iniciar.bat" (
    echo [ERROR] No hay entrega cliente. Ejecute scripts\dev\build\package.bat primero.
    pause
    exit /b 1
)

call "target\client-dist\Iniciar.bat"
exit /b %ERRORLEVEL%
