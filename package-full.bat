@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo  ============================================
echo   FITNESS TRAINING - Paquete completo cliente
echo  ============================================
echo.
echo  Esto genera el ZIP para entregar al gimnasio.
echo  (Java + PostgreSQL + app, sin instalar nada alli)
echo.

call "%~dp0prepare-runtime.bat"
if errorlevel 1 exit /b 1

echo.
echo Compilando y empaquetando...
call "%~dp0package.bat"
exit /b %ERRORLEVEL%
