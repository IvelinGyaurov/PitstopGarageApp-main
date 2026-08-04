# PitStop Garage
**Final Project – Spring Advanced**  
SoftUni Java Web Development Track

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring MVC](https://img.shields.io/badge/Spring_MVC-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Feign](https://img.shields.io/badge/Feign_Client-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![i18n](https://img.shields.io/badge/i18n_EN%2FBG-4479A1?style=for-the-badge)

---

## 1. Project Overview

**PitStop Garage** is a two-application platform for managing an auto-repair service workflow.

Clients register vehicles, submit repair requests, track progress, and review service history.  
Mechanics work the repair queue, accept jobs, add used parts, and complete repairs.  
Administrators manage users, inventory, and all repair activity across the garage.

The platform consists of two independent Spring Boot applications:

### 1) Main Application (this repository) — Customer, Mechanic & Admin Portal
- Role-based Thymeleaf UI for **USER**, **MECHANIC**, and **ADMIN**
- Cars, repair requests, repair details, and service history
- Admin user management and parts inventory screens
- EN/BG internationalization (cookie-based locale)
- Communicates with the parts microservice via **OpenFeign**

### 2) REST Microservice — Spare Parts Inventory
- Separate repository: [pitstop-parts](https://github.com/IvelinGyaurov/pitstop-parts)
- Owns the parts catalog (MySQL) and exposes REST endpoints
- Supports create / list / soft-delete / stock deduction
- Consumed by the main app through `PartsClient`

### Technologies & Architecture Highlights
- **Spring Security** with role-based access control
- **OpenFeign** for inter-service REST communication
- **Spring Scheduling** for stale / expired repair cleanup
- **Spring Cache** for user lookups
- **DTO validation**, custom exceptions, and global exception handling
- **JaCoCo** test coverage for the main application

---

## 2. SoftUni Requirements Compliance

### 2.1 Technology Stack

**Backend**
- Java 17
- Spring Boot 3.4.0
- Spring MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Scheduling
- Spring Cache
- OpenFeign
- Spring Actuator

**Frontend**
- Thymeleaf (+ Spring Security extras)
- Custom CSS / JS
- Cookie-based i18n (EN / BG)

**Database**
- MySQL (`pitstop_garage`)

**Tooling**
- Maven
- JaCoCo
- Git + GitHub

---

### 2.2 Project Architecture (Two Applications)

| Application | Port | Responsibility |
|-------------|------|----------------|
| **PitStop Garage (main)** | `8080` | Auth, users, cars, repairs, UI |
| **PitStop Parts (microservice)** | `8081` | Parts inventory REST API |

Main app Feign client base URL: `http://localhost:8081`

---

### 2.3 Domain Entities (Main Application — MySQL)

- **User**
- **Car**
- **ServiceRepair**
- **UsedPart**

> Spare **Part** entities live in the microservice database (`pitstop_parts`), not in the main app schema.

**ID Strategy**
- UUID identifiers
- Each entity has its own Repository and Service layer

**Repair statuses**
`PENDING` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`  
also: `CANCELLED`, `USER_CANCELLED`, `EXPIRED`

---

### 2.4 Web Pages

Dynamic pages include:
- Index / Home
- Login / Register
- Profile menu
- Cars (list / add)
- Repair request, repairs list, repair details
- Client service history (completed / cancelled / expired)
- Mechanic queue, accepted repairs, complete repair, mechanic history
- Admin repairs overview + admin history
- Admin parts inventory + add part
- Users management
- Custom error page

---

### 2.5 REST Microservice Integration (via Feign)

| Call from main app | Description |
|--------------------|-------------|
| `GET /api/parts` | List inventory |
| `POST /api/parts` | Create part |
| `DELETE /api/parts/{id}` | Soft-delete part |
| `POST /api/parts/deduct` | Deduct stock when completing a repair |

Full microservice details are documented in the [pitstop-parts](https://github.com/IvelinGyaurov/pitstop-parts) README.

---

### 2.6 Main Functionalities

- Register / login / logout
- Manage own cars (create / delete)
- Create repair request for a car
- Cancel pending repair (client)
- Mechanic: accept / reject queue requests
- Mechanic: start repair, add used parts, complete repair
- Admin: manage users (activate / deactivate / change role)
- Admin: manage parts inventory through the microservice
- Admin / mechanic / client: view repair history (incl. expired)
- Release stale ACCEPTED repairs back to queue (scheduled)
- Expire old PENDING repairs (scheduled)
- EN/BG language switch (persists via cookie)
- Custom confirm dialogs for destructive actions

---

### 2.7 Security & Roles

| Feature / Action | USER | MECHANIC | ADMIN |
|------------------|:---:|:--------:|:-----:|
| Register & Login | ✔ | ✔ | ✔ |
| Manage own cars | ✔ | ✖ | ✔* |
| Create repair request | ✔ | ✖ | ✔* |
| View own repairs / history | ✔ | assigned/all | ✔ |
| Accept / reject repair queue | ✖ | ✔ | ✖ |
| Complete repair + deduct parts | ✖ | ✔ | ✖ |
| Manage users | ✖ | ✖ | ✔ |
| Manage parts inventory | ✖ | ✖ | ✔ |
| View all repairs | ✖ | ✖ | ✔ |

\* Admin UI focuses on garage management; client car/repair flows are primarily for **USER**.

**Role notes**
- **USER** — client portal
- **MECHANIC** — repair queue and job completion
- **ADMIN** — users, inventory, full repair oversight
- Last active admin cannot be demoted/deactivated (protection rule)
- Passwords stored with BCrypt

---

### 2.8 Database

- MySQL database: `pitstop_garage`
- UUID primary keys
- Complex relationships: User ↔ Car ↔ ServiceRepair ↔ UsedPart
- Parts stock is owned by the microservice DB

---

### 2.9 Validation & Error Handling

- Bean Validation on DTOs / forms
- Custom domain exceptions
- Global `@ControllerAdvice` (`ExceptionAdvice`)
- Custom error page (no white-label for handled flows)
- Flash messages + i18n message keys

---

### 2.10 Scheduling & Caching

**Scheduling**
- Fixed-delay job: release ACCEPTED repairs not started within 7 days
- Monthly cron: expire PENDING repairs older than 30 days (`EXPIRED`)

**Caching**
- Spring Cache on user lookups (`@Cacheable`)

---

### 2.11 Testing

- Unit tests
- Controller / web-layer tests
- Service tests
- JaCoCo configured for line and branch coverage

---

### 2.12 Code Quality

- Thin controllers
- Feature-based package structure (`user`, `car`, `repair`, `parts`, `web`, …)
- Clear service / repository separation
- No dead code / unused imports (kept clean during development)

---

## 3. How to Run

### 3.1 Prerequisites
- JDK 17+
- Maven
- MySQL running locally
- Parts microservice running on port **8081**

### 3.2 Clone repositories
```bash
git clone https://github.com/IvelinGyaurov/PitstopGarageApp-main.git
git clone https://github.com/IvelinGyaurov/pitstop-parts.git
```

### 3.3 Configure MySQL
Default main-app settings (`application.properties`):
- URL: `jdbc:mysql://localhost:3306/pitstop_garage?createDatabaseIfNotExist=true`
- Username: `root`
- Password: `root`

Adjust if your local MySQL credentials differ.

### 3.4 Start the Parts microservice
```bash
cd pitstop-parts
mvn spring-boot:run
```
Runs on **http://localhost:8081**

### 3.5 Start the Main application
```bash
cd PitstopGarageApp-main
mvn spring-boot:run
```
Runs on **http://localhost:8080**

### 3.6 Open the app
Visit: [http://localhost:8080](http://localhost:8080)

---

## 4. Author

**Ivelin Petkov Gyaurov**  
SoftUni Java Developer Track (2024-2026)
