@echo off
title Start Branch Cash Management System

echo ====================================
echo [1/6] Starting Eureka Server...
echo ====================================
cd /d "%~dp0eureka-server"
start cmd /k "mvn spring-boot:run"

echo.
echo Waiting 5 seconds for Eureka Server to initialize...
timeout /t 5 /nobreak > nul

echo ====================================
echo [2/6] Starting Branch Service...
echo ====================================
cd /d "%~dp0branch-service"
start cmd /k "mvn spring-boot:run"

echo ====================================
echo [3/6] Starting Cash Management Service...
echo ====================================
cd /d "%~dp0cash-management-service"
start cmd /k "mvn spring-boot:run"

echo ====================================
echo [4/6] Starting Cash Requirement Service...
echo ====================================
cd /d "%~dp0cash-requirement-service"
start cmd /k "mvn spring-boot:run"

echo ====================================
echo [5/6] Starting Forecast Service...
echo ====================================
cd /d "%~dp0forecast-service"
start cmd /k "mvn spring-boot:run"

echo ====================================
echo [6/6] Starting Simulator Service...
echo ====================================
cd /d "%~dp0simulator-service"
start cmd /k "mvn spring-boot:run"

echo.
echo ====================================================
echo All 6 services have been triggered successfully!
echo ====================================================
pause