@echo off
setlocal enabledelayedexpansion

echo ===============================================================================
echo  LogView Pro - Windows Executable Packaging Script (jpackage)
echo ===============================================================================
echo.

cd /d "%~dp0\.."

echo [*] Building project and creating shaded fat JAR...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [*] Checking for jpackage...
where jpackage >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] jpackage command not found in PATH!
    echo Please ensure Java 17+ or 21+ JDK bin directory is on your PATH.
    exit /b 1
)

echo [*] Cleaning output directory 'dist'...
if exist dist rmdir /s /q dist
mkdir dist

echo [*] Generating standalone Windows application with bundled Java runtime...
jpackage ^
    --type app-image ^
    --name "LogViewPro" ^
    --app-version "1.0.0" ^
    --vendor "DeepMind Enterprise" ^
    --description "Advanced Log4j and Application Log Viewer for Windows" ^
    --input "target" ^
    --main-jar "logview-pro-1.0.0.jar" ^
    --main-class "com.logviewpro.Launcher" ^
    --dest "dist" ^
    --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] jpackage app-image creation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo ===============================================================================
echo [SUCCESS] LogView Pro packaged successfully!
echo Executable location: dist\LogViewPro\LogViewPro.exe
echo Bundled Java Runtime: dist\LogViewPro\runtime\
echo The application is 100%% standalone and requires NO pre-installed Java.
echo ===============================================================================
echo.
pause
