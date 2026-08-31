@echo off
REM Solo verifica Maven. Instalacion manual si falta.
call "%~dp0..\setup\check-requirements.bat" build
exit /b %ERRORLEVEL%
