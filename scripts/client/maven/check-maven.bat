@echo off
setlocal EnableExtensions EnableDelayedExpansion

if not where mvn >nul 2>&1 (
    for /d %%d in ("%ProgramFiles%\Apache\maven\*") do (
        if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
    )
    for /d %%d in ("%ProgramFiles%\Maven\*") do (
        if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
    )
)

where mvn >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=*" %%v in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do echo [OK] %%v
    goto :ok
)

echo [INFO] Maven no encontrado. Instalando con winget...

where winget >nul 2>&1
if errorlevel 1 (
    echo [ERROR] winget no disponible. Instale Maven: https://maven.apache.org/
    exit /b 1
)

echo Instalando Maven ^(puede tardar varios minutos^)...
winget install -e --id Apache.Maven --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo [ERROR] No se pudo instalar Maven.
    exit /b 1
)

for /d %%d in ("%ProgramFiles%\Apache\maven\*") do set "PATH=%%d\bin;!PATH!"
for /d %%d in ("%ProgramFiles%\Maven\*") do set "PATH=%%d\bin;!PATH!"

where mvn >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Maven instalado. Cierre esta ventana, abra una nueva y ejecute Iniciar.bat.
    exit /b 1
)

echo [OK] Maven instalado.

:ok
for /f "delims=" %%P in ("!PATH!") do (
    endlocal
    set "PATH=%%P"
    exit /b 0
)
endlocal
exit /b 0
