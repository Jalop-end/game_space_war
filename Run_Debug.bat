@echo off
cd /d "%~dp0"
echo Starting Space War directly...
call gradlew.bat lwjgl3:run
pause