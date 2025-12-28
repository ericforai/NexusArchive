--
-- PostgreSQL database dump
--

\restrict gcapTEDdkMeEFzomi28wRe6XN7WnD38C3KdSfCdvVvN0iLRje8jQmKoZ9cPZdpy

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: acc_archive; Type: TABLE; Schema: public; Owner: postgres
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
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    paper_ref_link character varying(128),
    destruction_hold boolean DEFAULT false,
    hold_reason character varying(255),
    summary character varying(2000)
);


ALTER TABLE public.acc_archive OWNER TO postgres;

--
-- Name: TABLE acc_archive; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.acc_archive IS '电子会计档案表';


--
-- Name: COLUMN acc_archive.title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.title IS '题名 (SM4加密存储，最大1000字符)';


--
-- Name: COLUMN acc_archive.org_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.org_name IS '立档单位名称 (最大500字符)';


--
-- Name: COLUMN acc_archive.creator; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.creator IS '责任者/制单人 (SM4加密存储，最大500字符)';


--
-- Name: COLUMN acc_archive.amount; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.amount IS '金额';


--
-- Name: COLUMN acc_archive.doc_date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.doc_date IS '业务日期';


--
-- Name: COLUMN acc_archive.unique_biz_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.unique_biz_id IS '唯一业务ID';


--
-- Name: COLUMN acc_archive.volume_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.volume_id IS '所属案卷ID';


--
-- Name: COLUMN acc_archive.paper_ref_link; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.paper_ref_link IS '纸质档案关联号 (物理存放位置)';


--
-- Name: COLUMN acc_archive.destruction_hold; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.destruction_hold IS '销毁留置 (冻结状态)';


--
-- Name: COLUMN acc_archive.hold_reason; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.hold_reason IS '留置/冻结原因 (如: 未结清债权)';


--
-- Name: COLUMN acc_archive.summary; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive.summary IS '摘要/说明 (SM4加密存储，最大2000字符)';


--
-- Name: acc_archive_attachment; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.acc_archive_attachment OWNER TO postgres;

--
-- Name: TABLE acc_archive_attachment; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.acc_archive_attachment IS '档案附件关联表';


--
-- Name: COLUMN acc_archive_attachment.archive_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_attachment.archive_id IS '档案ID (acc_archive.id)';


--
-- Name: COLUMN acc_archive_attachment.file_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_attachment.file_id IS '文件ID (arc_file_content.id)';


--
-- Name: COLUMN acc_archive_attachment.attachment_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_attachment.attachment_type IS '附件类型: invoice/contract/bank_slip/other';


--
-- Name: COLUMN acc_archive_attachment.relation_desc; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_attachment.relation_desc IS '关联描述';


--
-- Name: COLUMN acc_archive_attachment.created_by; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_attachment.created_by IS '创建人ID';


--
-- Name: COLUMN acc_archive_attachment.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_attachment.created_at IS '创建时间';


--
-- Name: acc_archive_relation; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.acc_archive_relation OWNER TO postgres;

--
-- Name: TABLE acc_archive_relation; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.acc_archive_relation IS '档案关联关系表';


--
-- Name: acc_archive_volume; Type: TABLE; Schema: public; Owner: postgres
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
    status character varying(20) DEFAULT 'draft'::character varying,
    reviewed_by character varying(32),
    reviewed_at timestamp without time zone,
    archived_at timestamp without time zone,
    custodian_dept character varying(32) DEFAULT 'ACCOUNTING'::character varying,
    validation_report_path character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


ALTER TABLE public.acc_archive_volume OWNER TO postgres;

--
-- Name: TABLE acc_archive_volume; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.acc_archive_volume IS '案卷表 (虚拟装订)';


--
-- Name: COLUMN acc_archive_volume.volume_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.volume_code IS '案卷号';


--
-- Name: COLUMN acc_archive_volume.title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.title IS '案卷标题 (格式: 责任者+年度+月度+业务子系统+业务单据名称)';


--
-- Name: COLUMN acc_archive_volume.fonds_no; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.fonds_no IS '全宗号';


--
-- Name: COLUMN acc_archive_volume.fiscal_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.fiscal_year IS '会计年度';


--
-- Name: COLUMN acc_archive_volume.fiscal_period; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.fiscal_period IS '会计期间 (YYYY-MM)';


--
-- Name: COLUMN acc_archive_volume.category_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.category_code IS '分类代号 (AC01=凭证, AC02=账簿, AC03=报告)';


--
-- Name: COLUMN acc_archive_volume.file_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.file_count IS '卷内文件数';


--
-- Name: COLUMN acc_archive_volume.retention_period; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.retention_period IS '保管期限';


--
-- Name: COLUMN acc_archive_volume.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.status IS '状态: draft, pending, archived';


--
-- Name: COLUMN acc_archive_volume.reviewed_by; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.reviewed_by IS '审核人ID';


--
-- Name: COLUMN acc_archive_volume.reviewed_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.reviewed_at IS '审核时间';


--
-- Name: COLUMN acc_archive_volume.archived_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.archived_at IS '归档时间';


--
-- Name: COLUMN acc_archive_volume.custodian_dept; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.custodian_dept IS '当前保管部门: ACCOUNTING(会计), ARCHIVES(档案)';


--
-- Name: COLUMN acc_archive_volume.validation_report_path; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.validation_report_path IS '四性检测报告路径';


--
-- Name: arc_abnormal_voucher; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.arc_abnormal_voucher OWNER TO postgres;

--
-- Name: TABLE arc_abnormal_voucher; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_abnormal_voucher IS '异常凭证数据池';


--
-- Name: COLUMN arc_abnormal_voucher.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.id IS '主键ID';


--
-- Name: COLUMN arc_abnormal_voucher.request_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.request_id IS '请求ID';


--
-- Name: COLUMN arc_abnormal_voucher.source_system; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.source_system IS '来源系统';


--
-- Name: COLUMN arc_abnormal_voucher.voucher_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.voucher_number IS '凭证号';


--
-- Name: COLUMN arc_abnormal_voucher.sip_data; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.sip_data IS '原始SIP数据(JSON)';


--
-- Name: COLUMN arc_abnormal_voucher.fail_reason; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.fail_reason IS '失败原因';


--
-- Name: COLUMN arc_abnormal_voucher.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.status IS '状态: PENDING/RETRYING/IGNORED/RESOLVED';


--
-- Name: COLUMN arc_abnormal_voucher.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.create_time IS '创建时间';


--
-- Name: COLUMN arc_abnormal_voucher.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_abnormal_voucher.update_time IS '更新时间';


--
-- Name: arc_archive_batch; Type: TABLE; Schema: public; Owner: postgres
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
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.arc_archive_batch OWNER TO postgres;

--
-- Name: TABLE arc_archive_batch; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_archive_batch IS '归档批次存证表 (哈希链核心)';


--
-- Name: COLUMN arc_archive_batch.chained_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_archive_batch.chained_hash IS '本批次防篡改挂接指纹';


--
-- Name: arc_archive_batch_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.arc_archive_batch_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.arc_archive_batch_id_seq OWNER TO postgres;

--
-- Name: arc_archive_batch_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.arc_archive_batch_id_seq OWNED BY public.arc_archive_batch.id;


--
-- Name: arc_convert_log; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.arc_convert_log OWNER TO postgres;

--
-- Name: TABLE arc_convert_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_convert_log IS '格式转换日志表';


--
-- Name: COLUMN arc_convert_log.source_format; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_convert_log.source_format IS '源格式 (PDF, JPG等)';


--
-- Name: COLUMN arc_convert_log.target_format; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_convert_log.target_format IS '目标格式 (OFD)';


--
-- Name: COLUMN arc_convert_log.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_convert_log.status IS '转换状态: SUCCESS, FAIL';


--
-- Name: COLUMN arc_convert_log.duration_ms; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_convert_log.duration_ms IS '转换耗时（毫秒）';


--
-- Name: arc_file_content; Type: TABLE; Schema: public; Owner: postgres
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
    sequence_in_batch integer
);


ALTER TABLE public.arc_file_content OWNER TO postgres;

--
-- Name: TABLE arc_file_content; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_file_content IS '电子文件存储记录表';


--
-- Name: COLUMN arc_file_content.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.id IS '文件ID';


--
-- Name: COLUMN arc_file_content.archival_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.archival_code IS '档号';


--
-- Name: COLUMN arc_file_content.file_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.file_name IS '文件名';


--
-- Name: COLUMN arc_file_content.file_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.file_type IS '文件类型 (PDF/OFD/XML)';


--
-- Name: COLUMN arc_file_content.file_size; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.file_size IS '文件大小(字节)';


--
-- Name: COLUMN arc_file_content.file_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.file_hash IS '文件哈希值';


--
-- Name: COLUMN arc_file_content.hash_algorithm; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.hash_algorithm IS '哈希算法 (SM3/SHA256)';


--
-- Name: COLUMN arc_file_content.storage_path; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.storage_path IS '存储路径';


--
-- Name: COLUMN arc_file_content.created_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.created_time IS '创建时间';


--
-- Name: COLUMN arc_file_content.item_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.item_id IS '关联单据ID';


--
-- Name: COLUMN arc_file_content.original_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.original_hash IS '原始哈希值 (接收时)';


--
-- Name: COLUMN arc_file_content.current_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.current_hash IS '当前哈希值 (巡检时)';


--
-- Name: COLUMN arc_file_content.timestamp_token; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.timestamp_token IS '时间戳Token';


--
-- Name: COLUMN arc_file_content.sign_value; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.sign_value IS '电子签名值';


--
-- Name: COLUMN arc_file_content.certificate; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.certificate IS '电子签章证书内容（Base64编码）';


--
-- Name: COLUMN arc_file_content.pre_archive_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.pre_archive_status IS '预归档状态: PENDING_CHECK/CHECK_FAILED/PENDING_METADATA/PENDING_ARCHIVE/ARCHIVED';


--
-- Name: COLUMN arc_file_content.fiscal_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.fiscal_year IS '会计年度';


--
-- Name: COLUMN arc_file_content.voucher_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.voucher_type IS '凭证类型';


--
-- Name: COLUMN arc_file_content.creator; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.creator IS '创建人';


