@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

call mvn -q -Pclient-package clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Fallo el build Maven.
    exit /b 1
)

set "DIST=target\client-dist"
set "CLIENT=%~dp0..\..\client"

if exist "%DIST%" rmdir /s /q "%DIST%"
mkdir "%DIST%\app"
mkdir "%DIST%\db"
mkdir "%DIST%\scripts\setup"
mkdir "%DIST%\scripts\java"
mkdir "%DIST%\scripts\maven"
mkdir "%DIST%\scripts\docker"
mkdir "%DIST%\scripts\db"
mkdir "%DIST%\docs"

xcopy /E /I /Y "target\client\*" "%DIST%\app\"
copy /Y "docker-compose.yml" "%DIST%\db\"
copy /Y ".env.example" "%DIST%\db\"
copy /Y "%CLIENT%\Iniciar.bat" "%DIST%\"
copy /Y "install\client-dist\LEEME.txt" "%DIST%\"
copy /Y "install\client-dist\DESCOMPRIMIR.txt" "%DIST%\"
copy /Y "%CLIENT%\setup\check-requirements.bat" "%DIST%\scripts\setup\"
copy /Y "%CLIENT%\setup\setup-first-run.bat" "%DIST%\scripts\setup\"
copy /Y "%CLIENT%\setup\validate-package.bat" "%DIST%\scripts\setup\"
copy /Y "%CLIENT%\java\check-java.bat" "%DIST%\scripts\java\"
copy /Y "%CLIENT%\maven\check-maven.bat" "%DIST%\scripts\maven\"
copy /Y "%CLIENT%\docker\check-docker.bat" "%DIST%\scripts\docker\"
copy /Y "%CLIENT%\db\start-db.bat" "%DIST%\scripts\db\"
copy /Y "%CLIENT%\db\start-db-silent.bat" "%DIST%\scripts\db\"
copy /Y "%CLIENT%\db\stop-db.bat" "%DIST%\scripts\db\"
copy /Y "%CLIENT%\db\backup-db.bat" "%DIST%\scripts\db\"
copy /Y "install\CLIENTE.md" "%DIST%\docs\"
copy /Y "install\CONFIGURAR-JAVA-POSTGRES.md" "%DIST%\docs\"
copy /Y "install\QUE-EJECUTAR.md" "%DIST%\docs\"

exit /b 0
