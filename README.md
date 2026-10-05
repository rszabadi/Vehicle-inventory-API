# Vehicle Inventory API

![CI](https://github.com/rszabadi/swiss/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![License](https://img.shields.io/badge/License-MIT-blue)

A REST API for managing a car dealer's vehicle inventory, built with Spring Boot and PostgreSQL and run with Docker Compose. It's a learning project that models the kind of data I handle at work (vehicle listings), using sample data only.
## Tech stack

- Java 21, Spring Boot 4.1.1 (Spring Web, Spring Data JPA)
- PostgreSQL 17
- Maven
- Docker and Docker Compose
- JUnit 5 and Mockito for tests

## Run it

Requirements: Docker with the Compose plugin.

```bash
git clone https://github.com/rszabadi/swiss.git
cd swiss
cp .env.example .env
# edit .env and set your own DB_PASSWORD
docker compose up -d --build
```

The API is then available at `http://localhost:8080`.

```bash
curl localhost:8080/vehicles
```

Stop it with `docker compose down`. The database data is kept in a Docker volume.

## API

| Method | Path | Description |
|---|---|---|
| GET | `/vehicles` | List all vehicles |
| GET | `/vehicles/{id}` | Get one vehicle (404 if missing) |
| POST | `/vehicles` | Create a vehicle (400 if invalid) |
| PUT | `/vehicles/{id}` | Update a vehicle |
| DELETE | `/vehicles/{id}` | Delete a vehicle |

Example request:

```bash
curl -X POST localhost:8080/vehicles \
  -H "Content-Type: application/json" \
  -d '{"brand":"Toyota","model":"Celica","year":1994,"km":41000,"price":11500}'
```

## Tests

```bash
docker compose up -d db
./mvnw test
```

The tests need the database running locally. CI runs them automatically on every push.

## Roadmap

- [x] CRUD endpoints with input validation
- [x] Docker Compose setup
- [x] Integration tests
- [x] CI with GitHub Actions
- [x] Database migrations (Flyway)
- [ ] Logging and database backups
- [ ] Deployment on a Linux server with a reverse proxy and HTTPS
