@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo Deteniendo PostgreSQL (Docker)...
docker compose stop
docker compose ps
pause
