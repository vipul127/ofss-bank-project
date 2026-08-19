# Branch Cash Position & Inter-Branch Transfer System

*Technical Implementation Guide — v2 (Microservices)*

Companion to the v2 Development Blueprint  ·  Training Project — Banking Consulting Practice

Stack: Oracle JET · Spring Boot · Spring Cloud (Eureka, OpenFeign) · JPA · Oracle Database 23ai

# 1. Purpose of This Document

This guide is the implementation-level companion to the v2 Development Blueprint. Where the Blueprint explains what the system does and why each architectural decision was made, this document specifies exactly how to build it: package names, file names, class names, REST endpoints, Feign client interfaces, DDL, the startup script, and the Oracle JET frontend structure — including how the mock (non-functional) UI sections are implemented.

Naming is kept consistent across all five business services plus the discovery server, so that a class, endpoint, or table referenced in one service section can be located unambiguously wherever else it is used (Feign clients, controllers, DTOs).

> *Note: Everywhere this guide states 'own tables only, no cross-schema join' it means at the application layer. The underlying Oracle 23ai instance is genuinely shared — see Section 3 for why foreign keys are deliberately NOT enforced at the DB level either, to keep the services honestly decoupled even in the schema.*

# 2. Naming, Package, and Port Conventions

All 6 processes share the group ID com.bankdemo. Each service is its own Maven module / standalone Spring Boot application with its own main class, application.yml, and port.

| # | Process | Root Package | Artifact / Folder | Port |
|---|---|---|---|---|
| 1 | Eureka Server | com.bankdemo.eurekaserver | eureka-server | 8761 |
| 2 | Branch Service | com.bankdemo.branchservice | branch-service | 8081 |
| 3 | Cash Management Service | com.bankdemo.cashmanagementservice | cash-management-service | 8082 |
| 4 | Forecast Service | com.bankdemo.forecastservice | forecast-service | 8083 |
| 5 | Cash Requirement Service | com.bankdemo.cashrequirementservice | cash-requirement-service | 8084 |
| 6 | Simulator Service | com.bankdemo.simulatorservice | simulator-service | 8085 |

Eureka application-name registration values (spring.application.name) match the folder names above exactly (e.g. branch-service), since Feign clients resolve targets by this name.

## 2.1 Layer / Package Suffix Convention (applies inside every service)

| Suffix | Purpose | Example |
|---|---|---|
| .entity | JPA @Entity classes | com.bankdemo.branchservice.entity.Branch |
| .repository | Spring Data JPA repositories | com.bankdemo.branchservice.repository.BranchRepository |
| .dto | Request/response payloads | com.bankdemo.branchservice.dto.BranchDto |
| .controller | REST controllers | com.bankdemo.branchservice.controller.BranchController |
| .service | Business logic | com.bankdemo.branchservice.service.BranchServiceImpl |
| .client | Feign client interfaces (calling OTHER services) | com.bankdemo.cashrequirementservice.client.BranchServiceClient |
| .config | Feign/Eureka/Security config classes | com.bankdemo.branchservice.config.SecurityConfig |

## 2.2 Feign Client Naming Rule

A Feign client is always named <TargetService>Client and lives in the CALLING service's .client package — never in the target service. Example: Cash Requirement Service calling Branch Service uses com.bankdemo.cashrequirementservice.client.BranchServiceClient, annotated @FeignClient(name = "branch-service").

# 3. Database Schema — Oracle Database 23ai

Single schema, e.g. CASHDEMO, shared by all 5 business services (Eureka and the Simulator hold no data of their own). Five tables. No FOREIGN KEY constraints between tables owned by different services — this is deliberate: it mirrors the 'no cross-schema JOIN' application rule at the schema level too, so nothing about the physical DB silently re-couples the services. Referential integrity across service boundaries is enforced in application code (a service validates a branchId by calling Branch Service via Feign before writing, not via a DB constraint).

**db/ddl/01_branch.sql — owned by Branch Service**

```sql
CREATE TABLE BRANCH (
  BRANCH_ID          VARCHAR2(10)   NOT NULL,
  BRANCH_NAME         VARCHAR2(100)  NOT NULL,
  LATITUDE            NUMBER(9,6)    NOT NULL,
  LONGITUDE           NUMBER(9,6)    NOT NULL,
  OPENING_RESERVE      NUMBER(15,2)   NOT NULL,
  CURRENT_RESERVE      NUMBER(15,2)   NOT NULL,
  MIN_THRESHOLD_PCT    NUMBER(5,2)    NOT NULL,
  CONSTRAINT PK_BRANCH PRIMARY KEY (BRANCH_ID)
);
 
CREATE TABLE APP_USER (
  USER_ID       VARCHAR2(36)  NOT NULL,
  USERNAME       VARCHAR2(50)  NOT NULL,
  PASSWORD_HASH  VARCHAR2(255) NOT NULL,
  CREATED_AT     TIMESTAMP     DEFAULT SYSTIMESTAMP,
  CONSTRAINT PK_APP_USER PRIMARY KEY (USER_ID),
  CONSTRAINT UQ_APP_USER_USERNAME UNIQUE (USERNAME)
);
```

**db/ddl/02_cash_transaction.sql — owned by Cash Management Service**

```sql
CREATE TABLE CASH_TRANSACTION (
  TRANSACTION_ID    VARCHAR2(36)  NOT NULL,
  BRANCH_ID          VARCHAR2(10)  NOT NULL,   -- logical reference only, no FK (see Section 3 note)
  TXN_TYPE           VARCHAR2(20)  NOT NULL,   -- 'DEPOSIT' | 'WITHDRAWAL'
  AMOUNT              NUMBER(15,2)  NOT NULL,
  EVENT_TIMESTAMP     TIMESTAMP     NOT NULL,
  CONSTRAINT PK_CASH_TXN PRIMARY KEY (TRANSACTION_ID)
);
CREATE INDEX IX_CASH_TXN_BRANCH ON CASH_TRANSACTION (BRANCH_ID, EVENT_TIMESTAMP);
```

**db/ddl/03_forecast_snapshot.sql — owned by Forecast Service**

