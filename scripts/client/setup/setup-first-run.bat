@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0..\..\"
set "ENV_FILE=%ROOT%db\.env"
set "ENV_EXAMPLE=%ROOT%db\.env.example"
set "CONFIG_DIR=%USERPROFILE%\.fitness-training"
set "CONFIG_FILE=%CONFIG_DIR%\database.properties"

if not exist "%ROOT%db" (
    echo [AVISO] Carpeta db\ no encontrada ^(solo necesaria con Docker^).
)

if not exist "%ENV_FILE%" (
    if exist "%ENV_EXAMPLE%" (
        echo [Setup] Creando db\.env desde plantilla...
        copy /Y "%ENV_EXAMPLE%" "%ENV_FILE%" >nul
        echo [Setup] Edite db\.env y cambie POSTGRES_PASSWORD antes de produccion.
    )
)

if exist "%CONFIG_FILE%" (
    echo [Setup] Conexion ya configurada: %CONFIG_FILE%
    endlocal
    exit /b 0
)

set "PG_PORT=5432"
set "PG_DB=fitness_training"
set "PG_USER=postgres"
set "PG_PASS=postgres"

if exist "%ENV_FILE%" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENV_FILE%") do (
        set "key=%%a"
        set "val=%%b"
        if "!key!"=="POSTGRES_PORT" set "PG_PORT=!val!"
        if "!key!"=="POSTGRES_DB" set "PG_DB=!val!"
        if "!key!"=="POSTGRES_USER" set "PG_USER=!val!"
        if "!key!"=="POSTGRES_PASSWORD" set "PG_PASS=!val!"
    )
)

echo [Setup] Configurando conexion local ^(primera vez^)...
if not exist "%CONFIG_DIR%" mkdir "%CONFIG_DIR%"

> "%CONFIG_FILE%" (
    echo db.host=localhost
    echo db.port=!PG_PORT!
    echo db.name=!PG_DB!
    echo db.user=!PG_USER!
    echo db.password=!PG_PASS!
)

echo [Setup] Listo: %CONFIG_FILE%
endlocal
exit /b 0
