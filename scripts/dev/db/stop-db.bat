@echo off
setlocal EnableExtensions
cd /d "%~dp0..\..\.."

echo Deteniendo contenedores...
docker compose stop
docker compose ps
pause
