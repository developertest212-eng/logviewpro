# LogView Pro - Windows Packaging Automation Script
$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $ProjectRoot

Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host " LogView Pro - Standalone Windows Package Builder (jpackage)" -ForegroundColor Cyan
Write-Host "===============================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[*] Compiling and packaging shaded JAR..." -ForegroundColor Yellow
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed."
    exit $LASTEXITCODE
}

Write-Host "[*] Checking jpackage availability..." -ForegroundColor Yellow
$jpackageCheck = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackageCheck) {
    Write-Error "jpackage command not found in PATH. Please ensure JDK 17+ or 21+ bin directory is added to PATH."
    exit 1
}

$DistDir = Join-Path $ProjectRoot "dist"
if (Test-Path $DistDir) {
    Remove-Item -Recurse -Force $DistDir
}
New-Item -ItemType Directory -Path $DistDir | Out-Null

Write-Host "[*] Generating standalone Windows Application (LogViewPro.exe with bundled JRE)..." -ForegroundColor Yellow
& jpackage `
    --type app-image `
    --name "LogViewPro" `
    --app-version "1.0.0" `
    --vendor "DeepMind Enterprise" `
    --description "Advanced Log4j and Application Log Viewer for Windows" `
    --input (Join-Path $ProjectRoot "target") `
    --main-jar "logview-pro-1.0.0.jar" `
    --main-class "com.logviewpro.Launcher" `
    --dest $DistDir `
    --java-options "-Dfile.encoding=UTF-8"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "===============================================================================" -ForegroundColor Green
    Write-Host " [SUCCESS] Windows Standalone App Built Successfully!" -ForegroundColor Green
    Write-Host " Executable: $DistDir\LogViewPro\LogViewPro.exe" -ForegroundColor Green
    Write-Host " Bundled Runtime: $DistDir\LogViewPro\runtime\" -ForegroundColor Green
    Write-Host " Runs on any Windows 10/11 system without requiring Java installed!" -ForegroundColor Green
    Write-Host "===============================================================================" -ForegroundColor Green
} else {
    Write-Error "jpackage execution failed with code $LASTEXITCODE"
}
