/*
 * Operations Service schema
 *
 * This migration is rerunnable. It owns only Operations Service tables in the
 * mysociety schema; identity and society identifiers remain UUID values without
 * database foreign keys because their owning services validate them externally.
 */

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS mysociety;
SET search_path TO mysociety, public;

CREATE TABLE IF NOT EXISTS complaints (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id          UUID NOT NULL,
    unit_id             UUID,
    complaint_number    VARCHAR(80) NOT NULL,
    reported_by         UUID NOT NULL,
    category            VARCHAR(60) NOT NULL,
    subcategory         VARCHAR(80),
    subject             VARCHAR(200) NOT NULL,
    description         TEXT NOT NULL,
    location_details    VARCHAR(300),
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN','UNDER_REVIEW','ASSIGNED','IN_PROGRESS','WAITING_FOR_RESIDENT','RESOLVED','CLOSED','REOPENED','REJECTED')),
    assigned_user_id    UUID,
    sla_due_at          TIMESTAMPTZ,
    preferred_visit_at  TIMESTAMPTZ,
    entry_permission    BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_summary  TEXT,
    resolved_at         TIMESTAMPTZ,
    closed_at           TIMESTAMPTZ,
    resident_rating     SMALLINT CHECK (resident_rating BETWEEN 1 AND 5),
    resident_feedback   VARCHAR(1000),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (society_id, complaint_number)
);

CREATE INDEX IF NOT EXISTS ix_complaints_status_priority
    ON complaints (society_id, status, priority, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_complaints_sla
    ON complaints (society_id, sla_due_at)
    WHERE status NOT IN ('CLOSED','REJECTED');

CREATE TABLE IF NOT EXISTS work_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id          UUID NOT NULL,
    complaint_id        UUID REFERENCES complaints(id) ON DELETE SET NULL,
    work_order_number   VARCHAR(80) NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    assigned_user_id    UUID,
    assigned_vendor_name VARCHAR(200),
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status              VARCHAR(30) NOT NULL DEFAULT 'CREATED'
                        CHECK (status IN ('CREATED','ASSIGNED','ACCEPTED','IN_PROGRESS','ON_HOLD','COMPLETED','VERIFIED','CANCELLED')),
    scheduled_at        TIMESTAMPTZ,
    due_at              TIMESTAMPTZ,
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    completion_notes    TEXT,
    estimated_cost      NUMERIC(19,2) CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
    actual_cost         NUMERIC(19,2) CHECK (actual_cost IS NULL OR actual_cost >= 0),
    created_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (society_id, work_order_number)
);

CREATE INDEX IF NOT EXISTS ix_work_orders_assignee
    ON work_orders (society_id, assigned_user_id, status, due_at);

CREATE TABLE IF NOT EXISTS complaint_comments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id     UUID NOT NULL,
    complaint_id   UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    author_user_id UUID,
    comment_text   TEXT NOT NULL,
    is_internal    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS complaint_status_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id     UUID NOT NULL,
    complaint_id   UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    old_status     VARCHAR(30),
    new_status     VARCHAR(30) NOT NULL,
    changed_by     UUID,
    reason         VARCHAR(1000),
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