--
-- Name: COLUMN arc_file_content.fonds_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.fonds_code IS '全宗号';


--
-- Name: COLUMN arc_file_content.source_system; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.source_system IS '来源系统';


--
-- Name: COLUMN arc_file_content.check_result; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.check_result IS '四性检测结果（JSON格式）';


--
-- Name: COLUMN arc_file_content.checked_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.checked_time IS '检测时间';


--
-- Name: COLUMN arc_file_content.archived_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.archived_time IS '归档时间';


--
-- Name: COLUMN arc_file_content.business_doc_no; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.business_doc_no IS '来源唯一标识（幂等性控制，如 YonSuite_xxx）';


--
-- Name: COLUMN arc_file_content.erp_voucher_no; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.erp_voucher_no IS 'ERP原始凭证号（用户可读，如 记-3）';


--
-- Name: COLUMN arc_file_content.source_data; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.source_data IS '原始业务数据(JSON)';


--
-- Name: COLUMN arc_file_content.batch_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_content.batch_id IS '关联的归档批次 ID';


--
-- Name: arc_file_metadata_index; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.arc_file_metadata_index OWNER TO postgres;

--
-- Name: TABLE arc_file_metadata_index; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_file_metadata_index IS '智能解析元数据索引表';


--
-- Name: COLUMN arc_file_metadata_index.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.id IS '索引ID';


--
-- Name: COLUMN arc_file_metadata_index.file_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.file_id IS '文件ID';


--
-- Name: COLUMN arc_file_metadata_index.invoice_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.invoice_code IS '发票代码';


--
-- Name: COLUMN arc_file_metadata_index.invoice_number; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.invoice_number IS '发票号码';


--
-- Name: COLUMN arc_file_metadata_index.total_amount; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.total_amount IS '价税合计';


--
-- Name: COLUMN arc_file_metadata_index.seller_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.seller_name IS '销售方名称';


--
-- Name: COLUMN arc_file_metadata_index.issue_date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.issue_date IS '开票日期';


--
-- Name: COLUMN arc_file_metadata_index.parsed_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.parsed_time IS '解析时间';


--
-- Name: COLUMN arc_file_metadata_index.parser_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_file_metadata_index.parser_type IS '解析器类型';


--
-- Name: arc_reconciliation_record; Type: TABLE; Schema: public; Owner: postgres
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
    source_system character varying(100)
);


ALTER TABLE public.arc_reconciliation_record OWNER TO postgres;

--
-- Name: TABLE arc_reconciliation_record; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_reconciliation_record IS '财务账、凭证与附件一致性核对记录表';


--
-- Name: COLUMN arc_reconciliation_record.erp_debit_total; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_reconciliation_record.erp_debit_total IS 'ERP侧借方合计';


--
-- Name: COLUMN arc_reconciliation_record.arc_debit_total; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_reconciliation_record.arc_debit_total IS '档案系统侧借方合计';


--
-- Name: COLUMN arc_reconciliation_record.recon_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_reconciliation_record.recon_status IS '核对状态: SUCCESS(通过), DISCREPANCY(有差异), ERROR(异常)';


--
-- Name: arc_signature_log; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.arc_signature_log OWNER TO postgres;

--
-- Name: TABLE arc_signature_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.arc_signature_log IS '签章日志表 - 记录电子签章/验签操作';


--
-- Name: COLUMN arc_signature_log.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.id IS '主键ID';


--
-- Name: COLUMN arc_signature_log.archive_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.archive_id IS '关联的档案ID';


--
-- Name: COLUMN arc_signature_log.file_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.file_id IS '关联的文件ID';


--
-- Name: COLUMN arc_signature_log.signer_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.signer_name IS '签章人姓名';


--
-- Name: COLUMN arc_signature_log.signer_cert_sn; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.signer_cert_sn IS '证书序列号';


--
-- Name: COLUMN arc_signature_log.signer_org; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.signer_org IS '签章单位';


--
-- Name: COLUMN arc_signature_log.sign_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.sign_time IS '签章时间';


--
-- Name: COLUMN arc_signature_log.sign_algorithm; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.sign_algorithm IS '签名算法(SM2/RSA)';


--
-- Name: COLUMN arc_signature_log.signature_value; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.signature_value IS '签名值(Base64)';


--
-- Name: COLUMN arc_signature_log.verify_result; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.verify_result IS '验证结果(VALID/INVALID/UNKNOWN)';


--
-- Name: COLUMN arc_signature_log.verify_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.verify_time IS '验证时间';


--
-- Name: COLUMN arc_signature_log.verify_message; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.verify_message IS '验证消息';


--
-- Name: COLUMN arc_signature_log.created_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.arc_signature_log.created_time IS '创建时间';


--
-- Name: audit_inspection_log; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.audit_inspection_log OWNER TO postgres;

--
-- Name: TABLE audit_inspection_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.audit_inspection_log IS '四性检测日志表';


--
-- Name: bas_erp_config; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.bas_erp_config OWNER TO postgres;

--
-- Name: TABLE bas_erp_config; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.bas_erp_config IS 'ERP 配置表';


--
-- Name: COLUMN bas_erp_config.adapter_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_erp_config.adapter_type IS '适配器类型: yonsuite=用友, kingdee=金蝶, generic=通用';


--
-- Name: COLUMN bas_erp_config.app_secret; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_erp_config.app_secret IS '应用密钥 (SM4 加密存储)';


--
-- Name: COLUMN bas_erp_config.extra_config; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_erp_config.extra_config IS '额外配置 (JSON格式，用于通用适配器字段映射等)';


--
-- Name: bas_fonds; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.bas_fonds OWNER TO postgres;

--
-- Name: TABLE bas_fonds; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.bas_fonds IS '全宗基础信息表';


--
-- Name: COLUMN bas_fonds.org_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_fonds.org_id IS '关联组织ID（公司级）';


--
-- Name: bas_location; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.bas_location OWNER TO postgres;

--
-- Name: TABLE bas_location; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.bas_location IS '库房位置表';


--
-- Name: COLUMN bas_location.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.id IS '主键ID';


--
-- Name: COLUMN bas_location.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.name IS '位置名称';


--
-- Name: COLUMN bas_location.code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.code IS '位置编码';


--
-- Name: COLUMN bas_location.type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.type IS '类型: WAREHOUSE, AREA, SHELF, BOX';


--
-- Name: COLUMN bas_location.parent_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.parent_id IS '父级ID';


--
-- Name: COLUMN bas_location.path; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.path IS '完整路径';


--
-- Name: COLUMN bas_location.capacity; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.capacity IS '容量';


--
-- Name: COLUMN bas_location.used_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.used_count IS '已用数量';


--
-- Name: COLUMN bas_location.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.status IS '状态: NORMAL, FULL, MAINTENANCE';


--
-- Name: COLUMN bas_location.rfid_tag; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.rfid_tag IS 'RFID标签号';


--
-- Name: COLUMN bas_location.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.created_at IS '创建时间';


--
-- Name: COLUMN bas_location.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.updated_at IS '更新时间';


--
-- Name: COLUMN bas_location.deleted; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_location.deleted IS '逻辑删除标识';


--
-- Name: biz_archive_approval; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.biz_archive_approval OWNER TO postgres;

--
-- Name: COLUMN biz_archive_approval.org_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_archive_approval.org_name IS '立档单位';


--
-- Name: biz_borrowing; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.biz_borrowing OWNER TO postgres;

--
-- Name: TABLE biz_borrowing; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.biz_borrowing IS '借阅申请表';


--
-- Name: COLUMN biz_borrowing.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.id IS '主键ID';


--
-- Name: COLUMN biz_borrowing.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.user_id IS '申请人ID';


--
-- Name: COLUMN biz_borrowing.user_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.user_name IS '申请人姓名';


--
-- Name: COLUMN biz_borrowing.archive_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.archive_id IS '借阅档案ID';


--
-- Name: COLUMN biz_borrowing.archive_title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.archive_title IS '档案题名';


--
-- Name: COLUMN biz_borrowing.reason; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.reason IS '借阅原因';


--
-- Name: COLUMN biz_borrowing.borrow_date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.borrow_date IS '借阅日期';


--
-- Name: COLUMN biz_borrowing.expected_return_date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.expected_return_date IS '预计归还日期';


--
-- Name: COLUMN biz_borrowing.actual_return_date; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.actual_return_date IS '实际归还日期';


--
-- Name: COLUMN biz_borrowing.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.status IS '状态: PENDING, APPROVED, REJECTED, RETURNED, CANCELLED';


--
-- Name: COLUMN biz_borrowing.approval_comment; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.approval_comment IS '审批意见';


--
-- Name: COLUMN biz_borrowing.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.created_at IS '创建时间';


--
-- Name: COLUMN biz_borrowing.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.updated_at IS '更新时间';


--
-- Name: COLUMN biz_borrowing.deleted; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_borrowing.deleted IS '逻辑删除标识';


--
-- Name: biz_destruction; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.biz_destruction OWNER TO postgres;

--
-- Name: TABLE biz_destruction; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.biz_destruction IS '销毁申请表';


--
-- Name: COLUMN biz_destruction.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.id IS '主键ID';


--
-- Name: COLUMN biz_destruction.applicant_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.applicant_id IS '申请人ID';


--
-- Name: COLUMN biz_destruction.applicant_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.applicant_name IS '申请人姓名';


--
-- Name: COLUMN biz_destruction.reason; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.reason IS '销毁原因';


--
-- Name: COLUMN biz_destruction.archive_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.archive_count IS '待销毁档案数量';


--
-- Name: COLUMN biz_destruction.archive_ids; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.archive_ids IS '待销毁档案ID列表(JSON)';


--
-- Name: COLUMN biz_destruction.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.status IS '状态: PENDING, APPROVED, REJECTED, EXECUTED';


--
-- Name: COLUMN biz_destruction.approver_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.approver_id IS '审批人ID';


--
-- Name: COLUMN biz_destruction.approver_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.approver_name IS '审批人姓名';


--
-- Name: COLUMN biz_destruction.approval_comment; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.approval_comment IS '审批意见';


--
-- Name: COLUMN biz_destruction.approval_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.approval_time IS '审批时间';


