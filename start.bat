@echo off
REM Smart Airport Ride Pooling - Quick Start Script for Windows

echo =========================================
echo 🚕 Ride Pooling Backend - Quick Start
echo =========================================
echo.

REM Check Java
echo 📋 Checking Java version...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed. Please install Java 17 or higher.
    exit /b 1
)
echo ✅ Java version OK
echo.

REM Check Maven
echo 📋 Checking Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven is not installed. Please install Maven 3.8 or higher.
    exit /b 1
)
echo ✅ Maven found
echo.

REM Build
echo 🔨 Building project...
call mvn clean install -DskipTests

if %errorlevel% neq 0 (
    echo ❌ Build failed. Please check the error messages above.
    exit /b 1
)
echo ✅ Build successful!
echo.

REM Run
echo 🚀 Starting application...
echo =========================================
echo.

call mvn spring-boot:run
