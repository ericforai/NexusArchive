-- Grant Borrowing Permissions to Admin Rule

-- 1. Ensure permissions exist in sys_permission (Using perm_key and label)
INSERT INTO sys_permission (id, perm_key, label, group_name, created_time, updated_time)
VALUES 
('perm-borrow-approve', 'borrowing:approve', '借阅审批', 'BORROW', NOW(), NOW()),
('perm-borrow-return', 'borrowing:return', '借阅归还', 'BORROW', NOW(), NOW()),
('perm-borrow-cancel', 'borrowing:cancel', '借阅取消', 'BORROW', NOW(), NOW())
ON CONFLICT (perm_key) DO NOTHING;

-- 2. Grant permissions to 'super_admin' and 'system_admin' by updating sys_role.permissions field
-- Assuming permissions are stored as comma-separated values in the 'permissions' column

UPDATE sys_role
SET permissions = CASE 
    WHEN permissions IS NULL OR permissions = '' THEN 'borrowing:approve,borrowing:return,borrowing:cancel'
    WHEN permissions LIKE '%borrowing:approve%' THEN permissions -- already has it
    ELSE permissions || ',borrowing:approve,borrowing:return,borrowing:cancel'
END
WHERE code IN ('super_admin', 'system_admin', 'business_user');

-- Verify the update
SELECT code, permissions FROM sys_role WHERE code IN ('super_admin', 'system_admin', 'business_user');