--
-- Name: COLUMN biz_destruction.execution_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.execution_time IS '执行时间';


--
-- Name: COLUMN biz_destruction.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.created_at IS '创建时间';


--
-- Name: COLUMN biz_destruction.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.updated_at IS '更新时间';


--
-- Name: COLUMN biz_destruction.deleted; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.biz_destruction.deleted IS '逻辑删除标识';


--
-- Name: biz_open_appraisal; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.biz_open_appraisal OWNER TO postgres;

--
-- Name: TABLE biz_open_appraisal; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.biz_open_appraisal IS '开放鉴定表';


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: sys_archival_code_sequence; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_archival_code_sequence (
    fonds_code character varying(50) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    category_code character varying(10) NOT NULL,
    current_val integer DEFAULT 0,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_archival_code_sequence OWNER TO postgres;

--
-- Name: TABLE sys_archival_code_sequence; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_archival_code_sequence IS '档号生成计数器';


--
-- Name: sys_audit_log; Type: TABLE; Schema: public; Owner: postgres
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
    ip_address character varying(50) NOT NULL,
    mac_address character varying(64) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    object_digest character varying(128),
    user_agent character varying(500),
    prev_log_hash character varying(128),
    log_hash character varying(128),
    device_fingerprint character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_audit_log OWNER TO postgres;

--
-- Name: TABLE sys_audit_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_audit_log IS '安全审计日志表';


--
-- Name: COLUMN sys_audit_log.action; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.action IS '操作类型: CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD';


--
-- Name: COLUMN sys_audit_log.data_before; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.data_before IS '操作前数据快照';


--
-- Name: COLUMN sys_audit_log.data_after; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.data_after IS '操作后数据快照';


--
-- Name: COLUMN sys_audit_log.ip_address; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.ip_address IS '客户端IP地址';


--
-- Name: COLUMN sys_audit_log.mac_address; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.mac_address IS 'MAC地址';


--
-- Name: COLUMN sys_audit_log.object_digest; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.object_digest IS '被操作对象的哈希值';


--
-- Name: COLUMN sys_audit_log.prev_log_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.prev_log_hash IS '前一条日志的SM3哈希值';


--
-- Name: COLUMN sys_audit_log.log_hash; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.log_hash IS '当前日志的SM3哈希值';


--
-- Name: COLUMN sys_audit_log.device_fingerprint; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_audit_log.device_fingerprint IS '客户端设备指纹';


--
-- Name: sys_erp_config; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_erp_config OWNER TO postgres;

--
-- Name: TABLE sys_erp_config; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_erp_config IS 'ERP对接配置表';


--
-- Name: COLUMN sys_erp_config.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_config.name IS '配置名称';


--
-- Name: COLUMN sys_erp_config.erp_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_config.erp_type IS 'ERP类型: YONSUITE/KINGDEE/GENERIC';


--
-- Name: COLUMN sys_erp_config.config_json; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_config.config_json IS '配置参数JSON';


--
-- Name: COLUMN sys_erp_config.is_active; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_config.is_active IS '是否启用';


--
-- Name: COLUMN sys_erp_config.org_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_config.org_id IS '关联组织ID';


--
-- Name: sys_erp_config_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_erp_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sys_erp_config_id_seq OWNER TO postgres;

--
-- Name: sys_erp_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_erp_config_id_seq OWNED BY public.sys_erp_config.id;


--
-- Name: sys_erp_feedback_queue; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_erp_feedback_queue OWNER TO postgres;

--
-- Name: TABLE sys_erp_feedback_queue; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_erp_feedback_queue IS 'ERP 回写失败重试队列 - 存证溯源';


--
-- Name: COLUMN sys_erp_feedback_queue.voucher_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.voucher_id IS 'ERP 凭证/单据 ID';


--
-- Name: COLUMN sys_erp_feedback_queue.archival_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.archival_code IS '生成的档号';


--
-- Name: COLUMN sys_erp_feedback_queue.erp_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.erp_type IS 'ERP 类型 (YONSUITE, KINGDEE 等)';


--
-- Name: COLUMN sys_erp_feedback_queue.retry_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.retry_count IS '已重试次数';


--
-- Name: COLUMN sys_erp_feedback_queue.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.status IS '状态: PENDING-待重试, RETRYING-重试中, SUCCESS-成功, FAILED-放弃';


--
-- Name: COLUMN sys_erp_feedback_queue.next_retry_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_feedback_queue.next_retry_time IS '下次重试时间 (指数退避算法)';


--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_erp_feedback_queue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sys_erp_feedback_queue_id_seq OWNER TO postgres;

--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_erp_feedback_queue_id_seq OWNED BY public.sys_erp_feedback_queue.id;


--
-- Name: sys_erp_scenario; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_erp_scenario OWNER TO postgres;

--
-- Name: TABLE sys_erp_scenario; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_erp_scenario IS 'ERP业务场景配置表 (Layer 2)';


--
-- Name: COLUMN sys_erp_scenario.scenario_key; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_scenario.scenario_key IS '场景唯一标识 (如 VOUCHER_SYNC)';


--
-- Name: COLUMN sys_erp_scenario.sync_strategy; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_scenario.sync_strategy IS '同步策略: MANUAL=手动, CRON=定时, REALTIME=实时';


--
-- Name: COLUMN sys_erp_scenario.params_json; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_scenario.params_json IS 'JSON格式的场景参数配置';


--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_erp_scenario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sys_erp_scenario_id_seq OWNER TO postgres;

--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_erp_scenario_id_seq OWNED BY public.sys_erp_scenario.id;


--
-- Name: sys_erp_sub_interface; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_erp_sub_interface OWNER TO postgres;

--
-- Name: TABLE sys_erp_sub_interface; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_erp_sub_interface IS '场景子接口配置表';


--
-- Name: COLUMN sys_erp_sub_interface.interface_key; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_sub_interface.interface_key IS '接口标识如LIST_QUERY/DETAIL_QUERY';


--
-- Name: COLUMN sys_erp_sub_interface.config_json; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_erp_sub_interface.config_json IS 'JSON格式的接口配置参数';


--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_erp_sub_interface_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sys_erp_sub_interface_id_seq OWNER TO postgres;

--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_erp_sub_interface_id_seq OWNED BY public.sys_erp_sub_interface.id;


--
-- Name: sys_ingest_request_status; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_ingest_request_status OWNER TO postgres;

--
-- Name: TABLE sys_ingest_request_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_ingest_request_status IS 'SIP接收请求状态追踪表';


--
-- Name: COLUMN sys_ingest_request_status.request_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_ingest_request_status.request_id IS '请求ID';


--
-- Name: COLUMN sys_ingest_request_status.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_ingest_request_status.status IS '状态: RECEIVED, CHECKING, CHECK_PASSED, PROCESSING, COMPLETED, FAILED';


--
-- Name: COLUMN sys_ingest_request_status.message; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_ingest_request_status.message IS '详细消息或错误信息';


--
-- Name: COLUMN sys_ingest_request_status.created_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_ingest_request_status.created_time IS '创建时间';


--
-- Name: COLUMN sys_ingest_request_status.updated_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_ingest_request_status.updated_time IS '更新时间';


--
-- Name: sys_org; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_org OWNER TO postgres;

--
-- Name: sys_permission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_permission (
    id character varying(64) NOT NULL,
    perm_key character varying(100) NOT NULL,
    label character varying(100) NOT NULL,
    group_name character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_permission OWNER TO postgres;

--
-- Name: sys_role; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_role OWNER TO postgres;

--
-- Name: sys_setting; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_setting OWNER TO postgres;

--
-- Name: COLUMN sys_setting.category; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_setting.category IS '配置分组/类别';


--
-- Name: COLUMN sys_setting.created_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_setting.created_at IS '创建时间';


--
-- Name: COLUMN sys_setting.deleted; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_setting.deleted IS '逻辑删除标记: 0=正常, 1=已删除';


--
-- Name: sys_sync_history; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_sync_history OWNER TO postgres;

--
-- Name: TABLE sys_sync_history; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_sync_history IS '同步历史记录表';


--
-- Name: COLUMN sys_sync_history.scenario_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_sync_history.scenario_id IS '关联的场景ID';


--
-- Name: COLUMN sys_sync_history.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_sync_history.status IS '同步状态: RUNNING/SUCCESS/FAIL';


--
-- Name: COLUMN sys_sync_history.sync_params; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_sync_history.sync_params IS 'JSON格式的同步参数';


--
-- Name: COLUMN sys_sync_history.operator_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_sync_history.operator_id IS '操作人ID';


--
-- Name: COLUMN sys_sync_history.client_ip; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_sync_history.client_ip IS '操作客户端IP';


--
-- Name: COLUMN sys_sync_history.four_nature_summary; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_sync_history.four_nature_summary IS 'JSON格式的四性检测统计摘要(真实性、完整性、可用性、安全性通过率)';


--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_sync_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sys_sync_history_id_seq OWNER TO postgres;

--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_sync_history_id_seq OWNED BY public.sys_sync_history.id;


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.sys_user OWNER TO postgres;

--
-- Name: sys_user_role; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_user_role (
    user_id character varying(64) NOT NULL,
    role_id character varying(64) NOT NULL
);


ALTER TABLE public.sys_user_role OWNER TO postgres;

--
-- Name: ys_sales_out; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.ys_sales_out OWNER TO postgres;

--
-- Name: TABLE ys_sales_out; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ys_sales_out IS 'YonSuite销售出库单同步数据';


--
-- Name: ys_sales_out_detail; Type: TABLE; Schema: public; Owner: postgres
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


ALTER TABLE public.ys_sales_out_detail OWNER TO postgres;

--
-- Name: TABLE ys_sales_out_detail; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ys_sales_out_detail IS 'YonSuite销售出库单明细同步数据';


--
-- Name: ys_sales_out_detail_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ys_sales_out_detail_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ys_sales_out_detail_id_seq OWNER TO postgres;

--
-- Name: ys_sales_out_detail_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ys_sales_out_detail_id_seq OWNED BY public.ys_sales_out_detail.id;


--
-- Name: ys_sales_out_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ys_sales_out_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ys_sales_out_id_seq OWNER TO postgres;

--
-- Name: ys_sales_out_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ys_sales_out_id_seq OWNED BY public.ys_sales_out.id;


--
-- Name: arc_archive_batch id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_archive_batch ALTER COLUMN id SET DEFAULT nextval('public.arc_archive_batch_id_seq'::regclass);


--
-- Name: sys_erp_config id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_config ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_config_id_seq'::regclass);


--
-- Name: sys_erp_feedback_queue id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_feedback_queue ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_feedback_queue_id_seq'::regclass);


--
-- Name: sys_erp_scenario id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_scenario ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_scenario_id_seq'::regclass);


--
-- Name: sys_erp_sub_interface id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_sub_interface ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_sub_interface_id_seq'::regclass);


--
-- Name: sys_sync_history id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_sync_history ALTER COLUMN id SET DEFAULT nextval('public.sys_sync_history_id_seq'::regclass);


--
-- Name: ys_sales_out id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out ALTER COLUMN id SET DEFAULT nextval('public.ys_sales_out_id_seq'::regclass);


--
-- Name: ys_sales_out_detail id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out_detail ALTER COLUMN id SET DEFAULT nextval('public.ys_sales_out_detail_id_seq'::regclass);


--
-- Data for Name: acc_archive; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, amount, doc_date, unique_biz_id, standard_metadata, custom_metadata, security_level, location, department_id, created_by, fixity_value, fixity_algo, volume_id, created_at, updated_at, deleted, paper_ref_link, destruction_hold, hold_reason, summary) FROM stdin;
seed-contract-001	DEMO	CON-2023-098	AC04	年度技术服务协议	2023	01	30Y	演示公司	系统	archived	150000.00	2023-01-15	CON-2023-098	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-contract-002	DEMO	C-202511-002	AC04	服务器采购合同	2025	11	30Y	演示公司	系统	archived	450000.00	2025-11-15	C-202511-002	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-invoice-001	DEMO	INV-202311-089	AC01	阿里云计算服务费发票	2023	11	30Y	演示公司	系统	archived	12800.00	2023-11-02	INV-202311-089	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-invoice-002	DEMO	INV-202311-092	AC01	服务器采购发票	2023	11	30Y	演示公司	系统	archived	45200.00	2023-11-03	INV-202311-092	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-voucher-001	DEMO	JZ-202311-0052	AC01	11月技术部费用报销	2023	11	30Y	演示公司	系统	archived	58000.00	2023-11-05	JZ-202311-0052	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-voucher-002	DEMO	V-202511-TEST	AC01	报销差旅费	2025	11	30Y	演示公司	张三	archived	5280.00	2025-11-07	V-202511-TEST	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-receipt-001	DEMO	B-20231105-003	AC04	招商银行付款回单	2023	11	30Y	演示公司	系统	archived	58000.00	2023-11-05	B-20231105-003	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-report-001	DEMO	REP-2023-11	AC03	11月科目余额表	2023	11	30Y	演示公司	系统	archived	\N	2023-11-30	REP-2023-11	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	0	\N	f	\N	\N
seed-book-001	COMP001	ARC-BOOK-2024-GL	AC02	2024年总账	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "GENERAL_LEDGER", "pageCount": 100}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-book-002	COMP001	ARC-BOOK-2024-CASH	AC02	2024年现金日记账	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "CASH_JOURNAL", "pageCount": 50}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-book-003	COMP001	ARC-BOOK-2024-BANK	AC02	2024年银行存款日记账	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "BANK_JOURNAL", "pageCount": 50}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-book-004	COMP001	ARC-BOOK-2024-FIXED	AC02	2024年固定资产卡片	2024	\N	30Y	总公司	\N	archived	\N	\N	\N	\N	{"bookType": "FIXED_ASSETS_CARD", "pageCount": 20}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-c03-001	COMP001	ARC-REP-2024-M01	AC03	2024年1月财务月报	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"period": "2024-01", "reportType": "MONTHLY"}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-c03-002	COMP001	ARC-REP-2024-Q1	AC03	2024年第一季度财务报表	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"period": "2024-Q1", "reportType": "QUARTERLY"}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-c03-003	COMP001	ARC-REP-2023-ANN	AC03	2023年度财务决算报告	2023	\N	PERMANENT	总公司	\N	archived	\N	\N	\N	\N	{"period": "2023", "reportType": "ANNUAL"}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-c04-001	COMP001	ARC-OTH-2024-BK-01	AC04	2024年1月银行对账单	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"otherType": "BANK_STATEMENT"}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-c04-002	COMP001	ARC-OTH-2024-TAX-01	AC04	2024年1月增值税纳税申报表	2024	\N	10Y	总公司	\N	archived	\N	\N	\N	\N	{"otherType": "TAX_RETURN"}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
seed-c04-003	COMP001	ARC-OTH-2024-HO-01	AC04	2024年度会计档案移交清册	2024	\N	30Y	档案室	\N	archived	\N	\N	\N	\N	{"otherType": "HANDOVER_REGISTER"}	internal	\N	\N	system	\N	\N	\N	2025-12-19 09:08:48.602238	2025-12-19 09:08:48.602238	0	\N	f	\N	\N
\.