```sql
CREATE TABLE FORECAST_SNAPSHOT (
  FORECAST_ID           VARCHAR2(36)  NOT NULL,
  BRANCH_ID              VARCHAR2(10)  NOT NULL,
  FORECAST_DATE           DATE          NOT NULL,
  PREDICTED_POSITION      NUMBER(15,2)  NOT NULL,
  CONFIDENCE_BAND_LOW      NUMBER(15,2)  NOT NULL,
  CONFIDENCE_BAND_HIGH     NUMBER(15,2)  NOT NULL,
  ACTUAL_POSITION          NUMBER(15,2),          -- backfilled once the date passes
  CONSTRAINT PK_FORECAST PRIMARY KEY (FORECAST_ID)
);
CREATE INDEX IX_FORECAST_BRANCH_DATE ON FORECAST_SNAPSHOT (BRANCH_ID, FORECAST_DATE);
```

**db/ddl/04_transfer_request.sql — owned by Cash Requirement Service**

```sql
CREATE TABLE TRANSFER_REQUEST (
  REQUEST_ID              VARCHAR2(36)  NOT NULL,
  SOURCE_BRANCH_ID          VARCHAR2(10)  NOT NULL,
  DESTINATION_BRANCH_ID     VARCHAR2(10)  NOT NULL,
  AMOUNT                    NUMBER(15,2)  NOT NULL,
  STATUS                    VARCHAR2(20)  NOT NULL,  -- REQUESTED|APPROVED|DISPATCHED|DELIVERED
  REQUESTED_AT              TIMESTAMP     DEFAULT SYSTIMESTAMP,
  UPDATED_AT                TIMESTAMP     DEFAULT SYSTIMESTAMP,
  CONSTRAINT PK_TRANSFER_REQ PRIMARY KEY (REQUEST_ID)
);
```

**db/ddl/05_seed_data.sql — 4 demo branches used by the Simulator**

```sql
INSERT INTO BRANCH VALUES ('BR001','Andheri West',19.1364,72.8296,500000,500000,15);
INSERT INTO BRANCH VALUES ('BR002','Bandra Kurla Complex',19.0653,72.8686,750000,750000,15);
INSERT INTO BRANCH VALUES ('BR003','Powai',19.1197,72.9051,400000,400000,15);
INSERT INTO BRANCH VALUES ('BR004','Thane',19.2183,72.9781,300000,300000,15);
-- BR004 is the branch permanently fixed to trend toward deficit (see Section 8, Simulator Service)
COMMIT;
```

# 4. Eureka Server (eureka-server)

Registry only — no business logic, no database connection.

**eureka-server/src/main/java/com/bankdemo/eurekaserver/EurekaServerApplication.java**

```java
package com.bankdemo.eurekaserver;
 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
 
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**eureka-server/src/main/resources/application.yml**

```yaml
server:
  port: 8761
 
spring:
  application:
    name: eureka-server
 
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false   # faster de-registration for a demo, not for prod
```

# 5. Branch Service (branch-service)

Owns BRANCH and APP_USER. Every other service reads branch data via this service's REST API through Feign — never directly from the BRANCH table.

## 5.1 Package Structure

```text
branch-service/src/main/java/com/bankdemo/branchservice/
├── BranchServiceApplication.java
├── entity/
│   ├── Branch.java
│   └── AppUser.java
├── repository/
│   ├── BranchRepository.java
│   └── AppUserRepository.java
├── dto/
│   ├── BranchDto.java
│   ├── ThresholdUpdateRequest.java
│   ├── LoginRequest.java
│   └── LoginResponse.java
├── controller/
│   ├── BranchController.java
│   └── AuthController.java
├── service/
│   ├── BranchService.java (interface)
│   ├── BranchServiceImpl.java
│   ├── AuthService.java (interface)
│   └── AuthServiceImpl.java
└── config/
    └── SecurityConfig.java
```

**branch-service/src/main/resources/application.yml**

```yaml
server:
  port: 8081
 
spring:
  application:
    name: branch-service
  datasource:
    url: jdbc:oracle:thin:@//localhost:1521/FREEPDB1
    username: cashdemo
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.dialect.OracleDialect
 
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

## 5.2 Entity: Branch.java

```java
package com.bankdemo.branchservice.entity;
 
import jakarta.persistence.*;
import java.math.BigDecimal;
 
@Entity
@Table(name = "BRANCH")
public class Branch {
 
    @Id
    @Column(name = "BRANCH_ID", length = 10)
    private String branchId;
 
    @Column(name = "BRANCH_NAME", nullable = false, length = 100)
    private String branchName;
 
    @Column(name = "LATITUDE", nullable = false)
    private BigDecimal latitude;
 
    @Column(name = "LONGITUDE", nullable = false)
    private BigDecimal longitude;
 
    @Column(name = "OPENING_RESERVE", nullable = false)
    private BigDecimal openingReserve;
 
    @Column(name = "CURRENT_RESERVE", nullable = false)
    private BigDecimal currentReserve;
 
    @Column(name = "MIN_THRESHOLD_PCT", nullable = false)
    private BigDecimal minThresholdPct;
 
    // getters and setters omitted for brevity — generate via Lombok @Data
    // or IDE; field names above are the contract other classes rely on.
}
```

## 5.3 Entity: AppUser.java

```java
package com.bankdemo.branchservice.entity;
 
import jakarta.persistence.*;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "APP_USER")
public class AppUser {
 
    @Id
    @Column(name = "USER_ID", length = 36)
    private String userId;
 
    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;
 
    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;
 
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
```

## 5.4 Repositories

```java
package com.bankdemo.branchservice.repository;
 
import com.bankdemo.branchservice.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
 
public interface BranchRepository extends JpaRepository<Branch, String> {
}
package com.bankdemo.branchservice.repository;
 
import com.bankdemo.branchservice.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByUsername(String username);
}
```

## 5.5 DTO: BranchDto.java

```java
package com.bankdemo.branchservice.dto;
 
import java.math.BigDecimal;
 
// This is the shape every OTHER service receives via Feign — keep it stable.
public class BranchDto {
    private String branchId;
    private String branchName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal currentReserve;
    private BigDecimal minThresholdPct;
    // getters/setters
}
```

## 5.6 Controller: BranchController.java

