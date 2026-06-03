# Project Commands Reference

Quick reference for every command you need to build, run, and manage this project.

---

## Prerequisites

This project uses the system Maven (`mvn`), not a wrapper — there is no `mvnw`/`mvnw.cmd` in the repo. Make sure you have:

- Java 21
- Maven 3.9+ (`mvn -version`)
- Docker (`docker --version`)

## Initial Setup (Run Once)

```bash
# 1. Start PostgreSQL + Redis
docker compose up -d

# 2. Verify containers are running
docker ps

# 3. Run the application
mvn spring-boot:run
```

Open API docs: http://localhost:8080/swagger-ui.html

---

## Daily Development Workflow

```bash
# Start containers (every day)
docker compose up -d

# Run the app
mvn spring-boot:run

# Stop containers (end of day)
docker compose down
```

---

## Docker — Containers

```bash
# Start all services (detached/background)
docker compose up -d

# Start and watch logs at same time
docker compose up

# Stop all services
docker compose down

# Stop and delete all data (fresh start)
docker compose down -v

# Restart all services
docker compose restart

# Restart one service only
docker compose restart postgres
docker compose restart redis
```

---

## Docker — Logs

```bash
# Watch all logs live
docker compose logs -f

# Watch logs for one service only
docker compose logs -f postgres
docker compose logs -f redis

# Save logs to a file
docker logs h1b_navigator_db > db.log 2>&1
```

---

## Docker — Status & Monitoring

```bash
# See running containers
docker ps

# See ALL containers including stopped ones
docker ps -a

# Live CPU and memory usage
docker stats

# Compact stats view (no stream)
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.CPUPerc}}"

# Get container IP address
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' h1b_navigator_db

# Check disk usage
docker system df
```

---

## Docker — Enter Containers

```bash
# Open PostgreSQL shell
docker exec -it h1b_navigator_db psql -U postgres -d h1b_navigator

# Open Redis shell
docker exec -it h1b_navigator_redis redis-cli

# Open bash inside any container
docker exec -it h1b_navigator_db /bin/bash

# Quick throwaway container for debugging
docker run --rm -it alpine /bin/sh
```

---

## Docker — Images

```bash
# Build project image
docker build -t h1b-career-navigator:1.0 .

# List all images
docker images

# Pull an image
docker pull postgres:15

# Remove an image
docker rmi h1b-career-navigator:1.0

# Tag image for pushing to registry
docker tag h1b-career-navigator:1.0 yourdockerhub/h1b-career-navigator:latest

# Push image to Docker Hub
docker push yourdockerhub/h1b-career-navigator:latest
```

---

## Docker — Volumes

```bash
# List all volumes
docker volume ls

# Inspect a volume
docker volume inspect h1b-career-navigator_postgres_data

# Create a volume manually
docker volume create mydata

# Remove a volume
docker volume rm mydata
```

---

## Docker — Networking

```bash
# List all networks
docker network ls

# Inspect a network
docker network inspect h1b-career-navigator_default

# Create a custom network
docker network create mynet
```

---

## Docker — Cleanup

```bash
# Stop ALL running containers
docker stop $(docker ps -q)

# Remove ALL stopped containers
docker rm $(docker ps -aq)

# Remove ALL unused images
docker image prune -a

# Remove ALL stopped containers
docker container prune

# Remove everything unused (containers, images, volumes, networks)
docker system prune --volumes

# Nuclear option — remove absolutely everything
docker system prune -a --volumes --force
```

---

## Maven — Build & Test

```bash
# Run the application
mvn spring-boot:run

# Run all tests
mvn test

# Build JAR file (skip tests)
mvn package -DskipTests

# Build JAR file (with tests)
mvn package

# Clean build artifacts
mvn clean

# Clean + rebuild
mvn clean package

# Check dependency tree
mvn dependency:tree
```

---

## Database — Flyway Migrations

```bash
# Run pending migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Repair migration checksum (if migration file changed)
mvn flyway:repair
```