--
-- Data for Name: acc_archive_attachment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_archive_attachment (id, archive_id, file_id, attachment_type, relation_desc, created_by, created_at) FROM stdin;
\.


--
-- Data for Name: acc_archive_relation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) FROM stdin;
seed-rel-001	seed-contract-001	seed-voucher-001	BASIS	合同依据	system	2025-12-19 09:08:48.434319	0
seed-rel-002	seed-invoice-001	seed-voucher-001	ORIGINAL_VOUCHER	原始凭证	system	2025-12-19 09:08:48.434319	0
seed-rel-003	seed-invoice-002	seed-voucher-001	ORIGINAL_VOUCHER	原始凭证	system	2025-12-19 09:08:48.434319	0
seed-rel-004	seed-voucher-001	seed-receipt-001	CASH_FLOW	资金流	system	2025-12-19 09:08:48.434319	0
seed-rel-005	seed-voucher-001	seed-report-001	ARCHIVE	归档	system	2025-12-19 09:08:48.434319	0
\.


--
-- Data for Name: acc_archive_volume; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_archive_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, reviewed_by, reviewed_at, archived_at, custodian_dept, validation_report_path, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: arc_abnormal_voucher; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_abnormal_voucher (id, request_id, source_system, voucher_number, sip_data, fail_reason, status, create_time, update_time) FROM stdin;
\.


--
-- Data for Name: arc_archive_batch; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_archive_batch (id, batch_no, prev_batch_hash, current_batch_hash, chained_hash, hash_algo, item_count, operator_id, created_time) FROM stdin;
\.


--
-- Data for Name: arc_convert_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_convert_log (id, archive_id, source_format, target_format, source_path, target_path, status, error_message, file_size_bytes, convert_duration_ms, created_time, source_size, target_size, duration_ms, convert_time) FROM stdin;
\.


--
-- Data for Name: arc_file_content; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no, erp_voucher_no, source_data, batch_id, sequence_in_batch) FROM stdin;
\.


--
-- Data for Name: arc_file_metadata_index; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_file_metadata_index (id, file_id, invoice_code, invoice_number, total_amount, seller_name, issue_date, parsed_time, parser_type) FROM stdin;
\.


--
-- Data for Name: arc_reconciliation_record; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_reconciliation_record (id, fonds_code, fiscal_year, fiscal_period, subject_code, subject_name, erp_debit_total, erp_credit_total, erp_voucher_count, arc_debit_total, arc_credit_total, arc_voucher_count, attachment_count, attachment_missing_count, recon_status, recon_message, recon_time, operator_id, snapshot_data, source_system) FROM stdin;
\.


--
-- Data for Name: arc_signature_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_signature_log (id, archive_id, file_id, signer_name, signer_cert_sn, signer_org, sign_time, sign_algorithm, signature_value, verify_result, verify_time, verify_message, created_time) FROM stdin;
\.


--
-- Data for Name: audit_inspection_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_inspection_log (id, archive_id, inspection_stage, inspection_time, inspector_id, is_authentic, is_complete, is_available, is_secure, hash_snapshot, integrity_check, authenticity_check, availability_check, security_check, check_result, detail_report, created_at, report_file_path, report_file_hash, is_compliant, compliance_violations, compliance_warnings) FROM stdin;
\.


--
-- Data for Name: bas_erp_config; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bas_erp_config (id, name, adapter_type, base_url, app_key, app_secret, tenant_id, accbook_code, extra_config, enabled, created_by, created_time, last_modified_time) FROM stdin;
\.


--
-- Data for Name: bas_fonds; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time, org_id) FROM stdin;
demo-fonds-001	DEMO	演示全宗	演示公司	系统初始演示数据	system	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
\.


--
-- Data for Name: bas_location; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: biz_archive_approval; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, approver_id, approver_name, status, approval_comment, approval_time, created_at, updated_at, deleted, org_name) FROM stdin;
\.