```java
package com.bankdemo.branchservice.controller;
 
import com.bankdemo.branchservice.dto.BranchDto;
import com.bankdemo.branchservice.dto.ThresholdUpdateRequest;
import com.bankdemo.branchservice.service.BranchService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
 
@RestController
@RequestMapping("/api/branches")
public class BranchController {
 
    private final BranchService branchService;
 
    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }
 
    @GetMapping
    public List<BranchDto> getAllBranches() {
        return branchService.getAllBranches();
    }
 
    @GetMapping("/{branchId}")
    public BranchDto getBranch(@PathVariable String branchId) {
        return branchService.getBranch(branchId);
    }
 
    @PutMapping("/{branchId}/threshold")
    public BranchDto updateThreshold(@PathVariable String branchId,
                                      @RequestBody ThresholdUpdateRequest request) {
        return branchService.updateThreshold(branchId, request.getMinThresholdPct());
    }
 
    // Called by Cash Management Service via Feign on every recorded transaction,
    // to keep this table's CURRENT_RESERVE the single source of truth for the
    // branch's running balance. delta is positive for a deposit, negative for
    // a withdrawal — the caller (Cash Management Service) decides the sign.
    @PatchMapping("/{branchId}/reserve")
    public BranchDto adjustReserve(@PathVariable String branchId,
                                    @RequestBody java.math.BigDecimal delta) {
        return branchService.adjustReserve(branchId, delta);
    }
}
```

## 5.7 Controller: AuthController.java

Single login endpoint used by the OJET client. Handles the one auth flow for the whole system (see Blueprint Section 10 — one role, no RBAC yet).

```java
package com.bankdemo.branchservice.controller;
 
import com.bankdemo.branchservice.dto.LoginRequest;
import com.bankdemo.branchservice.dto.LoginResponse;
import com.bankdemo.branchservice.service.AuthService;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/auth")
public class AuthController {
 
    private final AuthService authService;
 
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
 
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
        // AuthServiceImpl: looks up AppUser by username, verifies BCrypt hash,
        // issues a signed JWT (24h expiry) containing { sub: username, role: "USER" }
    }
}
```

> *Note: Threshold Configuration in the UI (Blueprint Section 12) is open to all users in this prototype — the endpoint above has no role check yet, matching that deliberate simplification.*

## 5.8 Service: BranchServiceImpl.java — adjustReserve() detail

Shown here specifically because it's the one method every other service's Feign write path depends on (Cash Management Service calls this after every recorded transaction — see Section 6.5).

```java
package com.bankdemo.branchservice.service;
 
import com.bankdemo.branchservice.dto.BranchDto;
import com.bankdemo.branchservice.entity.Branch;
import com.bankdemo.branchservice.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
 
@Service
public class BranchServiceImpl implements BranchService {
 
    private final BranchRepository branchRepository;
 
    public BranchServiceImpl(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }
 
    @Override
    @Transactional
    public BranchDto adjustReserve(String branchId, BigDecimal delta) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown branchId: " + branchId));
        branch.setCurrentReserve(branch.getCurrentReserve().add(delta));
        branchRepository.save(branch);
        return toDto(branch);
    }
 
    // getAllBranches / getBranch / updateThreshold follow the same
    // findById-then-map pattern; toDto(...) is a private field-by-field mapper.
}
```

# 6. Cash Management Service (cash-management-service)

Owns CASH_TRANSACTION. Computes each branch's live surplus/deficit position from its own transaction history. This is the service the Simulator writes to, and the service every other business service reads live positions from.

## 6.1 Package Structure

```text
cash-management-service/src/main/java/com/bankdemo/cashmanagementservice/
├── CashManagementServiceApplication.java
├── entity/
│   └── CashTransaction.java
├── repository/
│   └── CashTransactionRepository.java
├── dto/
│   ├── CashTransactionDto.java
│   └── CashPositionDto.java
├── client/
│   └── BranchServiceClient.java
├── controller/
│   └── CashPositionController.java
├── service/
│   ├── CashPositionService.java (interface)
│   └── CashPositionServiceImpl.java
└── config/
    └── FeignConfig.java
```

**cash-management-service/src/main/java/.../CashManagementServiceApplication.java**

```java
package com.bankdemo.cashmanagementservice;
 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
 
@SpringBootApplication
@EnableFeignClients
public class CashManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CashManagementServiceApplication.class, args);
    }
}
```

## 6.2 Entity: CashTransaction.java

```java
package com.bankdemo.cashmanagementservice.entity;
 
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "CASH_TRANSACTION")
public class CashTransaction {
 
    @Id
    @Column(name = "TRANSACTION_ID", length = 36)
    private String transactionId;
 
    @Column(name = "BRANCH_ID", nullable = false, length = 10)
    private String branchId;
 
    @Column(name = "TXN_TYPE", nullable = false, length = 20)
    private String txnType;   // "DEPOSIT" | "WITHDRAWAL"
 
    @Column(name = "AMOUNT", nullable = false)
    private BigDecimal amount;
 
    @Column(name = "EVENT_TIMESTAMP", nullable = false)
    private LocalDateTime eventTimestamp;
}
```

## 6.3 Repository

```java
package com.bankdemo.cashmanagementservice.repository;
 
import com.bankdemo.cashmanagementservice.entity.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
 
public interface CashTransactionRepository extends JpaRepository<CashTransaction, String> {
 
    List<CashTransaction> findByBranchIdOrderByEventTimestampDesc(String branchId);
 
    List<CashTransaction> findByBranchIdAndEventTimestampAfter(String branchId, LocalDateTime since);
}
```

## 6.4 Feign Client: BranchServiceClient.java

Used to resolve a branch's opening reserve and threshold when computing a position. This is the pattern every cross-service call in the system follows.

```java
package com.bankdemo.cashmanagementservice.client;
 
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.bankdemo.cashmanagementservice.dto.BranchDto; // local mirror DTO, same shape as Branch Service's
 
@FeignClient(name = "branch-service")   // resolved via Eureka, no hardcoded host:port
public interface BranchServiceClient {
 
    @GetMapping("/api/branches/{branchId}")
    BranchDto getBranch(@PathVariable("branchId") String branchId);
 
    @GetMapping("/api/branches")
    java.util.List<BranchDto> getAllBranches();
 
    @PatchMapping("/api/branches/{branchId}/reserve")
    BranchDto adjustReserve(@PathVariable("branchId") String branchId,
                             @RequestBody BigDecimal delta);
}
```

## 6.5 Service: CashPositionServiceImpl.java — the core surplus/deficit calculation

