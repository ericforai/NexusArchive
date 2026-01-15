--
-- PostgreSQL database dump
--

\restrict ECkGfRuLlHfHe93qoFNtCaRsADI1TtOSqcCI61aJAj7fdKNcHRIHgtnGlTN9sYF

-- Dumped from database version 14.20
-- Dumped by pg_dump version 14.20

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA public;


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: prevent_destruction_log_modification(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.prevent_destruction_log_modification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'destruction_log table is read-only. UPDATE operation is not allowed.';
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'destruction_log table is read-only. DELETE operation is not allowed.';
    END IF;
    RETURN NULL;
END;
$$;


--
-- Name: FUNCTION prevent_destruction_log_modification(); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.prevent_destruction_log_modification() IS '防止销毁清册表被修改或删除的触发器';


--
-- Name: update_row_version(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_row_version() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.row_version = OLD.row_version + 1;
    NEW.last_modified_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: FUNCTION update_row_version(); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.update_row_version() IS '自动递增乐观锁版本号';


--
-- Name: update_sys_entity_config_updated_time(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_sys_entity_config_updated_time() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: update_sys_entity_updated_time(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_sys_entity_updated_time() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: acc_archive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_archive (
    id character varying(64) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    archive_code character varying(64) NOT NULL,
    category_code character varying(50) NOT NULL,
    title character varying(1000) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    fiscal_period character varying(10),
    retention_period character varying(10) NOT NULL,
    org_name character varying(500) NOT NULL,
    creator character varying(500),
    status character varying(20) DEFAULT 'draft'::character varying,
    amount numeric(18,2),
    doc_date date,
    unique_biz_id character varying(64),
    standard_metadata jsonb,
    custom_metadata jsonb,
    security_level character varying(20) DEFAULT 'internal'::character varying,
    location character varying(200),
    department_id character varying(32),
    created_by character varying(32),
    fixity_value character varying(128),
    fixity_algo character varying(20),
    volume_id character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    paper_ref_link character varying(128),
    destruction_hold boolean DEFAULT false,
    hold_reason character varying(255),
    summary character varying(2000),
    match_score integer DEFAULT 0,
    match_method character varying(100),
    batch_id bigint,
    archived_at timestamp without time zone,
    counterparty character varying(255),
    voucher_no character varying(100),
    invoice_no character varying(100),
    retention_start_date date,
    destruction_status character varying(20) DEFAULT 'NORMAL'::character varying
);


--
-- Name: TABLE acc_archive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_archive IS '电子会计档案表';


--
-- Name: COLUMN acc_archive.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.title IS '题名 (SM4加密存储，最大1000字符)';


--
-- Name: COLUMN acc_archive.org_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.org_name IS '立档单位名称 (最大500字符)';


--
-- Name: COLUMN acc_archive.creator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.creator IS '责任者/制单人 (SM4加密存储，最大500字符)';


--
-- Name: COLUMN acc_archive.amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.amount IS '金额';


--
-- Name: COLUMN acc_archive.doc_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.doc_date IS '业务日期';


--
-- Name: COLUMN acc_archive.unique_biz_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.unique_biz_id IS '唯一业务ID';


--
-- Name: COLUMN acc_archive.volume_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.volume_id IS '所属案卷ID';


--
-- Name: COLUMN acc_archive.paper_ref_link; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.paper_ref_link IS '纸质档案关联号 (物理存放位置)';


--
-- Name: COLUMN acc_archive.destruction_hold; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.destruction_hold IS '销毁留置 (冻结状态)';


--
-- Name: COLUMN acc_archive.hold_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.hold_reason IS '留置/冻结原因 (如: 未结清债权)';


--
-- Name: COLUMN acc_archive.summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.summary IS '摘要/说明 (SM4加密存储，最大2000字符)';


--
-- Name: COLUMN acc_archive.match_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.match_score IS '智能匹配得分 (0-100)';


--
-- Name: COLUMN acc_archive.match_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.match_method IS '关联方式 (如：金额+日期匹配)';


--
-- Name: COLUMN acc_archive.counterparty; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.counterparty IS '对方单位 (结构化检索)';


--
-- Name: COLUMN acc_archive.voucher_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.voucher_no IS '凭证号 (结构化检索)';


--
-- Name: COLUMN acc_archive.invoice_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.invoice_no IS '发票号 (结构化检索)';


--
-- Name: COLUMN acc_archive.retention_start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.retention_start_date IS '保管期限起算日期（用于计算到期时间，默认为归档日期或会计年度结束日期）';


--
-- Name: COLUMN acc_archive.destruction_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive.destruction_status IS '销毁状态: NORMAL(正常), EXPIRED(到期), APPRAISING(鉴定中), DESTRUCTION_APPROVED(审批通过), DESTROYED(已销毁), FROZEN(冻结), HOLD(保全)';


--
-- Name: acc_archive_attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_archive_attachment (
    id character varying(64) NOT NULL,
    archive_id character varying(64) NOT NULL,
    file_id character varying(64) NOT NULL,
    attachment_type character varying(32) NOT NULL,
    relation_desc character varying(255),
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE acc_archive_attachment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_archive_attachment IS '档案附件关联表';


--
-- Name: COLUMN acc_archive_attachment.archive_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.archive_id IS '档案ID (acc_archive.id)';


--
-- Name: COLUMN acc_archive_attachment.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.file_id IS '文件ID (arc_file_content.id)';


--
-- Name: COLUMN acc_archive_attachment.attachment_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.attachment_type IS '附件类型: invoice/contract/bank_slip/other';


--
-- Name: COLUMN acc_archive_attachment.relation_desc; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.relation_desc IS '关联描述';


--
-- Name: COLUMN acc_archive_attachment.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.created_by IS '创建人ID';


--
-- Name: COLUMN acc_archive_attachment.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.created_time IS '创建时间';


--
-- Name: COLUMN acc_archive_attachment.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_attachment.created_at IS '创建时间';


--
-- Name: acc_archive_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_archive_relation (
    id character varying(32) NOT NULL,
    source_id character varying(32) NOT NULL,
    target_id character varying(32) NOT NULL,
    relation_type character varying(50) NOT NULL,
    relation_desc character varying(255),
    created_by character varying(32),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE acc_archive_relation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_archive_relation IS '档案关联关系表';


--
-- Name: COLUMN acc_archive_relation.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_relation.created_at IS '创建时间';


--
-- Name: acc_archive_volume; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_archive_volume (
    id character varying(32) NOT NULL,
    volume_code character varying(50) NOT NULL,
    title character varying(255),
    fonds_no character varying(50),
    fiscal_year character varying(4),
    fiscal_period character varying(10),
    category_code character varying(50),
    file_count integer DEFAULT 0,
    retention_period character varying(20) NOT NULL,
    volume_status character varying(20) DEFAULT 'draft'::character varying,
    reviewed_by character varying(32),
    reviewed_at timestamp without time zone,
    archived_at timestamp without time zone,
    custodian_dept character varying(32) DEFAULT 'ACCOUNTING'::character varying,
    validation_report_path character varying(255),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE acc_archive_volume; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_archive_volume IS '案卷信息表';


--
-- Name: COLUMN acc_archive_volume.volume_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.volume_code IS '案卷号';


--
-- Name: COLUMN acc_archive_volume.title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.title IS '案卷标题 (格式: 责任者+年度+月度+业务子系统+业务单据名称)';


--
-- Name: COLUMN acc_archive_volume.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.fonds_no IS '全宗号';


--
-- Name: COLUMN acc_archive_volume.fiscal_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.fiscal_year IS '会计年度';


--
-- Name: COLUMN acc_archive_volume.fiscal_period; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.fiscal_period IS '会计期间 (YYYY-MM)';


--
-- Name: COLUMN acc_archive_volume.category_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.category_code IS '分类代号 (AC01=凭证, AC02=账簿, AC03=报告)';


--
-- Name: COLUMN acc_archive_volume.file_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.file_count IS '卷内文件数';


--
-- Name: COLUMN acc_archive_volume.retention_period; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.retention_period IS '保管期限';


--
-- Name: COLUMN acc_archive_volume.volume_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.volume_status IS '状态: draft-草稿, pending-待审核, archived-已归档';


--
-- Name: COLUMN acc_archive_volume.reviewed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.reviewed_by IS '审核人ID';


--
-- Name: COLUMN acc_archive_volume.reviewed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.reviewed_at IS '审核时间';


--
-- Name: COLUMN acc_archive_volume.archived_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.archived_at IS '归档时间';


--
-- Name: COLUMN acc_archive_volume.custodian_dept; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.custodian_dept IS '保管部门: ACCOUNTING-会计, ARCHIVES-档案';


--
-- Name: COLUMN acc_archive_volume.validation_report_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.validation_report_path IS '四性检测报告路径';


--
-- Name: COLUMN acc_archive_volume.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.acc_archive_volume.updated_time IS '更新时间';


--
-- Name: acc_borrow_archive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_borrow_archive (
    id character varying(36) NOT NULL,
    borrow_request_id character varying(36) NOT NULL,
    archive_id character varying(36) NOT NULL,
    archive_code character varying(100) NOT NULL,
    archive_title character varying(500) NOT NULL,
    return_status character varying(20) DEFAULT 'BORROWED'::character varying,
    return_time timestamp without time zone,
    return_operator_id character varying(36),
    damaged boolean DEFAULT false,
    damage_desc character varying(500),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE acc_borrow_archive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_borrow_archive IS '借阅档案明细表';


--
-- Name: acc_borrow_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_borrow_log (
    id character varying(36) NOT NULL,
    request_no character varying(50) NOT NULL,
    applicant_id character varying(36) NOT NULL,
    applicant_name character varying(100) NOT NULL,
    dept_name character varying(200),
    purpose character varying(500),
    borrow_type character varying(20),
    borrow_start_date date,
    borrow_end_date date,
    archive_count integer,
    status character varying(20),
    created_time timestamp without time zone NOT NULL,
    completed_time timestamp without time zone
);


--
-- Name: TABLE acc_borrow_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_borrow_log IS '借阅记录历史表';


--
-- Name: acc_borrow_request; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_borrow_request (
    id character varying(36) NOT NULL,
    request_no character varying(50) NOT NULL,
    applicant_id character varying(36) NOT NULL,
    applicant_name character varying(100) NOT NULL,
    dept_id character varying(36),
    dept_name character varying(200),
    purpose character varying(500) NOT NULL,
    borrow_type character varying(20) NOT NULL,
    expected_start_date date NOT NULL,
    expected_end_date date NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    archive_ids text NOT NULL,
    archive_count integer NOT NULL,
    approver_id character varying(36),
    approver_name character varying(100),
    approval_time timestamp without time zone,
    approval_comment character varying(500),
    actual_start_date date,
    actual_end_date date,
    return_time timestamp without time zone,
    return_operator_id character varying(36),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted numeric(1,0) DEFAULT 0 NOT NULL
);


--
-- Name: TABLE acc_borrow_request; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_borrow_request IS '档案借阅申请表';


--
-- Name: access_review; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.access_review (
    id character varying(32) NOT NULL,
    user_id character varying(32) NOT NULL,
    review_type character varying(20) NOT NULL,
    review_date date NOT NULL,
    reviewer_id character varying(32) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    current_roles text,
    current_permissions text,
    review_result text,
    action_taken text,
    next_review_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE access_review; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.access_review IS '访问权限复核记录表';


--
-- Name: COLUMN access_review.review_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.access_review.review_type IS '复核类型: PERIODIC(定期), AD_HOC(临时), ON_DEMAND(按需)';


--
-- Name: COLUMN access_review.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.access_review.status IS '状态: PENDING(待复核), APPROVED(已批准), REJECTED(已拒绝)';


--
-- Name: arc_abnormal_voucher; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_abnormal_voucher (
    id character varying(50) NOT NULL,
    request_id character varying(100),
    source_system character varying(50),
    voucher_number character varying(100),
    sip_data text,
    fail_reason text,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fonds_code character varying(50)
);


--
-- Name: TABLE arc_abnormal_voucher; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_abnormal_voucher IS '异常凭证数据池';


--
-- Name: COLUMN arc_abnormal_voucher.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.id IS '主键ID';


--
-- Name: COLUMN arc_abnormal_voucher.request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.request_id IS '请求ID';


--
-- Name: COLUMN arc_abnormal_voucher.source_system; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.source_system IS '来源系统';


--
-- Name: COLUMN arc_abnormal_voucher.voucher_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.voucher_number IS '凭证号';


--
-- Name: COLUMN arc_abnormal_voucher.sip_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.sip_data IS '原始SIP数据(JSON)';


--
-- Name: COLUMN arc_abnormal_voucher.fail_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.fail_reason IS '失败原因';


--
-- Name: COLUMN arc_abnormal_voucher.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.status IS '状态: PENDING/RETRYING/IGNORED/RESOLVED';


--
-- Name: COLUMN arc_abnormal_voucher.create_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.create_time IS '创建时间';


--
-- Name: COLUMN arc_abnormal_voucher.update_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.update_time IS '更新时间';


--
-- Name: COLUMN arc_abnormal_voucher.fonds_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_abnormal_voucher.fonds_code IS '全宗号';


--
-- Name: arc_archive_batch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_archive_batch (
    id integer NOT NULL,
    batch_no character varying(64) NOT NULL,
    prev_batch_hash character varying(64),
    current_batch_hash character varying(64),
    chained_hash character varying(64) NOT NULL,
    hash_algo character varying(10) DEFAULT 'SM3'::character varying,
    item_count integer DEFAULT 0,
    operator_id character varying(50),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    batch_sequence bigint NOT NULL
);


--
-- Name: TABLE arc_archive_batch; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_archive_batch IS '归档批次存证表 (哈希链核心)';


--
-- Name: COLUMN arc_archive_batch.chained_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_archive_batch.chained_hash IS '本批次防篡改挂接指纹';


--
-- Name: COLUMN arc_archive_batch.batch_sequence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_archive_batch.batch_sequence IS '批次序列号，用于防止哈希链并发竞态条件';


--
-- Name: arc_archive_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arc_archive_batch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arc_archive_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arc_archive_batch_id_seq OWNED BY public.arc_archive_batch.id;


--
-- Name: arc_batch_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arc_batch_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: SEQUENCE arc_batch_seq; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON SEQUENCE public.arc_batch_seq IS '批次序列生成器，保证哈希链顺序唯一性';


--
-- Name: arc_convert_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_convert_log (
    id character varying(32) NOT NULL,
    archive_id character varying(32) NOT NULL,
    source_format character varying(20) NOT NULL,
    target_format character varying(20) NOT NULL,
    source_path character varying(500),
    target_path character varying(500),
    status character varying(20) NOT NULL,
    error_message text,
    file_size_bytes bigint,
    convert_duration_ms integer,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    source_size bigint,
    target_size bigint,
    duration_ms bigint,
    convert_time timestamp without time zone
);


--
-- Name: TABLE arc_convert_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_convert_log IS '格式转换日志表';


--
-- Name: COLUMN arc_convert_log.source_format; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_convert_log.source_format IS '源格式 (PDF, JPG等)';


--
-- Name: COLUMN arc_convert_log.target_format; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_convert_log.target_format IS '目标格式 (OFD)';


--
-- Name: COLUMN arc_convert_log.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_convert_log.status IS '转换状态: SUCCESS, FAIL';


--
-- Name: COLUMN arc_convert_log.duration_ms; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_convert_log.duration_ms IS '转换耗时（毫秒）';


--
-- Name: arc_file_content; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_file_content (
    id character varying(64) NOT NULL,
    archival_code character varying(100) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_type character varying(20) NOT NULL,
    file_size bigint NOT NULL,
    file_hash character varying(128),
    hash_algorithm character varying(20),
    storage_path character varying(500) NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    item_id character varying(32),
    original_hash character varying(128),
    current_hash character varying(128),
    timestamp_token bytea,
    sign_value bytea,
    certificate text,
    fiscal_year character varying(4),
    voucher_type character varying(50),
    creator character varying(100),
    fonds_code character varying(50),
    source_system character varying(50),
    check_result text,
    checked_time timestamp without time zone,
    archived_time timestamp without time zone,
    business_doc_no character varying(100),
    erp_voucher_no character varying(100),
    source_data text,
    batch_id integer,
    sequence_in_batch integer,
    summary character varying(512),
    voucher_word character varying(64),
    doc_date date,
    highlight_meta jsonb,
    pre_archive_status character varying(20) DEFAULT 'PENDING_CHECK'::character varying NOT NULL
);


--
-- Name: TABLE arc_file_content; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_file_content IS '电子文件存储记录表';


--
-- Name: COLUMN arc_file_content.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.id IS '文件ID';


--
-- Name: COLUMN arc_file_content.archival_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.archival_code IS '档号';


--
-- Name: COLUMN arc_file_content.file_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.file_name IS '文件名';


--
-- Name: COLUMN arc_file_content.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.file_type IS '文件类型 (PDF/OFD/XML)';


--
-- Name: COLUMN arc_file_content.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.file_size IS '文件大小(字节)';


--
-- Name: COLUMN arc_file_content.file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.file_hash IS '文件哈希值';


--
-- Name: COLUMN arc_file_content.hash_algorithm; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.hash_algorithm IS '哈希算法 (SM3/SHA256)';


--
-- Name: COLUMN arc_file_content.storage_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.storage_path IS '存储路径';


--
-- Name: COLUMN arc_file_content.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.created_time IS '创建时间';


--
-- Name: COLUMN arc_file_content.item_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.item_id IS '关联单据ID';


--
-- Name: COLUMN arc_file_content.original_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.original_hash IS '原始哈希值 (接收时)';


--
-- Name: COLUMN arc_file_content.current_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.current_hash IS '当前哈希值 (巡检时)';


--
-- Name: COLUMN arc_file_content.timestamp_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.timestamp_token IS '时间戳Token';


--
-- Name: COLUMN arc_file_content.sign_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.sign_value IS '电子签名值';


--
-- Name: COLUMN arc_file_content.certificate; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.certificate IS '电子签章证书内容（Base64编码）';


--
-- Name: COLUMN arc_file_content.fiscal_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.fiscal_year IS '会计年度';


--
-- Name: COLUMN arc_file_content.voucher_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.voucher_type IS '凭证类型';


--
-- Name: COLUMN arc_file_content.creator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.creator IS '创建人';


--
-- Name: COLUMN arc_file_content.fonds_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.fonds_code IS '全宗号';


--
-- Name: COLUMN arc_file_content.source_system; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.source_system IS '来源系统';


--
-- Name: COLUMN arc_file_content.check_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.check_result IS '四性检测结果（JSON格式）';


--
-- Name: COLUMN arc_file_content.checked_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.checked_time IS '检测时间';


--
-- Name: COLUMN arc_file_content.archived_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.archived_time IS '归档时间';


--
-- Name: COLUMN arc_file_content.business_doc_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.business_doc_no IS '来源唯一标识（幂等性控制，如 YonSuite_xxx）';


--
-- Name: COLUMN arc_file_content.erp_voucher_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.erp_voucher_no IS 'ERP原始凭证号（用户可读，如 记-3）';


--
-- Name: COLUMN arc_file_content.source_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.source_data IS '原始业务数据(JSON)';


--
-- Name: COLUMN arc_file_content.batch_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.batch_id IS '关联的归档批次 ID';


--
-- Name: COLUMN arc_file_content.summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.summary IS '摘要/业务描述';


--
-- Name: COLUMN arc_file_content.voucher_word; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.voucher_word IS '凭证字号 (如 记-1)';


--
-- Name: COLUMN arc_file_content.doc_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.doc_date IS '业务日期';


--
-- Name: COLUMN arc_file_content.highlight_meta; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.highlight_meta IS '文件高亮元数据(坐标信息)';


--
-- Name: COLUMN arc_file_content.pre_archive_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.pre_archive_status IS '简化后的预归档状态：PENDING_CHECK/NEEDS_ACTION/READY_TO_MATCH/READY_TO_ARCHIVE/COMPLETED';


--
-- Name: arc_file_metadata_index; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_file_metadata_index (
    id character varying(64) NOT NULL,
    file_id character varying(64) NOT NULL,
    invoice_code character varying(50),
    invoice_number character varying(50),
    total_amount numeric(18,2),
    seller_name character varying(200),
    issue_date date,
    parsed_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    parser_type character varying(50)
);


--
-- Name: TABLE arc_file_metadata_index; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_file_metadata_index IS '智能解析元数据索引表';


--
-- Name: COLUMN arc_file_metadata_index.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.id IS '索引ID';


--
-- Name: COLUMN arc_file_metadata_index.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.file_id IS '文件ID';


--
-- Name: COLUMN arc_file_metadata_index.invoice_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.invoice_code IS '发票代码';


--
-- Name: COLUMN arc_file_metadata_index.invoice_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.invoice_number IS '发票号码';


--
-- Name: COLUMN arc_file_metadata_index.total_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.total_amount IS '价税合计';


--
-- Name: COLUMN arc_file_metadata_index.seller_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.seller_name IS '销售方名称';


--
-- Name: COLUMN arc_file_metadata_index.issue_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.issue_date IS '开票日期';


--
-- Name: COLUMN arc_file_metadata_index.parsed_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.parsed_time IS '解析时间';


--
-- Name: COLUMN arc_file_metadata_index.parser_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_metadata_index.parser_type IS '解析器类型';


--
-- Name: arc_import_batch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_import_batch (
    id character varying(64) NOT NULL,
    batch_no character varying(64) NOT NULL,
    tenant_id bigint DEFAULT 1,
    source_system character varying(50) NOT NULL,
    voucher_type character varying(32),
    status character varying(20) DEFAULT 'UPLOADING'::character varying,
    total_files integer DEFAULT 0,
    success_count integer DEFAULT 0,
    failed_count integer DEFAULT 0,
    error_details jsonb,
    rollback_reason text,
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    completed_time timestamp without time zone,
    CONSTRAINT chk_batch_status CHECK (((status)::text = ANY (ARRAY[('UPLOADING'::character varying)::text, ('COMPLETED'::character varying)::text, ('FAILED'::character varying)::text, ('ROLLED_BACK'::character varying)::text])))
);


--
-- Name: TABLE arc_import_batch; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_import_batch IS '原始凭证导入批次表 - 支持批量导入追踪与回滚';


--
-- Name: COLUMN arc_import_batch.batch_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_import_batch.batch_no IS '批次编号，格式: IMP-{年月日}-{序号}';


--
-- Name: COLUMN arc_import_batch.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_import_batch.status IS '批次状态: UPLOADING(上传中), COMPLETED(完成), FAILED(失败), ROLLED_BACK(已回滚)';


--
-- Name: arc_original_voucher; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_original_voucher (
    id character varying(64) NOT NULL,
    voucher_no character varying(100) NOT NULL,
    voucher_category character varying(32) NOT NULL,
    voucher_type character varying(32) NOT NULL,
    business_date date NOT NULL,
    amount numeric(18,2),
    currency character varying(10) DEFAULT 'CNY'::character varying,
    counterparty character varying(200),
    summary text,
    creator character varying(100),
    auditor character varying(100),
    bookkeeper character varying(100),
    approver character varying(100),
    source_system character varying(50),
    source_doc_id character varying(200),
    fonds_code character varying(50) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    retention_period character varying(20) NOT NULL,
    archive_status character varying(20) DEFAULT 'DRAFT'::character varying,
    archived_time timestamp without time zone,
    version integer DEFAULT 1,
    parent_version_id character varying(64),
    version_reason text,
    is_latest boolean DEFAULT true,
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_by character varying(64),
    last_modified_time timestamp without time zone,
    deleted integer DEFAULT 0,
    pool_status character varying(20) DEFAULT 'ENTRY'::character varying,
    row_version integer DEFAULT 1,
    pool_batch_id character varying(64),
    parsed_payload jsonb,
    parsed_at timestamp without time zone,
    matched_voucher_id character varying(64),
    matched_at timestamp without time zone,
    deleted_at timestamp without time zone,
    deleted_by character varying(64),
    delete_reason text,
    tenant_id bigint DEFAULT 1
);


--
-- Name: TABLE arc_original_voucher; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_original_voucher IS '原始凭证主表 - 独立于记账凭证，符合DA/T 94-2022';


--
-- Name: COLUMN arc_original_voucher.voucher_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.voucher_no IS '原始凭证编号，格式: OV-{年度}-{类型}-{序号}';


--
-- Name: COLUMN arc_original_voucher.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.version IS '版本号，每次修改递增';


--
-- Name: COLUMN arc_original_voucher.parent_version_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.parent_version_id IS '指向前一版本的ID，形成版本链';


--
-- Name: COLUMN arc_original_voucher.pool_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.pool_status IS '单据池状态: ENTRY(入池), PARSED(已解析), PARSE_FAILED(解析失败), MATCHED(已关联), ARCHIVED(已归档)';


--
-- Name: COLUMN arc_original_voucher.row_version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.row_version IS '乐观锁版本号，每次更新递增';


--
-- Name: COLUMN arc_original_voucher.pool_batch_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.pool_batch_id IS '导入批次ID，关联 arc_import_batch';


--
-- Name: COLUMN arc_original_voucher.parsed_payload; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.parsed_payload IS 'OCR/智能解析结果（JSON格式）';


--
-- Name: COLUMN arc_original_voucher.parsed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.parsed_at IS '解析完成时间';


--
-- Name: COLUMN arc_original_voucher.matched_voucher_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.matched_voucher_id IS '关联的记账凭证ID';


--
-- Name: COLUMN arc_original_voucher.matched_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.matched_at IS '关联完成时间';


--
-- Name: COLUMN arc_original_voucher.tenant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher.tenant_id IS '租户/账套ID，用于数据隔离';


--
-- Name: arc_original_voucher_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_original_voucher_event (
    id bigint NOT NULL,
    voucher_id character varying(64) NOT NULL,
    from_status character varying(20),
    to_status character varying(20) NOT NULL,
    action character varying(50) NOT NULL,
    actor_type character varying(20) DEFAULT 'USER'::character varying,
    actor_id character varying(64),
    actor_name character varying(100),
    occurred_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    request_id character varying(100),
    client_ip character varying(50),
    reason text,
    details jsonb,
    CONSTRAINT chk_action CHECK (((action)::text = ANY (ARRAY[('UPLOAD'::character varying)::text, ('PARSE'::character varying)::text, ('PARSE_RETRY'::character varying)::text, ('MATCH'::character varying)::text, ('UNMATCH'::character varying)::text, ('ARCHIVE'::character varying)::text, ('DELETE'::character varying)::text, ('RESTORE'::character varying)::text, ('ROLLBACK'::character varying)::text, ('MOVE_TYPE'::character varying)::text])))
);


--
-- Name: TABLE arc_original_voucher_event; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_original_voucher_event IS '原始凭证事件溯源表 - 完整记录每一步状态变更';


--
-- Name: COLUMN arc_original_voucher_event.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher_event.action IS '操作类型: UPLOAD(上传), PARSE(解析), MATCH(关联), ARCHIVE(归档), DELETE(删除)等';


--
-- Name: COLUMN arc_original_voucher_event.actor_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_original_voucher_event.actor_type IS '操作者类型: USER(用户), SYSTEM(系统任务)';


--
-- Name: arc_original_voucher_event_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.arc_original_voucher_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: arc_original_voucher_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.arc_original_voucher_event_id_seq OWNED BY public.arc_original_voucher_event.id;


--
-- Name: arc_original_voucher_file; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_original_voucher_file (
    id character varying(64) NOT NULL,
    voucher_id character varying(64) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_type character varying(20) NOT NULL,
    file_size bigint NOT NULL,
    storage_path character varying(500) NOT NULL,
    file_hash character varying(128) NOT NULL,
    hash_algorithm character varying(20) DEFAULT 'SM3'::character varying,
    original_hash character varying(128),
    sign_value bytea,
    sign_cert text,
    sign_time timestamp without time zone,
    timestamp_token bytea,
    file_role character varying(20) DEFAULT 'PRIMARY'::character varying,
    sequence_no integer DEFAULT 1,
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE arc_original_voucher_file; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_original_voucher_file IS '原始凭证文件表 - 支持一凭证多文件';


--
-- Name: arc_original_voucher_sequence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_original_voucher_sequence (
    id character varying(64) NOT NULL,
    fonds_code character varying(50) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    voucher_category character varying(32) NOT NULL,
    current_seq bigint DEFAULT 0,
    last_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE arc_original_voucher_sequence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_original_voucher_sequence IS '原始凭证编号序列表';


--
-- Name: arc_reconciliation_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_reconciliation_record (
    id character varying(64) NOT NULL,
    fonds_code character varying(50) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    fiscal_period character varying(2) NOT NULL,
    subject_code character varying(50),
    subject_name character varying(100),
    erp_debit_total numeric(18,2) DEFAULT 0,
    erp_credit_total numeric(18,2) DEFAULT 0,
    erp_voucher_count integer DEFAULT 0,
    arc_debit_total numeric(18,2) DEFAULT 0,
    arc_credit_total numeric(18,2) DEFAULT 0,
    arc_voucher_count integer DEFAULT 0,
    attachment_count integer DEFAULT 0,
    attachment_missing_count integer DEFAULT 0,
    recon_status character varying(20) NOT NULL,
    recon_message text,
    recon_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    operator_id character varying(64),
    snapshot_data jsonb,
    source_system character varying(100),
    config_id bigint,
    accbook_code character varying(100),
    recon_start_date date,
    recon_end_date date
);


--
-- Name: TABLE arc_reconciliation_record; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_reconciliation_record IS '财务账、凭证与附件一致性核对记录表';


--
-- Name: COLUMN arc_reconciliation_record.erp_debit_total; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.erp_debit_total IS 'ERP侧借方合计';


--
-- Name: COLUMN arc_reconciliation_record.arc_debit_total; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.arc_debit_total IS '档案系统侧借方合计';


--
-- Name: COLUMN arc_reconciliation_record.recon_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.recon_status IS '核对状态: SUCCESS(通过), DISCREPANCY(有差异), ERROR(异常)';


--
-- Name: COLUMN arc_reconciliation_record.config_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.config_id IS 'ERP配置ID';


--
-- Name: COLUMN arc_reconciliation_record.accbook_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.accbook_code IS '账套代码';


--
-- Name: COLUMN arc_reconciliation_record.recon_start_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.recon_start_date IS '核对开始日期';


--
-- Name: COLUMN arc_reconciliation_record.recon_end_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_reconciliation_record.recon_end_date IS '核对结束日期';


--
-- Name: arc_signature_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_signature_log (
    id character varying(32) NOT NULL,
    archive_id character varying(32) NOT NULL,
    file_id character varying(32),
    signer_name character varying(100),
    signer_cert_sn character varying(100),
    signer_org character varying(200),
    sign_time timestamp without time zone,
    sign_algorithm character varying(20) DEFAULT 'SM2'::character varying,
    signature_value text,
    verify_result character varying(20),
    verify_time timestamp without time zone,
    verify_message text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE arc_signature_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_signature_log IS '签章日志表 - 记录电子签章/验签操作';


--
-- Name: COLUMN arc_signature_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.id IS '主键ID';


--
-- Name: COLUMN arc_signature_log.archive_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.archive_id IS '关联的档案ID';


--
-- Name: COLUMN arc_signature_log.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.file_id IS '关联的文件ID';


--
-- Name: COLUMN arc_signature_log.signer_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.signer_name IS '签章人姓名';


--
-- Name: COLUMN arc_signature_log.signer_cert_sn; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.signer_cert_sn IS '证书序列号';


--
-- Name: COLUMN arc_signature_log.signer_org; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.signer_org IS '签章单位';


--
-- Name: COLUMN arc_signature_log.sign_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.sign_time IS '签章时间';


--
-- Name: COLUMN arc_signature_log.sign_algorithm; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.sign_algorithm IS '签名算法(SM2/RSA)';


--
-- Name: COLUMN arc_signature_log.signature_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.signature_value IS '签名值(Base64)';


--
-- Name: COLUMN arc_signature_log.verify_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.verify_result IS '验证结果(VALID/INVALID/UNKNOWN)';


--
-- Name: COLUMN arc_signature_log.verify_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.verify_time IS '验证时间';


--
-- Name: COLUMN arc_signature_log.verify_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.verify_message IS '验证消息';


--
-- Name: COLUMN arc_signature_log.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_signature_log.created_time IS '创建时间';


--
-- Name: arc_voucher_relation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arc_voucher_relation (
    id character varying(64) NOT NULL,
    original_voucher_id character varying(64) NOT NULL,
    accounting_voucher_id character varying(64) NOT NULL,
    relation_type character varying(30) DEFAULT 'ORIGINAL_TO_ACCOUNTING'::character varying,
    relation_desc character varying(200),
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE arc_voucher_relation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.arc_voucher_relation IS '原始凭证与记账凭证多对多关联表';


--
-- Name: archive_amendment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_amendment (
    id bigint NOT NULL,
    archive_id bigint NOT NULL,
    batch_id bigint,
    amendment_type character varying(20) NOT NULL,
    reason text NOT NULL,
    original_content jsonb,
    amended_content jsonb,
    attachment_ids bigint[],
    status character varying(20) DEFAULT 'PENDING'::character varying,
    approved_by bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    created_by bigint,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE archive_amendment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_amendment IS '归档更正记录表 - 归档后的更正/补充/备注';


--
-- Name: archive_amendment_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_amendment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_amendment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_amendment_id_seq OWNED BY public.archive_amendment.id;


--
-- Name: archive_batch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_batch (
    id bigint NOT NULL,
    batch_no character varying(32) NOT NULL,
    fonds_id bigint NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    scope_type character varying(20) DEFAULT 'PERIOD'::character varying,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    voucher_count integer DEFAULT 0,
    doc_count integer DEFAULT 0,
    file_count integer DEFAULT 0,
    total_size_bytes bigint DEFAULT 0,
    validation_report jsonb,
    integrity_report jsonb,
    error_message text,
    submitted_by bigint,
    submitted_at timestamp without time zone,
    approved_by bigint,
    approved_at timestamp without time zone,
    approval_comment text,
    archived_at timestamp without time zone,
    archived_by bigint,
    created_by bigint,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE archive_batch; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_batch IS '归档批次表 - 管理从预归档库到正式档案库的批量归档';


--
-- Name: COLUMN archive_batch.scope_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_batch.scope_type IS '范围类型: PERIOD-按期间, CUSTOM-自定义';


--
-- Name: COLUMN archive_batch.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.archive_batch.status IS '状态: PENDING-待提交, VALIDATING-校验中, APPROVED-已审批, ARCHIVED-已归档, REJECTED-已驳回, FAILED-失败';


--
-- Name: archive_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_batch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_batch_id_seq OWNED BY public.archive_batch.id;


--
-- Name: archive_batch_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archive_batch_item (
    id bigint NOT NULL,
    batch_id bigint NOT NULL,
    item_type character varying(32) NOT NULL,
    ref_id character varying(64) NOT NULL,
    ref_no character varying(64),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    validation_result jsonb,
    hash_sm3 character varying(64),
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE archive_batch_item; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.archive_batch_item IS '归档批次条目表 - 记录批次包含的凭证和单据';


--
-- Name: archive_batch_item_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_batch_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: archive_batch_item_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.archive_batch_item_id_seq OWNED BY public.archive_batch_item.id;


--
-- Name: archive_batch_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.archive_batch_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_inspection_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_inspection_log (
    id character varying(32) NOT NULL,
    archive_id character varying(32) NOT NULL,
    inspection_stage character varying(20) NOT NULL,
    inspection_time timestamp without time zone NOT NULL,
    inspector_id character varying(32),
    is_authentic boolean NOT NULL,
    is_complete boolean NOT NULL,
    is_available boolean NOT NULL,
    is_secure boolean NOT NULL,
    hash_snapshot character varying(128),
    integrity_check jsonb,
    authenticity_check jsonb,
    availability_check jsonb,
    security_check jsonb,
    check_result character varying(20) NOT NULL,
    detail_report jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    report_file_path character varying(500),
    report_file_hash character varying(100),
    is_compliant boolean,
    compliance_violations text,
    compliance_warnings text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE audit_inspection_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.audit_inspection_log IS '四性检测日志表';


--
-- Name: COLUMN audit_inspection_log.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.audit_inspection_log.created_time IS '创建时间';


--
-- Name: auth_ticket; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_ticket (
    id character varying(32) NOT NULL,
    applicant_id character varying(32) NOT NULL,
    applicant_name character varying(100),
    source_fonds character varying(50) NOT NULL,
    target_fonds character varying(50) NOT NULL,
    scope text NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    approval_snapshot text,
    reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    CONSTRAINT chk_auth_ticket_expires CHECK (((expires_at > (created_at + '1 day'::interval)) AND (expires_at <= (created_at + '90 days'::interval))))
);


--
-- Name: TABLE auth_ticket; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.auth_ticket IS '跨全宗访问授权票据表';


--
-- Name: COLUMN auth_ticket.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.id IS '主键ID';


--
-- Name: COLUMN auth_ticket.applicant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.applicant_id IS '申请人ID';


--
-- Name: COLUMN auth_ticket.applicant_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.applicant_name IS '申请人姓名';


--
-- Name: COLUMN auth_ticket.source_fonds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.source_fonds IS '源全宗号（申请人所属全宗）';


--
-- Name: COLUMN auth_ticket.target_fonds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.target_fonds IS '目标全宗号';


--
-- Name: COLUMN auth_ticket.scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.scope IS '访问范围（JSON格式）：{ "archiveYears": [2020, 2021], "docTypes": ["凭证"], "keywords": [], "accessType": "READ_ONLY" }';


--
-- Name: COLUMN auth_ticket.expires_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.expires_at IS '有效期（必须 >= 当前时间 + 1天，<= 当前时间 + 90天）';


--
-- Name: COLUMN auth_ticket.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.status IS '状态: PENDING(待审批), FIRST_APPROVED(第一审批通过), APPROVED(已批准), REJECTED(已拒绝), REVOKED(已撤销), EXPIRED(已过期)';


--
-- Name: COLUMN auth_ticket.approval_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.approval_snapshot IS '审批链快照（JSON格式）：{ "firstApprover": {...}, "secondApprover": {...} }';


--
-- Name: COLUMN auth_ticket.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.reason IS '申请原因';


--
-- Name: COLUMN auth_ticket.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.auth_ticket.deleted IS '逻辑删除标记：0-未删除，1-已删除';


--
-- Name: bas_erp_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bas_erp_config (
    id character varying(32) NOT NULL,
    name character varying(100) NOT NULL,
    adapter_type character varying(50) NOT NULL,
    base_url character varying(500) NOT NULL,
    app_key character varying(200),
    app_secret character varying(500),
    tenant_id character varying(100),
    accbook_code character varying(100),
    extra_config text,
    enabled boolean DEFAULT true,
    created_by character varying(32),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE bas_erp_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.bas_erp_config IS 'ERP 配置表';


--
-- Name: COLUMN bas_erp_config.adapter_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_erp_config.adapter_type IS '适配器类型: yonsuite=用友, kingdee=金蝶, generic=通用';


--
-- Name: COLUMN bas_erp_config.app_secret; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_erp_config.app_secret IS '应用密钥 (SM4 加密存储)';


--
-- Name: COLUMN bas_erp_config.extra_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_erp_config.extra_config IS '额外配置 (JSON格式，用于通用适配器字段映射等)';


--
-- Name: bas_fonds; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bas_fonds (
    id character varying(32) NOT NULL,
    fonds_code character varying(50) NOT NULL,
    fonds_name character varying(100) NOT NULL,
    company_name character varying(100),
    description character varying(500),
    created_by character varying(32),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    org_id character varying(64),
    entity_id character varying(64)
);


--
-- Name: TABLE bas_fonds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.bas_fonds IS '全宗基础信息表';


--
-- Name: COLUMN bas_fonds.org_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_fonds.org_id IS '关联组织ID（公司级）';


--
-- Name: COLUMN bas_fonds.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_fonds.entity_id IS '所属法人ID（管理维度，不作为数据隔离键）';


--
-- Name: bas_location; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bas_location (
    id character varying(64) NOT NULL,
    name character varying(64) NOT NULL,
    code character varying(64),
    type character varying(32) NOT NULL,
    parent_id character varying(64) DEFAULT '0'::character varying,
    path character varying(255),
    capacity integer DEFAULT 0,
    used_count integer DEFAULT 0,
    status character varying(32) DEFAULT 'NORMAL'::character varying,
    rfid_tag character varying(64),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE bas_location; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.bas_location IS '库房位置表';


--
-- Name: COLUMN bas_location.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.id IS '主键ID';


--
-- Name: COLUMN bas_location.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.name IS '位置名称';


--
-- Name: COLUMN bas_location.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.code IS '位置编码';


--
-- Name: COLUMN bas_location.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.type IS '类型: WAREHOUSE, AREA, SHELF, BOX';


--
-- Name: COLUMN bas_location.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.parent_id IS '父级ID';


--
-- Name: COLUMN bas_location.path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.path IS '完整路径';


--
-- Name: COLUMN bas_location.capacity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.capacity IS '容量';


--
-- Name: COLUMN bas_location.used_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.used_count IS '已用数量';


--
-- Name: COLUMN bas_location.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.status IS '状态: NORMAL, FULL, MAINTENANCE';


--
-- Name: COLUMN bas_location.rfid_tag; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.rfid_tag IS 'RFID标签号';


--
-- Name: COLUMN bas_location.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.created_at IS '创建时间';


--
-- Name: COLUMN bas_location.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.updated_at IS '更新时间';


--
-- Name: COLUMN bas_location.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.deleted IS '逻辑删除标识';


--
-- Name: COLUMN bas_location.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.created_time IS '创建时间';


--
-- Name: COLUMN bas_location.last_modified_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bas_location.last_modified_time IS '更新时间';


--
-- Name: biz_appraisal_list; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_appraisal_list (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    archive_year integer NOT NULL,
    appraiser_id character varying(32) NOT NULL,
    appraiser_name character varying(100) NOT NULL,
    appraisal_date date NOT NULL,
    archive_ids text NOT NULL,
    archive_snapshot text NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE biz_appraisal_list; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.biz_appraisal_list IS '鉴定清单表';


--
-- Name: COLUMN biz_appraisal_list.archive_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_appraisal_list.archive_ids IS '待鉴定档案ID列表(JSON数组)';


--
-- Name: COLUMN biz_appraisal_list.archive_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_appraisal_list.archive_snapshot IS '档案元数据快照(JSON格式，包含档案基本信息、保管期限信息等)';


--
-- Name: biz_archive_approval; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_archive_approval (
    id character varying(64) NOT NULL,
    archive_id character varying(64) NOT NULL,
    archive_code character varying(100),
    archive_title character varying(1000),
    applicant_id character varying(64) NOT NULL,
    applicant_name character varying(100),
    application_reason text,
    approver_id character varying(64),
    approver_name character varying(100),
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    approval_comment text,
    approval_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL,
    org_name character varying(255),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: COLUMN biz_archive_approval.org_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_archive_approval.org_name IS '立档单位';


--
-- Name: COLUMN biz_archive_approval.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_archive_approval.created_time IS '创建时间';


--
-- Name: biz_borrowing; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_borrowing (
    id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL,
    user_name character varying(64),
    archive_id character varying(64) NOT NULL,
    archive_title character varying(255),
    reason character varying(512),
    borrow_date date,
    expected_return_date date,
    actual_return_date date,
    status character varying(32) DEFAULT 'PENDING'::character varying,
    approval_comment character varying(512),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    fonds_no character varying(50),
    archive_year integer,
    type character varying(20) DEFAULT 'electronic'::character varying,
    return_deadline date,
    actual_return_time date,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE biz_borrowing; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.biz_borrowing IS '借阅申请表';


--
-- Name: COLUMN biz_borrowing.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.id IS '主键ID';


--
-- Name: COLUMN biz_borrowing.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.user_id IS '申请人ID';


--
-- Name: COLUMN biz_borrowing.user_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.user_name IS '申请人姓名';


--
-- Name: COLUMN biz_borrowing.archive_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.archive_id IS '借阅档案ID';


--
-- Name: COLUMN biz_borrowing.archive_title; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.archive_title IS '档案题名';


--
-- Name: COLUMN biz_borrowing.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.reason IS '借阅原因';


--
-- Name: COLUMN biz_borrowing.borrow_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.borrow_date IS '借阅日期';


--
-- Name: COLUMN biz_borrowing.expected_return_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.expected_return_date IS '预计归还日期';


--
-- Name: COLUMN biz_borrowing.actual_return_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.actual_return_date IS '实际归还日期';


--
-- Name: COLUMN biz_borrowing.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.status IS '状态: PENDING, APPROVED, REJECTED, RETURNED, CANCELLED';


--
-- Name: COLUMN biz_borrowing.approval_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.approval_comment IS '审批意见';


--
-- Name: COLUMN biz_borrowing.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.created_at IS '创建时间';


--
-- Name: COLUMN biz_borrowing.last_modified_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.last_modified_time IS '更新时间';


--
-- Name: COLUMN biz_borrowing.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.deleted IS '逻辑删除标识';


--
-- Name: COLUMN biz_borrowing.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.fonds_no IS '全宗号';


--
-- Name: COLUMN biz_borrowing.archive_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.archive_year IS '归档年度';


--
-- Name: COLUMN biz_borrowing.type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.type IS '借阅类型: electronic/physical';


--
-- Name: COLUMN biz_borrowing.return_deadline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.return_deadline IS '归还截止日期';


--
-- Name: COLUMN biz_borrowing.actual_return_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.actual_return_time IS '实际归还时间';


--
-- Name: COLUMN biz_borrowing.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.updated_at IS '更新时间';


--
-- Name: biz_destruction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_destruction (
    id character varying(64) NOT NULL,
    applicant_id character varying(64) NOT NULL,
    applicant_name character varying(64),
    reason character varying(512),
    archive_count integer DEFAULT 0,
    archive_ids text,
    status character varying(32) DEFAULT 'PENDING'::character varying,
    approver_id character varying(64),
    approver_name character varying(64),
    approval_comment character varying(512),
    approval_time timestamp without time zone,
    execution_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    appraiser_id character varying(32),
    appraiser_name character varying(100),
    appraisal_date date,
    appraisal_conclusion character varying(20),
    appraisal_comment text,
    appraisal_list_id character varying(32),
    approval_snapshot text
);


--
-- Name: TABLE biz_destruction; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.biz_destruction IS '销毁申请表';


--
-- Name: COLUMN biz_destruction.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.id IS '主键ID';


--
-- Name: COLUMN biz_destruction.applicant_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.applicant_id IS '申请人ID';


--
-- Name: COLUMN biz_destruction.applicant_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.applicant_name IS '申请人姓名';


--
-- Name: COLUMN biz_destruction.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.reason IS '销毁原因';


--
-- Name: COLUMN biz_destruction.archive_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.archive_count IS '待销毁档案数量';


--
-- Name: COLUMN biz_destruction.archive_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.archive_ids IS '待销毁档案ID列表(JSON)';


--
-- Name: COLUMN biz_destruction.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.status IS '状态: PENDING(待审批), FIRST_APPROVED(初审通过), DESTRUCTION_APPROVED(审批通过), REJECTED(已拒绝), EXECUTED(已执行)';


--
-- Name: COLUMN biz_destruction.approver_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.approver_id IS '审批人ID';


--
-- Name: COLUMN biz_destruction.approver_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.approver_name IS '审批人姓名';


--
-- Name: COLUMN biz_destruction.approval_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.approval_comment IS '审批意见';


--
-- Name: COLUMN biz_destruction.approval_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.approval_time IS '审批时间';


--
-- Name: COLUMN biz_destruction.execution_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.execution_time IS '执行时间';


--
-- Name: COLUMN biz_destruction.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.created_at IS '创建时间';


--
-- Name: COLUMN biz_destruction.last_modified_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.last_modified_time IS '更新时间';


--
-- Name: COLUMN biz_destruction.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.deleted IS '逻辑删除标识';


--
-- Name: COLUMN biz_destruction.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.created_time IS '创建时间';


--
-- Name: COLUMN biz_destruction.appraiser_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.appraiser_id IS '鉴定人ID';


--
-- Name: COLUMN biz_destruction.appraiser_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.appraiser_name IS '鉴定人姓名';


--
-- Name: COLUMN biz_destruction.appraisal_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.appraisal_date IS '鉴定日期';


--
-- Name: COLUMN biz_destruction.appraisal_conclusion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.appraisal_conclusion IS '鉴定结论: APPROVED(同意销毁), REJECTED(不同意销毁), DEFERRED(延期保管)';


--
-- Name: COLUMN biz_destruction.appraisal_comment; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.appraisal_comment IS '鉴定意见';


--
-- Name: COLUMN biz_destruction.appraisal_list_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.appraisal_list_id IS '鉴定清单ID（用于关联鉴定清单记录）';


--
-- Name: COLUMN biz_destruction.approval_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.approval_snapshot IS '审批链快照(JSON格式，包含初审和复核的完整审批信息)';


--
-- Name: biz_open_appraisal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.biz_open_appraisal (
    id character varying(64) NOT NULL,
    archive_id character varying(64) NOT NULL,
    archive_code character varying(100),
    archive_title character varying(500),
    retention_period character varying(20),
    current_security_level character varying(20),
    appraiser_id character varying(64),
    appraiser_name character varying(100),
    appraisal_date date,
    appraisal_result character varying(20),
    open_level character varying(20),
    reason text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE biz_open_appraisal; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.biz_open_appraisal IS '开放鉴定表';


--
-- Name: COLUMN biz_open_appraisal.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_open_appraisal.created_time IS '创建时间';


--
-- Name: cfg_account_role_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cfg_account_role_mapping (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    account_code character varying(50) NOT NULL,
    aux_type character varying(50),
    account_role character varying(30) NOT NULL,
    source character varying(20) DEFAULT 'PRESET'::character varying,
    created_time timestamp without time zone DEFAULT now(),
    updated_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE cfg_account_role_mapping; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cfg_account_role_mapping IS '客户科目角色映射';


--
-- Name: COLUMN cfg_account_role_mapping.aux_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cfg_account_role_mapping.aux_type IS '辅助核算类别：PERSONAL/COMPANY/PROJECT/NONE';


--
-- Name: COLUMN cfg_account_role_mapping.source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cfg_account_role_mapping.source IS '来源：PRESET/MANUAL';


--
-- Name: cfg_account_role_mapping_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cfg_account_role_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cfg_account_role_mapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cfg_account_role_mapping_id_seq OWNED BY public.cfg_account_role_mapping.id;


--
-- Name: cfg_account_role_preset; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cfg_account_role_preset (
    id bigint NOT NULL,
    kit_id character varying(50) NOT NULL,
    account_pattern character varying(100) NOT NULL,
    account_role character varying(30) NOT NULL,
    priority integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE cfg_account_role_preset; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cfg_account_role_preset IS '科目角色预置规则（正则匹配）';


--
-- Name: cfg_account_role_preset_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cfg_account_role_preset_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cfg_account_role_preset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cfg_account_role_preset_id_seq OWNED BY public.cfg_account_role_preset.id;


--
-- Name: cfg_doc_type_mapping; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cfg_doc_type_mapping (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    customer_doc_type character varying(100) NOT NULL,
    evidence_role character varying(30) NOT NULL,
    display_name character varying(100),
    source character varying(20) DEFAULT 'PRESET'::character varying,
    created_time timestamp without time zone DEFAULT now(),
    updated_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE cfg_doc_type_mapping; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cfg_doc_type_mapping IS '客户单据类型映射';


--
-- Name: cfg_doc_type_mapping_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cfg_doc_type_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cfg_doc_type_mapping_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cfg_doc_type_mapping_id_seq OWNED BY public.cfg_doc_type_mapping.id;


--
-- Name: cfg_doc_type_preset; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cfg_doc_type_preset (
    id bigint NOT NULL,
    kit_id character varying(50) NOT NULL,
    doc_type_pattern character varying(100) NOT NULL,
    keywords text[],
    evidence_role character varying(30) NOT NULL,
    priority integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE cfg_doc_type_preset; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cfg_doc_type_preset IS '单据类型预置规则';


--
-- Name: cfg_doc_type_preset_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cfg_doc_type_preset_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cfg_doc_type_preset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cfg_doc_type_preset_id_seq OWNED BY public.cfg_doc_type_preset.id;


--
-- Name: cfg_preset_kit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cfg_preset_kit (
    id character varying(50) NOT NULL,
    industry character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    is_default boolean DEFAULT false,
    created_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE cfg_preset_kit; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cfg_preset_kit IS '行业预置规则包';


--
-- Name: COLUMN cfg_preset_kit.industry; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cfg_preset_kit.industry IS '行业类型：GENERAL/TRADE/MANUFACTURING';


--
-- Name: collection_batch; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collection_batch (
    id bigint NOT NULL,
    batch_no character varying(50) NOT NULL,
    batch_name character varying(200) NOT NULL,
    fonds_id character varying(32) NOT NULL,
    fonds_code character varying(20) NOT NULL,
    fiscal_year character varying(10) NOT NULL,
    fiscal_period character varying(20),
    archival_category character varying(50) NOT NULL,
    source_channel character varying(50) DEFAULT 'WEB上传'::character varying NOT NULL,
    status character varying(20) DEFAULT 'UPLOADING'::character varying NOT NULL,
    total_files integer DEFAULT 0 NOT NULL,
    uploaded_files integer DEFAULT 0 NOT NULL,
    failed_files integer DEFAULT 0 NOT NULL,
    total_size_bytes bigint DEFAULT 0 NOT NULL,
    validation_report jsonb,
    error_message text,
    created_by bigint NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_time timestamp without time zone,
    CONSTRAINT chk_collection_batch_status CHECK (((status)::text = ANY (ARRAY[('UPLOADING'::character varying)::text, ('UPLOADED'::character varying)::text, ('VALIDATING'::character varying)::text, ('VALIDATED'::character varying)::text, ('FAILED'::character varying)::text, ('ARCHIVED'::character varying)::text])))
);


--
-- Name: TABLE collection_batch; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.collection_batch IS '资料收集批次表 - 管理批量上传会话';


--
-- Name: COLUMN collection_batch.batch_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.batch_no IS '批次编号 (格式: COL-YYYYMMDD-NNN)';


--
-- Name: COLUMN collection_batch.batch_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.batch_name IS '批次名称';


--
-- Name: COLUMN collection_batch.fonds_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.fonds_id IS '全宗ID';


--
-- Name: COLUMN collection_batch.fonds_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.fonds_code IS '全宗代码';


--
-- Name: COLUMN collection_batch.fiscal_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.fiscal_year IS '会计年度';


--
-- Name: COLUMN collection_batch.fiscal_period; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.fiscal_period IS '会计期间';


--
-- Name: COLUMN collection_batch.archival_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.archival_category IS '档案门类 (VOUCHER/LEDGER/REPORT/OTHER)';


--
-- Name: COLUMN collection_batch.source_channel; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.source_channel IS '来源渠道';


--
-- Name: COLUMN collection_batch.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.status IS '批次状态';


--
-- Name: COLUMN collection_batch.validation_report; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch.validation_report IS '四性检测汇总报告 (JSONB)';


--
-- Name: collection_batch_file; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.collection_batch_file (
    id bigint NOT NULL,
    batch_id bigint NOT NULL,
    file_id character varying(50),
    original_filename character varying(500) NOT NULL,
    file_size_bytes bigint NOT NULL,
    file_type character varying(20),
    file_hash character varying(128),
    hash_algorithm character varying(20) DEFAULT 'SHA-256'::character varying,
    upload_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    processing_result jsonb,
    error_message text,
    upload_order integer NOT NULL,
    started_time timestamp without time zone,
    completed_time timestamp without time zone,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archive_id character varying(50),
    CONSTRAINT chk_upload_status CHECK (((upload_status)::text = ANY (ARRAY[('PENDING'::character varying)::text, ('UPLOADING'::character varying)::text, ('UPLOADED'::character varying)::text, ('FAILED'::character varying)::text, ('DUPLICATE'::character varying)::text, ('VALIDATING'::character varying)::text, ('VALIDATED'::character varying)::text, ('CHECK_FAILED'::character varying)::text])))
);


--
-- Name: TABLE collection_batch_file; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.collection_batch_file IS '资料收集批次文件表 - 记录批次内每个文件的上传和处理状态';


--
-- Name: COLUMN collection_batch_file.batch_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.batch_id IS '所属批次ID';


--
-- Name: COLUMN collection_batch_file.file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.file_id IS '关联的文件ID (arc_file_content.id)';


--
-- Name: COLUMN collection_batch_file.original_filename; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.original_filename IS '原始文件名';


--
-- Name: COLUMN collection_batch_file.file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.file_hash IS '文件哈希值 (用于幂等性控制)';


--
-- Name: COLUMN collection_batch_file.upload_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.upload_status IS '上传状态';


--
-- Name: COLUMN collection_batch_file.processing_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.processing_result IS '处理结果 (包含四性检测报告)';


--
-- Name: COLUMN collection_batch_file.archive_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.collection_batch_file.archive_id IS '关联的档案ID (acc_archive.id) - 上传完成后立即创建的档案记录，用于凭证关联';


--
-- Name: collection_batch_file_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.collection_batch_file_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: collection_batch_file_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.collection_batch_file_id_seq OWNED BY public.collection_batch_file.id;


--
-- Name: collection_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.collection_batch_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: collection_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.collection_batch_id_seq OWNED BY public.collection_batch.id;


--
-- Name: destruction_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.destruction_log (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    archive_year integer NOT NULL,
    archive_object_id character varying(32) NOT NULL,
    retention_policy_id character varying(32) NOT NULL,
    approval_ticket_id character varying(64) NOT NULL,
    destroyed_by character varying(32) NOT NULL,
    destroyed_at timestamp without time zone NOT NULL,
    trace_id character varying(64) NOT NULL,
    snapshot text NOT NULL,
    prev_hash character varying(128),
    curr_hash character varying(128),
    sig text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE destruction_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.destruction_log IS '销毁清册表（永久只读，禁止修改/删除）';


--
-- Name: COLUMN destruction_log.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.id IS '主键ID';


--
-- Name: COLUMN destruction_log.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.fonds_no IS '全宗号';


--
-- Name: COLUMN destruction_log.archive_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.archive_year IS '归档年度';


--
-- Name: COLUMN destruction_log.archive_object_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.archive_object_id IS '档案ID';


--
-- Name: COLUMN destruction_log.retention_policy_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.retention_policy_id IS '保管期限ID';


--
-- Name: COLUMN destruction_log.approval_ticket_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.approval_ticket_id IS '审批票据ID（销毁申请ID）';


--
-- Name: COLUMN destruction_log.destroyed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.destroyed_by IS '执行人ID';


--
-- Name: COLUMN destruction_log.destroyed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.destroyed_at IS '销毁时间';


--
-- Name: COLUMN destruction_log.trace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.trace_id IS '追踪ID（用于审计）';


--
-- Name: COLUMN destruction_log.snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.snapshot IS '档案元数据快照（JSON格式，包含完整信息）';


--
-- Name: COLUMN destruction_log.prev_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.prev_hash IS '前一条记录的哈希值（哈希链）';


--
-- Name: COLUMN destruction_log.curr_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.curr_hash IS '当前记录的哈希值（哈希链）';


--
-- Name: COLUMN destruction_log.sig; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.destruction_log.sig IS '数字签名（可选）';


--
-- Name: employee_lifecycle_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employee_lifecycle_event (
    id character varying(32) NOT NULL,
    employee_id character varying(32) NOT NULL,
    employee_name character varying(100),
    event_type character varying(20) NOT NULL,
    event_date date NOT NULL,
    previous_dept_id character varying(32),
    new_dept_id character varying(32),
    previous_role_ids text,
    new_role_ids text,
    reason text,
    processed boolean DEFAULT false,
    processed_at timestamp without time zone,
    processed_by character varying(32),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE employee_lifecycle_event; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.employee_lifecycle_event IS '员工生命周期事件表';


--
-- Name: COLUMN employee_lifecycle_event.event_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.employee_lifecycle_event.event_type IS '事件类型: ONBOARD(入职), OFFBOARD(离职), TRANSFER(调岗)';


--
-- Name: COLUMN employee_lifecycle_event.processed; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.employee_lifecycle_event.processed IS '是否已处理：false-待处理，true-已处理';


--
-- Name: file_hash_dedup_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.file_hash_dedup_scope (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    scope_type character varying(20) NOT NULL,
    enabled boolean DEFAULT true,
    created_by character varying(32),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE file_hash_dedup_scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.file_hash_dedup_scope IS '文件哈希去重范围配置表';


--
-- Name: COLUMN file_hash_dedup_scope.scope_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_hash_dedup_scope.scope_type IS '去重范围: SAME_FONDS(同全宗), AUTHORIZED(授权范围), GLOBAL(全局)';


--
-- Name: file_storage_policy; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.file_storage_policy (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    policy_type character varying(20) NOT NULL,
    retention_days integer,
    immutable_until date,
    enabled boolean DEFAULT true,
    created_by character varying(32),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE file_storage_policy; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.file_storage_policy IS '文件存储策略配置表';


--
-- Name: COLUMN file_storage_policy.policy_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.file_storage_policy.policy_type IS '策略类型: IMMUTABLE(不可变), RETENTION(保留策略)';


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: fonds_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fonds_history (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    event_type character varying(20) NOT NULL,
    from_fonds_no character varying(50),
    to_fonds_no character varying(50),
    effective_date date NOT NULL,
    reason text,
    approval_ticket_id character varying(64),
    snapshot_json text,
    created_by character varying(32),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE fonds_history; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.fonds_history IS '全宗沿革表（历史追溯）';


--
-- Name: COLUMN fonds_history.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.id IS '主键ID';


--
-- Name: COLUMN fonds_history.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.fonds_no IS '全宗号（当前全宗）';


--
-- Name: COLUMN fonds_history.event_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.event_type IS '事件类型: MERGE(合并), SPLIT(分立), MIGRATE(迁移), RENAME(重命名)';


--
-- Name: COLUMN fonds_history.from_fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.from_fonds_no IS '源全宗号（用于合并/迁移场景）';


--
-- Name: COLUMN fonds_history.to_fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.to_fonds_no IS '目标全宗号（用于迁移场景）';


--
-- Name: COLUMN fonds_history.effective_date; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.effective_date IS '生效日期';


--
-- Name: COLUMN fonds_history.reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.reason IS '变更原因';


--
-- Name: COLUMN fonds_history.approval_ticket_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.approval_ticket_id IS '审批票据ID（关联审批流程）';


--
-- Name: COLUMN fonds_history.snapshot_json; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.snapshot_json IS '变更时的快照信息（JSON格式）：包含全宗信息、档案数量等';


--
-- Name: COLUMN fonds_history.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.created_by IS '创建人ID';


--
-- Name: COLUMN fonds_history.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.fonds_history.deleted IS '逻辑删除标记：0-未删除，1-已删除';


--
-- Name: integrity_check; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.integrity_check (
    id bigint NOT NULL,
    target_type character varying(32) NOT NULL,
    target_id bigint NOT NULL,
    check_type character varying(32) NOT NULL,
    result character varying(20) NOT NULL,
    hash_expected character varying(64),
    hash_actual character varying(64),
    signature_valid boolean,
    details jsonb,
    checked_at timestamp without time zone DEFAULT now(),
    checked_by bigint
);


--
-- Name: TABLE integrity_check; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.integrity_check IS '四性检测结果表 - 真实性、完整性、可用性、安全性检测';


--
-- Name: COLUMN integrity_check.check_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.integrity_check.check_type IS '检测类型: AUTHENTICITY-真实性, INTEGRITY-完整性, USABILITY-可用性, SECURITY-安全性';


--
-- Name: integrity_check_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.integrity_check_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: integrity_check_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.integrity_check_id_seq OWNED BY public.integrity_check.id;


--
-- Name: legacy_import_task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.legacy_import_task (
    id character varying(32) NOT NULL,
    operator_id character varying(32) NOT NULL,
    operator_name character varying(100),
    fonds_no character varying(50) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    file_hash character varying(64),
    total_rows integer NOT NULL,
    success_rows integer DEFAULT 0 NOT NULL,
    failed_rows integer DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    error_report_path character varying(500),
    created_fonds_nos text,
    created_entity_ids text,
    started_at timestamp without time zone,
    completed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE legacy_import_task; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.legacy_import_task IS '历史数据导入任务表';


--
-- Name: COLUMN legacy_import_task.operator_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.operator_id IS '操作人ID';


--
-- Name: COLUMN legacy_import_task.operator_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.operator_name IS '操作人姓名';


--
-- Name: COLUMN legacy_import_task.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.fonds_no IS '全宗号';


--
-- Name: COLUMN legacy_import_task.file_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.file_name IS '文件名';


--
-- Name: COLUMN legacy_import_task.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.file_size IS '文件大小（字节）';


--
-- Name: COLUMN legacy_import_task.file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.file_hash IS '文件哈希值';


--
-- Name: COLUMN legacy_import_task.total_rows; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.total_rows IS '总行数';


--
-- Name: COLUMN legacy_import_task.success_rows; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.success_rows IS '成功行数';


--
-- Name: COLUMN legacy_import_task.failed_rows; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.failed_rows IS '失败行数';


--
-- Name: COLUMN legacy_import_task.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.status IS '状态: PENDING(待处理), PROCESSING(处理中), SUCCESS(成功), FAILED(失败), PARTIAL_SUCCESS(部分成功)';


--
-- Name: COLUMN legacy_import_task.error_report_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.error_report_path IS '错误报告文件路径';


--
-- Name: COLUMN legacy_import_task.created_fonds_nos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.created_fonds_nos IS '自动创建的全宗号列表（JSON 数组格式）';


--
-- Name: COLUMN legacy_import_task.created_entity_ids; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.created_entity_ids IS '自动创建的实体ID列表（JSON 数组格式）';


--
-- Name: COLUMN legacy_import_task.started_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.started_at IS '开始时间';


--
-- Name: COLUMN legacy_import_task.completed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.completed_at IS '完成时间';


--
-- Name: COLUMN legacy_import_task.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.legacy_import_task.created_at IS '创建时间';


--
-- Name: match_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.match_log (
    id bigint NOT NULL,
    match_batch_id character varying(50),
    voucher_id character varying(50),
    action character varying(30) NOT NULL,
    evidence_role character varying(30),
    source_doc_id character varying(50),
    score integer,
    reasons text[],
    before_state jsonb,
    after_state jsonb,
    is_manual_override boolean DEFAULT false,
    operator_id bigint,
    operator_name character varying(100),
    client_ip character varying(50),
    operation_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE match_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.match_log IS '匹配日志（审计与自学习）';


--
-- Name: match_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.match_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: match_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.match_log_id_seq OWNED BY public.match_log.id;


--
-- Name: match_rule_template; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.match_rule_template (
    id character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    version character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    scene character varying(50) NOT NULL,
    config jsonb NOT NULL,
    description text,
    created_time timestamp without time zone DEFAULT now(),
    updated_time timestamp without time zone DEFAULT now(),
    updated_by bigint
);


--
-- Name: TABLE match_rule_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.match_rule_template IS '智能关联规则模板';


--
-- Name: period_lock; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.period_lock (
    id bigint NOT NULL,
    fonds_id bigint NOT NULL,
    period character varying(7) NOT NULL,
    lock_type character varying(20) NOT NULL,
    locked_at timestamp without time zone NOT NULL,
    locked_by bigint,
    unlock_at timestamp without time zone,
    unlock_by bigint,
    reason text
);


--
-- Name: TABLE period_lock; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.period_lock IS '期间锁定表 - 控制会计期间的修改权限';


--
-- Name: COLUMN period_lock.lock_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.period_lock.lock_type IS '锁定类型: ERP_CLOSED-ERP结账, ARCHIVED-已归档, AUDIT_LOCKED-审计锁定';


--
-- Name: period_lock_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.period_lock_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: period_lock_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.period_lock_id_seq OWNED BY public.period_lock.id;


--
-- Name: scan_folder_monitor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scan_folder_monitor (
    id bigint NOT NULL,
    user_id character varying(64) NOT NULL,
    folder_path character varying(500) NOT NULL,
    is_active boolean DEFAULT true,
    file_filter character varying(200) DEFAULT '*.pdf;*.jpg;*.jpeg;*.png'::character varying,
    auto_delete boolean DEFAULT false,
    move_to_path character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE scan_folder_monitor; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.scan_folder_monitor IS '文件监控配置表';


--
-- Name: COLUMN scan_folder_monitor.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.id IS '主键ID';


--
-- Name: COLUMN scan_folder_monitor.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.user_id IS '用户ID';


--
-- Name: COLUMN scan_folder_monitor.folder_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.folder_path IS '监控文件夹路径';


--
-- Name: COLUMN scan_folder_monitor.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.is_active IS '是否启用';


--
-- Name: COLUMN scan_folder_monitor.file_filter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.file_filter IS '文件类型过滤';


--
-- Name: COLUMN scan_folder_monitor.auto_delete; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.auto_delete IS '导入后是否删除源文件';


--
-- Name: COLUMN scan_folder_monitor.move_to_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.move_to_path IS '导入后移动到的路径（可选）';


--
-- Name: COLUMN scan_folder_monitor.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.created_at IS '创建时间';


--
-- Name: COLUMN scan_folder_monitor.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_folder_monitor.updated_at IS '更新时间';


--
-- Name: scan_folder_monitor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.scan_folder_monitor_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: scan_folder_monitor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.scan_folder_monitor_id_seq OWNED BY public.scan_folder_monitor.id;


--
-- Name: scan_workspace; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scan_workspace (
    id bigint NOT NULL,
    session_id character varying(64),
    user_id character varying(64) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(500) NOT NULL,
    file_size bigint,
    file_type character varying(50),
    upload_source character varying(50) NOT NULL,
    ocr_status character varying(50) DEFAULT 'pending'::character varying NOT NULL,
    ocr_engine character varying(50),
    ocr_result jsonb,
    overall_score integer,
    doc_type character varying(50),
    submit_status character varying(50) DEFAULT 'draft'::character varying,
    archive_id character varying(64),
    submitted_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fonds_code character varying(32)
);


--
-- Name: TABLE scan_workspace; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.scan_workspace IS '扫描工作区：存储临时扫描文件和OCR识别结果';


--
-- Name: COLUMN scan_workspace.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.id IS '主键ID';


--
-- Name: COLUMN scan_workspace.session_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.session_id IS '会话ID（用于移动端关联，PC端上传可为空）';


--
-- Name: COLUMN scan_workspace.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.user_id IS '用户ID';


--
-- Name: COLUMN scan_workspace.file_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.file_name IS '原始文件名';


--
-- Name: COLUMN scan_workspace.file_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.file_path IS '文件存储路径';


--
-- Name: COLUMN scan_workspace.file_size; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.file_size IS '文件大小（字节）';


--
-- Name: COLUMN scan_workspace.file_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.file_type IS '文件类型（pdf, jpg, png等）';


--
-- Name: COLUMN scan_workspace.upload_source; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.upload_source IS '上传来源（upload, monitor, mobile）';


--
-- Name: COLUMN scan_workspace.ocr_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.ocr_status IS 'OCR状态（pending, processing, review, completed, failed）';


--
-- Name: COLUMN scan_workspace.ocr_engine; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.ocr_engine IS '使用的OCR引擎（paddleocr, baidu, aliyun）';


--
-- Name: COLUMN scan_workspace.ocr_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.ocr_result IS 'OCR识别结果（结构化数据）';


--
-- Name: COLUMN scan_workspace.overall_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.overall_score IS '整体置信度分数';


--
-- Name: COLUMN scan_workspace.doc_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.doc_type IS '文档类型（invoice, contract, receipt等）';


--
-- Name: COLUMN scan_workspace.submit_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.submit_status IS '提交状态（draft, submitted）';


--
-- Name: COLUMN scan_workspace.archive_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.archive_id IS '提交后关联的档案ID';


--
-- Name: COLUMN scan_workspace.submitted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.submitted_at IS '提交时间';


--
-- Name: COLUMN scan_workspace.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.created_at IS '创建时间';


--
-- Name: COLUMN scan_workspace.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.updated_at IS '更新时间';


--
-- Name: COLUMN scan_workspace.fonds_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scan_workspace.fonds_code IS '所属全宗代码';


--
-- Name: scan_workspace_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.scan_workspace_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: scan_workspace_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.scan_workspace_id_seq OWNED BY public.scan_workspace.id;


--
-- Name: search_performance_stats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.search_performance_stats (
    id character varying(32) NOT NULL,
    fonds_no character varying(50),
    search_type character varying(50) NOT NULL,
    search_duration_ms integer,
    result_count integer,
    user_id character varying(32),
    recorded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE search_performance_stats; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.search_performance_stats IS '检索性能统计表';


--
-- Name: storage_capacity_stats; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.storage_capacity_stats (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    total_size_gb numeric(18,2) NOT NULL,
    used_size_gb numeric(18,2) NOT NULL,
    file_count bigint NOT NULL,
    recorded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE storage_capacity_stats; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.storage_capacity_stats IS '文件存储容量统计表';


--
-- Name: sys_archival_code_sequence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_archival_code_sequence (
    fonds_code character varying(50) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    category_code character varying(10) NOT NULL,
    current_val integer DEFAULT 0,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_archival_code_sequence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_archival_code_sequence IS '档号生成计数器';


--
-- Name: sys_audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_audit_log (
    id character varying(64) NOT NULL,
    user_id character varying(64),
    username character varying(255),
    role_type character varying(50),
    action character varying(50) NOT NULL,
    resource_type character varying(50),
    resource_id character varying(64),
    operation_result character varying(50),
    risk_level character varying(20),
    details text,
    data_before text,
    data_after text,
    session_id character varying(64),
    client_ip character varying(50),
    mac_address character varying(64) DEFAULT 'UNKNOWN'::character varying,
    object_digest character varying(128),
    user_agent character varying(500),
    prev_log_hash character varying(128),
    log_hash character varying(128),
    device_fingerprint character varying(255),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ip_address character varying(64),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    trace_id character varying(64),
    data_snapshot text,
    source_fonds character varying(50),
    target_fonds character varying(50),
    auth_ticket_id character varying(64)
);


--
-- Name: TABLE sys_audit_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_audit_log IS '安全审计日志表';


--
-- Name: COLUMN sys_audit_log.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.action IS '操作类型: CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD';


--
-- Name: COLUMN sys_audit_log.data_before; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.data_before IS '操作前数据快照';


--
-- Name: COLUMN sys_audit_log.data_after; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.data_after IS '操作后数据快照';


--
-- Name: COLUMN sys_audit_log.client_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.client_ip IS '客户端IP地址';


--
-- Name: COLUMN sys_audit_log.mac_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.mac_address IS 'MAC地址';


--
-- Name: COLUMN sys_audit_log.object_digest; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.object_digest IS '被操作对象的哈希值';


--
-- Name: COLUMN sys_audit_log.prev_log_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.prev_log_hash IS '前一条日志的SM3哈希值';


--
-- Name: COLUMN sys_audit_log.log_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.log_hash IS '当前日志的SM3哈希值';


--
-- Name: COLUMN sys_audit_log.device_fingerprint; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.device_fingerprint IS '客户端设备指纹';


--
-- Name: COLUMN sys_audit_log.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.ip_address IS 'IP地址';


--
-- Name: COLUMN sys_audit_log.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.created_at IS '创建时间';


--
-- Name: COLUMN sys_audit_log.trace_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.trace_id IS 'TraceID(全链路追踪)';


--
-- Name: COLUMN sys_audit_log.data_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.data_snapshot IS '脱敏后的快照';


--
-- Name: COLUMN sys_audit_log.source_fonds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.source_fonds IS '跨全宗访问源全宗';


--
-- Name: COLUMN sys_audit_log.target_fonds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.target_fonds IS '跨全宗访问目标全宗';


--
-- Name: COLUMN sys_audit_log.auth_ticket_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_audit_log.auth_ticket_id IS '跨全宗授权票据ID';


--
-- Name: sys_entity; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_entity (
    id character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    tax_id character varying(50),
    address character varying(500),
    contact_person character varying(100),
    contact_phone character varying(50),
    contact_email character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    description character varying(1000),
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted smallint DEFAULT 0,
    parent_id character varying(64),
    order_num integer DEFAULT 0
);


--
-- Name: TABLE sys_entity; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_entity IS '法人实体表（管理维度，不作为数据隔离键）';


--
-- Name: COLUMN sys_entity.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.name IS '法人名称';


--
-- Name: COLUMN sys_entity.tax_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.tax_id IS '统一社会信用代码/税号';


--
-- Name: COLUMN sys_entity.address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.address IS '注册地址';


--
-- Name: COLUMN sys_entity.contact_person; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.contact_person IS '联系人';


--
-- Name: COLUMN sys_entity.contact_phone; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.contact_phone IS '联系电话';


--
-- Name: COLUMN sys_entity.contact_email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.contact_email IS '联系邮箱';


--
-- Name: COLUMN sys_entity.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.status IS '状态: ACTIVE, INACTIVE';


--
-- Name: COLUMN sys_entity.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.description IS '描述';


--
-- Name: COLUMN sys_entity.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.created_by IS '创建人ID';


--
-- Name: COLUMN sys_entity.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.created_time IS '创建时间';


--
-- Name: COLUMN sys_entity.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.updated_time IS '更新时间';


--
-- Name: COLUMN sys_entity.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.deleted IS '逻辑删除: 0=未删除, 1=已删除';


--
-- Name: COLUMN sys_entity.parent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.parent_id IS '父法人ID（用于集团层级：母公司-子公司）';


--
-- Name: COLUMN sys_entity.order_num; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity.order_num IS '排序号';


--
-- Name: sys_entity_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_entity_config (
    id character varying(64) NOT NULL,
    entity_id character varying(64) NOT NULL,
    config_type character varying(50) NOT NULL,
    config_key character varying(100) NOT NULL,
    config_value text,
    description character varying(500),
    created_by character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted smallint DEFAULT 0
);


--
-- Name: TABLE sys_entity_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_entity_config IS '法人配置表（每个法人独立的配置）';


--
-- Name: COLUMN sys_entity_config.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.entity_id IS '法人ID';


--
-- Name: COLUMN sys_entity_config.config_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.config_type IS '配置类型: ERP_INTEGRATION(ERP集成), BUSINESS_RULE(业务规则), COMPLIANCE_POLICY(合规策略)';


--
-- Name: COLUMN sys_entity_config.config_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.config_key IS '配置键';


--
-- Name: COLUMN sys_entity_config.config_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.config_value IS '配置值（JSON格式）';


--
-- Name: COLUMN sys_entity_config.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.description IS '配置描述';


--
-- Name: COLUMN sys_entity_config.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.created_by IS '创建人ID';


--
-- Name: COLUMN sys_entity_config.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.created_time IS '创建时间';


--
-- Name: COLUMN sys_entity_config.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.updated_time IS '更新时间';


--
-- Name: COLUMN sys_entity_config.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_entity_config.deleted IS '逻辑删除: 0=未删除, 1=已删除';


--
-- Name: sys_env_marker; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_env_marker (
    marker_key character varying(100) NOT NULL,
    marker_value character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_env_marker; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_env_marker IS '系统环境标记表 (用于启动守卫核验数据库身份)';


--
-- Name: sys_erp_adapter; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_erp_adapter (
    adapter_id character varying(100) NOT NULL,
    adapter_name character varying(200) NOT NULL,
    erp_type character varying(50) NOT NULL,
    base_url character varying(500),
    enabled boolean DEFAULT true,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_erp_adapter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_erp_adapter IS 'ERP AI 适配器主表';


--
-- Name: COLUMN sys_erp_adapter.adapter_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_adapter.adapter_id IS '适配器唯一标识';


--
-- Name: COLUMN sys_erp_adapter.adapter_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_adapter.adapter_name IS '适配器名称';


--
-- Name: COLUMN sys_erp_adapter.erp_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_adapter.erp_type IS 'ERP 系统类型（yonsuite, kingdee 等）';


--
-- Name: COLUMN sys_erp_adapter.base_url; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_adapter.base_url IS 'API 基础URL';


--
-- Name: sys_erp_adapter_scenario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_erp_adapter_scenario (
    id integer NOT NULL,
    adapter_id character varying(100) NOT NULL,
    scenario_code character varying(50) NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_erp_adapter_scenario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_erp_adapter_scenario IS 'ERP 适配器业务场景映射表';


--
-- Name: COLUMN sys_erp_adapter_scenario.scenario_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_adapter_scenario.scenario_code IS '场景代码（SALES_OUT, RECEIPT 等）';


--
-- Name: sys_erp_adapter_scenario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_erp_adapter_scenario_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_erp_adapter_scenario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_erp_adapter_scenario_id_seq OWNED BY public.sys_erp_adapter_scenario.id;


--
-- Name: sys_erp_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_erp_config (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    erp_type character varying(50) NOT NULL,
    config_json text NOT NULL,
    is_active smallint DEFAULT 1,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    accbook_mapping text,
    sap_interface_type character varying(20)
);


--
-- Name: TABLE sys_erp_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_erp_config IS 'ERP对接配置表';


--
-- Name: COLUMN sys_erp_config.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.name IS '配置名称';


--
-- Name: COLUMN sys_erp_config.erp_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.erp_type IS 'ERP类型: YONSUITE/KINGDEE/GENERIC';


--
-- Name: COLUMN sys_erp_config.config_json; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.config_json IS '配置参数JSON';


--
-- Name: COLUMN sys_erp_config.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.is_active IS '是否启用';


--
-- Name: COLUMN sys_erp_config.accbook_mapping; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.accbook_mapping IS '账套-全宗映射JSON: {"BR01": "FONDS_A", "BR02": "FONDS_B"} - 合规性要求一个全宗只能映射一个账套';


--
-- Name: COLUMN sys_erp_config.sap_interface_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.sap_interface_type IS 'SAP接口类型: ODATA, RFC, IDOC, GATEWAY (仅当erp_type=SAP时有效)';


--
-- Name: sys_erp_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_erp_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_erp_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_erp_config_id_seq OWNED BY public.sys_erp_config.id;


--
-- Name: sys_erp_feedback_queue; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_erp_feedback_queue (
    id bigint NOT NULL,
    voucher_id character varying(64) NOT NULL,
    archival_code character varying(128) NOT NULL,
    erp_type character varying(32) NOT NULL,
    erp_config_id bigint,
    retry_count integer DEFAULT 0,
    max_retries integer DEFAULT 3,
    last_error text,
    status character varying(16) DEFAULT 'PENDING'::character varying,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    next_retry_time timestamp without time zone
);


--
-- Name: TABLE sys_erp_feedback_queue; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_erp_feedback_queue IS 'ERP 回写失败重试队列 - 存证溯源';


--
-- Name: COLUMN sys_erp_feedback_queue.voucher_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.voucher_id IS 'ERP 凭证/单据 ID';


--
-- Name: COLUMN sys_erp_feedback_queue.archival_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.archival_code IS '生成的档号';


--
-- Name: COLUMN sys_erp_feedback_queue.erp_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.erp_type IS 'ERP 类型 (YONSUITE, KINGDEE 等)';


--
-- Name: COLUMN sys_erp_feedback_queue.retry_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.retry_count IS '已重试次数';


--
-- Name: COLUMN sys_erp_feedback_queue.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.status IS '状态: PENDING-待重试, RETRYING-重试中, SUCCESS-成功, FAILED-放弃';


--
-- Name: COLUMN sys_erp_feedback_queue.next_retry_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.next_retry_time IS '下次重试时间 (指数退避算法)';


--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_erp_feedback_queue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_erp_feedback_queue_id_seq OWNED BY public.sys_erp_feedback_queue.id;


--
-- Name: sys_erp_scenario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_erp_scenario (
    id bigint NOT NULL,
    config_id bigint NOT NULL,
    scenario_key character varying(100) NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500),
    is_active boolean DEFAULT false,
    sync_strategy character varying(50) DEFAULT 'MANUAL'::character varying,
    cron_expression character varying(100),
    last_sync_time timestamp without time zone,
    last_sync_status character varying(50),
    last_sync_msg text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    params_json text
);


--
-- Name: TABLE sys_erp_scenario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_erp_scenario IS 'ERP业务场景配置表 (Layer 2)';


--
-- Name: COLUMN sys_erp_scenario.scenario_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_scenario.scenario_key IS '场景唯一标识 (如 VOUCHER_SYNC)';


--
-- Name: COLUMN sys_erp_scenario.sync_strategy; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_scenario.sync_strategy IS '同步策略: MANUAL=手动, CRON=定时, REALTIME=实时';


--
-- Name: COLUMN sys_erp_scenario.params_json; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_scenario.params_json IS 'JSON格式的场景参数配置';


--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_erp_scenario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_erp_scenario_id_seq OWNED BY public.sys_erp_scenario.id;


--
-- Name: sys_erp_sub_interface; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_erp_sub_interface (
    id bigint NOT NULL,
    scenario_id bigint NOT NULL,
    interface_key character varying(100) NOT NULL,
    interface_name character varying(200) NOT NULL,
    description character varying(500),
    is_active boolean DEFAULT true,
    sort_order integer DEFAULT 0,
    config_json text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE sys_erp_sub_interface; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_erp_sub_interface IS '场景子接口配置表';


--
-- Name: COLUMN sys_erp_sub_interface.interface_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_sub_interface.interface_key IS '接口标识如LIST_QUERY/DETAIL_QUERY';


--
-- Name: COLUMN sys_erp_sub_interface.config_json; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_sub_interface.config_json IS 'JSON格式的接口配置参数';


--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_erp_sub_interface_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_erp_sub_interface_id_seq OWNED BY public.sys_erp_sub_interface.id;


--
-- Name: sys_ingest_request_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_ingest_request_status (
    request_id character varying(64) NOT NULL,
    status character varying(32) NOT NULL,
    message text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    fonds_no character varying(50)
);


--
-- Name: TABLE sys_ingest_request_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_ingest_request_status IS 'SIP接收请求状态追踪表';


--
-- Name: COLUMN sys_ingest_request_status.request_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_ingest_request_status.request_id IS '请求ID';


--
-- Name: COLUMN sys_ingest_request_status.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_ingest_request_status.status IS '状态: RECEIVED, CHECKING, CHECK_PASSED, PROCESSING, COMPLETED, FAILED';


--
-- Name: COLUMN sys_ingest_request_status.message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_ingest_request_status.message IS '详细消息或错误信息';


--
-- Name: COLUMN sys_ingest_request_status.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_ingest_request_status.created_time IS '创建时间';


--
-- Name: COLUMN sys_ingest_request_status.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_ingest_request_status.updated_time IS '更新时间';


--
-- Name: COLUMN sys_ingest_request_status.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_ingest_request_status.fonds_no IS '全宗号，用于数据隔离';


--
-- Name: sys_original_voucher_type; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_original_voucher_type (
    id character varying(64) NOT NULL,
    category_code character varying(32) NOT NULL,
    category_name character varying(50) NOT NULL,
    type_code character varying(32) NOT NULL,
    type_name character varying(100) NOT NULL,
    default_retention character varying(20) DEFAULT '30Y'::character varying,
    sort_order integer DEFAULT 0,
    enabled boolean DEFAULT true,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone
);


--
-- Name: TABLE sys_original_voucher_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_original_voucher_type IS '原始凭证类型字典表 - 支持运维扩展';


--
-- Name: sys_permission; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_permission (
    id character varying(64) NOT NULL,
    perm_key character varying(100) NOT NULL,
    label character varying(100) NOT NULL,
    group_name character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: COLUMN sys_permission.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission.created_time IS '创建时间';


--
-- Name: COLUMN sys_permission.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_permission.updated_time IS '更新时间';


--
-- Name: sys_position; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_position (
    id character varying(32) NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50) NOT NULL,
    department_id character varying(32),
    description character varying(500),
    status character varying(20) DEFAULT 'active'::character varying,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE sys_position; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_position IS '系统职位表';


--
-- Name: COLUMN sys_position.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.name IS '职位名称';


--
-- Name: COLUMN sys_position.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.code IS '职位编码';


--
-- Name: COLUMN sys_position.department_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.department_id IS '所属部门ID';


--
-- Name: COLUMN sys_position.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.description IS '职位描述';


--
-- Name: COLUMN sys_position.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.status IS '状态: active/disabled';


--
-- Name: COLUMN sys_position.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.created_time IS '创建时间';


--
-- Name: COLUMN sys_position.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.updated_time IS '更新时间';


--
-- Name: COLUMN sys_position.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_position.deleted IS '逻辑删除标记';


--
-- Name: sys_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_role (
    id character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(128) NOT NULL,
    role_category character varying(64),
    is_exclusive boolean DEFAULT false,
    description text,
    permissions text,
    data_scope character varying(32) DEFAULT 'self'::character varying,
    type character varying(32) DEFAULT 'custom'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: COLUMN sys_role.role_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.role_category IS '角色类别：system_admin-系统管理员, security_admin-安全保密员, audit_admin-安全审计员, business_user-业务操作员';


--
-- Name: COLUMN sys_role.is_exclusive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.is_exclusive IS '是否为互斥角色（三员角色必须互斥，同一用户不能同时拥有多个互斥角色）';


--
-- Name: COLUMN sys_role.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_role.created_time IS '创建时间';


--
-- Name: sys_setting; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_setting (
    id character varying(64) NOT NULL,
    config_key character varying(128),
    config_value text,
    description character varying(512),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    category character varying(64),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: COLUMN sys_setting.category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.category IS '配置分组/类别';


--
-- Name: COLUMN sys_setting.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.created_at IS '创建时间';


--
-- Name: COLUMN sys_setting.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.deleted IS '逻辑删除标记: 0=正常, 1=已删除';


--
-- Name: COLUMN sys_setting.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.created_time IS '创建时间';


--
-- Name: COLUMN sys_setting.updated_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_setting.updated_time IS '更新时间';


--
-- Name: sys_sql_audit_rule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_sql_audit_rule (
    rule_key character varying(64) NOT NULL,
    rule_value character varying(1024) NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: COLUMN sys_sql_audit_rule.rule_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sql_audit_rule.rule_key IS '规则键';


--
-- Name: COLUMN sys_sql_audit_rule.rule_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sql_audit_rule.rule_value IS '规则值';


--
-- Name: COLUMN sys_sql_audit_rule.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sql_audit_rule.created_time IS '创建时间';


--
-- Name: COLUMN sys_sql_audit_rule.last_modified_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sql_audit_rule.last_modified_time IS '更新时间';


--
-- Name: sys_sync_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_sync_history (
    id bigint NOT NULL,
    scenario_id bigint NOT NULL,
    sync_start_time timestamp without time zone,
    sync_end_time timestamp without time zone,
    status character varying(20) DEFAULT 'RUNNING'::character varying NOT NULL,
    total_count integer DEFAULT 0,
    success_count integer DEFAULT 0,
    fail_count integer DEFAULT 0,
    error_message text,
    sync_params text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    operator_id character varying(64),
    client_ip character varying(50),
    four_nature_summary text
);


--
-- Name: TABLE sys_sync_history; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_sync_history IS '同步历史记录表';


--
-- Name: COLUMN sys_sync_history.scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_history.scenario_id IS '关联的场景ID';


--
-- Name: COLUMN sys_sync_history.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_history.status IS '同步状态: RUNNING/SUCCESS/FAIL';


--
-- Name: COLUMN sys_sync_history.sync_params; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_history.sync_params IS 'JSON格式的同步参数';


--
-- Name: COLUMN sys_sync_history.operator_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_history.operator_id IS '操作人ID';


--
-- Name: COLUMN sys_sync_history.client_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_history.client_ip IS '操作客户端IP';


--
-- Name: COLUMN sys_sync_history.four_nature_summary; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_history.four_nature_summary IS 'JSON格式的四性检测统计摘要(真实性、完整性、可用性、安全性通过率)';


--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_sync_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_sync_history_id_seq OWNED BY public.sys_sync_history.id;


--
-- Name: sys_sync_task; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_sync_task (
    id bigint NOT NULL,
    task_id character varying(100) NOT NULL,
    scenario_id bigint NOT NULL,
    status character varying(20) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    total_count integer DEFAULT 0,
    success_count integer DEFAULT 0,
    fail_count integer DEFAULT 0,
    error_message text,
    progress numeric(5,4) DEFAULT 0.0,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    operator_id character varying(50),
    client_ip character varying(50),
    sync_params text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE sys_sync_task; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_sync_task IS '异步同步任务状态表，用于持久化 ERP 同步任务的执行状态';


--
-- Name: COLUMN sys_sync_task.task_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.task_id IS '任务唯一标识，格式: sync-{scenarioId}-{timestamp}';


--
-- Name: COLUMN sys_sync_task.scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.scenario_id IS '关联 sys_erp_scenario.id';


--
-- Name: COLUMN sys_sync_task.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.status IS '任务状态: SUBMITTED=已提交, RUNNING=运行中, SUCCESS=成功, FAIL=失败';


--
-- Name: COLUMN sys_sync_task.total_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.total_count IS '总记录数';


--
-- Name: COLUMN sys_sync_task.success_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.success_count IS '成功数';


--
-- Name: COLUMN sys_sync_task.fail_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.fail_count IS '失败数';


--
-- Name: COLUMN sys_sync_task.error_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.error_message IS '错误信息';


--
-- Name: COLUMN sys_sync_task.progress; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.progress IS '进度 (0.0 - 1.0)';


--
-- Name: COLUMN sys_sync_task.start_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.start_time IS '开始时间';


--
-- Name: COLUMN sys_sync_task.end_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.end_time IS '结束时间';


--
-- Name: COLUMN sys_sync_task.operator_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.operator_id IS '操作人 ID';


--
-- Name: COLUMN sys_sync_task.client_ip; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.client_ip IS '操作客户端 IP';


--
-- Name: COLUMN sys_sync_task.sync_params; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_sync_task.sync_params IS '同步参数 (JSON)';


--
-- Name: sys_sync_task_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sys_sync_task_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sys_sync_task_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sys_sync_task_id_seq OWNED BY public.sys_sync_task.id;


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user (
    id character varying(64) NOT NULL,
    username character varying(128) NOT NULL,
    password_hash character varying(255) NOT NULL,
    full_name character varying(255),
    org_code character varying(128),
    email character varying(255),
    phone character varying(64),
    avatar character varying(512),
    department_id character varying(64),
    status character varying(32) DEFAULT 'active'::character varying,
    last_login_at timestamp without time zone,
    employee_id character varying(64),
    job_title character varying(128),
    join_date character varying(32),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: sys_user_fonds_scope; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_fonds_scope (
    id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    scope_type character varying(32) DEFAULT 'DIRECT'::character varying,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE sys_user_fonds_scope; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sys_user_fonds_scope IS '用户-全宗授权范围';


--
-- Name: COLUMN sys_user_fonds_scope.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_fonds_scope.user_id IS '用户ID';


--
-- Name: COLUMN sys_user_fonds_scope.fonds_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_fonds_scope.fonds_no IS '全宗号';


--
-- Name: COLUMN sys_user_fonds_scope.scope_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_fonds_scope.scope_type IS '授权来源类型';


--
-- Name: COLUMN sys_user_fonds_scope.created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_fonds_scope.created_time IS '创建时间';


--
-- Name: COLUMN sys_user_fonds_scope.last_modified_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_fonds_scope.last_modified_time IS '更新时间';


--
-- Name: COLUMN sys_user_fonds_scope.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_user_fonds_scope.deleted IS '逻辑删除标识';


--
-- Name: sys_user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_role (
    user_id character varying(64) NOT NULL,
    role_id character varying(64) NOT NULL
);


--
-- Name: system_performance_metrics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.system_performance_metrics (
    id character varying(32) NOT NULL,
    metric_type character varying(50) NOT NULL,
    metric_name character varying(100) NOT NULL,
    metric_value numeric(18,2),
    metric_unit character varying(20),
    fonds_no character varying(50),
    recorded_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: TABLE system_performance_metrics; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.system_performance_metrics IS '系统性能指标表';


--
-- Name: COLUMN system_performance_metrics.metric_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.system_performance_metrics.metric_type IS '指标类型: FONDS_CAPACITY(单全宗容量), CONCURRENT_SEARCH(并发检索), FILE_SIZE(最大文件大小), PREVIEW_TIME(预览首屏时间), LOG_RETENTION(日志留存周期)';


--
-- Name: user_mfa_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_mfa_config (
    id character varying(32) NOT NULL,
    user_id character varying(32) NOT NULL,
    mfa_enabled boolean DEFAULT false,
    mfa_type character varying(20),
    secret_key character varying(255),
    backup_codes text,
    last_used_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE user_mfa_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.user_mfa_config IS '用户MFA配置表';


--
-- Name: COLUMN user_mfa_config.mfa_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_mfa_config.mfa_type IS 'MFA类型: TOTP(时间同步令牌), SMS(短信), EMAIL(邮件)';


--
-- Name: COLUMN user_mfa_config.secret_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.user_mfa_config.secret_key IS 'TOTP密钥（加密存储）';


--
-- Name: voucher_match_result; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.voucher_match_result (
    id bigint NOT NULL,
    task_id character varying(50),
    batch_task_id character varying(50),
    match_batch_id character varying(50) NOT NULL,
    voucher_id character varying(50) NOT NULL,
    voucher_hash character varying(64),
    config_hash character varying(64),
    template_id character varying(50),
    template_version character varying(20),
    scene character varying(50),
    confidence numeric(5,4),
    status character varying(30) NOT NULL,
    match_details jsonb,
    missing_docs text[],
    is_latest boolean DEFAULT true,
    created_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE voucher_match_result; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.voucher_match_result IS '凭证匹配结果';


--
-- Name: voucher_match_result_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.voucher_match_result_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: voucher_match_result_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.voucher_match_result_id_seq OWNED BY public.voucher_match_result.id;


--
-- Name: voucher_source_link; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.voucher_source_link (
    id bigint NOT NULL,
    match_batch_id character varying(50),
    voucher_id character varying(50) NOT NULL,
    source_doc_id character varying(50) NOT NULL,
    evidence_role character varying(30),
    link_type character varying(20) NOT NULL,
    match_score integer,
    match_reasons text[],
    allocated_amount numeric(18,2),
    is_auto boolean DEFAULT true,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_time timestamp without time zone DEFAULT now(),
    created_by bigint
);


--
-- Name: TABLE voucher_source_link; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.voucher_source_link IS '凭证-源单关联关系';


--
-- Name: COLUMN voucher_source_link.link_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.voucher_source_link.link_type IS 'must_link/should_link/may_link';


--
-- Name: voucher_source_link_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.voucher_source_link_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: voucher_source_link_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.voucher_source_link_id_seq OWNED BY public.voucher_source_link.id;


--
-- Name: ys_sales_out; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ys_sales_out (
    id bigint NOT NULL,
    ys_id character varying(50) NOT NULL,
    code character varying(50),
    vouchdate date,
    status character varying(20),
    cust_name character varying(200),
    warehouse_name character varying(200),
    total_quantity numeric(18,4),
    memo text,
    raw_json text,
    sync_time timestamp without time zone DEFAULT now(),
    created_time timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE ys_sales_out; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ys_sales_out IS 'YonSuite销售出库单同步数据';


--
-- Name: ys_sales_out_detail; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ys_sales_out_detail (
    id bigint NOT NULL,
    sales_out_id bigint,
    ys_detail_id character varying(50),
    rowno integer,
    product_code character varying(50),
    product_name character varying(200),
    qty numeric(18,4),
    unit_name character varying(50),
    ori_money numeric(18,2),
    ori_tax numeric(18,2)
);


--
-- Name: TABLE ys_sales_out_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ys_sales_out_detail IS 'YonSuite销售出库单明细同步数据';


--
-- Name: ys_sales_out_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ys_sales_out_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ys_sales_out_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ys_sales_out_detail_id_seq OWNED BY public.ys_sales_out_detail.id;


--
-- Name: ys_sales_out_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ys_sales_out_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ys_sales_out_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ys_sales_out_id_seq OWNED BY public.ys_sales_out.id;


--
-- Name: arc_archive_batch id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_archive_batch ALTER COLUMN id SET DEFAULT nextval('public.arc_archive_batch_id_seq'::regclass);


--
-- Name: arc_original_voucher_event id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_event ALTER COLUMN id SET DEFAULT nextval('public.arc_original_voucher_event_id_seq'::regclass);


--
-- Name: archive_amendment id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_amendment ALTER COLUMN id SET DEFAULT nextval('public.archive_amendment_id_seq'::regclass);


--
-- Name: archive_batch id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch ALTER COLUMN id SET DEFAULT nextval('public.archive_batch_id_seq'::regclass);


--
-- Name: archive_batch_item id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch_item ALTER COLUMN id SET DEFAULT nextval('public.archive_batch_item_id_seq'::regclass);


--
-- Name: cfg_account_role_mapping id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_account_role_mapping ALTER COLUMN id SET DEFAULT nextval('public.cfg_account_role_mapping_id_seq'::regclass);


--
-- Name: cfg_account_role_preset id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_account_role_preset ALTER COLUMN id SET DEFAULT nextval('public.cfg_account_role_preset_id_seq'::regclass);


--
-- Name: cfg_doc_type_mapping id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_doc_type_mapping ALTER COLUMN id SET DEFAULT nextval('public.cfg_doc_type_mapping_id_seq'::regclass);


--
-- Name: cfg_doc_type_preset id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_doc_type_preset ALTER COLUMN id SET DEFAULT nextval('public.cfg_doc_type_preset_id_seq'::regclass);


--
-- Name: collection_batch id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch ALTER COLUMN id SET DEFAULT nextval('public.collection_batch_id_seq'::regclass);


--
-- Name: collection_batch_file id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch_file ALTER COLUMN id SET DEFAULT nextval('public.collection_batch_file_id_seq'::regclass);


--
-- Name: integrity_check id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.integrity_check ALTER COLUMN id SET DEFAULT nextval('public.integrity_check_id_seq'::regclass);


--
-- Name: match_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_log ALTER COLUMN id SET DEFAULT nextval('public.match_log_id_seq'::regclass);


--
-- Name: period_lock id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.period_lock ALTER COLUMN id SET DEFAULT nextval('public.period_lock_id_seq'::regclass);


--
-- Name: scan_folder_monitor id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_folder_monitor ALTER COLUMN id SET DEFAULT nextval('public.scan_folder_monitor_id_seq'::regclass);


--
-- Name: scan_workspace id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_workspace ALTER COLUMN id SET DEFAULT nextval('public.scan_workspace_id_seq'::regclass);


--
-- Name: sys_erp_adapter_scenario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_adapter_scenario ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_adapter_scenario_id_seq'::regclass);


--
-- Name: sys_erp_config id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_config ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_config_id_seq'::regclass);


--
-- Name: sys_erp_feedback_queue id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_feedback_queue ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_feedback_queue_id_seq'::regclass);


--
-- Name: sys_erp_scenario id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_scenario ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_scenario_id_seq'::regclass);


--
-- Name: sys_erp_sub_interface id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_sub_interface ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_sub_interface_id_seq'::regclass);


--
-- Name: sys_sync_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_history ALTER COLUMN id SET DEFAULT nextval('public.sys_sync_history_id_seq'::regclass);


--
-- Name: sys_sync_task id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_task ALTER COLUMN id SET DEFAULT nextval('public.sys_sync_task_id_seq'::regclass);


--
-- Name: voucher_match_result id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.voucher_match_result ALTER COLUMN id SET DEFAULT nextval('public.voucher_match_result_id_seq'::regclass);


--
-- Name: voucher_source_link id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.voucher_source_link ALTER COLUMN id SET DEFAULT nextval('public.voucher_source_link_id_seq'::regclass);


--
-- Name: ys_sales_out id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out ALTER COLUMN id SET DEFAULT nextval('public.ys_sales_out_id_seq'::regclass);


--
-- Name: ys_sales_out_detail id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out_detail ALTER COLUMN id SET DEFAULT nextval('public.ys_sales_out_detail_id_seq'::regclass);


--
-- Data for Name: acc_archive; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at, counterparty, voucher_no, invoice_no, retention_start_date, destruction_status) FROM stdin;
arc-2024-001	BRJT	BRJT-2024-30Y-FIN-AC01-0001	AC01	2024年1月会计凭证01	2024	01	30Y	Boran Joint Venture	\N	ARCHIVED	\N	\N	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	2026-01-14 10:59:54.139127	2026-01-14 10:59:54.139127	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
arc-2024-002	BR-GROUP	BR-GROUP-2025-30Y-FIN-AC01-0002	AC01	2025年1月会计凭证02	2025	01	30Y	Boran Group	\N	ARCHIVED	\N	\N	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	2026-01-14 10:59:54.145573	2026-01-14 10:59:54.145573	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
arc-2024-003	BRJT	BRJT-2024-30Y-FIN-AC01-0003	AC01	2024年3月会计凭证03	2024	03	30Y	Boran Joint Venture	\N	ARCHIVED	\N	\N	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	2026-01-14 10:59:54.146429	2026-01-14 10:59:54.146429	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
arc-2024-004	BRJT	BRJT-2024-30Y-FIN-AC01-0004	AC01	2024年4月会计凭证04	2024	04	30Y	Boran Joint Venture	\N	ARCHIVED	\N	\N	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	2026-01-14 10:59:54.147077	2026-01-14 10:59:54.147077	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
arc-2024-005	BRJT	BRJT-2024-30Y-FIN-AC01-0005	AC01	2024年5月会计凭证05	2024	05	30Y	Boran Joint Venture	\N	ARCHIVED	\N	\N	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	2026-01-14 10:59:54.147632	2026-01-14 10:59:54.147632	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
seed-contract-001	DEMO	CON-2023-098	AC04	年度技术服务协议	2023	01	30Y	演示公司	系统	archived	150000.00	2023-01-15	CON-2023-098	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-01-15	NORMAL
seed-contract-002	DEMO	C-202511-002	AC04	服务器采购合同	2025	11	30Y	演示公司	系统	archived	450000.00	2025-11-15	C-202511-002	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-11-15	NORMAL
seed-invoice-001	DEMO	INV-202311-089	AC01	阿里云计算服务费发票	2023	11	30Y	演示公司	系统	archived	12800.00	2023-11-02	INV-202311-089	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-11-02	NORMAL
seed-invoice-002	DEMO	INV-202311-092	AC01	服务器采购发票	2023	11	30Y	演示公司	系统	archived	45200.00	2023-11-03	INV-202311-092	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-11-03	NORMAL
seed-voucher-001	DEMO	JZ-202311-0052	AC01	11月技术部费用报销	2023	11	30Y	演示公司	系统	archived	58000.00	2023-11-05	JZ-202311-0052	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-11-05	NORMAL
seed-voucher-002	DEMO	V-202511-TEST	AC01	报销差旅费	2025	11	30Y	演示公司	张三	archived	5280.00	2025-11-07	V-202511-TEST	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-11-07	NORMAL
seed-receipt-001	DEMO	B-20231105-003	AC04	招商银行付款回单	2023	11	30Y	演示公司	系统	archived	58000.00	2023-11-05	B-20231105-003	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-11-05	NORMAL
seed-report-001	DEMO	REP-2023-11	AC03	11月科目余额表	2023	11	30Y	演示公司	系统	archived	\N	2023-11-30	REP-2023-11	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-11-30	NORMAL
seed-book-001	COMP001	ARC-BOOK-2024-GL	AC02	2024年总账	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "GENERAL_LEDGER", "pageCount": 100}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-book-002	COMP001	ARC-BOOK-2024-CASH	AC02	2024年现金日记账	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "CASH_JOURNAL", "pageCount": 50}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-book-003	COMP001	ARC-BOOK-2024-BANK	AC02	2024年银行存款日记账	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "BANK_JOURNAL", "pageCount": 50}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-book-004	COMP001	ARC-BOOK-2024-FIXED	AC02	2024年固定资产卡片	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "FIXED_ASSETS_CARD", "pageCount": 20}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-c03-001	COMP001	ARC-REP-2024-M01	AC03	2024年1月财务月报	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"period": "2024-01", "reportType": "MONTHLY"}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-c03-002	COMP001	ARC-REP-2024-Q1	AC03	2024年第一季度财务报表	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"period": "2024-Q1", "reportType": "QUARTERLY"}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-c03-003	COMP001	ARC-REP-2023-ANN	AC03	2023年度财务决算报告	2023	\N	PERMANENT	总公司	\N	archived	\N	\N	\N	\N	{"period": "2023", "reportType": "ANNUAL"}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-12-31	NORMAL
seed-c04-001	COMP001	ARC-OTH-2024-BK-01	AC04	2024年1月银行对账单	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"otherType": "BANK_STATEMENT"}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-c04-002	COMP001	ARC-OTH-2024-TAX-01	AC04	2024年1月增值税纳税申报表	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"otherType": "TAX_RETURN"}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
seed-c04-003	COMP001	ARC-OTH-2024-HO-01	AC04	2024年度会计档案移交清册	2024	\N	30Y	档案室	\N	archived	\N	\N	\N	\N	{"otherType": "HANDOVER_REGISTER"}	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:34.909662	2025-12-28 09:03:34.909662	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
voucher-2024-12-001	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-3001	AC01	支付年终审计费	2024	12	30Y	泊冉集团有限公司	张三	archived	88000.00	2024-12-10	JZ-202412-0001	\N	[{"id": "1", "debit_org": 77876.11, "accsubject": {"code": "6602", "name": "管理费用-审计费"}, "credit_org": 0, "description": "年度审计服务费"}, {"id": "2", "debit_org": 10123.89, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 88000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-10	NORMAL
voucher-2024-12-002	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-3002	AC01	年终奖金计提	2024	12	30Y	泊冉集团有限公司	李四	archived	2580000.00	2024-12-20	JZ-202412-0002	\N	[{"id": "1", "debit_org": 1200000.00, "accsubject": {"code": "6602", "name": "管理费用-工资"}, "credit_org": 0, "description": "管理层年终奖"}, {"id": "2", "debit_org": 880000.00, "accsubject": {"code": "6601", "name": "销售费用-工资"}, "credit_org": 0, "description": "销售奖金"}, {"id": "3", "debit_org": 500000.00, "accsubject": {"code": "5001", "name": "生产成本-直接人工"}, "credit_org": 0, "description": "生产绩效奖"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 2580000.00, "description": "应付年终奖"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-20	NORMAL
voucher-2023-11-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0011	AC01	支付技术服务费	2023	11	30Y	泊冉集团有限公司	张三	archived	45800.00	2023-11-15	JZ-202311-0001	\N	[{"id": "1", "debit_org": 40530.97, "accsubject": {"code": "6602", "name": "管理费用-技术服务费"}, "credit_org": 0, "description": "华为云年度服务费"}, {"id": "2", "debit_org": 5269.03, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 45800.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-11-15	NORMAL
voucher-2023-10-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0021	AC01	采购生产设备	2023	10	30Y	泊冉集团有限公司	李四	archived	580000.00	2023-10-20	JZ-202310-0001	\N	[{"id": "1", "debit_org": 513274.34, "accsubject": {"code": "1601", "name": "固定资产"}, "credit_org": 0, "description": "数控机床采购"}, {"id": "2", "debit_org": 66725.66, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 580000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-10-20	NORMAL
voucher-2022-12-001	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC01-0001	AC01	年度损益结转	2022	12	30Y	泊冉集团有限公司	李四	archived	2680000.00	2022-12-31	JZ-202212-0001	\N	[{"id": "1", "debit_org": 2680000.00, "accsubject": {"code": "3131", "name": "本年利润"}, "credit_org": 0, "description": "结转本年利润"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "3141", "name": "利润分配"}, "credit_org": 2680000.00, "description": "利润分配"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-12-31	NORMAL
other-bank-2024-10	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-BANK10	AC04	2024年10月招商银行对账单	2024	10	30Y	泊冉集团有限公司	张三	archived	\N	2024-10-31	BANK-202410	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-31	NORMAL
voucher-2022-06-001	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC01-0011	AC01	半年度奖金发放	2022	06	30Y	泊冉集团有限公司	李四	archived	1250000.00	2022-06-30	JZ-202206-0001	\N	[{"id": "1", "debit_org": 1250000.00, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 0, "description": "应付工资结转"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 1250000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-06-30	NORMAL
voucher-2025-01-001	BR-GROUP	BR-GROUP-2025-30Y-FIN-AC01-0001	AC01	支付员工年终奖	2025	01	30Y	泊冉集团有限公司	李四	archived	2580000.00	2025-01-15	JZ-202501-0001	\N	[{"id": "1", "debit_org": 2580000.00, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 0, "description": "发放年终奖金"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 2580000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-01-15	NORMAL
voucher-sales-2024-11-001	BR-SALES	BR-SALES-2024-30Y-FIN-AC01-0001	AC01	销售产品收入	2024	11	30Y	泊冉销售有限公司	李四	archived	1280000.00	2024-11-18	JZ-SALES-202411-0001	\N	[{"id": "1", "debit_org": 1446400.00, "accsubject": {"code": "1122", "name": "应收账款"}, "credit_org": 0, "description": "销售智能设备A型"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 1280000.00, "description": "确认收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 166400.00, "description": "销项税额"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-18	NORMAL
voucher-trade-2024-11-001	BR-TRADE	BR-TRADE-2024-30Y-FIN-AC01-0001	AC01	进口设备采购	2024	11	30Y	泊冉国际贸易有限公司	张三	archived	860000.00	2024-11-22	JZ-TRADE-202411-0001	\N	[{"id": "1", "debit_org": 761061.95, "accsubject": {"code": "1403", "name": "原材料"}, "credit_org": 0, "description": "进口精密仪器"}, {"id": "2", "debit_org": 98938.05, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 860000.00, "description": "应付账款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-22	NORMAL
voucher-mfg-2024-11-001	BR-MFG	BR-MFG-2024-30Y-FIN-AC01-0001	AC01	生产材料领用	2024	11	30Y	泊冉制造有限公司	赵六	archived	156000.00	2024-11-08	JZ-MFG-202411-0001	\N	[{"id": "1", "debit_org": 156000.00, "accsubject": {"code": "5001", "name": "生产成本-直接材料"}, "credit_org": 0, "description": "领用钢材"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1403", "name": "原材料"}, "credit_org": 156000.00, "description": "原材料减少"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-08	NORMAL
ledger-2024-001	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC02-ZZ001	AC02	2024年度总账	2024	00	30Y	泊冉集团有限公司	张三	archived	\N	2024-12-31	ZZ-2024-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
ledger-2024-002	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC02-MX001	AC02	2024年度银行存款明细账	2024	00	30Y	泊冉集团有限公司	张三	archived	\N	2024-12-31	MX-2024-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
ledger-2024-003	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC02-RJ001	AC02	2024年度现金日记账	2024	00	30Y	泊冉集团有限公司	张三	archived	\N	2024-12-31	RJ-2024-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
ledger-2024-004	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC02-YS001	AC02	2024年度应收账款明细账	2024	00	30Y	泊冉集团有限公司	李四	archived	\N	2024-12-31	YS-2024-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
ledger-2024-005	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC02-GD001	AC02	2024年度固定资产卡片	2024	00	30Y	泊冉集团有限公司	张三	archived	\N	2024-12-31	GD-2024-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-31	NORMAL
ledger-2023-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC02-ZZ001	AC02	2023年度总账	2023	00	30Y	泊冉集团有限公司	张三	archived	\N	2023-12-31	ZZ-2023-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-12-31	NORMAL
ledger-2023-002	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC02-MX001	AC02	2023年度银行存款明细账	2023	00	30Y	泊冉集团有限公司	张三	archived	\N	2023-12-31	MX-2023-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-12-31	NORMAL
ledger-2022-001	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC02-ZZ001	AC02	2022年度总账	2022	00	30Y	泊冉集团有限公司	张三	archived	\N	2022-12-31	ZZ-2022-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-12-31	NORMAL
report-2024-zcfz-11	BR-GROUP	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11	AC03	2024年11月资产负债表	2024	11	PERMANENT	泊冉集团有限公司	李四	archived	\N	2024-11-30	ZCFZ-202411	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-30	NORMAL
report-2024-zcfz-10	BR-GROUP	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10	AC03	2024年10月资产负债表	2024	10	PERMANENT	泊冉集团有限公司	李四	archived	\N	2024-10-31	ZCFZ-202410	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-31	NORMAL
report-2024-zcfz-09	BR-GROUP	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09	AC03	2024年9月资产负债表	2024	09	PERMANENT	泊冉集团有限公司	李四	archived	\N	2024-09-30	ZCFZ-202409	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-09-30	NORMAL
report-2024-lr-11	BR-GROUP	BR-GROUP-2024-PERM-FIN-AC03-LR11	AC03	2024年11月利润表	2024	11	PERMANENT	泊冉集团有限公司	李四	archived	\N	2024-11-30	LR-202411	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-30	NORMAL
report-2024-lr-10	BR-GROUP	BR-GROUP-2024-PERM-FIN-AC03-LR10	AC03	2024年10月利润表	2024	10	PERMANENT	泊冉集团有限公司	李四	archived	\N	2024-10-31	LR-202410	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-31	NORMAL
report-2024-xjll-q3	BR-GROUP	BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3	AC03	2024年第三季度现金流量表	2024	Q3	PERMANENT	泊冉集团有限公司	李四	archived	\N	2024-09-30	XJLL-2024Q3	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-09-30	NORMAL
report-2023-annual	BR-GROUP	BR-GROUP-2023-PERM-FIN-AC03-ANNUAL	AC03	2023年度财务决算报告	2023	00	PERMANENT	泊冉集团有限公司	李四	archived	\N	2023-12-31	ANNUAL-2023	\N	\N	confidential	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-12-31	NORMAL
report-2022-annual	BR-GROUP	BR-GROUP-2022-PERM-FIN-AC03-ANNUAL	AC03	2022年度财务决算报告	2022	00	PERMANENT	泊冉集团有限公司	李四	archived	\N	2022-12-31	ANNUAL-2022	\N	\N	confidential	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-12-31	NORMAL
other-bank-2024-11	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-BANK11	AC04	2024年11月招商银行对账单	2024	11	30Y	泊冉集团有限公司	张三	archived	\N	2024-11-30	BANK-202411	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-30	NORMAL
other-tax-2024-11	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-TAX11	AC04	2024年11月增值税纳税申报表	2024	11	30Y	泊冉集团有限公司	李四	archived	168500.00	2024-12-15	TAX-202411	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-15	NORMAL
other-tax-2024-q3	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3	AC04	2024年第三季度企业所得税预缴申报表	2024	Q3	30Y	泊冉集团有限公司	李四	archived	286000.00	2024-10-20	TAX-2024Q3	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-20	NORMAL
other-contract-2024-001	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-CON001	AC04	华为云年度服务合同	2024	00	30Y	泊冉集团有限公司	张三	archived	158000.00	2024-01-15	CON-2024-001	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-01-15	NORMAL
other-contract-2024-002	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-CON002	AC04	办公楼租赁合同	2024	00	30Y	泊冉集团有限公司	张三	archived	816000.00	2024-01-01	CON-2024-002	\N	\N	confidential	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-01-01	NORMAL
other-contract-2024-003	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC04-CON003	AC04	年度审计服务协议	2024	00	30Y	泊冉集团有限公司	钱七	archived	88000.00	2024-03-01	CON-2024-003	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-03-01	NORMAL
other-audit-2023	BR-GROUP	BR-GROUP-2023-PERM-FIN-AC04-AUDIT	AC04	2023年度审计报告	2023	00	PERMANENT	泊冉集团有限公司	王五	archived	\N	2024-03-15	AUDIT-2023	\N	\N	confidential	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-03-15	NORMAL
voucher-2025-02-002	BR-GROUP	BR-GROUP-2025-30Y-FIN-AC01-0202	AC01	销售收入确认	2025	02	30Y	泊冉集团有限公司	李四	archived	358000.00	2025-02-20	JZ-202502-0002	\N	[{"id": "1", "debit_org": 404540.00, "accsubject": {"code": "1122", "name": "应收账款"}, "credit_org": 0, "description": "销售智能检测设备"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 358000.00, "description": "确认收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 46540.00, "description": "销项税额"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-02-20	NORMAL
voucher-2024-11-004	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-0004	AC01	支付阿里云服务器费用	2024	11	30Y	泊冉集团有限公司	张三	archived	12800.00	2024-11-10	JZ-202411-0004	\N	[{"id": "1", "debit_org": 12800.00, "accsubject": {"code": "6602", "name": "管理费用-技术服务费"}, "credit_org": 0, "description": "阿里云ECS服务器年费"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 12800.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-10	NORMAL
voucher-2024-11-005	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-0005	AC01	计提本月工资	2024	11	30Y	泊冉集团有限公司	李四	archived	856000.00	2024-11-25	JZ-202411-0005	\N	[{"id": "1", "debit_org": 356000.00, "accsubject": {"code": "6602", "name": "管理费用-工资"}, "credit_org": 0, "description": "管理人员工资"}, {"id": "2", "debit_org": 280000.00, "accsubject": {"code": "6601", "name": "销售费用-工资"}, "credit_org": 0, "description": "销售人员工资"}, {"id": "3", "debit_org": 220000.00, "accsubject": {"code": "5001", "name": "生产成本-直接人工"}, "credit_org": 0, "description": "生产人员工资"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 856000.00, "description": "应付工资"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-25	NORMAL
voucher-2024-11-006	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-0006	AC01	固定资产折旧计提	2024	11	30Y	泊冉集团有限公司	张三	archived	45600.00	2024-11-28	JZ-202411-0006	\N	[{"id": "1", "debit_org": 18500.00, "accsubject": {"code": "6602", "name": "管理费用-折旧费"}, "credit_org": 0, "description": "管理部门折旧"}, {"id": "2", "debit_org": 27100.00, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "生产部门折旧"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1602", "name": "累计折旧"}, "credit_org": 45600.00, "description": "累计折旧"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-28	NORMAL
voucher-2024-11-007	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-0007	AC01	支付供应商货款	2024	11	30Y	泊冉集团有限公司	李四	archived	286500.00	2024-11-12	JZ-202411-0007	\N	[{"id": "1", "debit_org": 286500.00, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 0, "description": "支付苏州精密机械有限公司货款"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 286500.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-12	NORMAL
voucher-2024-11-008	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-0008	AC01	销售商品确认收入	2024	11	30Y	泊冉集团有限公司	李四	archived	520000.00	2024-11-15	JZ-202411-0008	\N	[{"id": "1", "debit_org": 587600.00, "accsubject": {"code": "1122", "name": "应收账款"}, "credit_org": 0, "description": "销售智能设备"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 520000.00, "description": "确认收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 67600.00, "description": "销项税额"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-11-15	NORMAL
voucher-2024-10-001	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-1001	AC01	支付房租及物业费	2024	10	30Y	泊冉集团有限公司	张三	archived	85000.00	2024-10-05	JZ-202410-0001	\N	[{"id": "1", "debit_org": 68000.00, "accsubject": {"code": "6602", "name": "管理费用-租赁费"}, "credit_org": 0, "description": "办公楼租金"}, {"id": "2", "debit_org": 17000.00, "accsubject": {"code": "6602", "name": "管理费用-物业费"}, "credit_org": 0, "description": "物业管理费"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 85000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-10	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-05	NORMAL
voucher-2024-10-002	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-1002	AC01	采购原材料入库	2024	10	30Y	泊冉集团有限公司	李四	archived	468000.00	2024-10-12	JZ-202410-0002	\N	[{"id": "1", "debit_org": 410619.47, "accsubject": {"code": "1403", "name": "原材料"}, "credit_org": 0, "description": "原材料入库-钢材"}, {"id": "2", "debit_org": 57380.53, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 468000.00, "description": "暂估应付"}]	internal	\N	\N	system	\N	\N	volume-2024-10	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-12	NORMAL
voucher-2023-09-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0091	AC01	季度社保缴纳	2023	09	30Y	泊冉集团有限公司	李四	archived	186500.00	2023-09-25	JZ-202309-0001	\N	[{"id": "1", "debit_org": 186500.00, "accsubject": {"code": "6602", "name": "管理费用-社保费"}, "credit_org": 0, "description": "单位社保"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 186500.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-09-25	NORMAL
voucher-2024-10-003	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-1003	AC01	计提本月工资	2024	10	30Y	泊冉集团有限公司	李四	archived	842000.00	2024-10-25	JZ-202410-0003	\N	[{"id": "1", "debit_org": 348000.00, "accsubject": {"code": "6602", "name": "管理费用-工资"}, "credit_org": 0, "description": "管理人员工资"}, {"id": "2", "debit_org": 276000.00, "accsubject": {"code": "6601", "name": "销售费用-工资"}, "credit_org": 0, "description": "销售人员工资"}, {"id": "3", "debit_org": 218000.00, "accsubject": {"code": "5001", "name": "生产成本-直接人工"}, "credit_org": 0, "description": "生产人员工资"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 842000.00, "description": "应付工资"}]	internal	\N	\N	system	\N	\N	volume-2024-10	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-25	NORMAL
voucher-2024-10-004	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-1004	AC01	支付水电费	2024	10	30Y	泊冉集团有限公司	张三	archived	28600.00	2024-10-18	JZ-202410-0004	\N	[{"id": "1", "debit_org": 8600.00, "accsubject": {"code": "6602", "name": "管理费用-水电费"}, "credit_org": 0, "description": "办公楼水电费"}, {"id": "2", "debit_org": 20000.00, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "生产车间水电费"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 28600.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-10	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-18	NORMAL
voucher-2024-10-005	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-1005	AC01	结转本月成本	2024	10	30Y	泊冉集团有限公司	李四	archived	380000.00	2024-10-30	JZ-202410-0005	\N	[{"id": "1", "debit_org": 380000.00, "accsubject": {"code": "6401", "name": "主营业务成本"}, "credit_org": 0, "description": "结转销售成本"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1405", "name": "库存商品"}, "credit_org": 380000.00, "description": "库存商品减少"}]	internal	\N	\N	system	\N	\N	volume-2024-10	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-10-30	NORMAL
voucher-2024-09-001	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-2001	AC01	购买固定资产-服务器	2024	09	30Y	泊冉集团有限公司	张三	archived	185000.00	2024-09-08	JZ-202409-0001	\N	[{"id": "1", "debit_org": 163716.81, "accsubject": {"code": "1601", "name": "固定资产"}, "credit_org": 0, "description": "华为服务器采购"}, {"id": "2", "debit_org": 21283.19, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 185000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-09	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-09-08	NORMAL
voucher-2024-09-002	BR-GROUP	BR-GROUP-2024-30Y-FIN-AC01-2002	AC01	收到销售佣金收入	2024	09	30Y	泊冉集团有限公司	李四	archived	68000.00	2024-09-15	JZ-202409-0002	\N	[{"id": "1", "debit_org": 68000.00, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 0, "description": "收到代理佣金"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6051", "name": "其他业务收入"}, "credit_org": 60176.99, "description": "确认其他收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 7823.01, "description": "销项税额"}]	internal	\N	\N	system	\N	\N	volume-2024-09	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-09-15	NORMAL
voucher-2023-12-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0001	AC01	结转年度利润	2023	12	30Y	泊冉集团有限公司	李四	archived	3860000.00	2023-12-31	JZ-202312-0001	\N	[{"id": "1", "debit_org": 12580000.00, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 0, "description": "结转收入"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6401", "name": "主营业务成本"}, "credit_org": 7200000.00, "description": "结转成本"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "6602", "name": "管理费用"}, "credit_org": 1520000.00, "description": "结转费用"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "3131", "name": "本年利润"}, "credit_org": 3860000.00, "description": "本年利润"}]	internal	\N	\N	system	\N	\N	volume-2023-12	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-12-31	NORMAL
voucher-2014-01-001	BR-GROUP	BR-GROUP-2014-10Y-FIN-AC01-0001	AC01	2014年1月记账凭证汇总	2014	01	10Y	泊冉集团有限公司	系统	archived	580000.00	2014-01-31	JZ-201401-0001	\N	\N	internal	\N	\N	system	\N	\N	\N	2014-02-01 00:00:00	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2014-01-31	NORMAL
voucher-2014-02-001	BR-GROUP	BR-GROUP-2014-10Y-FIN-AC01-0002	AC01	2014年2月记账凭证汇总	2014	02	10Y	泊冉集团有限公司	系统	archived	620000.00	2014-02-28	JZ-201402-0001	\N	\N	internal	\N	\N	system	\N	\N	\N	2014-03-01 00:00:00	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2014-02-28	NORMAL
bank-2014-q1	BR-GROUP	BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1	AC04	2014年第一季度银行对账单	2014	Q1	10Y	泊冉集团有限公司	系统	archived	\N	2014-03-31	BANK-2014Q1	\N	\N	internal	\N	\N	system	\N	\N	\N	2014-04-01 00:00:00	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2014-03-31	NORMAL
pre-2024-12-001	BR-GROUP	PENDING-2024-12-001	AC01	支付快递费	2024	12	30Y	泊冉集团有限公司	张三	pending	680.00	2024-12-22	JZ-PRE-202412-0001	\N	[{"id": "1", "debit_org": 680.00, "accsubject": {"code": "6602", "name": "管理费用-快递费"}, "credit_org": 0, "description": "顺丰快递费"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 680.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-22	NORMAL
pre-2024-12-002	BR-GROUP	PENDING-2024-12-002	AC01	会议室租赁费	2024	12	30Y	泊冉集团有限公司	张三	pending	3500.00	2024-12-23	JZ-PRE-202412-0002	\N	[{"id": "1", "debit_org": 3500.00, "accsubject": {"code": "6602", "name": "管理费用-会议费"}, "credit_org": 0, "description": "酒店会议室租赁"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1001", "name": "库存现金"}, "credit_org": 3500.00, "description": "现金支付"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-23	NORMAL
pre-2024-12-003	BR-GROUP	PENDING-2024-12-003	AC01	员工团建活动费	2024	12	30Y	泊冉集团有限公司	李四	pending	28000.00	2024-12-24	JZ-PRE-202412-0003	\N	[{"id": "1", "debit_org": 28000.00, "accsubject": {"code": "6602", "name": "管理费用-福利费"}, "credit_org": 0, "description": "年会团建"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 28000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2024-12-24	NORMAL
voucher-2025-02-001	BR-GROUP	BR-GROUP-2025-30Y-FIN-AC01-0201	AC01	支付供应商货款	2025	02	30Y	泊冉集团有限公司	张三	archived	125600.00	2025-02-18	JZ-202502-0001	\N	[{"id": "1", "debit_org": 125600.00, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 0, "description": "支付宁波精密零部件货款"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 125600.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-02-18	NORMAL
voucher-2023-08-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0081	AC01	广告宣传费	2023	08	30Y	泊冉集团有限公司	张三	archived	75000.00	2023-08-15	JZ-202308-0001	\N	[{"id": "1", "debit_org": 66371.68, "accsubject": {"code": "6601", "name": "销售费用-广告费"}, "credit_org": 0, "description": "微信朋友圈广告投放"}, {"id": "2", "debit_org": 8628.32, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 75000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-08-15	NORMAL
voucher-2023-07-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0071	AC01	固定资产折旧	2023	07	30Y	泊冉集团有限公司	张三	archived	42800.00	2023-07-31	JZ-202307-0001	\N	[{"id": "1", "debit_org": 18200.00, "accsubject": {"code": "6602", "name": "管理费用-折旧费"}, "credit_org": 0, "description": "管理部门折旧"}, {"id": "2", "debit_org": 24600.00, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "生产部门折旧"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1602", "name": "累计折旧"}, "credit_org": 42800.00, "description": "累计折旧"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-07-31	NORMAL
voucher-2023-06-001	BR-GROUP	BR-GROUP-2023-30Y-FIN-AC01-0061	AC01	半年度奖金发放	2023	06	30Y	泊冉集团有限公司	李四	archived	980000.00	2023-06-28	JZ-202306-0001	\N	[{"id": "1", "debit_org": 980000.00, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 0, "description": "发放半年度奖金"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 980000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2023-06-28	NORMAL
voucher-2022-11-001	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC01-0111	AC01	设备维修费	2022	11	30Y	泊冉集团有限公司	张三	archived	35600.00	2022-11-18	JZ-202211-0001	\N	[{"id": "1", "debit_org": 31504.42, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "数控机床年度维保"}, {"id": "2", "debit_org": 4095.58, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 35600.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-11-18	NORMAL
voucher-2022-10-001	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC01-0101	AC01	研发材料采购	2022	10	30Y	泊冉集团有限公司	李四	archived	168000.00	2022-10-22	JZ-202210-0001	\N	[{"id": "1", "debit_org": 148672.57, "accsubject": {"code": "5201", "name": "研发支出-费用化支出"}, "credit_org": 0, "description": "研发用电子元器件"}, {"id": "2", "debit_org": 19327.43, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 168000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-10-22	NORMAL
voucher-2022-09-001	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC01-0091	AC01	展会参展费用	2022	09	30Y	泊冉集团有限公司	张三	archived	128500.00	2022-09-15	JZ-202209-0001	\N	[{"id": "1", "debit_org": 113716.81, "accsubject": {"code": "6601", "name": "销售费用-展览费"}, "credit_org": 0, "description": "上海工博会展位费"}, {"id": "2", "debit_org": 14783.19, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 128500.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-09-15	NORMAL
voucher-2024-11-003	BR-GROUP	BR-GROUP-2025-30Y-FIN-AC01-1003	AC01	支付业务招待费-米山神鸡	2025	10	30Y	泊冉集团有限公司	李四	archived	201.00	2025-10-28	JZ-202411-0003	\N	[{"id": "1", "debit_org": 201.00, "accsubject": {"code": "6602", "name": "管理费用-业务招待费"}, "credit_org": 0, "description": "支付业务招待费-员工工作餐"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 201.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-10-28	NORMAL
demo-reimb-fp-002	BR-GROUP	FP-2025-01-002	AC01	酒店住宿费发票-北京希尔顿酒店	2025	01	30Y	泊冉集团有限公司	张三	ARCHIVED	1200.00	2025-01-07	FP-2025-01-002	\N	{"nights": 3, "vendor": "北京希尔顿酒店", "invoiceType": "住宿费"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
voucher-2024-11-001	BR-GROUP	BR-GROUP-2025-30Y-FIN-AC01-1001	AC01	支付业务招待费-吴奕聪餐饮店	2025	10	30Y	泊冉集团有限公司	张三	archived	657.00	2025-10-25	JZ-202411-0001	\N	[{"id": "1", "debit_org": 650.50, "accsubject": {"code": "6602", "name": "管理费用-业务招待费"}, "credit_org": 0, "description": "支付业务招待费-客户接待餐费"}, {"id": "2", "debit_org": 6.50, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项税额)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 657.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2025-10-25	NORMAL
voucher-2024-11-002	BR-GROUP	BR-GROUP-2022-30Y-FIN-AC01-1002	AC01	支付强生交通费用	2022	09	30Y	泊冉集团有限公司	张三	archived	10000.00	2022-09-20	JZ-202411-0002	\N	[{"id": "1", "debit_org": 10000.00, "accsubject": {"code": "6602", "name": "管理费用-交通费"}, "credit_org": 0, "description": "支付交通费"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 10000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	volume-2024-11	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	2022-09-20	NORMAL
demo-reimb-sq-001	BR-GROUP	SQ-2025-01-001	AC04	出差申请单-张三-北京出差	2025	01	30Y	泊冉集团有限公司	张三	archived	\N	2025-01-05	SQ-2025-01-001	\N	{"endDate": "2025-01-09", "purpose": "参加技术交流会", "applicant": "张三", "startDate": "2025-01-06", "destination": "北京"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-bx-001	BR-GROUP	BX-2025-01-001	AC01	差旅费报销单-张三	2025	01	30Y	泊冉集团有限公司	张三	archived	3280.00	2025-01-10	BX-2025-01-001	\N	{"items": [{"type": "交通费", "amount": 553.00}, {"type": "住宿费", "amount": 1200.00}, {"type": "餐饮费", "amount": 450.00}, {"type": "出租车费", "amount": 87.00}], "applicant": "张三", "totalAmount": 3280.00}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-fp-001	BR-GROUP	FP-2025-01-001	AC01	高铁票发票-北京南至上海虹桥	2025	01	30Y	泊冉集团有限公司	张三	archived	553.00	2025-01-06	FP-2025-01-001	\N	{"route": "北京南-上海虹桥", "vendor": "中国铁路", "invoiceType": "交通费"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-fp-003	BR-GROUP	FP-2025-01-003	AC01	餐饮费发票-北京全聚德烤鸭店	2025	01	30Y	泊冉集团有限公司	张三	archived	450.00	2025-01-08	FP-2025-01-003	\N	{"vendor": "北京全聚德烤鸭店", "invoiceType": "餐饮费"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-fp-004	BR-GROUP	FP-2025-01-004	AC01	出租车发票-上海强生出租汽车	2025	01	30Y	泊冉集团有限公司	张三	archived	87.00	2025-01-09	FP-2025-01-004	\N	{"vendor": "上海强生出租汽车有限公司", "invoiceType": "出租车费"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-fk-001	BR-GROUP	FK-2025-01-001	AC01	付款单-差旅费报销	2025	01	30Y	泊冉集团有限公司	财务部	archived	3280.00	2025-01-12	FK-2025-01-001	\N	{"bank": "招商银行", "payee": "张三", "paymentMethod": "银行转账"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-hd-001	BR-GROUP	HD-2025-01-001	AC04	银行回单-招商银行转账	2025	01	30Y	泊冉集团有限公司	系统	archived	3280.00	2025-01-12	HD-2025-01-001	\N	{"bank": "招商银行", "accountTo": "6225889876543210", "accountFrom": "6225881234567890", "transactionType": "转账"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-jz-001	BR-GROUP	JZ-2025-01-001	AC01	记账凭证-差旅费报销	2025	01	30Y	泊冉集团有限公司	会计	archived	3280.00	2025-01-12	JZ-2025-01-001	\N	[{"id": "1", "debit_org": 3280.00, "accsubject": {"code": "6602", "name": "管理费用-差旅费"}, "credit_org": 0, "description": "差旅费报销"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 3280.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-reimb-bb-001	BR-GROUP	BB-2025-01	AC03	2025年1月科目余额表	2025	01	30Y	泊冉集团有限公司	财务部	archived	\N	2025-01-31	BB-2025-01	\N	{"period": "2025-01", "reportType": "科目余额表"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-purchase-ht-001	BR-GROUP	HT-2025-02-001	AC04	服务器采购合同-阿里云	2025	02	30Y	泊冉集团有限公司	采购部	archived	450000.00	2025-02-15	HT-2025-02-001	\N	{"vendor": "阿里云", "product": "云服务器ECS", "quantity": 10, "contractType": "采购合同"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-purchase-fp-001	BR-GROUP	FP-2025-02-001	AC01	服务器采购发票-阿里云	2025	02	30Y	泊冉集团有限公司	系统	archived	450000.00	2025-02-20	FP-2025-02-001	\N	{"vendor": "阿里云", "taxRate": 0.13, "invoiceType": "增值税专用发票"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-purchase-jz-001	BR-GROUP	JZ-2025-02-001	AC01	记账凭证-设备采购	2025	02	30Y	泊冉集团有限公司	会计	archived	450000.00	2025-02-20	JZ-2025-02-001	\N	[{"id": "1", "debit_org": 398230.09, "accsubject": {"code": "1601", "name": "固定资产"}, "credit_org": 0, "description": "服务器采购"}, {"id": "2", "debit_org": 51769.91, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 450000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-purchase-fk-001	BR-GROUP	FK-2025-02-001	AC01	付款单-设备采购款	2025	02	30Y	泊冉集团有限公司	财务部	archived	450000.00	2025-02-22	FK-2025-02-001	\N	{"bank": "招商银行", "payee": "阿里云", "paymentMethod": "银行转账"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-purchase-hd-001	BR-GROUP	HD-2025-02-001	AC04	银行回单-招商银行转账	2025	02	30Y	泊冉集团有限公司	系统	archived	450000.00	2025-02-22	HD-2025-02-001	\N	{"bank": "招商银行", "accountTo": "6225881111111111", "accountFrom": "6225881234567890", "transactionType": "转账"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-office-fp-001	BR-GROUP	FP-2025-03-001	AC01	办公用品采购发票-京东	2025	03	30Y	泊冉集团有限公司	系统	archived	2580.00	2025-03-10	FP-2025-03-001	\N	{"items": ["打印纸", "文件夹", "笔"], "vendor": "京东", "invoiceType": "电子发票"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-office-jz-001	BR-GROUP	JZ-2025-03-001	AC01	记账凭证-办公用品	2025	03	30Y	泊冉集团有限公司	会计	archived	2580.00	2025-03-10	JZ-2025-03-001	\N	[{"id": "1", "debit_org": 2283.19, "accsubject": {"code": "6602", "name": "管理费用-办公费"}, "credit_org": 0, "description": "办公用品"}, {"id": "2", "debit_org": 296.81, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 2580.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-office-hd-001	BR-GROUP	HD-2025-03-001	AC04	银行回单-工商银行转账	2025	03	30Y	泊冉集团有限公司	系统	archived	2580.00	2025-03-10	HD-2025-03-001	\N	{"bank": "工商银行", "accountTo": "6225882222222222", "accountFrom": "6225881234567890", "transactionType": "转账"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-service-ht-001	BR-GROUP	HT-2025-04-001	AC04	年度审计服务合同	2025	04	30Y	泊冉集团有限公司	财务部	archived	120000.00	2025-04-01	HT-2025-04-001	\N	{"period": "2025年度", "vendor": "XX会计师事务所", "serviceType": "年度审计", "contractType": "服务合同"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-service-fp-001	BR-GROUP	FP-2025-04-001	AC01	审计服务费发票-Q1	2025	04	30Y	泊冉集团有限公司	系统	archived	30000.00	2025-04-05	FP-2025-04-001	\N	{"period": "Q1", "vendor": "XX会计师事务所", "invoiceType": "增值税专用发票"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-service-fp-002	BR-GROUP	FP-2025-05-001	AC01	审计服务费发票-Q2	2025	05	30Y	泊冉集团有限公司	系统	archived	30000.00	2025-05-05	FP-2025-05-001	\N	{"period": "Q2", "vendor": "XX会计师事务所", "invoiceType": "增值税专用发票"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-service-fp-003	BR-GROUP	FP-2025-06-001	AC01	审计服务费发票-Q3	2025	06	30Y	泊冉集团有限公司	系统	archived	30000.00	2025-06-05	FP-2025-06-001	\N	{"period": "Q3", "vendor": "XX会计师事务所", "invoiceType": "增值税专用发票"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-service-jz-001	BR-GROUP	JZ-2025-06-001	AC01	记账凭证-审计服务费	2025	06	30Y	泊冉集团有限公司	会计	archived	90000.00	2025-06-10	JZ-2025-06-001	\N	[{"id": "1", "debit_org": 79646.02, "accsubject": {"code": "6602", "name": "管理费用-审计费"}, "credit_org": 0, "description": "审计服务费"}, {"id": "2", "debit_org": 10353.98, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 90000.00, "description": "银行付款"}]	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
demo-service-hd-001	BR-GROUP	HD-2025-06-001	AC04	银行回单-建设银行转账	2025	06	30Y	泊冉集团有限公司	系统	archived	90000.00	2025-06-10	HD-2025-06-001	\N	{"bank": "建设银行", "accountTo": "6225883333333333", "accountFrom": "6225881234567890", "transactionType": "转账"}	internal	\N	\N	system	\N	\N	\N	2026-01-15 13:59:43.247426	2026-01-15 13:59:43.247426	0	\N	f	\N	\N	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
6090f8648c4c4a3280376cad676f076f	BR-GROUP	YS-20260115-6090F864	AC01	803e45646af25af207ede4518bb6882b	2020	2025-08	30Y	用友 YonSuite (生产环境)	51e343dfadf277f80a2e8510f47ce173	MATCHED	150.00	2025-08-01	用友 YonSuite (生产环境)_记-1	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	2026-01-15 18:15:16.416292	0	\N	f	\N	803e45646af25af207ede4518bb6882b	0	智能算法	\N	\N	\N	\N	\N	\N	NORMAL
80b4c7e896474f9180f4a821c46cedef	BR-GROUP	YS-20260115-80B4C7E8	AC01	81572672c5b8cf274f6a64620748df14	2020	2025-08	30Y	用友 YonSuite (生产环境)	51e343dfadf277f80a2e8510f47ce173	MATCHED	150.00	2025-08-01	用友 YonSuite (生产环境)_记-2	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	2026-01-15 18:15:16.429073	0	\N	f	\N	81572672c5b8cf274f6a64620748df14	0	智能算法	\N	\N	\N	\N	\N	\N	NORMAL
be4a056e6fbf471281c9341d876da5c0	BR-GROUP	BR-GROUP-2020-30Y-ORG-AC01-000001	AC01	记-4_Voucher	2020	2025-08	30Y	用友 YonSuite (生产环境)	51e343dfadf277f80a2e8510f47ce173	ARCHIVED	50.00	2025-08-09	用友 YonSuite (生产环境)_记-4	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	2026-01-15 16:16:45.81305	0	\N	f	\N	记-4_Voucher.pdf	0	\N	\N	\N	\N	\N	\N	\N	NORMAL
b7817092a1294f82b0b8cd3cd346fc1b	BR-GROUP	YS-20260115-B7817092	AC01	e65924927abaf098fef1d99c4ad4df5a	2020	2025-08	30Y	用友 YonSuite (生产环境)	51e343dfadf277f80a2e8510f47ce173	MATCHED	30.00	2025-08-09	用友 YonSuite (生产环境)_记-3	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	2026-01-15 18:15:16.432509	0	\N	f	\N	e65924927abaf098fef1d99c4ad4df5a	0	智能算法	\N	\N	\N	\N	\N	\N	NORMAL
\.


--
-- Data for Name: acc_archive_attachment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_time, created_at) FROM stdin;
attach-link-001	voucher-2024-11-002	file-invoice-001	invoice	原始发票	system	2025-12-28 09:03:35.017527	2026-01-14 18:54:06.830786
attach-link-002	voucher-2024-11-001	file-invoice-002	invoice	原始发票	system	2025-12-28 09:03:35.017527	2026-01-14 18:54:06.830786
link-bank-1002	voucher-2024-11-002	file-bank-receipt-1002	bank_slip	银行回单附件	system	2025-12-28 09:03:35.066446	2026-01-14 18:54:06.830786
link-reimb-1002	voucher-2024-11-002	file-reimbursement-1002	other	员工报销单据	system	2025-12-28 09:03:35.066446	2026-01-14 18:54:06.830786
attach-link-003	voucher-2024-11-003	file-invoice-003	invoice	原始凭证	system	2025-12-28 09:03:35.017527	2026-01-14 18:54:06.830786
\.


--
-- Data for Name: acc_archive_relation; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_time, deleted, created_at) FROM stdin;
seed-rel-001	seed-contract-001	seed-voucher-001	BASIS	合同依据	system	2025-12-28 09:03:34.854164	0	2026-01-14 18:54:06.830786
seed-rel-002	seed-invoice-001	seed-voucher-001	ORIGINAL_VOUCHER	原始凭证	system	2025-12-28 09:03:34.854164	0	2026-01-14 18:54:06.830786
seed-rel-003	seed-invoice-002	seed-voucher-001	ORIGINAL_VOUCHER	原始凭证	system	2025-12-28 09:03:34.854164	0	2026-01-14 18:54:06.830786
seed-rel-004	seed-voucher-001	seed-receipt-001	CASH_FLOW	资金流	system	2025-12-28 09:03:34.854164	0	2026-01-14 18:54:06.830786
seed-rel-005	seed-voucher-001	seed-report-001	ARCHIVE	归档	system	2025-12-28 09:03:34.854164	0	2026-01-14 18:54:06.830786
rel-v2024-11-004-con	voucher-2024-11-004	other-contract-2024-001	BASIS	合同依据	system	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
rel-v2024-10-001-con	voucher-2024-10-001	other-contract-2024-002	BASIS	租赁合同依据	system	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
rel-v2024-12-001-con	voucher-2024-12-001	other-contract-2024-003	BASIS	审计服务合同依据	system	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
rel-v2024-11-005-lr	voucher-2024-11-005	report-2024-lr-11	ARCHIVE	归入月度报表	system	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
demo-rel-001	demo-reimb-sq-001	demo-reimb-bx-001	BASIS	出差申请依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-002	demo-reimb-fp-001	demo-reimb-bx-001	ORIGINAL_VOUCHER	交通费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-003	demo-reimb-fp-002	demo-reimb-bx-001	ORIGINAL_VOUCHER	住宿费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-004	demo-reimb-fp-003	demo-reimb-bx-001	ORIGINAL_VOUCHER	餐饮费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-005	demo-reimb-fp-004	demo-reimb-bx-001	ORIGINAL_VOUCHER	出租车费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-006	demo-reimb-bx-001	demo-reimb-fk-001	CASH_FLOW	报销付款	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-007	demo-reimb-fk-001	demo-reimb-hd-001	CASH_FLOW	银行转账	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-008	demo-reimb-fk-001	demo-reimb-jz-001	ARCHIVE	凭证归档	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-009	demo-reimb-jz-001	demo-reimb-bb-001	ARCHIVE	报表归档	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-030	demo-reimb-fp-001	demo-reimb-jz-001	ORIGINAL_VOUCHER	交通费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-031	demo-reimb-fp-002	demo-reimb-jz-001	ORIGINAL_VOUCHER	住宿费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-032	demo-reimb-fp-003	demo-reimb-jz-001	ORIGINAL_VOUCHER	餐饮费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-033	demo-reimb-fp-004	demo-reimb-jz-001	ORIGINAL_VOUCHER	出租车费原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-034	demo-reimb-bx-001	demo-reimb-jz-001	BASIS	报销单依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-035	demo-reimb-sq-001	demo-reimb-jz-001	BASIS	出差申请依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-010	demo-purchase-ht-001	demo-purchase-fp-001	BASIS	合同依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-011	demo-purchase-fp-001	demo-purchase-jz-001	ORIGINAL_VOUCHER	原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-012	demo-purchase-jz-001	demo-purchase-fk-001	CASH_FLOW	资金流	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-013	demo-purchase-fk-001	demo-purchase-hd-001	CASH_FLOW	银行转账	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-014	demo-office-fp-001	demo-office-jz-001	ORIGINAL_VOUCHER	原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-015	demo-office-jz-001	demo-office-hd-001	CASH_FLOW	银行转账	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-016	demo-service-ht-001	demo-service-fp-001	BASIS	合同依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-017	demo-service-ht-001	demo-service-fp-002	BASIS	合同依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-018	demo-service-ht-001	demo-service-fp-003	BASIS	合同依据	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-019	demo-service-fp-001	demo-service-jz-001	ORIGINAL_VOUCHER	原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-020	demo-service-fp-002	demo-service-jz-001	ORIGINAL_VOUCHER	原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-021	demo-service-fp-003	demo-service-jz-001	ORIGINAL_VOUCHER	原始凭证	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
demo-rel-022	demo-service-jz-001	demo-service-hd-001	CASH_FLOW	银行转账	system	2026-01-15 13:59:43.247426	0	2026-01-15 13:59:43.247426
rel-con-inv-001	seed-invoice-001	seed-contract-001	BASIS	合同依据发票	system	2026-01-15 06:39:47.401521	0	2026-01-15 06:39:47.401521
rel-con-inv-002	seed-invoice-002	seed-contract-001	BASIS	合同依据发票	system	2026-01-15 06:39:48.946128	0	2026-01-15 06:39:48.946128
\.


--
-- Data for Name: acc_archive_volume; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, volume_status, reviewed_by, reviewed_at, archived_at, custodian_dept, validation_report_path, created_time, last_modified_time, deleted, updated_time) FROM stdin;
volume-2024-11	AJ-2024-11	2024年11月会计凭证	BR-GROUP	2024	11	AC01	8	30Y	archived	user-qianqi	\N	2025-12-28 09:03:35.017527	ACCOUNTING	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
volume-2024-10	AJ-2024-10	2024年10月会计凭证	BR-GROUP	2024	10	AC01	5	30Y	archived	user-qianqi	\N	2025-12-28 09:03:35.017527	ACCOUNTING	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
volume-2024-09	AJ-2024-09	2024年9月会计凭证	BR-GROUP	2024	09	AC01	2	30Y	archived	user-qianqi	\N	2025-12-28 09:03:35.017527	ACCOUNTING	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
volume-2023-12	AJ-2023-12	2023年12月会计凭证	BR-GROUP	2023	12	AC01	2	30Y	archived	user-qianqi	\N	2025-12-28 09:03:35.017527	ACCOUNTING	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786
\.


--
-- Data for Name: acc_borrow_archive; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_borrow_archive (id, borrow_request_id, archive_id, archive_code, archive_title, return_status, return_time, return_operator_id, damaged, damage_desc, created_time) FROM stdin;
\.


--
-- Data for Name: acc_borrow_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_borrow_log (id, request_no, applicant_id, applicant_name, dept_name, purpose, borrow_type, borrow_start_date, borrow_end_date, archive_count, status, created_time, completed_time) FROM stdin;
\.


--
-- Data for Name: acc_borrow_request; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.acc_borrow_request (id, request_no, applicant_id, applicant_name, dept_id, dept_name, purpose, borrow_type, expected_start_date, expected_end_date, status, archive_ids, archive_count, approver_id, approver_name, approval_time, approval_comment, actual_start_date, actual_end_date, return_time, return_operator_id, created_time, updated_time, deleted) FROM stdin;
\.


--
-- Data for Name: access_review; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.access_review (id, user_id, review_type, review_date, reviewer_id, status, current_roles, current_permissions, review_result, action_taken, next_review_date, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: arc_abnormal_voucher; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_abnormal_voucher (id, request_id, source_system, voucher_number, sip_data, fail_reason, status, create_time, update_time, fonds_code) FROM stdin;
abn-001	req-2025-001	YONSuite	INV-2025-001	{"voucherType":"VAT_INVOICE","amount":100.00}	发票号格式错误	PENDING	2026-01-15 10:16:04.785197	2026-01-15 10:16:04.785197	default
abn-002	req-2025-002	YonSuite	BANK-2025-001	{"voucherType":"BANK_SLIP","amount":5000.00}	金额不匹配	PENDING	2026-01-15 10:16:04.785197	2026-01-15 10:16:04.785197	BR01
abn-003	req-2025-003	YONSuite	INV-2025-002	{"voucherType":"VAT_INVOICE","amount":200.00}	缺少必填字段	RETRYING	2026-01-15 10:16:04.785197	2026-01-15 10:16:04.785197	default
abn-004	req-2025-004	YONSuite	EXP-2025-001	{"voucherType":"EXPENSE","amount":500.00}	供应商不存在	RESOLVED	2026-01-15 10:16:04.785197	2026-01-15 10:16:04.785197	BR01
\.


--
-- Data for Name: arc_archive_batch; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_archive_batch (id, batch_no, prev_batch_hash, current_batch_hash, chained_hash, hash_algo, item_count, operator_id, created_time, batch_sequence) FROM stdin;
\.


--
-- Data for Name: arc_convert_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_convert_log (id, archive_id, source_format, target_format, source_path, target_path, status, error_message, file_size_bytes, convert_duration_ms, created_time, source_size, target_size, duration_ms, convert_time) FROM stdin;
\.


--
-- Data for Name: arc_file_content; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta, pre_archive_status) FROM stdin;
demo-file-001	BRJT-2024-30Y-FIN-AC01-0001	上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf	pdf	101613	\N	\N	uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf	2026-01-14 10:57:48.887492	arc-2024-001	\N	\N	\N	\N	\N	\N	\N	\N	BRJT	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.2756, "height": 396.8504, "regions": {"total_amount": {"h": 4.4954276, "w": 4.499939, "x": 286.04657, "y": 168.65747}}, "total_amount_value": "1"}	PENDING_CHECK
demo-file-002	BR-GROUP-2025-30Y-FIN-AC01-0002	dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf	pdf	101657	\N	\N	uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf	2026-01-14 10:57:48.889454	arc-2024-002	\N	\N	\N	\N	\N	\N	\N	\N	BR-GROUP	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.2756, "height": 396.8504, "regions": {"total_amount": {"h": 8.799959, "w": 33.59964, "x": 401.51144, "y": 268.2412}}, "total_amount_value": "650.50"}	PENDING_CHECK
demo-file-003	BRJT-2024-30Y-FIN-AC01-0003	报销.pdf	pdf	107783	\N	\N	uploads/demo/报销.pdf	2026-01-14 10:57:48.889854	arc-2024-003	\N	\N	\N	\N	\N	\N	\N	\N	BRJT	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.0, "height": 842.0, "regions": {}}	PENDING_CHECK
demo-file-004	BRJT-2024-30Y-FIN-AC01-0004	25312000000349611002_ba1d.pdf	pdf	101601	\N	\N	uploads/demo/25312000000349611002_ba1d.pdf	2026-01-14 10:57:48.890301	arc-2024-004	\N	\N	\N	\N	\N	\N	\N	\N	BRJT	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.2756, "height": 396.8504, "regions": {"total_amount": {"h": 4.4954276, "w": 4.499939, "x": 286.04657, "y": 168.65747}}, "total_amount_value": "1"}	PENDING_CHECK
demo-file-005	BRJT-2024-30Y-FIN-AC01-0005	20220927580001302018_上海强生出租汽车有限公司第一分公司_上海强生交通（集团）有限公司_20220920_10000.00.pdf	pdf	25399	\N	\N	uploads/demo/20220927580001302018_上海强生出租汽车有限公司第一分公司_上海强生交通（集团）有限公司_20220920_10000.00.pdf	2026-01-14 10:57:48.890662	arc-2024-005	\N	\N	\N	\N	\N	\N	\N	\N	BRJT	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.0, "height": 842.0, "regions": {"total_amount": {"h": 5.6699996, "w": 37.099976, "x": 361.0, "y": 165.71002}}, "total_amount_value": "10000.00"}	PENDING_CHECK
c9737086-ddbf-138e-7e5f-1b9d696280a7	ARC-BOOK-2024-BANK	ARC-BOOK-2024-BANK.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-BOOK-2024-BANK.pdf	2025-12-28 09:03:35.080539	seed-book-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-BOOK-2024-BANK", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-BOOK-2024-BANK	\N	\N	COMPLETED
file-invoice-003	BR-GROUP-2025-30Y-FIN-AC01-1003	电子发票_米山神鸡_201元.pdf	PDF	101613	b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e	SHA-256	uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf	2025-12-28 09:03:35.017527	voucher-2024-11-003	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.2756, "height": 396.8504, "regions": {"total_amount": {"h": 4.4954276, "w": 4.499939, "x": 286.04657, "y": 168.65747}}, "total_amount_value": "1"}	PENDING_CHECK
file-bank-receipt-1002	BR-GROUP-2022-30Y-FIN-AC01-1002	银行回单_10000元.pdf	PDF	102400	hash_placeholder_1	SHA-256	uploads/demo/bank_receipt_1002.pdf	2025-12-28 09:03:35.061126	voucher-2024-11-002	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 612.0, "height": 792.0, "regions": {}}	PENDING_CHECK
a688f9e3-35a0-5121-fcb4-30ae1cb2cc7d	CON-2023-098	CON-2023-098.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/CON-2023-098.pdf	2025-12-28 09:03:35.080539	seed-contract-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "CON-2023-098", "maketime": "2023-01-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 150000.00, "credit_original": 150000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	CON-2023-098	2023-01-15	\N	COMPLETED
c8c768e3-ee4f-4c0e-936a-1cda3a292b51	C-202511-002	C-202511-002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/C-202511-002.pdf	2025-12-28 09:03:35.080539	seed-contract-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "C-202511-002", "maketime": "2025-11-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 450000.00, "credit_original": 450000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	C-202511-002	2025-11-15	\N	COMPLETED
f4653466-b670-a083-acff-19ff6d55be02	INV-202311-089	INV-202311-089.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/INV-202311-089.pdf	2025-12-28 09:03:35.080539	seed-invoice-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "INV-202311-089", "maketime": "2023-11-02", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 12800.00, "credit_original": 12800.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	INV-202311-089	2023-11-02	\N	COMPLETED
1dc0f7b7-c5b4-2882-5cfc-9b7f9e0748bc	INV-202311-092	INV-202311-092.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/INV-202311-092.pdf	2025-12-28 09:03:35.080539	seed-invoice-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "INV-202311-092", "maketime": "2023-11-03", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 45200.00, "credit_original": 45200.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	INV-202311-092	2023-11-03	\N	COMPLETED
7f5d1ad9-ee09-9f47-4fc8-e0bddbb02e39	JZ-202311-0052	JZ-202311-0052.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/JZ-202311-0052.pdf	2025-12-28 09:03:35.080539	seed-voucher-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "JZ-202311-0052", "maketime": "2023-11-05", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 58000.00, "credit_original": 58000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	JZ-202311-0052	2023-11-05	\N	COMPLETED
eb867bea-c3bf-91ad-e54d-4291ba3b40ea	V-202511-TEST	V-202511-TEST.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/V-202511-TEST.pdf	2025-12-28 09:03:35.080539	seed-voucher-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "V-202511-TEST", "maketime": "2025-11-07", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 5280.00, "credit_original": 5280.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	V-202511-TEST	2025-11-07	\N	COMPLETED
45617208-7665-cfa7-7e8d-378997a9f996	B-20231105-003	B-20231105-003.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/B-20231105-003.pdf	2025-12-28 09:03:35.080539	seed-receipt-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "B-20231105-003", "maketime": "2023-11-05", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 58000.00, "credit_original": 58000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	B-20231105-003	2023-11-05	\N	COMPLETED
d1d8378c-5c96-9796-7687-073dfac21783	REP-2023-11	REP-2023-11.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/REP-2023-11.pdf	2025-12-28 09:03:35.080539	seed-report-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "REP-2023-11", "maketime": "2023-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	REP-2023-11	2023-11-30	\N	COMPLETED
3fdde7b4-4eb3-cd7d-8d94-4a3995504688	ARC-BOOK-2024-GL	ARC-BOOK-2024-GL.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-BOOK-2024-GL.pdf	2025-12-28 09:03:35.080539	seed-book-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-BOOK-2024-GL", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-BOOK-2024-GL	\N	\N	COMPLETED
4b29c5cf-cda4-58f0-b5d2-3387b88fa731	ARC-BOOK-2024-CASH	ARC-BOOK-2024-CASH.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-BOOK-2024-CASH.pdf	2025-12-28 09:03:35.080539	seed-book-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-BOOK-2024-CASH", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-BOOK-2024-CASH	\N	\N	COMPLETED
file-reimbursement-1002	BR-GROUP-2022-30Y-FIN-AC01-1002	员工报销单.pdf	PDF	51200	hash_placeholder_2	SHA-256	uploads/demo/reimbursement_1002.pdf	2025-12-28 09:03:35.061126	voucher-2024-11-002	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 612.0, "height": 792.0, "regions": {}}	PENDING_CHECK
ac419629-f6e4-c381-8941-428a9c46665d	ARC-BOOK-2024-FIXED	ARC-BOOK-2024-FIXED.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-BOOK-2024-FIXED.pdf	2025-12-28 09:03:35.080539	seed-book-004	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-BOOK-2024-FIXED", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-BOOK-2024-FIXED	\N	\N	COMPLETED
f6e4fce3-2d6c-bda0-d7dc-e91b0384b554	ARC-REP-2024-M01	ARC-REP-2024-M01.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-REP-2024-M01.pdf	2025-12-28 09:03:35.080539	seed-c03-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-REP-2024-M01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-REP-2024-M01	\N	\N	COMPLETED
129d19ec-5cce-c132-840b-f26d8ebc6336	ARC-REP-2024-Q1	ARC-REP-2024-Q1.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-REP-2024-Q1.pdf	2025-12-28 09:03:35.080539	seed-c03-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-REP-2024-Q1", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-REP-2024-Q1	\N	\N	COMPLETED
9f322f5e-6325-03c2-2933-af4d0d0352b4	ARC-REP-2023-ANN	ARC-REP-2023-ANN.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-REP-2023-ANN.pdf	2025-12-28 09:03:35.080539	seed-c03-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-REP-2023-ANN", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-REP-2023-ANN	\N	\N	COMPLETED
0754d9bb-2da4-d61f-6c4f-a2e1488d72d1	ARC-OTH-2024-BK-01	ARC-OTH-2024-BK-01.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-OTH-2024-BK-01.pdf	2025-12-28 09:03:35.080539	seed-c04-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-OTH-2024-BK-01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-OTH-2024-BK-01	\N	\N	COMPLETED
dc877913-6174-c47f-f6ef-65e6acfd8e77	ARC-OTH-2024-TAX-01	ARC-OTH-2024-TAX-01.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-OTH-2024-TAX-01.pdf	2025-12-28 09:03:35.080539	seed-c04-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-OTH-2024-TAX-01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-OTH-2024-TAX-01	\N	\N	COMPLETED
c8f0b420-d72a-b4df-a17c-960256208195	ARC-OTH-2024-HO-01	ARC-OTH-2024-HO-01.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/ARC-OTH-2024-HO-01.pdf	2025-12-28 09:03:35.080539	seed-c04-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "ARC-OTH-2024-HO-01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	ARC-OTH-2024-HO-01	\N	\N	COMPLETED
8679a4b4-1be5-b057-e291-ffbc02811f63	BR-GROUP-2024-30Y-FIN-AC01-3001	BR-GROUP-2024-30Y-FIN-AC01-3001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-3001.pdf	2025-12-28 09:03:35.080539	voucher-2024-12-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-3001", "maketime": "2024-12-10", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 88000.00, "credit_original": 88000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-3001	2024-12-10	\N	COMPLETED
691e7863-4047-ac94-56a3-9cdedc5c0cd3	BR-GROUP-2024-30Y-FIN-AC01-3002	BR-GROUP-2024-30Y-FIN-AC01-3002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-3002.pdf	2025-12-28 09:03:35.080539	voucher-2024-12-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-3002", "maketime": "2024-12-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 2580000.00, "credit_original": 2580000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-3002	2024-12-20	\N	COMPLETED
e8d8e2e5-407a-00ab-39cf-0465fd59caab	BR-GROUP-2023-30Y-FIN-AC01-0011	BR-GROUP-2023-30Y-FIN-AC01-0011.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0011.pdf	2025-12-28 09:03:35.080539	voucher-2023-11-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0011", "maketime": "2023-11-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 45800.00, "credit_original": 45800.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0011	2023-11-15	\N	COMPLETED
479ca8ec-933d-9486-fc1f-0299a064bcb7	BR-GROUP-2023-30Y-FIN-AC01-0021	BR-GROUP-2023-30Y-FIN-AC01-0021.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0021.pdf	2025-12-28 09:03:35.080539	voucher-2023-10-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0021", "maketime": "2023-10-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 580000.00, "credit_original": 580000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0021	2023-10-20	\N	COMPLETED
23029d10-ce5b-e2c7-4345-10bc7b52a6f1	BR-GROUP-2022-30Y-FIN-AC01-0001	BR-GROUP-2022-30Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-2022-12-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0001", "maketime": "2022-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 2680000.00, "credit_original": 2680000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-30Y-FIN-AC01-0001	2022-12-31	\N	COMPLETED
76071bfc-174f-1160-c5cc-590435c5b943	BR-GROUP-2022-30Y-FIN-AC01-0011	BR-GROUP-2022-30Y-FIN-AC01-0011.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0011.pdf	2025-12-28 09:03:35.080539	voucher-2022-06-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0011", "maketime": "2022-06-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 1250000.00, "credit_original": 1250000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-30Y-FIN-AC01-0011	2022-06-30	\N	COMPLETED
77d2fcb3-279d-756d-6d48-bac2f89d1669	BR-GROUP-2025-30Y-FIN-AC01-0001	BR-GROUP-2025-30Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2025-30Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-2025-01-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2025-30Y-FIN-AC01-0001", "maketime": "2025-01-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 2580000.00, "credit_original": 2580000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2025-30Y-FIN-AC01-0001	2025-01-15	\N	COMPLETED
fd42ce2f-919e-e293-1caf-933b627306d0	BR-SALES-2024-30Y-FIN-AC01-0001	BR-SALES-2024-30Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-SALES-2024-30Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-sales-2024-11-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-SALES-2024-30Y-FIN-AC01-0001", "maketime": "2024-11-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 1280000.00, "credit_original": 1280000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-SALES-2024-30Y-FIN-AC01-0001	2024-11-18	\N	COMPLETED
62656fa1-ee14-4730-817f-6c1c7b633cc9	BR-TRADE-2024-30Y-FIN-AC01-0001	BR-TRADE-2024-30Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-TRADE-2024-30Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-trade-2024-11-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-TRADE-2024-30Y-FIN-AC01-0001", "maketime": "2024-11-22", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 860000.00, "credit_original": 860000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-TRADE-2024-30Y-FIN-AC01-0001	2024-11-22	\N	COMPLETED
f126af4c-1246-a8f1-5e34-251341077197	BR-MFG-2024-30Y-FIN-AC01-0001	BR-MFG-2024-30Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-MFG-2024-30Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-mfg-2024-11-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-MFG-2024-30Y-FIN-AC01-0001", "maketime": "2024-11-08", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 156000.00, "credit_original": 156000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-MFG-2024-30Y-FIN-AC01-0001	2024-11-08	\N	COMPLETED
49536a22-1b58-0475-3d95-d9c005bea876	BR-GROUP-2024-30Y-FIN-AC02-ZZ001	BR-GROUP-2024-30Y-FIN-AC02-ZZ001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-ZZ001.pdf	2025-12-28 09:03:35.080539	ledger-2024-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-ZZ001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC02-ZZ001	2024-12-31	\N	COMPLETED
974df683-b584-dd21-9908-a6fb0cc27bf5	BR-GROUP-2024-30Y-FIN-AC02-MX001	BR-GROUP-2024-30Y-FIN-AC02-MX001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-MX001.pdf	2025-12-28 09:03:35.080539	ledger-2024-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-MX001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC02-MX001	2024-12-31	\N	COMPLETED
796aee94-c6e8-a1d0-5675-ff980d5ddb28	BR-GROUP-2024-30Y-FIN-AC02-RJ001	BR-GROUP-2024-30Y-FIN-AC02-RJ001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-RJ001.pdf	2025-12-28 09:03:35.080539	ledger-2024-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-RJ001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC02-RJ001	2024-12-31	\N	COMPLETED
4439068a-12ec-3ab7-8486-34593a9dcb7a	BR-GROUP-2024-30Y-FIN-AC02-YS001	BR-GROUP-2024-30Y-FIN-AC02-YS001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-YS001.pdf	2025-12-28 09:03:35.080539	ledger-2024-004	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-YS001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC02-YS001	2024-12-31	\N	COMPLETED
88a3aa69-f5e8-6a04-6c02-27b5c4b61549	BR-GROUP-2024-30Y-FIN-AC02-GD001	BR-GROUP-2024-30Y-FIN-AC02-GD001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-GD001.pdf	2025-12-28 09:03:35.080539	ledger-2024-005	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-GD001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC02-GD001	2024-12-31	\N	COMPLETED
71e000b3-27e3-86af-b8fb-4fcbe6c3d8d4	BR-GROUP-2023-30Y-FIN-AC02-ZZ001	BR-GROUP-2023-30Y-FIN-AC02-ZZ001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC02-ZZ001.pdf	2025-12-28 09:03:35.080539	ledger-2023-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC02-ZZ001", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC02-ZZ001	2023-12-31	\N	COMPLETED
d9b3dae8-c7f1-a44a-4762-94675732cbb9	BR-GROUP-2023-30Y-FIN-AC02-MX001	BR-GROUP-2023-30Y-FIN-AC02-MX001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC02-MX001.pdf	2025-12-28 09:03:35.080539	ledger-2023-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC02-MX001", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC02-MX001	2023-12-31	\N	COMPLETED
d23c9293-e081-a4ac-59a4-ad8105edad04	BR-GROUP-2022-30Y-FIN-AC02-ZZ001	BR-GROUP-2022-30Y-FIN-AC02-ZZ001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC02-ZZ001.pdf	2025-12-28 09:03:35.080539	ledger-2022-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC02-ZZ001", "maketime": "2022-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-30Y-FIN-AC02-ZZ001	2022-12-31	\N	COMPLETED
6787cd0f-07d8-93a4-7931-db76ddadb201	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11.pdf	2025-12-28 09:03:35.080539	report-2024-zcfz-11	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11", "maketime": "2024-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11	2024-11-30	\N	COMPLETED
609ca558-763d-86c8-d110-c5f3c5693bc5	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10.pdf	2025-12-28 09:03:35.080539	report-2024-zcfz-10	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10", "maketime": "2024-10-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10	2024-10-31	\N	COMPLETED
472450ef-4a97-3e1c-9733-e15373d8423d	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09.pdf	2025-12-28 09:03:35.080539	report-2024-zcfz-09	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09", "maketime": "2024-09-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09	2024-09-30	\N	COMPLETED
c78eee00-db72-79ce-ef97-fcc7093561fd	BR-GROUP-2024-PERM-FIN-AC03-LR11	BR-GROUP-2024-PERM-FIN-AC03-LR11.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-LR11.pdf	2025-12-28 09:03:35.080539	report-2024-lr-11	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-LR11", "maketime": "2024-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-PERM-FIN-AC03-LR11	2024-11-30	\N	COMPLETED
d7b07746-c7d0-51c6-e885-03efe9d86efd	BR-GROUP-2024-PERM-FIN-AC03-LR10	BR-GROUP-2024-PERM-FIN-AC03-LR10.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-LR10.pdf	2025-12-28 09:03:35.080539	report-2024-lr-10	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-LR10", "maketime": "2024-10-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-PERM-FIN-AC03-LR10	2024-10-31	\N	COMPLETED
2506ca90-601a-e5e0-bebf-0e2dec2028aa	BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3	BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3.pdf	2025-12-28 09:03:35.080539	report-2024-xjll-q3	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3", "maketime": "2024-09-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3	2024-09-30	\N	COMPLETED
59caf205-d7b0-3936-6770-59d3944099c4	BR-GROUP-2023-PERM-FIN-AC03-ANNUAL	BR-GROUP-2023-PERM-FIN-AC03-ANNUAL.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-PERM-FIN-AC03-ANNUAL.pdf	2025-12-28 09:03:35.080539	report-2023-annual	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-PERM-FIN-AC03-ANNUAL", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-PERM-FIN-AC03-ANNUAL	2023-12-31	\N	COMPLETED
53b373d0-5066-f911-ca61-64d191ac7c73	BR-GROUP-2022-PERM-FIN-AC03-ANNUAL	BR-GROUP-2022-PERM-FIN-AC03-ANNUAL.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-PERM-FIN-AC03-ANNUAL.pdf	2025-12-28 09:03:35.080539	report-2022-annual	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-PERM-FIN-AC03-ANNUAL", "maketime": "2022-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-PERM-FIN-AC03-ANNUAL	2022-12-31	\N	COMPLETED
eb7e102f-83c2-8fc4-92dc-5c61504932fb	BR-GROUP-2024-30Y-FIN-AC04-BANK11	BR-GROUP-2024-30Y-FIN-AC04-BANK11.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-BANK11.pdf	2025-12-28 09:03:35.080539	other-bank-2024-11	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-BANK11", "maketime": "2024-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-BANK11	2024-11-30	\N	COMPLETED
857cfc51-58b2-0857-f184-256cc23cbbc6	BR-GROUP-2024-30Y-FIN-AC04-BANK10	BR-GROUP-2024-30Y-FIN-AC04-BANK10.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-BANK10.pdf	2025-12-28 09:03:35.080539	other-bank-2024-10	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-BANK10", "maketime": "2024-10-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-BANK10	2024-10-31	\N	COMPLETED
fb5a30fb-81f1-1a53-015c-5e9a28aecc37	BR-GROUP-2024-30Y-FIN-AC04-TAX11	BR-GROUP-2024-30Y-FIN-AC04-TAX11.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-TAX11.pdf	2025-12-28 09:03:35.080539	other-tax-2024-11	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-TAX11", "maketime": "2024-12-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 168500.00, "credit_original": 168500.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-TAX11	2024-12-15	\N	COMPLETED
d03019d0-5c91-f99e-ddb4-0f6c76490e3d	BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3	BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3.pdf	2025-12-28 09:03:35.080539	other-tax-2024-q3	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3", "maketime": "2024-10-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 286000.00, "credit_original": 286000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3	2024-10-20	\N	COMPLETED
a467f475-8134-3cff-b214-c03a6c5acd88	BR-GROUP-2024-30Y-FIN-AC04-CON001	BR-GROUP-2024-30Y-FIN-AC04-CON001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-CON001.pdf	2025-12-28 09:03:35.080539	other-contract-2024-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-CON001", "maketime": "2024-01-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 158000.00, "credit_original": 158000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-CON001	2024-01-15	\N	COMPLETED
37c3fcbe-5680-5d1c-a096-973bea6c65fa	BR-GROUP-2024-30Y-FIN-AC04-CON002	BR-GROUP-2024-30Y-FIN-AC04-CON002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-CON002.pdf	2025-12-28 09:03:35.080539	other-contract-2024-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-CON002", "maketime": "2024-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 816000.00, "credit_original": 816000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-CON002	2024-01-01	\N	COMPLETED
5663bb69-02d8-d95f-3061-49483f6fd409	BR-GROUP-2024-30Y-FIN-AC04-CON003	BR-GROUP-2024-30Y-FIN-AC04-CON003.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-CON003.pdf	2025-12-28 09:03:35.080539	other-contract-2024-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-CON003", "maketime": "2024-03-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 88000.00, "credit_original": 88000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC04-CON003	2024-03-01	\N	COMPLETED
d0c20030-37e2-0a81-6e5c-dadf156e4363	BR-GROUP-2023-PERM-FIN-AC04-AUDIT	BR-GROUP-2023-PERM-FIN-AC04-AUDIT.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-PERM-FIN-AC04-AUDIT.pdf	2025-12-28 09:03:35.080539	other-audit-2023	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-PERM-FIN-AC04-AUDIT", "maketime": "2024-03-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-PERM-FIN-AC04-AUDIT	2024-03-15	\N	COMPLETED
9987a1c8-3bb8-85db-946e-5974b9154f1e	BR-GROUP-2025-30Y-FIN-AC01-0202	BR-GROUP-2025-30Y-FIN-AC01-0202.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2025-30Y-FIN-AC01-0202.pdf	2025-12-28 09:03:35.080539	voucher-2025-02-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2025-30Y-FIN-AC01-0202", "maketime": "2025-02-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 358000.00, "credit_original": 358000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2025-30Y-FIN-AC01-0202	2025-02-20	\N	COMPLETED
b32a147d-b9c4-8c52-2beb-812c0aa9cb5a	BR-GROUP-2024-30Y-FIN-AC01-0004	BR-GROUP-2024-30Y-FIN-AC01-0004.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0004.pdf	2025-12-28 09:03:35.080539	voucher-2024-11-004	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0004", "maketime": "2024-11-10", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 12800.00, "credit_original": 12800.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-0004	2024-11-10	\N	COMPLETED
93d2e117-2276-39c1-4890-185495c01a89	BR-GROUP-2024-30Y-FIN-AC01-0005	BR-GROUP-2024-30Y-FIN-AC01-0005.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0005.pdf	2025-12-28 09:03:35.080539	voucher-2024-11-005	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0005", "maketime": "2024-11-25", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 856000.00, "credit_original": 856000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-0005	2024-11-25	\N	COMPLETED
05c1073b-c872-6359-4db6-955730b315b8	BR-GROUP-2024-30Y-FIN-AC01-0006	BR-GROUP-2024-30Y-FIN-AC01-0006.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0006.pdf	2025-12-28 09:03:35.080539	voucher-2024-11-006	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0006", "maketime": "2024-11-28", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 45600.00, "credit_original": 45600.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-0006	2024-11-28	\N	COMPLETED
e321278c-eb32-b755-dbcd-3bb0eda9415a	BR-GROUP-2024-30Y-FIN-AC01-0007	BR-GROUP-2024-30Y-FIN-AC01-0007.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0007.pdf	2025-12-28 09:03:35.080539	voucher-2024-11-007	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0007", "maketime": "2024-11-12", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 286500.00, "credit_original": 286500.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-0007	2024-11-12	\N	COMPLETED
21522c99-8779-0401-090f-d9de65563aa3	BR-GROUP-2024-30Y-FIN-AC01-0008	BR-GROUP-2024-30Y-FIN-AC01-0008.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0008.pdf	2025-12-28 09:03:35.080539	voucher-2024-11-008	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0008", "maketime": "2024-11-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 520000.00, "credit_original": 520000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-0008	2024-11-15	\N	COMPLETED
734f0356-07a7-a9b2-d204-e62fa69c2329	BR-GROUP-2024-30Y-FIN-AC01-1001	BR-GROUP-2024-30Y-FIN-AC01-1001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1001.pdf	2025-12-28 09:03:35.080539	voucher-2024-10-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1001", "maketime": "2024-10-05", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 85000.00, "credit_original": 85000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-1001	2024-10-05	\N	COMPLETED
22a57159-390d-f21d-b6d4-73ced4be816f	BR-GROUP-2024-30Y-FIN-AC01-1002	BR-GROUP-2024-30Y-FIN-AC01-1002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1002.pdf	2025-12-28 09:03:35.080539	voucher-2024-10-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1002", "maketime": "2024-10-12", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 468000.00, "credit_original": 468000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-1002	2024-10-12	\N	COMPLETED
9baac94d-c7d8-4224-9d98-b5fcf103187c	BR-GROUP-2023-30Y-FIN-AC01-0091	BR-GROUP-2023-30Y-FIN-AC01-0091.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0091.pdf	2025-12-28 09:03:35.080539	voucher-2023-09-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0091", "maketime": "2023-09-25", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 186500.00, "credit_original": 186500.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0091	2023-09-25	\N	COMPLETED
bf511737-1a58-0966-c020-b3dff1c9c629	BR-GROUP-2024-30Y-FIN-AC01-1003	BR-GROUP-2024-30Y-FIN-AC01-1003.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1003.pdf	2025-12-28 09:03:35.080539	voucher-2024-10-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1003", "maketime": "2024-10-25", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 842000.00, "credit_original": 842000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-1003	2024-10-25	\N	COMPLETED
24858a2f-03f0-3327-f817-1b1ee51e716b	BR-GROUP-2024-30Y-FIN-AC01-1004	BR-GROUP-2024-30Y-FIN-AC01-1004.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1004.pdf	2025-12-28 09:03:35.080539	voucher-2024-10-004	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1004", "maketime": "2024-10-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 28600.00, "credit_original": 28600.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-1004	2024-10-18	\N	COMPLETED
38e8a8b2-4318-5124-affa-7457b5b1e784	BR-GROUP-2024-30Y-FIN-AC01-1005	BR-GROUP-2024-30Y-FIN-AC01-1005.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1005.pdf	2025-12-28 09:03:35.080539	voucher-2024-10-005	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1005", "maketime": "2024-10-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 380000.00, "credit_original": 380000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-1005	2024-10-30	\N	COMPLETED
0967e5d9-6d8d-2daf-883e-5dba15890611	BR-GROUP-2024-30Y-FIN-AC01-2001	BR-GROUP-2024-30Y-FIN-AC01-2001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-2001.pdf	2025-12-28 09:03:35.080539	voucher-2024-09-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-2001", "maketime": "2024-09-08", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 185000.00, "credit_original": 185000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-2001	2024-09-08	\N	COMPLETED
716d1742-7a3c-78d3-c1a4-373198ffa9c9	BR-GROUP-2024-30Y-FIN-AC01-2002	BR-GROUP-2024-30Y-FIN-AC01-2002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-2002.pdf	2025-12-28 09:03:35.080539	voucher-2024-09-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-2002", "maketime": "2024-09-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 68000.00, "credit_original": 68000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2024-30Y-FIN-AC01-2002	2024-09-15	\N	COMPLETED
4a6f4204-ec2e-b3ae-1513-e45d2f0d5041	BR-GROUP-2023-30Y-FIN-AC01-0001	BR-GROUP-2023-30Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-2023-12-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0001", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 3860000.00, "credit_original": 3860000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0001	2023-12-31	\N	COMPLETED
beb543f7-fb81-2c25-3be3-8b1adce57039	BR-GROUP-2014-10Y-FIN-AC01-0001	BR-GROUP-2014-10Y-FIN-AC01-0001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2014-10Y-FIN-AC01-0001.pdf	2025-12-28 09:03:35.080539	voucher-2014-01-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2014-10Y-FIN-AC01-0001", "maketime": "2014-01-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 580000.00, "credit_original": 580000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2014-10Y-FIN-AC01-0001	2014-01-31	\N	COMPLETED
95ed1bbd-110f-0908-831f-f943a8609c77	BR-GROUP-2014-10Y-FIN-AC01-0002	BR-GROUP-2014-10Y-FIN-AC01-0002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2014-10Y-FIN-AC01-0002.pdf	2025-12-28 09:03:35.080539	voucher-2014-02-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2014-10Y-FIN-AC01-0002", "maketime": "2014-02-28", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 620000.00, "credit_original": 620000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2014-10Y-FIN-AC01-0002	2014-02-28	\N	COMPLETED
2ece0ee9-a0b3-c907-8cf4-da85fd519232	BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1	BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1.pdf	2025-12-28 09:03:35.080539	bank-2014-q1	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1", "maketime": "2014-03-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1	2014-03-31	\N	COMPLETED
5ce177b3-a6f3-246e-b00d-c6853ad3daca	PENDING-2024-12-001	PENDING-2024-12-001.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/PENDING-2024-12-001.pdf	2025-12-28 09:03:35.080539	pre-2024-12-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "PENDING-2024-12-001", "maketime": "2024-12-22", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 680.00, "credit_original": 680.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	PENDING-2024-12-001	2024-12-22	\N	COMPLETED
2106b9d3-95dc-cb00-f411-ab3974b7519a	PENDING-2024-12-002	PENDING-2024-12-002.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/PENDING-2024-12-002.pdf	2025-12-28 09:03:35.080539	pre-2024-12-002	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "PENDING-2024-12-002", "maketime": "2024-12-23", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 3500.00, "credit_original": 3500.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	PENDING-2024-12-002	2024-12-23	\N	COMPLETED
9eec9816-0dd6-0583-2425-4da6129180ed	PENDING-2024-12-003	PENDING-2024-12-003.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/PENDING-2024-12-003.pdf	2025-12-28 09:03:35.080539	pre-2024-12-003	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "PENDING-2024-12-003", "maketime": "2024-12-24", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 28000.00, "credit_original": 28000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	PENDING-2024-12-003	2024-12-24	\N	COMPLETED
f58480af-4a2f-12e2-0151-9a584a353bf6	BR-GROUP-2025-30Y-FIN-AC01-0201	BR-GROUP-2025-30Y-FIN-AC01-0201.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2025-30Y-FIN-AC01-0201.pdf	2025-12-28 09:03:35.080539	voucher-2025-02-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2025-30Y-FIN-AC01-0201", "maketime": "2025-02-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 125600.00, "credit_original": 125600.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2025-30Y-FIN-AC01-0201	2025-02-18	\N	COMPLETED
dc5596f2-7655-62be-5566-da32c6a8535b	BR-GROUP-2023-30Y-FIN-AC01-0081	BR-GROUP-2023-30Y-FIN-AC01-0081.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0081.pdf	2025-12-28 09:03:35.080539	voucher-2023-08-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0081", "maketime": "2023-08-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 75000.00, "credit_original": 75000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0081	2023-08-15	\N	COMPLETED
9fc3ae0e-dd3c-6db9-8fba-26ffe783d509	BR-GROUP-2023-30Y-FIN-AC01-0071	BR-GROUP-2023-30Y-FIN-AC01-0071.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0071.pdf	2025-12-28 09:03:35.080539	voucher-2023-07-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0071", "maketime": "2023-07-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 42800.00, "credit_original": 42800.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0071	2023-07-31	\N	COMPLETED
04ce6ece-c836-0ce8-6223-0219e3abc70d	BR-GROUP-2023-30Y-FIN-AC01-0061	BR-GROUP-2023-30Y-FIN-AC01-0061.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0061.pdf	2025-12-28 09:03:35.080539	voucher-2023-06-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0061", "maketime": "2023-06-28", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 980000.00, "credit_original": 980000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2023-30Y-FIN-AC01-0061	2023-06-28	\N	COMPLETED
79a4e5b3-9e25-8a67-217b-e8d211281390	BR-GROUP-2022-30Y-FIN-AC01-0111	BR-GROUP-2022-30Y-FIN-AC01-0111.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0111.pdf	2025-12-28 09:03:35.080539	voucher-2022-11-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0111", "maketime": "2022-11-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 35600.00, "credit_original": 35600.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-30Y-FIN-AC01-0111	2022-11-18	\N	COMPLETED
7128f077-23ed-3e23-3861-d08fee1398ac	BR-GROUP-2022-30Y-FIN-AC01-0101	BR-GROUP-2022-30Y-FIN-AC01-0101.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0101.pdf	2025-12-28 09:03:35.080539	voucher-2022-10-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0101", "maketime": "2022-10-22", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 168000.00, "credit_original": 168000.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-30Y-FIN-AC01-0101	2022-10-22	\N	COMPLETED
02bd361a-ef2f-18cb-7fd6-0eba7754c6e2	BR-GROUP-2022-30Y-FIN-AC01-0091	BR-GROUP-2022-30Y-FIN-AC01-0091.pdf	pdf	0	\N	\N	/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0091.pdf	2025-12-28 09:03:35.080539	voucher-2022-09-001	\N	\N	\N	\N	\N	\N	VOUCHER	\N	\N	GENERATED	\N	\N	\N	\N	\N	{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0091", "maketime": "2022-09-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 128500.00, "credit_original": 128500.00, "subjectName": "自动生成科目"}]}	\N	\N	\N	BR-GROUP-2022-30Y-FIN-AC01-0091	2022-09-15	\N	COMPLETED
6090f8648c4c4a3280376cad676f076f	YS-20260115-6090F864	记-1_Voucher.pdf	application/pdf	31167	b7cb3986c771e8d4ed688f6b41d7951c5b3ae7dfa4ecd27043e65f14e9c9e2b8	SM3	/Users/user/nexusarchive/nexusarchive-java/pre-archive/BR-GROUP/记-1_Voucher.pdf	2026-01-15 16:13:36.78584	\N	b7cb3986c771e8d4ed688f6b41d7951c5b3ae7dfa4ecd27043e65f14e9c9e2b8	b7cb3986c771e8d4ed688f6b41d7951c5b3ae7dfa4ecd27043e65f14e9c9e2b8	\N	\N	\N	2020	\N	王心尹	BR-GROUP	用友YonSuite	\N	\N	\N	\N	记-1	{"voucherId":"记-1","voucherNo":"记-1","voucherWord":null,"voucherDate":[2025,8,1],"accountPeriod":"2025-08","accbookCode":null,"summary":null,"debitTotal":150.0,"creditTotal":150.0,"attachmentCount":null,"creator":"王心尹","auditor":"吴倩","poster":null,"status":null,"entries":[{"lineNo":1,"summary":"内部交易出库核算","accountCode":"6401","accountName":"主营业务成本","debit":150.0,"credit":null,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":150.0,"creditOriginal":0.0,"exchangeRate":null},{"lineNo":2,"summary":"内部交易出库核算","accountCode":"1405","accountName":"库存商品","debit":null,"credit":150.0,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":0.0,"creditOriginal":150.0,"exchangeRate":null}],"attachments":null}	\N	\N	单据-记-1	记	2025-08-01	\N	READY_TO_ARCHIVE
80b4c7e896474f9180f4a821c46cedef	YS-20260115-80B4C7E8	记-2_Voucher.pdf	application/pdf	31166	19757d97e444bbed70b65e44164f9628e5f1871236779dc1e98d9aa558473c17	SM3	/Users/user/nexusarchive/nexusarchive-java/pre-archive/BR-GROUP/记-2_Voucher.pdf	2026-01-15 16:13:37.366687	\N	19757d97e444bbed70b65e44164f9628e5f1871236779dc1e98d9aa558473c17	19757d97e444bbed70b65e44164f9628e5f1871236779dc1e98d9aa558473c17	\N	\N	\N	2020	\N	王心尹	BR-GROUP	用友YonSuite	\N	\N	\N	\N	记-2	{"voucherId":"记-2","voucherNo":"记-2","voucherWord":null,"voucherDate":[2025,8,1],"accountPeriod":"2025-08","accbookCode":null,"summary":null,"debitTotal":150.0,"creditTotal":150.0,"attachmentCount":null,"creator":"王心尹","auditor":"吴倩","poster":null,"status":null,"entries":[{"lineNo":1,"summary":"确认应收","accountCode":"112201","accountName":"应收账款_贷款","debit":150.0,"credit":null,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":150.0,"creditOriginal":0.0,"exchangeRate":null},{"lineNo":2,"summary":"应交销项税","accountCode":"2221010601","accountName":"应交税费_应交增值税_销项税额_销项","debit":null,"credit":17.26,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":0.0,"creditOriginal":17.26,"exchangeRate":null},{"lineNo":3,"summary":"收入","accountCode":"600101","accountName":"主营业务收入_贷款","debit":null,"credit":132.74,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":0.0,"creditOriginal":132.74,"exchangeRate":null}],"attachments":null}	\N	\N	单据-记-2	记	2025-08-01	\N	READY_TO_ARCHIVE
b7817092a1294f82b0b8cd3cd346fc1b	YS-20260115-B7817092	记-3_Voucher.pdf	application/pdf	31343	3dbf9eb6b9913ca529c4cd2c182c9e86bfdaa4c82a0e125d4ae824a46b9ad0b8	SM3	/Users/user/nexusarchive/nexusarchive-java/pre-archive/BR-GROUP/记-3_Voucher.pdf	2026-01-15 16:13:37.684099	\N	3dbf9eb6b9913ca529c4cd2c182c9e86bfdaa4c82a0e125d4ae824a46b9ad0b8	3dbf9eb6b9913ca529c4cd2c182c9e86bfdaa4c82a0e125d4ae824a46b9ad0b8	\N	\N	\N	2020	\N	王心尹	BR-GROUP	用友YonSuite	\N	\N	\N	\N	记-3	{"voucherId":"记-3","voucherNo":"记-3","voucherWord":null,"voucherDate":[2025,8,9],"accountPeriod":"2025-08","accbookCode":null,"summary":null,"debitTotal":30.0,"creditTotal":30.0,"attachmentCount":null,"creator":"王心尹","auditor":null,"poster":null,"status":null,"entries":[{"lineNo":1,"summary":"销售出库核算","accountCode":"6401","accountName":"主营业务成本","debit":30.0,"credit":null,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":30.0,"creditOriginal":0.0,"exchangeRate":null},{"lineNo":2,"summary":"销售出库核算","accountCode":"1405","accountName":"库存商品","debit":null,"credit":30.0,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":0.0,"creditOriginal":30.0,"exchangeRate":null}],"attachments":null}	\N	\N	单据-记-3	记	2025-08-09	\N	READY_TO_ARCHIVE
be4a056e6fbf471281c9341d876da5c0	BR-GROUP-2020-30Y-ORG-AC01-000001	记-4_Voucher.pdf	application/pdf	31505	1b931464a080ee0a313227910db06ccf8813a52150aeb0113b985fc1225d5573	SM3	/Users/user/nexusarchive/nexusarchive-java/pre-archive/BR-GROUP/记-4_Voucher.pdf	2026-01-15 16:13:38.208348	\N	1b931464a080ee0a313227910db06ccf8813a52150aeb0113b985fc1225d5573	1b931464a080ee0a313227910db06ccf8813a52150aeb0113b985fc1225d5573	\N	\\x554e5349474e45445f4445565f323032362d30312d31355431363a31373a31312e373135333138	\N	2020	\N	王心尹	BR-GROUP	用友YonSuite	\N	\N	2026-01-15 16:17:11.715368	\N	记-4	{"voucherId":"记-4","voucherNo":"记-4","voucherWord":null,"voucherDate":[2025,8,9],"accountPeriod":"2025-08","accbookCode":null,"summary":null,"debitTotal":50.0,"creditTotal":50.0,"attachmentCount":null,"creator":"王心尹","auditor":null,"poster":null,"status":null,"entries":[{"lineNo":1,"summary":"确认应收","accountCode":"112201","accountName":"应收账款_贷款","debit":50.0,"credit":null,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":50.0,"creditOriginal":0.0,"exchangeRate":null},{"lineNo":2,"summary":"应交销项税","accountCode":"2221010601","accountName":"应交税费_应交增值税_销项税额_销项","debit":null,"credit":5.75,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":0.0,"creditOriginal":5.75,"exchangeRate":null},{"lineNo":3,"summary":"收入","accountCode":"600101","accountName":"主营业务收入_贷款","debit":null,"credit":44.25,"currencyCode":"CNY","currencyName":"人民币","debitOriginal":0.0,"creditOriginal":44.25,"exchangeRate":null}],"attachments":null}	\N	\N	单据-记-4	记	2025-08-09	\N	COMPLETED
file-invoice-002	BR-GROUP-2025-30Y-FIN-AC01-1001	电子发票_吴奕聪餐饮店_657元.pdf	PDF	101657	4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e	SHA-256	uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf	2025-12-28 09:03:35.017527	voucher-2024-11-001	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	{"width": 595.2756, "height": 396.8504, "regions": {"total_amount": {"h": 8.799959, "w": 33.59964, "x": 401.51144, "y": 268.2412}}, "total_amount_value": "650.50"}	PENDING_CHECK
\.


--
-- Data for Name: arc_file_metadata_index; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_file_metadata_index (id, file_id, invoice_code, invoice_number, total_amount, seller_name, issue_date, parsed_time, parser_type) FROM stdin;
2011713367748497409	6090f8648c4c4a3280376cad676f076f	\N	记-1	150.00	\N	2025-08-01	2026-01-15 16:13:37.351096	ERP_SYNC
2011713369052925954	80b4c7e896474f9180f4a821c46cedef	\N	记-2	150.00	\N	2025-08-01	2026-01-15 16:13:37.664637	ERP_SYNC
2011713371267518465	b7817092a1294f82b0b8cd3cd346fc1b	\N	记-3	30.00	\N	2025-08-09	2026-01-15 16:13:38.19413	ERP_SYNC
2011713371691143170	be4a056e6fbf471281c9341d876da5c0	\N	记-4	50.00	\N	2025-08-09	2026-01-15 16:13:38.294835	ERP_SYNC
\.


--
-- Data for Name: arc_import_batch; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_import_batch (id, batch_no, tenant_id, source_system, voucher_type, status, total_files, success_count, failed_count, error_details, rollback_reason, created_by, created_time, completed_time) FROM stdin;
\.


--
-- Data for Name: arc_original_voucher; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) FROM stdin;
ov-demo-inv-004	OV-2025-INV-004	INVOICE	VAT_INVOICE	2025-08-09	50.00	CNY	Test Customer	销售服务费发票	\N	\N	\N	\N	\N	\N	BR01	2025	30Y	DRAFT	\N	1	\N	\N	t	\N	2025-12-28 09:03:35.11446	\N	2026-01-15 10:13:00.912032	0	ENTRY	2	\N	\N	\N	\N	\N	\N	\N	\N	1
ov-demo-bank-004	OV-2025-BANK-004	BANK	BANK_SLIP	2025-08-09	50.00	CNY	Test Customer	服务费收款回单	\N	\N	\N	\N	\N	\N	BR01	2025	30Y	DRAFT	\N	1	\N	\N	t	\N	2025-12-28 09:03:35.11446	\N	2026-01-15 10:13:00.912032	0	ENTRY	2	\N	\N	\N	\N	\N	\N	\N	\N	1
ov-demo-inv-003	OV-2025-INV-003	INVOICE	VAT_INVOICE	2025-08-09	30.00	CNY	Test Customer	商品销售发票	\N	\N	\N	\N	\N	\N	BR01	2025	30Y	DRAFT	\N	1	\N	\N	t	\N	2025-12-28 09:03:35.11446	\N	2026-01-15 10:13:00.912032	0	ENTRY	2	\N	\N	\N	\N	\N	\N	\N	\N	1
ov-test-100-1	OV-2025-TEST-01	INVOICE	VAT_INVOICE	2025-12-22	100.00	CNY	京东办公	办公用品采购	\N	\N	\N	\N	\N	\N	default	2025	30Y	DRAFT	\N	1	\N	\N	t	\N	2025-12-28 09:03:35.118758	\N	2026-01-15 10:13:00.912032	0	ENTRY	2	\N	\N	\N	\N	\N	\N	\N	\N	1
ov-test-100-2	OV-2025-TEST-02	INVOICE	VAT_INVOICE	2025-12-22	100.00	CNY	海底捞餐饮	客户接待	\N	\N	\N	\N	\N	\N	default	2025	30Y	DRAFT	\N	1	\N	\N	t	\N	2025-12-28 09:03:35.118758	\N	2026-01-15 10:13:00.912032	0	ENTRY	2	\N	\N	\N	\N	\N	\N	\N	\N	1
ov-brjt-45k-1	OV-2024-BANK-45K	BANK	BANK_SLIP	2024-03-15	45000.00	CNY	某大客户	服务费收款	\N	\N	\N	\N	\N	\N	BRJT	2024	30Y	DRAFT	\N	1	\N	\N	t	\N	2025-12-28 09:03:35.118758	\N	2026-01-15 10:13:00.912032	0	ENTRY	2	\N	\N	\N	\N	\N	\N	\N	\N	1
\.


--
-- Data for Name: arc_original_voucher_event; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_original_voucher_event (id, voucher_id, from_status, to_status, action, actor_type, actor_id, actor_name, occurred_at, request_id, client_ip, reason, details) FROM stdin;
\.


--
-- Data for Name: arc_original_voucher_file; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) FROM stdin;
file-ov-inv-004	ov-demo-inv-004	invoice_50.pdf	PDF	102400	uploads/demo/invoice_50.pdf	hash_inv_004	SM3	\N	\N	\N	\N	\N	PRIMARY	1	\N	2025-12-28 09:03:35.11446	0
file-ov-bank-004	ov-demo-bank-004	bank_receipt_50.pdf	PDF	102400	uploads/demo/bank_50.pdf	hash_bank_004	SM3	\N	\N	\N	\N	\N	PRIMARY	1	\N	2025-12-28 09:03:35.11446	0
file-ov-inv-003	ov-demo-inv-003	invoice_30.pdf	PDF	102400	uploads/demo/invoice_30.pdf	hash_inv_003	SM3	\N	\N	\N	\N	\N	PRIMARY	1	\N	2025-12-28 09:03:35.11446	0
f-test-100-1	ov-test-100-1	office_supplies_100.pdf	PDF	20480	uploads/demo/office_100.pdf	hash_test_1	SM3	\N	\N	\N	\N	\N	PRIMARY	1	\N	2025-12-28 09:03:35.118758	0
f-test-100-2	ov-test-100-2	food_receipt_100.pdf	PDF	20480	uploads/demo/food_100.pdf	hash_test_2	SM3	\N	\N	\N	\N	\N	PRIMARY	1	\N	2025-12-28 09:03:35.118758	0
f-brjt-45k-1	ov-brjt-45k-1	bank_receipt_45000.pdf	PDF	51200	uploads/demo/bank_45k.pdf	hash_brjt_1	SM3	\N	\N	\N	\N	\N	PRIMARY	1	\N	2025-12-28 09:03:35.118758	0
\.


--
-- Data for Name: arc_original_voucher_sequence; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_original_voucher_sequence (id, fonds_code, fiscal_year, voucher_category, current_seq, last_updated) FROM stdin;
\.


--
-- Data for Name: arc_reconciliation_record; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_reconciliation_record (id, fonds_code, fiscal_year, fiscal_period, subject_code, subject_name, erp_debit_total, erp_credit_total, erp_voucher_count, arc_debit_total, arc_credit_total, arc_voucher_count, attachment_count, attachment_missing_count, recon_status, recon_message, recon_time, operator_id, snapshot_data, source_system, config_id, accbook_code, recon_start_date, recon_end_date) FROM stdin;
\.


--
-- Data for Name: arc_signature_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_signature_log (id, archive_id, file_id, signer_name, signer_cert_sn, signer_org, sign_time, sign_algorithm, signature_value, verify_result, verify_time, verify_message, created_time) FROM stdin;
\.


--
-- Data for Name: arc_voucher_relation; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.arc_voucher_relation (id, original_voucher_id, accounting_voucher_id, relation_type, relation_desc, created_by, created_time, deleted) FROM stdin;
\.


--
-- Data for Name: archive_amendment; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.archive_amendment (id, archive_id, batch_id, amendment_type, reason, original_content, amended_content, attachment_ids, status, approved_by, approved_at, approval_comment, created_by, created_at) FROM stdin;
\.


--
-- Data for Name: archive_batch; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.archive_batch (id, batch_no, fonds_id, period_start, period_end, scope_type, status, voucher_count, doc_count, file_count, total_size_bytes, validation_report, integrity_report, error_message, submitted_by, submitted_at, approved_by, approved_at, approval_comment, archived_at, archived_by, created_by, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: archive_batch_item; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.archive_batch_item (id, batch_id, item_type, ref_id, ref_no, status, validation_result, hash_sm3, created_at) FROM stdin;
\.


--
-- Data for Name: audit_inspection_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, hash_snapshot, integrity_check, authenticity_check, availability_check, security_check, check_result, detail_report, created_at, report_file_path, report_file_hash, is_compliant, compliance_violations, compliance_warnings, created_time) FROM stdin;
inspection-v2024-11-001	voucher-2024-11-001	ARCHIVE	2025-12-28 09:03:35.017527	user-zhangsan	t	t	t	t	4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e	\N	\N	\N	\N	PASS	\N	2025-12-28 09:03:35.017527	\N	\N	\N	\N	\N	2026-01-14 18:54:06.830786
inspection-v2024-11-002	voucher-2024-11-002	ARCHIVE	2025-12-28 09:03:35.017527	user-zhangsan	t	t	t	t	4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02	\N	\N	\N	\N	PASS	\N	2025-12-28 09:03:35.017527	\N	\N	\N	\N	\N	2026-01-14 18:54:06.830786
inspection-v2024-11-003	voucher-2024-11-003	ARCHIVE	2025-12-28 09:03:35.017527	user-lisi	t	t	t	t	b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e	\N	\N	\N	\N	PASS	\N	2025-12-28 09:03:35.017527	\N	\N	\N	\N	\N	2026-01-14 18:54:06.830786
\.


--
-- Data for Name: auth_ticket; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.auth_ticket (id, applicant_id, applicant_name, source_fonds, target_fonds, scope, expires_at, status, approval_snapshot, reason, created_at, last_modified_time, deleted) FROM stdin;
\.


--
-- Data for Name: bas_erp_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bas_erp_config (id, name, adapter_type, base_url, app_key, app_secret, tenant_id, accbook_code, extra_config, enabled, created_by, created_time, last_modified_time) FROM stdin;
\.


--
-- Data for Name: bas_fonds; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id, entity_id) FROM stdin;
fonds-brjt	BRJT	Boran Joint Venture	\N	\N	\N	2026-01-14 10:57:48.151442	2026-01-14 10:57:48.151442	\N	\N
fonds-comp001	COMP001	总公司档案全宗	总公司	总公司电子会计档案全宗	system	2026-01-14 11:03:18.206718	2026-01-14 11:03:18.206718	\N	\N
fonds-br-group	BR-GROUP	泊冉集团有限公司	泊冉集团有限公司	集团总部档案全宗	system	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	\N	ORG_BR_GROUP
fonds-br-sales	BR-SALES	泊冉销售有限公司	泊冉销售有限公司	销售公司档案全宗	system	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	\N	ORG_BR_SALES
fonds-br-trade	BR-TRADE	泊冉国际贸易有限公司	泊冉国际贸易有限公司	贸易公司档案全宗	system	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	\N	ORG_BR_TRADE
fonds-br-mfg	BR-MFG	泊冉制造有限公司	泊冉制造有限公司	制造公司档案全宗	system	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	\N	ORG_BR_MFG
demo-fonds-001	DEMO	演示全宗	演示公司	系统初始演示数据	system	2025-12-28 09:03:34.854164	2025-12-28 09:03:34.854164	\N	ORG_BR_GROUP
\.


--
-- Data for Name: bas_location; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted, created_time, last_modified_time) FROM stdin;
loc-warehouse-main	主档案库房	W-MAIN	WAREHOUSE	0	/主档案库房	10000	2850	NORMAL	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
loc-area-a	A区-会计凭证区	A-VOUCHER	AREA	loc-warehouse-main	/主档案库房/A区-会计凭证区	3000	1580	NORMAL	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
loc-area-b	B区-财务报告区	B-REPORT	AREA	loc-warehouse-main	/主档案库房/B区-财务报告区	2000	680	NORMAL	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
loc-shelf-a1	A1号架	A1	SHELF	loc-area-a	/主档案库房/A区-会计凭证区/A1号架	500	486	NORMAL	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
loc-shelf-a2	A2号架	A2	SHELF	loc-area-a	/主档案库房/A区-会计凭证区/A2号架	500	320	NORMAL	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
\.


--
-- Data for Name: biz_appraisal_list; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.biz_appraisal_list (id, fonds_no, archive_year, appraiser_id, appraiser_name, appraisal_date, archive_ids, archive_snapshot, status, created_time, last_modified_time, deleted) FROM stdin;
\.


--
-- Data for Name: biz_archive_approval; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, approver_id, approver_name, status, approval_comment, approval_time, created_at, last_modified_time, deleted, org_name, created_time) FROM stdin;
test-approval-7e32cc3cf48cfa8324793e70d11fc3ce	demo-reimb-fp-002	FP-2025-01-002	酒店住宿费发票-北京希尔顿酒店	user-zhangsan	张三	测试待审批	user_admin_001	admin	APPROVED	批量批准	2026-01-15 16:04:50.823773	2026-01-15 08:02:50.883348	2026-01-15 16:04:50.823786	0	泊冉集团有限公司	2026-01-15 08:02:50.883348
2011714158295101441	be4a056e6fbf471281c9341d876da5c0	BR-GROUP-2020-30Y-ORG-AC01-000001	记-4_Voucher	user_admin_001	admin	批量归档申请	user_admin_001	admin	APPROVED	批量批准	2026-01-15 16:17:11.674979	2026-01-15 16:16:45.786248	2026-01-15 16:17:11.674991	0	用友 YonSuite (生产环境)	2026-01-15 16:16:45.83024
\.


--
-- Data for Name: biz_borrowing; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, last_modified_time, deleted, fonds_no, archive_year, type, return_deadline, actual_return_time, updated_at) FROM stdin;
borrow-003	user-zhaoliu	赵六	other-contract-2024-002	办公楼租赁合同	核对租金支付条款	2024-12-18	2024-12-22	\N	PENDING	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	BR-GROUP	2024	electronic	2024-12-22	\N	2026-01-14 18:54:07.428396
borrow-002	user-lisi	李四	report-2023-annual	2023年度财务决算报告	编制2024年预算参考	2024-11-20	2024-11-30	2024-11-28	RETURNED	已按期归还	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	BR-GROUP	2023	electronic	2024-11-30	2024-11-28	2026-01-14 18:54:07.428396
borrow-004	user-wangwu	王五	voucher-2023-10-001	采购生产设备	专项审计-固定资产核查	2024-10-10	2024-10-20	2024-10-18	RETURNED	已完成审计	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	BR-GROUP	2023	electronic	2024-10-20	2024-10-18	2026-01-14 18:54:07.428396
borrow-001	user-wangwu	王五	voucher-2024-11-008	销售商品确认收入	年度审计核查销售收入确认	2024-12-15	2024-12-25	\N	OVERDUE	审批通过，请于期限内归还	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	BR-GROUP	2024	electronic	2024-12-25	\N	2026-01-14 18:54:07.428396
borrow-005	user-zhangsan	张三	ledger-2024-002	2024年度银行存款明细账	月末对账核实	2024-12-20	2024-12-23	\N	OVERDUE	同意借阅	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	BR-GROUP	2024	electronic	2024-12-23	\N	2026-01-14 18:54:07.428396
\.


--
-- Data for Name: biz_destruction; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, approver_id, approver_name, approval_comment, approval_time, execution_time, created_at, last_modified_time, deleted, created_time, appraiser_id, appraiser_name, appraisal_date, appraisal_conclusion, appraisal_comment, appraisal_list_id, approval_snapshot) FROM stdin;
destruction-2024-001	user-qianqi	钱七	保管期限已满10年，经鉴定无继续保存价值	3	["voucher-2014-01-001","voucher-2014-02-001","bank-2014-q1"]	PENDING	\N	\N	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0	2026-01-14 18:54:06.830786	\N	\N	\N	\N	\N	\N	\N
\.


--
-- Data for Name: biz_open_appraisal; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.biz_open_appraisal (id, archive_id, archive_code, archive_title, retention_period, current_security_level, appraiser_id, appraiser_name, appraisal_date, appraisal_result, open_level, reason, status, created_at, last_modified_time, deleted, created_time) FROM stdin;
\.


--
-- Data for Name: cfg_account_role_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cfg_account_role_mapping (id, company_id, account_code, aux_type, account_role, source, created_time, updated_time) FROM stdin;
\.


--
-- Data for Name: cfg_account_role_preset; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) FROM stdin;
1	KIT_GENERAL	^1001.*	CASH	100	2025-12-28 09:03:35.090742
2	KIT_GENERAL	^1002.*	BANK	100	2025-12-28 09:03:35.090742
3	KIT_GENERAL	^1122.*	RECEIVABLE	100	2025-12-28 09:03:35.090742
4	KIT_GENERAL	^1123.*	RECEIVABLE	90	2025-12-28 09:03:35.090742
5	KIT_GENERAL	^2202.*	PAYABLE	100	2025-12-28 09:03:35.090742
6	KIT_GENERAL	^2203.*	PAYABLE	90	2025-12-28 09:03:35.090742
7	KIT_GENERAL	^2211.*	SALARY	100	2025-12-28 09:03:35.090742
8	KIT_GENERAL	^2221.*	TAX	100	2025-12-28 09:03:35.090742
9	KIT_GENERAL	^1601.*	ASSET	100	2025-12-28 09:03:35.090742
10	KIT_GENERAL	^1602.*	ASSET	90	2025-12-28 09:03:35.090742
11	KIT_GENERAL	^6601.*	EXPENSE	100	2025-12-28 09:03:35.090742
12	KIT_GENERAL	^6602.*	EXPENSE	100	2025-12-28 09:03:35.090742
13	KIT_GENERAL	^6603.*	EXPENSE	100	2025-12-28 09:03:35.090742
14	KIT_GENERAL	^6001.*	REVENUE	100	2025-12-28 09:03:35.090742
15	KIT_GENERAL	^6051.*	REVENUE	90	2025-12-28 09:03:35.090742
\.


--
-- Data for Name: cfg_doc_type_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cfg_doc_type_mapping (id, company_id, customer_doc_type, evidence_role, display_name, source, created_time, updated_time) FROM stdin;
1	1	VAT_INVOICE	INVOICE	增值税专用发票	PRESET	2025-12-28 09:03:35.11446	2025-12-28 09:03:35.11446
2	1	BANK_SLIP	BANK_RECEIPT	银行电子回单	PRESET	2025-12-28 09:03:35.11446	2025-12-28 09:03:35.11446
3	1	SALES_ORDER	CONTRACT	销售订单	PRESET	2025-12-28 09:03:35.11446	2025-12-28 09:03:35.11446
\.


--
-- Data for Name: cfg_doc_type_preset; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) FROM stdin;
1	KIT_GENERAL	付款.*	{付款审批,付款申请,付款指令}	AUTHORIZATION	100	2025-12-28 09:03:35.090742
2	KIT_GENERAL	银行.*|回单.*	{银行回单,转账回单,支付凭证,网银回单}	SETTLEMENT	100	2025-12-28 09:03:35.090742
3	KIT_GENERAL	发票.*|增值税.*	{发票,增值税专用发票,增值税普通发票}	TAX_EVIDENCE	100	2025-12-28 09:03:35.090742
4	KIT_GENERAL	合同.*|协议.*|订单.*	{合同,协议,订单,框架协议}	CONTRACTUAL_BASIS	100	2025-12-28 09:03:35.090742
5	KIT_GENERAL	入库.*|验收.*|签收.*	{入库单,验收单,签收单,收货单}	EXECUTION_PROOF	100	2025-12-28 09:03:35.090742
6	KIT_GENERAL	报销.*|费用.*	{报销单,费用申请,差旅报销}	ACCOUNTING_TRIGGER	100	2025-12-28 09:03:35.090742
\.


--
-- Data for Name: cfg_preset_kit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cfg_preset_kit (id, industry, name, description, is_default, created_time) FROM stdin;
KIT_GENERAL	GENERAL	通用行业预置包	适用于大多数企业的默认规则	t	2025-12-28 09:03:35.090742
\.


--
-- Data for Name: collection_batch; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collection_batch (id, batch_no, batch_name, fonds_id, fonds_code, fiscal_year, fiscal_period, archival_category, source_channel, status, total_files, uploaded_files, failed_files, total_size_bytes, validation_report, error_message, created_by, created_time, last_modified_time, completed_time) FROM stdin;
\.


--
-- Data for Name: collection_batch_file; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.collection_batch_file (id, batch_id, file_id, original_filename, file_size_bytes, file_type, file_hash, hash_algorithm, upload_status, processing_result, error_message, upload_order, started_time, completed_time, created_time, archive_id) FROM stdin;
\.


--
-- Data for Name: destruction_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.destruction_log (id, fonds_no, archive_year, archive_object_id, retention_policy_id, approval_ticket_id, destroyed_by, destroyed_at, trace_id, snapshot, prev_hash, curr_hash, sig, created_at) FROM stdin;
\.


--
-- Data for Name: employee_lifecycle_event; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.employee_lifecycle_event (id, employee_id, employee_name, event_type, event_date, previous_dept_id, new_dept_id, previous_role_ids, new_role_ids, reason, processed, processed_at, processed_by, created_at, deleted) FROM stdin;
\.


--
-- Data for Name: file_hash_dedup_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.file_hash_dedup_scope (id, fonds_no, scope_type, enabled, created_by, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: file_storage_policy; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.file_storage_policy (id, fonds_no, policy_type, retention_days, immutable_until, enabled, created_by, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
54	103	fix fonds entity mapping	SQL	V103__fix_fonds_entity_mapping.sql	1714086925	postgres	2026-01-15 15:47:15.404451	27	t
2	2	add missing columns	SQL	V2__add_missing_columns.sql	-798336013	postgres	2026-01-14 18:54:06.722343	19	t
3	3	fix audit log schema	SQL	V3__fix_audit_log_schema.sql	-985710024	postgres	2026-01-14 18:54:06.755148	11	t
4	67	standardize schema columns	SQL	V67__standardize_schema_columns.sql	-1707661208	postgres	2026-01-14 18:54:06.775807	41	t
5	68	fix missing schema columns	SQL	V68__fix_missing_schema_columns.sql	286116620	postgres	2026-01-14 18:54:06.827791	17	t
6	69	add sys sql audit rule	SQL	V69__add_sys_sql_audit_rule.sql	1352387501	postgres	2026-01-14 18:54:06.851767	7	t
7	70	add search indexes	SQL	V70__add_search_indexes.sql	-652212201	postgres	2026-01-14 18:54:06.865925	10	t
8	71	add destruction fields	SQL	V71__add_destruction_fields.sql	-1377787923	postgres	2026-01-14 18:54:06.884598	11	t
9	72	add appraisal fields	SQL	V72__add_appraisal_fields.sql	233446898	postgres	2026-01-14 18:54:06.906365	28	t
10	73	add approval snapshot	SQL	V73__add_approval_snapshot.sql	1167917237	postgres	2026-01-14 18:54:06.943758	5	t
11	74	create destruction log	SQL	V74__create_destruction_log.sql	976604539	postgres	2026-01-14 18:54:06.97266	17	t
12	75	create auth ticket	SQL	V75__create_auth_ticket.sql	1522214421	postgres	2026-01-14 18:54:06.998379	18	t
13	76	create fonds history	SQL	V76__create_fonds_history.sql	382821621	postgres	2026-01-14 18:54:07.023968	14	t
14	77	create user lifecycle tables	SQL	V77__create_user_lifecycle_tables.sql	1132729737	postgres	2026-01-14 18:54:07.046891	33	t
15	78	create performance metrics tables	SQL	V78__create_performance_metrics_tables.sql	573979235	postgres	2026-01-14 18:54:07.091819	20	t
16	79	fonds audit borrow alignment	SQL	V79__fonds_audit_borrow_alignment.sql	1987053717	postgres	2026-01-14 18:54:07.12001	24	t
17	80	create sys entity	SQL	V80__create_sys_entity.sql	-1130719194	postgres	2026-01-14 18:54:07.153522	17	t
18	81	create entity config	SQL	V81__create_entity_config.sql	-425637917	postgres	2026-01-14 18:54:07.180361	14	t
19	82	create legacy import task	SQL	V82__create_legacy_import_task.sql	-592376543	postgres	2026-01-14 18:54:07.201973	13	t
20	83	create jsonb indexes	SQL	V83__create_jsonb_indexes.sql	1039563323	postgres	2026-01-14 18:54:07.221793	18	t
21	84	fix user fonds scope for existing data	SQL	V84__fix_user_fonds_scope_for_existing_data.sql	-299036141	postgres	2026-01-14 18:54:07.246948	3	t
22	85	create sys position table	SQL	V85__create_sys_position_table.sql	1240342243	postgres	2026-01-14 18:54:07.258904	12	t
23	86	create erp adapter tables	SQL	V86__create_erp_adapter_tables.sql	-112890577	postgres	2026-01-14 18:54:07.278318	15	t
24	87	create collection batch	SQL	V87__create_collection_batch.sql	1796347566	postgres	2026-01-14 18:54:07.29925	22	t
25	88	create collection batch file	SQL	V88__create_collection_batch_file.sql	1720789885	postgres	2026-01-14 18:54:07.337972	24	t
26	89	add batch file archive id	SQL	V89__add_batch_file_archive_id.sql	214086561	postgres	2026-01-14 18:54:07.367261	7	t
27	90	create borrow request	SQL	V90__create_borrow_request.sql	-2098053014	postgres	2026-01-14 18:54:07.383632	10	t
28	91	create borrow archive	SQL	V91__create_borrow_archive.sql	627687196	postgres	2026-01-14 18:54:07.399365	6	t
29	92	create borrow log	SQL	V92__create_borrow_log.sql	-177014607	postgres	2026-01-14 18:54:07.412254	7	t
30	93	fix borrowing updated at	SQL	V93__fix_borrowing_updated_at.sql	-1633959246	postgres	2026-01-14 18:54:07.425622	2	t
31	94	create scan workspace table	SQL	V94__create_scan_workspace_table.sql	-1633318414	postgres	2026-01-14 18:54:07.432937	31	t
32	95	add accbook mapping to erp config	SQL	V95__add_accbook_mapping_to_erp_config.sql	-1553688774	postgres	2026-01-14 18:54:07.469148	3	t
33	97	simplify pre archive status	SQL	V97__simplify_pre_archive_status.sql	407404454	postgres	2026-01-14 18:54:07.479199	15	t
34	98	add sap interface type to erp config	SQL	V98__add_sap_interface_type_to_erp_config.sql	-390394453	postgres	2026-01-14 18:54:07.501007	3	t
35	101	sys entity add parent id	SQL	V101__sys_entity_add_parent_id.sql	157241701	postgres	2026-01-14 18:56:36.306818	62	t
36	20260110	create query user role	SQL	V20260110__create_query_user_role.sql	-6486005	postgres	2026-01-14 18:56:36.470285	14	t
37	20260111	create three admin roles	SQL	V20260111__create_three_admin_roles.sql	-768816411	postgres	2026-01-14 18:56:36.509454	10	t
38	20260113	fix acc borrow log schema	SQL	V20260113__fix_acc_borrow_log_schema.sql	1663112510	postgres	2026-01-14 18:56:36.541544	33	t
39	2026010701	relax audit log ip constraint	SQL	V2026010701__relax_audit_log_ip_constraint.sql	1622556769	postgres	2026-01-14 18:56:36.598494	12	t
40	2026010702	allow null session id	SQL	V2026010702__allow_null_session_id.sql	-856272891	postgres	2026-01-14 18:56:36.622415	5	t
41	2026010703	add fonds code to abnormal voucher	SQL	V2026010703__add_fonds_code_to_abnormal_voucher.sql	-1222196534	postgres	2026-01-14 18:56:36.63701	14	t
42	2026010704	add fonds code to scan workspace	SQL	V2026010704__add_fonds_code_to_scan_workspace.sql	-993526351	postgres	2026-01-14 18:56:36.66013	7	t
43	2026010705	add scan permissions	SQL	V2026010705__add_scan_permissions.sql	-1498871196	postgres	2026-01-14 18:56:36.684734	32	t
44	2026010706	add fonds no to ingest request	SQL	V2026010706__add_fonds_no_to_ingest_request.sql	1172797482	postgres	2026-01-14 18:56:36.814731	9	t
45	2026010707	fix sys user fonds scope unique	SQL	V2026010707__fix_sys_user_fonds_scope_unique.sql	775610654	postgres	2026-01-14 18:56:36.835927	5	t
46	2026010708	add performance indexes	SQL	V2026010708__add_performance_indexes.sql	505511715	postgres	2026-01-14 18:56:36.850202	30	t
47	2026010901	enhance jsonb expression indexes	SQL	V2026010901__enhance_jsonb_expression_indexes.sql	-1961360071	postgres	2026-01-14 18:56:36.892487	44	t
48	2026010902	create sync task table	SQL	V2026010902__create_sync_task_table.sql	-1209096693	postgres	2026-01-14 18:56:36.949514	29	t
49	2026010903	create business user role	SQL	V2026010903__create_business_user_role.sql	-1825989807	postgres	2026-01-14 18:56:36.986938	6	t
50	2026010904	fix existing users roles	SQL	V2026010904__fix_existing_users_roles.sql	-1855712192	postgres	2026-01-14 18:56:37.009757	7	t
51	2026010905	fix business user permissions	SQL	V2026010905__fix_business_user_permissions.sql	636436	postgres	2026-01-14 18:56:37.029131	3	t
52	99	add missing permissions	SQL	V99__add_missing_permissions.sql	-983731953	postgres	2026-01-15 13:59:43.171471	9	t
53	102	seed relationship demo data	SQL	V102__seed_relationship_demo_data.sql	-1700364708	postgres	2026-01-15 13:59:43.233672	34	t
1	1	init baseline 2025	SQL	V1__init_baseline_2025.sql	-1567004454	postgres	2026-01-14 18:54:05.583067	999	t
\.


--
-- Data for Name: fonds_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fonds_history (id, fonds_no, event_type, from_fonds_no, to_fonds_no, effective_date, reason, approval_ticket_id, snapshot_json, created_by, created_at, deleted) FROM stdin;
\.


--
-- Data for Name: integrity_check; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.integrity_check (id, target_type, target_id, check_type, result, hash_expected, hash_actual, signature_valid, details, checked_at, checked_by) FROM stdin;
\.


--
-- Data for Name: legacy_import_task; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.legacy_import_task (id, operator_id, operator_name, fonds_no, file_name, file_size, file_hash, total_rows, success_rows, failed_rows, status, error_report_path, created_fonds_nos, created_entity_ids, started_at, completed_at, created_at) FROM stdin;
\.


--
-- Data for Name: match_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.match_log (id, match_batch_id, voucher_id, action, evidence_role, source_doc_id, score, reasons, before_state, after_state, is_manual_override, operator_id, operator_name, client_ip, operation_time) FROM stdin;
\.


--
-- Data for Name: match_rule_template; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.match_rule_template (id, name, version, status, scene, config, description, created_time, updated_time, updated_by) FROM stdin;
T01_PAYMENT	付款业务关联规则	v1.0	ACTIVE	PAYMENT	{"mustLink": [{"strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票", "增值税"]}, {"strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"], "evidenceRole": "SETTLEMENT", "docTypeKeywords": ["银行回单", "付款凭证"]}], "shouldLink": [{"strategies": ["FUZZY_NAME", "REF_NO"], "evidenceRole": "CONTRACTUAL_BASIS", "docTypeKeywords": ["合同", "协议"]}]}	适用于标准对公付款业务，需匹配发票和银行回单	2025-12-28 09:03:35.110308	2025-12-28 09:03:35.110308	\N
T02_RECEIPT	收款业务关联规则	v1.0	ACTIVE	RECEIPT	{"mustLink": [{"strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"], "evidenceRole": "SETTLEMENT", "docTypeKeywords": ["银行回单", "收款凭证"]}], "shouldLink": [{"strategies": ["AMOUNT_EXACT", "FUZZY_NAME"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票", "销售发票"]}]}	适用于标准销售收款业务	2025-12-28 09:03:35.110308	2025-12-28 09:03:35.110308	\N
T03_EXPENSE	费用报销关联规则	v1.0	ACTIVE	EXPENSE	{"mayLink": [{"strategies": ["AMOUNT_EXACT"], "evidenceRole": "SETTLEMENT", "docTypeKeywords": ["支付凭证"]}], "mustLink": [{"strategies": ["AMOUNT_EXACT", "REF_NO"], "evidenceRole": "ACCOUNTING_TRIGGER", "docTypeKeywords": ["报销单", "费用申请"]}, {"strategies": ["DATE_PROXIMITY"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票", "行程单"]}]}	适用于员工费用报销业务	2025-12-28 09:03:35.110308	2025-12-28 09:03:35.110308	\N
T04_PURCHASE	采购供应链关联规则	v1.0	ACTIVE	PURCHASE_IN	{"mustLink": [{"strategies": ["REF_NO", "FUZZY_NAME"], "evidenceRole": "CONTRACTUAL_BASIS", "docTypeKeywords": ["采购订单", "合同"]}, {"strategies": ["DATE_PROXIMITY", "REF_NO"], "evidenceRole": "EXECUTION_PROOF", "docTypeKeywords": ["入库单", "验收单"]}], "shouldLink": [{"strategies": ["AMOUNT_EXACT"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票"]}]}	适用于原材料或商品采购入库	2025-12-28 09:03:35.110308	2025-12-28 09:03:35.110308	\N
T05_SALES	销售供应链关联规则	v1.0	ACTIVE	SALES_OUT	{"mustLink": [{"strategies": ["REF_NO", "FUZZY_NAME"], "evidenceRole": "CONTRACTUAL_BASIS", "docTypeKeywords": ["销售订单"]}, {"strategies": ["DATE_PROXIMITY", "REF_NO"], "evidenceRole": "EXECUTION_PROOF", "docTypeKeywords": ["出库单", "发货单"]}], "shouldLink": [{"strategies": ["AMOUNT_EXACT"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票"]}]}	适用于商品销售出库	2025-12-28 09:03:35.110308	2025-12-28 09:03:35.110308	\N
\.


--
-- Data for Name: period_lock; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.period_lock (id, fonds_id, period, lock_type, locked_at, locked_by, unlock_at, unlock_by, reason) FROM stdin;
\.


--
-- Data for Name: scan_folder_monitor; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.scan_folder_monitor (id, user_id, folder_path, is_active, file_filter, auto_delete, move_to_path, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: scan_workspace; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.scan_workspace (id, session_id, user_id, file_name, file_path, file_size, file_type, upload_source, ocr_status, ocr_engine, ocr_result, overall_score, doc_type, submit_status, archive_id, submitted_at, created_at, updated_at, fonds_code) FROM stdin;
\.


--
-- Data for Name: search_performance_stats; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.search_performance_stats (id, fonds_no, search_type, search_duration_ms, result_count, user_id, recorded_at, created_at) FROM stdin;
\.


--
-- Data for Name: storage_capacity_stats; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.storage_capacity_stats (id, fonds_no, total_size_gb, used_size_gb, file_count, recorded_at, created_at) FROM stdin;
\.


--
-- Data for Name: sys_archival_code_sequence; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_archival_code_sequence (fonds_code, fiscal_year, category_code, current_val, updated_time) FROM stdin;
BR-GROUP	2020	AC01	1	2026-01-15 16:16:45.80339
\.


--
-- Data for Name: sys_audit_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, client_ip, mac_address, object_digest, user_agent, prev_log_hash, log_hash, device_fingerprint, created_time, ip_address, created_at, trace_id, data_snapshot, source_fonds, target_fonds, auth_ticket_id) FROM stdin;
auditlog-001	user-zhangsan	zhangsan	\N	LOGIN	USER	user-zhangsan	SUCCESS	\N	用户登录系统	\N	\N	\N	192.168.1.100	UNKNOWN	\N	\N	\N	\N	\N	2025-12-28 07:03:35.017527	\N	2026-01-14 18:54:06.830786	\N	\N	\N	\N	\N
auditlog-002	user-lisi	lisi	\N	VIEW	ARCHIVE	voucher-2024-11-008	SUCCESS	\N	查看凭证详情：销售商品确认收入	\N	\N	\N	192.168.1.102	UNKNOWN	\N	\N	\N	\N	\N	2025-12-28 08:03:35.017527	\N	2026-01-14 18:54:06.830786	\N	\N	\N	\N	\N
auditlog-003	user-wangwu	wangwu	\N	CREATE	BORROWING	borrow-001	SUCCESS	\N	提交借阅申请：年度审计核查销售收入确认	\N	\N	\N	192.168.1.105	UNKNOWN	\N	\N	\N	\N	\N	2025-12-28 08:33:35.017527	\N	2026-01-14 18:54:06.830786	\N	\N	\N	\N	\N
673da58dfbcb460faec0487449453a3c	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	MEDIUM	用户登录	{"username":"zhangsan","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyLXpoYW5nc2FuIiwidXNlcm5hbWUiOiJ6aGFuZ3NhbiIsInN1YiI6InVzZXItemhhbmdzYW4iLCJpYXQiOjE3NjY4ODM5OTksImV4cCI6MTc2Njg4NzU5OX0.fnsoS2eS4qS4f9DoD3PuGGgdQoQCX5s3GXbcfYWIuywTQQw5RMMNn8b1fRbLjFJfsgqCjotJDAI7AEsHARg7E0RxTJxmqVvSNLPXymA2ABp5VkNu4EZOpuP73a6ymDC9bQssF5z8HlD9u8jFaoBkWZg3lGHtxtfOkoQCUufZqQpZHI21_-dkadaEp2T9mLVX_AC29SKWgY31D75nwdNyGFUHqRpR5wOjp0X7ZW95ZdKz2QIGNR_rEQxFJX1uLv93gx_DkSBiAtpa3Ko4PvA2sExXkj5jovLr6nqXum2W1LtcHcHw6tsKUaTvyuH22ExK5xk1untzpSXeovzp6-U0Ng","user":{"id":"user-zhangsan","username":"zhangsan","fullName":"张三","email":"zhangsan@boran.com","avatar":null,"departmentId":"ORG_BR_GROUP_FIN","status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","archive:view","nav:archive_mgmt","archive:manage","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","archive:read","audit:view","nav:pre_archive"]}},"timestamp":1766883999411}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1	\N	\N	Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-28 09:06:39.415193	\N	2026-01-14 18:54:06.830786	\N	\N	\N	\N	\N
1d379c10-ccac-4bb6-86b0-848994007a35	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 泊冉集团有限公司	sys_org:泊冉集团有限公司	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
f74dcb18-8a79-42db-8653-44da7d21e06c	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 泊冉销售有限公司	sys_org:泊冉销售有限公司	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
e629fea7-0e9a-4614-9ae2-bf6ea875a900	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 泊冉国际贸易有限公司	sys_org:泊冉国际贸易有限公司	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
d167ee79-f4b9-4c00-9e14-4fba8d1d80c2	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 泊冉制造有限公司	sys_org:泊冉制造有限公司	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
73f655df-cb6b-4386-898b-7a4bda20f47e	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 财务管理部	sys_org:财务管理部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
e655d67d-6f9e-4fb2-aae4-fffc18aac895	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 人力资源部	sys_org:人力资源部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
16020177-fd85-464f-b8e1-90b9d0df72da	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 信息技术部	sys_org:信息技术部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
a7b43013-3078-4806-ae6a-88cc988c74ff	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 法务合规部	sys_org:法务合规部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
ee0acb38-d53d-4314-8fce-2b836503c9de	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 审计监察部	sys_org:审计监察部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
2b81b342-4789-4424-92bc-ada3a589514d	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 国内销售部	sys_org:国内销售部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
a0f9efdb-a22a-4b30-8da3-4d1a31911acb	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 海外销售部	sys_org:海外销售部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
4031b5f0-d008-458a-8174-e5672c9be190	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 市场推广部	sys_org:市场推广部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
457d538b-59e3-45f3-b1ff-07a43125e5fb	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 财务部	sys_org:财务部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
ce727812-a419-4b98-849d-1062cde1f33a	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 进口业务部	sys_org:进口业务部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
25abc0df-fea7-45ee-b43f-b42bdd09a36e	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 出口业务部	sys_org:出口业务部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
658dc510-1c58-4a0b-9a52-f05901bea467	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 物流仓储部	sys_org:物流仓储部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
765a0ae5-45d9-4331-a21f-dda7b1c1d75a	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 财务部	sys_org:财务部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
dd69e825-a0de-4de2-bbe0-930a1b53b08d	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 生产管理部	sys_org:生产管理部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
27933ca4-2273-4792-92e8-4595d8bff9a8	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 质量控制部	sys_org:质量控制部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
67420226-2263-45d9-aa2d-74494b0218e0	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 研发技术部	sys_org:研发技术部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
ce2ce63c-8618-4050-97dd-e45afeff9116	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 采购供应部	sys_org:采购供应部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
472ac7c1-ffeb-44fe-a2d6-7f57a89d92f7	SYSTEM	SYSTEM	\N	MIGRATION	sys_entity	\N	SUCCESS	LOW	Migrated from sys_org: 财务部	sys_org:财务部	Migrated to sys_entity	\N	127.0.0.1	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:56:36.397194	\N	2026-01-14 18:56:36.397194	\N	\N	\N	\N	\N
19dba5fb487541d58ff2c8d37809c16f	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:57:02.516166	\N	2026-01-14 18:57:02.512459	\N	\N	\N	\N	\N
448bd8a1d01741a5ac800e9a4dc2c2f1	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-003	SUCCESS	low	查询档案文件内容: fileId=file-invoice-003	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:57:06.4552	\N	2026-01-14 18:57:06.453579	\N	\N	\N	\N	\N
ea227d0de00f40d6bc9c83fd44027fa0	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:57:09.943898	\N	2026-01-14 18:57:09.941296	\N	\N	\N	\N	\N
22a8a281ff4542a8ad7def75aa981971	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 18:57:22.624199	\N	2026-01-14 18:57:22.623219	\N	\N	\N	\N	\N
1f54e1323dda4788bef3f1ee379fc567	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	MEDIUM	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1768388424171}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-14 19:00:24.183747	0:0:0:0:0:0:0:1	2026-01-14 19:00:24.179035	\N	\N	\N	\N	\N
70948cefb02645f585452fbe0bfa72d7	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	MEDIUM	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJ1c2VyX2FkbWluXzAwMSIsImlhdCI6MTc2ODM4ODQyOCwiZXhwIjoxNzY4NDc0ODI4fQ.MOZ4jljVKfirAUyhmH5exh6F5hAyT4PTCPbANeD0ENpzOeXb4erps2W1bj8KJ5pOgrlnDxWRZYd3LvNtesq2gZaNsGFBuhjjUVCtM6QeU0jhHpDMyIeYHpQb_qMXLe-5iBKjAOij80nzU6s8uLvhIE_KqTcRYEvz8MI75S8wHYDGN3_76jsjGiwjkfWxvMXsA_blYq_TczWAW7sY0rgK8JqRi16uJfDFEgVnv0zLtWUwfz0SXM1Cw-b9c10WnnBvQmbU8cz5G-iyPBYnIUtfSW2O9_rQo-wn2v1VuNmH7PT-ioiXPsKskVCT5LIAQHmyS5o7kRIkTXOtr5HmcgLW6A","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","scan:manage","archive:view","nav:archive_mgmt","archive:manage","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","archive:read","audit:view","nav:pre_archive"],"phone":null,"employeeId":null,"jobTitle":null,"orgCode":"BR-GROUP","lastLoginAt":[2026,1,14,19,0,28,887587000],"createdTime":[2025,12,28,9,3,34,791488000],"roleNames":["超级管理员"],"allowedFonds":["BR-GROUP","BR-MFG","BR-SALES","BR-TRADE","COMP001","DEMO"]}},"timestamp":1768388428897}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-14 19:00:28.901138	0:0:0:0:0:0:0:1	2026-01-14 19:00:28.899063	\N	\N	\N	\N	\N
eb44c3f08f3d4b6bb9e91c40ed1b26cd	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-14 19:01:20.545888	\N	2026-01-14 19:01:20.545133	\N	\N	\N	\N	\N
e76d37b92fd745ca82d8a521cddd9594	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 14:15:34.894075	\N	2026-01-15 14:15:34.889612	\N	\N	\N	\N	\N
f67ad34f5e0c4eb4861af9ca651dbb9c	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-003	SUCCESS	low	查询档案文件内容: fileId=file-invoice-003	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 14:15:38.875081	\N	2026-01-15 14:15:38.872908	\N	\N	\N	\N	\N
13a6af75963d43d89f09006b78cfe9d8	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-003	SUCCESS	low	查询档案文件内容: fileId=file-invoice-003	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 14:15:55.226428	\N	2026-01-15 14:15:55.226473	\N	\N	\N	\N	\N
c259de6ee2564e7483f1a0841fbed90e	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 14:17:58.43573	\N	2026-01-15 14:17:58.43011	\N	\N	\N	\N	\N
3cbb27a82fb845b595e300b625b6bfff	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 14:18:41.901193	\N	2026-01-15 14:18:41.901152	\N	\N	\N	\N	\N
28cec5df0a1242b5863f759eb5e8a012	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 14:28:05.330063	\N	2026-01-15 14:28:05.329735	\N	\N	\N	\N	\N
43d9459960e0426a876437919183df0f	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	MEDIUM	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJ1c2VyX2FkbWluXzAwMSIsImlhdCI6MTc2ODQ2NTUxNCwiZXhwIjoxNzY4NTUxOTE0fQ.iPSGlyzSXRAk0BlHuh4Fuw3cSx9zl0XaI7FYYPrNeTzUH4CsZ3ps_KkR_ThA4H0RE2rimMP1mQAgMo65QirCZadPN54FWGZQ-z_CHdfsZq62VgAyDN2dQ7os-rQLGs2c6xZk2v2xAKIG5KjPv2s9p-K_D9UDPTfEmzquHUHV_626H32ZxIwOZTDVx3NNkDjI4Moj_lggSz8yQvlGD9ptYiMuVNCQaJRY7k1_cXJHVSAM39s5HnMo1mKbKfYNjk37ecFFlWPnTowU0C8ARV7sblppqjB7BEy6TtHwKQPWnAusCuEd0VoM0wCBlqjOydDvJWIoPQs-l028va-MFRk-bg","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","scan:manage","archive:view","nav:archive_mgmt","archive:manage","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","archive:read","audit:view","nav:pre_archive"],"phone":null,"employeeId":null,"jobTitle":null,"orgCode":"BR-GROUP","lastLoginAt":[2026,1,15,16,25,14,923470000],"createdTime":[2025,12,28,9,3,34,791488000],"roleNames":["超级管理员"],"allowedFonds":["BR-GROUP","BR-MFG","BR-SALES","BR-TRADE","COMP001","DEMO"]}},"timestamp":1768465514979}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:25:15.041383	0:0:0:0:0:0:0:1	2026-01-15 16:25:15.02833	\N	\N	\N	\N	\N
fb4aff6d49624571840da423ce082b14	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	demo-file-005	SUCCESS	low	查询档案文件内容: itemId=demo-file-005	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 16:37:45.981232	\N	2026-01-15 16:37:45.973746	\N	\N	\N	\N	\N
723acc4e51f142f0acbc24a354df48cc	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-003	SUCCESS	low	查询档案文件内容: fileId=file-invoice-003	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 17:06:08.746455	\N	2026-01-15 17:06:08.746568	\N	\N	\N	\N	\N
c8cc86c64d9b44aab539d500346aa2cc	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-003	SUCCESS	low	查询档案文件内容: fileId=file-invoice-003	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 17:13:39.388168	\N	2026-01-15 17:13:39.387409	\N	\N	\N	\N	\N
464d4aef0b43415c828b469eb61c93c1	user_admin_001	admin	\N	BATCH_APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	MEDIUM	批量批准归档申请	{"ids":["test-approval-7e32cc3cf48cfa8324793e70d11fc3ce"],"approverId":"user_admin_001","approverName":"admin","comment":"批量批准","skipIds":null}	{"code":200,"message":"操作成功","data":{"successCount":1,"failed":0,"errors":[]},"timestamp":1768464290885}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:04:50.898277	0:0:0:0:0:0:0:1	2026-01-15 16:04:50.891623	\N	\N	\N	\N	\N
d382adf1b3c345149cacc7bb2af1fd4a	user_admin_001	admin	\N	BATCH_APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	MEDIUM	批量批准归档申请	{"ids":["test-approval-7e32cc3cf48cfa8324793e70d11fc3ce"],"approverId":"user_admin_001","approverName":"admin","comment":"批量批准","skipIds":null}	{"successCount":1,"failed":0,"errors":[]}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:04:50.898831	0:0:0:0:0:0:0:1	2026-01-15 16:04:50.891623	\N	\N	\N	\N	\N
46b3ef71fb8e4009a233c0e4a215e883	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	MEDIUM	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJ1c2VyX2FkbWluXzAwMSIsImlhdCI6MTc2ODQ2NDY2MywiZXhwIjoxNzY4NTUxMDYzfQ.GaFWYAnldLFmwyOCW31cfLp4WxdJ_GNwrI0gv9_ms8_0zfEV_1L-mDD_clLFhWoHtdlSNBz89MUWsuLNQ5XT3Yee1Afc3ZQOq2jaykAZ1UtnKL5zTBRZVBs64gpdd7Y3Y7lAkjWS9i31N9HiWF2JgjViJ_q_ccXvfJzLDqihbRjrmSb0VGroDOLPovRXvJPojHpbyqzmrdiAVhpHh7EYTVP8N8F6gp5pFwOWH4gHWWKTm7ukNKgFBpOLZde2N70Vw2yBC67sGB2ZgWB022Ag9TcusLctUAnziBAX-UT8Z1YsGlqX59r0P2_JRjWn7rcrQwdCAm07OX3wD6N9e3vzMQ","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","scan:manage","archive:view","nav:archive_mgmt","archive:manage","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","archive:read","audit:view","nav:pre_archive"],"phone":null,"employeeId":null,"jobTitle":null,"orgCode":"BR-GROUP","lastLoginAt":[2026,1,15,16,11,3,456512000],"createdTime":[2025,12,28,9,3,34,791488000],"roleNames":["超级管理员"],"allowedFonds":["BR-GROUP","BR-MFG","BR-SALES","BR-TRADE","COMP001","DEMO"]}},"timestamp":1768464663484}	\N	\N	UNKNOWN	\N	curl/8.7.1	\N	\N	curl/8.7.1|||	2026-01-15 16:11:03.502364	0:0:0:0:0:0:0:1	2026-01-15 16:11:03.499185	\N	\N	\N	\N	\N
c79a4238a72e45db8ce375bc24e5b06a	user_admin_001	admin	\N	CAPTURE	ERP_SYNC	\N	SUCCESS	MEDIUM	手动触发ERP同步场景	6	{"code":200,"message":"操作成功","data":{"taskId":"sync-6-1768464810461","status":"SUBMITTED","message":"同步任务已提交"},"timestamp":1768464810557}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:13:30.598811	0:0:0:0:0:0:0:1	2026-01-15 16:13:30.560266	\N	\N	\N	\N	\N
b836efa05d864e4d95d04cdb7e2b997d	user_admin_001	USER_user_admin_001	\N	CAPTURE	ERP_SYNC	6	SUCCESS	low	ERP采集同步: 场景=凭证同步, 结果=SUCCESS, 获取=10, 成功=4, 失败=6	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 16:13:38.344276	0:0:0:0:0:0:0:1	2026-01-15 16:13:38.343593	\N	\N	\N	\N	\N
49e818770fad4dc69d18e00ae5f01123	user_admin_001	admin	\N	SUBMIT_ARCHIVE_BATCH	PRE_ARCHIVE	\N	SUCCESS	MEDIUM	批量提交归档申请	{"fileIds":["demo-file-005"],"applicantId":"user_admin_001","applicantName":"admin","reason":"批量归档申请"}	{"code":200,"message":"操作成功","data":{"successItems":[],"failures":{"demo-file-005":"文件状态不允许提交归档，当前状态: PENDING_CHECK"},"failureCount":1,"successCount":0},"timestamp":1768464824854}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:13:44.859356	0:0:0:0:0:0:0:1	2026-01-15 16:13:44.857714	\N	\N	\N	\N	\N
cb9d44acf6624fa9b7541af59943c641	user_admin_001	admin	\N	CREATE_APPROVAL	ARCHIVE_APPROVAL	\N	SUCCESS	MEDIUM	创建归档审批申请	{"id":null,"archiveId":"be4a056e6fbf471281c9341d876da5c0","archiveCode":"BR-GROUP-2020-30Y-ORG-AC01-000001","orgName":"用友 YonSuite (生产环境)","archiveTitle":"记-4_Voucher","applicantId":"user_admin_001","applicantName":"admin","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":null,"approvalComment":null,"approvalTime":null,"createdTime":[2026,1,15,16,16,45,830240000],"lastModifiedTime":[2026,1,15,16,16,45,830250000],"deleted":null}	{"id":"2011714158295101441","archiveId":"be4a056e6fbf471281c9341d876da5c0","archiveCode":"BR-GROUP-2020-30Y-ORG-AC01-000001","orgName":"用友 YonSuite (生产环境)","archiveTitle":"记-4_Voucher","applicantId":"user_admin_001","applicantName":"admin","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":"PENDING","approvalComment":null,"approvalTime":null,"createdTime":[2026,1,15,16,16,45,830240000],"lastModifiedTime":[2026,1,15,16,16,45,830250000],"deleted":null}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:16:45.85828	0:0:0:0:0:0:0:1	2026-01-15 16:16:45.857151	\N	\N	\N	\N	\N
0bb7a042df9945e6ab24404764c6c098	user_admin_001	admin	\N	SUBMIT_ARCHIVE_BATCH	PRE_ARCHIVE	\N	SUCCESS	MEDIUM	批量提交归档申请	{"fileIds":["be4a056e6fbf471281c9341d876da5c0"],"applicantId":"user_admin_001","applicantName":"admin","reason":"批量归档申请"}	{"code":200,"message":"操作成功","data":{"successItems":[{"id":"2011714158295101441","archiveId":"be4a056e6fbf471281c9341d876da5c0","archiveCode":"BR-GROUP-2020-30Y-ORG-AC01-000001","orgName":"用友 YonSuite (生产环境)","archiveTitle":"记-4_Voucher","applicantId":"user_admin_001","applicantName":"admin","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":"PENDING","approvalComment":null,"approvalTime":null,"createdTime":[2026,1,15,16,16,45,830240000],"lastModifiedTime":[2026,1,15,16,16,45,830250000],"deleted":null}],"failures":{},"failureCount":0,"successCount":1},"timestamp":1768465005858}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:16:45.86052	0:0:0:0:0:0:0:1	2026-01-15 16:16:45.859926	\N	\N	\N	\N	\N
0a6df557dfb4477d9cb11552cbcab1d6	user_admin_001	admin	\N	BATCH_APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	MEDIUM	批量批准归档申请	{"ids":["2011714158295101441"],"approverId":"user_admin_001","approverName":"admin","comment":"批量批准","skipIds":null}	{"successCount":1,"failed":0,"errors":[]}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:17:11.721152	0:0:0:0:0:0:0:1	2026-01-15 16:17:11.720277	\N	\N	\N	\N	\N
a9dd0cb5f63d4160b58d71311a7fd79e	user_admin_001	admin	\N	BATCH_APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	MEDIUM	批量批准归档申请	{"ids":["2011714158295101441"],"approverId":"user_admin_001","approverName":"admin","comment":"批量批准","skipIds":null}	{"code":200,"message":"操作成功","data":{"successCount":1,"failed":0,"errors":[]},"timestamp":1768465031720}	\N	\N	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	\N	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2026-01-15 16:17:11.721835	0:0:0:0:0:0:0:1	2026-01-15 16:17:11.721769	\N	\N	\N	\N	\N
6f645f214d7543e7a19988b622c5fe33	user_admin_001	USER_user_admin_001	\N	ARCHIVE_FILE_VIEWED	ARCHIVE_FILE	file-invoice-002	SUCCESS	low	查询档案文件内容: fileId=file-invoice-002	\N	\N	\N	\N	UNKNOWN	\N	\N	\N	\N	\N	2026-01-15 17:06:04.414106	\N	2026-01-15 17:06:04.406655	\N	\N	\N	\N	\N
\.


--
-- Data for Name: sys_entity; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_entity (id, name, tax_id, address, contact_person, contact_phone, contact_email, status, description, created_by, created_time, updated_time, deleted, parent_id, order_num) FROM stdin;
ORG_BR_GROUP	泊冉集团有限公司	\N	\N	\N	\N	\N	ACTIVE	泊冉集团有限公司（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	\N	1
ORG_BR_SALES	泊冉销售有限公司	\N	\N	\N	\N	\N	ACTIVE	泊冉销售有限公司（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	1
ORG_BR_TRADE	泊冉国际贸易有限公司	\N	\N	\N	\N	\N	ACTIVE	泊冉国际贸易有限公司（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	2
ORG_BR_MFG	泊冉制造有限公司	\N	\N	\N	\N	\N	ACTIVE	泊冉制造有限公司（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	3
ORG_BR_GROUP_FIN	财务管理部	\N	\N	\N	\N	\N	ACTIVE	财务管理部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	10
ORG_BR_GROUP_HR	人力资源部	\N	\N	\N	\N	\N	ACTIVE	人力资源部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	11
ORG_BR_GROUP_IT	信息技术部	\N	\N	\N	\N	\N	ACTIVE	信息技术部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	12
ORG_BR_GROUP_LEGAL	法务合规部	\N	\N	\N	\N	\N	ACTIVE	法务合规部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	13
ORG_BR_GROUP_AUDIT	审计监察部	\N	\N	\N	\N	\N	ACTIVE	审计监察部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_GROUP	14
ORG_BR_SALES_DOM	国内销售部	\N	\N	\N	\N	\N	ACTIVE	国内销售部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_SALES	1
ORG_BR_SALES_INT	海外销售部	\N	\N	\N	\N	\N	ACTIVE	海外销售部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_SALES	2
ORG_BR_SALES_MKT	市场推广部	\N	\N	\N	\N	\N	ACTIVE	市场推广部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_SALES	3
ORG_BR_SALES_FIN	财务部	\N	\N	\N	\N	\N	ACTIVE	财务部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_SALES	10
ORG_BR_TRADE_IMP	进口业务部	\N	\N	\N	\N	\N	ACTIVE	进口业务部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_TRADE	1
ORG_BR_TRADE_EXP	出口业务部	\N	\N	\N	\N	\N	ACTIVE	出口业务部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_TRADE	2
ORG_BR_TRADE_LOG	物流仓储部	\N	\N	\N	\N	\N	ACTIVE	物流仓储部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_TRADE	3
ORG_BR_TRADE_FIN	财务部	\N	\N	\N	\N	\N	ACTIVE	财务部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_TRADE	10
ORG_BR_MFG_PROD	生产管理部	\N	\N	\N	\N	\N	ACTIVE	生产管理部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_MFG	1
ORG_BR_MFG_QC	质量控制部	\N	\N	\N	\N	\N	ACTIVE	质量控制部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_MFG	2
ORG_BR_MFG_RD	研发技术部	\N	\N	\N	\N	\N	ACTIVE	研发技术部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_MFG	3
ORG_BR_MFG_SUPPLY	采购供应部	\N	\N	\N	\N	\N	ACTIVE	采购供应部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_MFG	4
ORG_BR_MFG_FIN	财务部	\N	\N	\N	\N	\N	ACTIVE	财务部（从组织迁移）	\N	2025-12-28 09:03:34.916025	2026-01-14 18:54:06.830786	0	ORG_BR_MFG	10
\.


--
-- Data for Name: sys_entity_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_entity_config (id, entity_id, config_type, config_key, config_value, description, created_by, created_time, updated_time, deleted) FROM stdin;
\.


--
-- Data for Name: sys_env_marker; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_env_marker (marker_key, marker_value, created_at) FROM stdin;
INSTANCE_SIG	NEXUS_ARCHIVE_SAFE_INSTANCE	2025-12-28 09:14:13.474449
\.


--
-- Data for Name: sys_erp_adapter; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_erp_adapter (adapter_id, adapter_name, erp_type, base_url, enabled, created_time, updated_time) FROM stdin;
\.


--
-- Data for Name: sys_erp_adapter_scenario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_erp_adapter_scenario (id, adapter_id, scenario_code, created_time) FROM stdin;
\.


--
-- Data for Name: sys_erp_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_erp_config (id, name, erp_type, config_json, is_active, created_time, last_modified_time, accbook_mapping, sap_interface_type) FROM stdin;
3	金蝶云星空	KINGDEE	{"system": "金蝶云星空", "frequency": "每日 23:00", "description": "存货核算数据同步"}	1	2025-12-28 09:03:34.854164	2026-01-15 08:47:39.335424	\N	\N
5	易快报	GENERIC	{"system": "易快报", "frequency": "每小时", "description": "差旅费用数据同步"}	1	2025-12-28 09:03:34.854164	2026-01-15 08:47:39.335424	\N	\N
2	SAP ERP	SAP	{"system": "SAP ERP", "frequency": "实时", "description": "SAP 财务凭证自动同步接口"}	1	2025-12-28 09:03:34.854164	2026-01-15 08:47:39.335424	\N	\N
4	泛微 OA	WEAVER	{"system": "泛微OA", "frequency": "每小时", "description": "员工报销单据同步"}	1	2025-12-28 09:03:34.854164	2026-01-15 08:47:39.335424	\N	\N
1	用友 YonSuite	YONSUITE	{"baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway", "appKey": "96a95c00982446cba484ccc4936b221b", "appSecret": "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6", "accbookCode": "BR01", "extraConfig": ""}	1	2025-12-28 09:03:34.854164	2026-01-15 08:47:39.335424	{\n  "001": "COMP001", \n  "CS002": "DEMO", \n  "BR01": "BR-GROUP", \n  "BR02": "BR-SALES", \n  "BR03": "BR-TRADE", \n  "BR04": "BR-MFG", \n  "WQ01": "BRJT"\n}	\N
\.


--
-- Data for Name: sys_erp_feedback_queue; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_erp_feedback_queue (id, voucher_id, archival_code, erp_type, erp_config_id, retry_count, max_retries, last_error, status, created_time, updated_time, next_retry_time) FROM stdin;
\.


--
-- Data for Name: sys_erp_scenario; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_erp_scenario (id, config_id, scenario_key, name, description, is_active, sync_strategy, cron_expression, last_sync_time, last_sync_status, last_sync_msg, created_time, last_modified_time, params_json) FROM stdin;
7	1	ORG_SYNC	组织架构同步	从用友YonSuite同步组织架构	t	SCHEDULED	0 0 2 * * ?	2025-12-27 02:00:00	SUCCESS	同步成功，更新50个组织节点	2025-12-20 10:00:00	2025-12-27 02:00:00	\N
8	1	PAYMENT_APPLY_SYNC	付款申请同步	从用友YonSuite同步付款申请单	t	MANUAL	\N	\N	\N	\N	2025-12-20 10:00:00	2025-12-20 10:00:00	\N
9	2	VOUCHER_SYNC	凭证同步	从SAP ERP同步凭证数据	t	MANUAL	\N	\N	\N	\N	2025-12-20 10:00:00	2025-12-20 10:00:00	\N
10	3	VOUCHER_SYNC	凭证同步	从金蝶云星空同步记账凭证	t	MANUAL	\N	\N	\N	\N	2025-12-20 10:00:00	2025-12-20 10:00:00	\N
11	4	VOUCHER_SYNC	凭证同步	从泛微OA同步凭证数据	t	MANUAL	\N	\N	\N	\N	2025-12-20 10:00:00	2025-12-20 10:00:00	\N
12	5	EXPENSE_SYNC	报销单同步	从易快报同步报销单数据	t	MANUAL	\N	\N	\N	\N	2025-12-20 10:00:00	2025-12-20 10:00:00	\N
6	1	VOUCHER_SYNC	凭证同步	从用友YonSuite同步记账凭证	t	MANUAL	\N	2026-01-15 16:13:38.31859	SUCCESS	同步成功: 获取 10 条，其中新增 4 条 (2020-01-01至2026-01-15)	2025-12-20 10:00:00	2025-12-26 14:30:00	\N
13	1	ATTACHMENT_SYNC	凭证附件同步	从用友YonSuite同步凭证附件文件	t	MANUAL	\N	\N	\N	\N	2026-01-15 08:40:42.220481	2026-01-15 08:40:42.220481	\N
14	1	COLLECTION_FILE_SYNC	收款文件同步	从用友YonSuite同步收款相关文件	t	MANUAL	\N	\N	\N	\N	2026-01-15 08:40:42.220481	2026-01-15 08:40:42.220481	\N
15	1	PAYMENT_FILE_SYNC	付款文件同步	从用友YonSuite同步付款相关文件	t	MANUAL	\N	\N	\N	\N	2026-01-15 08:40:42.220481	2026-01-15 08:40:42.220481	\N
16	1	REFUND_FILE_SYNC	退款文件同步	从用友YonSuite同步退款相关文件	t	MANUAL	\N	\N	\N	\N	2026-01-15 08:40:42.220481	2026-01-15 08:40:42.220481	\N
\.


--
-- Data for Name: sys_erp_sub_interface; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_erp_sub_interface (id, scenario_id, interface_key, interface_name, description, is_active, sort_order, config_json, created_time, last_modified_time) FROM stdin;
\.


--
-- Data for Name: sys_ingest_request_status; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_ingest_request_status (request_id, status, message, created_time, updated_time, created_at, updated_at, fonds_no) FROM stdin;
\.


--
-- Data for Name: sys_original_voucher_type; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) FROM stdin;
ovt-001	INVOICE	发票类	INV_PAPER	纸质发票	30Y	1	t	2025-12-28 09:03:34.992088	\N
ovt-002	INVOICE	发票类	INV_VAT_E	增值税电子发票	30Y	2	t	2025-12-28 09:03:34.992088	\N
ovt-003	INVOICE	发票类	INV_DIGITAL	数电发票	30Y	3	t	2025-12-28 09:03:34.992088	\N
ovt-004	INVOICE	发票类	INV_RAIL	数电票（铁路）	30Y	4	t	2025-12-28 09:03:34.992088	\N
ovt-005	INVOICE	发票类	INV_AIR	数电票（航空）	30Y	5	t	2025-12-28 09:03:34.992088	\N
ovt-006	INVOICE	发票类	INV_GOV	数电票（财政）	30Y	6	t	2025-12-28 09:03:34.992088	\N
ovt-007	BANK	银行类	BANK_RECEIPT	银行回单	30Y	10	t	2025-12-28 09:03:34.992088	\N
ovt-009	DOCUMENT	单据类	DOC_PAYMENT	付款单	30Y	20	t	2025-12-28 09:03:34.992088	\N
ovt-010	DOCUMENT	单据类	DOC_RECEIPT	收款单	30Y	21	t	2025-12-28 09:03:34.992088	\N
ovt-011	DOCUMENT	单据类	DOC_RECEIPT_VOUCHER	收款单据（收据）	30Y	22	t	2025-12-28 09:03:34.992088	\N
ovt-012	DOCUMENT	单据类	DOC_PAYROLL	工资单	30Y	23	t	2025-12-28 09:03:34.992088	\N
ovt-014	CONTRACT	合同类	AGREEMENT	协议	30Y	31	t	2025-12-28 09:03:34.992088	\N
ovt-015	OTHER	其他类	OTHER	其他	30Y	99	t	2025-12-28 09:03:34.992088	\N
OVT-DOC-001	DOCUMENT	单据类	SALES_ORDER	销售订单	30Y	10	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-DOC-002	DOCUMENT	单据类	DELIVERY_ORDER	出库单	30Y	20	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-DOC-003	DOCUMENT	单据类	PURCHASE_ORDER	采购订单	30Y	30	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-DOC-004	DOCUMENT	单据类	RECEIPT_ORDER	入库单	30Y	40	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-DOC-005	DOCUMENT	单据类	PAYMENT_REQ	付款申请单	30Y	50	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-DOC-006	DOCUMENT	单据类	EXPENSE_REPORT	报销单	30Y	60	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-INV-001	INVOICE	发票类	GEN_INVOICE	普通发票	30Y	10	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-INV-002	INVOICE	发票类	VAT_INVOICE	增值税专票	30Y	20	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-BNK-001	BANK	银行类	BANK_SLIP	银行回单	30Y	10	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
OVT-CON-001	CONTRACT	合同类	CONTRACT	合同协议	30Y	10	t	2025-12-28 09:03:35.003657	2025-12-28 09:03:35.003657
\.


--
-- Data for Name: sys_permission; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_permission (id, perm_key, label, group_name, created_at, last_modified_time, created_time, updated_time) FROM stdin;
perm_manage_users	manage_users	用户管理	系统管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_manage_roles	manage_roles	角色管理	系统管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_manage_org	manage_org	组织管理	系统管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_manage_settings	manage_settings	系统设置	系统管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_manage_fonds	manage_fonds	全宗管理	系统管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_all	nav:all	所有导航	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_portal	nav:portal	门户首页	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_panorama	nav:panorama	全景视图	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_pre_archive	nav:pre_archive	预归档库	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_collection	nav:collection	资料收集	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_archive_mgmt	nav:archive_mgmt	档案管理	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_query	nav:query	档案查询	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_borrowing	nav:borrowing	档案借阅	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_destruction	nav:destruction	档案销毁	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_warehouse	nav:warehouse	库房管理	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_stats	nav:stats	数据统计	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_nav_settings	nav:settings	系统设置	导航权限	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_create	archive:create	创建档案	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_view	archive:view	查看档案	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_edit	archive:edit	编辑档案	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_delete	archive:delete	删除档案	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_download	archive:download	下载档案	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_print	archive:print	打印档案	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_archive_approve	archive:approve	审批归档	档案操作	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_borrow_apply	borrow:apply	申请借阅	借阅管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_borrow_approve	borrow:approve	审批借阅	借阅管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_destruction_apply	destruction:apply	销毁鉴定	销毁管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_destruction_approve	destruction:approve	审批销毁	销毁管理	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_audit_view	audit:view	查看审计日志	安全审计	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_audit_export	audit:export	导出审计日志	安全审计	2025-12-28 09:03:34.86437	2025-12-28 09:03:34.86437	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
perm_scan_view	scan:view	查看扫描工作区	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_scan_upload	scan:upload	上传扫描文件	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_scan_ocr	scan:ocr	执行OCR识别	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_scan_edit	scan:edit	编辑扫描结果	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_scan_submit	scan:submit	提交到预归档	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_scan_delete	scan:delete	删除扫描文件	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_scan_manage	scan:manage	管理扫描工作区	扫描管理	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984	2026-01-14 18:56:36.762984
perm_view_entity	entity:view	查看法人	entity	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044
perm_manage_entity	entity:manage	管理法人	entity	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044
perm_view_fonds	fonds:view	查看全宗	fonds	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044	2026-01-15 13:59:43.220044
\.


--
-- Data for Name: sys_position; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_position (id, name, code, department_id, description, status, created_time, updated_time, deleted) FROM stdin;
\.


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_role (id, name, code, role_category, is_exclusive, description, permissions, data_scope, type, created_at, last_modified_time, deleted, created_time) FROM stdin;
role_query_user	查询用户	query_user	query_user	f	查询用户角色，仅能通过借阅审批访问特定档案，具有最小权限	["nav:utilization","borrowing:create","borrowing:view","archive:read"]	self	custom	2026-01-14 18:56:36.485735	2026-01-14 18:56:36.485735	0	2026-01-14 18:56:36.485735
role_security_admin	安全保密员	security_admin	security_admin	t	负责用户权限分配、角色管理、密钥管理（三员分立之一）	["nav:portal","nav:settings","manage_users","manage_roles","manage_positions","manage_org","reset_password","view_users","view_roles","archive:read","archive:view"]	all	system	2026-01-14 18:56:36.518649	2026-01-14 18:56:36.518649	0	2026-01-14 18:56:36.518649
role_audit_admin	安全审计员	audit_admin	audit_admin	t	负责查看和审计系统日志（三员分立之一）	["nav:portal","audit:view","audit:export","audit:trace","archive:read","archive:view"]	all	system	2026-01-14 18:56:36.518649	2026-01-14 18:56:36.518649	0	2026-01-14 18:56:36.518649
role_business_user	业务操作员	business_user	business_user	f	默认业务用户角色，具有基本业务操作权限，无管理权限	["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:repository","nav:archive_mgmt","nav:operations","nav:utilization","nav:stats","archive:read","archive:view","borrowing:create","borrowing:view"]	self	custom	2026-01-14 18:56:36.992067	2026-01-14 18:56:37.034293	0	2026-01-14 18:56:36.992067
role_system_admin	系统管理员	system_admin	system_admin	t	负责系统运维和配置管理（三员分立之一）	["nav:portal","nav:settings","manage_settings","system:backup","system:restore","system:monitor","archive:read","archive:view","audit:view"]["entity:view", "entity:manage", "fonds:view"]	all	system	2026-01-14 18:56:36.518649	2026-01-14 18:56:36.518649	0	2026-01-14 18:56:36.518649
role_super_admin	超级管理员	super_admin	system_admin	f	\N	["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users","archive:read","archive:view","archive:manage","audit:view","scan:manage"]["entity:view", "entity:manage", "fonds:view"]	self	custom	2025-12-28 09:03:34.791488	2025-12-28 09:03:34.791488	0	2026-01-14 18:54:06.830786
\.


--
-- Data for Name: sys_setting; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted, created_time, updated_time) FROM stdin;
2005083092585488385	system.name	Nexus Archive System	系统名称	\N	system	\N	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
2005083092585488386	archive.prefix	QZ-2024-	档号前缀	\N	archive	\N	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
2005083092589682689	storage.type	local	存储类型 local/nas/oss	\N	storage	\N	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
2005083092589682690	storage.path	/data/archive	本地存储路径	\N	storage	\N	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
2005083092589682691	retention.default	10Y	默认保管期限	\N	archive	\N	0	2026-01-14 18:54:06.830786	2026-01-14 18:54:06.830786
\.


--
-- Data for Name: sys_sql_audit_rule; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_sql_audit_rule (rule_key, rule_value, created_time, last_modified_time) FROM stdin;
protected_markers	acc_archive,arc_,bas_fonds,sys_fonds	2026-01-14 18:54:06.855977	2026-01-14 18:54:06.855977
required_columns	fonds_no,archive_year	2026-01-14 18:54:06.855977	2026-01-14 18:54:06.855977
\.


--
-- Data for Name: sys_sync_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_sync_history (id, scenario_id, sync_start_time, sync_end_time, status, total_count, success_count, fail_count, error_message, sync_params, created_time, operator_id, client_ip, four_nature_summary) FROM stdin;
1	6	2026-01-15 16:13:30.673613	2026-01-15 16:13:38.31897	SUCCESS	10	4	6	\N	\N	2026-01-15 16:13:30.673642	user_admin_001	0:0:0:0:0:0:0:1	\N
\.


--
-- Data for Name: sys_sync_task; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_sync_task (id, task_id, scenario_id, status, total_count, success_count, fail_count, error_message, progress, start_time, end_time, operator_id, client_ip, sync_params, created_time, updated_time) FROM stdin;
1	sync-6-1768464810461	6	SUCCESS	0	0	0	\N	1.0000	2026-01-15 16:13:30.462713	2026-01-15 16:13:38.34383	user_admin_001	0:0:0:0:0:0:0:1	\N	2026-01-15 16:13:30.462728	2026-01-15 16:13:38.343823
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_time, last_modified_time, deleted) FROM stdin;
user-lisi	lisi	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	李四	BR-SALES	lisi@boran.com	\N	\N	ORG_BR_SALES_FIN	active	\N	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0
user-wangwu	wangwu	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	王五	BR-GROUP	wangwu@boran.com	\N	\N	ORG_BR_GROUP_AUDIT	active	\N	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0
user-zhaoliu	zhaoliu	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	赵六	BR-MFG	zhaoliu@boran.com	\N	\N	ORG_BR_MFG_FIN	active	\N	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0
user-qianqi	qianqi	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	钱七	BR-GROUP	qianqi@boran.com	\N	\N	ORG_BR_GROUP_FIN	active	\N	\N	\N	\N	2025-12-28 09:03:35.017527	2025-12-28 09:03:35.017527	0
user-zhangsan	zhangsan	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	张三	BR-GROUP	zhangsan@boran.com	\N	\N	ORG_BR_GROUP_FIN	active	2025-12-28 09:06:39.402242	\N	\N	\N	2025-12-28 09:03:35.017527	\N	0
user_admin_001	admin	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	系统管理员	BR-GROUP	admin@nexusarchive.local	\N	\N	ORG_BR_GROUP_FIN	active	2026-01-15 16:25:14.92347	\N	\N	\N	2025-12-28 09:03:34.791488	2025-12-28 09:03:34.791488	0
\.


--
-- Data for Name: sys_user_fonds_scope; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_user_fonds_scope (id, user_id, fonds_no, scope_type, created_time, last_modified_time, deleted) FROM stdin;
user_admin_001-BR-GROUP	user_admin_001	BR-GROUP	DIRECT	2026-01-14 18:54:07.125878	2026-01-14 18:54:07.125878	0
user-lisi-BR-SALES	user-lisi	BR-SALES	DIRECT	2026-01-14 18:54:07.125878	2026-01-14 18:54:07.125878	0
user-wangwu-BR-GROUP	user-wangwu	BR-GROUP	DIRECT	2026-01-14 18:54:07.125878	2026-01-14 18:54:07.125878	0
user-zhaoliu-BR-MFG	user-zhaoliu	BR-MFG	DIRECT	2026-01-14 18:54:07.125878	2026-01-14 18:54:07.125878	0
user-qianqi-BR-GROUP	user-qianqi	BR-GROUP	DIRECT	2026-01-14 18:54:07.125878	2026-01-14 18:54:07.125878	0
user-zhangsan-BR-GROUP	user-zhangsan	BR-GROUP	DIRECT	2026-01-14 18:54:07.125878	2026-01-14 18:54:07.125878	0
user-lisi-BR-GROUP	user-lisi	BR-GROUP	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhaoliu-BR-GROUP	user-zhaoliu	BR-GROUP	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user_admin_001-BR-MFG	user_admin_001	BR-MFG	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-lisi-BR-MFG	user-lisi	BR-MFG	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-wangwu-BR-MFG	user-wangwu	BR-MFG	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-qianqi-BR-MFG	user-qianqi	BR-MFG	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhangsan-BR-MFG	user-zhangsan	BR-MFG	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user_admin_001-BR-SALES	user_admin_001	BR-SALES	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-wangwu-BR-SALES	user-wangwu	BR-SALES	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhaoliu-BR-SALES	user-zhaoliu	BR-SALES	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-qianqi-BR-SALES	user-qianqi	BR-SALES	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhangsan-BR-SALES	user-zhangsan	BR-SALES	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user_admin_001-BR-TRADE	user_admin_001	BR-TRADE	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-lisi-BR-TRADE	user-lisi	BR-TRADE	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-wangwu-BR-TRADE	user-wangwu	BR-TRADE	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhaoliu-BR-TRADE	user-zhaoliu	BR-TRADE	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-qianqi-BR-TRADE	user-qianqi	BR-TRADE	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhangsan-BR-TRADE	user-zhangsan	BR-TRADE	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user_admin_001-COMP001	user_admin_001	COMP001	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-lisi-COMP001	user-lisi	COMP001	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-wangwu-COMP001	user-wangwu	COMP001	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhaoliu-COMP001	user-zhaoliu	COMP001	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-qianqi-COMP001	user-qianqi	COMP001	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhangsan-COMP001	user-zhangsan	COMP001	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user_admin_001-DEMO	user_admin_001	DEMO	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-lisi-DEMO	user-lisi	DEMO	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-wangwu-DEMO	user-wangwu	DEMO	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhaoliu-DEMO	user-zhaoliu	DEMO	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-qianqi-DEMO	user-qianqi	DEMO	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
user-zhangsan-DEMO	user-zhangsan	DEMO	MIGRATION	2026-01-14 18:54:07.252396	2026-01-14 18:54:07.252396	0
\.


--
-- Data for Name: sys_user_role; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sys_user_role (user_id, role_id) FROM stdin;
user_admin_001	role_super_admin
user-zhangsan	role_super_admin
user-lisi	role_super_admin
user-wangwu	role_super_admin
user-zhaoliu	role_super_admin
user-qianqi	role_super_admin
\.


--
-- Data for Name: system_performance_metrics; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.system_performance_metrics (id, metric_type, metric_name, metric_value, metric_unit, fonds_no, recorded_at, created_at) FROM stdin;
\.


--
-- Data for Name: user_mfa_config; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_mfa_config (id, user_id, mfa_enabled, mfa_type, secret_key, backup_codes, last_used_at, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: voucher_match_result; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.voucher_match_result (id, task_id, batch_task_id, match_batch_id, voucher_id, voucher_hash, config_hash, template_id, template_version, scene, confidence, status, match_details, missing_docs, is_latest, created_time) FROM stdin;
1	\N	\N	5e4e4e34a43948fb86ef2de2166acd70	6090f8648c4c4a3280376cad676f076f	f7c8851fbd2219ed3229ad812137f40e50ba488a00eecc489c0a279ce9a818b7	\N	T00_MANUAL	1.0.0	UNKNOWN	\N	MATCHED	[]	\N	t	2026-01-15 18:15:16.409871
2	\N	\N	0492d2c9ab864ef4b1a8322f4ef18fd3	80b4c7e896474f9180f4a821c46cedef	bf77a7feb339017865ee1d2ef529367a7e8008d5c704b384d643874ac23b687e	\N	T00_MANUAL	1.0.0	UNKNOWN	\N	MATCHED	[]	\N	t	2026-01-15 18:15:16.4283
3	\N	\N	d1b5d024d4d942ea8bd52724ca2d634c	b7817092a1294f82b0b8cd3cd346fc1b	d5915d1932a51a44d78aed3ebd3e315a083c588d087c496bf2f0860b17fbad1b	\N	T00_MANUAL	1.0.0	UNKNOWN	\N	MATCHED	[]	\N	t	2026-01-15 18:15:16.431805
\.


--
-- Data for Name: voucher_source_link; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.voucher_source_link (id, match_batch_id, voucher_id, source_doc_id, evidence_role, link_type, match_score, match_reasons, allocated_amount, is_auto, status, created_time, created_by) FROM stdin;
\.


--
-- Data for Name: ys_sales_out; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ys_sales_out (id, ys_id, code, vouchdate, status, cust_name, warehouse_name, total_quantity, memo, raw_json, sync_time, created_time) FROM stdin;
\.


--
-- Data for Name: ys_sales_out_detail; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ys_sales_out_detail (id, sales_out_id, ys_detail_id, rowno, product_code, product_name, qty, unit_name, ori_money, ori_tax) FROM stdin;
\.


--
-- Name: arc_archive_batch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.arc_archive_batch_id_seq', 1, false);


--
-- Name: arc_batch_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.arc_batch_seq', 1, true);


--
-- Name: arc_original_voucher_event_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.arc_original_voucher_event_id_seq', 1, false);


--
-- Name: archive_amendment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.archive_amendment_id_seq', 1, false);


--
-- Name: archive_batch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.archive_batch_id_seq', 1, false);


--
-- Name: archive_batch_item_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.archive_batch_item_id_seq', 1, false);


--
-- Name: archive_batch_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.archive_batch_seq', 1, false);


--
-- Name: cfg_account_role_mapping_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cfg_account_role_mapping_id_seq', 1, false);


--
-- Name: cfg_account_role_preset_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cfg_account_role_preset_id_seq', 15, true);


--
-- Name: cfg_doc_type_mapping_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cfg_doc_type_mapping_id_seq', 3, true);


--
-- Name: cfg_doc_type_preset_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cfg_doc_type_preset_id_seq', 6, true);


--
-- Name: collection_batch_file_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.collection_batch_file_id_seq', 1, false);


--
-- Name: collection_batch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.collection_batch_id_seq', 1, false);


--
-- Name: integrity_check_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.integrity_check_id_seq', 1, false);


--
-- Name: match_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.match_log_id_seq', 1, false);


--
-- Name: period_lock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.period_lock_id_seq', 1, false);


--
-- Name: scan_folder_monitor_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.scan_folder_monitor_id_seq', 1, false);


--
-- Name: scan_workspace_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.scan_workspace_id_seq', 1, false);


--
-- Name: sys_erp_adapter_scenario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_erp_adapter_scenario_id_seq', 1, false);


--
-- Name: sys_erp_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_erp_config_id_seq', 13, true);


--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_erp_feedback_queue_id_seq', 1, false);


--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_erp_scenario_id_seq', 13, true);


--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_erp_sub_interface_id_seq', 2, true);


--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_sync_history_id_seq', 1, false);


--
-- Name: sys_sync_task_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_sync_task_id_seq', 1, false);


--
-- Name: voucher_match_result_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.voucher_match_result_id_seq', 3, true);


--
-- Name: voucher_source_link_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.voucher_source_link_id_seq', 1, false);


--
-- Name: ys_sales_out_detail_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ys_sales_out_detail_id_seq', 1, false);


--
-- Name: ys_sales_out_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ys_sales_out_id_seq', 1, false);


--
-- Name: acc_archive acc_archive_archive_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_archive_code_key UNIQUE (archive_code);


--
-- Name: acc_archive_attachment acc_archive_attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive_attachment
    ADD CONSTRAINT acc_archive_attachment_pkey PRIMARY KEY (id);


--
-- Name: acc_archive acc_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_pkey PRIMARY KEY (id);


--
-- Name: acc_archive_relation acc_archive_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_pkey PRIMARY KEY (id);


--
-- Name: acc_archive_volume acc_archive_volume_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive_volume
    ADD CONSTRAINT acc_archive_volume_pkey PRIMARY KEY (id);


--
-- Name: acc_borrow_archive acc_borrow_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_borrow_archive
    ADD CONSTRAINT acc_borrow_archive_pkey PRIMARY KEY (id);


--
-- Name: acc_borrow_log acc_borrow_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_borrow_log
    ADD CONSTRAINT acc_borrow_log_pkey PRIMARY KEY (id);


--
-- Name: acc_borrow_request acc_borrow_request_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_borrow_request
    ADD CONSTRAINT acc_borrow_request_pkey PRIMARY KEY (id);


--
-- Name: acc_borrow_request acc_borrow_request_request_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_borrow_request
    ADD CONSTRAINT acc_borrow_request_request_no_key UNIQUE (request_no);


--
-- Name: access_review access_review_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.access_review
    ADD CONSTRAINT access_review_pkey PRIMARY KEY (id);


--
-- Name: arc_abnormal_voucher arc_abnormal_voucher_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_abnormal_voucher
    ADD CONSTRAINT arc_abnormal_voucher_pkey PRIMARY KEY (id);


--
-- Name: arc_archive_batch arc_archive_batch_batch_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_archive_batch
    ADD CONSTRAINT arc_archive_batch_batch_no_key UNIQUE (batch_no);


--
-- Name: arc_archive_batch arc_archive_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_archive_batch
    ADD CONSTRAINT arc_archive_batch_pkey PRIMARY KEY (id);


--
-- Name: arc_convert_log arc_convert_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_convert_log
    ADD CONSTRAINT arc_convert_log_pkey PRIMARY KEY (id);


--
-- Name: arc_file_content arc_file_content_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_file_content
    ADD CONSTRAINT arc_file_content_pkey PRIMARY KEY (id);


--
-- Name: arc_file_metadata_index arc_file_metadata_index_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_file_metadata_index
    ADD CONSTRAINT arc_file_metadata_index_pkey PRIMARY KEY (id);


--
-- Name: arc_import_batch arc_import_batch_batch_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_import_batch
    ADD CONSTRAINT arc_import_batch_batch_no_key UNIQUE (batch_no);


--
-- Name: arc_import_batch arc_import_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_import_batch
    ADD CONSTRAINT arc_import_batch_pkey PRIMARY KEY (id);


--
-- Name: arc_original_voucher_event arc_original_voucher_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_event
    ADD CONSTRAINT arc_original_voucher_event_pkey PRIMARY KEY (id);


--
-- Name: arc_original_voucher_file arc_original_voucher_file_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_file
    ADD CONSTRAINT arc_original_voucher_file_pkey PRIMARY KEY (id);


--
-- Name: arc_original_voucher arc_original_voucher_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher
    ADD CONSTRAINT arc_original_voucher_pkey PRIMARY KEY (id);


--
-- Name: arc_original_voucher_sequence arc_original_voucher_sequence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_sequence
    ADD CONSTRAINT arc_original_voucher_sequence_pkey PRIMARY KEY (id);


--
-- Name: arc_reconciliation_record arc_reconciliation_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_reconciliation_record
    ADD CONSTRAINT arc_reconciliation_record_pkey PRIMARY KEY (id);


--
-- Name: arc_signature_log arc_signature_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_signature_log
    ADD CONSTRAINT arc_signature_log_pkey PRIMARY KEY (id);


--
-- Name: arc_voucher_relation arc_voucher_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_voucher_relation
    ADD CONSTRAINT arc_voucher_relation_pkey PRIMARY KEY (id);


--
-- Name: archive_amendment archive_amendment_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_amendment
    ADD CONSTRAINT archive_amendment_pkey PRIMARY KEY (id);


--
-- Name: archive_batch_item archive_batch_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch_item
    ADD CONSTRAINT archive_batch_item_pkey PRIMARY KEY (id);


--
-- Name: archive_batch archive_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch
    ADD CONSTRAINT archive_batch_pkey PRIMARY KEY (id);


--
-- Name: audit_inspection_log audit_inspection_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_inspection_log
    ADD CONSTRAINT audit_inspection_log_pkey PRIMARY KEY (id);


--
-- Name: auth_ticket auth_ticket_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_ticket
    ADD CONSTRAINT auth_ticket_pkey PRIMARY KEY (id);


--
-- Name: bas_erp_config bas_erp_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bas_erp_config
    ADD CONSTRAINT bas_erp_config_pkey PRIMARY KEY (id);


--
-- Name: bas_fonds bas_fonds_fonds_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bas_fonds
    ADD CONSTRAINT bas_fonds_fonds_code_key UNIQUE (fonds_code);


--
-- Name: bas_fonds bas_fonds_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bas_fonds
    ADD CONSTRAINT bas_fonds_pkey PRIMARY KEY (id);


--
-- Name: bas_location bas_location_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bas_location
    ADD CONSTRAINT bas_location_pkey PRIMARY KEY (id);


--
-- Name: biz_appraisal_list biz_appraisal_list_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_appraisal_list
    ADD CONSTRAINT biz_appraisal_list_pkey PRIMARY KEY (id);


--
-- Name: biz_archive_approval biz_archive_approval_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_archive_approval
    ADD CONSTRAINT biz_archive_approval_pkey PRIMARY KEY (id);


--
-- Name: biz_borrowing biz_borrowing_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_borrowing
    ADD CONSTRAINT biz_borrowing_pkey PRIMARY KEY (id);


--
-- Name: biz_destruction biz_destruction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_destruction
    ADD CONSTRAINT biz_destruction_pkey PRIMARY KEY (id);


--
-- Name: biz_open_appraisal biz_open_appraisal_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.biz_open_appraisal
    ADD CONSTRAINT biz_open_appraisal_pkey PRIMARY KEY (id);


--
-- Name: cfg_account_role_mapping cfg_account_role_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_account_role_mapping
    ADD CONSTRAINT cfg_account_role_mapping_pkey PRIMARY KEY (id);


--
-- Name: cfg_account_role_preset cfg_account_role_preset_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_account_role_preset
    ADD CONSTRAINT cfg_account_role_preset_pkey PRIMARY KEY (id);


--
-- Name: cfg_doc_type_mapping cfg_doc_type_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_doc_type_mapping
    ADD CONSTRAINT cfg_doc_type_mapping_pkey PRIMARY KEY (id);


--
-- Name: cfg_doc_type_preset cfg_doc_type_preset_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_doc_type_preset
    ADD CONSTRAINT cfg_doc_type_preset_pkey PRIMARY KEY (id);


--
-- Name: cfg_preset_kit cfg_preset_kit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_preset_kit
    ADD CONSTRAINT cfg_preset_kit_pkey PRIMARY KEY (id);


--
-- Name: collection_batch collection_batch_batch_no_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch
    ADD CONSTRAINT collection_batch_batch_no_key UNIQUE (batch_no);


--
-- Name: collection_batch_file collection_batch_file_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch_file
    ADD CONSTRAINT collection_batch_file_pkey PRIMARY KEY (id);


--
-- Name: collection_batch collection_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch
    ADD CONSTRAINT collection_batch_pkey PRIMARY KEY (id);


--
-- Name: destruction_log destruction_log_archive_object_id_fonds_no_archive_year_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.destruction_log
    ADD CONSTRAINT destruction_log_archive_object_id_fonds_no_archive_year_key UNIQUE (archive_object_id, fonds_no, archive_year);


--
-- Name: destruction_log destruction_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.destruction_log
    ADD CONSTRAINT destruction_log_pkey PRIMARY KEY (id, fonds_no, archive_year);


--
-- Name: employee_lifecycle_event employee_lifecycle_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee_lifecycle_event
    ADD CONSTRAINT employee_lifecycle_event_pkey PRIMARY KEY (id);


--
-- Name: file_hash_dedup_scope file_hash_dedup_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_hash_dedup_scope
    ADD CONSTRAINT file_hash_dedup_scope_pkey PRIMARY KEY (id);


--
-- Name: file_storage_policy file_storage_policy_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file_storage_policy
    ADD CONSTRAINT file_storage_policy_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: fonds_history fonds_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fonds_history
    ADD CONSTRAINT fonds_history_pkey PRIMARY KEY (id);


--
-- Name: integrity_check integrity_check_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.integrity_check
    ADD CONSTRAINT integrity_check_pkey PRIMARY KEY (id);


--
-- Name: legacy_import_task legacy_import_task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.legacy_import_task
    ADD CONSTRAINT legacy_import_task_pkey PRIMARY KEY (id);


--
-- Name: match_log match_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_log
    ADD CONSTRAINT match_log_pkey PRIMARY KEY (id);


--
-- Name: match_rule_template match_rule_template_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.match_rule_template
    ADD CONSTRAINT match_rule_template_pkey PRIMARY KEY (id);


--
-- Name: period_lock period_lock_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.period_lock
    ADD CONSTRAINT period_lock_pkey PRIMARY KEY (id);


--
-- Name: scan_folder_monitor scan_folder_monitor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_folder_monitor
    ADD CONSTRAINT scan_folder_monitor_pkey PRIMARY KEY (id);


--
-- Name: scan_workspace scan_workspace_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_workspace
    ADD CONSTRAINT scan_workspace_pkey PRIMARY KEY (id);


--
-- Name: search_performance_stats search_performance_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_performance_stats
    ADD CONSTRAINT search_performance_stats_pkey PRIMARY KEY (id);


--
-- Name: storage_capacity_stats storage_capacity_stats_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.storage_capacity_stats
    ADD CONSTRAINT storage_capacity_stats_pkey PRIMARY KEY (id);


--
-- Name: sys_archival_code_sequence sys_archival_code_sequence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_archival_code_sequence
    ADD CONSTRAINT sys_archival_code_sequence_pkey PRIMARY KEY (fonds_code, fiscal_year, category_code);


--
-- Name: sys_audit_log sys_audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_audit_log
    ADD CONSTRAINT sys_audit_log_pkey PRIMARY KEY (id);


--
-- Name: sys_entity_config sys_entity_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_entity_config
    ADD CONSTRAINT sys_entity_config_pkey PRIMARY KEY (id);


--
-- Name: sys_entity sys_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_entity
    ADD CONSTRAINT sys_entity_pkey PRIMARY KEY (id);


--
-- Name: sys_env_marker sys_env_marker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_env_marker
    ADD CONSTRAINT sys_env_marker_pkey PRIMARY KEY (marker_key);


--
-- Name: sys_erp_adapter sys_erp_adapter_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_adapter
    ADD CONSTRAINT sys_erp_adapter_pkey PRIMARY KEY (adapter_id);


--
-- Name: sys_erp_adapter_scenario sys_erp_adapter_scenario_adapter_id_scenario_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_adapter_scenario
    ADD CONSTRAINT sys_erp_adapter_scenario_adapter_id_scenario_code_key UNIQUE (adapter_id, scenario_code);


--
-- Name: sys_erp_adapter_scenario sys_erp_adapter_scenario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_adapter_scenario
    ADD CONSTRAINT sys_erp_adapter_scenario_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_config sys_erp_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_config
    ADD CONSTRAINT sys_erp_config_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_feedback_queue sys_erp_feedback_queue_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_feedback_queue
    ADD CONSTRAINT sys_erp_feedback_queue_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_scenario sys_erp_scenario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_scenario
    ADD CONSTRAINT sys_erp_scenario_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_sub_interface sys_erp_sub_interface_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_sub_interface
    ADD CONSTRAINT sys_erp_sub_interface_pkey PRIMARY KEY (id);


--
-- Name: sys_ingest_request_status sys_ingest_request_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_ingest_request_status
    ADD CONSTRAINT sys_ingest_request_status_pkey PRIMARY KEY (request_id);


--
-- Name: sys_original_voucher_type sys_original_voucher_type_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_original_voucher_type
    ADD CONSTRAINT sys_original_voucher_type_pkey PRIMARY KEY (id);


--
-- Name: sys_original_voucher_type sys_original_voucher_type_type_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_original_voucher_type
    ADD CONSTRAINT sys_original_voucher_type_type_code_key UNIQUE (type_code);


--
-- Name: sys_permission sys_permission_perm_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_perm_key_key UNIQUE (perm_key);


--
-- Name: sys_permission sys_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_pkey PRIMARY KEY (id);


--
-- Name: sys_position sys_position_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_position
    ADD CONSTRAINT sys_position_pkey PRIMARY KEY (id);


--
-- Name: sys_role sys_role_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_code_key UNIQUE (code);


--
-- Name: sys_role sys_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (id);


--
-- Name: sys_setting sys_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_setting
    ADD CONSTRAINT sys_setting_pkey PRIMARY KEY (id);


--
-- Name: sys_sql_audit_rule sys_sql_audit_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sql_audit_rule
    ADD CONSTRAINT sys_sql_audit_rule_pkey PRIMARY KEY (rule_key);


--
-- Name: sys_sync_history sys_sync_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT sys_sync_history_pkey PRIMARY KEY (id);


--
-- Name: sys_sync_task sys_sync_task_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_task
    ADD CONSTRAINT sys_sync_task_pkey PRIMARY KEY (id);


--
-- Name: sys_sync_task sys_sync_task_task_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_task
    ADD CONSTRAINT sys_sync_task_task_id_key UNIQUE (task_id);


--
-- Name: sys_user_fonds_scope sys_user_fonds_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_fonds_scope
    ADD CONSTRAINT sys_user_fonds_scope_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user_role sys_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: sys_user sys_user_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_username_key UNIQUE (username);


--
-- Name: system_performance_metrics system_performance_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.system_performance_metrics
    ADD CONSTRAINT system_performance_metrics_pkey PRIMARY KEY (id);


--
-- Name: cfg_account_role_mapping uk_account_role; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_account_role_mapping
    ADD CONSTRAINT uk_account_role UNIQUE (company_id, account_code, aux_type);


--
-- Name: archive_batch uk_archive_batch_no; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch
    ADD CONSTRAINT uk_archive_batch_no UNIQUE (batch_no);


--
-- Name: acc_archive_attachment uk_archive_file; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive_attachment
    ADD CONSTRAINT uk_archive_file UNIQUE (archive_id, file_id);


--
-- Name: sys_erp_scenario uk_config_scenario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_scenario
    ADD CONSTRAINT uk_config_scenario UNIQUE (config_id, scenario_key);


--
-- Name: cfg_doc_type_mapping uk_doc_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_doc_type_mapping
    ADD CONSTRAINT uk_doc_type UNIQUE (company_id, customer_doc_type);


--
-- Name: sys_entity_config uk_entity_config; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_entity_config
    ADD CONSTRAINT uk_entity_config UNIQUE (entity_id, config_type, config_key, deleted);


--
-- Name: bas_erp_config uk_erp_config_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bas_erp_config
    ADD CONSTRAINT uk_erp_config_name UNIQUE (name);


--
-- Name: arc_original_voucher_sequence uk_ov_seq; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_sequence
    ADD CONSTRAINT uk_ov_seq UNIQUE (fonds_code, fiscal_year, voucher_category);


--
-- Name: arc_original_voucher uk_ov_voucher_no; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher
    ADD CONSTRAINT uk_ov_voucher_no UNIQUE (fonds_code, fiscal_year, voucher_no);


--
-- Name: period_lock uk_period_lock; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.period_lock
    ADD CONSTRAINT uk_period_lock UNIQUE (fonds_id, period, lock_type);


--
-- Name: arc_voucher_relation uk_voucher_rel; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_voucher_relation
    ADD CONSTRAINT uk_voucher_rel UNIQUE (original_voucher_id, accounting_voucher_id);


--
-- Name: voucher_source_link uk_voucher_source_batch; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.voucher_source_link
    ADD CONSTRAINT uk_voucher_source_batch UNIQUE (voucher_id, source_doc_id, match_batch_id);


--
-- Name: user_mfa_config user_mfa_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_mfa_config
    ADD CONSTRAINT user_mfa_config_pkey PRIMARY KEY (id);


--
-- Name: user_mfa_config user_mfa_config_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_mfa_config
    ADD CONSTRAINT user_mfa_config_user_id_key UNIQUE (user_id);


--
-- Name: voucher_match_result voucher_match_result_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.voucher_match_result
    ADD CONSTRAINT voucher_match_result_pkey PRIMARY KEY (id);


--
-- Name: voucher_source_link voucher_source_link_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.voucher_source_link
    ADD CONSTRAINT voucher_source_link_pkey PRIMARY KEY (id);


--
-- Name: ys_sales_out_detail ys_sales_out_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out_detail
    ADD CONSTRAINT ys_sales_out_detail_pkey PRIMARY KEY (id);


--
-- Name: ys_sales_out ys_sales_out_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out
    ADD CONSTRAINT ys_sales_out_pkey PRIMARY KEY (id);


--
-- Name: ys_sales_out ys_sales_out_ys_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out
    ADD CONSTRAINT ys_sales_out_ys_id_key UNIQUE (ys_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_abnormal_create_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_abnormal_create_time ON public.arc_abnormal_voucher USING btree (create_time);


--
-- Name: idx_abnormal_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_abnormal_status ON public.arc_abnormal_voucher USING btree (status);


--
-- Name: idx_abnormal_voucher_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_abnormal_voucher_fonds ON public.arc_abnormal_voucher USING btree (fonds_code);


--
-- Name: idx_acc_archive_batch_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_batch_id ON public.acc_archive USING btree (batch_id);


--
-- Name: idx_acc_archive_category_booktype; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_category_booktype ON public.acc_archive USING btree (category_code, ((custom_metadata ->> 'bookType'::text))) WHERE (((category_code)::text = 'AC02'::text) AND ((custom_metadata ->> 'bookType'::text) IS NOT NULL));


--
-- Name: INDEX idx_acc_archive_category_booktype; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_category_booktype IS '分类号 + bookType 复合表达式索引，优化 AC02 类别查询';


--
-- Name: idx_acc_archive_category_reporttype; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_category_reporttype ON public.acc_archive USING btree (category_code, ((custom_metadata ->> 'reportType'::text))) WHERE (((category_code)::text = 'AC03'::text) AND ((custom_metadata ->> 'reportType'::text) IS NOT NULL));


--
-- Name: INDEX idx_acc_archive_category_reporttype; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_category_reporttype IS '分类号 + reportType 复合表达式索引，优化 AC03 类别查询';


--
-- Name: idx_acc_archive_custom_metadata_path_ops; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_custom_metadata_path_ops ON public.acc_archive USING gin (custom_metadata jsonb_path_ops);


--
-- Name: INDEX idx_acc_archive_custom_metadata_path_ops; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_custom_metadata_path_ops IS 'custom_metadata jsonb_path_ops GIN 索引，优化 @> 包含查询，索引体积更小';


--
-- Name: idx_acc_archive_custom_other_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_custom_other_type ON public.acc_archive USING btree (((custom_metadata ->> 'otherType'::text))) WHERE ((custom_metadata ->> 'otherType'::text) IS NOT NULL);


--
-- Name: INDEX idx_acc_archive_custom_other_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_custom_other_type IS 'AC04 其他材料类型表达式索引，优化 otherType 精确查询';


--
-- Name: idx_acc_archive_custom_report_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_custom_report_type ON public.acc_archive USING btree (((custom_metadata ->> 'reportType'::text))) WHERE ((custom_metadata ->> 'reportType'::text) IS NOT NULL);


--
-- Name: INDEX idx_acc_archive_custom_report_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_custom_report_type IS 'AC03 报税表单类型表达式索引，优化 reportType 精确查询';


--
-- Name: idx_acc_archive_department_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_department_id ON public.acc_archive USING btree (department_id) WHERE (department_id IS NOT NULL);


--
-- Name: INDEX idx_acc_archive_department_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_department_id IS '部门ID部分索引，用于按部门筛选档案查询优化';


--
-- Name: idx_acc_archive_fiscal_period_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_fiscal_period_year ON public.acc_archive USING btree (fiscal_period, fiscal_year) WHERE (deleted = 0);


--
-- Name: INDEX idx_acc_archive_fiscal_period_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_fiscal_period_year IS '会计期间+年度复合索引，用于期间维度查询优化（仅未删除记录）';


--
-- Name: idx_acc_archive_fonds_category_booktype; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_fonds_category_booktype ON public.acc_archive USING btree (fonds_no, category_code, ((custom_metadata ->> 'bookType'::text))) WHERE (((category_code)::text = 'AC02'::text) AND ((custom_metadata ->> 'bookType'::text) IS NOT NULL));


--
-- Name: INDEX idx_acc_archive_fonds_category_booktype; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_fonds_category_booktype IS '全宗号 + 分类号 + bookType 复合表达式索引，优化全宗隔离下的分类查询';


--
-- Name: idx_acc_archive_fonds_status_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_fonds_status_year ON public.acc_archive USING btree (fonds_no, status, fiscal_year) WHERE (deleted = 0);


--
-- Name: INDEX idx_acc_archive_fonds_status_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_fonds_status_year IS '全宗+状态+年度复合索引，用于全宗维度状态查询和统计优化（仅未删除记录）';


--
-- Name: idx_acc_archive_standard_buyer_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_standard_buyer_name ON public.acc_archive USING btree (((standard_metadata ->> 'buyerName'::text))) WHERE ((standard_metadata ->> 'buyerName'::text) IS NOT NULL);


--
-- Name: INDEX idx_acc_archive_standard_buyer_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_standard_buyer_name IS 'standard_metadata 购方名称表达式索引，优化发票检索';


--
-- Name: idx_acc_archive_standard_invoice_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_standard_invoice_number ON public.acc_archive USING btree (((standard_metadata ->> 'invoiceNumber'::text))) WHERE ((standard_metadata ->> 'invoiceNumber'::text) IS NOT NULL);


--
-- Name: INDEX idx_acc_archive_standard_invoice_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_standard_invoice_number IS 'standard_metadata 发票号表达式索引，用于发票号匹配策略';


--
-- Name: idx_acc_archive_standard_metadata_path_ops; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_standard_metadata_path_ops ON public.acc_archive USING gin (standard_metadata jsonb_path_ops);


--
-- Name: INDEX idx_acc_archive_standard_metadata_path_ops; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_standard_metadata_path_ops IS 'standard_metadata jsonb_path_ops GIN 索引，优化 @> 包含查询，索引体积更小';


--
-- Name: idx_acc_archive_standard_seller_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_standard_seller_name ON public.acc_archive USING btree (((standard_metadata ->> 'sellerName'::text))) WHERE ((standard_metadata ->> 'sellerName'::text) IS NOT NULL);


--
-- Name: INDEX idx_acc_archive_standard_seller_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_acc_archive_standard_seller_name IS 'standard_metadata 销方名称表达式索引，优化发票检索';


--
-- Name: idx_access_review_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_access_review_date ON public.access_review USING btree (review_date, status, deleted);


--
-- Name: idx_access_review_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_access_review_user ON public.access_review USING btree (user_id, status, deleted);


--
-- Name: idx_account_mapping_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_mapping_company ON public.cfg_account_role_mapping USING btree (company_id);


--
-- Name: idx_account_preset_kit; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_preset_kit ON public.cfg_account_role_preset USING btree (kit_id);


--
-- Name: idx_appraisal_list_fonds_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appraisal_list_fonds_year ON public.biz_appraisal_list USING btree (fonds_no, archive_year, appraisal_date);


--
-- Name: idx_appraisal_list_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_appraisal_list_status ON public.biz_appraisal_list USING btree (status, created_time);


--
-- Name: idx_arc_file_content_batch_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arc_file_content_batch_status ON public.arc_file_content USING btree (batch_id, pre_archive_status) WHERE (batch_id IS NOT NULL);


--
-- Name: INDEX idx_arc_file_content_batch_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_arc_file_content_batch_status IS '批次ID+预归档状态复合索引，用于批次处理状态查询优化';


--
-- Name: idx_arc_file_content_fiscal_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arc_file_content_fiscal_year ON public.arc_file_content USING btree (fiscal_year);


--
-- Name: idx_arc_file_content_fonds_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arc_file_content_fonds_status ON public.arc_file_content USING btree (fonds_code, pre_archive_status) WHERE (fonds_code IS NOT NULL);


--
-- Name: INDEX idx_arc_file_content_fonds_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_arc_file_content_fonds_status IS '全宗代码+预归档状态复合索引，用于全宗维度预归档状态查询优化';


--
-- Name: idx_arc_file_content_pre_archive_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arc_file_content_pre_archive_status ON public.arc_file_content USING btree (pre_archive_status);


--
-- Name: INDEX idx_arc_file_content_pre_archive_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_arc_file_content_pre_archive_status IS '预归档状态索引 - 用于仪表板统计和筛选';


--
-- Name: idx_archival_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archival_code ON public.arc_file_content USING btree (archival_code);


--
-- Name: idx_archive_amendment_archive; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_amendment_archive ON public.archive_amendment USING btree (archive_id);


--
-- Name: idx_archive_amendment_batch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_amendment_batch ON public.archive_amendment USING btree (batch_id);


--
-- Name: idx_archive_amount; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_amount ON public.acc_archive USING btree (fonds_no, fiscal_year, amount);


--
-- Name: idx_archive_approval_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_approval_applicant ON public.biz_archive_approval USING btree (applicant_id);


--
-- Name: idx_archive_approval_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_approval_archive_id ON public.biz_archive_approval USING btree (archive_id);


--
-- Name: idx_archive_approval_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_approval_created_at ON public.biz_archive_approval USING btree (created_at);


--
-- Name: idx_archive_approval_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_approval_status ON public.biz_archive_approval USING btree (status);


--
-- Name: idx_archive_archived_fonds_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_archived_fonds_year ON public.acc_archive USING btree (fonds_no, fiscal_year) WHERE ((status)::text = 'archived'::text);


--
-- Name: INDEX idx_archive_archived_fonds_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_archive_archived_fonds_year IS '已归档档案的部分索引，优化常用查询';


--
-- Name: idx_archive_attachment_archive; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_attachment_archive ON public.acc_archive_attachment USING btree (archive_id);


--
-- Name: idx_archive_attachment_file; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_attachment_file ON public.acc_archive_attachment USING btree (file_id);


--
-- Name: idx_archive_batch_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_batch_created ON public.archive_batch USING btree (created_at);


--
-- Name: idx_archive_batch_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_batch_fonds ON public.archive_batch USING btree (fonds_id);


--
-- Name: idx_archive_batch_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_batch_period ON public.archive_batch USING btree (period_start, period_end);


--
-- Name: idx_archive_batch_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_batch_status ON public.archive_batch USING btree (status);


--
-- Name: idx_archive_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_category ON public.acc_archive USING btree (category_code);


--
-- Name: idx_archive_category_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_category_code ON public.acc_archive USING btree (category_code);


--
-- Name: idx_archive_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_code ON public.acc_archive USING btree (archive_code);


--
-- Name: idx_archive_counterparty; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_counterparty ON public.acc_archive USING btree (fonds_no, fiscal_year, counterparty);


--
-- Name: idx_archive_custom_booktype; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_custom_booktype ON public.acc_archive USING btree (((custom_metadata ->> 'bookType'::text))) WHERE ((custom_metadata ->> 'bookType'::text) IS NOT NULL);


--
-- Name: INDEX idx_archive_custom_booktype; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_archive_custom_booktype IS '凭证册类型表达式索引，优化 AC02 类别的 bookType 精确查询';


--
-- Name: idx_archive_custom_metadata_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_custom_metadata_gin ON public.acc_archive USING gin (custom_metadata);


--
-- Name: INDEX idx_archive_custom_metadata_gin; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_archive_custom_metadata_gin IS 'custom_metadata JSONB GIN 索引，用于 JSONB 包含和键值查询';


--
-- Name: idx_archive_destruction_hold; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_destruction_hold ON public.acc_archive USING btree (destruction_hold);


--
-- Name: idx_archive_doc_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_doc_date ON public.acc_archive USING btree (fonds_no, fiscal_year, doc_date);


--
-- Name: idx_archive_fiscal_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_fiscal_year ON public.acc_archive USING btree (fiscal_year);


--
-- Name: idx_archive_fonds_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_fonds_no ON public.acc_archive USING btree (fonds_no);


--
-- Name: idx_archive_fonds_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_fonds_status ON public.acc_archive USING btree (fonds_no, status) WHERE ((status)::text = ANY (ARRAY[('archived'::character varying)::text, ('pending'::character varying)::text]));


--
-- Name: idx_archive_fonds_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_fonds_year ON public.acc_archive USING btree (fonds_no, fiscal_year);


--
-- Name: idx_archive_fonds_year_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_fonds_year_category ON public.acc_archive USING btree (fonds_no, fiscal_year, category_code);


--
-- Name: INDEX idx_archive_fonds_year_category; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_archive_fonds_year_category IS '全宗号+年度+分类号复合索引，用于多条件组合查询';


--
-- Name: idx_archive_invoice_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_invoice_no ON public.acc_archive USING btree (fonds_no, fiscal_year, invoice_no);


--
-- Name: idx_archive_retention_expiration; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_retention_expiration ON public.acc_archive USING btree (retention_period, retention_start_date, destruction_status) WHERE ((destruction_status)::text = ANY (ARRAY[('NORMAL'::character varying)::text, ('EXPIRED'::character varying)::text]));


--
-- Name: idx_archive_standard_metadata_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_standard_metadata_gin ON public.acc_archive USING gin (standard_metadata);


--
-- Name: INDEX idx_archive_standard_metadata_gin; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_archive_standard_metadata_gin IS 'standard_metadata JSONB GIN 索引，用于 JSONB 包含和键值查询';


--
-- Name: idx_archive_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_status ON public.acc_archive USING btree (status);


--
-- Name: idx_archive_volume_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_volume_id ON public.acc_archive USING btree (volume_id);


--
-- Name: idx_archive_voucher_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_voucher_no ON public.acc_archive USING btree (fonds_no, fiscal_year, voucher_no);


--
-- Name: idx_archive_year_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_year_status ON public.acc_archive USING btree (fiscal_year, status);


--
-- Name: idx_audit_inspection_archive_compliance; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_inspection_archive_compliance ON public.audit_inspection_log USING btree (archive_id, is_compliant);


--
-- Name: idx_audit_inspection_compliance; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_inspection_compliance ON public.audit_inspection_log USING btree (is_compliant, inspection_time);


--
-- Name: idx_audit_log_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_created_at ON public.sys_audit_log USING btree (created_time);


--
-- Name: idx_audit_log_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_log_hash ON public.sys_audit_log USING btree (log_hash);


--
-- Name: idx_auth_ticket_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_ticket_applicant ON public.auth_ticket USING btree (applicant_id, status, deleted);


--
-- Name: idx_auth_ticket_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_ticket_expires ON public.auth_ticket USING btree (expires_at, status, deleted);


--
-- Name: idx_auth_ticket_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_ticket_status ON public.auth_ticket USING btree (status, deleted);


--
-- Name: idx_auth_ticket_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_auth_ticket_target ON public.auth_ticket USING btree (target_fonds, status, expires_at, deleted);


--
-- Name: idx_bas_fonds_org; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bas_fonds_org ON public.bas_fonds USING btree (org_id);


--
-- Name: idx_batch_item_batch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_batch_item_batch ON public.archive_batch_item USING btree (batch_id);


--
-- Name: idx_batch_item_type_ref; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_batch_item_type_ref ON public.archive_batch_item USING btree (item_type, ref_id);


--
-- Name: idx_batch_sequence; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_batch_sequence ON public.arc_archive_batch USING btree (batch_sequence);


--
-- Name: idx_batch_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_batch_status ON public.arc_import_batch USING btree (status);


--
-- Name: idx_batch_tenant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_batch_tenant ON public.arc_import_batch USING btree (tenant_id);


--
-- Name: idx_borrow_archive_archive; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_archive_archive ON public.acc_borrow_archive USING btree (archive_id);


--
-- Name: idx_borrow_archive_request; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_archive_request ON public.acc_borrow_archive USING btree (borrow_request_id);


--
-- Name: idx_borrow_log_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_log_applicant ON public.acc_borrow_log USING btree (applicant_id);


--
-- Name: idx_borrow_log_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_log_dates ON public.acc_borrow_log USING btree (borrow_start_date, borrow_end_date);


--
-- Name: idx_borrow_request_applicant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_request_applicant ON public.acc_borrow_request USING btree (applicant_id);


--
-- Name: idx_borrow_request_dates; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_request_dates ON public.acc_borrow_request USING btree (expected_start_date, expected_end_date);


--
-- Name: idx_borrow_request_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrow_request_status ON public.acc_borrow_request USING btree (status);


--
-- Name: idx_borrowing_fonds_year_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrowing_fonds_year_status ON public.biz_borrowing USING btree (fonds_no, archive_year, status);


--
-- Name: idx_borrowing_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrowing_status ON public.biz_borrowing USING btree (status);


--
-- Name: idx_borrowing_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrowing_user ON public.biz_borrowing USING btree (user_id);


--
-- Name: idx_collection_batch_created_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_created_time ON public.collection_batch USING btree (created_time DESC);


--
-- Name: INDEX idx_collection_batch_created_time; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_created_time IS '创建时间索引，用于时间排序和查询';


--
-- Name: idx_collection_batch_file_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_file_archive_id ON public.collection_batch_file USING btree (archive_id);


--
-- Name: idx_collection_batch_file_batch_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_file_batch_id ON public.collection_batch_file USING btree (batch_id);


--
-- Name: INDEX idx_collection_batch_file_batch_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_file_batch_id IS '批次查询索引';


--
-- Name: idx_collection_batch_file_batch_name; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_collection_batch_file_batch_name ON public.collection_batch_file USING btree (batch_id, original_filename) WHERE ((upload_status)::text <> ALL (ARRAY[('FAILED'::character varying)::text, ('DUPLICATE'::character varying)::text]));


--
-- Name: INDEX idx_collection_batch_file_batch_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_file_batch_name IS '批次内文件名唯一约束索引';


--
-- Name: idx_collection_batch_file_file_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_file_file_id ON public.collection_batch_file USING btree (file_id);


--
-- Name: INDEX idx_collection_batch_file_file_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_file_file_id IS '文件关联索引';


--
-- Name: idx_collection_batch_file_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_file_hash ON public.collection_batch_file USING btree (file_hash);


--
-- Name: INDEX idx_collection_batch_file_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_file_hash IS '文件哈希去重索引';


--
-- Name: idx_collection_batch_file_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_file_status ON public.collection_batch_file USING btree (upload_status);


--
-- Name: INDEX idx_collection_batch_file_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_file_status IS '上传状态索引';


--
-- Name: idx_collection_batch_fiscal_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_fiscal_year ON public.collection_batch USING btree (fiscal_year);


--
-- Name: INDEX idx_collection_batch_fiscal_year; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_fiscal_year IS '会计年度索引，用于按年度筛选';


--
-- Name: idx_collection_batch_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_fonds ON public.collection_batch USING btree (fonds_id);


--
-- Name: INDEX idx_collection_batch_fonds; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_fonds IS '全宗ID索引，用于按全宗筛选批次';


--
-- Name: idx_collection_batch_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_collection_batch_status ON public.collection_batch USING btree (status);


--
-- Name: INDEX idx_collection_batch_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON INDEX public.idx_collection_batch_status IS '批次状态索引，用于按状态筛选';


--
-- Name: idx_convert_log_archive; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_convert_log_archive ON public.arc_convert_log USING btree (archive_id);


--
-- Name: idx_convert_log_convert_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_convert_log_convert_time ON public.arc_convert_log USING btree (convert_time);


--
-- Name: idx_convert_log_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_convert_log_status ON public.arc_convert_log USING btree (status);


--
-- Name: idx_convert_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_convert_log_time ON public.arc_convert_log USING btree (created_time);


--
-- Name: idx_created_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_created_time ON public.arc_file_content USING btree (created_time);


--
-- Name: idx_destruction_log_destroyed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destruction_log_destroyed_at ON public.destruction_log USING btree (destroyed_at);


--
-- Name: idx_destruction_log_fonds_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destruction_log_fonds_year ON public.destruction_log USING btree (fonds_no, archive_year, destroyed_at);


--
-- Name: idx_destruction_log_trace_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destruction_log_trace_id ON public.destruction_log USING btree (trace_id);


--
-- Name: idx_destruction_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_destruction_status ON public.biz_destruction USING btree (status);


--
-- Name: idx_doc_mapping_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_mapping_company ON public.cfg_doc_type_mapping USING btree (company_id);


--
-- Name: idx_doc_preset_kit; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_doc_preset_kit ON public.cfg_doc_type_preset USING btree (kit_id);


--
-- Name: idx_employee_lifecycle_event_employee; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employee_lifecycle_event_employee ON public.employee_lifecycle_event USING btree (employee_id, processed, deleted);


--
-- Name: idx_employee_lifecycle_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employee_lifecycle_event_type ON public.employee_lifecycle_event USING btree (event_type, event_date, deleted);


--
-- Name: idx_entity_config_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_config_deleted ON public.sys_entity_config USING btree (deleted);


--
-- Name: idx_entity_config_entity; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_config_entity ON public.sys_entity_config USING btree (entity_id);


--
-- Name: idx_entity_config_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_config_type ON public.sys_entity_config USING btree (config_type);


--
-- Name: idx_entity_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_deleted ON public.sys_entity USING btree (deleted);


--
-- Name: idx_entity_parent_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_parent_id ON public.sys_entity USING btree (parent_id);


--
-- Name: idx_entity_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_status ON public.sys_entity USING btree (status);


--
-- Name: idx_entity_tax_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_entity_tax_id ON public.sys_entity USING btree (tax_id);


--
-- Name: idx_erp_adapter_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_adapter_enabled ON public.sys_erp_adapter USING btree (enabled);


--
-- Name: idx_erp_adapter_scenario_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_adapter_scenario_code ON public.sys_erp_adapter_scenario USING btree (scenario_code);


--
-- Name: idx_erp_adapter_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_adapter_type ON public.sys_erp_adapter USING btree (erp_type);


--
-- Name: idx_erp_config_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_config_enabled ON public.bas_erp_config USING btree (enabled);


--
-- Name: idx_erp_config_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_config_type ON public.bas_erp_config USING btree (adapter_type);


--
-- Name: idx_event_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_action ON public.arc_original_voucher_event USING btree (action);


--
-- Name: idx_event_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_time ON public.arc_original_voucher_event USING btree (occurred_at);


--
-- Name: idx_event_voucher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_voucher ON public.arc_original_voucher_event USING btree (voucher_id);


--
-- Name: idx_feedback_queue_erp_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_queue_erp_type ON public.sys_erp_feedback_queue USING btree (erp_type);


--
-- Name: idx_feedback_queue_next_retry; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_queue_next_retry ON public.sys_erp_feedback_queue USING btree (next_retry_time) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: idx_feedback_queue_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_feedback_queue_status ON public.sys_erp_feedback_queue USING btree (status);


--
-- Name: idx_file_hash_dedup_scope_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_hash_dedup_scope_fonds ON public.file_hash_dedup_scope USING btree (fonds_no, enabled, deleted);


--
-- Name: idx_file_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_id ON public.arc_file_metadata_index USING btree (file_id);


--
-- Name: idx_file_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_item_id ON public.arc_file_content USING btree (item_id);


--
-- Name: idx_file_storage_policy_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_storage_policy_fonds ON public.file_storage_policy USING btree (fonds_no, enabled, deleted);


--
-- Name: idx_file_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_type ON public.arc_file_content USING btree (file_type);


--
-- Name: idx_fonds_entity_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fonds_entity_id ON public.bas_fonds USING btree (entity_id);


--
-- Name: idx_fonds_history_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fonds_history_event_type ON public.fonds_history USING btree (event_type, effective_date, deleted);


--
-- Name: idx_fonds_history_fonds_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fonds_history_fonds_no ON public.fonds_history USING btree (fonds_no, deleted);


--
-- Name: idx_fonds_history_from_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fonds_history_from_fonds ON public.fonds_history USING btree (from_fonds_no, deleted);


--
-- Name: idx_fonds_history_to_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fonds_history_to_fonds ON public.fonds_history USING btree (to_fonds_no, deleted);


--
-- Name: idx_import_task_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_task_fonds ON public.legacy_import_task USING btree (fonds_no, created_at);


--
-- Name: idx_import_task_operator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_task_operator ON public.legacy_import_task USING btree (operator_id, created_at);


--
-- Name: idx_import_task_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_import_task_status ON public.legacy_import_task USING btree (status, created_at);


--
-- Name: idx_ingest_request_fonds_no; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ingest_request_fonds_no ON public.sys_ingest_request_status USING btree (fonds_no);


--
-- Name: idx_integrity_check_result; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_integrity_check_result ON public.integrity_check USING btree (result);


--
-- Name: idx_integrity_check_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_integrity_check_target ON public.integrity_check USING btree (target_type, target_id);


--
-- Name: idx_invoice_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_invoice_number ON public.arc_file_metadata_index USING btree (invoice_number);


--
-- Name: idx_issue_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_issue_date ON public.arc_file_metadata_index USING btree (issue_date);


--
-- Name: idx_link_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_link_source ON public.voucher_source_link USING btree (source_doc_id);


--
-- Name: idx_link_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_link_status ON public.voucher_source_link USING btree (status);


--
-- Name: idx_link_voucher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_link_voucher ON public.voucher_source_link USING btree (voucher_id);


--
-- Name: idx_location_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_location_parent ON public.bas_location USING btree (parent_id);


--
-- Name: idx_location_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_location_type ON public.bas_location USING btree (type);


--
-- Name: idx_match_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_match_log_time ON public.match_log USING btree (operation_time);


--
-- Name: idx_match_log_voucher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_match_log_voucher ON public.match_log USING btree (voucher_id);


--
-- Name: idx_open_appraisal_archive_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_open_appraisal_archive_id ON public.biz_open_appraisal USING btree (archive_id);


--
-- Name: idx_open_appraisal_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_open_appraisal_created_at ON public.biz_open_appraisal USING btree (created_at);


--
-- Name: idx_open_appraisal_result; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_open_appraisal_result ON public.biz_open_appraisal USING btree (appraisal_result);


--
-- Name: idx_open_appraisal_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_open_appraisal_status ON public.biz_open_appraisal USING btree (status);


--
-- Name: idx_orig_voucher_amount_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orig_voucher_amount_date ON public.arc_original_voucher USING btree (amount, business_date);


--
-- Name: idx_orig_voucher_scene_search; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orig_voucher_scene_search ON public.arc_original_voucher USING btree (voucher_type, business_date, amount);


--
-- Name: idx_ov_batch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_batch ON public.arc_original_voucher USING btree (pool_batch_id);


--
-- Name: idx_ov_counterparty; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_counterparty ON public.arc_original_voucher USING btree (counterparty);


--
-- Name: idx_ov_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_date ON public.arc_original_voucher USING btree (business_date);


--
-- Name: idx_ov_pool_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_pool_status ON public.arc_original_voucher USING btree (pool_status);


--
-- Name: idx_ov_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_source ON public.arc_original_voucher USING btree (source_system, source_doc_id);


--
-- Name: idx_ov_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_status ON public.arc_original_voucher USING btree (archive_status);


--
-- Name: idx_ov_tenant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_tenant ON public.arc_original_voucher USING btree (tenant_id);


--
-- Name: idx_ov_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ov_type ON public.arc_original_voucher USING btree (voucher_category, voucher_type);


--
-- Name: idx_ovf_hash; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ovf_hash ON public.arc_original_voucher_file USING btree (file_hash);


--
-- Name: idx_ovf_voucher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ovf_voucher ON public.arc_original_voucher_file USING btree (voucher_id);


--
-- Name: idx_performance_metrics_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_performance_metrics_fonds ON public.system_performance_metrics USING btree (fonds_no, recorded_at);


--
-- Name: idx_performance_metrics_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_performance_metrics_type ON public.system_performance_metrics USING btree (metric_type, recorded_at);


--
-- Name: idx_period_lock_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_period_lock_fonds ON public.period_lock USING btree (fonds_id);


--
-- Name: idx_period_lock_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_period_lock_period ON public.period_lock USING btree (period);


--
-- Name: idx_permission_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permission_group ON public.sys_permission USING btree (group_name);


--
-- Name: idx_permission_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_permission_key ON public.sys_permission USING btree (perm_key);


--
-- Name: idx_position_department; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_position_department ON public.sys_position USING btree (department_id) WHERE (deleted = 0);


--
-- Name: idx_position_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_position_status ON public.sys_position USING btree (status) WHERE (deleted = 0);


--
-- Name: idx_recon_record_config; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recon_record_config ON public.arc_reconciliation_record USING btree (config_id);


--
-- Name: idx_recon_record_operator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recon_record_operator ON public.arc_reconciliation_record USING btree (operator_id);


--
-- Name: idx_recon_record_range; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_recon_record_range ON public.arc_reconciliation_record USING btree (recon_start_date, recon_end_date);


--
-- Name: idx_result_batch_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_result_batch_task ON public.voucher_match_result USING btree (batch_task_id);


--
-- Name: idx_result_latest; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_result_latest ON public.voucher_match_result USING btree (voucher_id, is_latest) WHERE (is_latest = true);


--
-- Name: idx_result_task; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_result_task ON public.voucher_match_result USING btree (task_id);


--
-- Name: idx_result_voucher; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_result_voucher ON public.voucher_match_result USING btree (voucher_id);


--
-- Name: idx_result_voucher_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_result_voucher_status ON public.voucher_match_result USING btree (voucher_id, status, is_latest);


--
-- Name: idx_scan_folder_monitor_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_folder_monitor_active ON public.scan_folder_monitor USING btree (is_active);


--
-- Name: idx_scan_folder_monitor_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_folder_monitor_user ON public.scan_folder_monitor USING btree (user_id);


--
-- Name: idx_scan_workspace_archive; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_workspace_archive ON public.scan_workspace USING btree (archive_id);


--
-- Name: idx_scan_workspace_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_workspace_created ON public.scan_workspace USING btree (created_at DESC);


--
-- Name: idx_scan_workspace_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_workspace_fonds ON public.scan_workspace USING btree (fonds_code);


--
-- Name: idx_scan_workspace_session; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_workspace_session ON public.scan_workspace USING btree (session_id);


--
-- Name: idx_scan_workspace_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_workspace_status ON public.scan_workspace USING btree (ocr_status, submit_status);


--
-- Name: idx_scan_workspace_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_scan_workspace_user ON public.scan_workspace USING btree (user_id);


--
-- Name: idx_search_performance_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_search_performance_fonds ON public.search_performance_stats USING btree (fonds_no, recorded_at);


--
-- Name: idx_search_performance_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_search_performance_type ON public.search_performance_stats USING btree (search_type, recorded_at);


--
-- Name: idx_seller_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_seller_name ON public.arc_file_metadata_index USING btree (seller_name);


--
-- Name: idx_signature_archive; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_signature_archive ON public.arc_signature_log USING btree (archive_id);


--
-- Name: idx_signature_file; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_signature_file ON public.arc_signature_log USING btree (file_id);


--
-- Name: idx_signature_verify_result; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_signature_verify_result ON public.arc_signature_log USING btree (verify_result);


--
-- Name: idx_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_status ON public.sys_ingest_request_status USING btree (status);


--
-- Name: idx_storage_capacity_fonds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_storage_capacity_fonds ON public.storage_capacity_stats USING btree (fonds_no, recorded_at);


--
-- Name: idx_sub_interface_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_sub_interface_unique ON public.sys_erp_sub_interface USING btree (scenario_id, interface_key);


--
-- Name: idx_sync_history_operator; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_history_operator ON public.sys_sync_history USING btree (operator_id);


--
-- Name: idx_sync_history_scenario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_history_scenario ON public.sys_sync_history USING btree (scenario_id);


--
-- Name: idx_sync_history_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_history_time ON public.sys_sync_history USING btree (sync_start_time DESC);


--
-- Name: idx_sync_task_created_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_task_created_time ON public.sys_sync_task USING btree (created_time DESC);


--
-- Name: idx_sync_task_scenario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_task_scenario_id ON public.sys_sync_task USING btree (scenario_id);


--
-- Name: idx_sync_task_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_task_status ON public.sys_sync_task USING btree (status);


--
-- Name: idx_sync_task_task_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sync_task_task_id ON public.sys_sync_task USING btree (task_id);


--
-- Name: idx_sys_setting_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_setting_category ON public.sys_setting USING btree (category);


--
-- Name: idx_sys_user_role_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_user_role_role ON public.sys_user_role USING btree (role_id);


--
-- Name: idx_sys_user_role_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_user_role_user ON public.sys_user_role USING btree (user_id);


--
-- Name: idx_template_scene; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_scene ON public.match_rule_template USING btree (scene);


--
-- Name: idx_template_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_template_status ON public.match_rule_template USING btree (status);


--
-- Name: idx_user_fonds_scope_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_fonds_scope_user ON public.sys_user_fonds_scope USING btree (user_id, deleted);


--
-- Name: idx_volume_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_volume_category ON public.acc_archive_volume USING btree (category_code);


--
-- Name: idx_volume_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_volume_code ON public.acc_archive_volume USING btree (volume_code);


--
-- Name: idx_volume_fiscal_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_volume_fiscal_period ON public.acc_archive_volume USING btree (fiscal_period);


--
-- Name: idx_volume_fonds_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_volume_fonds_year ON public.acc_archive_volume USING btree (fonds_no, fiscal_year);


--
-- Name: idx_volume_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_volume_period ON public.acc_archive_volume USING btree (fiscal_period);


--
-- Name: idx_volume_volume_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_volume_volume_status ON public.acc_archive_volume USING btree (volume_status);


--
-- Name: idx_vr_accounting; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vr_accounting ON public.arc_voucher_relation USING btree (accounting_voucher_id);


--
-- Name: idx_vr_original; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vr_original ON public.arc_voucher_relation USING btree (original_voucher_id);


--
-- Name: idx_ys_sales_out_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ys_sales_out_code ON public.ys_sales_out USING btree (code);


--
-- Name: idx_ys_sales_out_detail_sales_out_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ys_sales_out_detail_sales_out_id ON public.ys_sales_out_detail USING btree (sales_out_id);


--
-- Name: idx_ys_sales_out_vouchdate; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ys_sales_out_vouchdate ON public.ys_sales_out USING btree (vouchdate);


--
-- Name: uk_position_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_position_code ON public.sys_position USING btree (code) WHERE (deleted = 0);


--
-- Name: uk_user_fonds_scope; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uk_user_fonds_scope ON public.sys_user_fonds_scope USING btree (user_id, fonds_no) WHERE (deleted = 0);


--
-- Name: uq_recon_record_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_recon_record_key ON public.arc_reconciliation_record USING btree (config_id, subject_code, recon_start_date, recon_end_date) WHERE ((config_id IS NOT NULL) AND (recon_start_date IS NOT NULL) AND (recon_end_date IS NOT NULL));


--
-- Name: ux_acc_archive_unique_biz_id_not_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_acc_archive_unique_biz_id_not_deleted ON public.acc_archive USING btree (unique_biz_id) WHERE (deleted = 0);


--
-- Name: destruction_log destruction_log_readonly_trigger; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER destruction_log_readonly_trigger BEFORE DELETE OR UPDATE ON public.destruction_log FOR EACH ROW EXECUTE FUNCTION public.prevent_destruction_log_modification();


--
-- Name: arc_original_voucher trg_ov_row_version; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ov_row_version BEFORE UPDATE ON public.arc_original_voucher FOR EACH ROW EXECUTE FUNCTION public.update_row_version();


--
-- Name: sys_entity_config trigger_update_entity_config_updated_time; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_update_entity_config_updated_time BEFORE UPDATE ON public.sys_entity_config FOR EACH ROW EXECUTE FUNCTION public.update_sys_entity_config_updated_time();


--
-- Name: sys_entity trigger_update_sys_entity_updated_time; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_update_sys_entity_updated_time BEFORE UPDATE ON public.sys_entity FOR EACH ROW EXECUTE FUNCTION public.update_sys_entity_updated_time();


--
-- Name: acc_archive_relation acc_archive_relation_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: acc_archive_relation acc_archive_relation_target_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_target_id_fkey FOREIGN KEY (target_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: arc_file_metadata_index arc_file_metadata_index_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_file_metadata_index
    ADD CONSTRAINT arc_file_metadata_index_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.arc_file_content(id) ON DELETE CASCADE;


--
-- Name: audit_inspection_log audit_inspection_log_archive_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_inspection_log
    ADD CONSTRAINT audit_inspection_log_archive_id_fkey FOREIGN KEY (archive_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: cfg_account_role_preset cfg_account_role_preset_kit_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_account_role_preset
    ADD CONSTRAINT cfg_account_role_preset_kit_id_fkey FOREIGN KEY (kit_id) REFERENCES public.cfg_preset_kit(id);


--
-- Name: cfg_doc_type_preset cfg_doc_type_preset_kit_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cfg_doc_type_preset
    ADD CONSTRAINT cfg_doc_type_preset_kit_id_fkey FOREIGN KEY (kit_id) REFERENCES public.cfg_preset_kit(id);


--
-- Name: collection_batch_file collection_batch_file_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch_file
    ADD CONSTRAINT collection_batch_file_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.collection_batch(id) ON DELETE CASCADE;


--
-- Name: sys_erp_adapter_scenario fk_adapter_scenario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_adapter_scenario
    ADD CONSTRAINT fk_adapter_scenario FOREIGN KEY (adapter_id) REFERENCES public.sys_erp_adapter(adapter_id) ON DELETE CASCADE;


--
-- Name: scan_workspace fk_archive; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_workspace
    ADD CONSTRAINT fk_archive FOREIGN KEY (archive_id) REFERENCES public.acc_archive(id) ON DELETE SET NULL;


--
-- Name: archive_batch_item fk_batch_item_batch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch_item
    ADD CONSTRAINT fk_batch_item_batch FOREIGN KEY (batch_id) REFERENCES public.archive_batch(id) ON DELETE CASCADE;


--
-- Name: collection_batch_file fk_collection_batch_file_archive; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.collection_batch_file
    ADD CONSTRAINT fk_collection_batch_file_archive FOREIGN KEY (archive_id) REFERENCES public.acc_archive(id) ON DELETE SET NULL;


--
-- Name: arc_original_voucher_event fk_event_voucher; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_event
    ADD CONSTRAINT fk_event_voucher FOREIGN KEY (voucher_id) REFERENCES public.arc_original_voucher(id) ON DELETE CASCADE;


--
-- Name: arc_file_content fk_file_content_batch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_file_content
    ADD CONSTRAINT fk_file_content_batch FOREIGN KEY (batch_id) REFERENCES public.arc_archive_batch(id) ON DELETE SET NULL;


--
-- Name: scan_folder_monitor fk_monitor_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_folder_monitor
    ADD CONSTRAINT fk_monitor_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: arc_original_voucher fk_ov_batch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher
    ADD CONSTRAINT fk_ov_batch FOREIGN KEY (pool_batch_id) REFERENCES public.arc_import_batch(id) ON DELETE SET NULL;


--
-- Name: arc_original_voucher_file fk_ovf_voucher; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_original_voucher_file
    ADD CONSTRAINT fk_ovf_voucher FOREIGN KEY (voucher_id) REFERENCES public.arc_original_voucher(id) ON DELETE CASCADE;


--
-- Name: sys_sync_history fk_sync_history_operator; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT fk_sync_history_operator FOREIGN KEY (operator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;


--
-- Name: sys_sync_history fk_sync_history_scenario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT fk_sync_history_scenario FOREIGN KEY (scenario_id) REFERENCES public.sys_erp_scenario(id) ON DELETE CASCADE;


--
-- Name: scan_workspace fk_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scan_workspace
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: arc_voucher_relation fk_vr_original; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arc_voucher_relation
    ADD CONSTRAINT fk_vr_original FOREIGN KEY (original_voucher_id) REFERENCES public.arc_original_voucher(id) ON DELETE CASCADE;


--
-- Name: sys_erp_scenario sys_erp_scenario_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_erp_scenario
    ADD CONSTRAINT sys_erp_scenario_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.sys_erp_config(id) ON DELETE CASCADE;


--
-- Name: user_mfa_config user_mfa_config_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_mfa_config
    ADD CONSTRAINT user_mfa_config_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: ys_sales_out_detail ys_sales_out_detail_sales_out_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out_detail
    ADD CONSTRAINT ys_sales_out_detail_sales_out_id_fkey FOREIGN KEY (sales_out_id) REFERENCES public.ys_sales_out(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict ECkGfRuLlHfHe93qoFNtCaRsADI1TtOSqcCI61aJAj7fdKNcHRIHgtnGlTN9sYF

