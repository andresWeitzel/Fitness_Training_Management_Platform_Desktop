@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM Uso: check-requirements.bat [build|run]
set "MODE=%~1"
if not defined MODE set "MODE=run"
set "FAIL=0"

echo.
echo  === Verificacion de requisitos ===
echo.

REM --- Java 21 ---
set "JAVA_OK=0"
where java >nul 2>&1
if errorlevel 1 (
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
    echo !JAVA_VER! | findstr /r "21\." >nul
    if not errorlevel 1 (
        echo  [OK]    Java 21: !JAVA_VER!
        set "JAVA_OK=1"
    ) else (
        echo  [AVISO] Java detectado pero se recomienda 21: !JAVA_VER!
        set "JAVA_OK=1"
    )
)

if "!JAVA_OK!"=="0" (
    echo  [FALTA] Java 21
    echo          Descargar: https://adoptium.net/
    set "FAIL=1"
)

REM --- Maven (solo compilar desde GitHub) ---
if /i "%MODE%"=="build" (
    set "MVN_OK=0"
    where mvn >nul 2>&1
    if errorlevel 1 (
        for /d %%d in ("%ProgramFiles%\Apache\maven\*") do (
            if exist "%%d\bin\mvn.cmd" set "PATH=%%d\bin;!PATH!"
        )
    )
    where mvn >nul 2>&1
    if not errorlevel 1 (
        for /f "tokens=*" %%v in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do echo  [OK]    %%v
        set "MVN_OK=1"
    )
    if "!MVN_OK!"=="0" (
        echo  [FALTA] Maven 3.9+
        echo          Descargar: https://maven.apache.org/download.cgi
        set "FAIL=1"
    )
)

REM --- Docker / PostgreSQL (informativo en run) ---
if /i "%MODE%"=="run" (
    set "DB_OK=0"
    where docker >nul 2>&1
    if not errorlevel 1 (
        docker info >nul 2>&1
        if not errorlevel 1 (
            echo  [OK]    Docker Desktop en ejecucion
            set "DB_OK=1"
        ) else (
            echo  [AVISO] Docker instalado pero no esta en ejecucion
            echo          Abra Docker Desktop y vuelva a ejecutar Iniciar.bat
        )
    )
    if "!DB_OK!"=="0" (
        sc query state= all 2>nul | findstr /i "postgresql" >nul 2>&1
        if not errorlevel 1 (
            echo  [OK]    Servicio PostgreSQL detectado en Windows
            set "DB_OK=1"
        )
    )
    netstat -ano 2>nul | findstr ":5432" | findstr "LISTENING" >nul 2>&1
    if not errorlevel 1 if "!DB_OK!"=="0" (
        echo  [OK]    Puerto 5432 en uso ^(posible PostgreSQL activo^)
        set "DB_OK=1"
    )
    if "!DB_OK!"=="0" (
        echo  [AVISO] No hay Docker ni PostgreSQL detectado
        echo          Docker: https://www.docker.com/products/docker-desktop/
        echo          PostgreSQL: https://www.postgresql.org/download/windows/
        echo          Instale uno y vuelva a ejecutar Iniciar.bat
    )
)

echo.

if "!FAIL!"=="1" (
    echo  Instale lo marcado como [FALTA] y ejecute Iniciar.bat de nuevo.
    for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 1
)

echo  [OK] Requisitos criticos listos. Continuando...
echo.
for /f "delims=" %%P in ("!PATH!") do endlocal & set "PATH=%%P" & exit /b 0
