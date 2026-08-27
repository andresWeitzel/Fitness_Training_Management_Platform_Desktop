@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "DIST=%~dp0.."
set "PGROOT=%DIST%\runtime\postgresql"
set "PGDATA=%DIST%\db\data\pgdata"
set "PGLOGDIR=%DIST%\db\logs"
set "PGLOG=%PGLOGDIR%\postgres.log"
set "ENV_FILE=%DIST%\db\.env"

if not exist "%PGROOT%\bin\pg_ctl.exe" (
    echo [ERROR] No hay PostgreSQL portable en runtime\postgresql
    pause
    exit /b 1
)

call "%~dp0init-db-portable.bat"
if errorlevel 1 (
    pause
    exit /b 1
)

set "PG_PORT=5432"
if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        if "%%a"=="POSTGRES_PORT" set "PG_PORT=%%b"
    )
)

if not exist "%PGLOGDIR%" mkdir "%PGLOGDIR%"

"%PGROOT%\bin\pg_ctl.exe" -D "%PGDATA%" -o "-p %PG_PORT%" -l "%PGLOG%" status >nul 2>&1
if not errorlevel 1 (
    echo PostgreSQL portable ya esta en ejecucion.
    goto :ready
)

echo Levantando PostgreSQL portable en puerto %PG_PORT%...
"%PGROOT%\bin\pg_ctl.exe" -D "%PGDATA%" -o "-p %PG_PORT%" -l "%PGLOG%" start
if errorlevel 1 (
    echo [ERROR] No se pudo iniciar PostgreSQL. Ver %PGLOG%
    pause
    exit /b 1
)

:ready
set /a tries=0
:wait_loop
"%PGROOT%\bin\pg_isready.exe" -p %PG_PORT% -q
if not errorlevel 1 goto :done
set /a tries+=1
if %tries% geq 30 (
    echo [ERROR] PostgreSQL no respondio a tiempo.
    pause
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto :wait_loop

:done
echo PostgreSQL portable listo en localhost:%PG_PORT%
pause
endlocal
exit /b 0
