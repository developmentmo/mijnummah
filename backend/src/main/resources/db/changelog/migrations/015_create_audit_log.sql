--liquibase formatted sql

--changeset mijnummah:015-create-audit-log
-- Important management actions: who did what, when, for which mosque (fase A). Rows are written in the same
-- transaction as the action, so a failed action leaves no entry. No personal data beyond the actor's account ID:
-- the target is referred to by type and ID only. Rows older than mijnummah.retention.audit-log are removed.
--
-- actor_user_id becomes NULL when the account is deleted (the entry stays, the person does not). mosque_id is NULL
-- for platform-wide actions such as blocking an account.

CREATE TABLE audit_log (
    id             UUID        NOT NULL,
    occurred_at    TIMESTAMPTZ NOT NULL,
    actor_user_id  UUID,
    mosque_id      UUID,
    action         VARCHAR(60) NOT NULL,
    target_type    VARCHAR(30) NOT NULL,
    target_id      UUID,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_actor_user_id FOREIGN KEY (actor_user_id) REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_log_mosque_id FOREIGN KEY (mosque_id) REFERENCES mosque (id) ON DELETE CASCADE,
    CONSTRAINT ck_audit_log_action_format CHECK (action ~ '^[A-Z]+(_[A-Z]+)*$')
);

CREATE INDEX ix_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_mosque_occurred_at ON audit_log (mosque_id, occurred_at DESC);
CREATE INDEX ix_audit_log_actor_user_id ON audit_log (actor_user_id);
