@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo Modo desarrollo - compilando ultimos cambios...
call mvn -q compile javafx:run
if errorlevel 1 pause
