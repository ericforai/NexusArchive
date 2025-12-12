-- Kingbase ES V8 (PostgreSQL Compatible)

CREATE TABLE IF NOT EXISTS sys_org (
  id            VARCHAR(64) PRIMARY KEY,
  name          VARCHAR(255) NOT NULL,
  code          VARCHAR(128),
  parent_id     VARCHAR(64),
  type          VARCHAR(32) DEFAULT 'DEPARTMENT',
  order_num     INTEGER DEFAULT 0,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted       INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_sys_org_parent ON sys_org(parent_id);

CREATE TABLE IF NOT EXISTS sys_role (
  id             VARCHAR(64) PRIMARY KEY,
  name           VARCHAR(255) NOT NULL,
  code           VARCHAR(128) NOT NULL UNIQUE,
  role_category  VARCHAR(64),
  is_exclusive   BOOLEAN DEFAULT FALSE,
  description    TEXT,
  permissions    TEXT,
  data_scope     VARCHAR(32) DEFAULT 'self',
  type           VARCHAR(32) DEFAULT 'custom',
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted        INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user (
  id               VARCHAR(64) PRIMARY KEY,
  username         VARCHAR(128) NOT NULL UNIQUE,
  password_hash    VARCHAR(255) NOT NULL,
  full_name        VARCHAR(255),
  org_code         VARCHAR(128),
  email            VARCHAR(255),
  phone            VARCHAR(64),
  avatar           VARCHAR(512),
  department_id    VARCHAR(64),
  status           VARCHAR(32) DEFAULT 'active',
  last_login_at    TIMESTAMP,
  employee_id      VARCHAR(64),
  job_title        VARCHAR(128),
  join_date        VARCHAR(32),
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted          INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id   VARCHAR(64) NOT NULL,
  role_id   VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user ON sys_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role ON sys_user_role(role_id);

CREATE TABLE IF NOT EXISTS sys_permission (
  id            VARCHAR(64) PRIMARY KEY,
  perm_key      VARCHAR(128) NOT NULL UNIQUE,
  label         VARCHAR(255) NOT NULL,
  group_name    VARCHAR(128),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