--
-- Data for Name: biz_borrowing; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.biz_borrowing (id, user_id, user_name, archive_id, archive_title, reason, borrow_date, expected_return_date, actual_return_date, status, approval_comment, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: biz_destruction; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.biz_destruction (id, applicant_id, applicant_name, reason, archive_count, archive_ids, status, approver_id, approver_name, approval_comment, approval_time, execution_time, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: biz_open_appraisal; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.biz_open_appraisal (id, archive_id, archive_code, archive_title, retention_period, current_security_level, appraiser_id, appraiser_name, appraisal_date, appraisal_result, open_level, reason, status, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init base schema	SQL	V1__init_base_schema.sql	434465831	postgres	2025-12-19 09:08:47.575492	70	t
2	2.0.0	init auth	SQL	V2.0.0__init_auth.sql	-52622364	postgres	2025-12-19 09:08:47.710413	36	t
3	3	smart parser tables	SQL	V3__smart_parser_tables.sql	-1210006438	postgres	2025-12-19 09:08:47.762371	33	t
4	4	fix archive and audit columns	SQL	V4__fix_archive_and_audit_columns.sql	1269090204	postgres	2025-12-19 09:08:47.805044	8	t
5	5	ingest request status	SQL	V5__ingest_request_status.sql	-1904538745	postgres	2025-12-19 09:08:47.821584	16	t
6	6	add business modules	SQL	V6__add_business_modules.sql	-1930095926	postgres	2025-12-19 09:08:47.848696	39	t
7	7	add archive approval	SQL	V7__add_archive_approval.sql	1997964789	postgres	2025-12-19 09:08:47.894558	15	t
8	8	add open appraisal	SQL	V8__add_open_appraisal.sql	1916481524	postgres	2025-12-19 09:08:47.915558	15	t
9	9	ensure metadata tables	SQL	V9__ensure_metadata_tables.sql	1360829760	postgres	2025-12-19 09:08:47.938595	3	t
10	10	compliance schema update	SQL	V10__compliance_schema_update.sql	1317807689	postgres	2025-12-19 09:08:47.948278	42	t
11	11	add missing archive columns	SQL	V11__add_missing_archive_columns.sql	-2012880184	postgres	2025-12-19 09:08:47.998375	18	t
12	12	add missing timestamps	SQL	V12__add_missing_timestamps.sql	-119234559	postgres	2025-12-19 09:08:48.02355	31	t
13	15	add convert log table	SQL	V15__add_convert_log_table.sql	1791460354	postgres	2025-12-19 09:08:48.061396	26	t
14	16	add erp config table	SQL	V16__add_erp_config_table.sql	904579297	postgres	2025-12-19 09:08:48.098833	14	t
15	20	compliance enhancement	SQL	V20__compliance_enhancement.sql	-266136302	postgres	2025-12-19 09:08:48.122415	12	t
16	21	add compliance fields	SQL	V21__add_compliance_fields.sql	758204874	postgres	2025-12-19 09:08:48.145011	11	t
17	22	add admin user	SQL	V22__add_admin_user.sql	659711366	postgres	2025-12-19 09:08:48.162071	4	t
18	23	add signature log	SQL	V23__add_signature_log.sql	1720095927	postgres	2025-12-19 09:08:48.173876	15	t
19	24	enhance audit log	SQL	V24__enhance_audit_log.sql	822112610	postgres	2025-12-19 09:08:48.194149	9	t
20	25	add archive summary	SQL	V25__add_archive_summary.sql	-511296424	postgres	2025-12-19 09:08:48.209192	3	t
21	26	ofd convert log	SQL	V26__ofd_convert_log.sql	1575575351	postgres	2025-12-19 09:08:48.21852	27	t
22	27	erp config	SQL	V27__erp_config.sql	1010520792	postgres	2025-12-19 09:08:48.28474	14	t
23	28	add certificate to arc file content	SQL	V28__add_certificate_to_arc_file_content.sql	806664392	postgres	2025-12-19 09:08:48.305472	2	t
24	29	add pre archive status	SQL	V29__add_pre_archive_status.sql	-1890227063	postgres	2025-12-19 09:08:48.312286	6	t
25	30	increase column length for archive submit	SQL	V30__increase_column_length_for_archive_submit.sql	-687052612	postgres	2025-12-19 09:08:48.322793	16	t
26	31	add org name to approval	SQL	V31__add_org_name_to_approval.sql	1237029946	postgres	2025-12-19 09:08:48.343933	1	t
27	32	add business doc no to arc file content	SQL	V32__add_business_doc_no_to_arc_file_content.sql	-1520271041	postgres	2025-12-19 09:08:48.350011	2	t
28	33	create abnormal voucher table	SQL	V33__create_abnormal_voucher_table.sql	-1551042214	postgres	2025-12-19 09:08:48.359062	22	t
29	34	increase archive column lengths for sm4	SQL	V34__increase_archive_column_lengths_for_sm4.sql	-708532401	postgres	2025-12-19 09:08:48.392562	14	t
30	35	add yonsuite salesout tables	SQL	V35__add_yonsuite_salesout_tables.sql	-614900511	postgres	2025-12-19 09:08:48.410252	16	t
31	36	insert seed data	SQL	V36__insert_seed_data.sql	-97412025	postgres	2025-12-19 09:08:48.430028	12	t
32	37	add erp voucher no	SQL	V37__add_erp_voucher_no.sql	-1010970794	postgres	2025-12-19 09:08:48.448965	7	t
33	38	add permission table	SQL	V38__add_permission_table.sql	1161423952	postgres	2025-12-19 09:08:48.461723	9	t
34	39	add signature columns	SQL	V39__add_signature_columns.sql	620064816	postgres	2025-12-19 09:08:48.478284	6	t
35	40	add missing entity columns	SQL	V40__add_missing_entity_columns.sql	2023024372	postgres	2025-12-19 09:08:48.48916	23	t
36	41	fix schema validation	SQL	V41__fix_schema_validation.sql	-1839887393	postgres	2025-12-19 09:08:48.517429	9	t
37	42	increase archive column lengths	SQL	V42__increase_archive_column_lengths.sql	1046984356	postgres	2025-12-19 09:08:48.531685	4	t
38	43	create erp scenario table	SQL	V43__create_erp_scenario_table.sql	1679219628	postgres	2025-12-19 09:08:48.540159	7	t
39	44	seed erp config	SQL	V44__seed_erp_config.sql	-1958718828	postgres	2025-12-19 09:08:48.550517	1	t
40	45	add weaver config	SQL	V45__add_weaver_config.sql	1716014399	postgres	2025-12-19 09:08:48.554466	0	t
41	46	add weaver e10 config	SQL	V46__add_weaver_e10_config.sql	782535747	postgres	2025-12-19 09:08:48.557853	1	t
42	47	update weaver e10 credentials	SQL	V47__update_weaver_e10_credentials.sql	-702954979	postgres	2025-12-19 09:08:48.561332	1	t
43	48	update weaver e10 host	SQL	V48__update_weaver_e10_host.sql	1914533299	postgres	2025-12-19 09:08:48.566356	1	t
44	49	add unique biz id unique index	SQL	V49__add_unique_biz_id_unique_index.sql	-317899089	postgres	2025-12-19 09:08:48.569382	2	t
45	50	add source data column	SQL	V50__add_source_data_column.sql	1531248083	postgres	2025-12-19 09:08:48.57421	1	t
46	51	archive attachment link	SQL	V51__archive_attachment_link.sql	1422255506	postgres	2025-12-19 09:08:48.577411	15	t
47	52	seed dynamic book types	SQL	V52__seed_dynamic_book_types.sql	714062901	postgres	2025-12-19 09:08:48.598697	6	t
48	53	update yonsuite config add scenario	SQL	V53__update_yonsuite_config_add_scenario.sql	1947224969	postgres	2025-12-19 09:08:48.614255	3	t
49	54	seed boran group org	SQL	V54__seed_boran_group_org.sql	1843779492	postgres	2025-12-19 09:08:48.622126	93	t
50	55	add org id to fonds and erp	SQL	V55__add_org_id_to_fonds_and_erp.sql	-1306393729	postgres	2025-12-19 09:08:48.722888	6	t
51	56	fix payment sync config and scenario	SQL	V56__fix_payment_sync_config_and_scenario.sql	-526925874	postgres	2025-12-19 09:08:48.736179	3	t
52	57	fix yonsuite accbook code	SQL	V57__fix_yonsuite_accbook_code.sql	-1313342885	postgres	2025-12-19 09:08:48.746319	3	t
53	58	integration center enhancement	SQL	V58__integration_center_enhancement.sql	-485470313	postgres	2025-12-19 09:08:48.757229	204	t
54	59	integration audit enhancement	SQL	V59__integration_audit_enhancement.sql	1731253970	postgres	2025-12-19 09:08:48.977548	10	t
55	60	integration templates and sub interfaces	SQL	V60__integration_templates_and_sub_interfaces.sql	138085633	postgres	2025-12-19 09:08:48.999168	9	t
56	61	sync history compliance enhancement	SQL	V61__sync_history_compliance_enhancement.sql	1934068662	postgres	2025-12-19 09:08:49.020074	2	t
57	62	reconciliation engine schema	SQL	V62__reconciliation_engine_schema.sql	-1319349764	postgres	2025-12-19 09:08:49.027814	6	t
58	63	enhanced security hash chain	SQL	V63__enhanced_security_hash_chain.sql	-1547672070	postgres	2025-12-19 09:08:49.038936	17	t
59	64	erp feedback queue	SQL	V64__erp_feedback_queue.sql	-57878814	postgres	2025-12-19 09:08:49.062709	9	t
60	65	fix foreign keys and schema	SQL	V65__fix_foreign_keys_and_schema.sql	-1857337971	postgres	2025-12-19 09:08:49.075558	13	t
61	66	fix erp template active status	SQL	V66__fix_erp_template_active_status.sql	-897713051	postgres	2025-12-19 09:08:49.093726	1	t
\.


--
-- Data for Name: sys_archival_code_sequence; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_archival_code_sequence (fonds_code, fiscal_year, category_code, current_val, updated_time) FROM stdin;
\.


--
-- Data for Name: sys_audit_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, ip_address, mac_address, object_digest, user_agent, prev_log_hash, log_hash, device_fingerprint, created_at) FROM stdin;
71c5efe1f6174fa184aa55ee97fa13c3	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	\N	\N	192.168.65.1	UNKNOWN	\N	curl/8.7.1	\N	8b5018e28f25b8340d0e16b288b57cb5a747ab8fe7e0db8ec1763308e445b1ae	curl/8.7.1|||	2025-12-19 09:09:09.096647
88001df6cbb04c52bc96a53bdbab3df8	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJ1c2VyX2FkbWluXzAwMSIsImlhdCI6MTc2NjEzNTk2MiwiZXhwIjoxNzY2MTM5NTYyfQ.LClwLPqCDFOYkFB5EVkUTFMHAw5fZTVjBioldaTfaBS88vGw6wLaZLRM1oK1WvKkSe7rHGKa1FxDTS-IXAwprnWcH9sYhJttq5aLMZYZfyIZ80N9Izj7o8HPQJrJfDir3YkFmqdvfBogLKnugKy07dsk4lEv7nSLPfTzIdfeVCdahFvIZIDZn4ODCtcwJG3F5WjpBY2ENsOLAOsQDSLV5xM2k85Y8NIygD7xo6VaTvRzq1fMxT_qt7M88aF-tGslPCQeZHqSp0nWaGXbj4eMW1Z82RhIsYH9eEbqsy3rD35ujs8voslo7_YOS-L4xwhUVeBqHyH-QM0fTSRAd8l6fg","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":"ORG_BR_GROUP_FIN","status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1766135962520}	\N	192.168.65.1	UNKNOWN	\N	curl/8.7.1	8b5018e28f25b8340d0e16b288b57cb5a747ab8fe7e0db8ec1763308e445b1ae	c56dfdcbad79b544a4b833e2dce6222731f3ca1ae95e18dafc7f905b9c271c31	curl/8.7.1|||	2025-12-19 09:19:22.532489
94f687f36d3b4242aa933bcc2eb39731	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJSUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJ1c2VyX2FkbWluXzAwMSIsImlhdCI6MTc2NjEzNjQ5NywiZXhwIjoxNzY2MTQwMDk3fQ.aEzB6uYFxHALuQKWFLOo7eUwxZKV5Yu4gvPAQaVNMCLYGlllPBKSlT3DsTuyb-VB0QTkol8IwH-doH2wHAmcdTUIijYCnE-X_Gul16GHTixCP-z7Ie5Nzxli-tdX8G5FjUYt3EJ1KxuoNl-4BScoFNMVY2yukXLXFKhDzEzbhhkSodRO5v0WmDpvqhfNdqErN09cBFJJ6VoMBh5BjzQSwVaqBh0W7K07ZcWwAmq73TmpD4EffrIb_cG0L-JRoqoEp-iprPX5rjuiMus_3_iWau6ubezKeapLPEyzL2EdBslw-CSjuJ8jg3DQ0_eQQr-wuH9p-kdjH-t1LT3ZlT3qyg","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":"ORG_BR_GROUP_FIN","status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1766136498356}	\N	172.18.0.5	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36	c56dfdcbad79b544a4b833e2dce6222731f3ca1ae95e18dafc7f905b9c271c31	8f35989e26bff67f65ad4ffeda27315b79696bba3c220e265d26270054d03e18	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-19 09:28:18.390298
\.


