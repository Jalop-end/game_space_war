@echo off
cd /d "%~dp0"
start "" powershell.exe -WindowStyle Hidden -ExecutionPolicy Bypass -File "%~dp0launcher\splash_launcher.ps1"
