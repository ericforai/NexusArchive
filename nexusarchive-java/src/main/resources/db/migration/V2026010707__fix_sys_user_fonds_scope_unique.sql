-- Fix unique constraint on sys_user_fonds_scope to allow re-assigning permissions after logical deletion
-- Previous constraint uk_user_fonds_scope (user_id, fonds_no) blocked re-insertion if a 'deleted=1' record existed.

-- Drop the old unique index that doesn't account for logical deletion
DROP INDEX IF EXISTS uk_user_fonds_scope;

-- Create partial unique index (only active rows need to be unique)
CREATE UNIQUE INDEX uk_user_fonds_scope
    ON sys_user_fonds_scope(user_id, fonds_no)
    WHERE deleted = 0;
