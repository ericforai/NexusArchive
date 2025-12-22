-- Input: 数据库引擎
-- Output: 数据库结构初始化/变更
-- Pos: 数据库初始化脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- MySQL 8.0+ Schema

CREATE TABLE IF NOT EXISTS `sys_org` (
  `id`            VARCHAR(64) NOT NULL,
  `name`          VARCHAR(255) NOT NULL,
  `code`          VARCHAR(128),
  `parent_id`     VARCHAR(64),
  `type`          VARCHAR(32) DEFAULT 'DEPARTMENT',
  `order_num`     INT DEFAULT 0,
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       INT DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_sys_org_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id`             VARCHAR(64) NOT NULL,
  `name`           VARCHAR(255) NOT NULL,
  `code`           VARCHAR(128) NOT NULL,
  `role_category`  VARCHAR(64),
  `is_exclusive`   BOOLEAN DEFAULT FALSE,
  `description`    TEXT,
  `permissions`    TEXT,
  `data_scope`     VARCHAR(32) DEFAULT 'self',
  `type`           VARCHAR(32) DEFAULT 'custom',
  `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`               VARCHAR(64) NOT NULL,
  `username`         VARCHAR(128) NOT NULL,
  `password_hash`    VARCHAR(255) NOT NULL,
  `full_name`        VARCHAR(255),
  `org_code`         VARCHAR(128),
  `email`            VARCHAR(255),
  `phone`            VARCHAR(64),
  `avatar`           VARCHAR(512),
  `department_id`    VARCHAR(64),
  `status`           VARCHAR(32) DEFAULT 'active',
  `last_login_at`    DATETIME,
  `employee_id`      VARCHAR(64),
  `job_title`        VARCHAR(128),
  `join_date`        VARCHAR(32),
  `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          INT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id`   VARCHAR(64) NOT NULL,
  `role_id`   VARCHAR(64) NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  INDEX `idx_sys_user_role_user` (`user_id`),
  INDEX `idx_sys_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id`            VARCHAR(64) NOT NULL,
  `perm_key`      VARCHAR(128) NOT NULL,
  `label`         VARCHAR(255) NOT NULL,
  `group_name`    VARCHAR(128),
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_key` (`perm_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
