-- Базовая схема системы записи на приём

CREATE TABLE user_account (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_user_account_username UNIQUE (username),
    CONSTRAINT ck_user_account_role CHECK (role IN ('ADMIN', 'DOCTOR', 'REGISTRAR'))
);

CREATE TABLE doctor (
    id                   BIGSERIAL PRIMARY KEY,
    user_account_id      BIGINT,
    full_name            VARCHAR(255) NOT NULL,
    specialty            VARCHAR(64)  NOT NULL,
    cabinet              VARCHAR(16),
    phone                VARCHAR(32),
    default_slot_minutes INT          NOT NULL DEFAULT 15,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_doctor_user_account FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    CONSTRAINT uq_doctor_user_account UNIQUE (user_account_id),
    CONSTRAINT ck_doctor_slot_minutes CHECK (default_slot_minutes BETWEEN 5 AND 120)
);

CREATE INDEX idx_doctor_specialty ON doctor (specialty) WHERE deleted = FALSE;

CREATE TABLE patient (
    id          BIGSERIAL PRIMARY KEY,
    iin         VARCHAR(12)  NOT NULL,
    last_name   VARCHAR(128) NOT NULL,
    first_name  VARCHAR(128) NOT NULL,
    middle_name VARCHAR(128),
    birth_date  DATE         NOT NULL,
    gender      VARCHAR(16)  NOT NULL,
    phone       VARCHAR(32),
    address     VARCHAR(512),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_patient_iin UNIQUE (iin),
    CONSTRAINT ck_patient_iin CHECK (iin ~ '^[0-9]{12}$'),
    CONSTRAINT ck_patient_gender CHECK (gender IN ('MALE', 'FEMALE'))
);

CREATE INDEX idx_patient_last_name ON patient (lower(last_name)) WHERE deleted = FALSE;

CREATE TABLE time_slot (
    id         BIGSERIAL PRIMARY KEY,
    doctor_id  BIGINT      NOT NULL,
    slot_date  DATE        NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    status     VARCHAR(16) NOT NULL DEFAULT 'FREE',
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_time_slot_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT ck_time_slot_status CHECK (status IN ('FREE', 'BOOKED', 'BLOCKED')),
    CONSTRAINT ck_time_slot_interval CHECK (start_time < end_time)
);

-- Один и тот же интервал у врача не может быть заведён дважды.
CREATE UNIQUE INDEX uq_time_slot_doctor_interval
    ON time_slot (doctor_id, slot_date, start_time)
    WHERE deleted = FALSE;

CREATE INDEX idx_time_slot_search ON time_slot (slot_date, status, doctor_id) WHERE deleted = FALSE;

CREATE TABLE appointment (
    id            BIGSERIAL PRIMARY KEY,
    time_slot_id  BIGINT      NOT NULL,
    patient_id    BIGINT      NOT NULL,
    doctor_id     BIGINT      NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    complaint     VARCHAR(1000),
    conclusion    VARCHAR(2000),
    cancel_reason VARCHAR(512),
    created_by    VARCHAR(64),
    closed_at     TIMESTAMPTZ,
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_appointment_time_slot FOREIGN KEY (time_slot_id) REFERENCES time_slot (id),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT ck_appointment_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);

-- На один слот допустима только одна активная запись; отменённые не мешают перезаписи.
CREATE UNIQUE INDEX uq_appointment_active_slot
    ON appointment (time_slot_id)
    WHERE status = 'SCHEDULED' AND deleted = FALSE;

CREATE INDEX idx_appointment_patient ON appointment (patient_id, status) WHERE deleted = FALSE;
CREATE INDEX idx_appointment_doctor ON appointment (doctor_id, status) WHERE deleted = FALSE;
