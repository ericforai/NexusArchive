--
-- PostgreSQL database dump
--

-- Dumped from database version 14.18 (Homebrew)
-- Dumped by pg_dump version 14.18 (Homebrew)

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
    archived_at timestamp without time zone
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
-- Name: acc_archive_attachment; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.acc_archive_attachment (
    id character varying(64) NOT NULL,
    archive_id character varying(64) NOT NULL,
    file_id character varying(64) NOT NULL,
    attachment_type character varying(32) NOT NULL,
    relation_desc character varying(255),
    created_by character varying(64),
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
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: TABLE acc_archive_relation; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.acc_archive_relation IS '档案关联关系表';


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
    deleted integer DEFAULT 0
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
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
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
    pre_archive_status character varying(20) DEFAULT 'PENDING_CHECK'::character varying,
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
    highlight_meta jsonb
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
-- Name: COLUMN arc_file_content.pre_archive_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.arc_file_content.pre_archive_status IS '预归档状态: PENDING_CHECK/CHECK_FAILED/PENDING_METADATA/PENDING_ARCHIVE/ARCHIVED';


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
    CONSTRAINT chk_batch_status CHECK (((status)::text = ANY ((ARRAY['UPLOADING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 'ROLLED_BACK'::character varying])::text[])))
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
    CONSTRAINT chk_action CHECK (((action)::text = ANY ((ARRAY['UPLOAD'::character varying, 'PARSE'::character varying, 'PARSE_RETRY'::character varying, 'MATCH'::character varying, 'UNMATCH'::character varying, 'ARCHIVE'::character varying, 'DELETE'::character varying, 'RESTORE'::character varying, 'ROLLBACK'::character varying, 'MOVE_TYPE'::character varying])::text[])))
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
    compliance_warnings text
);


--
-- Name: TABLE audit_inspection_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.audit_inspection_log IS '四性检测日志表';


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
    org_id character varying(64)
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
    deleted integer DEFAULT 0
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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL,
    org_name character varying(255)
);


--
-- Name: COLUMN biz_archive_approval.org_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_archive_approval.org_name IS '立档单位';


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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
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
-- Name: COLUMN biz_borrowing.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.updated_at IS '更新时间';


--
-- Name: COLUMN biz_borrowing.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_borrowing.deleted IS '逻辑删除标识';


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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
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

COMMENT ON COLUMN public.biz_destruction.status IS '状态: PENDING, APPROVED, REJECTED, EXECUTED';


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
-- Name: COLUMN biz_destruction.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.updated_at IS '更新时间';


--
-- Name: COLUMN biz_destruction.deleted; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.biz_destruction.deleted IS '逻辑删除标识';


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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL
);


--
-- Name: TABLE biz_open_appraisal; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.biz_open_appraisal IS '开放鉴定表';


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
    client_ip character varying(50) NOT NULL,
    mac_address character varying(64) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    object_digest character varying(128),
    user_agent character varying(500),
    prev_log_hash character varying(128),
    log_hash character varying(128),
    device_fingerprint character varying(255),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
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
    org_id character varying(64)
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
-- Name: COLUMN sys_erp_config.org_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sys_erp_config.org_id IS '关联组织ID';


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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
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
-- Name: sys_org; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_org (
    id character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(128),
    parent_id character varying(64),
    type character varying(32) DEFAULT 'DEPARTMENT'::character varying,
    order_num integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


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
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


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
    deleted integer DEFAULT 0
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
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


--
-- Name: sys_user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sys_user_role (
    user_id character varying(64) NOT NULL,
    role_id character varying(64) NOT NULL
);


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

INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-contract-001', 'DEMO', 'CON-2023-098', 'AC04', '年度技术服务协议', '2023', '01', '30Y', '演示公司', '系统', 'archived', 150000.00, '2023-01-15', 'CON-2023-098', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-contract-002', 'DEMO', 'C-202511-002', 'AC04', '服务器采购合同', '2025', '11', '30Y', '演示公司', '系统', 'archived', 450000.00, '2025-11-15', 'C-202511-002', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-invoice-001', 'DEMO', 'INV-202311-089', 'AC01', '阿里云计算服务费发票', '2023', '11', '30Y', '演示公司', '系统', 'archived', 12800.00, '2023-11-02', 'INV-202311-089', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-invoice-002', 'DEMO', 'INV-202311-092', 'AC01', '服务器采购发票', '2023', '11', '30Y', '演示公司', '系统', 'archived', 45200.00, '2023-11-03', 'INV-202311-092', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-voucher-001', 'DEMO', 'JZ-202311-0052', 'AC01', '11月技术部费用报销', '2023', '11', '30Y', '演示公司', '系统', 'archived', 58000.00, '2023-11-05', 'JZ-202311-0052', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-voucher-002', 'DEMO', 'V-202511-TEST', 'AC01', '报销差旅费', '2025', '11', '30Y', '演示公司', '张三', 'archived', 5280.00, '2025-11-07', 'V-202511-TEST', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-receipt-001', 'DEMO', 'B-20231105-003', 'AC04', '招商银行付款回单', '2023', '11', '30Y', '演示公司', '系统', 'archived', 58000.00, '2023-11-05', 'B-20231105-003', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-report-001', 'DEMO', 'REP-2023-11', 'AC03', '11月科目余额表', '2023', '11', '30Y', '演示公司', '系统', 'archived', NULL, '2023-11-30', 'REP-2023-11', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-book-001', 'COMP001', 'ARC-BOOK-2024-GL', 'AC02', '2024年总账', '2024', NULL, '30Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"bookType": "GENERAL_LEDGER", "pageCount": 100}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-book-002', 'COMP001', 'ARC-BOOK-2024-CASH', 'AC02', '2024年现金日记账', '2024', NULL, '30Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"bookType": "CASH_JOURNAL", "pageCount": 50}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-book-003', 'COMP001', 'ARC-BOOK-2024-BANK', 'AC02', '2024年银行存款日记账', '2024', NULL, '30Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"bookType": "BANK_JOURNAL", "pageCount": 50}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-book-004', 'COMP001', 'ARC-BOOK-2024-FIXED', 'AC02', '2024年固定资产卡片', '2024', NULL, '30Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"bookType": "FIXED_ASSETS_CARD", "pageCount": 20}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-c03-001', 'COMP001', 'ARC-REP-2024-M01', 'AC03', '2024年1月财务月报', '2024', NULL, '10Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"period": "2024-01", "reportType": "MONTHLY"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-c03-002', 'COMP001', 'ARC-REP-2024-Q1', 'AC03', '2024年第一季度财务报表', '2024', NULL, '10Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"period": "2024-Q1", "reportType": "QUARTERLY"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-c03-003', 'COMP001', 'ARC-REP-2023-ANN', 'AC03', '2023年度财务决算报告', '2023', NULL, 'PERMANENT', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"period": "2023", "reportType": "ANNUAL"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-c04-001', 'COMP001', 'ARC-OTH-2024-BK-01', 'AC04', '2024年1月银行对账单', '2024', NULL, '10Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"otherType": "BANK_STATEMENT"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-c04-002', 'COMP001', 'ARC-OTH-2024-TAX-01', 'AC04', '2024年1月增值税纳税申报表', '2024', NULL, '10Y', '总公司', NULL, 'archived', NULL, NULL, NULL, NULL, '{"otherType": "TAX_RETURN"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('seed-c04-003', 'COMP001', 'ARC-OTH-2024-HO-01', 'AC04', '2024年度会计档案移交清册', '2024', NULL, '30Y', '档案室', NULL, 'archived', NULL, NULL, NULL, NULL, '{"otherType": "HANDOVER_REGISTER"}', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:34.909662', '2025-12-28 09:03:34.909662', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-12-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-3001', 'AC01', '支付年终审计费', '2024', '12', '30Y', '泊冉集团有限公司', '张三', 'archived', 88000.00, '2024-12-10', 'JZ-202412-0001', NULL, '[{"id": "1", "debit_org": 77876.11, "accsubject": {"code": "6602", "name": "管理费用-审计费"}, "credit_org": 0, "description": "年度审计服务费"}, {"id": "2", "debit_org": 10123.89, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 88000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-12-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-3002', 'AC01', '年终奖金计提', '2024', '12', '30Y', '泊冉集团有限公司', '李四', 'archived', 2580000.00, '2024-12-20', 'JZ-202412-0002', NULL, '[{"id": "1", "debit_org": 1200000.00, "accsubject": {"code": "6602", "name": "管理费用-工资"}, "credit_org": 0, "description": "管理层年终奖"}, {"id": "2", "debit_org": 880000.00, "accsubject": {"code": "6601", "name": "销售费用-工资"}, "credit_org": 0, "description": "销售奖金"}, {"id": "3", "debit_org": 500000.00, "accsubject": {"code": "5001", "name": "生产成本-直接人工"}, "credit_org": 0, "description": "生产绩效奖"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 2580000.00, "description": "应付年终奖"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-11-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0011', 'AC01', '支付技术服务费', '2023', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 45800.00, '2023-11-15', 'JZ-202311-0001', NULL, '[{"id": "1", "debit_org": 40530.97, "accsubject": {"code": "6602", "name": "管理费用-技术服务费"}, "credit_org": 0, "description": "华为云年度服务费"}, {"id": "2", "debit_org": 5269.03, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 45800.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-10-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0021', 'AC01', '采购生产设备', '2023', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 580000.00, '2023-10-20', 'JZ-202310-0001', NULL, '[{"id": "1", "debit_org": 513274.34, "accsubject": {"code": "1601", "name": "固定资产"}, "credit_org": 0, "description": "数控机床采购"}, {"id": "2", "debit_org": 66725.66, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 580000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2022-12-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0001', 'AC01', '年度损益结转', '2022', '12', '30Y', '泊冉集团有限公司', '李四', 'archived', 2680000.00, '2022-12-31', 'JZ-202212-0001', NULL, '[{"id": "1", "debit_org": 2680000.00, "accsubject": {"code": "3131", "name": "本年利润"}, "credit_org": 0, "description": "结转本年利润"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "3141", "name": "利润分配"}, "credit_org": 2680000.00, "description": "利润分配"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2022-06-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0011', 'AC01', '半年度奖金发放', '2022', '06', '30Y', '泊冉集团有限公司', '李四', 'archived', 1250000.00, '2022-06-30', 'JZ-202206-0001', NULL, '[{"id": "1", "debit_org": 1250000.00, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 0, "description": "应付工资结转"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 1250000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2025-01-001', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-0001', 'AC01', '支付员工年终奖', '2025', '01', '30Y', '泊冉集团有限公司', '李四', 'archived', 2580000.00, '2025-01-15', 'JZ-202501-0001', NULL, '[{"id": "1", "debit_org": 2580000.00, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 0, "description": "发放年终奖金"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 2580000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-sales-2024-11-001', 'BR-SALES', 'BR-SALES-2024-30Y-FIN-AC01-0001', 'AC01', '销售产品收入', '2024', '11', '30Y', '泊冉销售有限公司', '李四', 'archived', 1280000.00, '2024-11-18', 'JZ-SALES-202411-0001', NULL, '[{"id": "1", "debit_org": 1446400.00, "accsubject": {"code": "1122", "name": "应收账款"}, "credit_org": 0, "description": "销售智能设备A型"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 1280000.00, "description": "确认收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 166400.00, "description": "销项税额"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-trade-2024-11-001', 'BR-TRADE', 'BR-TRADE-2024-30Y-FIN-AC01-0001', 'AC01', '进口设备采购', '2024', '11', '30Y', '泊冉国际贸易有限公司', '张三', 'archived', 860000.00, '2024-11-22', 'JZ-TRADE-202411-0001', NULL, '[{"id": "1", "debit_org": 761061.95, "accsubject": {"code": "1403", "name": "原材料"}, "credit_org": 0, "description": "进口精密仪器"}, {"id": "2", "debit_org": 98938.05, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 860000.00, "description": "应付账款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-mfg-2024-11-001', 'BR-MFG', 'BR-MFG-2024-30Y-FIN-AC01-0001', 'AC01', '生产材料领用', '2024', '11', '30Y', '泊冉制造有限公司', '赵六', 'archived', 156000.00, '2024-11-08', 'JZ-MFG-202411-0001', NULL, '[{"id": "1", "debit_org": 156000.00, "accsubject": {"code": "5001", "name": "生产成本-直接材料"}, "credit_org": 0, "description": "领用钢材"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1403", "name": "原材料"}, "credit_org": 156000.00, "description": "原材料减少"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2024-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-ZZ001', 'AC02', '2024年度总账', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'ZZ-2024-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2024-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-MX001', 'AC02', '2024年度银行存款明细账', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'MX-2024-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2024-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-RJ001', 'AC02', '2024年度现金日记账', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'RJ-2024-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2024-004', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-YS001', 'AC02', '2024年度应收账款明细账', '2024', '00', '30Y', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-12-31', 'YS-2024-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2024-005', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC02-GD001', 'AC02', '2024年度固定资产卡片', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-12-31', 'GD-2024-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2023-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC02-ZZ001', 'AC02', '2023年度总账', '2023', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2023-12-31', 'ZZ-2023-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2023-002', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC02-MX001', 'AC02', '2023年度银行存款明细账', '2023', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2023-12-31', 'MX-2023-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('ledger-2022-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC02-ZZ001', 'AC02', '2022年度总账', '2022', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2022-12-31', 'ZZ-2022-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2024-zcfz-11', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11', 'AC03', '2024年11月资产负债表', '2024', '11', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-11-30', 'ZCFZ-202411', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2024-zcfz-10', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10', 'AC03', '2024年10月资产负债表', '2024', '10', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-10-31', 'ZCFZ-202410', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2024-zcfz-09', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09', 'AC03', '2024年9月资产负债表', '2024', '09', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-09-30', 'ZCFZ-202409', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2024-lr-11', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-LR11', 'AC03', '2024年11月利润表', '2024', '11', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-11-30', 'LR-202411', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2024-lr-10', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-LR10', 'AC03', '2024年10月利润表', '2024', '10', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-10-31', 'LR-202410', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2024-xjll-q3', 'BR-GROUP', 'BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3', 'AC03', '2024年第三季度现金流量表', '2024', 'Q3', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2024-09-30', 'XJLL-2024Q3', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2023-annual', 'BR-GROUP', 'BR-GROUP-2023-PERM-FIN-AC03-ANNUAL', 'AC03', '2023年度财务决算报告', '2023', '00', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2023-12-31', 'ANNUAL-2023', NULL, NULL, 'confidential', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('report-2022-annual', 'BR-GROUP', 'BR-GROUP-2022-PERM-FIN-AC03-ANNUAL', 'AC03', '2022年度财务决算报告', '2022', '00', 'PERMANENT', '泊冉集团有限公司', '李四', 'archived', NULL, '2022-12-31', 'ANNUAL-2022', NULL, NULL, 'confidential', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-bank-2024-11', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-BANK11', 'AC04', '2024年11月招商银行对账单', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-11-30', 'BANK-202411', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-bank-2024-10', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-BANK10', 'AC04', '2024年10月招商银行对账单', '2024', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', NULL, '2024-10-31', 'BANK-202410', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-tax-2024-11', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-TAX11', 'AC04', '2024年11月增值税纳税申报表', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 168500.00, '2024-12-15', 'TAX-202411', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-tax-2024-q3', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3', 'AC04', '2024年第三季度企业所得税预缴申报表', '2024', 'Q3', '30Y', '泊冉集团有限公司', '李四', 'archived', 286000.00, '2024-10-20', 'TAX-2024Q3', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-contract-2024-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-CON001', 'AC04', '华为云年度服务合同', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', 158000.00, '2024-01-15', 'CON-2024-001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-contract-2024-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-CON002', 'AC04', '办公楼租赁合同', '2024', '00', '30Y', '泊冉集团有限公司', '张三', 'archived', 816000.00, '2024-01-01', 'CON-2024-002', NULL, NULL, 'confidential', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-contract-2024-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC04-CON003', 'AC04', '年度审计服务协议', '2024', '00', '30Y', '泊冉集团有限公司', '钱七', 'archived', 88000.00, '2024-03-01', 'CON-2024-003', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('other-audit-2023', 'BR-GROUP', 'BR-GROUP-2023-PERM-FIN-AC04-AUDIT', 'AC04', '2023年度审计报告', '2023', '00', 'PERMANENT', '泊冉集团有限公司', '王五', 'archived', NULL, '2024-03-15', 'AUDIT-2023', NULL, NULL, 'confidential', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2025-02-002', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-0202', 'AC01', '销售收入确认', '2025', '02', '30Y', '泊冉集团有限公司', '李四', 'archived', 358000.00, '2025-02-20', 'JZ-202502-0002', NULL, '[{"id": "1", "debit_org": 404540.00, "accsubject": {"code": "1122", "name": "应收账款"}, "credit_org": 0, "description": "销售智能检测设备"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 358000.00, "description": "确认收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 46540.00, "description": "销项税额"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-004', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0004', 'AC01', '支付阿里云服务器费用', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 12800.00, '2024-11-10', 'JZ-202411-0004', NULL, '[{"id": "1", "debit_org": 12800.00, "accsubject": {"code": "6602", "name": "管理费用-技术服务费"}, "credit_org": 0, "description": "阿里云ECS服务器年费"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 12800.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-005', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0005', 'AC01', '计提本月工资', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 856000.00, '2024-11-25', 'JZ-202411-0005', NULL, '[{"id": "1", "debit_org": 356000.00, "accsubject": {"code": "6602", "name": "管理费用-工资"}, "credit_org": 0, "description": "管理人员工资"}, {"id": "2", "debit_org": 280000.00, "accsubject": {"code": "6601", "name": "销售费用-工资"}, "credit_org": 0, "description": "销售人员工资"}, {"id": "3", "debit_org": 220000.00, "accsubject": {"code": "5001", "name": "生产成本-直接人工"}, "credit_org": 0, "description": "生产人员工资"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 856000.00, "description": "应付工资"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-006', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0006', 'AC01', '固定资产折旧计提', '2024', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 45600.00, '2024-11-28', 'JZ-202411-0006', NULL, '[{"id": "1", "debit_org": 18500.00, "accsubject": {"code": "6602", "name": "管理费用-折旧费"}, "credit_org": 0, "description": "管理部门折旧"}, {"id": "2", "debit_org": 27100.00, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "生产部门折旧"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1602", "name": "累计折旧"}, "credit_org": 45600.00, "description": "累计折旧"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-007', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0007', 'AC01', '支付供应商货款', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 286500.00, '2024-11-12', 'JZ-202411-0007', NULL, '[{"id": "1", "debit_org": 286500.00, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 0, "description": "支付苏州精密机械有限公司货款"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 286500.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-008', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-0008', 'AC01', '销售商品确认收入', '2024', '11', '30Y', '泊冉集团有限公司', '李四', 'archived', 520000.00, '2024-11-15', 'JZ-202411-0008', NULL, '[{"id": "1", "debit_org": 587600.00, "accsubject": {"code": "1122", "name": "应收账款"}, "credit_org": 0, "description": "销售智能设备"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 520000.00, "description": "确认收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 67600.00, "description": "销项税额"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-10-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1001', 'AC01', '支付房租及物业费', '2024', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', 85000.00, '2024-10-05', 'JZ-202410-0001', NULL, '[{"id": "1", "debit_org": 68000.00, "accsubject": {"code": "6602", "name": "管理费用-租赁费"}, "credit_org": 0, "description": "办公楼租金"}, {"id": "2", "debit_org": 17000.00, "accsubject": {"code": "6602", "name": "管理费用-物业费"}, "credit_org": 0, "description": "物业管理费"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 85000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-10', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-10-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1002', 'AC01', '采购原材料入库', '2024', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 468000.00, '2024-10-12', 'JZ-202410-0002', NULL, '[{"id": "1", "debit_org": 410619.47, "accsubject": {"code": "1403", "name": "原材料"}, "credit_org": 0, "description": "原材料入库-钢材"}, {"id": "2", "debit_org": 57380.53, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 468000.00, "description": "暂估应付"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-10', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-09-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0091', 'AC01', '季度社保缴纳', '2023', '09', '30Y', '泊冉集团有限公司', '李四', 'archived', 186500.00, '2023-09-25', 'JZ-202309-0001', NULL, '[{"id": "1", "debit_org": 186500.00, "accsubject": {"code": "6602", "name": "管理费用-社保费"}, "credit_org": 0, "description": "单位社保"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 186500.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-10-003', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1003', 'AC01', '计提本月工资', '2024', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 842000.00, '2024-10-25', 'JZ-202410-0003', NULL, '[{"id": "1", "debit_org": 348000.00, "accsubject": {"code": "6602", "name": "管理费用-工资"}, "credit_org": 0, "description": "管理人员工资"}, {"id": "2", "debit_org": 276000.00, "accsubject": {"code": "6601", "name": "销售费用-工资"}, "credit_org": 0, "description": "销售人员工资"}, {"id": "3", "debit_org": 218000.00, "accsubject": {"code": "5001", "name": "生产成本-直接人工"}, "credit_org": 0, "description": "生产人员工资"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 842000.00, "description": "应付工资"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-10', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-10-004', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1004', 'AC01', '支付水电费', '2024', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', 28600.00, '2024-10-18', 'JZ-202410-0004', NULL, '[{"id": "1", "debit_org": 8600.00, "accsubject": {"code": "6602", "name": "管理费用-水电费"}, "credit_org": 0, "description": "办公楼水电费"}, {"id": "2", "debit_org": 20000.00, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "生产车间水电费"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 28600.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-10', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-10-005', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-1005', 'AC01', '结转本月成本', '2024', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 380000.00, '2024-10-30', 'JZ-202410-0005', NULL, '[{"id": "1", "debit_org": 380000.00, "accsubject": {"code": "6401", "name": "主营业务成本"}, "credit_org": 0, "description": "结转销售成本"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1405", "name": "库存商品"}, "credit_org": 380000.00, "description": "库存商品减少"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-10', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-09-001', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-2001', 'AC01', '购买固定资产-服务器', '2024', '09', '30Y', '泊冉集团有限公司', '张三', 'archived', 185000.00, '2024-09-08', 'JZ-202409-0001', NULL, '[{"id": "1", "debit_org": 163716.81, "accsubject": {"code": "1601", "name": "固定资产"}, "credit_org": 0, "description": "华为服务器采购"}, {"id": "2", "debit_org": 21283.19, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 185000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-09', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-09-002', 'BR-GROUP', 'BR-GROUP-2024-30Y-FIN-AC01-2002', 'AC01', '收到销售佣金收入', '2024', '09', '30Y', '泊冉集团有限公司', '李四', 'archived', 68000.00, '2024-09-15', 'JZ-202409-0002', NULL, '[{"id": "1", "debit_org": 68000.00, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 0, "description": "收到代理佣金"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6051", "name": "其他业务收入"}, "credit_org": 60176.99, "description": "确认其他收入"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(销项)"}, "credit_org": 7823.01, "description": "销项税额"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-09', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-12-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0001', 'AC01', '结转年度利润', '2023', '12', '30Y', '泊冉集团有限公司', '李四', 'archived', 3860000.00, '2023-12-31', 'JZ-202312-0001', NULL, '[{"id": "1", "debit_org": 12580000.00, "accsubject": {"code": "6001", "name": "主营业务收入"}, "credit_org": 0, "description": "结转收入"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "6401", "name": "主营业务成本"}, "credit_org": 7200000.00, "description": "结转成本"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "6602", "name": "管理费用"}, "credit_org": 1520000.00, "description": "结转费用"}, {"id": "4", "debit_org": 0, "accsubject": {"code": "3131", "name": "本年利润"}, "credit_org": 3860000.00, "description": "本年利润"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2023-12', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2014-01-001', 'BR-GROUP', 'BR-GROUP-2014-10Y-FIN-AC01-0001', 'AC01', '2014年1月记账凭证汇总', '2014', '01', '10Y', '泊冉集团有限公司', '系统', 'archived', 580000.00, '2014-01-31', 'JZ-201401-0001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2014-02-01 00:00:00', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2014-02-001', 'BR-GROUP', 'BR-GROUP-2014-10Y-FIN-AC01-0002', 'AC01', '2014年2月记账凭证汇总', '2014', '02', '10Y', '泊冉集团有限公司', '系统', 'archived', 620000.00, '2014-02-28', 'JZ-201402-0001', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2014-03-01 00:00:00', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('bank-2014-q1', 'BR-GROUP', 'BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1', 'AC04', '2014年第一季度银行对账单', '2014', 'Q1', '10Y', '泊冉集团有限公司', '系统', 'archived', NULL, '2014-03-31', 'BANK-2014Q1', NULL, NULL, 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2014-04-01 00:00:00', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('pre-2024-12-001', 'BR-GROUP', 'PENDING-2024-12-001', 'AC01', '支付快递费', '2024', '12', '30Y', '泊冉集团有限公司', '张三', 'pending', 680.00, '2024-12-22', 'JZ-PRE-202412-0001', NULL, '[{"id": "1", "debit_org": 680.00, "accsubject": {"code": "6602", "name": "管理费用-快递费"}, "credit_org": 0, "description": "顺丰快递费"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 680.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('pre-2024-12-002', 'BR-GROUP', 'PENDING-2024-12-002', 'AC01', '会议室租赁费', '2024', '12', '30Y', '泊冉集团有限公司', '张三', 'pending', 3500.00, '2024-12-23', 'JZ-PRE-202412-0002', NULL, '[{"id": "1", "debit_org": 3500.00, "accsubject": {"code": "6602", "name": "管理费用-会议费"}, "credit_org": 0, "description": "酒店会议室租赁"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1001", "name": "库存现金"}, "credit_org": 3500.00, "description": "现金支付"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('pre-2024-12-003', 'BR-GROUP', 'PENDING-2024-12-003', 'AC01', '员工团建活动费', '2024', '12', '30Y', '泊冉集团有限公司', '李四', 'pending', 28000.00, '2024-12-24', 'JZ-PRE-202412-0003', NULL, '[{"id": "1", "debit_org": 28000.00, "accsubject": {"code": "6602", "name": "管理费用-福利费"}, "credit_org": 0, "description": "年会团建"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 28000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2025-02-001', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-0201', 'AC01', '支付供应商货款', '2025', '02', '30Y', '泊冉集团有限公司', '张三', 'archived', 125600.00, '2025-02-18', 'JZ-202502-0001', NULL, '[{"id": "1", "debit_org": 125600.00, "accsubject": {"code": "2202", "name": "应付账款"}, "credit_org": 0, "description": "支付宁波精密零部件货款"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 125600.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-08-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0081', 'AC01', '广告宣传费', '2023', '08', '30Y', '泊冉集团有限公司', '张三', 'archived', 75000.00, '2023-08-15', 'JZ-202308-0001', NULL, '[{"id": "1", "debit_org": 66371.68, "accsubject": {"code": "6601", "name": "销售费用-广告费"}, "credit_org": 0, "description": "微信朋友圈广告投放"}, {"id": "2", "debit_org": 8628.32, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 75000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-07-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0071', 'AC01', '固定资产折旧', '2023', '07', '30Y', '泊冉集团有限公司', '张三', 'archived', 42800.00, '2023-07-31', 'JZ-202307-0001', NULL, '[{"id": "1", "debit_org": 18200.00, "accsubject": {"code": "6602", "name": "管理费用-折旧费"}, "credit_org": 0, "description": "管理部门折旧"}, {"id": "2", "debit_org": 24600.00, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "生产部门折旧"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1602", "name": "累计折旧"}, "credit_org": 42800.00, "description": "累计折旧"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2023-06-001', 'BR-GROUP', 'BR-GROUP-2023-30Y-FIN-AC01-0061', 'AC01', '半年度奖金发放', '2023', '06', '30Y', '泊冉集团有限公司', '李四', 'archived', 980000.00, '2023-06-28', 'JZ-202306-0001', NULL, '[{"id": "1", "debit_org": 980000.00, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "credit_org": 0, "description": "发放半年度奖金"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 980000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2022-11-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0111', 'AC01', '设备维修费', '2022', '11', '30Y', '泊冉集团有限公司', '张三', 'archived', 35600.00, '2022-11-18', 'JZ-202211-0001', NULL, '[{"id": "1", "debit_org": 31504.42, "accsubject": {"code": "5001", "name": "生产成本-制造费用"}, "credit_org": 0, "description": "数控机床年度维保"}, {"id": "2", "debit_org": 4095.58, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 35600.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2022-10-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0101', 'AC01', '研发材料采购', '2022', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 168000.00, '2022-10-22', 'JZ-202210-0001', NULL, '[{"id": "1", "debit_org": 148672.57, "accsubject": {"code": "5201", "name": "研发支出-费用化支出"}, "credit_org": 0, "description": "研发用电子元器件"}, {"id": "2", "debit_org": 19327.43, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 168000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2022-09-001', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-0091', 'AC01', '展会参展费用', '2022', '09', '30Y', '泊冉集团有限公司', '张三', 'archived', 128500.00, '2022-09-15', 'JZ-202209-0001', NULL, '[{"id": "1", "debit_org": 113716.81, "accsubject": {"code": "6601", "name": "销售费用-展览费"}, "credit_org": 0, "description": "上海工博会展位费"}, {"id": "2", "debit_org": 14783.19, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 128500.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-003', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-1003', 'AC01', '支付业务招待费-米山神鸡', '2025', '10', '30Y', '泊冉集团有限公司', '李四', 'archived', 201.00, '2025-10-28', 'JZ-202411-0003', NULL, '[{"id": "1", "debit_org": 201.00, "accsubject": {"code": "6602", "name": "管理费用-业务招待费"}, "credit_org": 0, "description": "支付业务招待费-员工工作餐"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 201.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-001', 'BR-GROUP', 'BR-GROUP-2025-30Y-FIN-AC01-1001', 'AC01', '支付业务招待费-吴奕聪餐饮店', '2025', '10', '30Y', '泊冉集团有限公司', '张三', 'archived', 657.00, '2025-10-25', 'JZ-202411-0001', NULL, '[{"id": "1", "debit_org": 650.50, "accsubject": {"code": "6602", "name": "管理费用-业务招待费"}, "credit_org": 0, "description": "支付业务招待费-客户接待餐费"}, {"id": "2", "debit_org": 6.50, "accsubject": {"code": "2221", "name": "应交税费-应交增值税(进项税额)"}, "credit_org": 0, "description": "进项税额"}, {"id": "3", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 657.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);
INSERT INTO public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_time, last_modified_time, deleted, paper_ref_link, destruction_hold, hold_reason, summary, match_score, match_method, batch_id, archived_at) VALUES ('voucher-2024-11-002', 'BR-GROUP', 'BR-GROUP-2022-30Y-FIN-AC01-1002', 'AC01', '支付强生交通费用', '2022', '09', '30Y', '泊冉集团有限公司', '张三', 'archived', 10000.00, '2022-09-20', 'JZ-202411-0002', NULL, '[{"id": "1", "debit_org": 10000.00, "accsubject": {"code": "6602", "name": "管理费用-交通费"}, "credit_org": 0, "description": "支付交通费"}, {"id": "2", "debit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "credit_org": 10000.00, "description": "银行付款"}]', 'internal', NULL, NULL, 'system', NULL, NULL, 'volume-2024-11', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0, NULL, false, NULL, NULL, 0, NULL, NULL, NULL);


--
-- Data for Name: acc_archive_attachment; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at) VALUES ('attach-link-001', 'voucher-2024-11-002', 'file-invoice-001', 'invoice', '原始发票', 'system', '2025-12-28 09:03:35.017527');
INSERT INTO public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at) VALUES ('attach-link-002', 'voucher-2024-11-001', 'file-invoice-002', 'invoice', '原始发票', 'system', '2025-12-28 09:03:35.017527');
INSERT INTO public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at) VALUES ('attach-link-003', 'voucher-2024-11-003', 'file-invoice-003', 'bank_slip', '银行回单', 'system', '2025-12-28 09:03:35.017527');
INSERT INTO public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at) VALUES ('link-bank-1002', 'voucher-2024-11-002', 'file-bank-receipt-1002', 'bank_slip', '银行回单附件', 'system', '2025-12-28 09:03:35.066446');
INSERT INTO public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at) VALUES ('link-reimb-1002', 'voucher-2024-11-002', 'file-reimbursement-1002', 'other', '员工报销单据', 'system', '2025-12-28 09:03:35.066446');


--
-- Data for Name: acc_archive_relation; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('seed-rel-001', 'seed-contract-001', 'seed-voucher-001', 'BASIS', '合同依据', 'system', '2025-12-28 09:03:34.854164', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('seed-rel-002', 'seed-invoice-001', 'seed-voucher-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', '2025-12-28 09:03:34.854164', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('seed-rel-003', 'seed-invoice-002', 'seed-voucher-001', 'ORIGINAL_VOUCHER', '原始凭证', 'system', '2025-12-28 09:03:34.854164', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('seed-rel-004', 'seed-voucher-001', 'seed-receipt-001', 'CASH_FLOW', '资金流', 'system', '2025-12-28 09:03:34.854164', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('seed-rel-005', 'seed-voucher-001', 'seed-report-001', 'ARCHIVE', '归档', 'system', '2025-12-28 09:03:34.854164', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('rel-v2024-11-004-con', 'voucher-2024-11-004', 'other-contract-2024-001', 'BASIS', '合同依据', 'system', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('rel-v2024-10-001-con', 'voucher-2024-10-001', 'other-contract-2024-002', 'BASIS', '租赁合同依据', 'system', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('rel-v2024-12-001-con', 'voucher-2024-12-001', 'other-contract-2024-003', 'BASIS', '审计服务合同依据', 'system', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) VALUES ('rel-v2024-11-005-lr', 'voucher-2024-11-005', 'report-2024-lr-11', 'ARCHIVE', '归入月度报表', 'system', '2025-12-28 09:03:35.017527', 0);


--
-- Data for Name: acc_archive_volume; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, volume_status, reviewed_by, reviewed_at, archived_at, custodian_dept, validation_report_path, created_time, last_modified_time, deleted) VALUES ('volume-2024-11', 'AJ-2024-11', '2024年11月会计凭证', 'BR-GROUP', '2024', '11', 'AC01', 8, '30Y', 'archived', 'user-qianqi', NULL, '2025-12-28 09:03:35.017527', 'ACCOUNTING', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, volume_status, reviewed_by, reviewed_at, archived_at, custodian_dept, validation_report_path, created_time, last_modified_time, deleted) VALUES ('volume-2024-10', 'AJ-2024-10', '2024年10月会计凭证', 'BR-GROUP', '2024', '10', 'AC01', 5, '30Y', 'archived', 'user-qianqi', NULL, '2025-12-28 09:03:35.017527', 'ACCOUNTING', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, volume_status, reviewed_by, reviewed_at, archived_at, custodian_dept, validation_report_path, created_time, last_modified_time, deleted) VALUES ('volume-2024-09', 'AJ-2024-09', '2024年9月会计凭证', 'BR-GROUP', '2024', '09', 'AC01', 2, '30Y', 'archived', 'user-qianqi', NULL, '2025-12-28 09:03:35.017527', 'ACCOUNTING', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, volume_status, reviewed_by, reviewed_at, archived_at, custodian_dept, validation_report_path, created_time, last_modified_time, deleted) VALUES ('volume-2023-12', 'AJ-2023-12', '2023年12月会计凭证', 'BR-GROUP', '2023', '12', 'AC01', 2, '30Y', 'archived', 'user-qianqi', NULL, '2025-12-28 09:03:35.017527', 'ACCOUNTING', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);


--
-- Data for Name: arc_abnormal_voucher; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_archive_batch; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_convert_log; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_file_content; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('file-invoice-002', 'BR-GROUP-2025-30Y-FIN-AC01-1001', '电子发票_吴奕聪餐饮店_657元.pdf', 'PDF', 101657, '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e', 'SHA-256', 'uploads/demo/dzfp_25314000000004648601_上海市长宁区吴奕聪餐饮店_20251025012013.pdf', '2025-12-28 09:03:35.017527', 'voucher-2024-11-001', NULL, NULL, NULL, NULL, NULL, 'PENDING_CHECK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('file-invoice-003', 'BR-GROUP-2025-30Y-FIN-AC01-1003', '电子发票_米山神鸡_201元.pdf', 'PDF', 101613, 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e', 'SHA-256', 'uploads/demo/上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf', '2025-12-28 09:03:35.017527', 'voucher-2024-11-003', NULL, NULL, NULL, NULL, NULL, 'PENDING_CHECK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('file-bank-receipt-1002', 'BR-GROUP-2022-30Y-FIN-AC01-1002', '银行回单_10000元.pdf', 'PDF', 102400, 'hash_placeholder_1', 'SHA-256', 'uploads/demo/bank_receipt_1002.pdf', '2025-12-28 09:03:35.061126', 'voucher-2024-11-002', NULL, NULL, NULL, NULL, NULL, 'PENDING_CHECK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('file-reimbursement-1002', 'BR-GROUP-2022-30Y-FIN-AC01-1002', '员工报销单.pdf', 'PDF', 51200, 'hash_placeholder_2', 'SHA-256', 'uploads/demo/reimbursement_1002.pdf', '2025-12-28 09:03:35.061126', 'voucher-2024-11-002', NULL, NULL, NULL, NULL, NULL, 'PENDING_CHECK', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('a688f9e3-35a0-5121-fcb4-30ae1cb2cc7d', 'CON-2023-098', 'CON-2023-098.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/CON-2023-098.pdf', '2025-12-28 09:03:35.080539', 'seed-contract-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "CON-2023-098", "maketime": "2023-01-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 150000.00, "credit_original": 150000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'CON-2023-098', '2023-01-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('c8c768e3-ee4f-4c0e-936a-1cda3a292b51', 'C-202511-002', 'C-202511-002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/C-202511-002.pdf', '2025-12-28 09:03:35.080539', 'seed-contract-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "C-202511-002", "maketime": "2025-11-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 450000.00, "credit_original": 450000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'C-202511-002', '2025-11-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('f4653466-b670-a083-acff-19ff6d55be02', 'INV-202311-089', 'INV-202311-089.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/INV-202311-089.pdf', '2025-12-28 09:03:35.080539', 'seed-invoice-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "INV-202311-089", "maketime": "2023-11-02", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 12800.00, "credit_original": 12800.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'INV-202311-089', '2023-11-02', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('1dc0f7b7-c5b4-2882-5cfc-9b7f9e0748bc', 'INV-202311-092', 'INV-202311-092.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/INV-202311-092.pdf', '2025-12-28 09:03:35.080539', 'seed-invoice-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "INV-202311-092", "maketime": "2023-11-03", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 45200.00, "credit_original": 45200.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'INV-202311-092', '2023-11-03', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('7f5d1ad9-ee09-9f47-4fc8-e0bddbb02e39', 'JZ-202311-0052', 'JZ-202311-0052.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/JZ-202311-0052.pdf', '2025-12-28 09:03:35.080539', 'seed-voucher-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "JZ-202311-0052", "maketime": "2023-11-05", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 58000.00, "credit_original": 58000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'JZ-202311-0052', '2023-11-05', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('eb867bea-c3bf-91ad-e54d-4291ba3b40ea', 'V-202511-TEST', 'V-202511-TEST.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/V-202511-TEST.pdf', '2025-12-28 09:03:35.080539', 'seed-voucher-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "V-202511-TEST", "maketime": "2025-11-07", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 5280.00, "credit_original": 5280.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'V-202511-TEST', '2025-11-07', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('45617208-7665-cfa7-7e8d-378997a9f996', 'B-20231105-003', 'B-20231105-003.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/B-20231105-003.pdf', '2025-12-28 09:03:35.080539', 'seed-receipt-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "B-20231105-003", "maketime": "2023-11-05", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 58000.00, "credit_original": 58000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'B-20231105-003', '2023-11-05', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('d1d8378c-5c96-9796-7687-073dfac21783', 'REP-2023-11', 'REP-2023-11.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/REP-2023-11.pdf', '2025-12-28 09:03:35.080539', 'seed-report-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "REP-2023-11", "maketime": "2023-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'REP-2023-11', '2023-11-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('3fdde7b4-4eb3-cd7d-8d94-4a3995504688', 'ARC-BOOK-2024-GL', 'ARC-BOOK-2024-GL.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-BOOK-2024-GL.pdf', '2025-12-28 09:03:35.080539', 'seed-book-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-BOOK-2024-GL", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-BOOK-2024-GL', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('4b29c5cf-cda4-58f0-b5d2-3387b88fa731', 'ARC-BOOK-2024-CASH', 'ARC-BOOK-2024-CASH.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-BOOK-2024-CASH.pdf', '2025-12-28 09:03:35.080539', 'seed-book-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-BOOK-2024-CASH", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-BOOK-2024-CASH', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('c9737086-ddbf-138e-7e5f-1b9d696280a7', 'ARC-BOOK-2024-BANK', 'ARC-BOOK-2024-BANK.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-BOOK-2024-BANK.pdf', '2025-12-28 09:03:35.080539', 'seed-book-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-BOOK-2024-BANK", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-BOOK-2024-BANK', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('ac419629-f6e4-c381-8941-428a9c46665d', 'ARC-BOOK-2024-FIXED', 'ARC-BOOK-2024-FIXED.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-BOOK-2024-FIXED.pdf', '2025-12-28 09:03:35.080539', 'seed-book-004', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-BOOK-2024-FIXED", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-BOOK-2024-FIXED', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('f6e4fce3-2d6c-bda0-d7dc-e91b0384b554', 'ARC-REP-2024-M01', 'ARC-REP-2024-M01.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-REP-2024-M01.pdf', '2025-12-28 09:03:35.080539', 'seed-c03-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-REP-2024-M01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-REP-2024-M01', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('129d19ec-5cce-c132-840b-f26d8ebc6336', 'ARC-REP-2024-Q1', 'ARC-REP-2024-Q1.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-REP-2024-Q1.pdf', '2025-12-28 09:03:35.080539', 'seed-c03-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-REP-2024-Q1", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-REP-2024-Q1', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('9f322f5e-6325-03c2-2933-af4d0d0352b4', 'ARC-REP-2023-ANN', 'ARC-REP-2023-ANN.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-REP-2023-ANN.pdf', '2025-12-28 09:03:35.080539', 'seed-c03-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-REP-2023-ANN", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-REP-2023-ANN', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('0754d9bb-2da4-d61f-6c4f-a2e1488d72d1', 'ARC-OTH-2024-BK-01', 'ARC-OTH-2024-BK-01.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-OTH-2024-BK-01.pdf', '2025-12-28 09:03:35.080539', 'seed-c04-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-OTH-2024-BK-01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-OTH-2024-BK-01', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('dc877913-6174-c47f-f6ef-65e6acfd8e77', 'ARC-OTH-2024-TAX-01', 'ARC-OTH-2024-TAX-01.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-OTH-2024-TAX-01.pdf', '2025-12-28 09:03:35.080539', 'seed-c04-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-OTH-2024-TAX-01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-OTH-2024-TAX-01', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('c8f0b420-d72a-b4df-a17c-960256208195', 'ARC-OTH-2024-HO-01', 'ARC-OTH-2024-HO-01.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/ARC-OTH-2024-HO-01.pdf', '2025-12-28 09:03:35.080539', 'seed-c04-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "ARC-OTH-2024-HO-01", "maketime": "1970-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'ARC-OTH-2024-HO-01', NULL, NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('8679a4b4-1be5-b057-e291-ffbc02811f63', 'BR-GROUP-2024-30Y-FIN-AC01-3001', 'BR-GROUP-2024-30Y-FIN-AC01-3001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-3001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-12-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-3001", "maketime": "2024-12-10", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 88000.00, "credit_original": 88000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-3001', '2024-12-10', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('691e7863-4047-ac94-56a3-9cdedc5c0cd3', 'BR-GROUP-2024-30Y-FIN-AC01-3002', 'BR-GROUP-2024-30Y-FIN-AC01-3002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-3002.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-12-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-3002", "maketime": "2024-12-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 2580000.00, "credit_original": 2580000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-3002', '2024-12-20', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('e8d8e2e5-407a-00ab-39cf-0465fd59caab', 'BR-GROUP-2023-30Y-FIN-AC01-0011', 'BR-GROUP-2023-30Y-FIN-AC01-0011.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0011.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-11-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0011", "maketime": "2023-11-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 45800.00, "credit_original": 45800.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0011', '2023-11-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('479ca8ec-933d-9486-fc1f-0299a064bcb7', 'BR-GROUP-2023-30Y-FIN-AC01-0021', 'BR-GROUP-2023-30Y-FIN-AC01-0021.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0021.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-10-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0021", "maketime": "2023-10-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 580000.00, "credit_original": 580000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0021', '2023-10-20', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('23029d10-ce5b-e2c7-4345-10bc7b52a6f1', 'BR-GROUP-2022-30Y-FIN-AC01-0001', 'BR-GROUP-2022-30Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2022-12-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0001", "maketime": "2022-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 2680000.00, "credit_original": 2680000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-30Y-FIN-AC01-0001', '2022-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('76071bfc-174f-1160-c5cc-590435c5b943', 'BR-GROUP-2022-30Y-FIN-AC01-0011', 'BR-GROUP-2022-30Y-FIN-AC01-0011.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0011.pdf', '2025-12-28 09:03:35.080539', 'voucher-2022-06-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0011", "maketime": "2022-06-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 1250000.00, "credit_original": 1250000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-30Y-FIN-AC01-0011', '2022-06-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('77d2fcb3-279d-756d-6d48-bac2f89d1669', 'BR-GROUP-2025-30Y-FIN-AC01-0001', 'BR-GROUP-2025-30Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2025-30Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2025-01-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2025-30Y-FIN-AC01-0001", "maketime": "2025-01-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 2580000.00, "credit_original": 2580000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2025-30Y-FIN-AC01-0001', '2025-01-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('fd42ce2f-919e-e293-1caf-933b627306d0', 'BR-SALES-2024-30Y-FIN-AC01-0001', 'BR-SALES-2024-30Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-SALES-2024-30Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-sales-2024-11-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-SALES-2024-30Y-FIN-AC01-0001", "maketime": "2024-11-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 1280000.00, "credit_original": 1280000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-SALES-2024-30Y-FIN-AC01-0001', '2024-11-18', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('62656fa1-ee14-4730-817f-6c1c7b633cc9', 'BR-TRADE-2024-30Y-FIN-AC01-0001', 'BR-TRADE-2024-30Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-TRADE-2024-30Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-trade-2024-11-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-TRADE-2024-30Y-FIN-AC01-0001", "maketime": "2024-11-22", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 860000.00, "credit_original": 860000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-TRADE-2024-30Y-FIN-AC01-0001', '2024-11-22', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('f126af4c-1246-a8f1-5e34-251341077197', 'BR-MFG-2024-30Y-FIN-AC01-0001', 'BR-MFG-2024-30Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-MFG-2024-30Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-mfg-2024-11-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-MFG-2024-30Y-FIN-AC01-0001", "maketime": "2024-11-08", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 156000.00, "credit_original": 156000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-MFG-2024-30Y-FIN-AC01-0001', '2024-11-08', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('49536a22-1b58-0475-3d95-d9c005bea876', 'BR-GROUP-2024-30Y-FIN-AC02-ZZ001', 'BR-GROUP-2024-30Y-FIN-AC02-ZZ001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-ZZ001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2024-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-ZZ001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC02-ZZ001', '2024-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('974df683-b584-dd21-9908-a6fb0cc27bf5', 'BR-GROUP-2024-30Y-FIN-AC02-MX001', 'BR-GROUP-2024-30Y-FIN-AC02-MX001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-MX001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2024-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-MX001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC02-MX001', '2024-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('796aee94-c6e8-a1d0-5675-ff980d5ddb28', 'BR-GROUP-2024-30Y-FIN-AC02-RJ001', 'BR-GROUP-2024-30Y-FIN-AC02-RJ001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-RJ001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2024-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-RJ001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC02-RJ001', '2024-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('4439068a-12ec-3ab7-8486-34593a9dcb7a', 'BR-GROUP-2024-30Y-FIN-AC02-YS001', 'BR-GROUP-2024-30Y-FIN-AC02-YS001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-YS001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2024-004', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-YS001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC02-YS001', '2024-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('88a3aa69-f5e8-6a04-6c02-27b5c4b61549', 'BR-GROUP-2024-30Y-FIN-AC02-GD001', 'BR-GROUP-2024-30Y-FIN-AC02-GD001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC02-GD001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2024-005', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC02-GD001", "maketime": "2024-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC02-GD001', '2024-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('71e000b3-27e3-86af-b8fb-4fcbe6c3d8d4', 'BR-GROUP-2023-30Y-FIN-AC02-ZZ001', 'BR-GROUP-2023-30Y-FIN-AC02-ZZ001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC02-ZZ001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2023-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC02-ZZ001", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC02-ZZ001', '2023-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('d9b3dae8-c7f1-a44a-4762-94675732cbb9', 'BR-GROUP-2023-30Y-FIN-AC02-MX001', 'BR-GROUP-2023-30Y-FIN-AC02-MX001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC02-MX001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2023-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC02-MX001", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC02-MX001', '2023-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('d23c9293-e081-a4ac-59a4-ad8105edad04', 'BR-GROUP-2022-30Y-FIN-AC02-ZZ001', 'BR-GROUP-2022-30Y-FIN-AC02-ZZ001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC02-ZZ001.pdf', '2025-12-28 09:03:35.080539', 'ledger-2022-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC02-ZZ001", "maketime": "2022-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-30Y-FIN-AC02-ZZ001', '2022-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('6787cd0f-07d8-93a4-7931-db76ddadb201', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11.pdf', '2025-12-28 09:03:35.080539', 'report-2024-zcfz-11', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11", "maketime": "2024-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ11', '2024-11-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('609ca558-763d-86c8-d110-c5f3c5693bc5', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10.pdf', '2025-12-28 09:03:35.080539', 'report-2024-zcfz-10', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10", "maketime": "2024-10-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ10', '2024-10-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('472450ef-4a97-3e1c-9733-e15373d8423d', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09', 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09.pdf', '2025-12-28 09:03:35.080539', 'report-2024-zcfz-09', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09", "maketime": "2024-09-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-PERM-FIN-AC03-ZCFZ09', '2024-09-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('c78eee00-db72-79ce-ef97-fcc7093561fd', 'BR-GROUP-2024-PERM-FIN-AC03-LR11', 'BR-GROUP-2024-PERM-FIN-AC03-LR11.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-LR11.pdf', '2025-12-28 09:03:35.080539', 'report-2024-lr-11', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-LR11", "maketime": "2024-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-PERM-FIN-AC03-LR11', '2024-11-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('d7b07746-c7d0-51c6-e885-03efe9d86efd', 'BR-GROUP-2024-PERM-FIN-AC03-LR10', 'BR-GROUP-2024-PERM-FIN-AC03-LR10.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-LR10.pdf', '2025-12-28 09:03:35.080539', 'report-2024-lr-10', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-LR10", "maketime": "2024-10-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-PERM-FIN-AC03-LR10', '2024-10-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('2506ca90-601a-e5e0-bebf-0e2dec2028aa', 'BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3', 'BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3.pdf', '2025-12-28 09:03:35.080539', 'report-2024-xjll-q3', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3", "maketime": "2024-09-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-PERM-FIN-AC03-XJLL-Q3', '2024-09-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('59caf205-d7b0-3936-6770-59d3944099c4', 'BR-GROUP-2023-PERM-FIN-AC03-ANNUAL', 'BR-GROUP-2023-PERM-FIN-AC03-ANNUAL.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-PERM-FIN-AC03-ANNUAL.pdf', '2025-12-28 09:03:35.080539', 'report-2023-annual', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-PERM-FIN-AC03-ANNUAL", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-PERM-FIN-AC03-ANNUAL', '2023-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('53b373d0-5066-f911-ca61-64d191ac7c73', 'BR-GROUP-2022-PERM-FIN-AC03-ANNUAL', 'BR-GROUP-2022-PERM-FIN-AC03-ANNUAL.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-PERM-FIN-AC03-ANNUAL.pdf', '2025-12-28 09:03:35.080539', 'report-2022-annual', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-PERM-FIN-AC03-ANNUAL", "maketime": "2022-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-PERM-FIN-AC03-ANNUAL', '2022-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('eb7e102f-83c2-8fc4-92dc-5c61504932fb', 'BR-GROUP-2024-30Y-FIN-AC04-BANK11', 'BR-GROUP-2024-30Y-FIN-AC04-BANK11.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-BANK11.pdf', '2025-12-28 09:03:35.080539', 'other-bank-2024-11', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-BANK11", "maketime": "2024-11-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-BANK11', '2024-11-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('857cfc51-58b2-0857-f184-256cc23cbbc6', 'BR-GROUP-2024-30Y-FIN-AC04-BANK10', 'BR-GROUP-2024-30Y-FIN-AC04-BANK10.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-BANK10.pdf', '2025-12-28 09:03:35.080539', 'other-bank-2024-10', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-BANK10", "maketime": "2024-10-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-BANK10', '2024-10-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('fb5a30fb-81f1-1a53-015c-5e9a28aecc37', 'BR-GROUP-2024-30Y-FIN-AC04-TAX11', 'BR-GROUP-2024-30Y-FIN-AC04-TAX11.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-TAX11.pdf', '2025-12-28 09:03:35.080539', 'other-tax-2024-11', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-TAX11", "maketime": "2024-12-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 168500.00, "credit_original": 168500.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-TAX11', '2024-12-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('d03019d0-5c91-f99e-ddb4-0f6c76490e3d', 'BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3', 'BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3.pdf', '2025-12-28 09:03:35.080539', 'other-tax-2024-q3', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3", "maketime": "2024-10-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 286000.00, "credit_original": 286000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-TAX-Q3', '2024-10-20', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('a467f475-8134-3cff-b214-c03a6c5acd88', 'BR-GROUP-2024-30Y-FIN-AC04-CON001', 'BR-GROUP-2024-30Y-FIN-AC04-CON001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-CON001.pdf', '2025-12-28 09:03:35.080539', 'other-contract-2024-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-CON001", "maketime": "2024-01-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 158000.00, "credit_original": 158000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-CON001', '2024-01-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('37c3fcbe-5680-5d1c-a096-973bea6c65fa', 'BR-GROUP-2024-30Y-FIN-AC04-CON002', 'BR-GROUP-2024-30Y-FIN-AC04-CON002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-CON002.pdf', '2025-12-28 09:03:35.080539', 'other-contract-2024-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-CON002", "maketime": "2024-01-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 816000.00, "credit_original": 816000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-CON002', '2024-01-01', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('5663bb69-02d8-d95f-3061-49483f6fd409', 'BR-GROUP-2024-30Y-FIN-AC04-CON003', 'BR-GROUP-2024-30Y-FIN-AC04-CON003.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC04-CON003.pdf', '2025-12-28 09:03:35.080539', 'other-contract-2024-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC04-CON003", "maketime": "2024-03-01", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 88000.00, "credit_original": 88000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC04-CON003', '2024-03-01', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('d0c20030-37e2-0a81-6e5c-dadf156e4363', 'BR-GROUP-2023-PERM-FIN-AC04-AUDIT', 'BR-GROUP-2023-PERM-FIN-AC04-AUDIT.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-PERM-FIN-AC04-AUDIT.pdf', '2025-12-28 09:03:35.080539', 'other-audit-2023', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-PERM-FIN-AC04-AUDIT", "maketime": "2024-03-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-PERM-FIN-AC04-AUDIT', '2024-03-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('9987a1c8-3bb8-85db-946e-5974b9154f1e', 'BR-GROUP-2025-30Y-FIN-AC01-0202', 'BR-GROUP-2025-30Y-FIN-AC01-0202.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2025-30Y-FIN-AC01-0202.pdf', '2025-12-28 09:03:35.080539', 'voucher-2025-02-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2025-30Y-FIN-AC01-0202", "maketime": "2025-02-20", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 358000.00, "credit_original": 358000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2025-30Y-FIN-AC01-0202', '2025-02-20', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('b32a147d-b9c4-8c52-2beb-812c0aa9cb5a', 'BR-GROUP-2024-30Y-FIN-AC01-0004', 'BR-GROUP-2024-30Y-FIN-AC01-0004.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0004.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-11-004', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0004", "maketime": "2024-11-10", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 12800.00, "credit_original": 12800.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-0004', '2024-11-10', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('93d2e117-2276-39c1-4890-185495c01a89', 'BR-GROUP-2024-30Y-FIN-AC01-0005', 'BR-GROUP-2024-30Y-FIN-AC01-0005.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0005.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-11-005', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0005", "maketime": "2024-11-25", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 856000.00, "credit_original": 856000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-0005', '2024-11-25', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('05c1073b-c872-6359-4db6-955730b315b8', 'BR-GROUP-2024-30Y-FIN-AC01-0006', 'BR-GROUP-2024-30Y-FIN-AC01-0006.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0006.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-11-006', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0006", "maketime": "2024-11-28", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 45600.00, "credit_original": 45600.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-0006', '2024-11-28', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('e321278c-eb32-b755-dbcd-3bb0eda9415a', 'BR-GROUP-2024-30Y-FIN-AC01-0007', 'BR-GROUP-2024-30Y-FIN-AC01-0007.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0007.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-11-007', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0007", "maketime": "2024-11-12", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 286500.00, "credit_original": 286500.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-0007', '2024-11-12', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('21522c99-8779-0401-090f-d9de65563aa3', 'BR-GROUP-2024-30Y-FIN-AC01-0008', 'BR-GROUP-2024-30Y-FIN-AC01-0008.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-0008.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-11-008', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-0008", "maketime": "2024-11-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 520000.00, "credit_original": 520000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-0008', '2024-11-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('734f0356-07a7-a9b2-d204-e62fa69c2329', 'BR-GROUP-2024-30Y-FIN-AC01-1001', 'BR-GROUP-2024-30Y-FIN-AC01-1001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-10-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1001", "maketime": "2024-10-05", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 85000.00, "credit_original": 85000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-1001', '2024-10-05', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('22a57159-390d-f21d-b6d4-73ced4be816f', 'BR-GROUP-2024-30Y-FIN-AC01-1002', 'BR-GROUP-2024-30Y-FIN-AC01-1002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1002.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-10-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1002", "maketime": "2024-10-12", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 468000.00, "credit_original": 468000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-1002', '2024-10-12', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('9baac94d-c7d8-4224-9d98-b5fcf103187c', 'BR-GROUP-2023-30Y-FIN-AC01-0091', 'BR-GROUP-2023-30Y-FIN-AC01-0091.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0091.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-09-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0091", "maketime": "2023-09-25", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 186500.00, "credit_original": 186500.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0091', '2023-09-25', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('bf511737-1a58-0966-c020-b3dff1c9c629', 'BR-GROUP-2024-30Y-FIN-AC01-1003', 'BR-GROUP-2024-30Y-FIN-AC01-1003.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1003.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-10-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1003", "maketime": "2024-10-25", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 842000.00, "credit_original": 842000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-1003', '2024-10-25', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('24858a2f-03f0-3327-f817-1b1ee51e716b', 'BR-GROUP-2024-30Y-FIN-AC01-1004', 'BR-GROUP-2024-30Y-FIN-AC01-1004.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1004.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-10-004', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1004", "maketime": "2024-10-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 28600.00, "credit_original": 28600.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-1004', '2024-10-18', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('38e8a8b2-4318-5124-affa-7457b5b1e784', 'BR-GROUP-2024-30Y-FIN-AC01-1005', 'BR-GROUP-2024-30Y-FIN-AC01-1005.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-1005.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-10-005', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-1005", "maketime": "2024-10-30", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 380000.00, "credit_original": 380000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-1005', '2024-10-30', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('0967e5d9-6d8d-2daf-883e-5dba15890611', 'BR-GROUP-2024-30Y-FIN-AC01-2001', 'BR-GROUP-2024-30Y-FIN-AC01-2001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-2001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-09-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-2001", "maketime": "2024-09-08", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 185000.00, "credit_original": 185000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-2001', '2024-09-08', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('716d1742-7a3c-78d3-c1a4-373198ffa9c9', 'BR-GROUP-2024-30Y-FIN-AC01-2002', 'BR-GROUP-2024-30Y-FIN-AC01-2002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2024-30Y-FIN-AC01-2002.pdf', '2025-12-28 09:03:35.080539', 'voucher-2024-09-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2024-30Y-FIN-AC01-2002", "maketime": "2024-09-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 68000.00, "credit_original": 68000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2024-30Y-FIN-AC01-2002', '2024-09-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('4a6f4204-ec2e-b3ae-1513-e45d2f0d5041', 'BR-GROUP-2023-30Y-FIN-AC01-0001', 'BR-GROUP-2023-30Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-12-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0001", "maketime": "2023-12-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 3860000.00, "credit_original": 3860000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0001', '2023-12-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('beb543f7-fb81-2c25-3be3-8b1adce57039', 'BR-GROUP-2014-10Y-FIN-AC01-0001', 'BR-GROUP-2014-10Y-FIN-AC01-0001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2014-10Y-FIN-AC01-0001.pdf', '2025-12-28 09:03:35.080539', 'voucher-2014-01-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2014-10Y-FIN-AC01-0001", "maketime": "2014-01-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 580000.00, "credit_original": 580000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2014-10Y-FIN-AC01-0001', '2014-01-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('95ed1bbd-110f-0908-831f-f943a8609c77', 'BR-GROUP-2014-10Y-FIN-AC01-0002', 'BR-GROUP-2014-10Y-FIN-AC01-0002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2014-10Y-FIN-AC01-0002.pdf', '2025-12-28 09:03:35.080539', 'voucher-2014-02-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2014-10Y-FIN-AC01-0002", "maketime": "2014-02-28", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 620000.00, "credit_original": 620000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2014-10Y-FIN-AC01-0002', '2014-02-28', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('2ece0ee9-a0b3-c907-8cf4-da85fd519232', 'BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1', 'BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1.pdf', '2025-12-28 09:03:35.080539', 'bank-2014-q1', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1", "maketime": "2014-03-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 0, "credit_original": 0, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2014-10Y-FIN-AC04-BANK-Q1', '2014-03-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('5ce177b3-a6f3-246e-b00d-c6853ad3daca', 'PENDING-2024-12-001', 'PENDING-2024-12-001.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/PENDING-2024-12-001.pdf', '2025-12-28 09:03:35.080539', 'pre-2024-12-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "PENDING-2024-12-001", "maketime": "2024-12-22", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 680.00, "credit_original": 680.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'PENDING-2024-12-001', '2024-12-22', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('2106b9d3-95dc-cb00-f411-ab3974b7519a', 'PENDING-2024-12-002', 'PENDING-2024-12-002.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/PENDING-2024-12-002.pdf', '2025-12-28 09:03:35.080539', 'pre-2024-12-002', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "PENDING-2024-12-002", "maketime": "2024-12-23", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 3500.00, "credit_original": 3500.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'PENDING-2024-12-002', '2024-12-23', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('9eec9816-0dd6-0583-2425-4da6129180ed', 'PENDING-2024-12-003', 'PENDING-2024-12-003.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/PENDING-2024-12-003.pdf', '2025-12-28 09:03:35.080539', 'pre-2024-12-003', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "PENDING-2024-12-003", "maketime": "2024-12-24", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 28000.00, "credit_original": 28000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'PENDING-2024-12-003', '2024-12-24', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('f58480af-4a2f-12e2-0151-9a584a353bf6', 'BR-GROUP-2025-30Y-FIN-AC01-0201', 'BR-GROUP-2025-30Y-FIN-AC01-0201.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2025-30Y-FIN-AC01-0201.pdf', '2025-12-28 09:03:35.080539', 'voucher-2025-02-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2025-30Y-FIN-AC01-0201", "maketime": "2025-02-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 125600.00, "credit_original": 125600.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2025-30Y-FIN-AC01-0201', '2025-02-18', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('dc5596f2-7655-62be-5566-da32c6a8535b', 'BR-GROUP-2023-30Y-FIN-AC01-0081', 'BR-GROUP-2023-30Y-FIN-AC01-0081.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0081.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-08-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0081", "maketime": "2023-08-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 75000.00, "credit_original": 75000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0081', '2023-08-15', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('9fc3ae0e-dd3c-6db9-8fba-26ffe783d509', 'BR-GROUP-2023-30Y-FIN-AC01-0071', 'BR-GROUP-2023-30Y-FIN-AC01-0071.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0071.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-07-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0071", "maketime": "2023-07-31", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 42800.00, "credit_original": 42800.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0071', '2023-07-31', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('04ce6ece-c836-0ce8-6223-0219e3abc70d', 'BR-GROUP-2023-30Y-FIN-AC01-0061', 'BR-GROUP-2023-30Y-FIN-AC01-0061.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2023-30Y-FIN-AC01-0061.pdf', '2025-12-28 09:03:35.080539', 'voucher-2023-06-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2023-30Y-FIN-AC01-0061", "maketime": "2023-06-28", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 980000.00, "credit_original": 980000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2023-30Y-FIN-AC01-0061', '2023-06-28', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('79a4e5b3-9e25-8a67-217b-e8d211281390', 'BR-GROUP-2022-30Y-FIN-AC01-0111', 'BR-GROUP-2022-30Y-FIN-AC01-0111.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0111.pdf', '2025-12-28 09:03:35.080539', 'voucher-2022-11-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0111", "maketime": "2022-11-18", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 35600.00, "credit_original": 35600.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-30Y-FIN-AC01-0111', '2022-11-18', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('7128f077-23ed-3e23-3861-d08fee1398ac', 'BR-GROUP-2022-30Y-FIN-AC01-0101', 'BR-GROUP-2022-30Y-FIN-AC01-0101.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0101.pdf', '2025-12-28 09:03:35.080539', 'voucher-2022-10-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0101", "maketime": "2022-10-22", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 168000.00, "credit_original": 168000.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-30Y-FIN-AC01-0101', '2022-10-22', NULL);
INSERT INTO public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch, summary, voucher_word, doc_date, highlight_meta) VALUES ('02bd361a-ef2f-18cb-7fd6-0eba7754c6e2', 'BR-GROUP-2022-30Y-FIN-AC01-0091', 'BR-GROUP-2022-30Y-FIN-AC01-0091.pdf', 'pdf', 0, NULL, NULL, '/tmp/nexusarchive/generated/BR-GROUP-2022-30Y-FIN-AC01-0091.pdf', '2025-12-28 09:03:35.080539', 'voucher-2022-09-001', NULL, NULL, NULL, NULL, NULL, 'ARCHIVED', NULL, 'VOUCHER', NULL, NULL, 'GENERATED', NULL, NULL, NULL, NULL, NULL, '{"header": {"displayname": "BR-GROUP-2022-30Y-FIN-AC01-0091", "maketime": "2022-09-15", "attachmentQuantity": "0", "vouchertype": {"voucherstr": "记"}}, "bodies": [{"description": "自动生成凭证", "debit_original": 128500.00, "credit_original": 128500.00, "subjectName": "自动生成科目"}]}', NULL, NULL, NULL, 'BR-GROUP-2022-30Y-FIN-AC01-0091', '2022-09-15', NULL);


--
-- Data for Name: arc_file_metadata_index; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_import_batch; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_original_voucher; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) VALUES ('ov-demo-inv-004', 'OV-2025-INV-004', 'INVOICE', 'VAT_INVOICE', '2025-08-09', 50.00, 'CNY', 'Test Customer', '销售服务费发票', NULL, NULL, NULL, NULL, NULL, NULL, 'DEMO', '2025', '30Y', 'ARCHIVED', NULL, 1, NULL, NULL, true, NULL, '2025-12-28 09:03:35.11446', NULL, '2025-12-28 09:03:35.11446', 0, 'ARCHIVED', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) VALUES ('ov-demo-bank-004', 'OV-2025-BANK-004', 'BANK', 'BANK_SLIP', '2025-08-09', 50.00, 'CNY', 'Test Customer', '服务费收款回单', NULL, NULL, NULL, NULL, NULL, NULL, 'DEMO', '2025', '30Y', 'ARCHIVED', NULL, 1, NULL, NULL, true, NULL, '2025-12-28 09:03:35.11446', NULL, '2025-12-28 09:03:35.11446', 0, 'ARCHIVED', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) VALUES ('ov-demo-inv-003', 'OV-2025-INV-003', 'INVOICE', 'VAT_INVOICE', '2025-08-09', 30.00, 'CNY', 'Test Customer', '商品销售发票', NULL, NULL, NULL, NULL, NULL, NULL, 'DEMO', '2025', '30Y', 'ARCHIVED', NULL, 1, NULL, NULL, true, NULL, '2025-12-28 09:03:35.11446', NULL, '2025-12-28 09:03:35.11446', 0, 'ARCHIVED', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) VALUES ('ov-test-100-1', 'OV-2025-TEST-01', 'INVOICE', 'VAT_INVOICE', '2025-12-22', 100.00, 'CNY', '京东办公', '办公用品采购', NULL, NULL, NULL, NULL, NULL, NULL, 'DEMO', '2025', '30Y', 'ARCHIVED', NULL, 1, NULL, NULL, true, NULL, '2025-12-28 09:03:35.118758', NULL, '2025-12-28 09:03:35.118758', 0, 'ARCHIVED', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) VALUES ('ov-test-100-2', 'OV-2025-TEST-02', 'INVOICE', 'VAT_INVOICE', '2025-12-22', 100.00, 'CNY', '海底捞餐饮', '客户接待', NULL, NULL, NULL, NULL, NULL, NULL, 'DEMO', '2025', '30Y', 'ARCHIVED', NULL, 1, NULL, NULL, true, NULL, '2025-12-28 09:03:35.118758', NULL, '2025-12-28 09:03:35.118758', 0, 'ARCHIVED', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1);
INSERT INTO public.arc_original_voucher (id, voucher_no, voucher_category, voucher_type, business_date, amount, currency, counterparty, summary, creator, auditor, bookkeeper, approver, source_system, source_doc_id, fonds_code, fiscal_year, retention_period, archive_status, archived_time, version, parent_version_id, version_reason, is_latest, created_by, created_time, last_modified_by, last_modified_time, deleted, pool_status, row_version, pool_batch_id, parsed_payload, parsed_at, matched_voucher_id, matched_at, deleted_at, deleted_by, delete_reason, tenant_id) VALUES ('ov-brjt-45k-1', 'OV-2024-BANK-45K', 'BANK', 'BANK_SLIP', '2024-03-15', 45000.00, 'CNY', '某大客户', '服务费收款', NULL, NULL, NULL, NULL, NULL, NULL, 'DEMO', '2024', '30Y', 'ARCHIVED', NULL, 1, NULL, NULL, true, NULL, '2025-12-28 09:03:35.118758', NULL, '2025-12-28 09:03:35.118758', 0, 'ARCHIVED', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1);


--
-- Data for Name: arc_original_voucher_event; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_original_voucher_file; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) VALUES ('file-ov-inv-004', 'ov-demo-inv-004', 'invoice_50.pdf', 'PDF', 102400, 'uploads/demo/invoice_50.pdf', 'hash_inv_004', 'SM3', NULL, NULL, NULL, NULL, NULL, 'PRIMARY', 1, NULL, '2025-12-28 09:03:35.11446', 0);
INSERT INTO public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) VALUES ('file-ov-bank-004', 'ov-demo-bank-004', 'bank_receipt_50.pdf', 'PDF', 102400, 'uploads/demo/bank_50.pdf', 'hash_bank_004', 'SM3', NULL, NULL, NULL, NULL, NULL, 'PRIMARY', 1, NULL, '2025-12-28 09:03:35.11446', 0);
INSERT INTO public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) VALUES ('file-ov-inv-003', 'ov-demo-inv-003', 'invoice_30.pdf', 'PDF', 102400, 'uploads/demo/invoice_30.pdf', 'hash_inv_003', 'SM3', NULL, NULL, NULL, NULL, NULL, 'PRIMARY', 1, NULL, '2025-12-28 09:03:35.11446', 0);
INSERT INTO public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) VALUES ('f-test-100-1', 'ov-test-100-1', 'office_supplies_100.pdf', 'PDF', 20480, 'uploads/demo/office_100.pdf', 'hash_test_1', 'SM3', NULL, NULL, NULL, NULL, NULL, 'PRIMARY', 1, NULL, '2025-12-28 09:03:35.118758', 0);
INSERT INTO public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) VALUES ('f-test-100-2', 'ov-test-100-2', 'food_receipt_100.pdf', 'PDF', 20480, 'uploads/demo/food_100.pdf', 'hash_test_2', 'SM3', NULL, NULL, NULL, NULL, NULL, 'PRIMARY', 1, NULL, '2025-12-28 09:03:35.118758', 0);
INSERT INTO public.arc_original_voucher_file (id, voucher_id, file_name, file_type, file_size, storage_path, file_hash, hash_algorithm, original_hash, sign_value, sign_cert, sign_time, timestamp_token, file_role, sequence_no, created_by, created_time, deleted) VALUES ('f-brjt-45k-1', 'ov-brjt-45k-1', 'bank_receipt_45000.pdf', 'PDF', 51200, 'uploads/demo/bank_45k.pdf', 'hash_brjt_1', 'SM3', NULL, NULL, NULL, NULL, NULL, 'PRIMARY', 1, NULL, '2025-12-28 09:03:35.118758', 0);


--
-- Data for Name: arc_original_voucher_sequence; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_reconciliation_record; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_signature_log; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: arc_voucher_relation; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: archive_amendment; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: archive_batch; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: archive_batch_item; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: audit_inspection_log; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, hash_snapshot, integrity_check, authenticity_check, availability_check, security_check, check_result, detail_report, created_at, report_file_path, report_file_hash, is_compliant, compliance_violations, compliance_warnings) VALUES ('inspection-v2024-11-001', 'voucher-2024-11-001', 'ARCHIVE', '2025-12-28 09:03:35.017527', 'user-zhangsan', true, true, true, true, '4fe6caa86fdc175a7cb35887ba5e3ee95460250cd00f7c3b84478af3720d696e', NULL, NULL, NULL, NULL, 'PASS', NULL, '2025-12-28 09:03:35.017527', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, hash_snapshot, integrity_check, authenticity_check, availability_check, security_check, check_result, detail_report, created_at, report_file_path, report_file_hash, is_compliant, compliance_violations, compliance_warnings) VALUES ('inspection-v2024-11-002', 'voucher-2024-11-002', 'ARCHIVE', '2025-12-28 09:03:35.017527', 'user-zhangsan', true, true, true, true, '4c40ce396c10762acfd891c897f986c7646cecc335ced88a7c8d9e10cac44f02', NULL, NULL, NULL, NULL, 'PASS', NULL, '2025-12-28 09:03:35.017527', NULL, NULL, NULL, NULL, NULL);
INSERT INTO public.audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, hash_snapshot, integrity_check, authenticity_check, availability_check, security_check, check_result, detail_report, created_at, report_file_path, report_file_hash, is_compliant, compliance_violations, compliance_warnings) VALUES ('inspection-v2024-11-003', 'voucher-2024-11-003', 'ARCHIVE', '2025-12-28 09:03:35.017527', 'user-lisi', true, true, true, true, 'b88176ca3d3dcc0ddd3e9da3cda5c8712ad0c2abde9e6293679dbab5177d562e', NULL, NULL, NULL, NULL, 'PASS', NULL, '2025-12-28 09:03:35.017527', NULL, NULL, NULL, NULL, NULL);


--
-- Data for Name: bas_erp_config; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: bas_fonds; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id) VALUES ('demo-fonds-001', 'DEMO', '演示全宗', '演示公司', '系统初始演示数据', 'system', '2025-12-28 09:03:34.854164', '2025-12-28 09:03:34.854164', NULL);
INSERT INTO public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id) VALUES ('fonds-br-group', 'BR-GROUP', '泊冉集团有限公司', '泊冉集团有限公司', '集团总部档案全宗', 'system', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', NULL);
INSERT INTO public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id) VALUES ('fonds-br-sales', 'BR-SALES', '泊冉销售有限公司', '泊冉销售有限公司', '销售公司档案全宗', 'system', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', NULL);
INSERT INTO public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id) VALUES ('fonds-br-trade', 'BR-TRADE', '泊冉国际贸易有限公司', '泊冉国际贸易有限公司', '贸易公司档案全宗', 'system', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', NULL);
INSERT INTO public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id) VALUES ('fonds-br-mfg', 'BR-MFG', '泊冉制造有限公司', '泊冉制造有限公司', '制造公司档案全宗', 'system', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', NULL);


--
-- Data for Name: bas_location; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) VALUES ('loc-warehouse-main', '主档案库房', 'W-MAIN', 'WAREHOUSE', '0', '/主档案库房', 10000, 2850, 'NORMAL', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) VALUES ('loc-area-a', 'A区-会计凭证区', 'A-VOUCHER', 'AREA', 'loc-warehouse-main', '/主档案库房/A区-会计凭证区', 3000, 1580, 'NORMAL', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) VALUES ('loc-area-b', 'B区-财务报告区', 'B-REPORT', 'AREA', 'loc-warehouse-main', '/主档案库房/B区-财务报告区', 2000, 680, 'NORMAL', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) VALUES ('loc-shelf-a1', 'A1号架', 'A1', 'SHELF', 'loc-area-a', '/主档案库房/A区-会计凭证区/A1号架', 500, 486, 'NORMAL', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) VALUES ('loc-shelf-a2', 'A2号架', 'A2', 'SHELF', 'loc-area-a', '/主档案库房/A区-会计凭证区/A2号架', 500, 320, 'NORMAL', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);


--
-- Data for Name: biz_archive_approval; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: biz_borrowing; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, updated_at, deleted) VALUES ('borrow-001', 'user-wangwu', '王五', 'voucher-2024-11-008', '销售商品确认收入', '年度审计核查销售收入确认', '2024-12-15', '2024-12-25', NULL, 'APPROVED', '审批通过，请于期限内归还', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, updated_at, deleted) VALUES ('borrow-002', 'user-lisi', '李四', 'report-2023-annual', '2023年度财务决算报告', '编制2024年预算参考', '2024-11-20', '2024-11-30', '2024-11-28', 'RETURNED', '已按期归还', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, updated_at, deleted) VALUES ('borrow-003', 'user-zhaoliu', '赵六', 'other-contract-2024-002', '办公楼租赁合同', '核对租金支付条款', '2024-12-18', '2024-12-22', NULL, 'PENDING', NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, updated_at, deleted) VALUES ('borrow-004', 'user-wangwu', '王五', 'voucher-2023-10-001', '采购生产设备', '专项审计-固定资产核查', '2024-10-10', '2024-10-20', '2024-10-18', 'RETURNED', '已完成审计', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, updated_at, deleted) VALUES ('borrow-005', 'user-zhangsan', '张三', 'ledger-2024-002', '2024年度银行存款明细账', '月末对账核实', '2024-12-20', '2024-12-23', NULL, 'APPROVED', '同意借阅', '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);


--
-- Data for Name: biz_destruction; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, approver_id, approver_name, approval_comment, approval_time, execution_time, created_at, updated_at, deleted) VALUES ('destruction-2024-001', 'user-qianqi', '钱七', '保管期限已满10年，经鉴定无继续保存价值', 3, '["voucher-2014-01-001","voucher-2014-02-001","bank-2014-q1"]', 'PENDING', NULL, NULL, NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);


--
-- Data for Name: biz_open_appraisal; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: cfg_account_role_mapping; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: cfg_account_role_preset; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (1, 'KIT_GENERAL', '^1001.*', 'CASH', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (2, 'KIT_GENERAL', '^1002.*', 'BANK', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (3, 'KIT_GENERAL', '^1122.*', 'RECEIVABLE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (4, 'KIT_GENERAL', '^1123.*', 'RECEIVABLE', 90, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (5, 'KIT_GENERAL', '^2202.*', 'PAYABLE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (6, 'KIT_GENERAL', '^2203.*', 'PAYABLE', 90, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (7, 'KIT_GENERAL', '^2211.*', 'SALARY', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (8, 'KIT_GENERAL', '^2221.*', 'TAX', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (9, 'KIT_GENERAL', '^1601.*', 'ASSET', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (10, 'KIT_GENERAL', '^1602.*', 'ASSET', 90, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (11, 'KIT_GENERAL', '^6601.*', 'EXPENSE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (12, 'KIT_GENERAL', '^6602.*', 'EXPENSE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (13, 'KIT_GENERAL', '^6603.*', 'EXPENSE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (14, 'KIT_GENERAL', '^6001.*', 'REVENUE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_account_role_preset (id, kit_id, account_pattern, account_role, priority, created_time) VALUES (15, 'KIT_GENERAL', '^6051.*', 'REVENUE', 90, '2025-12-28 09:03:35.090742');


--
-- Data for Name: cfg_doc_type_mapping; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.cfg_doc_type_mapping (id, company_id, customer_doc_type, evidence_role, display_name, source, created_time, updated_time) VALUES (1, 1, 'VAT_INVOICE', 'INVOICE', '增值税专用发票', 'PRESET', '2025-12-28 09:03:35.11446', '2025-12-28 09:03:35.11446');
INSERT INTO public.cfg_doc_type_mapping (id, company_id, customer_doc_type, evidence_role, display_name, source, created_time, updated_time) VALUES (2, 1, 'BANK_SLIP', 'BANK_RECEIPT', '银行电子回单', 'PRESET', '2025-12-28 09:03:35.11446', '2025-12-28 09:03:35.11446');
INSERT INTO public.cfg_doc_type_mapping (id, company_id, customer_doc_type, evidence_role, display_name, source, created_time, updated_time) VALUES (3, 1, 'SALES_ORDER', 'CONTRACT', '销售订单', 'PRESET', '2025-12-28 09:03:35.11446', '2025-12-28 09:03:35.11446');


--
-- Data for Name: cfg_doc_type_preset; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) VALUES (1, 'KIT_GENERAL', '付款.*', '{付款审批,付款申请,付款指令}', 'AUTHORIZATION', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) VALUES (2, 'KIT_GENERAL', '银行.*|回单.*', '{银行回单,转账回单,支付凭证,网银回单}', 'SETTLEMENT', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) VALUES (3, 'KIT_GENERAL', '发票.*|增值税.*', '{发票,增值税专用发票,增值税普通发票}', 'TAX_EVIDENCE', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) VALUES (4, 'KIT_GENERAL', '合同.*|协议.*|订单.*', '{合同,协议,订单,框架协议}', 'CONTRACTUAL_BASIS', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) VALUES (5, 'KIT_GENERAL', '入库.*|验收.*|签收.*', '{入库单,验收单,签收单,收货单}', 'EXECUTION_PROOF', 100, '2025-12-28 09:03:35.090742');
INSERT INTO public.cfg_doc_type_preset (id, kit_id, doc_type_pattern, keywords, evidence_role, priority, created_time) VALUES (6, 'KIT_GENERAL', '报销.*|费用.*', '{报销单,费用申请,差旅报销}', 'ACCOUNTING_TRIGGER', 100, '2025-12-28 09:03:35.090742');


--
-- Data for Name: cfg_preset_kit; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.cfg_preset_kit (id, industry, name, description, is_default, created_time) VALUES ('KIT_GENERAL', 'GENERAL', '通用行业预置包', '适用于大多数企业的默认规则', true, '2025-12-28 09:03:35.090742');


--
-- Data for Name: integrity_check; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: match_log; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: match_rule_template; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.match_rule_template (id, name, version, status, scene, config, description, created_time, updated_time, updated_by) VALUES ('T01_PAYMENT', '付款业务关联规则', 'v1.0', 'ACTIVE', 'PAYMENT', '{"mustLink": [{"strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票", "增值税"]}, {"strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"], "evidenceRole": "SETTLEMENT", "docTypeKeywords": ["银行回单", "付款凭证"]}], "shouldLink": [{"strategies": ["FUZZY_NAME", "REF_NO"], "evidenceRole": "CONTRACTUAL_BASIS", "docTypeKeywords": ["合同", "协议"]}]}', '适用于标准对公付款业务，需匹配发票和银行回单', '2025-12-28 09:03:35.110308', '2025-12-28 09:03:35.110308', NULL);
INSERT INTO public.match_rule_template (id, name, version, status, scene, config, description, created_time, updated_time, updated_by) VALUES ('T02_RECEIPT', '收款业务关联规则', 'v1.0', 'ACTIVE', 'RECEIPT', '{"mustLink": [{"strategies": ["AMOUNT_EXACT", "DATE_PROXIMITY"], "evidenceRole": "SETTLEMENT", "docTypeKeywords": ["银行回单", "收款凭证"]}], "shouldLink": [{"strategies": ["AMOUNT_EXACT", "FUZZY_NAME"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票", "销售发票"]}]}', '适用于标准销售收款业务', '2025-12-28 09:03:35.110308', '2025-12-28 09:03:35.110308', NULL);
INSERT INTO public.match_rule_template (id, name, version, status, scene, config, description, created_time, updated_time, updated_by) VALUES ('T03_EXPENSE', '费用报销关联规则', 'v1.0', 'ACTIVE', 'EXPENSE', '{"mayLink": [{"strategies": ["AMOUNT_EXACT"], "evidenceRole": "SETTLEMENT", "docTypeKeywords": ["支付凭证"]}], "mustLink": [{"strategies": ["AMOUNT_EXACT", "REF_NO"], "evidenceRole": "ACCOUNTING_TRIGGER", "docTypeKeywords": ["报销单", "费用申请"]}, {"strategies": ["DATE_PROXIMITY"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票", "行程单"]}]}', '适用于员工费用报销业务', '2025-12-28 09:03:35.110308', '2025-12-28 09:03:35.110308', NULL);
INSERT INTO public.match_rule_template (id, name, version, status, scene, config, description, created_time, updated_time, updated_by) VALUES ('T04_PURCHASE', '采购供应链关联规则', 'v1.0', 'ACTIVE', 'PURCHASE_IN', '{"mustLink": [{"strategies": ["REF_NO", "FUZZY_NAME"], "evidenceRole": "CONTRACTUAL_BASIS", "docTypeKeywords": ["采购订单", "合同"]}, {"strategies": ["DATE_PROXIMITY", "REF_NO"], "evidenceRole": "EXECUTION_PROOF", "docTypeKeywords": ["入库单", "验收单"]}], "shouldLink": [{"strategies": ["AMOUNT_EXACT"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票"]}]}', '适用于原材料或商品采购入库', '2025-12-28 09:03:35.110308', '2025-12-28 09:03:35.110308', NULL);
INSERT INTO public.match_rule_template (id, name, version, status, scene, config, description, created_time, updated_time, updated_by) VALUES ('T05_SALES', '销售供应链关联规则', 'v1.0', 'ACTIVE', 'SALES_OUT', '{"mustLink": [{"strategies": ["REF_NO", "FUZZY_NAME"], "evidenceRole": "CONTRACTUAL_BASIS", "docTypeKeywords": ["销售订单"]}, {"strategies": ["DATE_PROXIMITY", "REF_NO"], "evidenceRole": "EXECUTION_PROOF", "docTypeKeywords": ["出库单", "发货单"]}], "shouldLink": [{"strategies": ["AMOUNT_EXACT"], "evidenceRole": "TAX_EVIDENCE", "docTypeKeywords": ["发票"]}]}', '适用于商品销售出库', '2025-12-28 09:03:35.110308', '2025-12-28 09:03:35.110308', NULL);


--
-- Data for Name: period_lock; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sys_archival_code_sequence; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sys_audit_log; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, client_ip, mac_address, object_digest, user_agent, prev_log_hash, log_hash, device_fingerprint, created_time) VALUES ('auditlog-001', 'user-zhangsan', 'zhangsan', NULL, 'LOGIN', 'USER', 'user-zhangsan', 'SUCCESS', NULL, '用户登录系统', NULL, NULL, NULL, '192.168.1.100', 'UNKNOWN', NULL, NULL, NULL, NULL, NULL, '2025-12-28 07:03:35.017527');
INSERT INTO public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, client_ip, mac_address, object_digest, user_agent, prev_log_hash, log_hash, device_fingerprint, created_time) VALUES ('auditlog-002', 'user-lisi', 'lisi', NULL, 'VIEW', 'ARCHIVE', 'voucher-2024-11-008', 'SUCCESS', NULL, '查看凭证详情：销售商品确认收入', NULL, NULL, NULL, '192.168.1.102', 'UNKNOWN', NULL, NULL, NULL, NULL, NULL, '2025-12-28 08:03:35.017527');
INSERT INTO public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, client_ip, mac_address, object_digest, user_agent, prev_log_hash, log_hash, device_fingerprint, created_time) VALUES ('auditlog-003', 'user-wangwu', 'wangwu', NULL, 'CREATE', 'BORROWING', 'borrow-001', 'SUCCESS', NULL, '提交借阅申请：年度审计核查销售收入确认', NULL, NULL, NULL, '192.168.1.105', 'UNKNOWN', NULL, NULL, NULL, NULL, NULL, '2025-12-28 08:33:35.017527');
INSERT INTO public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, client_ip, mac_address, object_digest, user_agent, prev_log_hash, log_hash, device_fingerprint, created_time) VALUES ('673da58dfbcb460faec0487449453a3c', 'UNKNOWN', 'anonymousUser', NULL, 'LOGIN', 'AUTH', NULL, 'SUCCESS', 'MEDIUM', '用户登录', '{"username":"zhangsan","password":"admin123"}', '{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyLXpoYW5nc2FuIiwidXNlcm5hbWUiOiJ6aGFuZ3NhbiIsInN1YiI6InVzZXItemhhbmdzYW4iLCJpYXQiOjE3NjY4ODM5OTksImV4cCI6MTc2Njg4NzU5OX0.fnsoS2eS4qS4f9DoD3PuGGgdQoQCX5s3GXbcfYWIuywTQQw5RMMNn8b1fRbLjFJfsgqCjotJDAI7AEsHARg7E0RxTJxmqVvSNLPXymA2ABp5VkNu4EZOpuP73a6ymDC9bQssF5z8HlD9u8jFaoBkWZg3lGHtxtfOkoQCUufZqQpZHI21_-dkadaEp2T9mLVX_AC29SKWgY31D75nwdNyGFUHqRpR5wOjp0X7ZW95ZdKz2QIGNR_rEQxFJX1uLv93gx_DkSBiAtpa3Ko4PvA2sExXkj5jovLr6nqXum2W1LtcHcHw6tsKUaTvyuH22ExK5xk1untzpSXeovzp6-U0Ng","user":{"id":"user-zhangsan","username":"zhangsan","fullName":"张三","email":"zhangsan@boran.com","avatar":null,"departmentId":"ORG_BR_GROUP_FIN","status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","archive:view","nav:archive_mgmt","archive:manage","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","archive:read","audit:view","nav:pre_archive"]}},"timestamp":1766883999411}', NULL, '0:0:0:0:0:0:0:1', 'UNKNOWN', NULL, 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1', NULL, NULL, 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|', '2025-12-28 09:06:39.415193');


--
-- Data for Name: sys_env_marker; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_env_marker (marker_key, marker_value, created_at) VALUES ('INSTANCE_SIG', 'NEXUS_ARCHIVE_SAFE_INSTANCE', '2025-12-28 09:14:13.474449');


--
-- Data for Name: sys_erp_config; Type: TABLE DATA; Schema: public; Owner: -
--

-- ERP配置数据已迁移到独立的 demo 数据文件
-- V1__init_baseline_2025.sql 保持为空表，避免重复测试数据
-- 如需演示数据，请执行 V1xx__seed_demo_erp_configs.sql


--
-- Data for Name: sys_erp_feedback_queue; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sys_erp_scenario; Type: TABLE DATA; Schema: public; Owner: -
--

-- ERP场景数据依赖于 sys_erp_config，已同步迁移
-- 如需演示数据，请执行 V1xx__seed_demo_erp_configs.sql


--
-- Data for Name: sys_erp_sub_interface; Type: TABLE DATA; Schema: public; Owner: -
--

-- ERP子接口数据依赖于 sys_erp_scenario，已同步迁移
-- 如需演示数据，请执行 V1xx__seed_demo_erp_configs.sql


--
-- Data for Name: sys_ingest_request_status; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sys_org; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_GROUP', '泊冉集团有限公司', 'BR-GROUP', NULL, 'COMPANY', 1, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_SALES', '泊冉销售有限公司', 'BR-SALES', 'ORG_BR_GROUP', 'COMPANY', 1, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_TRADE', '泊冉国际贸易有限公司', 'BR-TRADE', 'ORG_BR_GROUP', 'COMPANY', 2, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_MFG', '泊冉制造有限公司', 'BR-MFG', 'ORG_BR_GROUP', 'COMPANY', 3, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_GROUP_FIN', '财务管理部', 'BR-GROUP-FIN', 'ORG_BR_GROUP', 'DEPARTMENT', 10, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_GROUP_HR', '人力资源部', 'BR-GROUP-HR', 'ORG_BR_GROUP', 'DEPARTMENT', 11, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_GROUP_IT', '信息技术部', 'BR-GROUP-IT', 'ORG_BR_GROUP', 'DEPARTMENT', 12, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_GROUP_LEGAL', '法务合规部', 'BR-GROUP-LEGAL', 'ORG_BR_GROUP', 'DEPARTMENT', 13, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_GROUP_AUDIT', '审计监察部', 'BR-GROUP-AUDIT', 'ORG_BR_GROUP', 'DEPARTMENT', 14, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_SALES_DOM', '国内销售部', 'BR-SALES-DOM', 'ORG_BR_SALES', 'DEPARTMENT', 1, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_SALES_INT', '海外销售部', 'BR-SALES-INT', 'ORG_BR_SALES', 'DEPARTMENT', 2, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_SALES_MKT', '市场推广部', 'BR-SALES-MKT', 'ORG_BR_SALES', 'DEPARTMENT', 3, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_SALES_FIN', '财务部', 'BR-SALES-FIN', 'ORG_BR_SALES', 'DEPARTMENT', 10, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_TRADE_IMP', '进口业务部', 'BR-TRADE-IMP', 'ORG_BR_TRADE', 'DEPARTMENT', 1, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_TRADE_EXP', '出口业务部', 'BR-TRADE-EXP', 'ORG_BR_TRADE', 'DEPARTMENT', 2, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_TRADE_LOG', '物流仓储部', 'BR-TRADE-LOG', 'ORG_BR_TRADE', 'DEPARTMENT', 3, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_TRADE_FIN', '财务部', 'BR-TRADE-FIN', 'ORG_BR_TRADE', 'DEPARTMENT', 10, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_MFG_PROD', '生产管理部', 'BR-MFG-PROD', 'ORG_BR_MFG', 'DEPARTMENT', 1, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_MFG_QC', '质量控制部', 'BR-MFG-QC', 'ORG_BR_MFG', 'DEPARTMENT', 2, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_MFG_RD', '研发技术部', 'BR-MFG-RD', 'ORG_BR_MFG', 'DEPARTMENT', 3, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_MFG_SUPPLY', '采购供应部', 'BR-MFG-SUPPLY', 'ORG_BR_MFG', 'DEPARTMENT', 4, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);
INSERT INTO public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) VALUES ('ORG_BR_MFG_FIN', '财务部', 'BR-MFG-FIN', 'ORG_BR_MFG', 'DEPARTMENT', 10, '2025-12-28 09:03:34.916025', '2025-12-28 09:03:34.916025', 0);


--
-- Data for Name: sys_original_voucher_type; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-001', 'INVOICE', '发票类', 'INV_PAPER', '纸质发票', '30Y', 1, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-002', 'INVOICE', '发票类', 'INV_VAT_E', '增值税电子发票', '30Y', 2, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-003', 'INVOICE', '发票类', 'INV_DIGITAL', '数电发票', '30Y', 3, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-004', 'INVOICE', '发票类', 'INV_RAIL', '数电票（铁路）', '30Y', 4, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-005', 'INVOICE', '发票类', 'INV_AIR', '数电票（航空）', '30Y', 5, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-006', 'INVOICE', '发票类', 'INV_GOV', '数电票（财政）', '30Y', 6, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-007', 'BANK', '银行类', 'BANK_RECEIPT', '银行回单', '30Y', 10, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-009', 'DOCUMENT', '单据类', 'DOC_PAYMENT', '付款单', '30Y', 20, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-010', 'DOCUMENT', '单据类', 'DOC_RECEIPT', '收款单', '30Y', 21, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-011', 'DOCUMENT', '单据类', 'DOC_RECEIPT_VOUCHER', '收款单据（收据）', '30Y', 22, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-012', 'DOCUMENT', '单据类', 'DOC_PAYROLL', '工资单', '30Y', 23, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-014', 'CONTRACT', '合同类', 'AGREEMENT', '协议', '30Y', 31, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('ovt-015', 'OTHER', '其他类', 'OTHER', '其他', '30Y', 99, true, '2025-12-28 09:03:34.992088', NULL);
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-DOC-001', 'DOCUMENT', '单据类', 'SALES_ORDER', '销售订单', '30Y', 10, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-DOC-002', 'DOCUMENT', '单据类', 'DELIVERY_ORDER', '出库单', '30Y', 20, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-DOC-003', 'DOCUMENT', '单据类', 'PURCHASE_ORDER', '采购订单', '30Y', 30, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-DOC-004', 'DOCUMENT', '单据类', 'RECEIPT_ORDER', '入库单', '30Y', 40, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-DOC-005', 'DOCUMENT', '单据类', 'PAYMENT_REQ', '付款申请单', '30Y', 50, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-DOC-006', 'DOCUMENT', '单据类', 'EXPENSE_REPORT', '报销单', '30Y', 60, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-INV-001', 'INVOICE', '发票类', 'GEN_INVOICE', '普通发票', '30Y', 10, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-INV-002', 'INVOICE', '发票类', 'VAT_INVOICE', '增值税专票', '30Y', 20, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-BNK-001', 'BANK', '银行类', 'BANK_SLIP', '银行回单', '30Y', 10, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');
INSERT INTO public.sys_original_voucher_type (id, category_code, category_name, type_code, type_name, default_retention, sort_order, enabled, created_time, last_modified_time) VALUES ('OVT-CON-001', 'CONTRACT', '合同类', 'CONTRACT', '合同协议', '30Y', 10, true, '2025-12-28 09:03:35.003657', '2025-12-28 09:03:35.003657');


--
-- Data for Name: sys_permission; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_manage_users', 'manage_users', '用户管理', '系统管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_manage_roles', 'manage_roles', '角色管理', '系统管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_manage_org', 'manage_org', '组织管理', '系统管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_manage_settings', 'manage_settings', '系统设置', '系统管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_manage_fonds', 'manage_fonds', '全宗管理', '系统管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_all', 'nav:all', '所有导航', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_portal', 'nav:portal', '门户首页', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_panorama', 'nav:panorama', '全景视图', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_pre_archive', 'nav:pre_archive', '预归档库', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_collection', 'nav:collection', '资料收集', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_archive_mgmt', 'nav:archive_mgmt', '档案管理', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_query', 'nav:query', '档案查询', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_borrowing', 'nav:borrowing', '档案借阅', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_destruction', 'nav:destruction', '档案销毁', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_warehouse', 'nav:warehouse', '库房管理', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_stats', 'nav:stats', '数据统计', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_nav_settings', 'nav:settings', '系统设置', '导航权限', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_create', 'archive:create', '创建档案', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_view', 'archive:view', '查看档案', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_edit', 'archive:edit', '编辑档案', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_delete', 'archive:delete', '删除档案', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_download', 'archive:download', '下载档案', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_print', 'archive:print', '打印档案', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_archive_approve', 'archive:approve', '审批归档', '档案操作', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_borrow_apply', 'borrow:apply', '申请借阅', '借阅管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_borrow_approve', 'borrow:approve', '审批借阅', '借阅管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_destruction_apply', 'destruction:apply', '销毁鉴定', '销毁管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_destruction_approve', 'destruction:approve', '审批销毁', '销毁管理', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_audit_view', 'audit:view', '查看审计日志', '安全审计', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');
INSERT INTO public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) VALUES ('perm_audit_export', 'audit:export', '导出审计日志', '安全审计', '2025-12-28 09:03:34.86437', '2025-12-28 09:03:34.86437');


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_role (id, name, code, role_category, is_exclusive, description, permissions, data_scope, type, created_at, updated_at, deleted) VALUES ('role_super_admin', '超级管理员', 'super_admin', 'system_admin', false, NULL, '["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users","archive:read","archive:view","archive:manage","audit:view"]', 'self', 'custom', '2025-12-28 09:03:34.791488', '2025-12-28 09:03:34.791488', 0);


--
-- Data for Name: sys_setting; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted) VALUES ('2005083092585488385', 'system.name', 'Nexus Archive System', '系统名称', NULL, 'system', NULL, 0);
INSERT INTO public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted) VALUES ('2005083092585488386', 'archive.prefix', 'QZ-2024-', '档号前缀', NULL, 'archive', NULL, 0);
INSERT INTO public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted) VALUES ('2005083092589682689', 'storage.type', 'local', '存储类型 local/nas/oss', NULL, 'storage', NULL, 0);
INSERT INTO public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted) VALUES ('2005083092589682690', 'storage.path', '/data/archive', '本地存储路径', NULL, 'storage', NULL, 0);
INSERT INTO public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted) VALUES ('2005083092589682691', 'retention.default', '10Y', '默认保管期限', NULL, 'archive', NULL, 0);


--
-- Data for Name: sys_sync_history; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) VALUES ('user_admin_001', 'admin', '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA', '系统管理员', 'BR-GROUP', 'admin@nexusarchive.local', NULL, NULL, 'ORG_BR_GROUP_FIN', 'active', NULL, NULL, NULL, NULL, '2025-12-28 09:03:34.791488', '2025-12-28 09:03:34.791488', 0);
INSERT INTO public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) VALUES ('user-lisi', 'lisi', '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA', '李四', 'BR-SALES', 'lisi@boran.com', NULL, NULL, 'ORG_BR_SALES_FIN', 'active', NULL, NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) VALUES ('user-wangwu', 'wangwu', '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA', '王五', 'BR-GROUP', 'wangwu@boran.com', NULL, NULL, 'ORG_BR_GROUP_AUDIT', 'active', NULL, NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) VALUES ('user-zhaoliu', 'zhaoliu', '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA', '赵六', 'BR-MFG', 'zhaoliu@boran.com', NULL, NULL, 'ORG_BR_MFG_FIN', 'active', NULL, NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) VALUES ('user-qianqi', 'qianqi', '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA', '钱七', 'BR-GROUP', 'qianqi@boran.com', NULL, NULL, 'ORG_BR_GROUP_FIN', 'active', NULL, NULL, NULL, NULL, '2025-12-28 09:03:35.017527', '2025-12-28 09:03:35.017527', 0);
INSERT INTO public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) VALUES ('user-zhangsan', 'zhangsan', '$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA', '张三', 'BR-GROUP', 'zhangsan@boran.com', NULL, NULL, 'ORG_BR_GROUP_FIN', 'active', '2025-12-28 09:06:39.402242', NULL, NULL, NULL, '2025-12-28 09:03:35.017527', NULL, 0);


--
-- Data for Name: sys_user_role; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sys_user_role (user_id, role_id) VALUES ('user_admin_001', 'role_super_admin');
INSERT INTO public.sys_user_role (user_id, role_id) VALUES ('user-zhangsan', 'role_super_admin');
INSERT INTO public.sys_user_role (user_id, role_id) VALUES ('user-lisi', 'role_super_admin');
INSERT INTO public.sys_user_role (user_id, role_id) VALUES ('user-wangwu', 'role_super_admin');
INSERT INTO public.sys_user_role (user_id, role_id) VALUES ('user-zhaoliu', 'role_super_admin');
INSERT INTO public.sys_user_role (user_id, role_id) VALUES ('user-qianqi', 'role_super_admin');


--
-- Data for Name: voucher_match_result; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: voucher_source_link; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: ys_sales_out; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: ys_sales_out_detail; Type: TABLE DATA; Schema: public; Owner: -
--



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
-- Name: sys_erp_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

-- ERP配置数据已删除，序列重置为 0（首次插入将从 1 开始）
SELECT pg_catalog.setval('public.sys_erp_config_id_seq', 0, false);


--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_erp_feedback_queue_id_seq', 1, false);


--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

-- ERP场景数据已删除，序列重置为 0（首次插入将从 1 开始）
SELECT pg_catalog.setval('public.sys_erp_scenario_id_seq', 0, false);


--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

-- ERP子接口数据已删除，序列重置为 0（首次插入将从 1 开始）
SELECT pg_catalog.setval('public.sys_erp_sub_interface_id_seq', 0, false);


--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sys_sync_history_id_seq', 1, false);


--
-- Name: voucher_match_result_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.voucher_match_result_id_seq', 1, false);


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
-- Name: integrity_check integrity_check_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.integrity_check
    ADD CONSTRAINT integrity_check_pkey PRIMARY KEY (id);


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
-- Name: sys_env_marker sys_env_marker_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_env_marker
    ADD CONSTRAINT sys_env_marker_pkey PRIMARY KEY (marker_key);


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
-- Name: sys_org sys_org_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_org
    ADD CONSTRAINT sys_org_pkey PRIMARY KEY (id);


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
-- Name: sys_sync_history sys_sync_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT sys_sync_history_pkey PRIMARY KEY (id);


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
-- Name: idx_abnormal_create_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_abnormal_create_time ON public.arc_abnormal_voucher USING btree (create_time);


--
-- Name: idx_abnormal_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_abnormal_status ON public.arc_abnormal_voucher USING btree (status);


--
-- Name: idx_acc_archive_batch_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_acc_archive_batch_id ON public.acc_archive USING btree (batch_id);


--
-- Name: idx_account_mapping_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_mapping_company ON public.cfg_account_role_mapping USING btree (company_id);


--
-- Name: idx_account_preset_kit; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_account_preset_kit ON public.cfg_account_role_preset USING btree (kit_id);


--
-- Name: idx_arc_file_content_fiscal_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arc_file_content_fiscal_year ON public.arc_file_content USING btree (fiscal_year);


--
-- Name: idx_arc_file_content_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_arc_file_content_status ON public.arc_file_content USING btree (pre_archive_status);


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
-- Name: idx_archive_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_code ON public.acc_archive USING btree (archive_code);


--
-- Name: idx_archive_destruction_hold; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_destruction_hold ON public.acc_archive USING btree (destruction_hold);


--
-- Name: idx_archive_fonds_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_fonds_year ON public.acc_archive USING btree (fonds_no, fiscal_year);


--
-- Name: idx_archive_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_status ON public.acc_archive USING btree (status);


--
-- Name: idx_archive_volume_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_archive_volume_id ON public.acc_archive USING btree (volume_id);


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
-- Name: idx_borrowing_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrowing_status ON public.biz_borrowing USING btree (status);


--
-- Name: idx_borrowing_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_borrowing_user ON public.biz_borrowing USING btree (user_id);


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
-- Name: idx_erp_config_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_config_enabled ON public.bas_erp_config USING btree (enabled);


--
-- Name: idx_erp_config_org; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_erp_config_org ON public.sys_erp_config USING btree (org_id);


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
-- Name: idx_file_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_id ON public.arc_file_metadata_index USING btree (file_id);


--
-- Name: idx_file_item_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_item_id ON public.arc_file_content USING btree (item_id);


--
-- Name: idx_file_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_file_type ON public.arc_file_content USING btree (file_type);


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
-- Name: idx_sys_org_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sys_org_parent ON public.sys_org USING btree (parent_id);


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
-- Name: uq_recon_record_key; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_recon_record_key ON public.arc_reconciliation_record USING btree (config_id, subject_code, recon_start_date, recon_end_date) WHERE ((config_id IS NOT NULL) AND (recon_start_date IS NOT NULL) AND (recon_end_date IS NOT NULL));


--
-- Name: ux_acc_archive_unique_biz_id_not_deleted; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_acc_archive_unique_biz_id_not_deleted ON public.acc_archive USING btree (unique_biz_id) WHERE (deleted = 0);


--
-- Name: arc_original_voucher trg_ov_row_version; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ov_row_version BEFORE UPDATE ON public.arc_original_voucher FOR EACH ROW EXECUTE FUNCTION public.update_row_version();


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
-- Name: archive_batch_item fk_batch_item_batch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archive_batch_item
    ADD CONSTRAINT fk_batch_item_batch FOREIGN KEY (batch_id) REFERENCES public.archive_batch(id) ON DELETE CASCADE;


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
-- Name: sys_user sys_user_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_org(id);


--
-- Name: ys_sales_out_detail ys_sales_out_detail_sales_out_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ys_sales_out_detail
    ADD CONSTRAINT ys_sales_out_detail_sales_out_id_fkey FOREIGN KEY (sales_out_id) REFERENCES public.ys_sales_out(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--
