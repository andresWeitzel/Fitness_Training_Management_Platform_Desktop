@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title Fitness Training - Iniciando...

echo.
echo  Fitness Training Management Platform
echo  ------------------------------------
echo.

call "%~dp0configurar-primera-vez.bat"

where docker >nul 2>&1
if not errorlevel 1 (
    echo [1/2] Docker detectado - levantando PostgreSQL...
    call "%~dp0scripts\start-db-silent.bat"
) else (
    echo [1/2] Sin Docker - PostgreSQL del sistema ^(servicio Windows^).
    echo       Verifique que PostgreSQL este en ejecucion en esta PC
    echo       o configure servidor remoto en la app ^(ver docs\CLIENTE.md^).
    echo.
)

echo [2/2] Abriendo aplicacion...
echo.
call "%~dp0app\FitnessTraining.bat"

endlocal
