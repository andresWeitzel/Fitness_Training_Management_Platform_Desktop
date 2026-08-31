@echo off
setlocal EnableExtensions
cd /d "%~dp0"

title Fitness Training - Iniciando...
set "ERR=0"

echo.
echo  Fitness Training Management Platform
echo  Carpeta: %~dp0
echo.

if not exist "scripts\client\Iniciar.bat" (
    echo [ERROR] No se encuentra scripts\client\Iniciar.bat
    echo.
    echo  Ejecute Iniciar.bat desde la carpeta descomprimida de GitHub.
    echo.
    set "ERR=1"
    goto :fin
)

set "FT_SKIP_PAUSE=1"
call "scripts\client\Iniciar.bat"
set "ERR=%ERRORLEVEL%"

:fin
echo.
if %ERR% neq 0 (
    echo  Finalizado con errores ^(codigo %ERR%^).
) else (
    echo  Finalizado.
)
echo.
pause
endlocal
exit /b %ERR%
