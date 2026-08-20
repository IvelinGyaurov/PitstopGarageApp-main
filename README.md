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
- Supports create / list / soft-delete / stock deduction / restock
- Consumed by the main app through `PartsClient`

### Technologies & Architecture Highlights
- **Spring Security** with role-based access control
- **OpenFeign** for inter-service REST communication
- **Spring Scheduling** for stale / expired repair cleanup
- **Spring Cache** for user lookups
- **Spring Events** on repair completion (listener logs completion details)
- **Spring AOP** for service-layer execution logging
- **NHTSA VPIC REST API** for optional VIN decode on the add-car form (brand / model / year)
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
- Spring AOP
- OpenFeign
- Spring Actuator
- OpenPDF (completed-repair invoices)

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
- **UsedPart** (child entity — see note below)

> Spare **Part** entities live in the microservice database (`pitstop_parts`), not in the main app schema.

**ID Strategy**
- UUID identifiers
- **User**, **Car**, and **ServiceRepair** each have their own Repository and Service layer

**About `UsedPart` (child entity)**  
`UsedPart` is intentionally modeled as a **child entity** of `ServiceRepair`, not as a standalone aggregate. It stores which spare parts were used on a completed repair (snapshot of part id, name, quantity, and unit price). Lifecycle is managed through the parent repair via `@OneToMany` with `CascadeType.ALL` and `orphanRemoval = true`. For that reason `UsedPart` has **no** dedicated Repository or Service — create/read happens through `RepairService` / `ServiceRepair`. This still satisfies the “minimum 3 entities with repository and service” expectation via **User**, **Car**, and **ServiceRepair**.

**Repair statuses**
`PENDING` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`  
also: `CANCELLED`, `USER_CANCELLED`, `EXPIRED`

---

### 2.4 Web Pages

Dynamic pages include:
- Index / Home
- Login / Register
- Profile menu
- Cars (list / add — with random sample-car preset on the add form)
- Repair request, repairs list, repair details
- Client service history (completed / cancelled / expired)
- Mechanic queue, accepted repairs, complete repair, mechanic history
- Admin repairs overview + admin history
- Admin parts inventory, add part (with random sample-part preset), and restock
- Users management
- Custom error page

---

### 2.5 REST Microservice Integration (via Feign)

| Call from main app | Description |
|--------------------|-------------|
| `GET /api/parts` | List inventory |
| `GET /api/parts/{id}` | Get one part |
| `POST /api/parts` | Create part |
| `POST /api/parts/{id}/restock` | Add quantity to an existing part |
| `DELETE /api/parts/{id}` | Soft-delete part |
| `POST /api/parts/deduct` | Deduct stock when completing a repair |

Full microservice details are documented in the [pitstop-parts](https://github.com/IvelinGyaurov/pitstop-parts) README.

---

### 2.6 Main Functionalities

- Register / login / logout
- Manage own cars (create / delete)
- Soft-deleted car VIN stays unique forever — the same VIN cannot be reused for a new car
- Cannot delete a car that has an active repair (`PENDING` / `ACCEPTED` / `IN_PROGRESS`)
- Create repair request for a car
- Cancel pending repair (client)
- Mechanic: accept / reject queue requests
- Mechanic: start repair, add used parts, complete repair
- Mechanic complete: labor cost is required (minimum **1.00 EUR**); used parts are optional
- Out-of-stock parts are disabled on the complete-repair form (cannot be selected)
- Admin: manage users (activate / deactivate / change role)
- Admin: manage parts inventory through the microservice
- Admin: **restock** an existing part when stock reaches 0 (or any quantity) — name, SKU, and price stay the same; only quantity is increased (no need to recreate the same SKU)
- Demo helpers: random **sample car** preset on car add, and random **sample part** preset on part add, to fill forms quickly during testing/demo
- Client: own repair history (completed / cancelled / expired)
- Mechanic history: own completed / rejected; **expired = all** garage expired requests (same shared queue idea as PENDING)
- Admin: full repair history (incl. all expired)
- Release stale ACCEPTED repairs back to queue (scheduled)
- Expire old PENDING repairs (scheduled)
- EN/BG language switch (persists via cookie)
- Custom confirm dialogs for destructive actions
- Download PDF invoice for **COMPLETED** repairs (EN/BG follows UI language); client and mechanic for own jobs, admin for any completed repair
- **Bonus:** Spring Events published when a mechanic completes a repair; listener records completion details in the log
- **Bonus:** AOP advice logs execution time for service methods (car, repair, user, parts admin)
- **Bonus:** NHTSA VIN decode on add car — optional lookup fills brand, model, and year via `RestClient` (no API key)

---

### 2.7 Security & Roles

| Feature / Action | USER | MECHANIC | ADMIN |
|------------------|:---:|:--------:|:-----:|
| Register & Login | ✔ | ✔ | ✔ |
| Manage own cars (`/cars`) | ✔ | ✖ | ✔ |
| Create repair request (`/repairs`) | ✔ | ✖ | ✔ |
| View own repairs / history | ✔ | own completed/rejected; all expired | ✔ (own + admin overview) |
| Accept / reject repair queue | ✖ | ✔ | ✖ |
| Complete repair + deduct parts | ✖ | ✔ | ✖ |
| Manage users | ✖ | ✖ | ✔ |
| Manage parts inventory | ✖ | ✖ | ✔ |
| View all repairs | ✖ | ✖ | ✔ |

\* `/cars` and `/repairs` are restricted to **USER** and **ADMIN** (`@PreAuthorize`). **MECHANIC** cannot access those client portals.

**Role notes**
- **USER** — client portal (cars + repair requests + own history)
- **MECHANIC** — repair queue and job completion (`/mechanic/**` only for repair work)
- **ADMIN** — users, inventory, full repair oversight, plus the same client car/repair flows as USER
- There are no seeded accounts. The first registered user becomes **ADMIN**; later registrations are **USER**. Use the Users page as admin to promote a mechanic or another admin.
- Last active admin cannot be demoted/deactivated (protection rule)
- Cannot deactivate a user who still has **ACCEPTED** or **IN_PROGRESS** repairs assigned as mechanic
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
- Access denied is shown as the custom error page with **HTTP 404** (not 500)
- Flash messages + i18n message keys

---

### 2.10 Logging

Log statements around:
- Car create / soft-delete
- Repair request, cancel, accept, reject, start, complete
- Parts admin create / restock / soft-delete (via Feign flows)
- Scheduled release of stale ACCEPTED repairs and daily expire of PENDING
- Domain / validation error paths where relevant
- PDF invoice download for completed repairs

---

### 2.11 Scheduling & Caching

**Scheduling**
- Fixed-delay job: release ACCEPTED repairs not started within 7 days
- Daily cron (`0 0 0 * * *`): expire PENDING repairs older than 30 days (`EXPIRED`)

**Caching**
- Spring Cache on user lookups (`@Cacheable`)

---

### 2.12 Testing

- Unit tests
- Controller / web-layer tests
- Service tests
- JaCoCo configured for line and branch coverage

---

### 2.13 Code Quality

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
