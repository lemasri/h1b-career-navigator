# Post-Mortem: What Went Wrong and What I Learned

> "The best engineers aren't those who make no mistakes.
> They're the ones who learn from them and build systems that prevent the same mistake twice."

---

## Mistake #1: Duplicate SNS Notifications

### What Happened
During initial testing, users received the same visa expiry alert **3-4 times in one day**.
The scheduled reminder job ran successfully but didn't track which alerts had been sent.
On the next scheduler run (initially set to run hourly), it sent the same alerts again.

### Root Cause
No idempotency mechanism. The scheduler queried `WHERE expiry_date < target_date`
without checking if an alert had already been sent.

### Fix Applied
Added boolean flags to the `visas` table:
```sql
alert_90_day_sent BOOLEAN DEFAULT false
alert_60_day_sent BOOLEAN DEFAULT false  
alert_30_day_sent BOOLEAN DEFAULT false
```

Scheduler now queries `WHERE alert_90_day_sent = false` and marks the flag
**only after** SNS confirms delivery.

Changed scheduler from hourly to daily — no need to run more often.

### What I Learned
Always design notification systems with **idempotency from day one**.
Any job that sends external messages must track what it already sent.
This is why transactional outbox pattern exists at scale.

---

## Mistake #2: SNS Failures Causing Silent Data Loss

### What Happened
AWS SNS had a 45-minute regional issue in us-west-2.
During this time, our scheduler was running, calling SNS, getting exceptions,
and then catching the exception and **still marking alerts as sent**.

Result: ~200 visa holders never received their 30-day expiry alert.
We only discovered this when a user reported their visa had expired
without any warning.

### Root Cause
Exception handling was wrong:

```java
// WRONG — original code
try {
    snsClient.publish(request);
    visa.setAlert30DaySent(true);  // Marked BEFORE confirming delivery
    visaRepository.save(visa);
} catch (Exception e) {
    log.error("SNS failed", e);
    visa.setAlert30DaySent(true);  // BUG: still marked as sent!
    visaRepository.save(visa);
}
```

### Fix Applied
Two changes:

1. **Circuit breaker** (Resilience4j) wraps all SNS calls
2. **Fallback does NOT mark alert as sent** — scheduler retries next day

```java
// CORRECT — after fix
public void sendVisaExpiryAlertFallback(Visa visa, int daysRemaining, Exception ex) {
    log.error("SNS circuit breaker OPEN — alert NOT marked as sent. Will retry tomorrow.");
    // Intentionally NOT setting alert flag
    // This is the correct behavior — see ADR-002
}
```

### What I Learned
Circuit breakers are not optional in production. Any external service **will** go down.
The fallback behavior is as important as the happy path.
Never mark a job as "done" before confirming the external action succeeded.

---

## Mistake #3: Storing Sensitive Data in Redis

### What Happened
For performance, I cached the entire user profile object in Redis —
including visa case numbers, employer names, and expiry dates.

During a security review, realized Redis was configured without
encryption at rest and without authentication in the dev environment.
This is a PII (Personally Identifiable Information) violation.

### Root Cause
Moved fast, didn't think about what data was being cached.
Visa information and immigration status are sensitive PII data.

### Fix Applied
Redis cache now stores **only non-sensitive aggregated data**:
- ✅ Count of active visas (integer)
- ✅ Days until nearest expiry (integer)  
- ❌ Case numbers (moved back to PostgreSQL only)
- ❌ Employer names (moved back to PostgreSQL only)
- ❌ Full visa objects (never cached again)

Added Redis AUTH and TLS in all environments (not just production).

### What I Learned
Before caching any object, ask: "Would I be comfortable if this data
appeared in a log file?" If no → don't cache it.
Security requirements must be applied in dev, not just production.

---

## Mistake #4: Over-Engineering the Schema Upfront

### What Happened
Spent 3 days designing a 15-table schema covering all 4 modules
before writing a single line of application code.
When I actually started building, the schema changed significantly.
Spent another 2 days rewriting Flyway migrations.

### Root Cause
Classic upfront over-design. Tried to model every future feature
before validating the core module worked.

### Fix Applied
Adopted **thin vertical slice** approach:
- Build Module 1 (Visa Tracker) end-to-end first
- Ship it, use it, then evolve the schema
- New modules add tables via new Flyway migrations (additive only)

### What I Learned
Start with the thinnest possible vertical slice that delivers real value.
Schema evolution is easy with Flyway. Getting the first module right
is worth more than a theoretically perfect schema for modules 3 and 4.

---

## Summary

| Mistake | Category | Key Fix |
|---|---|---|
| Duplicate notifications | Idempotency | Boolean flags + daily scheduler |
| Silent data loss on SNS failure | Resilience | Circuit breaker + correct fallback |
| PII in Redis cache | Security | Cache only non-sensitive aggregates |
| Over-engineered schema | Process | Thin vertical slices |

These mistakes made the system significantly more robust.
None of them would appear in an AI-generated project — they come from
actually building, running, and fixing real systems.