--
-- Data for Name: sys_erp_config; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_erp_config (id, name, erp_type, config_json, is_active, created_time, last_modified_time, org_id) FROM stdin;
2	SAP_VOUCHER_SYNC	GENERIC	{"system":"SAP ERP","frequency":"实时","description":"SAP 财务凭证自动同步接口"}	1	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
3	K3_INVENTORY_SYNC	KINGDEE	{"system":"金蝶云星空","frequency":"每日 23:00","description":"存货核算数据同步"}	1	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
4	OA_EXPENSE_SYNC	GENERIC	{"system":"泛微OA","frequency":"每小时","description":"员工报销单据同步"}	1	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
5	EKB_TRAVEL_SYNC	GENERIC	{"system":"易快报","frequency":"每小时","description":"差旅费用数据同步"}	1	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
6	HLY_REIMBURSE_SYNC	GENERIC	{"system":"汇联易","frequency":"每小时","description":"费用报销同步","status":"error"}	0	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
8	金蝶云星空	kingdee	{"baseUrl":"https://api.kingdee.com", "appKey":"mock", "appSecret":"mock"}	1	2025-12-19 09:08:48.551717	2025-12-19 09:08:48.551717	\N
9	泛微OA系统	weaver	{"baseUrl": "http://oa.nexus-demo.com", "appKey": "weaver_demo_key", "appSecret": "weaver_demo_secret", "accbookCode": "WEAVER01"}	1	2025-12-19 09:08:48.555417	2025-12-19 09:08:48.555417	\N
10	泛微E10中台	weaver_e10	{"baseUrl": "https://api.eteams.cn", "clientId": "7577f814096e611038c5eff1479d3b9", "tenantId": "1001", "clientSecret": "cdc0d6c9bc39312bd6288ced1789a49"}	1	2025-12-19 09:08:48.558952	2025-12-19 09:08:48.558952	\N
1	YONSUITE_VOUCHER_SYNC	YONSUITE	{\n        "baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway",\n        "appKey": "96a95c00982446cba484ccc4936b221b",\n        "appSecret": "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6",\n        "accbookCode": "BR01",\n        "extraConfig": ""\n    }	1	2025-12-19 09:08:48.434319	2025-12-19 09:08:48.434319	\N
7	用友YonSuite	yonsuite	{"appKey": "96a95c00982446cba484ccc4936b221b", "baseUrl": "https://dbox.yonyoucloud.com/iuap-api-gateway", "appSecret": "e9a58fd35f3ca3f0a46d27b8859758b1ed35f0b6", "accbookCode": "BRYS002", "extraConfig": ""}	1	2025-12-19 09:08:48.551717	2025-12-19 09:08:48.749421	\N
11	金蝶云星空 (标准模板)	kingdee	{"baseUrl":"https://api.kingdee.com/k3cloud/", "appKey":"YOUR_APP_KEY", "appSecret":"YOUR_APP_SECRET"}	0	2025-12-19 09:08:49.003877	2025-12-19 09:08:49.003877	\N
12	泛微 OA (标准模板)	weaver	{"baseUrl":"http://YOUR_OA_HOST/weaver/", "appKey":"YOUR_APP_KEY", "appSecret":"YOUR_APP_SECRET"}	0	2025-12-19 09:08:49.003877	2025-12-19 09:08:49.003877	\N
13	用友 YonSuite (标准模板)	yonsuite	{"baseUrl":"https://api.yonyoucloud.com/iuap-api-gateway", "appKey":"YOUR_APP_KEY", "appSecret":"YOUR_APP_SECRET"}	0	2025-12-19 09:08:49.003877	2025-12-19 09:08:49.003877	\N
\.


--
-- Data for Name: sys_erp_feedback_queue; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_erp_feedback_queue (id, voucher_id, archival_code, erp_type, erp_config_id, retry_count, max_retries, last_error, status, created_time, updated_time, next_retry_time) FROM stdin;
\.


--
-- Data for Name: sys_erp_scenario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_erp_scenario (id, config_id, scenario_key, name, description, is_active, sync_strategy, cron_expression, last_sync_time, last_sync_status, last_sync_msg, created_time, last_modified_time, params_json) FROM stdin;
1	7	PAYMENT_FILE_SYNC	付款单文件获取	从YonSuite获取资金结算文件 (AI Integration)	t	MANUAL	\N	\N	\N	\N	2025-12-19 09:08:48.616719	2025-12-19 09:08:48.739384	\N
\.


--
-- Data for Name: sys_erp_sub_interface; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_erp_sub_interface (id, scenario_id, interface_key, interface_name, description, is_active, sort_order, config_json, created_time, last_modified_time) FROM stdin;
1	1	LIST_QUERY	付款单列表查询	查询指定期间的付款单列表	t	1	\N	2025-12-19 09:08:48.780827	2025-12-19 09:08:48.780827
2	1	FILE_DOWNLOAD	付款单文件下载	下载付款单关联的文件	t	2	\N	2025-12-19 09:08:48.780827	2025-12-19 09:08:48.780827
\.


--
-- Data for Name: sys_ingest_request_status; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_ingest_request_status (request_id, status, message, created_time, updated_time, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: sys_org; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_org (id, name, code, parent_id, type, order_num, created_at, updated_at, deleted) FROM stdin;
ORG_BR_GROUP	泊冉集团有限公司	BR-GROUP	\N	COMPANY	1	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_SALES	泊冉销售有限公司	BR-SALES	ORG_BR_GROUP	COMPANY	1	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_TRADE	泊冉国际贸易有限公司	BR-TRADE	ORG_BR_GROUP	COMPANY	2	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_MFG	泊冉制造有限公司	BR-MFG	ORG_BR_GROUP	COMPANY	3	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_GROUP_FIN	财务管理部	BR-GROUP-FIN	ORG_BR_GROUP	DEPARTMENT	10	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_GROUP_HR	人力资源部	BR-GROUP-HR	ORG_BR_GROUP	DEPARTMENT	11	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_GROUP_IT	信息技术部	BR-GROUP-IT	ORG_BR_GROUP	DEPARTMENT	12	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_GROUP_LEGAL	法务合规部	BR-GROUP-LEGAL	ORG_BR_GROUP	DEPARTMENT	13	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_GROUP_AUDIT	审计监察部	BR-GROUP-AUDIT	ORG_BR_GROUP	DEPARTMENT	14	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_SALES_DOM	国内销售部	BR-SALES-DOM	ORG_BR_SALES	DEPARTMENT	1	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_SALES_INT	海外销售部	BR-SALES-INT	ORG_BR_SALES	DEPARTMENT	2	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_SALES_MKT	市场推广部	BR-SALES-MKT	ORG_BR_SALES	DEPARTMENT	3	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_SALES_FIN	财务部	BR-SALES-FIN	ORG_BR_SALES	DEPARTMENT	10	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_TRADE_IMP	进口业务部	BR-TRADE-IMP	ORG_BR_TRADE	DEPARTMENT	1	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_TRADE_EXP	出口业务部	BR-TRADE-EXP	ORG_BR_TRADE	DEPARTMENT	2	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_TRADE_LOG	物流仓储部	BR-TRADE-LOG	ORG_BR_TRADE	DEPARTMENT	3	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_TRADE_FIN	财务部	BR-TRADE-FIN	ORG_BR_TRADE	DEPARTMENT	10	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_MFG_PROD	生产管理部	BR-MFG-PROD	ORG_BR_MFG	DEPARTMENT	1	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_MFG_QC	质量控制部	BR-MFG-QC	ORG_BR_MFG	DEPARTMENT	2	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_MFG_RD	研发技术部	BR-MFG-RD	ORG_BR_MFG	DEPARTMENT	3	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_MFG_SUPPLY	采购供应部	BR-MFG-SUPPLY	ORG_BR_MFG	DEPARTMENT	4	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
ORG_BR_MFG_FIN	财务部	BR-MFG-FIN	ORG_BR_MFG	DEPARTMENT	10	2025-12-19 09:08:48.636974	2025-12-19 09:08:48.636974	0
\.


--
-- Data for Name: sys_permission; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_permission (id, perm_key, label, group_name, created_at, updated_at) FROM stdin;
perm_manage_users	manage_users	用户管理	系统管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_manage_roles	manage_roles	角色管理	系统管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_manage_org	manage_org	组织管理	系统管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_manage_settings	manage_settings	系统设置	系统管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_manage_fonds	manage_fonds	全宗管理	系统管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_all	nav:all	所有导航	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_portal	nav:portal	门户首页	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_panorama	nav:panorama	全景视图	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_pre_archive	nav:pre_archive	预归档库	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_collection	nav:collection	资料收集	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_archive_mgmt	nav:archive_mgmt	档案管理	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_query	nav:query	档案查询	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_borrowing	nav:borrowing	档案借阅	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_destruction	nav:destruction	档案销毁	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_warehouse	nav:warehouse	库房管理	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_stats	nav:stats	数据统计	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_nav_settings	nav:settings	系统设置	导航权限	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_create	archive:create	创建档案	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_view	archive:view	查看档案	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_edit	archive:edit	编辑档案	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_delete	archive:delete	删除档案	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_download	archive:download	下载档案	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_print	archive:print	打印档案	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_archive_approve	archive:approve	审批归档	档案操作	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_borrow_apply	borrow:apply	申请借阅	借阅管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_borrow_approve	borrow:approve	审批借阅	借阅管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_destruction_apply	destruction:apply	销毁鉴定	销毁管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_destruction_approve	destruction:approve	审批销毁	销毁管理	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_audit_view	audit:view	查看审计日志	安全审计	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
perm_audit_export	audit:export	导出审计日志	安全审计	2025-12-19 09:08:48.464971	2025-12-19 09:08:48.464971
\.


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_role (id, name, code, role_category, is_exclusive, description, permissions, data_scope, type, created_at, updated_at, deleted) FROM stdin;
role_super_admin	超级管理员	super_admin	system_admin	f	\N	["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users"]	self	custom	2025-12-19 09:08:48.165959	2025-12-19 09:08:48.165959	0
\.


--
-- Data for Name: sys_setting; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_setting (id, config_key, config_value, description, updated_at, category, created_at, deleted) FROM stdin;
\.


--
-- Data for Name: sys_sync_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_sync_history (id, scenario_id, sync_start_time, sync_end_time, status, total_count, success_count, fail_count, error_message, sync_params, created_time, operator_id, client_ip, four_nature_summary) FROM stdin;
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) FROM stdin;
user_admin_001	admin	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	系统管理员	BR-GROUP	admin@nexusarchive.local	\N	\N	ORG_BR_GROUP_FIN	active	2025-12-19 09:28:18.280485	\N	\N	\N	2025-12-19 09:08:48.165959	\N	0
\.


