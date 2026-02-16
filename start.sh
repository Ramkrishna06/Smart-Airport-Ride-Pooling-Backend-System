#!/bin/bash

# Smart Airport Ride Pooling - Quick Start Script
# This script builds and runs the application

set -e  # Exit on error

echo "========================================="
echo "🚕 Ride Pooling Backend - Quick Start"
echo "========================================="
echo ""

# Check Java version
echo "📋 Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi
echo "✅ Java version OK"
echo ""

# Check Maven
echo "📋 Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.8 or higher."
    exit 1
fi
echo "✅ Maven found"
echo ""

# Clean and build
echo "🔨 Building project..."
mvn clean install -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
else
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

# Run the application
echo "🚀 Starting application..."
echo "========================================="
echo ""

mvn spring-boot:run