```java
package com.bankdemo.cashmanagementservice.service;
 
import com.bankdemo.cashmanagementservice.client.BranchServiceClient;
import com.bankdemo.cashmanagementservice.dto.BranchDto;
import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.entity.CashTransaction;
import com.bankdemo.cashmanagementservice.repository.CashTransactionRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
 
@Service
public class CashPositionServiceImpl implements CashPositionService {
 
    private final CashTransactionRepository transactionRepository;
    private final BranchServiceClient branchServiceClient;
 
    public CashPositionServiceImpl(CashTransactionRepository transactionRepository,
                                    BranchServiceClient branchServiceClient) {
        this.transactionRepository = transactionRepository;
        this.branchServiceClient = branchServiceClient;
    }
 
    @Override
    public CashPositionDto getPosition(String branchId) {
        BranchDto branch = branchServiceClient.getBranch(branchId);
 
        // currentReserve on Branch is kept as the running balance; each new
        // transaction PATCHes Branch Service's currentReserve via Feign (see
        // recordTransaction below), so a position read is O(1) — no transaction
        // history lookup needed here. Recent-transaction history is a separate
        // concern, served by getRecentTransactions() below for the UI's feed.
        BigDecimal net = branch.getCurrentReserve();
 
        BigDecimal thresholdAmount = branch.getCurrentReserve()
                .multiply(branch.getMinThresholdPct()).divide(BigDecimal.valueOf(100));
        boolean isDeficit = net.compareTo(thresholdAmount) < 0;
 
        CashPositionDto dto = new CashPositionDto();
        dto.setBranchId(branchId);
        dto.setCurrentReserve(net);
        dto.setThresholdAmount(thresholdAmount);
        dto.setStatus(isDeficit ? "DEFICIT" : "SURPLUS");
        dto.setSurplusOrDeficitAmount(net.subtract(thresholdAmount));
        return dto;
    }
 
    @Override
    public List<CashPositionDto> getAllPositions() {
        return branchServiceClient.getAllBranches().stream()
                .map(b -> getPosition(b.getBranchId()))
                .toList();
    }
 
    @Override
    public CashTransaction recordTransaction(String branchId, String txnType, BigDecimal amount) {
        CashTransaction txn = new CashTransaction();
        txn.setTransactionId(java.util.UUID.randomUUID().toString());
        txn.setBranchId(branchId);
        txn.setTxnType(txnType);
        txn.setAmount(amount);
        txn.setEventTimestamp(java.time.LocalDateTime.now());
        transactionRepository.save(txn);
 
        // keep Branch Service's currentReserve in sync — this is the ONE place
        // Cash Management Service writes into another service's data, and it
        // always goes through Branch Service's own API, never the BRANCH table.
        BigDecimal delta = "DEPOSIT".equals(txnType) ? amount : amount.negate();
        branchServiceClient.adjustReserve(branchId, delta); // PATCH /api/branches/{id}/reserve
 
        return txn;
    }
 
    @Override
    public List<CashTransactionDto> getRecentTransactions(String branchId, int limit) {
        return transactionRepository.findByBranchIdOrderByEventTimestampDesc(branchId)
                .stream()
                .limit(limit)
                .map(this::toDto)   // field-by-field mapper: entity -> CashTransactionDto
                .toList();
    }
}
```

## 6.6 Controller: CashPositionController.java

```java
package com.bankdemo.cashmanagementservice.controller;
 
import com.bankdemo.cashmanagementservice.dto.CashPositionDto;
import com.bankdemo.cashmanagementservice.dto.CashTransactionDto;
import com.bankdemo.cashmanagementservice.service.CashPositionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
 
@RestController
@RequestMapping("/api")
public class CashPositionController {
 
    private final CashPositionService cashPositionService;
 
    public CashPositionController(CashPositionService cashPositionService) {
        this.cashPositionService = cashPositionService;
    }
 
    @GetMapping("/cash-position")
    public List<CashPositionDto> getAllPositions() {
        return cashPositionService.getAllPositions();
    }
 
    @GetMapping("/cash-position/{branchId}")
    public CashPositionDto getPosition(@PathVariable String branchId) {
        return cashPositionService.getPosition(branchId);
    }
 
    @PostMapping("/cash-transaction")
    public void recordTransaction(@RequestBody CashTransactionDto request) {
        // called by Simulator Service, and (in production) would be called by
        // the real OTC deposit/withdrawal feed
        cashPositionService.recordTransaction(
            request.getBranchId(), request.getTxnType(), request.getAmount());
    }
 
    @GetMapping("/cash-transaction/{branchId}/recent")
    public List<CashTransactionDto> getRecentTransactions(@PathVariable String branchId) {
        return cashPositionService.getRecentTransactions(branchId, 20);
    }
}
```

# 7. Forecast Service (forecast-service)

Owns FORECAST_SNAPSHOT. Computes next-day predicted position per branch using rolling mean, standard deviation, day-of-week adjustment, and a linear trend, exactly as specified in Blueprint Section 8 — no ML.

## 7.1 Package Structure

```java
forecast-service/src/main/java/com/bankdemo/forecastservice/
├── ForecastServiceApplication.java
├── entity/
│   └── ForecastSnapshot.java
├── repository/
│   └── ForecastSnapshotRepository.java
├── dto/
│   ├── ForecastDto.java
│   └── CashTransactionDto.java   (local mirror of Cash Mgmt's shape)
├── client/
│   ├── BranchServiceClient.java
│   └── CashManagementServiceClient.java
├── controller/
│   └── ForecastController.java
└── service/
    ├── ForecastCalculator.java     ← the math, isolated & unit-testable
    ├── ForecastService.java (interface)
    └── ForecastServiceImpl.java
```

## 7.2 Entity: ForecastSnapshot.java

```java
package com.bankdemo.forecastservice.entity;
 
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
 
@Entity
@Table(name = "FORECAST_SNAPSHOT")
public class ForecastSnapshot {
 
    @Id
    @Column(name = "FORECAST_ID", length = 36)
    private String forecastId;
 
    @Column(name = "BRANCH_ID", nullable = false, length = 10)
    private String branchId;
 
    @Column(name = "FORECAST_DATE", nullable = false)
    private LocalDate forecastDate;
 
    @Column(name = "PREDICTED_POSITION", nullable = false)
    private BigDecimal predictedPosition;
 
    @Column(name = "CONFIDENCE_BAND_LOW", nullable = false)
    private BigDecimal confidenceBandLow;
 
    @Column(name = "CONFIDENCE_BAND_HIGH", nullable = false)
    private BigDecimal confidenceBandHigh;
 
    @Column(name = "ACTUAL_POSITION")
    private BigDecimal actualPosition;
}
```

## 7.3 Feign Clients

```java
package com.bankdemo.forecastservice.client;
 
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.bankdemo.forecastservice.dto.CashTransactionDto;
import java.util.List;
 
@FeignClient(name = "cash-management-service")
public interface CashManagementServiceClient {
 
    @GetMapping("/api/cash-transaction/{branchId}/recent")
    List<CashTransactionDto> getRecentTransactions(@PathVariable String branchId);
}
```

