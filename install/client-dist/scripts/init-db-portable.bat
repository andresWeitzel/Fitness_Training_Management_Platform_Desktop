@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "DIST=%~dp0.."
set "PGROOT=%DIST%\runtime\postgresql"
set "PGDATA=%DIST%\db\data\pgdata"
set "PGLOGDIR=%DIST%\db\logs"
set "ENV_FILE=%DIST%\db\.env"

if not exist "%PGROOT%\bin\initdb.exe" (
    echo [ERROR] No hay PostgreSQL portable en runtime\postgresql
    exit /b 1
)

if exist "%PGDATA%\PG_VERSION" (
    exit /b 0
)

set "PG_PORT=5432"
set "PG_PASS=postgres"
if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        set "key=%%a"
        set "val=%%b"
        if "!key!"=="POSTGRES_PORT" set "PG_PORT=!val!"
        if "!key!"=="POSTGRES_PASSWORD" set "PG_PASS=!val!"
    )
)

if not exist "%PGLOGDIR%" mkdir "%PGLOGDIR%"
if not exist "%DIST%\db\data" mkdir "%DIST%\db\data"

set "PWFILE=%TEMP%\ft_pg_pw_%RANDOM%.txt"
> "%PWFILE%" echo !PG_PASS!

echo [PostgreSQL] Inicializando base por primera vez...
"%PGROOT%\bin\initdb.exe" -D "%PGDATA%" -U postgres -E UTF8 --locale=C --pwfile="%PWFILE%"
set "INIT_ERR=!errorlevel!"
del /f /q "%PWFILE%" 2>nul
if !INIT_ERR! neq 0 (
    echo [ERROR] initdb fallo.
    exit /b 1
)

echo [PostgreSQL] Base inicializada en db\data\pgdata
endlocal
exit /b 0
