-- Crea las tablas que el codigo ya consultaba pero que ninguna migracion creaba.
-- Todas usan CREATE TABLE IF NOT EXISTS: en la BD de desarrollo, inicializada desde
-- database/reference/db.sql, estas sentencias son no-ops y no alteran nada.
-- Su efecto real es dar esquema reproducible al entorno de pruebas (H2) y a cualquier
-- entorno nuevo levantado solo con Flyway.
--
-- NOTA: refresh_tokens del modelo de referencia NO se crea aqui. El codigo usa
-- refresh_sessions (V2), decision registrada y mantenida de forma deliberada.
-- Estilo alineado con V1-V6: sin UNSIGNED y sin ON UPDATE CURRENT_TIMESTAMP, para
-- que las mismas sentencias funcionen en MySQL 8.4 y en H2 con MODE=MySQL.

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME NOT NULL,
  used_at DATETIME NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_password_reset_hash UNIQUE (token_hash),
  CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX ix_password_reset_user (user_id),
  INDEX ix_password_reset_expiry (expires_at)
);

CREATE TABLE IF NOT EXISTS eps (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(150) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS eps_plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  eps_id BIGINT NOT NULL,
  regime_id SMALLINT NOT NULL,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(150) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_eps_plan_code UNIQUE (eps_id, code),
  CONSTRAINT fk_eps_plans_eps FOREIGN KEY (eps_id) REFERENCES eps(id),
  CONSTRAINT fk_eps_plans_regime FOREIGN KEY (regime_id) REFERENCES insurance_regimes(id),
  INDEX ix_eps_plans_regime (regime_id)
);

CREATE TABLE IF NOT EXISTS user_insurance_affiliations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  membership_number VARCHAR(80) NOT NULL,
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  valid_from DATE NULL,
  valid_to DATE NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_user_membership UNIQUE (user_id, plan_id, membership_number),
  CONSTRAINT fk_user_insurance_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_insurance_plan FOREIGN KEY (plan_id) REFERENCES eps_plans(id),
  INDEX ix_user_insurance_current (user_id, is_current)
);

CREATE TABLE IF NOT EXISTS appointments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_user_id BIGINT NOT NULL,
  professional_id BIGINT NOT NULL,
  location_id SMALLINT NOT NULL,
  specialty_id SMALLINT NOT NULL,
  insurance_affiliation_id BIGINT NULL,
  status_id SMALLINT NOT NULL,
  reason VARCHAR(500) NULL,
  scheduled_start_at DATETIME NOT NULL,
  scheduled_end_at DATETIME NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  approved_by_user_id BIGINT NULL,
  approved_at DATETIME NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_appointment_time CHECK (scheduled_end_at > scheduled_start_at),
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_user_id) REFERENCES users(id),
  CONSTRAINT fk_appointments_professional FOREIGN KEY (professional_id) REFERENCES professionals(id),
  CONSTRAINT fk_appointments_location FOREIGN KEY (location_id) REFERENCES locations(id),
  CONSTRAINT fk_appointments_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id),
  CONSTRAINT fk_appointments_insurance FOREIGN KEY (insurance_affiliation_id) REFERENCES user_insurance_affiliations(id),
  CONSTRAINT fk_appointments_status FOREIGN KEY (status_id) REFERENCES appointment_statuses(id),
  CONSTRAINT fk_appointments_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id),
  CONSTRAINT fk_appointments_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users(id),
  INDEX ix_appointments_patient (patient_user_id, scheduled_start_at),
  INDEX ix_appointments_professional (professional_id, scheduled_start_at),
  INDEX ix_appointments_status (status_id)
);

CREATE TABLE IF NOT EXISTS appointment_status_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_id BIGINT NOT NULL,
  status_id SMALLINT NOT NULL,
  changed_by_user_id BIGINT NULL,
  change_source VARCHAR(20) NOT NULL DEFAULT 'USER',
  reason VARCHAR(500) NULL,
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_status_history_source CHECK (change_source IN ('SYSTEM','USER','ADMIN')),
  CONSTRAINT fk_status_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  CONSTRAINT fk_status_history_status FOREIGN KEY (status_id) REFERENCES appointment_statuses(id),
  CONSTRAINT fk_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id),
  INDEX ix_status_history_appointment (appointment_id, changed_at)
);

CREATE TABLE IF NOT EXISTS reschedule_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_id BIGINT NOT NULL,
  requested_by_user_id BIGINT NOT NULL,
  requested_location_id SMALLINT NOT NULL,
  status_id SMALLINT NOT NULL,
  previous_start_at DATETIME NOT NULL,
  previous_end_at DATETIME NOT NULL,
  requested_start_at DATETIME NOT NULL,
  requested_end_at DATETIME NOT NULL,
  decision_reason VARCHAR(500) NULL,
  decided_by_user_id BIGINT NULL,
  decided_at DATETIME NULL,
  patient_action_after_rejection VARCHAR(30) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_reschedule_time CHECK (requested_end_at > requested_start_at),
  CONSTRAINT ck_reschedule_patient_action CHECK (patient_action_after_rejection IS NULL OR patient_action_after_rejection IN ('KEEP_APPOINTMENT','CANCEL_APPOINTMENT')),
  CONSTRAINT fk_reschedule_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  CONSTRAINT fk_reschedule_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users(id),
  CONSTRAINT fk_reschedule_location FOREIGN KEY (requested_location_id) REFERENCES locations(id),
  CONSTRAINT fk_reschedule_status FOREIGN KEY (status_id) REFERENCES reschedule_request_statuses(id),
  CONSTRAINT fk_reschedule_decided_by FOREIGN KEY (decided_by_user_id) REFERENCES users(id),
  INDEX ix_reschedule_appointment (appointment_id),
  INDEX ix_reschedule_status (status_id)
);
