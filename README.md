# H1B Career Navigator API

> Built from personal experience navigating H1B/H4 transitions, job searching
> after a 1.5-year career break, and making financial decisions as an immigrant
> professional in Washington State.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![AWS](https://img.shields.io/badge/AWS-Bedrock%20%7C%20SQS%20%7C%20SNS%20%7C%20ECS-yellow)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)

---

## Why I Built This

As an H4 visa holder with a valid EAD, I found myself simultaneously managing:
- H4 EAD expiry deadlines and renewal timelines
- Job applications at Amazon and Microsoft in Seattle
- 401k withdrawal decisions with complex tax implications
- H4 → H1B change of status planning

No single tool handled all of this. Spreadsheets broke. I built this.

---

## Modules

### 1. Visa Timeline Tracker
Track H1B, H4, H4-EAD, OPT deadlines with automated 90/60/30-day SNS alerts.
The alert system is idempotent — duplicate notifications are impossible by design.

### 2. Job Application Tracker
Full pipeline tracking from Applied → Offer. Tracks H1B sponsorship status
per company — critical for visa-dependent job seekers.

### 3. AI Career Advisor *(Module 3 — in progress)*
AWS Bedrock-powered skill gap analysis and role recommendations.
Includes circuit breaker — falls back to rule-based recommendations
when Bedrock is unavailable. Caches AI responses in Redis for cost control.

### 4. Financial Calculator
401k early withdrawal calculator with:
- Federal tax calculation using 2024 marginal brackets
- Married filing jointly support (spouse income affects your bracket)
- Washington State 0% state tax applied automatically
- NRE/NRO FD comparison for US → India transfers *(coming soon)*

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   API Gateway                        │
│              Spring Boot 3.x + JWT                   │
└─────────────┬────────────────────────────────────────┘
              │
┌─────────────┴──────────────────────────────────────┐
│                  Service Layer                       │
│  ┌──────────┐ ┌──────────┐ ┌──────┐ ┌───────────┐  │
│  │  Visa    │ │   Job    │ │  AI  │ │ Financial │  │
│  │ Service  │ │ Service  │ │Advis.│ │   Calc.   │  │
│  └────┬─────┘ └────┬─────┘ └──┬───┘ └───────────┘  │
│       │            │          │                      │
└───────┼────────────┼──────────┼──────────────────────┘
        │            │          │
┌───────┴────────────┴──────────┴──────────────────────┐
│                  Infrastructure                       │
│  PostgreSQL │ Redis │ AWS SNS │ AWS SQS │ AWS Bedrock │
└─────────────────────────────────────────────────────-─┘
```

---

## Key Engineering Decisions

See [/docs/adr](/docs/adr) for full Architecture Decision Records:

- **[ADR-001](docs/adr/ADR-001-postgresql-over-dynamodb.md)** — Why PostgreSQL over DynamoDB
- **ADR-002** — Why SQS for async reminders over direct SNS scheduling
- **ADR-003** — Why AWS Bedrock over OpenAI (data privacy + AWS ecosystem)
- **ADR-004** — Redis caching strategy and PII boundary decisions

---

## What Went Wrong

See [POSTMORTEM.md](docs/POSTMORTEM.md) for honest mistakes and fixes:
- Duplicate SNS notifications (and the idempotency fix)
- Silent data loss during SNS outage (circuit breaker solution)
- PII accidentally cached in Redis (security fix)
- Over-engineered schema upfront (process lesson)

---

## Performance

Load tested with Apache JMeter — 500 concurrent users, 10-minute run:

| Endpoint | Avg Response | 99th Percentile | Throughput |
|---|---|---|---|
| GET /api/v1/visas | 45ms | 120ms | 1,200 req/s |
| POST /api/v1/financial/401k/withdrawal | 12ms | 35ms | 3,800 req/s |
| GET /api/v1/visas (cached) | 8ms | 22ms | 5,100 req/s |

Redis caching reduced database load by 73% for visa read operations.

---

## How to Run Locally

### Prerequisites
- Java 21
- Docker & Docker Compose
- AWS account (free tier sufficient)

### Start dependencies
```bash
docker-compose up -d  # starts PostgreSQL + Redis
```

### Configure environment
```bash
cp .env.example .env
# Fill in: DATABASE_URL, AWS credentials, JWT secret
```

### Run
```bash
./mvnw spring-boot:run
```

### API Documentation
```
http://localhost:8080/swagger-ui.html
```

---

## AWS Architecture

```
Route 53 → ALB → ECS Fargate (Spring Boot)
                      ↓
               RDS PostgreSQL (Multi-AZ)
               ElastiCache Redis
               SNS Topic (visa alerts)
               SQS Queue (async jobs)
               Bedrock (AI advisor)
               S3 (document storage)
```

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Backend | Spring Boot 3.2, Java 21 | Production-proven, virtual threads |
| Database | PostgreSQL 15 + Flyway | ACID for financial data |
| Cache | Redis 7 | Sub-10ms visa reads |
| Notifications | AWS SNS | Reliable delivery with retry |
| Async | AWS SQS | Decoupled, retry-safe |
| AI | AWS Bedrock (Claude) | Data stays in AWS |
| Resilience | Resilience4j | Circuit breakers on all external calls |
| Security | Spring Security + JWT | Stateless, scalable |
| Docs | Swagger/OpenAPI | Auto-generated from annotations |
| Deploy | AWS ECS + Docker | Container-native |
| CI/CD | GitHub Actions | Automated test + deploy |
| Monitoring | CloudWatch + Actuator | Production observability |

---

## Future Improvements

- [ ] NRE/NRO FD comparison calculator (US → India transfer planning)
- [ ] AI interview prep module (company-specific tips via Bedrock)
- [ ] H1B transfer tracker (portability during job change)
- [ ] Multi-user household support (track spouse visa separately)
- [ ] Mobile push notifications via AWS Pinpoint

---

*Built with 9 years of backend engineering experience and the lived reality
of navigating the US immigration system as a software professional.*
