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
    org_name character varying(100) NOT NULL,
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

COMMENT ON COLUMN public.acc_archive.summary IS '档案摘要/说明 - SM4加密存储';


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

COMMENT ON COLUMN public.acc_archive_volume.title IS '案卷标题 (新增)';


--
-- Name: COLUMN acc_archive_volume.fiscal_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.fiscal_year IS '会计年度 (原 archive_year)';


--
-- Name: COLUMN acc_archive_volume.retention_period; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.retention_period IS '保管期限';


--
-- Name: COLUMN acc_archive_volume.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.status IS '状态: draft, pending, archived';


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
    business_doc_no character varying(100)
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

COMMENT ON COLUMN public.arc_file_content.certificate IS '数字证书 (Base64编码)';


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

COMMENT ON COLUMN public.arc_file_content.creator IS '责任者/创建人';


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

COMMENT ON COLUMN public.arc_file_content.check_result IS '四性检测结果(JSON)';


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

COMMENT ON COLUMN public.arc_file_content.business_doc_no IS '业务单据号 (来自 ERP 的凭证号)';


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
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.bas_fonds OWNER TO postgres;

--
-- Name: TABLE bas_fonds; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.bas_fonds IS '全宗基础信息表';


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
    last_modified_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
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
-- Name: sys_erp_config id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_erp_config ALTER COLUMN id SET DEFAULT nextval('public.sys_erp_config_id_seq'::regclass);


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
6b13cc36b1424f5e856631094e04a2f6	DEFAULT	QZ-2025-30Y-AC04-E204FA	AC04	945c9a582f75721cae8cb79731df3325ae2b2386df991ca6e88003048e8151a9002a8a4efa863ccad024ac0300bb40d2	2025	\N	30Y	默认立档单位	a91b5ea691d72afbff4545a050bce4ba7350b8fdfd399827384795309b31dff3002a8a4efa863ccad024ac0300bb40d2	REJECTED	\N	2025-01-01	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	2025-12-10 11:52:42.03024	0	\N	f	\N	dbeed966353fcb3bc5118361039ef2ee4003a8158104ebc73823d3c5abf87edd002a8a4efa863ccad024ac0300bb40d2
43b6fb802c6942f68a777a7346956789	DEFAULT	QZ-2025-30Y-AC04-A035AA	AC01	945c9a582f75721cae8cb79731df3325ae2b2386df991ca6e88003048e8151a9002a8a4efa863ccad024ac0300bb40d2	2025	\N	30Y	默认立档单位	a91b5ea691d72afbff4545a050bce4ba7350b8fdfd399827384795309b31dff3002a8a4efa863ccad024ac0300bb40d2	ARCHIVED	\N	2025-01-01	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	dbeed966353fcb3bc5118361039ef2ee4003a8158104ebc73823d3c5abf87edd002a8a4efa863ccad024ac0300bb40d2
191f1f70b9ca436388d556fc184b7dcf	DEFAULT	QZ-2025-30Y-AC04-551BB2	AC01	f05065b1d98438b079a0e60699c6125f43e6370286302753390e4eb7b0d63aa73973321ac10dea99bca230c25837e54c205a8caf65277547f0d70b6bccd27ab5548cee3310d602703d629fa8a3003fe2151497ad783fa9df24a5c386e136f034838dbcbc3e9a53da453948c3ef4506ef359e0865ffbf77241c459fba34ec5f7b5bf424c064918f54397b158989161ba5488bb1aaba27d7a7d5b75cf27e8c6373002a8a4efa863ccad024ac0300bb40d2	2025	\N	30Y	默认立档单位	fdbba33bb372b53a96b287f45c46f2e589f848d7574e7b7dda5fc56710994c58002a8a4efa863ccad024ac0300bb40d2	ARCHIVED	\N	2025-01-01	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	f05065b1d98438b079a0e60699c6125f43e6370286302753390e4eb7b0d63aa73973321ac10dea99bca230c25837e54c205a8caf65277547f0d70b6bccd27ab5548cee3310d602703d629fa8a3003fe2151497ad783fa9df24a5c386e136f034838dbcbc3e9a53da453948c3ef4506ef359e0865ffbf77241c459fba34ec5f7bdd7a8bdcb6c83dcb160a30407d009289098826adab5593c0fefd1b1b93355d00002a8a4efa863ccad024ac0300bb40d2
bf8ebba57c914bbdae086700a563bc01	DEFAULT	QZ-2025-30Y-AC04-4C4CBC	AC01	e9675ff9327058df694eda30887878b5437d666bdf345ad14ec89f8b03a63d95a2f6645574f0c717b87d6010759fcfd99466d6a0448bddd5733db8c0263a88cd3fa36bb6ef2ac9f8ca9ec3071d010780381a8c09501ef99086d094d5ec689779a0246f00320741491330c1414e77841558ee850e9cd1878766689ffba214d8811f0006d54ac263530e64ae49863cd5b64580d8fce744f02b65057d2ce4f0bb24002a8a4efa863ccad024ac0300bb40d2	2025	\N	30Y	默认立档单位	631a510cfb42da3784c9855bce12d81014a7518969494759b74ffbe4514c4e29002a8a4efa863ccad024ac0300bb40d2	ARCHIVED	\N	2025-01-01	\N	\N	\N	internal	\N	\N	\N	\N	\N	\N	\N	\N	0	\N	f	\N	e9675ff9327058df694eda30887878b5437d666bdf345ad14ec89f8b03a63d95a2f6645574f0c717b87d6010759fcfd99466d6a0448bddd5733db8c0263a88cd3fa36bb6ef2ac9f8ca9ec3071d010780381a8c09501ef99086d094d5ec689779a0246f00320741491330c1414e77841558ee850e9cd1878766689ffba214d8815f307917453e6e66666b7d43cfba5af79fa781fc9359073be7613216dc605fe0002a8a4efa863ccad024ac0300bb40d2
seed-contract-001	DEMO	CON-2023-098	AC04	年度技术服务协议	2023	01	30Y	演示公司	系统	archived	150000.00	2023-01-15	CON-2023-098	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-contract-002	DEMO	C-202511-002	AC04	服务器采购合同	2025	11	30Y	演示公司	系统	archived	450000.00	2025-11-15	C-202511-002	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-invoice-001	DEMO	INV-202311-089	AC01	阿里云计算服务费发票	2023	11	30Y	演示公司	系统	archived	12800.00	2023-11-02	INV-202311-089	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-invoice-002	DEMO	INV-202311-092	AC01	服务器采购发票	2023	11	30Y	演示公司	系统	archived	45200.00	2023-11-03	INV-202311-092	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-voucher-001	DEMO	JZ-202311-0052	AC01	11月技术部费用报销	2023	11	30Y	演示公司	系统	archived	58000.00	2023-11-05	JZ-202311-0052	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-voucher-002	DEMO	V-202511-TEST	AC01	报销差旅费	2025	11	30Y	演示公司	张三	archived	5280.00	2025-11-07	V-202511-TEST	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-receipt-001	DEMO	B-20231105-003	AC04	招商银行付款回单	2023	11	30Y	演示公司	系统	archived	58000.00	2023-11-05	B-20231105-003	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
seed-report-001	DEMO	REP-2023-11	AC03	11月科目余额表	2023	11	30Y	演示公司	系统	archived	\N	2023-11-30	REP-2023-11	\N	\N	internal	\N	\N	system	\N	\N	\N	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939	0	\N	f	\N	\N
72330ac0d8f142ed895fa6ae1f4ce899	BR01	YS-2025-08-记-1	AC01	3db4b1799d98d319bffed09d9334c9ec88b9c3a103dd1516ab41f73a3a37f1c2	2025	2025-08	10Y	泊冉演示集团	a592b2dcbab1690d7035f04792172f32	archived	150.00	2025-08-01	YonSuite_2320437652155531280	\N	[{"id": "2320437652155531281", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 150.0, "voucherid": "2320437652155531280", "accsubject": {"id": "2319002806690775239", "code": "6401", "name": "主营业务成本", "cashCategory": "Other"}, "credit_org": 0.0, "description": "内部交易出库核算", "recordnumber": 1, "debit_original": 150.0, "credit_original": 0.0}, {"id": "2320437652155531282", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652155531280", "accsubject": {"id": "2319002806690775072", "code": "1405", "name": "库存商品", "cashCategory": "Other"}, "credit_org": 150.0, "description": "内部交易出库核算", "recordnumber": 2, "debit_original": 0.0, "credit_original": 150.0}]	internal	\N	\N	system	\N	\N	\N	2025-12-11 18:21:58.720048	2025-12-11 18:21:58.720056	0	\N	f	\N	\N
417e705256434d5b9521704dfa93652e	BR01	YS-2025-08-记-2	AC01	3db4b1799d98d319bffed09d9334c9eca987b1f2912622cc52a3312c325611d4	2025	2025-08	10Y	泊冉演示集团	a592b2dcbab1690d7035f04792172f32	archived	150.00	2025-08-01	YonSuite_2320392357430427652	\N	[{"id": "2320392357430427653", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 150.0, "voucherid": "2320392357430427652", "accsubject": {"id": "2319002806690775056", "code": "112201", "name": "应收账款_贷款", "cashCategory": "Other"}, "credit_org": 0.0, "description": "确认应收", "recordnumber": 1, "debit_original": 150.0, "credit_original": 0.0}, {"id": "2320392357430427654", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320392357430427652", "accsubject": {"id": "2319002806690775143", "code": "2221010601", "name": "应交税费_应交增值税_销项税额_销项", "cashCategory": "Other"}, "credit_org": 17.26, "description": "应交销项税", "recordnumber": 2, "debit_original": 0.0, "credit_original": 17.26}, {"id": "2320392357430427655", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320392357430427652", "accsubject": {"id": "2319002806690775232", "code": "600101", "name": "主营业务收入_贷款", "cashCategory": "Other"}, "credit_org": 132.74, "description": "收入", "recordnumber": 3, "debit_original": 0.0, "credit_original": 132.74}]	internal	\N	\N	system	\N	\N	\N	2025-12-11 18:21:58.758264	2025-12-11 18:21:58.758272	0	\N	f	\N	\N
baf871cc09dd4230be8cfff25cea13ac	BR01	YS-2025-08-记-3	AC01	3db4b1799d98d319bffed09d9334c9ecacb425f494a754a013511207229943ec	2025	2025-08	10Y	泊冉演示集团	a592b2dcbab1690d7035f04792172f32	draft	30.00	2025-08-09	YonSuite_2320437652170211340	\N	[{"id": "2320437652170211341", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 30.0, "voucherid": "2320437652170211340", "accsubject": {"id": "2319002806690775239", "code": "6401", "name": "主营业务成本", "cashCategory": "Other"}, "credit_org": 0.0, "description": "销售出库核算", "recordnumber": 1, "debit_original": 30.0, "credit_original": 0.0}, {"id": "2320437652170211342", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652170211340", "accsubject": {"id": "2319002806690775072", "code": "1405", "name": "库存商品", "cashCategory": "Other"}, "credit_org": 30.0, "description": "销售出库核算", "recordnumber": 2, "debit_original": 0.0, "credit_original": 30.0}]	internal	\N	\N	system	\N	\N	\N	2025-12-11 18:21:58.762875	2025-12-11 18:21:58.762884	0	\N	f	\N	\N
a059d7ca1de84bbfa53fd4a25c121ce0	BR01	YS-2025-08-记-4	AC01	3db4b1799d98d319bffed09d9334c9ece7b3ade8e8300b34eb5958636238083d	2025	2025-08	10Y	泊冉演示集团	a592b2dcbab1690d7035f04792172f32	draft	50.00	2025-08-09	YonSuite_2320437652155531287	\N	[{"id": "2320437652155531288", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 50.0, "voucherid": "2320437652155531287", "accsubject": {"id": "2319002806690775056", "code": "112201", "name": "应收账款_贷款", "cashCategory": "Other"}, "credit_org": 0.0, "description": "确认应收", "recordnumber": 1, "debit_original": 50.0, "credit_original": 0.0}, {"id": "2320437652155531289", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652155531287", "accsubject": {"id": "2319002806690775143", "code": "2221010601", "name": "应交税费_应交增值税_销项税额_销项", "cashCategory": "Other"}, "credit_org": 5.75, "description": "应交销项税", "recordnumber": 2, "debit_original": 0.0, "credit_original": 5.75}, {"id": "2320437652155531290", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652155531287", "accsubject": {"id": "2319002806690775232", "code": "600101", "name": "主营业务收入_贷款", "cashCategory": "Other"}, "credit_org": 44.25, "description": "收入", "recordnumber": 3, "debit_original": 0.0, "credit_original": 44.25}]	internal	\N	\N	system	\N	\N	\N	2025-12-11 18:21:58.766795	2025-12-11 18:21:58.766806	0	\N	f	\N	\N
\.


--
-- Data for Name: acc_archive_relation; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) FROM stdin;
seed-rel-001	seed-contract-001	seed-voucher-001	BASIS	合同依据	system	2025-12-11 17:29:45.962939	0
seed-rel-002	seed-invoice-001	seed-voucher-001	ORIGINAL_VOUCHER	原始凭证	system	2025-12-11 17:29:45.962939	0
seed-rel-003	seed-invoice-002	seed-voucher-001	ORIGINAL_VOUCHER	原始凭证	system	2025-12-11 17:29:45.962939	0
seed-rel-004	seed-voucher-001	seed-receipt-001	CASH_FLOW	资金流	system	2025-12-11 17:29:45.962939	0
seed-rel-005	seed-voucher-001	seed-report-001	ARCHIVE	归档	system	2025-12-11 17:29:45.962939	0
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
-- Data for Name: arc_convert_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_convert_log (id, archive_id, source_format, target_format, source_path, target_path, status, error_message, file_size_bytes, convert_duration_ms, created_time, source_size, target_size, duration_ms, convert_time) FROM stdin;
\.


--
-- Data for Name: arc_file_content; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value, certificate, pre_archive_status, fiscal_year, voucher_type, creator, fonds_code, source_system, check_result, checked_time, archived_time, business_doc_no) FROM stdin;
48a8cc9c-0575-447b-ac02-f30e1adeb9c0	TEMP-POOL-20251210-48A8CC9C	优刻得科技股份有限公司_20251023203642_25512000000251832086.pdf	PDF	137498	e8dee1c1d03f12f55ca601c15543637e2aea391b2e741ed42d14927b46b06967	SM3	./data/temp/uploads/48a8cc9c-0575-447b-ac02-f30e1adeb9c0.pdf	2025-12-10 10:44:40.043684	\N	\N	\N	\N	\N	\N	PENDING_CHECK	\N	\N	\N	\N	Web上传	\N	\N	\N	\N
928f28b0-5c4a-4a92-815d-5af67726117f	QZ-2025-30Y-AC04-A035AA	test_upload.ofd	OFD	615	9e8eccc6ddf0831dccee0eadb5677d5e497c755f5ba91d22c4c337fc85952325	SM3	./data/temp/uploads/test_upload.ofd	2025-12-10 10:48:20.768268	\N	\N	e27b2b697574bc9894c3122696cd201180c15075c15e069b8df35821aa73da6a	\N	\\x5349474e45445f534d325f323032352d31322d31305431313a35393a32382e373338303537	\N	ARCHIVED	2025	AC01	Automation Test	\N	Web上传	WARNING	2025-12-10 10:51:02.102392	2025-12-10 11:59:28.738536	\N
2d91f86d-4f45-44d8-a84b-815efcfff12e	QZ-2025-30Y-AC04-551BB2	优刻得科技股份有限公司_20251023203642_25512000000251832086.ofd	OFD	137498	e8dee1c1d03f12f55ca601c15543637e2aea391b2e741ed42d14927b46b06967	SM3	./data/temp/uploads/优刻得科技股份有限公司_20251023203642_25512000000251832086.ofd	2025-12-10 12:01:38.812568	\N	\N	c4f2d99192dbc16ea13125214594373fc15635ca545fadf16b92fff7b4536a34	\N	\\x5349474e45445f534d325f323032352d31322d31305431323a30323a32382e383237333039	\N	ARCHIVED	2025	AC01	测试	\N	Web上传	WARNING	2025-12-10 12:02:07.221718	2025-12-10 12:02:28.827366	\N
581d7c40-9a85-4e89-86c9-304059dba55e	QZ-2025-30Y-AC04-4C4CBC	dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.ofd	OFD	104331	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	SM3	./data/temp/uploads/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.ofd	2025-12-10 13:28:10.183566	\N	\N	94792fcac83299453d28815779f8fc4ad7d365e4c610962ed4b593e9ffe2814c	\N	\\x5349474e45445f534d325f323032352d31322d31305431333a33303a34352e313530383937	\N	ARCHIVED	2025	AC01	demo	\N	Web上传	WARNING	2025-12-10 13:28:34.090284	2025-12-10 13:30:45.151004	\N
9db0a7da-cc8e-41f4-b850-65fe4385349a	TEMP-POOL-20251210-9DB0A7DA	优刻得科技股份有限公司_20251023203642_25512000000251832086.pdf	PDF	137498	e8dee1c1d03f12f55ca601c15543637e2aea391b2e741ed42d14927b46b06967	SM3	./data/temp/uploads/9db0a7da-cc8e-41f4-b850-65fe4385349a.pdf	2025-12-10 14:19:43.106945	\N	\N	\N	\N	\N	\N	PENDING_CHECK	\N	\N	\N	\N	Web上传	\N	\N	\N	\N
\.


--
-- Data for Name: arc_file_metadata_index; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_file_metadata_index (id, file_id, invoice_code, invoice_number, total_amount, seller_name, issue_date, parsed_time, parser_type) FROM stdin;
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

COPY public.bas_fonds (id, fonds_code, fonds_name, company_name, description, created_by, created_time, updated_time) FROM stdin;
demo-fonds-001	DEMO	演示全宗	演示公司	系统初始演示数据	system	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
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
1998586316413833217	6b13cc36b1424f5e856631094e04a2f6	QZ-2025-30Y-AC04-E204FA	c1f3f3ce6f784e07bb1eec82872846df	admin	管理员	批量归档申请	admin	管理员	REJECTED	demo	2025-12-10 11:52:42.027369	2025-12-10 10:51:24.179589	2025-12-10 11:52:42.027386	0	默认立档单位
1998601784969240577	43b6fb802c6942f68a777a7346956789	QZ-2025-30Y-AC04-A035AA	c1f3f3ce6f784e07bb1eec82872846df	admin	管理员	批量归档申请	admin	管理员	APPROVED	批准归档	2025-12-10 11:59:28.059557	2025-12-10 11:52:52.415438	2025-12-10 11:59:28.059574	0	默认立档单位
1998604143787053057	191f1f70b9ca436388d556fc184b7dcf	QZ-2025-30Y-AC04-551BB2	3577fb3913942701e38ed6fe4b5d486781f5993df46c86cd57217c477b713506fd83639425997a66abd4d9dc9e09a90f18cd36f93742d0e03ed1d2c788b78a117b4b2da4a8b2cc0b1c47219aa9a3407a	admin	管理员	批量归档申请	admin	管理员	APPROVED	批准归档	2025-12-10 12:02:28.719704	2025-12-10 12:02:14.796964	2025-12-10 12:02:28.719713	0	默认立档单位
1998626383031242754	bf8ebba57c914bbdae086700a563bc01	QZ-2025-30Y-AC04-4C4CBC	40bda8245b6bbe4ba04e5bbe8ba62fb4d0005ff7741c76b203d7b4e0de21322927ddbb015ba48b91938fbbb9998f9bd4332c1f8bca0bf19b37423e0dbaed8f3eb3f968584d7b5f042b89ac5b8edbf52d	admin	管理员	批量归档申请	admin	管理员	APPROVED	批准归档	2025-12-10 13:30:44.121462	2025-12-10 13:30:37.037561	2025-12-10 13:30:44.121475	0	默认立档单位
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
1	1	init base schema	SQL	V1__init_base_schema.sql	434465831	postgres	2025-12-10 09:52:23.000027	29	t
2	2.0.0	init auth	SQL	V2.0.0__init_auth.sql	-52622364	postgres	2025-12-10 09:52:23.047867	14	t
3	3	smart parser tables	SQL	V3__smart_parser_tables.sql	-1210006438	postgres	2025-12-10 09:52:23.067803	11	t
4	4	fix archive and audit columns	SQL	V4__fix_archive_and_audit_columns.sql	1269090204	postgres	2025-12-10 09:52:23.083808	3	t
5	5	ingest request status	SQL	V5__ingest_request_status.sql	-1904538745	postgres	2025-12-10 09:52:23.090994	5	t
6	6	add business modules	SQL	V6__add_business_modules.sql	-1930095926	postgres	2025-12-10 09:52:23.099091	12	t
7	7	add archive approval	SQL	V7__add_archive_approval.sql	1997964789	postgres	2025-12-10 09:52:23.11637	3	t
8	8	add open appraisal	SQL	V8__add_open_appraisal.sql	1916481524	postgres	2025-12-10 09:52:23.123298	4	t
9	9	ensure metadata tables	SQL	V9__ensure_metadata_tables.sql	1360829760	postgres	2025-12-10 09:52:23.130164	1	t
10	10	compliance schema update	SQL	V10__compliance_schema_update.sql	1317807689	postgres	2025-12-10 09:52:23.133375	6	t
11	11	add missing archive columns	SQL	V11__add_missing_archive_columns.sql	-2012880184	postgres	2025-12-10 09:52:23.143398	2	t
12	12	add missing timestamps	SQL	V12__add_missing_timestamps.sql	-119234559	postgres	2025-12-10 09:52:23.149126	13	t
13	15	add convert log table	SQL	V15__add_convert_log_table.sql	1791460354	postgres	2025-12-10 09:52:23.165693	3	t
14	16	add erp config table	SQL	V16__add_erp_config_table.sql	904579297	postgres	2025-12-10 09:52:23.171128	2	t
15	20	compliance enhancement	SQL	V20__compliance_enhancement.sql	-266136302	postgres	2025-12-10 09:52:23.17601	3	t
16	21	add compliance fields	SQL	V21__add_compliance_fields.sql	758204874	postgres	2025-12-10 09:52:23.181709	1	t
17	22	add admin user	SQL	V22__add_admin_user.sql	659711366	postgres	2025-12-10 09:52:23.18578	2	t
18	23	add signature log	SQL	V23__add_signature_log.sql	1720095927	postgres	2025-12-10 09:52:23.190555	4	t
19	24	enhance audit log	SQL	V24__enhance_audit_log.sql	822112610	postgres	2025-12-10 09:52:23.197608	2	t
20	25	add archive summary	SQL	V25__add_archive_summary.sql	-511296424	postgres	2025-12-10 09:52:23.201765	0	t
21	26	ofd convert log	SQL	V26__ofd_convert_log.sql	1575575351	postgres	2025-12-10 09:52:23.204326	3	t
22	27	erp config	SQL	V27__erp_config.sql	1010520792	postgres	2025-12-10 09:52:23.210393	3	t
23	28	add certificate to arc file content	SQL	V28__add_certificate_to_arc_file_content.sql	806664392	postgres	2025-12-10 09:52:23.216372	0	t
24	29	add pre archive status	SQL	V29__add_pre_archive_status.sql	-1890227063	postgres	2025-12-10 09:52:23.218648	3	t
25	30	increase column length for archive submit	SQL	V30__increase_column_length_for_archive_submit.sql	-687052612	postgres	2025-12-10 09:52:23.22402	7	t
26	31	add org name to approval	SQL	V31__add_org_name_to_approval.sql	1237029946	postgres	2025-12-10 09:52:23.234116	0	t
27	32	add business doc no to arc file content	SQL	V32__add_business_doc_no_to_arc_file_content.sql	-1520271041	postgres	2025-12-10 09:52:23.236805	0	t
28	33	create abnormal voucher table	SQL	V33__create_abnormal_voucher_table.sql	-1551042214	postgres	2025-12-10 11:22:09.352443	39	t
29	34	increase archive column lengths for sm4	SQL	V34__increase_archive_column_lengths_for_sm4.sql	-708532401	postgres	2025-12-10 11:46:13.898472	20	t
30	35	add yonsuite salesout tables	SQL	V35__add_yonsuite_salesout_tables.sql	-614900511	postgres	2025-12-11 10:34:33.596461	71	t
31	36	insert seed data	SQL	V36__insert_seed_data.sql	-97412025	postgres	2025-12-11 17:29:45.930851	126	t
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
4429b18e587a4dd995630db9e606cf07	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTMzMTc5MywiZXhwIjoxNzY1NDE4MTkzfQ.Wgw9VHU7g_FBEJTEENgbfojxoth3su8qWv9vszaWXx8","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765331793747}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	\N	28246ca019654d4ca08878d6ce0e74af82e71817141cb735f72a72329e3285e6	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 09:56:33.751616
2e7b6ac9309f40aca7c54c115bbda0f4	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTMzMjM5OSwiZXhwIjoxNzY1NDE4Nzk5fQ.AR8lcjcyN6CFTJPlWlrqmPgglAKCWanQukE9lSUKvjc","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765332399492}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	28246ca019654d4ca08878d6ce0e74af82e71817141cb735f72a72329e3285e6	45e5c5930d4b09a630ae743a990cea96c9c8f05ce0a59f0d43e036fda3b474e1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 10:06:39.493721
8254ad7504bb4cc49c16857eaafaf0dc	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTMzMzQ5MiwiZXhwIjoxNzY1NDE5ODkyfQ.d6UwkjLBrLYwRgOTPGEZyB0w5esKwbdM7loRnrqM3Ds","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765333492013}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	45e5c5930d4b09a630ae743a990cea96c9c8f05ce0a59f0d43e036fda3b474e1	b37d77c46ecddef83b909188a32306b87be4691b1e3862b30bf5a3a3b4afdc17	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 10:24:52.013921
caeb72251df142a9adf9da22ad2f2aa7	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTMzNDYxNywiZXhwIjoxNzY1NDIxMDE3fQ.AZAbITUQAadea1u-9ZegNU04sFWnA1Dir-IP6X5NglA","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765334617654}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	b37d77c46ecddef83b909188a32306b87be4691b1e3862b30bf5a3a3b4afdc17	cfadda30a074d3cd6987c4172e5cd477d660370f34baf355736a8fc6079344ee	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 10:43:37.661629
c31fd17245164ea58d93f103522114ae	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTMzNDg5OSwiZXhwIjoxNzY1NDIxMjk5fQ.g7Xud2d5ZSMEJSI6CAkrnxpLyZyioRVrkjU0yEZjgRA","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765334900604}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	cfadda30a074d3cd6987c4172e5cd477d660370f34baf355736a8fc6079344ee	74ac60dbe2ab4a0f06496baf4f96552fbc260dccd6222012295395cfeebf8d3e	curl/8.7.1|||	2025-12-10 10:48:20.649047
38797e8740074fa39a1e32c1ba3aca4c	user_admin_001	未知用户	\N	METADATA_UPDATE	ARC_FILE_CONTENT	928f28b0-5c4a-4a92-815d-5af67726117f	SUCCESS	low	元数据补录: Automated Test Entry | 修改前:fiscalYear=null, voucherType=null, creator=null, fondsCode=null | 修改后:fiscalYear=2025, voucherType=AC04, creator=Automation Test, fondsCode=null	\N	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	\N	74ac60dbe2ab4a0f06496baf4f96552fbc260dccd6222012295395cfeebf8d3e	752d1f3048db88505ef105b8b8395e8eb03e50e66770a52198c0fb792a249925	\N	2025-12-10 10:51:01.715074
a0c1e7ee5ee249bcae138d07fdf1839c	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	FAILURE	HIGH	批准归档申请	"1998586316413833217"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	752d1f3048db88505ef105b8b8395e8eb03e50e66770a52198c0fb792a249925	3569837e6b3d4f8b1376471e7d8440e7835e29c7c291bf676e9031fb944e9eea	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:41:39.225558
641acce999b04883a996e39254cd6f78	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	FAILURE	HIGH	批准归档申请	"1998586316413833217"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	3569837e6b3d4f8b1376471e7d8440e7835e29c7c291bf676e9031fb944e9eea	6fc94b9cec9034274446918b5fac3fa4216476ffd420eff126cf9d63330bc456	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:48:18.852173
41e9fa5f0aeb4b6298cfa3d600953b40	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	FAILURE	HIGH	批准归档申请	"1998586316413833217"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	6fc94b9cec9034274446918b5fac3fa4216476ffd420eff126cf9d63330bc456	df8b67271aa42fac327bb822a57079eda148565eababb883c9b0af71b90dd773	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:48:47.982521
31e544afb7e8499986246a1e4eb8cbd4	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	FAILURE	HIGH	批准归档申请	"1998586316413833217"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	df8b67271aa42fac327bb822a57079eda148565eababb883c9b0af71b90dd773	9b783360d30029c7dc8a94af1ef45b6a73017fc7a751b089a6c8ae0669673453	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:51:38.332239
f0e87a9eaaa04b0fb480abb10e786c98	user_admin_001	admin	\N	REJECT_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	拒绝归档申请	"1998586316413833217"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	9b783360d30029c7dc8a94af1ef45b6a73017fc7a751b089a6c8ae0669673453	d5d2fb7232b3f6926b7ebbd40e94af0bbdf083f141922aad180499bd3599b0cb	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:52:42.043744
c1cf9ec9e2a84aafa97c2dd3726b7185	user_admin_001	admin	\N	CREATE_APPROVAL	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	创建归档审批申请	{"id":null,"archiveId":"43b6fb802c6942f68a777a7346956789","archiveCode":"QZ-2025-30Y-AC04-A035AA","orgName":"默认立档单位","archiveTitle":"test_upload","applicantId":"admin","applicantName":"管理员","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":null,"approvalComment":null,"approvalTime":null,"createdTime":"2025-12-10T11:52:52.415438","lastModifiedTime":"2025-12-10T11:52:52.41545","deleted":null}	{"id":"1998601784969240577","archiveId":"43b6fb802c6942f68a777a7346956789","archiveCode":"QZ-2025-30Y-AC04-A035AA","orgName":"默认立档单位","archiveTitle":"c1f3f3ce6f784e07bb1eec82872846df","applicantId":"admin","applicantName":"管理员","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":"PENDING","approvalComment":null,"approvalTime":null,"createdTime":"2025-12-10T11:52:52.415438","lastModifiedTime":"2025-12-10T11:52:52.41545","deleted":null}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	d5d2fb7232b3f6926b7ebbd40e94af0bbdf083f141922aad180499bd3599b0cb	3770c7725a132a845b1b84380c5e8e7752a6cd7a05a825d01a3ea783541dc8ec	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:52:52.422609
b0b77ede25e74d19abead336b127f82f	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	FAILURE	HIGH	批准归档申请	"1998601784969240577"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	3770c7725a132a845b1b84380c5e8e7752a6cd7a05a825d01a3ea783541dc8ec	c04d97cdb1c465ea18b75c45a4dadc0863b926223c83ee2ce09a3a9ca49c0324	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:52:58.275918
cea178a633bb490083ab64a8757602bc	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	批准归档申请	"1998601784969240577"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	c04d97cdb1c465ea18b75c45a4dadc0863b926223c83ee2ce09a3a9ca49c0324	bc9ffb1c547fc2fa13650dd7d8f01b20d189af7a3eb918ace123c40091c0b6bc	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 11:59:28.743444
68703642799241d884743c75b51d50fe	user_admin_001	未知用户	\N	METADATA_UPDATE	ARC_FILE_CONTENT	2d91f86d-4f45-44d8-a84b-815efcfff12e	SUCCESS	low	元数据补录: demo | 修改前:fiscalYear=null, voucherType=null, creator=null, fondsCode=null | 修改后:fiscalYear=2025, voucherType=AC04, creator=测试, fondsCode=null	\N	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	\N	bc9ffb1c547fc2fa13650dd7d8f01b20d189af7a3eb918ace123c40091c0b6bc	5cc420bfe85c03a38773927fdc78e88f7cf549473931acc81a6d220d6e26d9d9	\N	2025-12-10 12:02:07.218761
5f8c5000645443cbbc04896cd34cfd9a	user_admin_001	admin	\N	CREATE_APPROVAL	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	创建归档审批申请	{"id":null,"archiveId":"191f1f70b9ca436388d556fc184b7dcf","archiveCode":"QZ-2025-30Y-AC04-551BB2","orgName":"默认立档单位","archiveTitle":"优刻得科技股份有限公司_20251023203642_25512000000251832086","applicantId":"admin","applicantName":"管理员","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":null,"approvalComment":null,"approvalTime":null,"createdTime":"2025-12-10T12:02:14.796964","lastModifiedTime":"2025-12-10T12:02:14.79697","deleted":null}	{"id":"1998604143787053057","archiveId":"191f1f70b9ca436388d556fc184b7dcf","archiveCode":"QZ-2025-30Y-AC04-551BB2","orgName":"默认立档单位","archiveTitle":"3577fb3913942701e38ed6fe4b5d486781f5993df46c86cd57217c477b713506fd83639425997a66abd4d9dc9e09a90f18cd36f93742d0e03ed1d2c788b78a117b4b2da4a8b2cc0b1c47219aa9a3407a","applicantId":"admin","applicantName":"管理员","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":"PENDING","approvalComment":null,"approvalTime":null,"createdTime":"2025-12-10T12:02:14.796964","lastModifiedTime":"2025-12-10T12:02:14.79697","deleted":null}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	5cc420bfe85c03a38773927fdc78e88f7cf549473931acc81a6d220d6e26d9d9	a609b25e74b132b213a63f099b71679737b914a482731e64e529afd75f90ba30	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 12:02:14.807585
fea8159277de42689f92b4ae51cb1cc1	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	批准归档申请	"1998604143787053057"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	a609b25e74b132b213a63f099b71679737b914a482731e64e529afd75f90ba30	5a3bb6bb992ab389a0fb6afac75ad4c4cb899d1ace351f7f5149979d6a538857	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 12:02:28.828967
e689e0ca91d14061b4b88b806d1c0a6b	user_admin_001	未知用户	\N	METADATA_UPDATE	ARC_FILE_CONTENT	581d7c40-9a85-4e89-86c9-304059dba55e	SUCCESS	low	元数据补录: demo | 修改前:fiscalYear=null, voucherType=null, creator=null, fondsCode=null | 修改后:fiscalYear=2025, voucherType=AC04, creator=demo, fondsCode=null	\N	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	\N	5a3bb6bb992ab389a0fb6afac75ad4c4cb899d1ace351f7f5149979d6a538857	232ebcf0ef8901d82e0f411cdecd030940d1f65ad555b0e3b7039c385b1c79d3	\N	2025-12-10 13:28:34.086446
8935bb7bd47c469791741634469a7b37	user_admin_001	admin	\N	CREATE_APPROVAL	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	创建归档审批申请	{"id":null,"archiveId":"bf8ebba57c914bbdae086700a563bc01","archiveCode":"QZ-2025-30Y-AC04-4C4CBC","orgName":"默认立档单位","archiveTitle":"dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428","applicantId":"admin","applicantName":"管理员","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":null,"approvalComment":null,"approvalTime":null,"createdTime":"2025-12-10T13:30:37.037561","lastModifiedTime":"2025-12-10T13:30:37.037568","deleted":null}	{"id":"1998626383031242754","archiveId":"bf8ebba57c914bbdae086700a563bc01","archiveCode":"QZ-2025-30Y-AC04-4C4CBC","orgName":"默认立档单位","archiveTitle":"40bda8245b6bbe4ba04e5bbe8ba62fb4d0005ff7741c76b203d7b4e0de21322927ddbb015ba48b91938fbbb9998f9bd4332c1f8bca0bf19b37423e0dbaed8f3eb3f968584d7b5f042b89ac5b8edbf52d","applicantId":"admin","applicantName":"管理员","applicationReason":"批量归档申请","approverId":null,"approverName":null,"status":"PENDING","approvalComment":null,"approvalTime":null,"createdTime":"2025-12-10T13:30:37.037561","lastModifiedTime":"2025-12-10T13:30:37.037568","deleted":null}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	232ebcf0ef8901d82e0f411cdecd030940d1f65ad555b0e3b7039c385b1c79d3	efc631f4d4dc06f102e80edb644cb2ae21dc37d2014726e3e6f7545fa76155cc	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 13:30:37.057507
32472e84fc06419781723955c3817e1e	user_admin_001	admin	\N	APPROVE_ARCHIVE	ARCHIVE_APPROVAL	\N	SUCCESS	LOW	批准归档申请	"1998626383031242754"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	efc631f4d4dc06f102e80edb644cb2ae21dc37d2014726e3e6f7545fa76155cc	92201c0a5e6f3efded668cf709800b65cdfc8b73b3d23cfaeca2fa12867ceebd	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 13:30:45.15292
9f8810cf37404d7ab4db3bb0f50f8f00	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	FAILURE	HIGH	导出 AIP 包	"QZ-2025-30Y-AC04-E204FA"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	92201c0a5e6f3efded668cf709800b65cdfc8b73b3d23cfaeca2fa12867ceebd	900c8f29bf2e2e507a18a41028628830fc5a32fb234f62b9840163b236716a5b	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 14:04:30.829579
e5f644dba78b49c6b76c296c2a97ea36	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-A035AA"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	900c8f29bf2e2e507a18a41028628830fc5a32fb234f62b9840163b236716a5b	49e0b263bc52e24c77a3b03f14fdb47d453b0707e11c40b6b5717b096f473e99	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 14:36:22.246373
37f8faedb1f549ab866f575f03e284ee	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-A035AA"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	49e0b263bc52e24c77a3b03f14fdb47d453b0707e11c40b6b5717b096f473e99	080127015ba5e83018065880c38d5dcbcf8d2ca058899eb1e9518eb117001a37	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 14:36:46.091868
242aef6072fe4f3eae1724f3e794fe11	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-A035AA"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	080127015ba5e83018065880c38d5dcbcf8d2ca058899eb1e9518eb117001a37	2799beae75a57677bfe23feec86907fd8676861ae5787304a737d464f68b6bb2	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 14:38:29.910406
2ffd5c7695ba44f7b082c2afd9cdb5c0	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM0ODc4MiwiZXhwIjoxNzY1NDM1MTgyfQ.0dvtAHo0Wgx1gAMbl0I4SPtjx63sN7bNGLeINOy0pio","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765348782880}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	2799beae75a57677bfe23feec86907fd8676861ae5787304a737d464f68b6bb2	4f053b0346cc3ceb80b7c960266694ff6bbf3913f5a306a4b9b113fc17c3b0a0	curl/8.7.1|||	2025-12-10 14:39:42.883147
d58918e1b440415289f0c1690509bfd4	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	FAILURE	HIGH	导出 AIP 包	"ARC-AC01-2024-0001"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	4f053b0346cc3ceb80b7c960266694ff6bbf3913f5a306a4b9b113fc17c3b0a0	48f0b44131dedda05b9be8b46747871512b5d132c22eb1e26e9650a232cdc168	curl/8.7.1|||	2025-12-10 14:39:42.908878
1c57c4cb763a4d8b89ff36a260232ce8	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM0ODgyMiwiZXhwIjoxNzY1NDM1MjIyfQ.JwRF2bjlUu4aci_zRHBMjkai5Mjeb9gORHBPcterD6Q","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765348822931}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	48f0b44131dedda05b9be8b46747871512b5d132c22eb1e26e9650a232cdc168	80ee9b183c1d133abe873de88500295b42d52442ccf733a165a19f7cf0a6eef1	curl/8.7.1|||	2025-12-10 14:40:22.932429
a02db1ba6b974e73865bb806437e9fce	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	FAILURE	HIGH	导出 AIP 包	"QZ-2025-30Y-AC04-E204FA"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	80ee9b183c1d133abe873de88500295b42d52442ccf733a165a19f7cf0a6eef1	7bede85e841339712f84890071eca27d3c3e8cc65dccfe508f9f7ce8794173c7	curl/8.7.1|||	2025-12-10 14:40:22.95611
45b4329e3eff4d1a81c2d2daa2354eee	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MDc0MCwiZXhwIjoxNzY1NDM3MTQwfQ.z9UZhEfzhu9c0YxNP1owzLNgDb-i6LzgXKmXZWf8Wtw","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765350740271}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	7bede85e841339712f84890071eca27d3c3e8cc65dccfe508f9f7ce8794173c7	feea7a0c97d23c21a3a92540413b9ec5a8bccd302f67b31b639a4bc2c54ed6aa	curl/8.7.1|||	2025-12-10 15:12:20.273868
5466d07f96ab4981a186511bd41f7526	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MDc1OSwiZXhwIjoxNzY1NDM3MTU5fQ.ghvCU2GnS2KBnbI_q1ei1BtpbtbT89fcmm7EexmYYs4","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765350759056}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	feea7a0c97d23c21a3a92540413b9ec5a8bccd302f67b31b639a4bc2c54ed6aa	66120f5659f42f39e8940d12c78aff68f4fd55f4fc5fe89f68fc77256b4e54d6	curl/8.7.1|||	2025-12-10 15:12:39.057038
2ea818e9ac3240ed8abee625c92c6434	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MDc3NiwiZXhwIjoxNzY1NDM3MTc2fQ.mqgnQvHAJVDYb4ZQ1tm9tbIN6jgNAVkwObBtFgWoHWo","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765350776856}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	66120f5659f42f39e8940d12c78aff68f4fd55f4fc5fe89f68fc77256b4e54d6	72170ee79ba3d8f01b4e5a7d517c454d404f36149a15dd0c2600475d36f481d0	curl/8.7.1|||	2025-12-10 15:12:56.856638
2dfc8e0b7d4343788ce6cb6b75a300ec	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MDc4MSwiZXhwIjoxNzY1NDM3MTgxfQ.56Qa4cwdkCqJ58UZQ8dUsIwzLurbKpIOCdtrsAdadPs","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765350781921}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	72170ee79ba3d8f01b4e5a7d517c454d404f36149a15dd0c2600475d36f481d0	a998523ed938807a04485f07fd07fbf6d43fc48c7066a7e691263d09063092dd	curl/8.7.1|||	2025-12-10 15:13:01.921829
f5f7f06f0bae42a5b5b2493e3f6a34be	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MTEyMCwiZXhwIjoxNzY1NDM3NTIwfQ.a-k2wLgRAom56JGePc_bipi5ssZvRkh1Z3O3zYzl8Eg","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765351121048}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	a998523ed938807a04485f07fd07fbf6d43fc48c7066a7e691263d09063092dd	7346b4512daa976295e28f42dd7286457d9798aa2b6569463718c99947a1526d	curl/8.7.1|||	2025-12-10 15:18:41.051891
8cc7bdce585244b9a23963f4d2e2b2b6	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MTE1MCwiZXhwIjoxNzY1NDM3NTUwfQ.z8MZDmIvWcWv55kluAFIS2hWiwztnnF8Ym5YLQADqjA","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765351150332}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	7346b4512daa976295e28f42dd7286457d9798aa2b6569463718c99947a1526d	0fb5a41753483c098fdd217ba9c685f5254835b39c810df662f067c76e1b9a6e	curl/8.7.1|||	2025-12-10 15:19:10.333287
3adeff9a9f234f8bbc40c8df356755da	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MTQwMSwiZXhwIjoxNzY1NDM3ODAxfQ.xzd7_hXRBtjGc9m8ryEoUa2905fnVPOnsDlJedw9HVs","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765351401092}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	0fb5a41753483c098fdd217ba9c685f5254835b39c810df662f067c76e1b9a6e	35851bdecb56476efd950dadddc71ef07f6adaba35f2e77edf4ef280f4f878f9	curl/8.7.1|||	2025-12-10 15:23:21.09259
2962c99c54624a159382d2ee8bd6dd31	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-4C4CBC"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	35851bdecb56476efd950dadddc71ef07f6adaba35f2e77edf4ef280f4f878f9	6f02c24a7e0851b5c624689003f9d77356664ad0bc0c6681bbb12936940c4d2a	curl/8.7.1|||	2025-12-10 15:23:21.191446
57b222a1117f45be8d130ec122e6e748	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MjAxMiwiZXhwIjoxNzY1NDM4NDEyfQ.xxomyGFLbJRDAeHW3tuY941_qjd2GsWp0_QjniaVBJg","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765352012365}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	6f02c24a7e0851b5c624689003f9d77356664ad0bc0c6681bbb12936940c4d2a	929ea12b75b9511f29cd398974fdee2e52f2d993d6d9239d79ee3bf9b94055a0	curl/8.7.1|||	2025-12-10 15:33:32.366068
1aeb0001df7a4aa299976c718dbb5443	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-4C4CBC"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	929ea12b75b9511f29cd398974fdee2e52f2d993d6d9239d79ee3bf9b94055a0	d8e4049e6602afc607f31447dd51ab13b1f04f07dc02ff6d57fae3920f882966	curl/8.7.1|||	2025-12-10 15:33:32.426905
4d59f23a83044aafb6e3cca6fb807e62	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MjUxNiwiZXhwIjoxNzY1NDM4OTE2fQ.duZuu3wR8FNn_a4_VZhZkwpFcMKpraDmD-bjUq_8VnQ","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765352516640}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	d8e4049e6602afc607f31447dd51ab13b1f04f07dc02ff6d57fae3920f882966	d953c0bafdc5311c354305a4c7cb4a6fa497109f0ea46aca31ae409244d17810	curl/8.7.1|||	2025-12-10 15:41:56.645797
affd5971e06e4f43b25cfde0af3e01f7	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-4C4CBC"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	d953c0bafdc5311c354305a4c7cb4a6fa497109f0ea46aca31ae409244d17810	60b37026270e77e300843e939d313cd37850b1fdb9fe89afde3095ab92ab12a2	curl/8.7.1|||	2025-12-10 15:41:56.79715
1d3f520695e8439bab28ad24f15c87e5	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MjYyNywiZXhwIjoxNzY1NDM5MDI3fQ.01I2dG-GA-PfER7BToqlb6yoqdbaCTnNOb2QFyKpSvE","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765352627979}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	60b37026270e77e300843e939d313cd37850b1fdb9fe89afde3095ab92ab12a2	44bbd61df9a01e14e6d710ca361ad548587485284d8f10262ae08978cfee6ebe	curl/8.7.1|||	2025-12-10 15:43:47.980636
ccf7147ebb0b43aea47d1beeb9eaa1a3	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-4C4CBC"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	44bbd61df9a01e14e6d710ca361ad548587485284d8f10262ae08978cfee6ebe	f40bffd51e158db8c79c555ee6c3ffa804c9c6692b1dcb63a40afa0c5e38a35f	curl/8.7.1|||	2025-12-10 15:43:48.022646
de563c31c7834abba723eda62705e00a	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MjY0MiwiZXhwIjoxNzY1NDM5MDQyfQ.cD-3U-On5URwb7CIs9DI3E0AXRT63KUfOUBj9rAFUMk","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765352642316}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	f40bffd51e158db8c79c555ee6c3ffa804c9c6692b1dcb63a40afa0c5e38a35f	913df48e6f3e522917acded4b3f0f50f2bb0399485711df6f1953860241da983	curl/8.7.1|||	2025-12-10 15:44:02.316964
20ae7b190b6c4098b44ebacecf9dd041	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1MjY2NiwiZXhwIjoxNzY1NDM5MDY2fQ.yoNj45bDJ0GEO2YOw7qR_05v_uJd18JrwXRyKehJAlE","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765352666042}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	913df48e6f3e522917acded4b3f0f50f2bb0399485711df6f1953860241da983	15077682172b058d7e664e009745460b1ca7b01418f7a285479e9888ecb735be	curl/8.7.1|||	2025-12-10 15:44:26.043334
d6601c735ca144a7a756c807f19dfbd7	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM1Mjc1NywiZXhwIjoxNzY1NDM5MTU3fQ.Dd3EImvAsk7z4r1Q8XhxisGclfqmz4qfBXvfzScfAZ0","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765352757409}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	15077682172b058d7e664e009745460b1ca7b01418f7a285479e9888ecb735be	c53ce188a6ce0443e955275dfc7c35a5eb6a2847871ccdaa090fb91bca4ca8fd	curl/8.7.1|||	2025-12-10 15:45:57.410296
645b0b1b6d1f4652a862726cf6e90d38	user_admin_001	admin	\N	EXPORT	ARCHIVE	\N	SUCCESS	LOW	导出 AIP 包	"QZ-2025-30Y-AC04-4C4CBC"	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	c53ce188a6ce0443e955275dfc7c35a5eb6a2847871ccdaa090fb91bca4ca8fd	e1b0b7cb3139e2cbaae07f89f34ef39157e28f0090e0f4014c704fdb43a6c1e3	curl/8.7.1|||	2025-12-10 15:45:57.440741
d5be43368a2140bc811a2b4720e5428d	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM2MjE0MiwiZXhwIjoxNzY1NDQ4NTQyfQ.7nVgxeVcMdxDeszOXdVVRGAgSIbm0w_lWpjZkR5dhzU","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765362142953}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	e1b0b7cb3139e2cbaae07f89f34ef39157e28f0090e0f4014c704fdb43a6c1e3	fdf38d3cf56887b608024cb4ce47ff0df26203d2fa4d064e1b1ab89de8bc7c28	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 18:22:22.957802
b2b351f53ba24c2da1e98c17d1f0aa95	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765362872094}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	fdf38d3cf56887b608024cb4ce47ff0df26203d2fa4d064e1b1ab89de8bc7c28	8493f40868ea98e382e7d2b90ce878075a73e9f4693870ae6abba8665a6a5541	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 18:34:32.097604
852d84e208964b9882d412b04fca101c	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM2Mjg3NywiZXhwIjoxNzY1NDQ5Mjc3fQ.wK-jtYR60D5rT1NKaRRWAfSeDHc1uPvu3SQtV8d5qes","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765362877887}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	8493f40868ea98e382e7d2b90ce878075a73e9f4693870ae6abba8665a6a5541	736b3d69fb548428963525acc756bc44a3b304ef420a7ffc49fbcd2521dee8fa	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 18:34:37.88992
ca31816a7d1c4dee898a772660f18fcc	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765363105050}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	736b3d69fb548428963525acc756bc44a3b304ef420a7ffc49fbcd2521dee8fa	6a48681f5bd26ea9f6c998443f5d782491b79e0762ef09a33642bd0664e69f3e	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 18:38:25.051008
0f84b8b752884fce80fd02db54b6c403	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTM2MzExMCwiZXhwIjoxNzY1NDQ5NTEwfQ.AGKrOqaDXKcKNJBm1uY8Lis98x1DYZdMS467HfGb8jE","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765363110205}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	6a48681f5bd26ea9f6c998443f5d782491b79e0762ef09a33642bd0664e69f3e	d698c3ac5d5c17b0e2381f3d6eecc3cecefee333e43d5d86cb8faae9fadac7f4	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-10 18:38:30.205958
f67fd8f3118b495282981ee22e281ed5	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQxODM3NiwiZXhwIjoxNzY1NTA0Nzc2fQ.H6oO6NaXOQqKHV4J8FQEXs2fLuvljjLli_uEs-9i5tI","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765418376494}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	d698c3ac5d5c17b0e2381f3d6eecc3cecefee333e43d5d86cb8faae9fadac7f4	f24be136a8458513ebf9ac2f26fdd4be6a01c48eb52e38626455c62b5e2ee27d	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 09:59:36.504294
6db31492230146909176aa599002b84e	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQxODU1MCwiZXhwIjoxNzY1NTA0OTUwfQ.gbMB9B7MkK_hxdamvvlVMMNyUGCYnePo9lhp__fpaA8","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765418550059}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	f24be136a8458513ebf9ac2f26fdd4be6a01c48eb52e38626455c62b5e2ee27d	9b2b4026834b9337aa04dbc31223121693812e7d05757be9abea5eada848fcf2	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 10:02:30.061489
dff9bb96ed5b46cf999318c211e4ffc5	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765420034555}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	9b2b4026834b9337aa04dbc31223121693812e7d05757be9abea5eada848fcf2	5a64e72b5035733b6f52ffc1043de038dc82823a8e37a94c52740566526cb198	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 10:27:14.562986
2c42a417825b4941b69c65351949d6be	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQyMDAzOSwiZXhwIjoxNzY1NTA2NDM5fQ.chVGzpW7v6Wlt4foTOnIcalWFj_QrMdMoid9NfTvJ00","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765420039541}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	5a64e72b5035733b6f52ffc1043de038dc82823a8e37a94c52740566526cb198	b514be73494c18279b86f32796a2c8f961f359c556dd36b96a649d915e1a6044	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 10:27:19.548386
6daacb11e5424dd8b49da15c87a3b271	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765420178098}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	b514be73494c18279b86f32796a2c8f961f359c556dd36b96a649d915e1a6044	ef9294096cce8c1a318d054ebd73521c684df1fc23157f6bf16b90b2377a3f87	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 10:29:38.101955
4d16460851154a348815c7fe5986ba38	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQyMDc1MiwiZXhwIjoxNzY1NTA3MTUyfQ.U9McN2F_KEbt6Hnnzbr8rH13Kqf0pIp7lhfg6DEtavQ","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765420752853}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	ef9294096cce8c1a318d054ebd73521c684df1fc23157f6bf16b90b2377a3f87	a8237da46bb37f0dfb40e79aa9f45aad9d4de7836208fe08ba2301785eebdc8d	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 10:39:12.865311
5a00fb8d9b694713bac941b3445d6619	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"nexus_admin_123"}	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	a8237da46bb37f0dfb40e79aa9f45aad9d4de7836208fe08ba2301785eebdc8d	84c2a46aadc8ec766fdde76f12e17b2d10d883f2e65049ddb99608373cd79eca	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 13:53:43.357103
57747212a5444d61be98a0028f3896ac	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"nexus_admin_123"}	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	84c2a46aadc8ec766fdde76f12e17b2d10d883f2e65049ddb99608373cd79eca	fa05249752d0cda016e4523354ab62567c405581d83cc09300298324490cc244	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 13:55:39.825279
dadacc4e94c1478fa58d59f2ee7c37a5	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"nexus_admin_123"}	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	fa05249752d0cda016e4523354ab62567c405581d83cc09300298324490cc244	62cdc6e7dc9a273fe8d20db8941d3393dc7a1ccda4c53b01f32fe45e7a6dbc2e	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 13:56:05.562032
1f26f00c30e24805919d7ad32864f1ee	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"nexus_admin_123"}	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	62cdc6e7dc9a273fe8d20db8941d3393dc7a1ccda4c53b01f32fe45e7a6dbc2e	0f6cb817400f5418da7c3d4217faf01a6909e2de72ef96cc67c18c0a9582ff69	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 13:56:32.450635
20f522aed8b040978ba04428f2392e55	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQzMzcxNSwiZXhwIjoxNzY1NTIwMTE1fQ.EEeZlVKH_GyIJFA8WD0FV7IUsqRjD9e8tEUXbMkQVFU","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765433715692}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	0f6cb817400f5418da7c3d4217faf01a6909e2de72ef96cc67c18c0a9582ff69	bc92613af7ab004eb90eff13feee1edf92a40f6c7b91bbe7eebe50581ab25786	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 14:15:15.695734
eec721e6cf784c84a7a2095383e21019	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765440538335}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	bc92613af7ab004eb90eff13feee1edf92a40f6c7b91bbe7eebe50581ab25786	53e520083afde3b41ec22b51eda0729b08206f6c52a748901d9d9d165ec43ca3	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 16:08:58.341594
ffdf1491c0bd42418963694bacaa9425	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0MDU0NCwiZXhwIjoxNzY1NTI2OTQ0fQ.5zSm2XOAq8YC71gja1Wcwl0UO8RxS9K6XzUWY6iXKIE","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765440544806}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	53e520083afde3b41ec22b51eda0729b08206f6c52a748901d9d9d165ec43ca3	f36af556609c80abbe8148799a51e4bf17b699860a9718ae1bae2123fc487246	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 16:09:04.808213
7d1270e618994a699b968846d7e78867	user_admin_001	admin	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765440835126}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	f36af556609c80abbe8148799a51e4bf17b699860a9718ae1bae2123fc487246	0ed43ae19dbf6a99f3f8554da1f2b323375e8dab8f2fc3a492ddaafcf271376a	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 16:13:55.127971
402f4064d97c4d10b24b605060dd7c5e	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0MDgzOSwiZXhwIjoxNzY1NTI3MjM5fQ.WVBGbXTARZNX_bpxQkuMOKRWZ8vcbwv4XU9oBDWBbjM","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765440839914}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	0ed43ae19dbf6a99f3f8554da1f2b323375e8dab8f2fc3a492ddaafcf271376a	e0adbec7f676308abf60945a77aa8986bf094e47ec65be02f3d76048650a95a4	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 16:13:59.91545
9f1ed39b4cb441f98e6249afbbd51235	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0MjY0MywiZXhwIjoxNzY1NTI5MDQzfQ.8HRwfkVIOqtj6wcQmZsPRY-EFX4zb_ezNyM77b8ql-Q","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765442643463}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	e0adbec7f676308abf60945a77aa8986bf094e47ec65be02f3d76048650a95a4	e75743fa13d65d62275b555c3b63cdc6f7201f48f657bbdd60acf592c155346e	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 16:44:03.464787
71483561fffd4d71afe5ee5d1d6fde01	user_admin_001	admin	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin ","password":"admin123"}	\N	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	e75743fa13d65d62275b555c3b63cdc6f7201f48f657bbdd60acf592c155346e	394b148be8954a1f9698129c9476466ed68bcf56cc12689e04941591f6b987e7	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:32:25.596281
af1223d6a2ea42a3b46abc847a86ad2a	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0NTYxNiwiZXhwIjoxNzY1NTMyMDE2fQ.M53eY1D4ZmOhWFPJzWTfj_YXtratKzgM9QXL96Iu9zk","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765445616650}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	394b148be8954a1f9698129c9476466ed68bcf56cc12689e04941591f6b987e7	3f097e9cd2da72689df4f22408df01e4abdf49b7ea7831659accce08affe3add	curl/8.7.1|||	2025-12-11 17:33:36.654484
399e08f334844dcc94c0742d74124243	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0NTczMiwiZXhwIjoxNzY1NTMyMTMyfQ.uRqDjJbKhCcskwlBGjDZB28ee9By3NqDa6p4bXwm8G8","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765445732646}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	curl/8.7.1	3f097e9cd2da72689df4f22408df01e4abdf49b7ea7831659accce08affe3add	37cd0aa02c5f5ff41db1871a833cc235792586a385244e6ac040eb74d0384c82	curl/8.7.1|||	2025-12-11 17:35:32.647053
d9cb287c4c4f454aab89fc53b6d21878	user_admin_001	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0NTg3OCwiZXhwIjoxNzY1NTMyMjc4fQ.t6xJmb7QPvEjmuOeyQ-BdiTy-M5eM-xXXjSh6Wc3HeM","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765445878556}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	37cd0aa02c5f5ff41db1871a833cc235792586a385244e6ac040eb74d0384c82	310495fa1d02a5627523b21b35cb9fd48750b81cac99c21070d7b89c6a5be3e0	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:37:58.55706
24d72ec018e54fbdb0c7d8b1992105e2	UNKNOWN	anonymousUser	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765446120973}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	310495fa1d02a5627523b21b35cb9fd48750b81cac99c21070d7b89c6a5be3e0	56dcada3e467455efb4b0386a607435c0fc9a945a8c8f113e51d06601ae00a85	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:42:00.975037
9a036169ea324fc389c2ca87273fd18c	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0NjEyOSwiZXhwIjoxNzY1NTMyNTI5fQ.W4VVPhOYfXYpAA8IFvXAjXGfFeWuM5SAn8SgcmJM4Zg","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765446129155}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	56dcada3e467455efb4b0386a607435c0fc9a945a8c8f113e51d06601ae00a85	e6844bdca71bacee155ccb8e694f5141bfadfbd319ea3f86f0bdcd4b26acb9d5	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:42:09.155889
bbac12671b074d0284a276800d5a0bfb	UNKNOWN	anonymousUser	\N	LOGOUT	AUTH	\N	SUCCESS	LOW	用户登出	\N	{"code":200,"message":"登出成功","data":null,"timestamp":1765446439497}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	e6844bdca71bacee155ccb8e694f5141bfadfbd319ea3f86f0bdcd4b26acb9d5	23236397091ae3cefcb9995c9152395fdc642d0ee137ab8b0d2b8d25a2a388bb	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:47:19.500213
094977ce7e434339b5ddf6e070508797	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0NjQ1MCwiZXhwIjoxNzY1NTMyODUwfQ.90ODldszklTHZUyLrJzEvEXTVra0U4buKiDp-G5ZDro","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765446450379}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	23236397091ae3cefcb9995c9152395fdc642d0ee137ab8b0d2b8d25a2a388bb	d6023ff04efd4873e0963c0f8b5f652f05505fd0a7b9fb5c8ff5306bcb9d565c	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:47:30.380692
c3e4b64ee55b4f8384e7f9ea2cb240b6	UNKNOWN	anonymousUser	\N	LOAD_LICENSE	LICENSE	\N	SUCCESS	LOW	加载/切换 License	"{\\"sig\\":\\"iBKRrz+fTuxJe2/IIJv++HTt3IAMbkoOp6mIA50BJVnFcyLe92OmK2yISn5GLrPE1t6pe0gj0pL6EDPn5aCazeXMc8sSIo2EcANIwjrzUNXXXsaZMqdDT70esPCduubymAJKRPRldSXFiuqjQvDDrqwEFjOnGOzG1V4rLU209whf45jZLUy8qy5/iLrjWb/38QigLW5BkqsU8fym821dgXSPJJ4ayb1tqC/EZkSRu8m1lf9DAbAIY4Fu9xne+rAXTIieZCVkXfSQgunEwxrrNdWCHNCRAE3kqj8Myh7g0pkpNRk5R3y32uqcHFU8QUEpEy2hoGY6uwpA/9mDlY9TAA==\\",\\"payload\\":\\"eyJtYXhVc2VycyI6MTAwLCJub2RlTGltaXQiOjUwLCJleHBpcmVBdCI6IjIwMzAtMTItMzEifQ==\\"}"	{"code":200,"message":"License 加载成功","data":{"expireAt":"2030-12-31","maxUsers":100,"nodeLimit":50,"raw":"{\\"sig\\":\\"iBKRrz+fTuxJe2/IIJv++HTt3IAMbkoOp6mIA50BJVnFcyLe92OmK2yISn5GLrPE1t6pe0gj0pL6EDPn5aCazeXMc8sSIo2EcANIwjrzUNXXXsaZMqdDT70esPCduubymAJKRPRldSXFiuqjQvDDrqwEFjOnGOzG1V4rLU209whf45jZLUy8qy5/iLrjWb/38QigLW5BkqsU8fym821dgXSPJJ4ayb1tqC/EZkSRu8m1lf9DAbAIY4Fu9xne+rAXTIieZCVkXfSQgunEwxrrNdWCHNCRAE3kqj8Myh7g0pkpNRk5R3y32uqcHFU8QUEpEy2hoGY6uwpA/9mDlY9TAA==\\",\\"payload\\":\\"eyJtYXhVc2VycyI6MTAwLCJub2RlTGltaXQiOjUwLCJleHBpcmVBdCI6IjIwMzAtMTItMzEifQ==\\"}","expired":false},"timestamp":1765446788583}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	d6023ff04efd4873e0963c0f8b5f652f05505fd0a7b9fb5c8ff5306bcb9d565c	539c399f944dba56a560dc34db5053992755d85531d0c2e1789f796024d60160	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:53:08.587044
ef3320fd932046b09e6736d58b5c608e	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0Njk2OCwiZXhwIjoxNzY1NTMzMzY4fQ.bcgkHK6st58S9X1SYH-Ryp7O3S2a2llzKVb-FqDYMB8","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765446968229}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	539c399f944dba56a560dc34db5053992755d85531d0c2e1789f796024d60160	d0f0868ba64aa4d6dd5d3ae36f811181dc7a9ee679b1367b3b592dd390a3345e	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 17:56:08.230693
6cc385badb504d159c8f64663b49eb8b	UNKNOWN	anonymousUser	\N	LOAD_LICENSE	LICENSE	\N	SUCCESS	LOW	加载/切换 License	"{\\"payload\\":\\"eyJleHBpcmVBdCI6IjIwMzAtMTItMzEiLCJtYXhVc2VycyI6MTAwLCJub2RlTGltaXQiOjUwfQ==\\",\\"sig\\":\\"JdA6qz2It3DjNKlRSdGl5j1Lz1j/wIedM0tQZqGFWXlEi53DSNpctZdr6kSBjcSwTvdBMzgQPWP2r987VfVO05uO7kI1s96B+iEkaTih0R7kiSrwIbvZHiJ7PK/4sVCzFGmwckYVnpSaAy7BZx7jUFKi3743Zm4t5hBnHP9axzENg/L0HA4zwxS7i2QILmuFaT0xUqv0dXxBNXBUbgODPAmY1aTQ2qzw/vAOXBSQyjmwQSsWiqBegvdY537gNuaOonIMR8tpAfjUSxlUGt/OJaEO9kZX0aYmPruluGT8YfScjdFr3r5BJWorSMA2S9gzvkNdOhCI7+Ts/0EizaryEA==\\"}"	{"code":200,"message":"License 加载成功","data":{"expireAt":"2030-12-31","maxUsers":100,"nodeLimit":50,"raw":"{\\"payload\\":\\"eyJleHBpcmVBdCI6IjIwMzAtMTItMzEiLCJtYXhVc2VycyI6MTAwLCJub2RlTGltaXQiOjUwfQ==\\",\\"sig\\":\\"JdA6qz2It3DjNKlRSdGl5j1Lz1j/wIedM0tQZqGFWXlEi53DSNpctZdr6kSBjcSwTvdBMzgQPWP2r987VfVO05uO7kI1s96B+iEkaTih0R7kiSrwIbvZHiJ7PK/4sVCzFGmwckYVnpSaAy7BZx7jUFKi3743Zm4t5hBnHP9axzENg/L0HA4zwxS7i2QILmuFaT0xUqv0dXxBNXBUbgODPAmY1aTQ2qzw/vAOXBSQyjmwQSsWiqBegvdY537gNuaOonIMR8tpAfjUSxlUGt/OJaEO9kZX0aYmPruluGT8YfScjdFr3r5BJWorSMA2S9gzvkNdOhCI7+Ts/0EizaryEA==\\"}","expired":false},"timestamp":1765448502363}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	d0f0868ba64aa4d6dd5d3ae36f811181dc7a9ee679b1367b3b592dd390a3345e	0ace1a4f2e72fccc203a95c25aca99703a56b576c0161159cbcb02c4c3ece117	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 18:21:42.377092
898fc52a1d344ed28bc146e41c6097d4	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluXzAwMSIsInVzZXJuYW1lIjoiYWRtaW4iLCJzdWIiOiJhZG1pbiIsImlhdCI6MTc2NTQ0ODUxMCwiZXhwIjoxNzY1NTM0OTEwfQ.JiwuxJq4yvQVj2b4_BTPqQSusD0tD_Gdh9i0cZpB5Ew","user":{"id":"user_admin_001","username":"admin","fullName":"系统管理员","email":"admin@nexusarchive.local","avatar":null,"departmentId":null,"status":"active","roles":["super_admin"],"permissions":["nav:settings","nav:portal","nav:all","nav:archive_mgmt","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","nav:destruction","nav:pre_archive"]}},"timestamp":1765448510258}	\N	0:0:0:0:0:0:0:1	UNKNOWN	\N	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	0ace1a4f2e72fccc203a95c25aca99703a56b576c0161159cbcb02c4c3ece117	ce90d4648029ea68d25865fd22ba094b22552f22c75e1228749f3e7a6da1d2d7	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|	2025-12-11 18:21:50.260377
\.