## 7.4 Service: ForecastCalculator.java — the forecast math

This class implements Blueprint Section 8 exactly: rolling 14-day mean and standard deviation of net daily position, a day-of-week adjustment, a simple linear trend over the window, and a ±1 std-dev confidence band, flagged at-risk if the low end breaches the branch's threshold.

```java
package com.bankdemo.forecastservice.service;
 
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
 
@Component
public class ForecastCalculator {
 
    private static final int WINDOW_DAYS = 14;
 
    /**
     * dailyNetPositions: chronological list of each of the last WINDOW_DAYS
     * days' net position (deposits - withdrawals) for one branch, paired
     * with the calendar date they occurred on.
     */
    public ForecastResult forecastNextDay(List<DailyPosition> dailyNetPositions,
                                           DayOfWeek targetDay,
                                           BigDecimal thresholdAmount) {
 
        double[] values = dailyNetPositions.stream()
                .mapToDouble(d -> d.netPosition().doubleValue())
                .toArray();
 
        double mean = mean(values);
        double stdDev = stdDev(values, mean);
 
        // day-of-week adjustment: average historical net position for that
        // specific weekday, if we have any history for it
        Map<DayOfWeek, List<Double>> byWeekday = dailyNetPositions.stream()
                .collect(Collectors.groupingBy(d -> d.date().getDayOfWeek(),
                         Collectors.mapping(d -> d.netPosition().doubleValue(), Collectors.toList())));
        double weekdayAdjustment = byWeekday.containsKey(targetDay)
                ? mean(byWeekday.get(targetDay).stream().mapToDouble(Double::doubleValue).toArray()) - mean
                : 0.0;
 
        // simple linear trend across the rolling window (least-squares slope)
        double trendPerDay = linearTrendSlope(values);
 
        double predicted = mean + weekdayAdjustment + trendPerDay;
 
        double bandLow = predicted - stdDev;
        double bandHigh = predicted + stdDev;
 
        boolean atRisk = BigDecimal.valueOf(bandLow).compareTo(thresholdAmount) < 0;
 
        return new ForecastResult(
                BigDecimal.valueOf(predicted),
                BigDecimal.valueOf(bandLow),
                BigDecimal.valueOf(bandHigh),
                atRisk);
    }
 
    private double mean(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }
 
    private double stdDev(double[] values, double mean) {
        if (values.length < 2) return 0.0;
        double sumSq = 0;
        for (double v : values) sumSq += Math.pow(v - mean, 2);
        return Math.sqrt(sumSq / (values.length - 1));   // sample std dev
    }
 
    private double linearTrendSlope(double[] values) {
        int n = values.length;
        if (n < 2) return 0.0;
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values[i];
            sumXY += (double) i * values[i];
            sumXX += (double) i * i;
        }
        double denominator = (n * sumXX - sumX * sumX);
        if (denominator == 0) return 0.0;
        return (n * sumXY - sumX * sumY) / denominator;
    }
 
    public record DailyPosition(java.time.LocalDate date, BigDecimal netPosition) {}
 
    public record ForecastResult(BigDecimal predictedPosition, BigDecimal bandLow,
                                  BigDecimal bandHigh, boolean atRisk) {}
}
```

## 7.5 Controller: ForecastController.java

```java
package com.bankdemo.forecastservice.controller;
 
import com.bankdemo.forecastservice.dto.ForecastDto;
import com.bankdemo.forecastservice.service.ForecastService;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
 
    private final ForecastService forecastService;
 
    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }
 
    @GetMapping("/{branchId}")
    public ForecastDto getForecast(@PathVariable String branchId) {
        // returns the most recent snapshot for tomorrow; recomputes on demand
        // if none exists yet for the requested date
        return forecastService.getOrComputeForecast(branchId);
    }
}
```

# 8. Cash Requirement Service (cash-requirement-service)

Owns TRANSFER_REQUEST. Read-only view of which branches need cash (via Cash Management Service), suggests the nearest surplus branch (via Branch Service for lat/long), and owns the transfer workflow state machine.

## 8.1 Package Structure

```java
cash-requirement-service/src/main/java/com/bankdemo/cashrequirementservice/
├── CashRequirementServiceApplication.java
├── entity/
│   └── TransferRequest.java
├── repository/
│   └── TransferRequestRepository.java
├── dto/
│   ├── TransferRequestDto.java
│   ├── SuggestedSourceDto.java
│   └── CashPositionDto.java / BranchDto.java (local mirrors)
├── client/
│   ├── BranchServiceClient.java
│   └── CashManagementServiceClient.java
├── controller/
│   └── CashRequirementController.java
└── service/
    ├── NearestBranchLocator.java   ← haversine distance logic
    ├── CashRequirementService.java (interface)
    └── CashRequirementServiceImpl.java
```

## 8.2 Entity: TransferRequest.java

```java
package com.bankdemo.cashrequirementservice.entity;
 
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "TRANSFER_REQUEST")
public class TransferRequest {
 
    @Id
    @Column(name = "REQUEST_ID", length = 36)
    private String requestId;
 
    @Column(name = "SOURCE_BRANCH_ID", nullable = false, length = 10)
    private String sourceBranchId;
 
    @Column(name = "DESTINATION_BRANCH_ID", nullable = false, length = 10)
    private String destinationBranchId;
 
    @Column(name = "AMOUNT", nullable = false)
    private BigDecimal amount;
 
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;   // REQUESTED | APPROVED | DISPATCHED | DELIVERED
 
    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;
 
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

## 8.3 Service: NearestBranchLocator.java — the request-flow example from Blueprint 4.3

```java
package com.bankdemo.cashrequirementservice.service;
 
import com.bankdemo.cashrequirementservice.dto.BranchDto;
import com.bankdemo.cashrequirementservice.dto.CashPositionDto;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
 
@Component
public class NearestBranchLocator {
 
    private static final double EARTH_RADIUS_KM = 6371.0;
 
