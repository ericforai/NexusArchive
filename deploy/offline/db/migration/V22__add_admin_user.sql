-- V22: Ensure admin user exists with known password and update role permissions
-- Password: admin123 (Argon2id hash)

-- First, ensure the super_admin role exists with correct permissions
INSERT INTO sys_role (id, code, name, role_category, permissions)
VALUES (
    'role_super_admin',
    'super_admin',
    '超级管理员',
    'system_admin',
    '["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users"]'
) ON CONFLICT (code) DO UPDATE SET permissions = EXCLUDED.permissions;

-- Update existing admin user password (if exists, create if not)
-- Password is: admin123
DO $$
DECLARE
    v_admin_id VARCHAR(32);
BEGIN
    -- Check if admin user exists
    SELECT id INTO v_admin_id FROM sys_user WHERE username = 'admin';
    
    IF v_admin_id IS NOT NULL THEN
        -- Update existing admin user password
        UPDATE sys_user 
        SET password_hash = '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA',
            status = 'active'
        WHERE id = v_admin_id;
        
        -- Link to super_admin role if not already linked
        INSERT INTO sys_user_role (user_id, role_id)
        SELECT v_admin_id, id FROM sys_role WHERE code = 'super_admin'
        ON CONFLICT DO NOTHING;
    ELSE
        -- Create new admin user
        INSERT INTO sys_user (id, username, password_hash, full_name, email, status)
        VALUES (
            'user_admin_001',
            'admin',
            '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA',
            '系统管理员',
            'admin@nexusarchive.local',
            'active'
        );
        
        -- Link to super_admin role
        INSERT INTO sys_user_role (user_id, role_id)
        SELECT 'user_admin_001', id FROM sys_role WHERE code = 'super_admin';
    END IF;
END $$;