--
-- Data for Name: sys_erp_config; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_erp_config (id, name, erp_type, config_json, is_active, created_time, last_modified_time) FROM stdin;
1	YONSUITE_VOUCHER_SYNC	YONSUITE	{"endpoint":"/integration/yonsuite/vouchers/sync","accbookCode":"BR01","frequency":"manual","description":"用友YonSuite会计凭证自动采集接口"}	1	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
2	SAP_VOUCHER_SYNC	GENERIC	{"system":"SAP ERP","frequency":"实时","description":"SAP 财务凭证自动同步接口"}	1	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
3	K3_INVENTORY_SYNC	KINGDEE	{"system":"金蝶云星空","frequency":"每日 23:00","description":"存货核算数据同步"}	1	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
4	OA_EXPENSE_SYNC	GENERIC	{"system":"泛微OA","frequency":"每小时","description":"员工报销单据同步"}	1	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
5	EKB_TRAVEL_SYNC	GENERIC	{"system":"易快报","frequency":"每小时","description":"差旅费用数据同步"}	1	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
6	HLY_REIMBURSE_SYNC	GENERIC	{"system":"汇联易","frequency":"每小时","description":"费用报销同步","status":"error"}	0	2025-12-11 17:29:45.962939	2025-12-11 17:29:45.962939
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
\.


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_role (id, name, code, role_category, is_exclusive, description, permissions, data_scope, type, created_at, updated_at, deleted) FROM stdin;
role_super_admin	超级管理员	super_admin	system_admin	f	\N	["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users"]	self	custom	2025-12-10 09:52:23.187178	2025-12-10 09:52:23.187178	0
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) FROM stdin;
user_admin_001	admin	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	系统管理员	\N	admin@nexusarchive.local	\N	\N	\N	active	2025-12-11 18:21:50.243217	\N	\N	\N	2025-12-10 09:52:23.187178	\N	0
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
-- Name: sys_erp_config_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_erp_config_id_seq', 6, true);


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
-- Name: idx_archive_unique_biz_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_archive_unique_biz_id ON public.acc_archive USING btree (unique_biz_id);


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
-- Name: idx_erp_config_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_erp_config_type ON public.bas_erp_config USING btree (adapter_type);


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
-- Name: idx_sys_org_parent; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_org_parent ON public.sys_org USING btree (parent_id);


--
-- Name: idx_sys_user_role_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_user_role_role ON public.sys_user_role USING btree (role_id);


--
-- Name: idx_sys_user_role_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_sys_user_role_user ON public.sys_user_role USING btree (user_id);


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
-- Name: ys_sales_out_detail ys_sales_out_detail_sales_out_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ys_sales_out_detail
    ADD CONSTRAINT ys_sales_out_detail_sales_out_id_fkey FOREIGN KEY (sales_out_id) REFERENCES public.ys_sales_out(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

