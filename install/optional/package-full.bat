@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo  [OPCIONAL] Paquete con Java y PostgreSQL embebidos
echo  La entrega normal usa package.bat en la raiz del proyecto.
echo.

call "%~dp0prepare-runtime.bat"
if errorlevel 1 exit /b 1

echo.
echo Copiando runtime al paquete cliente...
call "%~dp0..\..\package.bat"
exit /b %ERRORLEVEL%
