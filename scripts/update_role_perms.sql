UPDATE sys_role SET permissions = '["audit:view", "audit_logs", "nav:portal"]' WHERE code = 'auditor';
UPDATE sys_role SET permissions = '["archive:read", "archive:manage", "nav:archive_mgmt", "nav:query", "nav:portal"]' WHERE code = 'user';
