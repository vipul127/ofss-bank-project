@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem ================================================================
rem Branch Cash Position & Inter-Branch Transfer System
rem Windows scaffold generator
rem
rem Put this BAT in the workspace folder and run it.
rem No root pom.xml is required.
rem Existing files are preserved.
rem ================================================================

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

cd /d "%ROOT%" >nul 2>&1

if not exist "%ROOT%" (
    echo ERROR: Workspace folder does not exist:
    echo %ROOT%
    exit /b 1
)

echo.
echo ================================================================
echo  Branch Cash Position System - Project Scaffold
 echo ================================================================
echo Root: %ROOT%
echo.

rem ---------------------------------------------------------------
rem Root folders
rem ---------------------------------------------------------------
for %%D in (
    "db"
    "db\ddl"
    "logs"
    "scripts"
) do if not exist "%ROOT%\%%~D" mkdir "%ROOT%\%%~D"

rem ---------------------------------------------------------------
rem Eureka Server
rem ---------------------------------------------------------------
call :MAKE_SERVICE "eureka-server" "com\bankdemo\eurekaserver" "" "EurekaServerApplication.java"

rem ---------------------------------------------------------------
rem Branch Service
rem ---------------------------------------------------------------
call :MAKE_SERVICE "branch-service" "com\bankdemo\branchservice" "entity repository dto controller service config" ^
  "BranchServiceApplication.java entity\Branch.java entity\AppUser.java repository\BranchRepository.java repository\AppUserRepository.java dto\BranchDto.java dto\ThresholdUpdateRequest.java dto\LoginRequest.java dto\LoginResponse.java controller\BranchController.java controller\AuthController.java service\BranchService.java service\BranchServiceImpl.java service\AuthService.java service\AuthServiceImpl.java config\SecurityConfig.java"

rem ---------------------------------------------------------------
rem Cash Management Service
rem ---------------------------------------------------------------
call :MAKE_SERVICE "cash-management-service" "com\bankdemo\cashmanagementservice" "entity repository dto client controller service config" ^
  "CashManagementServiceApplication.java entity\CashTransaction.java repository\CashTransactionRepository.java dto\CashTransactionDto.java dto\CashPositionDto.java dto\BranchDto.java client\BranchServiceClient.java controller\CashPositionController.java service\CashPositionService.java service\CashPositionServiceImpl.java config\FeignConfig.java"

rem ---------------------------------------------------------------
rem Forecast Service
rem ---------------------------------------------------------------
call :MAKE_SERVICE "forecast-service" "com\bankdemo\forecastservice" "entity repository dto client controller service config" ^
  "ForecastServiceApplication.java entity\ForecastSnapshot.java repository\ForecastSnapshotRepository.java dto\ForecastDto.java dto\CashTransactionDto.java dto\BranchDto.java client\BranchServiceClient.java client\CashManagementServiceClient.java controller\ForecastController.java service\ForecastCalculator.java service\ForecastService.java service\ForecastServiceImpl.java"

rem ---------------------------------------------------------------
rem Cash Requirement Service
rem ---------------------------------------------------------------
call :MAKE_SERVICE "cash-requirement-service" "com\bankdemo\cashrequirementservice" "entity repository dto client controller service config" ^
  "CashRequirementServiceApplication.java entity\TransferRequest.java repository\TransferRequestRepository.java dto\TransferRequestDto.java dto\SuggestedSourceDto.java dto\CashPositionDto.java dto\BranchDto.java client\BranchServiceClient.java client\CashManagementServiceClient.java controller\CashRequirementController.java service\NearestBranchLocator.java service\CashRequirementService.java service\CashRequirementServiceImpl.java"

rem ---------------------------------------------------------------
rem Simulator Service
rem ---------------------------------------------------------------
call :MAKE_SERVICE "simulator-service" "com\bankdemo\simulatorservice" "client config job dto" ^
  "SimulatorServiceApplication.java client\CashManagementServiceClient.java config\SchedulingConfig.java job\TransactionSimulatorJob.java dto\CashTransactionDto.java"

rem ---------------------------------------------------------------
rem DDL
rem ---------------------------------------------------------------
call :MAKE_FILE "%ROOT%\db\ddl\01_branch.sql"
call :MAKE_FILE "%ROOT%\db\ddl\02_cash_transaction.sql"
call :MAKE_FILE "%ROOT%\db\ddl\03_forecast_snapshot.sql"
call :MAKE_FILE "%ROOT%\db\ddl\04_transfer_request.sql"
call :MAKE_FILE "%ROOT%\db\ddl\05_seed_data.sql"

