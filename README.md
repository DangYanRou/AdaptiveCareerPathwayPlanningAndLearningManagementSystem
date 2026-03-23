# Adaptive Career Pathway and Learning Management System

The **Adaptive Career Pathway and Learning Management System** is a full-stack platform designed to support learning paths and career development. It combines a modern **Angular frontend** with a **Spring Boot REST API backend**, emphasizing modularity, scalability, and clean separation of concerns.

---

## Project Overview

The system is divided into two major components:

* **Frontend (Angular)** – A responsive user interface for learners, managers, and administrators.
* **Backend (Spring Boot)** – A secure REST API that manages business logic, persistence, and integrations.

Each layer is structured for maintainability and clear responsibilities.

---

## Frontend (Angular)

Located in `frontend/src/app/`, the Angular app follows a feature-oriented structure:

* **`components/`** – Reusable UI elements (buttons, modals, widgets).
* **`guards/`** – Route guards enforcing authentication and authorization.
* **`interceptors/`** – Global HTTP request/response handling (e.g., centralized error handling).
* **`models/`** – TypeScript interfaces for API communication.
* **`pages/`** – Full route views composed of smaller components.
* **`pipes/`** – Custom pipes for data transformation in templates.
* **`providers/`** – Dependency injection providers.
* **`services/`** – API interaction and business logic for the frontend.
* **`_theme.scss`** – Custom color palette and styling.
* **`assets/`** – Static assets such as images and transcripts.
* **`environments/`** – Environment-specific configs (`environment.ts` for dev, `environment.uat.ts` for UAT).

---

## Backend (Spring Boot REST API)

Located in `backend/src/main/java/com/tbm/careerpathlearning/`, the backend follows a layered architecture:

* **`aspect/`** – Cross-cutting concerns (e.g., logging).
* **`config/`** – Application configuration (e.g., security setup).
* **`controller/`** – REST controllers exposing endpoints and handling requests.
* **`dto/`** – Data Transfer Objects defining request/response payloads.
* **`enums/`** – Enumerations for predefined values.
* **`exception/`** – Custom exception classes and global error handling.

  * `GlobalExceptionHandler.java` – Centralized exception management.
* **`mapper/`** – Entity ↔ DTO mapping logic.
* **`model/`** – JPA entities mapped to database tables.
* **`repository/`** – Spring Data JPA repositories for persistence operations.
* **`service/`** – Business logic interfaces.

  * **`impl/`** – Service implementations coordinating repositories and workflows.
* **`resources/`**

  * `data-upload-template/` – templates of bulk import features.
  * `db/migration` – sql scripts for Flyway migration.
  * `static/` – Static files.
  * `*.yml` – Environment-specific configs (dev, uat).
  * * `logback-spring.xml` – Logs files configuration.
  * `messages.properties` – System transcripts and localized messages.

---

## Getting Started

### Prerequisites

Before running the project, ensure you have installed:

* **JDK 17+**
* **Apache Maven 3.x** (or use the included `./mvnw` wrapper)
* **Node.js (LTS version)**
* **Angular CLI** (`npm install -g @angular/cli`)

---

### Backend Setup

1. **Navigate to the backend folder:**

   ```bash
   cd backend
   ```

2. **Set up environment variables:**

   ```bash
   cp .env.example .env
   ```

   Edit `.env` with your database credentials and other settings.

3. **Install dependencies and build the project:**

   ```bash
   ./mvnw clean install
   ```

4. **Run the Spring Boot server:**

   ```bash
   ./mvnw spring-boot:run
   ```

   By default, it runs at: [http://localhost:8081](http://localhost:8081).

---

### Frontend Setup

1. **Navigate to the frontend folder:**

   ```bash
   cd frontend
   ```

2. **Install dependencies:**

   ```bash
   npm install
   # or
   yarn install
   ```

3. **Start the Angular development server:**

   ```bash
   ng serve
   ```

   The app runs at: [http://localhost:4200](http://localhost:4200).
