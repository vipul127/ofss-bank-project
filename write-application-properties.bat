@echo off
setlocal
cd /d "%~dp0"

call :writeDbService "branch-service" 8081
call :writeDbService "cash-management-service" 8082
call :writeDbService "forecast-service" 8083
call :writeDbService "cash-requirement-service" 8084

if not exist "eureka-server\src\main\resources" mkdir "eureka-server\src\main\resources"
(
echo server:
echo   port: 8761
echo spring:
echo   application:
echo     name: eureka-server
echo eureka:
echo   client:
echo     register-with-eureka: false
echo     fetch-registry: false
echo   server:
echo     enable-self-preservation: false
echo management:
echo   endpoints:
echo     web:
echo       exposure:
echo         include: health,info
) > "eureka-server\src\main\resources\application.yml"

if not exist "simulator-service\src\main\resources" mkdir "simulator-service\src\main\resources"
(
echo server:
echo   port: 8085
echo spring:
echo   application:
echo     name: simulator-service
echo eureka:
echo   client:
echo     service-url:
echo       defaultZone: http://localhost:8761/eureka/
echo   instance:
echo     ip-address: 127.0.0.1
echo     prefer-ip-address: true
echo management:
echo   endpoints:
echo     web:
echo       exposure:
echo         include: health,info
) > "simulator-service\src\main\resources\application.yml"

echo YAML configuration written.
exit /b 0

:writeDbService
set "SVC=%~1"
set "PORT=%~2"
if not exist "%SVC%\src\main\resources" mkdir "%SVC%\src\main\resources"
(
echo server:
echo   port: %PORT%
echo spring:
echo   application:
echo     name: %SVC%
echo   datasource:
echo     url: jdbc:oracle:thin:@//localhost:1521/FREEPDB1
echo     username: bankroot
echo     password: bankroot123
echo     driver-class-name: oracle.jdbc.OracleDriver
echo   jpa:
echo     hibernate:
echo       ddl-auto: validate
echo     properties:
echo       hibernate.dialect: org.hibernate.dialect.OracleDialect
echo eureka:
echo   client:
echo     service-url:
echo       defaultZone: http://localhost:8761/eureka/
echo   instance:
echo     ip-address: 127.0.0.1
echo     prefer-ip-address: true
echo management:
echo   endpoints:
echo     web:
echo       exposure:
echo         include: health,info
) > "%SVC%\src\main\resources\application.yml"
exit /b 0
