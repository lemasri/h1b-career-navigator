-- V1__initial_schema.sql
-- H1B Career Navigator — Initial Database Schema
--
-- Design decisions documented in ADR-001:
-- Why PostgreSQL over DynamoDB:
--   - Visa, job, and financial data is relational
--   - Complex joins required (e.g. visa + job + user in one query)
--   - ACID transactions critical for financial calculations
--   - DynamoDB's eventual consistency unacceptable for financial data

-- ===================== USERS =====================
CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    phone             VARCHAR(20),
    state_of_residence VARCHAR(2),   -- e.g. 'WA' for Washington
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ===================== VISAS =====================
CREATE TABLE visas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    visa_type           VARCHAR(20) NOT NULL,  -- H1B, H4, H4_EAD, etc.
    start_date          DATE NOT NULL,
    expiry_date         DATE NOT NULL,
    case_number         VARCHAR(100),
    sponsor_employer    VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Alert tracking flags
    -- Critical: flags prevent duplicate SNS notifications
    -- See POSTMORTEM.md Mistake #1 for why these exist
    alert_90_day_sent   BOOLEAN DEFAULT false,
    alert_60_day_sent   BOOLEAN DEFAULT false,
    alert_30_day_sent   BOOLEAN DEFAULT false,

    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visas_user_id ON visas(user_id);
CREATE INDEX idx_visas_expiry_date ON visas(expiry_date);
CREATE INDEX idx_visas_status ON visas(status);

-- Composite index for the scheduler query (most frequent query)
CREATE INDEX idx_visas_expiry_status ON visas(expiry_date, status)
    WHERE status = 'ACTIVE';

-- ===================== JOB APPLICATIONS =====================
CREATE TABLE job_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name        VARCHAR(255) NOT NULL,
    role_title          VARCHAR(255) NOT NULL,
    job_url             TEXT,
    recruiter_name      VARCHAR(255),
    recruiter_email     VARCHAR(255),
    status              VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    applied_date        DATE,
    next_interview_date DATE,
    sponsors_h1b        BOOLEAN,      -- critical for visa-dependent candidates
    salary_range_min    INTEGER,
    salary_range_max    INTEGER,
    referred_by         VARCHAR(255), -- employee referrals tracked separately
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_apps_user_id ON job_applications(user_id);
CREATE INDEX idx_job_apps_status ON job_applications(status);
CREATE INDEX idx_job_apps_company ON job_applications(company_name);

-- ===================== AUDIT TRIGGER =====================
-- Auto-update updated_at on any row change
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_visas_updated_at
    BEFORE UPDATE ON visas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_job_applications_updated_at
    BEFORE UPDATE ON job_applications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