    public Optional<BranchDto> findNearestSurplusBranch(
            BranchDto deficitBranch,
            List<CashPositionDto> allPositions,
            List<BranchDto> allBranches) {
 
        return allPositions.stream()
                .filter(pos -> "SURPLUS".equals(pos.getStatus()))
                .filter(pos -> !pos.getBranchId().equals(deficitBranch.getBranchId()))
                .map(pos -> allBranches.stream()
                        .filter(b -> b.getBranchId().equals(pos.getBranchId()))
                        .findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .min(Comparator.comparingDouble(candidate -> haversineKm(
                        deficitBranch.getLatitude().doubleValue(), deficitBranch.getLongitude().doubleValue(),
                        candidate.getLatitude().doubleValue(), candidate.getLongitude().doubleValue())));
    }
 
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
```

## 8.4 Controller: CashRequirementController.java

```java
package com.bankdemo.cashrequirementservice.controller;
 
import com.bankdemo.cashrequirementservice.dto.*;
import com.bankdemo.cashrequirementservice.service.CashRequirementService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
 
@RestController
@RequestMapping("/api")
public class CashRequirementController {
 
    private final CashRequirementService cashRequirementService;
 
    public CashRequirementController(CashRequirementService cashRequirementService) {
        this.cashRequirementService = cashRequirementService;
    }
 
    @GetMapping("/cash-requirement/deficit-branches")
    public List<CashPositionDto> getDeficitBranches() {
        return cashRequirementService.getDeficitBranches();
    }
 
    @GetMapping("/cash-requirement/suggest-source/{deficitBranchId}")
    public SuggestedSourceDto suggestSource(@PathVariable String deficitBranchId) {
        return cashRequirementService.suggestSourceBranch(deficitBranchId);
    }
 
    @PostMapping("/transfer-requests")
    public TransferRequestDto createRequest(@RequestBody TransferRequestDto request) {
        return cashRequirementService.createTransferRequest(request);
    }
 
    @GetMapping("/transfer-requests")
    public List<TransferRequestDto> getAllRequests() {
        return cashRequirementService.getAllRequests();
    }
 
    @PutMapping("/transfer-requests/{requestId}/status")
    public TransferRequestDto updateStatus(@PathVariable String requestId,
                                            @RequestParam String status) {
        // status must be one of REQUESTED -> APPROVED -> DISPATCHED -> DELIVERED,
        // validated in the service layer (no skipping states)
        return cashRequirementService.updateStatus(requestId, status);
    }
}
```

# 9. Simulator Service (simulator-service) — Prototype-Only

This is the service you referred to as the transaction-generator that biases specific branches toward surplus or deficit so the demo narrative is repeatable. It has no controller endpoints of consequence — it is a self-contained scheduled job that calls Cash Management Service directly. Per Blueprint Section 9, it is intentionally isolated (own app, own package, own log prefix) so it can be deleted outright once real transaction integration exists, without anyone needing to untangle it from real logic.

## 9.1 Package Structure

```java
simulator-service/src/main/java/com/bankdemo/simulatorservice/
├── SimulatorServiceApplication.java
├── client/
│   └── CashManagementServiceClient.java
├── config/
│   └── SchedulingConfig.java
└── job/
    └── TransactionSimulatorJob.java     ← the patterned-generation logic
```

## 9.2 Feign Client: CashManagementServiceClient.java

```java
package com.bankdemo.simulatorservice.client;
 
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.bankdemo.simulatorservice.dto.CashTransactionDto;
 
@FeignClient(name = "cash-management-service")   // resolved via Eureka
public interface CashManagementServiceClient {
 
    @PostMapping("/api/cash-transaction")
    void recordTransaction(@RequestBody CashTransactionDto request);
}
```

## 9.3 The Patterned Generator — TransactionSimulatorJob.java

3 of the 4 seeded branches are biased toward surplus (deposits favored ~70/30 whenever picked); BR004 (Thane) is permanently biased toward deficit (withdrawals favored ~70/30 whenever picked). Branch selection each tick is still random across all 4 — it is the *bias applied once a branch is picked* that is fixed to that branch, not a guarantee that BR004 fires every tick. Over the course of a multi-minute demo this reliably produces the same story ("Thane trending short, others trending surplus"), but individual ticks are not deterministic — worth knowing if asked to trace a single transaction rather than the overall trend.

```java
package com.bankdemo.simulatorservice.job;
 
import com.bankdemo.simulatorservice.client.CashManagementServiceClient;
import com.bankdemo.simulatorservice.dto.CashTransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
 
@Component
public class TransactionSimulatorJob {
 
    private static final Logger log = LoggerFactory.getLogger(TransactionSimulatorJob.class);
    private static final String LOG_PREFIX = "[SIMULATOR]";
 
    // same 4 seeded branches as db/ddl/05_seed_data.sql
    private static final List<String> SURPLUS_TREND_BRANCHES = List.of("BR001", "BR002", "BR003");
    private static final String DEFICIT_TREND_BRANCH = "BR004";
 
    private final CashManagementServiceClient cashManagementServiceClient;
 
    public TransactionSimulatorJob(CashManagementServiceClient cashManagementServiceClient) {
        this.cashManagementServiceClient = cashManagementServiceClient;
    }
 
    @Scheduled(fixedDelay = 5000)   // every 5 seconds, starts automatically on boot
    public void generateTransaction() {
        String branchId = pickBranch();
        boolean isDeficitBranch = DEFICIT_TREND_BRANCH.equals(branchId);
 
        // deficit-trend branch: withdrawals outweigh deposits ~70/30
        // surplus-trend branches: deposits outweigh withdrawals ~70/30
        boolean isDeposit = isDeficitBranch
                ? ThreadLocalRandom.current().nextInt(100) < 30
                : ThreadLocalRandom.current().nextInt(100) < 70;
 
        BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(5_000, 50_000));
 
        CashTransactionDto txn = new CashTransactionDto();
        txn.setBranchId(branchId);
        txn.setTxnType(isDeposit ? "DEPOSIT" : "WITHDRAWAL");
        txn.setAmount(amount);
 
        cashManagementServiceClient.recordTransaction(txn);
 
        log.info("{} {} {} of {} for branch {}", LOG_PREFIX,
                isDeposit ? "credited" : "debited", txn.getTxnType(), amount, branchId);
    }
 
    private String pickBranch() {
        // weighted: deficit branch appears in the rotation too, just biased
        // toward withdrawals when it comes up, not excluded from deposits entirely
        List<String> all = List.of("BR001", "BR002", "BR003", "BR004");
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }
}
```

## 9.3 Enabling the Scheduler — SchedulingConfig.java

```java
package com.bankdemo.simulatorservice.config;
 
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
 
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // presence of @EnableScheduling is what makes the job auto-start on boot —
    // there is no manual Start/Stop button anywhere in this service
}
```

> *Note: Log prefix [SIMULATOR] on every line from this service (configure via logback pattern in application.yml, e.g. logging.pattern.console including "[SIMULATOR]") is deliberate — it must read unambiguously as synthetic data in logs, distinct from anything Cash Management Service logs about itself.*

# 10. Inter-Service Communication Summary

Every arrow below is a Feign call resolved through Eureka at request time — no hardcoded hosts or ports anywhere in application code.

| Calling Service | Feign Client Class | Target Service | Used For |
|---|---|---|---|
| Cash Management Service | BranchServiceClient | Branch Service | read branch + threshold, PATCH reserve |
| Forecast Service | BranchServiceClient | Branch Service | read threshold for at-risk check |
| Forecast Service | CashManagementServiceClient | Cash Management Service | read recent transaction history |
| Cash Requirement Service | CashManagementServiceClient | Cash Management Service | get live positions, all branches |
| Cash Requirement Service | BranchServiceClient | Branch Service | get lat/long for distance calc |
| Simulator Service | CashManagementServiceClient | Cash Management Service | post generated transactions |

> *Note: Branch Service itself calls no other service — it sits at the bottom of the dependency graph, which is exactly why it must be first up after Eureka in the startup sequence (Section 11).*

# 11. Startup Script

Enforces the order from Blueprint Section 11: Eureka must be confirmed healthy before any business service starts (Feign registration can silently fail or delay otherwise), and the Simulator starts last since it depends on Cash Management Service being registered and reachable.

**start-all.sh**

```bash
#!/bin/bash
set -e
 
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$BASE_DIR/logs"
mkdir -p "$LOG_DIR"
 
