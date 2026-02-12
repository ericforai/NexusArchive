-- 批次编号生成器：使用 advisory lock 替代 FOR UPDATE 悲观锁
-- 解决并发请求下 batch_no 唯一约束冲突（409 Conflict）
--
-- 原理：pg_advisory_xact_lock 锁定逻辑概念而非具体行，
-- 避免 READ COMMITTED 隔离级别下的幻读问题。
-- 锁在事务提交/回滚时自动释放。

CREATE OR REPLACE FUNCTION next_batch_no(p_date_part VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    v_lock_key BIGINT;
    v_max_seq  INT;
    v_next_seq INT;
    v_batch_no VARCHAR;
BEGIN
    -- 用日期字符串的哈希值作为 advisory lock key，确保同日期串行
    v_lock_key := hashtext('batch_no_' || p_date_part);
    PERFORM pg_advisory_xact_lock(v_lock_key);

    -- 在锁保护下查询当前最大序号（兼容旧格式 3位 和新格式 5位）
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(batch_no FROM LENGTH('COL-' || p_date_part || '-') + 1) AS INT)
    ), 0) INTO v_max_seq
    FROM collection_batch
    WHERE batch_no LIKE 'COL-' || p_date_part || '-%';

    v_next_seq := v_max_seq + 1;

    IF v_next_seq > 99999 THEN
        RAISE EXCEPTION '每日批次数量已达上限 (99999)';
    END IF;

    v_batch_no := 'COL-' || p_date_part || '-' || LPAD(v_next_seq::TEXT, 5, '0');

    RETURN v_batch_no;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION next_batch_no(VARCHAR) IS
    '生成下一个批次编号。使用 pg_advisory_xact_lock 保证并发安全。格式: COL-YYYYMMDD-NNNNN';
