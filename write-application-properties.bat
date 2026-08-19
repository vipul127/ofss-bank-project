@echo off
setlocal
cd /d "%~dp0"

call :writeDbService "branch-service" 8081
call :writeDbService "cash-management-service" 8082
call :writeDbService "forecast-service" 8083
call :writeDbService "cash-requirement-service" 8084

if not exist "eureka-server\src\main\resources" mkdir "eureka-server\src\main\resources"
(
echo server.port=8761
echo spring.application.name=eureka-server
echo eureka.client.register-with-eureka=false
echo eureka.client.fetch-registry=false
echo eureka.server.enable-self-preservation=false
echo management.endpoints.web.exposure.include=health,info
) > "eureka-server\src\main\resources\application.properties"

if not exist "simulator-service\src\main\resources" mkdir "simulator-service\src\main\resources"
(
echo server.port=8085
echo spring.application.name=simulator-service
echo eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
echo eureka.instance.prefer-ip-address=true
echo management.endpoints.web.exposure.include=health,info
) > "simulator-service\src\main\resources\application.properties"

echo Done.
exit /b 0

:writeDbService
set "SVC=%~1"
set "PORT=%~2"
if not exist "%SVC%\src\main\resources" mkdir "%SVC%\src\main\resources"
(
echo server.port=%PORT%
echo spring.application.name=%SVC%
echo spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
echo spring.datasource.username=bankroot
echo spring.datasource.password=bankroot123
echo spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
echo spring.jpa.hibernate.ddl-auto=validate
echo spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
echo eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
echo eureka.instance.prefer-ip-address=true
echo management.endpoints.web.exposure.include=health,info
) > "%SVC%\src\main\resources\application.properties"
exit /b 0
