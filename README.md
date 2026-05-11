# Bank Licensing & Compliance Portal

End-to-end MVP for supervising **commercial bank licence applications** (National Bank of Rwanda–style regulator workflow): JWT auth, role-based REST API enforcement, workflow state machine, append-only audit log, versioned applicant documents (simulated disk storage), and a small React UI.

## Prerequisites

- **Java 17** and **Maven 3**
- **Node.js 20+** and **npm**
- **Docker** (optional, for Postgres + API)

## Quick start (local)

1. **Postgres** (or use Docker Compose only for the DB):

   ```bash
   docker run --name bnr-pg -e POSTGRES_USER=bnr -e POSTGRES_PASSWORD=bnr -e POSTGRES_DB=licensing -p 5432:5432 -d postgres:16-alpine
   ```

2. **Backend** (from `backend/`):

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   - API: `http://localhost:8080`
   - OpenAPI JSON: `http://localhost:8080/v3/api-docs`
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`

3. **Frontend** (from `frontend/`):

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`. The dev server **proxies** `/api` and `/v3` to the backend.

## Docker Compose (API + database)

Build and run (builds the JAR inside Docker):

```bash
docker compose up --build
```

API on `http://localhost:8080`. For the UI, run the Vite dev server separately (see above) and point it at the same API, or build static assets and host them as you prefer.

## Seeded demo users

On first startup with an **empty** database, the backend seeds:

| Email                 | Role               | Password              |
|-----------------------|--------------------|-----------------------|
| `applicant@bnr.rw`    | `APPLICANT`        | `tentativepassword`   |
| `reviewer@bnr.rw`   | `REVIEW_OFFICER`   | `tentativepassword`   |
| `approver@bnr.rw`   | `APPROVAL_OFFICER` | `tentativepassword`   |

Sample applications use Rwandan microfinance-style names: **Urwego Bank** (under review), **Letshego** (pending approval after review by `reviewer@bnr.rw`, so **`approver@bnr.rw`** can approve but **`reviewer@bnr.rw`** cannot), plus **Unguka Bank**, **AB Bank**, and **CoPEDU** as newly submitted cases.

## API documentation (Postman)

Import OpenAPI:

1. Run the backend.
2. Download `http://localhost:8080/v3/api-docs` and save as `openapi.json`, or paste the URL into Postman **Import → Link**.

## Tests

```bash
cd backend
mvn test
```

Includes:

- State machine transitions (unit).
- Authorisation boundaries (same reviewer cannot approve; different approver can).
- Concurrent final approvals: optimistic locking yields **exactly one** conflict; final state stays consistent.

## Project layout

- `backend/` — Spring Boot 3, JPA, Flyway, JWT, PostgreSQL.
- `frontend/` — React + TypeScript + Vite.
- `BNR Licensing & Compliance Portal (1).pdf` — architecture, data model, state machine, roles, audit, trade-offs.

## Configuration

| Variable / property              | Meaning                           |
|----------------------------------|-----------------------------------|
| `SPRING_DATASOURCE_*`          | JDBC URL, user, password          |
| `PORTAL_TOKENS_SECRET`       | HS256 signing secret (≥ 32 bytes) |
| `PORTAL_FILES_UPLOAD_DIR`      | Folder for uploaded files (simulated storage) |

YAML equivalents: `portal.tokens.secret`, `portal.files.upload-dir`.

Max upload size is **5MB** per file (enforced by Spring and `DocumentUploads`).

---

See **BNR Licensing & Compliance Portal (1).pdf** for how each assessment requirement is met and what would be extended with more time.