echo "[1/5] Starting Eureka Server..."
nohup java -jar "$BASE_DIR/eureka-server/target/eureka-server.jar" \
    > "$LOG_DIR/eureka-server.log" 2>&1 &
 
echo "[2/5] Waiting for Eureka Server health check..."
until curl -sf http://localhost:8761/actuator/health > /dev/null; do
    sleep 2
    echo "      ...still waiting on Eureka"
done
echo "      Eureka Server is healthy."
 
echo "[3/5] Starting Branch, Cash Management, Forecast, Cash Requirement services in parallel..."
nohup java -jar "$BASE_DIR/branch-service/target/branch-service.jar" \
    > "$LOG_DIR/branch-service.log" 2>&1 &
nohup java -jar "$BASE_DIR/cash-management-service/target/cash-management-service.jar" \
    > "$LOG_DIR/cash-management-service.log" 2>&1 &
nohup java -jar "$BASE_DIR/forecast-service/target/forecast-service.jar" \
    > "$LOG_DIR/forecast-service.log" 2>&1 &
nohup java -jar "$BASE_DIR/cash-requirement-service/target/cash-requirement-service.jar" \
    > "$LOG_DIR/cash-requirement-service.log" 2>&1 &
 
echo "[4/5] Waiting for Cash Management Service to register with Eureka..."
until curl -sf http://localhost:8761/eureka/apps/CASH-MANAGEMENT-SERVICE | grep -q "UP"; do
    sleep 2
    echo "      ...still waiting on cash-management-service registration"
done
echo "      cash-management-service is UP in Eureka."
 
echo "[5/5] Starting Simulator Service..."
nohup java -jar "$BASE_DIR/simulator-service/target/simulator-service.jar" \
    > "$LOG_DIR/simulator-service.log" 2>&1 &
 
echo "All 6 processes launched. Tail logs in $LOG_DIR to confirm."
```

# 12. Oracle JET Frontend

Two live pages backed by real REST calls, three hardcoded mock pages. Both the sidebar's Cash Management tab and the Home dashboard's Cash Management bento box route to the same live Cash Management module — there is only one real Cash Management experience, just two entry points into it.

## 12.1 Project Structure

```bash
ojet-client/src/
├── js/
│   ├── appController.js
│   ├── main.js
│   ├── viewModels/
│   │   ├── home.js               ← Bento grid, LIVE
│   │   ├── cash-management.js    ← LIVE, sub-views below
│   │   │   ├── branch-position.js
│   │   │   ├── forecast.js
│   │   │   ├── transfer-board.js
│   │   │   └── threshold-config.js
│   │   ├── customers.js          ← MOCK
│   │   ├── reports.js            ← MOCK
│   │   └── settings.js           ← MOCK
│   └── services/
│       └── apiClient.js          ← wraps fetch() to the 5 live services
└── views/
    ├── home.html
    ├── cash-management.html
    ├── customers.html
    ├── reports.html
    └── settings.html
```

## 12.2 Home — Bento Grid (LIVE)

home.js pulls live data for the Cash Management bento box from Cash Management Service (via apiClient) and Forecast Service for the trend arrow. This is the only bento box on the Home page that makes a real network call on load.

**js/viewModels/home.js**

```javascript
define(['knockout', 'ojs/ojcontext', '../services/apiClient'],
  function (ko, Context, apiClient) {
 
    function HomeViewModel() {
      var self = this;
 
      // Cash Management bento box — the ONE live tile
      self.cashStatus = ko.observable('Loading...');
      self.cashAmount = ko.observable(null);
      self.cashPercentOfThreshold = ko.observable(null);
      self.forecastTrendArrow = ko.observable('flat');
 
      apiClient.getAllCashPositions().then(function (positions) {
        var summary = positions[0]; // aggregate/first branch for the summary tile
        self.cashStatus(summary.status);            // 'SURPLUS' | 'DEFICIT'
        self.cashAmount(summary.surplusOrDeficitAmount);
        self.cashPercentOfThreshold(
          (summary.currentReserve / summary.thresholdAmount * 100).toFixed(1));
 
        // chained off the same summary branch, not a second independent call —
        // the trend arrow must reflect the same branch the tile is showing
        return apiClient.getForecast(summary.branchId);
      }).then(function (forecast) {
        self.forecastTrendArrow(
          forecast.predictedPosition > forecast.currentPosition ? 'up' : 'down');
      });
 
      // remaining bento boxes are static — see Section 12.4
      self.customersMockLabel = 'Mock view — illustrative of production behavior';
      self.reportsMockLabel = 'Mock view — illustrative of production behavior';
      self.settingsMockLabel = 'Mock view — illustrative of production behavior';
    }
 
    return new HomeViewModel();
  }
);
```

**views/home.html — bento grid markup (structure only)**

```html
<div class="oj-flex bento-grid">
 
  <!-- LIVE: Cash Management tile, largest card -->
  <a href="#cash-management" class="bento-tile bento-tile--large bento-tile--live">
    <h3>Cash Management</h3>
    <span data-bind="text: cashStatus" class="status-pill"></span>
    <p data-bind="text: cashAmount"></p>
    <p data-bind="text: cashPercentOfThreshold() + '% of threshold'"></p>
    <span class="trend-arrow" data-bind="css: forecastTrendArrow"></span>
  </a>
 
  <!-- MOCK: Customers tile -->
  <div class="bento-tile bento-tile--mock">
    <h3>Customers</h3>
    <p class="mock-content">142 active accounts (sample)</p>
    <span class="mock-disclaimer" data-bind="text: customersMockLabel"></span>
  </div>
 
  <!-- MOCK: Reports tile -->
  <div class="bento-tile bento-tile--mock">
    <h3>Reports</h3>
    <p class="mock-content">Monthly summary (sample)</p>
    <span class="mock-disclaimer" data-bind="text: reportsMockLabel"></span>
  </div>
 
  <!-- MOCK: Settings tile -->
  <div class="bento-tile bento-tile--mock">
    <h3>Settings</h3>
    <p class="mock-content">Branch preferences (sample)</p>
    <span class="mock-disclaimer" data-bind="text: settingsMockLabel"></span>
  </div>
 
