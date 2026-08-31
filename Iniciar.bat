@echo off
cd /d "%~dp0"
call "scripts\client\Iniciar.bat"
exit /b %ERRORLEVEL%