---

## Database — PostgreSQL Queries (Inside psql shell)

```sql
-- Connect (run this first)
docker exec -it h1b_navigator_db psql -U postgres -d h1b_navigator

-- List all tables
\dt

-- Describe a table
\d visas
\d users
\d job_applications

-- List all active visas
SELECT * FROM visas WHERE status = 'ACTIVE';

-- Check upcoming expiries (next 90 days)
SELECT visa_type, expiry_date, expiry_date - CURRENT_DATE AS days_left
FROM visas
WHERE expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 90
AND status = 'ACTIVE'
ORDER BY expiry_date;

-- Count job applications by status
SELECT status, COUNT(*) FROM job_applications GROUP BY status;

-- Exit psql
\q
```

---

## Redis — Common Commands (Inside redis-cli shell)

```bash
# Connect
docker exec -it h1b_navigator_redis redis-cli

# Check all cached keys
KEYS *

# Get a cached value
GET "userVisas::some-user-id"

# Delete a specific cache entry
DEL "userVisas::some-user-id"

# Flush ALL cache (use carefully)
FLUSHALL

# Check memory usage
INFO memory

# Exit
exit
```

---

## Git — Daily Workflow

```bash
# Check what changed
git status

# Stage all changes
git add .

# Commit with message
git commit -m "feat: add visa expiry alert service"

# Push to GitHub
git push

# Pull latest changes
git pull

# Create a new branch
git checkout -b feature/job-tracker-module

# Switch branches
git checkout main

# Merge a branch
git merge feature/job-tracker-module
```

---

## Git — Commit Message Format

Use this format for professional commit history (interviewers check this):

```
feat: add new feature
fix: bug fix
docs: update README or comments
refactor: code change with no feature/fix
test: add or update tests
chore: dependency updates, config changes
```

Examples:
```bash
git commit -m "feat: add visa expiry 90-day SNS alert"
git commit -m "fix: circuit breaker fallback not preventing duplicate alerts"
git commit -m "docs: add ADR-002 for SQS design decision"
git commit -m "refactor: extract tax bracket logic into separate method"
git commit -m "test: add unit tests for 401k withdrawal calculator"
```

---

## Actuator — Health & Metrics (App Must Be Running)

```bash
# Health check
curl http://localhost:8080/actuator/health

# All metrics
curl http://localhost:8080/actuator/metrics

# Circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

---

## Troubleshooting

```bash
# Port 8080 already in use?
# Find what's using it:
netstat -ano | findstr :8080        # Windows
lsof -i :8080                       # Mac/Linux

# Kill the process (Windows — use PID from above)
taskkill /PID <PID> /F

# Docker container won't start?
docker compose logs postgres        # Check postgres logs
docker compose down -v              # Fresh start
docker compose up -d

# Java not found?
java -version                       # Check Java version (need 21)
echo $JAVA_HOME                     # Check JAVA_HOME is set

# Maven won't run?
mvn --version                       # Check Maven is installed (need 3.9+)

# Database connection refused?
docker ps                           # Make sure postgres container is running
docker compose up -d postgres       # Start only postgres
```

---

## Quick Reference Card

| Task | Command |
|---|---|
| Start project | `docker compose up -d` |
| Stop project | `docker compose down` |
| Fresh start | `docker compose down -v && docker compose up -d` |
| Run app | `mvn spring-boot:run` |
| Run tests | `mvn test` |
| Build JAR | `mvn package -DskipTests` |
| View logs | `docker compose logs -f` |
| Enter PostgreSQL | `docker exec -it h1b_navigator_db psql -U postgres -d h1b_navigator` |
| Enter Redis | `docker exec -it h1b_navigator_redis redis-cli` |
| Check containers | `docker ps` |
| Check disk usage | `docker system df` |
| Clean everything | `docker system prune --volumes` |
| Git push | `git add . && git commit -m "message" && git push` |
| Health check | `curl http://localhost:8080/actuator/health` |