--
-- Data for Name: sys_user_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user_role (user_id, role_id) FROM stdin;
user_admin_001	role_super_admin
\.


--
-- Data for Name: ys_sales_out; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ys_sales_out (id, ys_id, code, vouchdate, status, cust_name, warehouse_name, total_quantity, memo, raw_json, sync_time, created_time) FROM stdin;
\.


--
-- Data for Name: ys_sales_out_detail; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ys_sales_out_detail (id, sales_out_id, ys_detail_id, rowno, product_code, product_name, qty, unit_name, ori_money, ori_tax) FROM stdin;
\.


--
-- Name: arc_archive_batch_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.arc_archive_batch_id_seq', 1, false);


--
-- Name: sys_erp_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_erp_config_id_seq', 13, true);


--
-- Name: sys_erp_feedback_queue_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_erp_feedback_queue_id_seq', 1, false);


--
-- Name: sys_erp_scenario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_erp_scenario_id_seq', 1, true);


--
-- Name: sys_erp_sub_interface_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_erp_sub_interface_id_seq', 2, true);


--
-- Name: sys_sync_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_sync_history_id_seq', 1, false);


--
-- Name: ys_sales_out_detail_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ys_sales_out_detail_id_seq', 1, false);


--
-- Name: ys_sales_out_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ys_sales_out_id_seq', 1, false);


--
-- Name: acc_archive acc_archive_archive_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_archive_code_key UNIQUE (archive_code);


--
-- Name: acc_archive_attachment acc_archive_attachment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_attachment
    ADD CONSTRAINT acc_archive_attachment_pkey PRIMARY KEY (id);


--
-- Name: acc_archive acc_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_pkey PRIMARY KEY (id);


--
-- Name: acc_archive_relation acc_archive_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_pkey PRIMARY KEY (id);


--
-- Name: acc_archive_volume acc_archive_volume_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_volume
    ADD CONSTRAINT acc_archive_volume_pkey PRIMARY KEY (id);


--
-- Name: arc_abnormal_voucher arc_abnormal_voucher_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_abnormal_voucher
    ADD CONSTRAINT arc_abnormal_voucher_pkey PRIMARY KEY (id);


--
-- Name: arc_archive_batch arc_archive_batch_batch_no_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_archive_batch
    ADD CONSTRAINT arc_archive_batch_batch_no_key UNIQUE (batch_no);


--
-- Name: arc_archive_batch arc_archive_batch_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_archive_batch
    ADD CONSTRAINT arc_archive_batch_pkey PRIMARY KEY (id);


--
-- Name: arc_convert_log arc_convert_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_convert_log
    ADD CONSTRAINT arc_convert_log_pkey PRIMARY KEY (id);


--
-- Name: arc_file_content arc_file_content_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_file_content
    ADD CONSTRAINT arc_file_content_pkey PRIMARY KEY (id);


--
-- Name: arc_file_metadata_index arc_file_metadata_index_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_file_metadata_index
    ADD CONSTRAINT arc_file_metadata_index_pkey PRIMARY KEY (id);


--
-- Name: arc_reconciliation_record arc_reconciliation_record_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_reconciliation_record
    ADD CONSTRAINT arc_reconciliation_record_pkey PRIMARY KEY (id);


--
-- Name: arc_signature_log arc_signature_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_signature_log
    ADD CONSTRAINT arc_signature_log_pkey PRIMARY KEY (id);


--
-- Name: audit_inspection_log audit_inspection_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_inspection_log
    ADD CONSTRAINT audit_inspection_log_pkey PRIMARY KEY (id);


--
-- Name: bas_erp_config bas_erp_config_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bas_erp_config
    ADD CONSTRAINT bas_erp_config_pkey PRIMARY KEY (id);


--
-- Name: bas_fonds bas_fonds_fonds_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bas_fonds
    ADD CONSTRAINT bas_fonds_fonds_code_key UNIQUE (fonds_code);


--
-- Name: bas_fonds bas_fonds_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bas_fonds
    ADD CONSTRAINT bas_fonds_pkey PRIMARY KEY (id);


--
-- Name: bas_location bas_location_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bas_location
    ADD CONSTRAINT bas_location_pkey PRIMARY KEY (id);


--
-- Name: biz_archive_approval biz_archive_approval_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.biz_archive_approval
    ADD CONSTRAINT biz_archive_approval_pkey PRIMARY KEY (id);


--
-- Name: biz_borrowing biz_borrowing_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.biz_borrowing
    ADD CONSTRAINT biz_borrowing_pkey PRIMARY KEY (id);


--
-- Name: biz_destruction biz_destruction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.biz_destruction
    ADD CONSTRAINT biz_destruction_pkey PRIMARY KEY (id);


--
-- Name: biz_open_appraisal biz_open_appraisal_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.biz_open_appraisal
    ADD CONSTRAINT biz_open_appraisal_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: sys_archival_code_sequence sys_archival_code_sequence_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_archival_code_sequence
    ADD CONSTRAINT sys_archival_code_sequence_pkey PRIMARY KEY (fonds_code, fiscal_year, category_code);


--
-- Name: sys_audit_log sys_audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_audit_log
    ADD CONSTRAINT sys_audit_log_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_config sys_erp_config_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_config
    ADD CONSTRAINT sys_erp_config_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_feedback_queue sys_erp_feedback_queue_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_feedback_queue
    ADD CONSTRAINT sys_erp_feedback_queue_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_scenario sys_erp_scenario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_scenario
    ADD CONSTRAINT sys_erp_scenario_pkey PRIMARY KEY (id);


--
-- Name: sys_erp_sub_interface sys_erp_sub_interface_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_sub_interface
    ADD CONSTRAINT sys_erp_sub_interface_pkey PRIMARY KEY (id);


--
-- Name: sys_ingest_request_status sys_ingest_request_status_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_ingest_request_status
    ADD CONSTRAINT sys_ingest_request_status_pkey PRIMARY KEY (request_id);


--
-- Name: sys_org sys_org_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_org
    ADD CONSTRAINT sys_org_pkey PRIMARY KEY (id);


--
-- Name: sys_permission sys_permission_perm_key_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_perm_key_key UNIQUE (perm_key);


--
-- Name: sys_permission sys_permission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_pkey PRIMARY KEY (id);


--
-- Name: sys_role sys_role_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_code_key UNIQUE (code);


--
-- Name: sys_role sys_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (id);


--
-- Name: sys_setting sys_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_setting
    ADD CONSTRAINT sys_setting_pkey PRIMARY KEY (id);


--
-- Name: sys_sync_history sys_sync_history_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT sys_sync_history_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user_role sys_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: sys_user sys_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_username_key UNIQUE (username);


--
-- Name: acc_archive_attachment uk_archive_file; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_attachment
    ADD CONSTRAINT uk_archive_file UNIQUE (archive_id, file_id);


--
-- Name: sys_erp_scenario uk_config_scenario; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_scenario
    ADD CONSTRAINT uk_config_scenario UNIQUE (config_id, scenario_key);


--
-- Name: bas_erp_config uk_erp_config_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bas_erp_config
    ADD CONSTRAINT uk_erp_config_name UNIQUE (name);


--
-- Name: ys_sales_out_detail ys_sales_out_detail_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out_detail
    ADD CONSTRAINT ys_sales_out_detail_pkey PRIMARY KEY (id);


--
-- Name: ys_sales_out ys_sales_out_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out
    ADD CONSTRAINT ys_sales_out_pkey PRIMARY KEY (id);


