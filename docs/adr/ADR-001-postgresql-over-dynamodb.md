# ADR-001: PostgreSQL over DynamoDB for Primary Data Store

**Date:** 2024-06  
**Status:** Accepted  
**Author:** [Your Name]

---

## Context

Needed to choose a database for storing visa, job application, and financial data.
Two main options were AWS DynamoDB (fits naturally into our AWS stack) or PostgreSQL.

This decision was harder than it looks because we're already using DynamoDB for
the URL shortener project — so the team had familiarity with it.

---

## Options Considered

### Option A: AWS DynamoDB
**Pros:**
- Native AWS integration, no EC2 management
- Auto-scaling with zero ops overhead
- Great for high read throughput
- We already use it in another service

**Cons:**
- No joins — every relational query becomes multiple round-trips
- Eventual consistency unacceptable for financial calculations
- Complex access patterns require careful upfront modeling
- Hard to query "all active visas expiring in 90 days" efficiently

### Option B: PostgreSQL on AWS RDS
**Pros:**
- ACID transactions critical for 401k calculation accuracy
- Complex joins natural: `visa JOIN user JOIN job_application`
- Rich query capabilities (date range queries, composite indexes)
- pgvector extension available for future AI/embeddings module
- Team has 9 years of SQL expertise

**Cons:**
- Requires RDS instance management
- Manual scaling vs DynamoDB auto-scale
- Higher ops overhead

---

## Decision

**Chose PostgreSQL (AWS RDS).**

The deciding factor: financial calculation data requires ACID guarantees.
A 401k withdrawal calculation that reads stale data due to eventual consistency
could show a user the wrong tax amount — unacceptable for a financial tool.

Additionally, our scheduler query `WHERE expiry_date BETWEEN x AND y AND status = 'ACTIVE'`
is a natural SQL range query. In DynamoDB this would require a full table scan
or complex GSI design.

---

## What I Would Change at 10x Scale

At 1M+ users:
- Add **read replicas** for the scheduler queries (read-heavy, latency-tolerant)
- Move job application analytics to **DynamoDB** (high write throughput, simple access)
- Consider **Aurora Serverless v2** for auto-scaling the RDS instance
- Add **pgvector** for AI career advisor embeddings (already planned in Module 3)

---

## Tradeoffs Accepted

- We accept higher ops overhead for correctness guarantees
- We accept manual scaling for query flexibility
- We will NOT move to DynamoDB unless read throughput exceeds 10,000 RPS

---

## References
- [AWS RDS PostgreSQL docs](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [DynamoDB best practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
