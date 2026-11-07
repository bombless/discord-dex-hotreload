@echo off
set VERSION=9.1.0
set BASE=%USERPROFILE%\.gradle\manual-wrapper
set DIST=%BASE%\gradle-%VERSION%
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip -OutFile '%BASE%\gradle-%VERSION%-bin.zip'; Expand-Archive -Force '%BASE%\gradle-%VERSION%-bin.zip' '%BASE%'"
)
call "%DIST%\bin\gradle.bat" %*