--
-- Name: ys_sales_out ys_sales_out_ys_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out
    ADD CONSTRAINT ys_sales_out_ys_id_key UNIQUE (ys_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_abnormal_create_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_abnormal_create_time ON public.arc_abnormal_voucher USING btree (create_time);


--
-- Name: idx_abnormal_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_abnormal_status ON public.arc_abnormal_voucher USING btree (status);


--
-- Name: idx_arc_file_content_fiscal_year; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_arc_file_content_fiscal_year ON public.arc_file_content USING btree (fiscal_year);


--
-- Name: idx_arc_file_content_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_arc_file_content_status ON public.arc_file_content USING btree (pre_archive_status);


--
-- Name: idx_archival_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archival_code ON public.arc_file_content USING btree (archival_code);


--
-- Name: idx_archive_approval_applicant; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_approval_applicant ON public.biz_archive_approval USING btree (applicant_id);


--
-- Name: idx_archive_approval_archive_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_approval_archive_id ON public.biz_archive_approval USING btree (archive_id);


--
-- Name: idx_archive_approval_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_approval_created_at ON public.biz_archive_approval USING btree (created_at);


--
-- Name: idx_archive_approval_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_approval_status ON public.biz_archive_approval USING btree (status);


--
-- Name: idx_archive_attachment_archive; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_attachment_archive ON public.acc_archive_attachment USING btree (archive_id);


--
-- Name: idx_archive_attachment_file; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_attachment_file ON public.acc_archive_attachment USING btree (file_id);


--
-- Name: idx_archive_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_category ON public.acc_archive USING btree (category_code);


--
-- Name: idx_archive_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_code ON public.acc_archive USING btree (archive_code);


--
-- Name: idx_archive_destruction_hold; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_destruction_hold ON public.acc_archive USING btree (destruction_hold);


--
-- Name: idx_archive_fonds_year; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_fonds_year ON public.acc_archive USING btree (fonds_no, fiscal_year);


--
-- Name: idx_archive_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_status ON public.acc_archive USING btree (status);


--
-- Name: idx_archive_volume_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_volume_id ON public.acc_archive USING btree (volume_id);


--
-- Name: idx_audit_inspection_archive_compliance; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_inspection_archive_compliance ON public.audit_inspection_log USING btree (archive_id, is_compliant);


--
-- Name: idx_audit_inspection_compliance; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_inspection_compliance ON public.audit_inspection_log USING btree (is_compliant, inspection_time);


--
-- Name: idx_audit_log_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_log_created_at ON public.sys_audit_log USING btree (created_at);


--
-- Name: idx_audit_log_hash; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_log_hash ON public.sys_audit_log USING btree (log_hash);


--
-- Name: idx_bas_fonds_org; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_bas_fonds_org ON public.bas_fonds USING btree (org_id);


--
-- Name: idx_borrowing_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_borrowing_status ON public.biz_borrowing USING btree (status);


--
-- Name: idx_borrowing_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_borrowing_user ON public.biz_borrowing USING btree (user_id);


--
-- Name: idx_convert_log_archive; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_convert_log_archive ON public.arc_convert_log USING btree (archive_id);


--
-- Name: idx_convert_log_convert_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_convert_log_convert_time ON public.arc_convert_log USING btree (convert_time);


--
-- Name: idx_convert_log_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_convert_log_status ON public.arc_convert_log USING btree (status);


--
-- Name: idx_convert_log_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_convert_log_time ON public.arc_convert_log USING btree (created_time);


--
-- Name: idx_created_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_created_time ON public.arc_file_content USING btree (created_time);


--
-- Name: idx_destruction_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_destruction_status ON public.biz_destruction USING btree (status);


--
-- Name: idx_erp_config_enabled; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_erp_config_enabled ON public.bas_erp_config USING btree (enabled);


--
-- Name: idx_erp_config_org; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_erp_config_org ON public.sys_erp_config USING btree (org_id);


--
-- Name: idx_erp_config_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_erp_config_type ON public.bas_erp_config USING btree (adapter_type);


--
-- Name: idx_feedback_queue_erp_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_feedback_queue_erp_type ON public.sys_erp_feedback_queue USING btree (erp_type);


--
-- Name: idx_feedback_queue_next_retry; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_feedback_queue_next_retry ON public.sys_erp_feedback_queue USING btree (next_retry_time) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: idx_feedback_queue_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_feedback_queue_status ON public.sys_erp_feedback_queue USING btree (status);


--
-- Name: idx_file_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_id ON public.arc_file_metadata_index USING btree (file_id);


--
-- Name: idx_file_item_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_item_id ON public.arc_file_content USING btree (item_id);


--
-- Name: idx_file_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_file_type ON public.arc_file_content USING btree (file_type);


--
-- Name: idx_invoice_number; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_invoice_number ON public.arc_file_metadata_index USING btree (invoice_number);


--
-- Name: idx_issue_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_issue_date ON public.arc_file_metadata_index USING btree (issue_date);


--
-- Name: idx_location_parent; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_location_parent ON public.bas_location USING btree (parent_id);


--
-- Name: idx_location_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_location_type ON public.bas_location USING btree (type);


--
-- Name: idx_open_appraisal_archive_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_open_appraisal_archive_id ON public.biz_open_appraisal USING btree (archive_id);


--
-- Name: idx_open_appraisal_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_open_appraisal_created_at ON public.biz_open_appraisal USING btree (created_at);


--
-- Name: idx_open_appraisal_result; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_open_appraisal_result ON public.biz_open_appraisal USING btree (appraisal_result);


--
-- Name: idx_open_appraisal_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_open_appraisal_status ON public.biz_open_appraisal USING btree (status);


--
-- Name: idx_permission_group; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_permission_group ON public.sys_permission USING btree (group_name);


--
-- Name: idx_permission_key; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_permission_key ON public.sys_permission USING btree (perm_key);


--
-- Name: idx_recon_record_operator; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_recon_record_operator ON public.arc_reconciliation_record USING btree (operator_id);


--
-- Name: idx_seller_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_seller_name ON public.arc_file_metadata_index USING btree (seller_name);


--
-- Name: idx_signature_archive; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signature_archive ON public.arc_signature_log USING btree (archive_id);


--
-- Name: idx_signature_file; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signature_file ON public.arc_signature_log USING btree (file_id);


--
-- Name: idx_signature_verify_result; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signature_verify_result ON public.arc_signature_log USING btree (verify_result);


--
-- Name: idx_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_status ON public.sys_ingest_request_status USING btree (status);


--
-- Name: idx_sub_interface_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_sub_interface_unique ON public.sys_erp_sub_interface USING btree (scenario_id, interface_key);


--
-- Name: idx_sync_history_operator; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sync_history_operator ON public.sys_sync_history USING btree (operator_id);


--
-- Name: idx_sync_history_scenario; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sync_history_scenario ON public.sys_sync_history USING btree (scenario_id);


--
-- Name: idx_sync_history_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sync_history_time ON public.sys_sync_history USING btree (sync_start_time DESC);


--
-- Name: idx_sys_org_parent; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_org_parent ON public.sys_org USING btree (parent_id);


--
-- Name: idx_sys_setting_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_setting_category ON public.sys_setting USING btree (category);


--
-- Name: idx_sys_user_role_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_user_role_role ON public.sys_user_role USING btree (role_id);


--
-- Name: idx_sys_user_role_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_user_role_user ON public.sys_user_role USING btree (user_id);


--
-- Name: idx_volume_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_volume_category ON public.acc_archive_volume USING btree (category_code);


--
-- Name: idx_volume_fiscal_period; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_volume_fiscal_period ON public.acc_archive_volume USING btree (fiscal_period);


--
-- Name: idx_volume_fonds_year; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_volume_fonds_year ON public.acc_archive_volume USING btree (fonds_no, fiscal_year);


--
-- Name: idx_ys_sales_out_code; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ys_sales_out_code ON public.ys_sales_out USING btree (code);


--
-- Name: idx_ys_sales_out_detail_sales_out_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ys_sales_out_detail_sales_out_id ON public.ys_sales_out_detail USING btree (sales_out_id);


--
-- Name: idx_ys_sales_out_vouchdate; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ys_sales_out_vouchdate ON public.ys_sales_out USING btree (vouchdate);


--
-- Name: ux_acc_archive_unique_biz_id_not_deleted; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ux_acc_archive_unique_biz_id_not_deleted ON public.acc_archive USING btree (unique_biz_id) WHERE (deleted = 0);


--
-- Name: acc_archive_relation acc_archive_relation_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: acc_archive_relation acc_archive_relation_target_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_target_id_fkey FOREIGN KEY (target_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: arc_file_metadata_index arc_file_metadata_index_file_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_file_metadata_index
    ADD CONSTRAINT arc_file_metadata_index_file_id_fkey FOREIGN KEY (file_id) REFERENCES public.arc_file_content(id) ON DELETE CASCADE;


--
-- Name: audit_inspection_log audit_inspection_log_archive_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_inspection_log
    ADD CONSTRAINT audit_inspection_log_archive_id_fkey FOREIGN KEY (archive_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: arc_file_content fk_file_content_batch; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.arc_file_content
    ADD CONSTRAINT fk_file_content_batch FOREIGN KEY (batch_id) REFERENCES public.arc_archive_batch(id) ON DELETE SET NULL;


--
-- Name: sys_sync_history fk_sync_history_operator; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT fk_sync_history_operator FOREIGN KEY (operator_id) REFERENCES public.sys_user(id) ON DELETE SET NULL;


--
-- Name: sys_sync_history fk_sync_history_scenario; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_sync_history
    ADD CONSTRAINT fk_sync_history_scenario FOREIGN KEY (scenario_id) REFERENCES public.sys_erp_scenario(id) ON DELETE CASCADE;


--
-- Name: sys_erp_scenario sys_erp_scenario_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_scenario
    ADD CONSTRAINT sys_erp_scenario_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.sys_erp_config(id) ON DELETE CASCADE;


--
-- Name: sys_user sys_user_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_org(id);


--
-- Name: ys_sales_out_detail ys_sales_out_detail_sales_out_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out_detail
    ADD CONSTRAINT ys_sales_out_detail_sales_out_id_fkey FOREIGN KEY (sales_out_id) REFERENCES public.ys_sales_out(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict gcapTEDdkMeEFzomi28wRe6XN7WnD38C3KdSfCdvVvN0iLRje8jQmKoZ9cPZdpy

