@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === Fitness Training - Empaquetado para cliente ===
echo.

call mvn -q -Pclient-package clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Fallo el build Maven.
    pause
    exit /b 1
)

set "DIST=target\client-dist"
if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%\app"
mkdir "%DIST%\db"
mkdir "%DIST%\scripts"
mkdir "%DIST%\docs"

xcopy /E /I /Y "target\client\*" "%DIST%\app\"
copy /Y "docker-compose.yml" "%DIST%\db\"
copy /Y ".env.example" "%DIST%\db\"
copy /Y "install\client-dist\Iniciar.bat" "%DIST%\"
copy /Y "install\client-dist\configurar-primera-vez.bat" "%DIST%\"
copy /Y "install\client-dist\LEEME.txt" "%DIST%\"
copy /Y "install\client-dist\scripts\start-db.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\start-db-silent.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\start-db-portable.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\start-db-portable-silent.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\init-db-portable.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\stop-db.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\stop-db-portable.bat" "%DIST%\scripts\"
copy /Y "install\client-dist\scripts\backup-db.bat" "%DIST%\scripts\"
copy /Y "install\CLIENTE.md" "%DIST%\docs\"
copy /Y "install\ENTREGA-PORTABLE.md" "%DIST%\docs\"

if exist "runtime\jdk\bin\java.exe" (
    echo Incluyendo Java portable en runtime\jdk ...
    xcopy /E /I /Y "runtime\jdk" "%DIST%\runtime\jdk"
) else (
    echo [INFO] Sin runtime\jdk. Cliente necesita Java 21 o copiar JRE ^(docs\ENTREGA-PORTABLE.md^).
)

if exist "runtime\postgresql\bin\pg_ctl.exe" (
    echo Incluyendo PostgreSQL portable en runtime\postgresql ...
    xcopy /E /I /Y "runtime\postgresql" "%DIST%\runtime\postgresql"
) else (
    echo [INFO] Sin runtime\postgresql. Cliente usa Docker o PostgreSQL instalado ^(docs\ENTREGA-PORTABLE.md^).
)

set "ZIP=target\FitnessTraining-client-win64.zip"
if exist "%ZIP%" del /f /q "%ZIP%"

powershell -NoProfile -Command "Compress-Archive -Path '%DIST%\*' -DestinationPath '%ZIP%' -Force"
if errorlevel 1 (
    echo [AVISO] No se pudo crear el zip. Entregue la carpeta %DIST%
) else (
    echo Zip listo: %ZIP%
)

echo.
echo === Entrega al cliente ===
echo   Carpeta: %DIST%
echo   Zip:     %ZIP%
echo.
echo   Iniciar.bat     - un clic: base + app
echo   runtime\jdk     - Java incluido ^(si lo copiaste antes^)
echo   runtime\postgresql - base incluida ^(si lo copiaste antes^)
echo   docs\ENTREGA-PORTABLE.md - guia completa portable
echo.
pause
exit /b 0