</div>
```

## 12.3 Cash Management Module (LIVE)

Reached identically from the sidebar's Cash Management tab and from the Home bento tile above. Four sub-views, matching Blueprint Section 12 exactly:

Branch Position — live reserve vs. threshold per branch, recent transaction feed (calls GET /api/cash-position and GET /api/cash-transaction/{branchId}/recent)

Forecast — 14-day trend chart + tomorrow's prediction with confidence band (calls GET /api/forecast/{branchId})

Transfer Requests board — kanban Requested / Approved / Dispatched / Delivered (calls GET/POST/PUT on /api/transfer-requests)

Threshold Configuration — open to all users in this prototype, with an inline disclaimer that it would be manager-restricted in real production (calls PUT /api/branches/{branchId}/threshold)

**js/viewModels/cash-management/branch-position.js**

```javascript
define(['knockout', '../../services/apiClient'], function (ko, apiClient) {
  function BranchPositionViewModel() {
    var self = this;
    self.positions = ko.observableArray([]);
    self.recentTransactions = ko.observableArray([]);
 
    apiClient.getAllCashPositions().then(self.positions);
 
    self.selectBranch = function (branchId) {
      apiClient.getRecentTransactions(branchId).then(self.recentTransactions);
    };
  }
  return new BranchPositionViewModel();
});
```

## 12.4 Mock Sections — Customers, Reports, Settings

Same file/folder pattern as the live pages so the app doesn't visually or structurally telegraph which sections are real, but the view-model has no apiClient calls at all — every value is a hardcoded literal, and every view carries the same small grey disclaimer text pattern used on the Home bento tiles.

**js/viewModels/customers.js**

```javascript
define(['knockout'], function (ko) {
  function CustomersViewModel() {
    var self = this;
 
    // Hardcoded — no apiClient import, no network call, by design.
    self.customers = ko.observableArray([
      { name: 'Rohan Mehta', accountType: 'Savings', balance: 214500 },
      { name: 'Priya Nair', accountType: 'Current', balance: 892100 },
      { name: 'Sameer Iyer', accountType: 'Savings', balance: 45200 }
    ]);
 
    self.mockDisclaimer =
      'Mock view — illustrative of production behavior, not backed by live data.';
  }
  return new CustomersViewModel();
});
```

**views/customers.html**

```html
<div class="oj-panel">
  <h2>Customers</h2>
  <span class="mock-disclaimer" data-bind="text: mockDisclaimer"></span>
 
  <table class="oj-table" data-bind="foreach: customers">
    <tr>
      <td data-bind="text: name"></td>
      <td data-bind="text: accountType"></td>
      <td data-bind="text: balance"></td>
    </tr>
  </table>
</div>
```

> *Note: reports.js and settings.js follow the identical pattern: hardcoded observableArray/literals, no apiClient import, one mockDisclaimer string rendered in small grey text (CSS class .mock-disclaimer — suggest color: #888, font-size: 0.75rem, defined once in app.css and reused across all three mock views and the three mock bento tiles).*

## 12.5 Sidebar / Routing

appController.js defines the router with 5 named views: home, cash-management, customers, reports, settings — matching the sidebar items from Blueprint Section 12. cash-management is reachable both as its own top-level route and as the destination of the Home bento tile's link, so there is exactly one implementation of that module, entered two ways.

**js/appController.js — router config excerpt**

```javascript
self.router = CoreRouter([
  { path: '', redirect: 'home' },
  { path: 'home', detail: { label: 'Home', iconClass: 'oj-ux-ico-bar-chart' } },
  { path: 'cash-management', detail: { label: 'Cash Management', iconClass: 'oj-ux-ico-money' } },
  { path: 'customers', detail: { label: 'Customers', iconClass: 'oj-ux-ico-contact-group' } },
  { path: 'reports', detail: { label: 'Reports', iconClass: 'oj-ux-ico-reports' } },
  { path: 'settings', detail: { label: 'Settings', iconClass: 'oj-ux-ico-settings' } }
], { history: 'skip' });
```

# 13. Appendix — REST Endpoint Reference

| Service | Method | Path |
|---|---|---|
| Branch Service | GET | /api/branches |
| Branch Service | GET | /api/branches/{branchId} |
| Branch Service | PUT | /api/branches/{branchId}/threshold |
| Branch Service | PATCH | /api/branches/{branchId}/reserve |
| Branch Service | POST | /api/auth/login |
| Cash Management Service | GET | /api/cash-position |
| Cash Management Service | GET | /api/cash-position/{branchId} |
| Cash Management Service | POST | /api/cash-transaction |
| Cash Management Service | GET | /api/cash-transaction/{branchId}/recent |
| Forecast Service | GET | /api/forecast/{branchId} |
| Cash Requirement Service | GET | /api/cash-requirement/deficit-branches |
| Cash Requirement Service | GET | /api/cash-requirement/suggest-source/{branchId} |
| Cash Requirement Service | POST | /api/transfer-requests |
| Cash Requirement Service | GET | /api/transfer-requests |
| Cash Requirement Service | PUT | /api/transfer-requests/{requestId}/status |

> *Note: PATCH /api/branches/{branchId}/reserve is implemented in Section 5.6 (BranchController) and Section 5.8 (BranchServiceImpl) — included above in full since it's the one endpoint every other service's write path depends on.*
