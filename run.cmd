@echo off
rem Build the release APK and install/launch it on the connected phone. Arguments pass through,
rem e.g.  run -Store myket  or  run -Serial 2b945d16
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Run-Phone.ps1" %*
if errorlevel 1 pause
