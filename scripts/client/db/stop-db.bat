@echo off
setlocal EnableExtensions
set "ROOT=%~dp0..\..\"
cd /d "%ROOT%db"

echo Deteniendo PostgreSQL ^(Docker^)...
docker compose stop
docker compose ps
pause
endlocal
exit /b 0
