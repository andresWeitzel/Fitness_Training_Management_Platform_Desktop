@echo off
REM Solo verifica Java. Instalacion manual si falta.
call "%~dp0..\setup\check-requirements.bat" run
exit /b %ERRORLEVEL%