rem ---------------------------------------------------------------
rem Oracle JET client
rem ---------------------------------------------------------------
for %%D in (
    "ojet-client"
    "ojet-client\src"
    "ojet-client\src\js"
    "ojet-client\src\js\services"
    "ojet-client\src\js\viewModels"
    "ojet-client\src\js\viewModels\cash-management"
    "ojet-client\src\views"
    "ojet-client\src\css"
) do if not exist "%ROOT%\%%~D" mkdir "%ROOT%\%%~D"

for %%F in (
    "ojet-client\package.json"
    "ojet-client\oraclejetconfig.json"
    "ojet-client\src\js\main.js"
    "ojet-client\src\js\appController.js"
    "ojet-client\src\js\services\apiClient.js"
    "ojet-client\src\js\viewModels\home.js"
    "ojet-client\src\js\viewModels\cash-management.js"
    "ojet-client\src\js\viewModels\cash-management\branch-position.js"
    "ojet-client\src\js\viewModels\cash-management\forecast.js"
    "ojet-client\src\js\viewModels\cash-management\transfer-board.js"
    "ojet-client\src\js\viewModels\cash-management\threshold-config.js"
    "ojet-client\src\js\viewModels\customers.js"
    "ojet-client\src\js\viewModels\reports.js"
    "ojet-client\src\js\viewModels\settings.js"
    "ojet-client\src\views\home.html"
    "ojet-client\src\views\cash-management.html"
    "ojet-client\src\views\customers.html"
    "ojet-client\src\views\reports.html"
    "ojet-client\src\views\settings.html"
    "ojet-client\src\css\app.css"
) do call :MAKE_FILE "%ROOT%\%%~F"

call :MAKE_FILE "%ROOT%\scripts\start-all.bat"
call :MAKE_FILE "%ROOT%\README-SCAFFOLD.md"

echo.
echo ================================================================
echo Scaffold created successfully.
echo ================================================================
echo.
echo Root workspace:
echo   %ROOT%
echo.
echo Modules:
echo   eureka-server              8761
echo   branch-service             8081
echo   cash-management-service    8082
echo   forecast-service           8083
echo   cash-requirement-service   8084
echo   simulator-service          8085
echo.
echo No root pom.xml is required.
echo Existing files were preserved.
echo Placeholder files were created where missing.
echo.
exit /b 0

:MAKE_SERVICE
set "SERVICE=%~1"
set "PKG=%~2"
set "SUBDIRS=%~3"
set "FILES=%~4"

if not exist "%ROOT%\%SERVICE%" mkdir "%ROOT%\%SERVICE%"
if not exist "%ROOT%\%SERVICE%\src" mkdir "%ROOT%\%SERVICE%\src"
if not exist "%ROOT%\%SERVICE%\src\main" mkdir "%ROOT%\%SERVICE%\src\main"
if not exist "%ROOT%\%SERVICE%\src\main\java" mkdir "%ROOT%\%SERVICE%\src\main\java"
if not exist "%ROOT%\%SERVICE%\src\main\resources" mkdir "%ROOT%\%SERVICE%\src\main\resources"
if not exist "%ROOT%\%SERVICE%\src\test" mkdir "%ROOT%\%SERVICE%\src\test"
if not exist "%ROOT%\%SERVICE%\src\test\java" mkdir "%ROOT%\%SERVICE%\src\test\java"
if not exist "%ROOT%\%SERVICE%\src\main\java\%PKG%" mkdir "%ROOT%\%SERVICE%\src\main\java\%PKG%"
if not exist "%ROOT%\%SERVICE%\src\test\java\%PKG%" mkdir "%ROOT%\%SERVICE%\src\test\java\%PKG%"
if not "%SUBDIRS%"=="" for %%D in (%SUBDIRS%) do if not exist "%ROOT%\%SERVICE%\src\main\java\%PKG%\%%D" mkdir "%ROOT%\%SERVICE%\src\main\java\%PKG%\%%D"

call :MAKE_FILE "%ROOT%\%SERVICE%\pom.xml"
call :MAKE_FILE "%ROOT%\%SERVICE%\src\main\resources\application.yml"

if not "%FILES%"=="" for %%F in (%FILES%) do call :MAKE_FILE "%ROOT%\%SERVICE%\src\main\java\%PKG%\%%F"

exit /b 0

:MAKE_FILE
if exist "%~1" exit /b 0
for %%A in ("%~1") do set "PARENT=%%~dpA"
if not exist "!PARENT!" mkdir "!PARENT!" >nul 2>&1
if not exist "%~1" copy /y nul "%~1" >nul 2>&1
if not exist "%~1" (
    echo ERROR: Could not create:
    echo   %~1
) else (
    echo Created: %~1
)
exit /b 0
