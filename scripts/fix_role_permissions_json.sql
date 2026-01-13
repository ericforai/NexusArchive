-- Fix Role Permissions with Valid JSON

-- 1. Fix Super Admin (Merge existing + new borrowing perms)
UPDATE sys_role
SET permissions = '[
    "nav:portal", "nav:panorama", "nav:pre_archive", "nav:collection", 
    "nav:archive_mgmt", "nav:query", "nav:borrowing", "nav:destruction", 
    "nav:warehouse", "nav:stats", "nav:settings", "nav:all", 
    "system_admin", "manage_users", 
    "archive:read", "archive:view", "archive:manage", 
    "audit:view", "scan:manage", 
    "entity:view", "entity:manage",
    "borrowing:approve", "borrowing:return", "borrowing:cancel"
]'
WHERE code = 'super_admin';

-- 2. Fix System Admin (Ensure valid JSON)
-- Previously NULL, giving them borrowing access and nav access
UPDATE sys_role
SET permissions = '[
    "nav:borrowing", 
    "borrowing:approve", "borrowing:return", "borrowing:cancel"
]'
WHERE code = 'system_admin';

-- 3. Fix Business User
UPDATE sys_role
SET permissions = '[
    "nav:portal", "nav:panorama", "nav:pre_archive", "nav:collection", 
    "nav:repository", "nav:archive_mgmt", "nav:operations", "nav:utilization", 
    "nav:stats", "archive:read", "archive:view", 
    "borrowing:create", "borrowing:view",
    "borrowing:approve", "borrowing:return", "borrowing:cancel"
]'
WHERE code = 'business_user';
