@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title Fitness Training - Iniciando...

echo.
echo  Fitness Training Management Platform
echo  ------------------------------------
echo.

call "%~dp0configurar-primera-vez.bat"

if exist "%~dp0runtime\postgresql\bin\pg_ctl.exe" (
    echo [1/2] Base de datos ^(PostgreSQL portable^)...
    call "%~dp0scripts\start-db-portable-silent.bat"
    if errorlevel 1 (
        echo [AVISO] No se pudo levantar PostgreSQL portable.
        echo.
    )
) else (
    where docker >nul 2>&1
    if not errorlevel 1 (
        echo [1/2] Base de datos ^(Docker^)...
        call "%~dp0scripts\start-db-silent.bat"
    ) else (
        echo [1/2] Sin PostgreSQL portable ni Docker.
        echo         Opciones: copiar PG en runtime\postgresql ^(ver install\ENTREGA-PORTABLE.md^)
        echo                   instalar Docker Desktop
        echo                   o PostgreSQL 16 como servicio Windows
        echo.
    )
)

echo [2/2] Abriendo aplicacion...
echo.
call "%~dp0app\FitnessTraining.bat"

endlocal
