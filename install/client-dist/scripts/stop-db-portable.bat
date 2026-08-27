@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "DIST=%~dp0.."
set "PGROOT=%DIST%\runtime\postgresql"
set "PGDATA=%DIST%\db\data\pgdata"
set "ENV_FILE=%DIST%\db\.env"

if not exist "%PGROOT%\bin\pg_ctl.exe" exit /b 1

set "PG_PORT=5432"
if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        if "%%a"=="POSTGRES_PORT" set "PG_PORT=%%b"
    )
)

echo Deteniendo PostgreSQL portable...
"%PGROOT%\bin\pg_ctl.exe" -D "%PGDATA%" -o "-p %PG_PORT%" stop
pause
endlocal
exit /b 0
