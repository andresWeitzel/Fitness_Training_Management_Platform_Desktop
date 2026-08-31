@echo off
setlocal EnableExtensions EnableDelayedExpansion

if not where java >nul 2>&1 (
    for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
        if exist "%%d\bin\java.exe" set "PATH=%%d\bin;!PATH!"
    )
    for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jre-21*") do (
        if exist "%%d\bin\java.exe" set "PATH=%%d\bin;!PATH!"
    )
    for /d %%d in ("%LocalAppData%\Programs\Eclipse Adoptium\jdk-21*") do (
        if exist "%%d\bin\java.exe" set "PATH=%%d\bin;!PATH!"
    )
)

where java >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=*" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%v"
    echo %JAVA_VER% | findstr /r "21\." >nul
    if not errorlevel 1 (
        echo [OK] Java 21 detectado: %JAVA_VER%
        endlocal
        exit /b 0
    )
    echo [AVISO] Java encontrado pero se recomienda version 21:
    echo         %JAVA_VER%
    endlocal
    exit /b 0
)

echo [AVISO] Java no encontrado en el PATH.

where winget >nul 2>&1
if errorlevel 1 (
    echo [ERROR] winget no disponible. Instale Java 21 manualmente:
    echo         https://adoptium.net/
    echo         Luego vuelva a ejecutar Iniciar.bat
    endlocal
    exit /b 1
)

echo.
echo ¿Instalar Java 21 ^(Temurin JRE^) con winget?
set /p INSTALL_JAVA="Escriba S para instalar, N para cancelar: "
if /i not "%INSTALL_JAVA%"=="S" (
    echo [ERROR] Instalacion cancelada. Java 21 es requerido.
    endlocal
    exit /b 1
)

echo.
echo Instalando Java 21 con winget ^(puede tardar varios minutos^)...
winget install -e --id EclipseAdoptium.Temurin.21.JRE --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    echo [ERROR] No se pudo instalar Java automaticamente.
    echo         Instale manualmente desde https://adoptium.net/
    endlocal
    exit /b 1
)

echo.
echo [OK] Java instalado. Cierre esta ventana, abra una nueva y ejecute Iniciar.bat de nuevo.
echo      ^(El PATH se actualiza al abrir una ventana nueva^)
endlocal
exit /b 1
