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
        for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 0
    )
    echo [AVISO] Java encontrado pero se recomienda version 21:
    echo         %JAVA_VER%
    for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 0
)

echo [INFO] Java no encontrado en PATH.

where winget >nul 2>&1
if errorlevel 1 (
    echo [ERROR] winget no disponible. Instale Java 21: https://adoptium.net/
    endlocal
    exit /b 1
)

if /i not "%FT_AUTO_INSTALL%"=="1" (
    echo.
    echo ¿Instalar Java 21 ^(Temurin JDK^) con winget?
    set /p INSTALL_JAVA="Escriba S para instalar, N para cancelar: "
    if /i not "!INSTALL_JAVA!"=="S" (
        echo [ERROR] Java 21 es requerido.
        endlocal
        exit /b 1
    )
)

echo.
echo Instalando Java 21 con winget ^(puede tardar varios minutos^)...
winget install -e --id EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
    winget install -e --id EclipseAdoptium.Temurin.21.JRE --accept-package-agreements --accept-source-agreements
)
if errorlevel 1 (
    echo [ERROR] No se pudo instalar Java automaticamente.
    endlocal
    exit /b 1
)

for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do set "PATH=%%d\bin;!PATH!"
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jre-21*") do set "PATH=%%d\bin;!PATH!"
for /d %%d in ("%LocalAppData%\Programs\Eclipse Adoptium\jdk-21*") do set "PATH=%%d\bin;!PATH!"

where java >nul 2>&1
if errorlevel 1 (
    echo [AVISO] Java instalado. Cierre esta ventana, abra una nueva y ejecute Iniciar.bat de nuevo.
    endlocal
    exit /b 1
)

echo [OK] Java 21 listo.
for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 0
