@echo off
cd /d "%~dp0\.."
echo Launching LogView Pro...
java -jar target\logview-pro-1.0.0.jar
if %ERRORLEVEL% neq 0 (
    echo.
    echo If jar is missing, run: mvn clean package -DskipTests
    pause
)
