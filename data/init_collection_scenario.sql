-- Input: 数据库引擎
-- Output: 数据库结构初始化/变更
-- Pos: 数据库初始化脚本
-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

-- 找到所有用友YonSuite配置的ID
DO $$
DECLARE
    config_record RECORD;
    exists_count INT;
BEGIN
    FOR config_record IN SELECT id FROM sys_erp_config WHERE erp_type = 'yonsuite' LOOP
        -- 检查该配置下是否已存在 COLLECTION_FILE_SYNC
        SELECT COUNT(*) INTO exists_count 
        FROM sys_erp_scenario 
        WHERE config_id = config_record.id AND scenario_key = 'COLLECTION_FILE_SYNC';
        
        IF exists_count = 0 THEN
            INSERT INTO sys_erp_scenario (
                config_id, 
                scenario_key, 
                name, 
                description, 
                is_active, 
                sync_strategy, 
                created_time, 
                last_modified_time, 
                last_sync_status
            ) VALUES (
                config_record.id,
                'COLLECTION_FILE_SYNC',
                '收款单文件同步',
                '从YonSuite获取收款单文件',
                true,
                'MANUAL',
                NOW(),
                NOW(),
                'NONE'
            );
            RAISE NOTICE 'Added COLLECTION_FILE_SYNC for config %', config_record.id;
        ELSE
            RAISE NOTICE 'COLLECTION_FILE_SYNC already exists for config %', config_record.id;
        END IF;
    END LOOP;
END $$;
