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
-- Name: acc_archive; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.acc_archive (
    id character varying(32) NOT NULL,
    fonds_no character varying(50) NOT NULL,
    archive_code character varying(100) NOT NULL,
    category_code character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    fiscal_year character varying(4) NOT NULL,
    fiscal_period character varying(10),
    retention_period character varying(10) NOT NULL,
    org_name character varying(100) NOT NULL,
    creator character varying(50),
    status character varying(20) DEFAULT 'draft'::character varying,
    standard_metadata jsonb,
    custom_metadata jsonb,
    security_level character varying(20) DEFAULT 'internal'::character varying,
    location character varying(200),
    department_id character varying(32),
    created_by character varying(32),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    unique_biz_id character varying(64),
    amount numeric(18,2),
    doc_date date,
    volume_id character varying(64),
    fixity_value character varying(128),
    fixity_algo character varying(20),
    fonds_id character varying(32),
    paper_ref_link character varying(128),
    destruction_hold boolean DEFAULT false,
    hold_reason character varying(255),
    summary text
);


ALTER TABLE public.acc_archive OWNER TO "user";

--
-- Name: TABLE acc_archive; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.acc_archive IS '电子会计档案表';


--
-- Name: COLUMN acc_archive.fonds_no; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.fonds_no IS 'M9 全宗号';


--
-- Name: COLUMN acc_archive.archive_code; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.archive_code IS 'M13 档号';


--
-- Name: COLUMN acc_archive.category_code; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.category_code IS 'M14 类别号';


--
-- Name: COLUMN acc_archive.title; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.title IS 'M22 题名';


--
-- Name: COLUMN acc_archive.fiscal_year; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.fiscal_year IS 'M11 年度';


--
-- Name: COLUMN acc_archive.fiscal_period; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.fiscal_period IS 'M41 会计月份/期间';


--
-- Name: COLUMN acc_archive.retention_period; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.retention_period IS 'M12 保管期限';


--
-- Name: COLUMN acc_archive.org_name; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.org_name IS 'M6 立档单位名称';


--
-- Name: COLUMN acc_archive.creator; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.creator IS 'M32 责任者/制单人';


--
-- Name: COLUMN acc_archive.standard_metadata; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.standard_metadata IS 'DA/T 94标准元数据(JSON)';


--
-- Name: COLUMN acc_archive.custom_metadata; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.custom_metadata IS '客户自定义元数据(JSON)';


--
-- Name: COLUMN acc_archive.unique_biz_id; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.unique_biz_id IS '唯一业务ID';


--
-- Name: COLUMN acc_archive.amount; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.amount IS '金额';


--
-- Name: COLUMN acc_archive.doc_date; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.doc_date IS '业务日期';


--
-- Name: COLUMN acc_archive.volume_id; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.volume_id IS '所属案卷ID';


--
-- Name: COLUMN acc_archive.fixity_value; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.fixity_value IS '文件哈希值(SM3/SHA256)';


--
-- Name: COLUMN acc_archive.fixity_algo; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.fixity_algo IS '哈希算法: SM3, SHA256';


--
-- Name: COLUMN acc_archive.fonds_id; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.fonds_id IS '所属全宗ID';


--
-- Name: COLUMN acc_archive.paper_ref_link; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.paper_ref_link IS '纸质档案关联号 (物理存放位置)';


--
-- Name: COLUMN acc_archive.destruction_hold; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.destruction_hold IS '销毁留置 (冻结状态)';


--
-- Name: COLUMN acc_archive.hold_reason; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.hold_reason IS '留置/冻结原因 (如: 未结清债权)';


--
-- Name: COLUMN acc_archive.summary; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive.summary IS '档案摘要/说明 - SM4加密存储';


--
-- Name: acc_archive_relation; Type: TABLE; Schema: public; Owner: user
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


ALTER TABLE public.acc_archive_relation OWNER TO "user";

--
-- Name: TABLE acc_archive_relation; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.acc_archive_relation IS '档案关联关系表';


--
-- Name: COLUMN acc_archive_relation.relation_type; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive_relation.relation_type IS 'M93 关系类型: voucher_source/red_dash/attachment/reference/replacement';


--
-- Name: COLUMN acc_archive_relation.relation_desc; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.acc_archive_relation.relation_desc IS 'M95 关系描述';


--
-- Name: acc_archive_volume; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.acc_archive_volume (
    id character varying(32) NOT NULL,
    volume_code character varying(50) NOT NULL,
    archive_year integer NOT NULL,
    retention_period character varying(20) NOT NULL,
    status integer DEFAULT 0,
    validation_report_path character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    custodian_dept character varying(32) DEFAULT 'ACCOUNTING'::character varying
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
-- Name: COLUMN acc_archive_volume.archive_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.archive_year IS '年度';


--
-- Name: COLUMN acc_archive_volume.retention_period; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.retention_period IS '保管期限';


--
-- Name: COLUMN acc_archive_volume.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.status IS '状态: 0=打开, 1=封卷';


--
-- Name: COLUMN acc_archive_volume.validation_report_path; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.validation_report_path IS '四性检测报告路径';


--
-- Name: COLUMN acc_archive_volume.custodian_dept; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.acc_archive_volume.custodian_dept IS '当前保管部门: ACCOUNTING(会计), ARCHIVES(档案)';


--
-- Name: acc_volume; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.acc_volume (
    id character varying(32) NOT NULL,
    volume_code character varying(100) NOT NULL,
    title character varying(500) NOT NULL,
    fonds_no character varying(50),
    fiscal_year character varying(4),
    fiscal_period character varying(7),
    category_code character varying(10),
    file_count integer DEFAULT 0,
    retention_period character varying(20),
    status character varying(20) DEFAULT 'draft'::character varying,
    reviewed_by character varying(32),
    reviewed_at timestamp without time zone,
    archived_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.acc_volume OWNER TO postgres;

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
    duration_ms bigint,
    source_size bigint,
    target_size bigint,
    convert_time timestamp without time zone NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
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
    item_id character varying(64),
    original_hash character varying(128),
    current_hash character varying(128),
    timestamp_token bytea,
    sign_value bytea
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
-- Name: audit_inspection_log; Type: TABLE; Schema: public; Owner: user
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


ALTER TABLE public.audit_inspection_log OWNER TO "user";

--
-- Name: TABLE audit_inspection_log; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.audit_inspection_log IS '四性检测日志表(合规证据)';


--
-- Name: COLUMN audit_inspection_log.inspection_stage; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.inspection_stage IS '检测环节: receive/transfer/patrol/migration';


--
-- Name: COLUMN audit_inspection_log.is_authentic; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.is_authentic IS '真实性(验签)';


--
-- Name: COLUMN audit_inspection_log.is_complete; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.is_complete IS '完整性(哈希)';


--
-- Name: COLUMN audit_inspection_log.is_available; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.is_available IS '可用性(格式)';


--
-- Name: COLUMN audit_inspection_log.is_secure; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.is_secure IS '安全性(病毒)';


--
-- Name: COLUMN audit_inspection_log.report_file_path; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.report_file_path IS '检测报告物理文件路径(XML)';


--
-- Name: COLUMN audit_inspection_log.report_file_hash; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.audit_inspection_log.report_file_hash IS '检测报告文件哈希';


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
-- Name: COLUMN bas_fonds.fonds_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_fonds.fonds_code IS '全宗号 (Fonds Code)';


--
-- Name: COLUMN bas_fonds.fonds_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_fonds.fonds_name IS '全宗名称 (Fonds Name)';


--
-- Name: COLUMN bas_fonds.company_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_fonds.company_name IS '立档单位名称 (Constituting Unit)';


--
-- Name: COLUMN bas_fonds.description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_fonds.description IS '描述';


--
-- Name: COLUMN bas_fonds.created_by; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.bas_fonds.created_by IS '创建人ID';


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
    archive_title character varying(500),
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
    deleted smallint DEFAULT 0 NOT NULL
);


ALTER TABLE public.biz_archive_approval OWNER TO postgres;

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
-- Name: COLUMN sys_archival_code_sequence.fonds_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_archival_code_sequence.fonds_code IS '全宗号';


--
-- Name: COLUMN sys_archival_code_sequence.fiscal_year; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_archival_code_sequence.fiscal_year IS '会计年度';


--
-- Name: COLUMN sys_archival_code_sequence.category_code; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_archival_code_sequence.category_code IS '档案类别 (AC01/AC02...)';


--
-- Name: COLUMN sys_archival_code_sequence.current_val; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.sys_archival_code_sequence.current_val IS '当前流水号';


--
-- Name: sys_audit_log; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_audit_log (
    id character varying(32) NOT NULL,
    user_id character varying(32),
    username character varying(50),
    role_type character varying(20),
    action character varying(50) NOT NULL,
    resource_type character varying(50),
    resource_id character varying(32),
    operation_result character varying(20),
    risk_level character varying(20),
    details text,
    data_before text,
    data_after text,
    session_id character varying(100),
    ip_address character varying(50) NOT NULL,
    user_agent character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    mac_address character varying(64) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    object_digest character varying(128),
    prev_log_hash character varying(64),
    log_hash character varying(64),
    device_fingerprint character varying(200)
);


ALTER TABLE public.sys_audit_log OWNER TO "user";

--
-- Name: TABLE sys_audit_log; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_audit_log IS '安全审计日志表';


--
-- Name: COLUMN sys_audit_log.role_type; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.role_type IS '操作人角色类型';


--
-- Name: COLUMN sys_audit_log.action; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.action IS '操作类型: CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD';


--
-- Name: COLUMN sys_audit_log.operation_result; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.operation_result IS '操作结果: success/fail/denied';


--
-- Name: COLUMN sys_audit_log.risk_level; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.risk_level IS '风险等级: low/medium/high/critical';


--
-- Name: COLUMN sys_audit_log.data_before; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.data_before IS '操作前数据快照';


--
-- Name: COLUMN sys_audit_log.data_after; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.data_after IS '操作后数据快照';


--
-- Name: COLUMN sys_audit_log.ip_address; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.ip_address IS '客户端IP地址(必填)';


--
-- Name: COLUMN sys_audit_log.mac_address; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.mac_address IS 'MAC地址(必填,无法获取时为UNKNOWN)';


--
-- Name: COLUMN sys_audit_log.object_digest; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.object_digest IS '被操作对象的哈希值';


--
-- Name: COLUMN sys_audit_log.prev_log_hash; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.prev_log_hash IS '前一条日志的SM3哈希值';


--
-- Name: COLUMN sys_audit_log.log_hash; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.log_hash IS '当前日志的SM3哈希值';


--
-- Name: COLUMN sys_audit_log.device_fingerprint; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_audit_log.device_fingerprint IS '客户端设备指纹';


--
-- Name: sys_department; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_department (
    id character varying(32) NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50),
    parent_id character varying(32),
    manager_id character varying(32),
    description character varying(500),
    "order" integer DEFAULT 0,
    status character varying(20) DEFAULT 'active'::character varying,
    type character varying(20) DEFAULT 'department'::character varying,
    path character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


ALTER TABLE public.sys_department OWNER TO "user";

--
-- Name: TABLE sys_department; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_department IS '部门表';


--
-- Name: COLUMN sys_department.name; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_department.name IS '部门名称';


--
-- Name: COLUMN sys_department.parent_id; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_department.parent_id IS '上级部门ID';


--
-- Name: COLUMN sys_department.manager_id; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_department.manager_id IS '部门负责人ID';


--
-- Name: COLUMN sys_department.path; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_department.path IS '部门路径 /root/id1/id2';


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
-- Name: sys_position; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_position (
    id character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    code character varying(128) NOT NULL,
    department_id character varying(64),
    description text,
    status character varying(32) DEFAULT 'active'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


ALTER TABLE public.sys_position OWNER TO postgres;

--
-- Name: sys_role; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_role (
    id character varying(32) NOT NULL,
    name character varying(50) NOT NULL,
    code character varying(50) NOT NULL,
    role_category character varying(20) NOT NULL,
    is_exclusive boolean DEFAULT false,
    description character varying(200),
    permissions text,
    data_scope character varying(20) DEFAULT 'self'::character varying,
    type character varying(20) DEFAULT 'custom'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


ALTER TABLE public.sys_role OWNER TO "user";

--
-- Name: TABLE sys_role; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_role IS '系统角色表(三员管理)';


--
-- Name: COLUMN sys_role.role_category; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_role.role_category IS '角色类别: system_admin/security_admin/audit_admin/business_user';


--
-- Name: COLUMN sys_role.is_exclusive; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_role.is_exclusive IS '是否互斥(三员角色)';


--
-- Name: COLUMN sys_role.permissions; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_role.permissions IS 'JSON权限列表';


--
-- Name: COLUMN sys_role.data_scope; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_role.data_scope IS '数据权限范围: all/dept/dept_and_child/self';


--
-- Name: sys_setting; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_setting (
    key character varying(100) NOT NULL,
    value text NOT NULL,
    description character varying(500),
    "group" character varying(50) DEFAULT 'general'::character varying,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_setting OWNER TO "user";

--
-- Name: TABLE sys_setting; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_setting IS '系统设置表';


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_user (
    id character varying(32) NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(200) NOT NULL,
    full_name character varying(50) NOT NULL,
    org_code character varying(50),
    email character varying(100),
    phone character varying(20),
    avatar character varying(500),
    department_id character varying(32),
    status character varying(20) DEFAULT 'active'::character varying,
    last_login_at timestamp without time zone,
    employee_id character varying(50),
    job_title character varying(50),
    join_date character varying(10),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


ALTER TABLE public.sys_user OWNER TO "user";

--
-- Name: TABLE sys_user; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_user IS '系统用户表';


--
-- Name: COLUMN sys_user.full_name; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_user.full_name IS 'M84 机构人员名称 (DA/T 94)';


--
-- Name: COLUMN sys_user.org_code; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_user.org_code IS 'M85 组织机构代码 (DA/T 94)';


--
-- Name: COLUMN sys_user.status; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON COLUMN public.sys_user.status IS '状态: active/disabled/locked';


--
-- Name: sys_user_department; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_user_department (
    user_id character varying(32) NOT NULL,
    department_id character varying(32) NOT NULL,
    is_primary boolean DEFAULT false
);


ALTER TABLE public.sys_user_department OWNER TO "user";

--
-- Name: TABLE sys_user_department; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_user_department IS '用户部门关联表';


--
-- Name: sys_user_role; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.sys_user_role (
    user_id character varying(32) NOT NULL,
    role_id character varying(32) NOT NULL
);


ALTER TABLE public.sys_user_role OWNER TO "user";

--
-- Name: TABLE sys_user_role; Type: COMMENT; Schema: public; Owner: user
--

COMMENT ON TABLE public.sys_user_role IS '用户角色关联表';


--
-- Data for Name: acc_archive; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.acc_archive (id, fonds_no, archive_code, category_code, title, fiscal_year, fiscal_period, retention_period, org_name, creator, status, standard_metadata, custom_metadata, security_level, location, department_id, created_by, created_at, updated_at, deleted, unique_biz_id, amount, doc_date, volume_id, fixity_value, fixity_algo, fonds_id, paper_ref_link, destruction_hold, hold_reason, summary) FROM stdin;
d4bcb7c3583d41759606c9863f2984eb	BR01	YS-2025-09-记-1	AC01	会计凭证-记-1	2025	2025-09	10Y	泊冉演示集团	李珍珍	archived	\N	\N	internal	\N	\N	system	2025-12-04 16:48:57.02387	2025-12-04 18:15:53.700696	0	YonSuite_2322790126007353354	-3.00	2025-09-17	8c33bb96f29244d5ae6e0a934db111a3	\N	\N	\N	\N	f	\N	\N
d078f1d10b004f95aa9992d066c14545	BR01	YS-2025-09-记-2	AC01	会计凭证-记-2	2025	2025-09	10Y	泊冉演示集团	李珍珍	archived	\N	\N	internal	\N	\N	system	2025-12-04 16:48:57.03054	2025-12-04 18:15:53.700696	0	YonSuite_2322790194726830091	15.00	2025-09-17	8c33bb96f29244d5ae6e0a934db111a3	\N	\N	\N	\N	f	\N	\N
7d99f5fd81ef4bf4a8dff9744eb8b47d	BR01	YS-2025-08-记-4	AC01	会计凭证-记-4	2025	2025-08	10Y	泊冉演示集团	王心尹	draft	\N	[{"id": "2320437652155531288", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 50.0, "voucherid": "2320437652155531287", "accsubject": {"id": "2319002806690775056", "code": "112201", "name": "应收账款_贷款", "cashCategory": "Other"}, "credit_org": 0.0, "description": "确认应收", "recordnumber": 1, "debit_original": 50.0, "credit_original": 0.0}, {"id": "2320437652155531289", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652155531287", "accsubject": {"id": "2319002806690775143", "code": "2221010601", "name": "应交税费_应交增值税_销项税额_销项", "cashCategory": "Other"}, "credit_org": 5.75, "description": "应交销项税", "recordnumber": 2, "debit_original": 0.0, "credit_original": 5.75}, {"id": "2320437652155531290", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652155531287", "accsubject": {"id": "2319002806690775232", "code": "600101", "name": "主营业务收入_贷款", "cashCategory": "Other"}, "credit_org": 44.25, "description": "收入", "recordnumber": 3, "debit_original": 0.0, "credit_original": 44.25}]	internal	\N	\N	system	2025-12-04 16:48:57.015888	2025-12-05 23:15:28.183314	0	YonSuite_2320437652155531287	50.00	2025-08-09	9ea8f602a9e340db984932cd3c0365db	\N	\N	\N	\N	f	\N	\N
43fa25605ec948578265f23d61e832fd	BR01	YS-2025-08-记-2	AC01	会计凭证-记-2	2025	2025-08	10Y	泊冉演示集团	王心尹	archived	\N	[{"id": "2320392357430427653", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 150.0, "voucherid": "2320392357430427652", "accsubject": {"id": "2319002806690775056", "code": "112201", "name": "应收账款_贷款", "cashCategory": "Other"}, "credit_org": 0.0, "description": "确认应收", "recordnumber": 1, "debit_original": 150.0, "credit_original": 0.0}, {"id": "2320392357430427654", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320392357430427652", "accsubject": {"id": "2319002806690775143", "code": "2221010601", "name": "应交税费_应交增值税_销项税额_销项", "cashCategory": "Other"}, "credit_org": 17.26, "description": "应交销项税", "recordnumber": 2, "debit_original": 0.0, "credit_original": 17.26}, {"id": "2320392357430427655", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320392357430427652", "accsubject": {"id": "2319002806690775232", "code": "600101", "name": "主营业务收入_贷款", "cashCategory": "Other"}, "credit_org": 132.74, "description": "收入", "recordnumber": 3, "debit_original": 0.0, "credit_original": 132.74}]	internal	\N	\N	system	2025-12-04 16:48:57.004148	2025-12-05 23:15:28.172831	0	YonSuite_2320392357430427652	150.00	2025-08-01	9ea8f602a9e340db984932cd3c0365db	\N	\N	\N	\N	f	\N	\N
a9493c26a9254161bb4fc88dee0afdbe	BR01	YS-2025-08-记-3	AC01	会计凭证-记-3	2025	2025-08	10Y	泊冉演示集团	王心尹	draft	\N	[{"id": "2320437652170211341", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 30.0, "voucherid": "2320437652170211340", "accsubject": {"id": "2319002806690775239", "code": "6401", "name": "主营业务成本", "cashCategory": "Other"}, "credit_org": 0.0, "description": "销售出库核算", "recordnumber": 1, "debit_original": 30.0, "credit_original": 0.0}, {"id": "2320437652170211342", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652170211340", "accsubject": {"id": "2319002806690775072", "code": "1405", "name": "库存商品", "cashCategory": "Other"}, "credit_org": 30.0, "description": "销售出库核算", "recordnumber": 2, "debit_original": 0.0, "credit_original": 30.0}]	internal	\N	\N	system	2025-12-04 16:48:57.009816	2025-12-05 23:15:28.178371	0	YonSuite_2320437652170211340	30.00	2025-08-09	9ea8f602a9e340db984932cd3c0365db	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-001	COMP001	COMP001-2023-10Y-FIN-AC01-V0051	AC01	付款凭证-1002 银行存款	2023	11	10Y	总公司	\N	archived	\N	{"subject": "1002 银行存款", "pageCount": 2}	INTERNAL	\N	\N	user_admin	2023-11-03 09:00:00	2025-12-06 08:38:23.296164	0	\N	45200.00	2023-11-03	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-002	COMP001	COMP001-2023-10Y-FIN-AC01-V0052	AC01	收款凭证-5001 主营业务收入	2023	11	10Y	分公司A	\N	archived	\N	{"subject": "5001 主营业务收入", "pageCount": 2}	INTERNAL	\N	\N	user_admin	2023-11-02 10:30:00	2025-12-06 08:38:23.296164	0	\N	125000.00	2023-11-02	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-003	COMP001	COMP001-2023-10Y-FIN-AC01-V0053	AC01	转账凭证-6001 主营业务成本	2023	11	10Y	总公司	\N	archived	\N	{"subject": "6001 主营业务成本", "pageCount": 2}	INTERNAL	\N	\N	user_admin	2023-11-01 14:20:00	2025-12-06 08:38:23.296164	0	\N	28500.00	2023-11-01	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-004	COMP001	COMP001-2023-10Y-FIN-AC01-V0098	AC01	收款凭证-2001 短期借款	2023	10	10Y	分公司B	\N	archived	\N	{"subject": "2001 短期借款"}	INTERNAL	\N	\N	user_admin	2023-10-28 11:15:00	2025-12-06 08:38:23.296164	0	\N	500000.00	2023-10-28	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-005	COMP001	COMP001-2023-10Y-FIN-AC01-V0095	AC01	付款凭证-1001 库存现金	2023	10	10Y	总公司	\N	archived	\N	{"subject": "1001 库存现金"}	INTERNAL	\N	\N	user_admin	2023-10-25 16:45:00	2025-12-06 08:38:23.296164	0	\N	5600.00	2023-10-25	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-006	COMP001	COMP001-2023-30Y-FIN-AC02-L0001	AC02	2023年度总账	2023	全年	30Y	总公司	\N	archived	\N	{"subject": "总账", "pageCount": 120}	INTERNAL	\N	\N	user_admin	2023-12-31 09:00:00	2025-12-06 08:38:23.296164	0	\N	\N	2023-12-31	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-007	COMP001	COMP001-2023-30Y-FIN-AC02-L0002	AC02	2023年现金日记账	2023	全年	30Y	分公司A	\N	archived	\N	{"subject": "现金日记账", "pageCount": 36}	INTERNAL	\N	\N	user_admin	2023-12-31 09:30:00	2025-12-06 08:38:23.296164	0	\N	\N	2023-12-31	\N	\N	\N	\N	\N	f	\N	\N
1800000000000000088	COMP001	COMP001-2023-10Y-FIN-AC01-V0088	AC01	2023年11月报销凭证	2023	2023-11	10Y	Nexus Corp	Demo User	archived	\N	\N	INTERNAL	\N	\N	\N	2025-12-04 11:22:23.963444	2025-12-04 11:22:23.963444	0	\N	\N	\N	\N	\N	\N	\N	\N	f	\N	\N
896d5044d151484ea8186554876cb52b	BR01	YS-2025-08-记-1	AC01	会计凭证-记-1	2025	2025-08	10Y	泊冉演示集团	王心尹	archived	\N	[{"id": "2320437652155531281", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 150.0, "voucherid": "2320437652155531280", "accsubject": {"id": "2319002806690775239", "code": "6401", "name": "主营业务成本", "cashCategory": "Other"}, "credit_org": 0.0, "description": "内部交易出库核算", "recordnumber": 1, "debit_original": 150.0, "credit_original": 0.0}, {"id": "2320437652155531282", "currency": {"id": "1759241668886790176", "code": "CNY", "name": "人民币"}, "debit_org": 0.0, "voucherid": "2320437652155531280", "accsubject": {"id": "2319002806690775072", "code": "1405", "name": "库存商品", "cashCategory": "Other"}, "credit_org": 150.0, "description": "内部交易出库核算", "recordnumber": 2, "debit_original": 0.0, "credit_original": 150.0}]	internal	\N	\N	system	2025-12-04 16:48:56.994739	2025-12-05 23:15:28.157955	0	YonSuite_2320437652155531280	150.00	2025-08-01	9ea8f602a9e340db984932cd3c0365db	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-008	COMP001	COMP001-2023-PERM-FIN-AC03-R0001	AC03	2023年度财务决算报告	2023	2023	PERM	总公司	\N	archived	\N	{"pageCount": 80, "reportType": "年度报告", "totalAssets": 12000000}	INTERNAL	\N	\N	user_admin	2024-03-31 09:00:00	2025-12-06 08:38:23.296164	0	\N	\N	2024-03-31	\N	\N	\N	\N	\N	f	\N	\N
DEMO-ARC-009	COMP001	COMP001-2023-Q1-FIN-AC03-R0002	AC03	2023年第一季度财务报告	2023	Q1	PERM	分公司A	\N	archived	\N	{"revenue": 3500000, "pageCount": 35, "reportType": "季度报告"}	INTERNAL	\N	\N	user_admin	2023-04-15 14:00:00	2025-12-06 08:38:23.296164	0	\N	\N	2023-04-15	\N	\N	\N	\N	\N	f	\N	\N
\.


--
-- Data for Name: acc_archive_relation; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.acc_archive_relation (id, source_id, target_id, relation_type, relation_desc, created_by, created_at, deleted) FROM stdin;
\.


--
-- Data for Name: acc_archive_volume; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_archive_volume (id, volume_code, archive_year, retention_period, status, validation_report_path, created_at, updated_at, deleted, custodian_dept) FROM stdin;
\.


--
-- Data for Name: acc_volume; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.acc_volume (id, volume_code, title, fonds_no, fiscal_year, fiscal_period, category_code, file_count, retention_period, status, reviewed_by, reviewed_at, archived_at, created_at, updated_at) FROM stdin;
9ea8f602a9e340db984932cd3c0365db	BR01-AC01-202508	泊冉演示集团2025年08月会计凭证	BR01	2025	2025-08	AC01	4	10Y	archived	admin	2025-12-04 17:09:15.287579	2025-12-04 17:09:15.287579	2025-12-04 17:08:39.004796	2025-12-04 17:09:15.287579
8c33bb96f29244d5ae6e0a934db111a3	BR01-AC01-202509	泊冉演示集团2025年09月会计凭证	BR01	2025	2025-09	AC01	2	10Y	archived	admin	2025-12-04 18:15:53.700696	2025-12-04 18:15:53.700696	2025-12-04 17:08:47.269523	2025-12-04 18:15:53.700696
\.


--
-- Data for Name: arc_convert_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_convert_log (id, archive_id, source_format, target_format, source_path, target_path, status, error_message, duration_ms, source_size, target_size, convert_time, created_time) FROM stdin;
\.


--
-- Data for Name: arc_file_content; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_file_content (id, archival_code, file_name, file_type, file_size, file_hash, hash_algorithm, storage_path, created_time, item_id, original_hash, current_hash, timestamp_token, sign_value) FROM stdin;
test-file-001	TEMP-POOL-20251202-TEST001	test_document.pdf	PDF	102400	abc123def456	SHA-256	/tmp/nexusarchive/uploads/test_document.pdf	2025-12-02 17:49:48.074299	\N	abc123def456	\N	\N	\N
fd779266-c5b8-4a2c-a53a-87c01a76bcd1	TEMP-POOL-20251202-FD779266	dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf	PDF	104331	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	SM3	/tmp/nexusarchive/uploads/fd779266-c5b8-4a2c-a53a-87c01a76bcd1.pdf	2025-12-02 18:05:05.880916	\N	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	\N	\N	\N
d4c744d8-bc59-4297-b8b7-978b5f807112	TEMP-POOL-20251202-D4C744D8	凭证_20251202_1000.pdf	PDF	40371	DEMO_HASH_d4c744d8	SHA-256	/tmp/nexusarchive/uploads/d4c744d8-bc59-4297-b8b7-978b5f807112.pdf	2025-12-02 17:39:57.752196	\N	DEMO_HASH_d4c744d8	\N	\N	\N
df481122-3e05-4a75-85bb-0a15bf25aef5	TEMP-POOL-20251202-DF481122	凭证_20251202_1001.pdf	PDF	40371	DEMO_HASH_df481122	SHA-256	/tmp/nexusarchive/uploads/df481122-3e05-4a75-85bb-0a15bf25aef5.pdf	2025-12-02 17:44:57.756984	\N	DEMO_HASH_df481122	\N	\N	\N
11b6341b-f40a-467a-9435-58c3da32281f	TEMP-POOL-20251202-11B6341B	凭证_20251202_1002.pdf	PDF	40371	DEMO_HASH_11b6341b	SHA-256	/tmp/nexusarchive/uploads/11b6341b-f40a-467a-9435-58c3da32281f.pdf	2025-12-02 17:42:57.758445	\N	DEMO_HASH_11b6341b	\N	\N	\N
a8076b84-28ad-4692-88be-8b4a983cdc03	TEMP-POOL-20251202-A8076B84	凭证_20251202_1003.pdf	PDF	40371	DEMO_HASH_a8076b84	SHA-256	/tmp/nexusarchive/uploads/a8076b84-28ad-4692-88be-8b4a983cdc03.pdf	2025-12-02 18:18:57.760693	\N	DEMO_HASH_a8076b84	\N	\N	\N
62b1a199-ebfd-4d4e-94e8-46b55a379e32	TEMP-POOL-20251202-62B1A199	凭证_20251202_1004.pdf	PDF	40371	DEMO_HASH_62b1a199	SHA-256	/tmp/nexusarchive/uploads/62b1a199-ebfd-4d4e-94e8-46b55a379e32.pdf	2025-12-02 18:29:57.765037	\N	DEMO_HASH_62b1a199	\N	\N	\N
9746290e-3324-4bcb-8e54-671e82ae233b	TEMP-POOL-20251202-9746290E	凭证_20251202_1005.pdf	PDF	40371	DEMO_HASH_9746290e	SHA-256	/tmp/nexusarchive/uploads/9746290e-3324-4bcb-8e54-671e82ae233b.pdf	2025-12-02 17:55:57.768539	\N	DEMO_HASH_9746290e	\N	\N	\N
c720f1fe-14fe-4a8a-a925-4b9bece09d8f	TEMP-POOL-20251202-C720F1FE	凭证_20251202_1006.pdf	PDF	40371	DEMO_HASH_c720f1fe	SHA-256	/tmp/nexusarchive/uploads/c720f1fe-14fe-4a8a-a925-4b9bece09d8f.pdf	2025-12-02 17:48:57.771322	\N	DEMO_HASH_c720f1fe	\N	\N	\N
37c72710-842f-46f3-8c33-96396ff7cecc	TEMP-POOL-20251202-37C72710	凭证_20251202_1007.pdf	PDF	40371	DEMO_HASH_37c72710	SHA-256	/tmp/nexusarchive/uploads/37c72710-842f-46f3-8c33-96396ff7cecc.pdf	2025-12-02 18:29:57.772931	\N	DEMO_HASH_37c72710	\N	\N	\N
3fc9d653-46b1-48b0-9432-f6002c5d2a93	TEMP-POOL-20251202-3FC9D653	凭证_20251202_1008.pdf	PDF	40371	DEMO_HASH_3fc9d653	SHA-256	/tmp/nexusarchive/uploads/3fc9d653-46b1-48b0-9432-f6002c5d2a93.pdf	2025-12-02 17:33:57.774608	\N	DEMO_HASH_3fc9d653	\N	\N	\N
c47a5a74-f70a-4798-9ffa-9371595f5c51	TEMP-POOL-20251202-C47A5A74	凭证_20251202_1009.pdf	PDF	40371	DEMO_HASH_c47a5a74	SHA-256	/tmp/nexusarchive/uploads/c47a5a74-f70a-4798-9ffa-9371595f5c51.pdf	2025-12-02 17:44:57.776178	\N	DEMO_HASH_c47a5a74	\N	\N	\N
a2e2b7df-b0f4-43eb-b702-cfcc29b0446d	TEMP-POOL-20251203-A2E2B7DF	dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf	PDF	104331	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	SM3	/tmp/nexusarchive/uploads/a2e2b7df-b0f4-43eb-b702-cfcc29b0446d.pdf	2025-12-03 10:57:21.920452	\N	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	\N	\N	\N
802cbfb8-7752-4715-bc02-54af6dfae17a	TEMP-POOL-20251203-802CBFB8	dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf	PDF	104331	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	SM3	/tmp/nexusarchive/uploads/802cbfb8-7752-4715-bc02-54af6dfae17a.pdf	2025-12-03 10:57:55.122503	\N	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	\N	\N	\N
52d6aa01-daa5-4407-9f17-59de998c7f88	TEMP-POOL-20251203-52D6AA01	uploaded_image_1764730699017.png	PNG	98788	b7b37acfceed74bfbef8122f1e382313559659d20509719d48eb516925a27032	SM3	/tmp/nexusarchive/uploads/52d6aa01-daa5-4407-9f17-59de998c7f88.png	2025-12-03 10:59:06.58574	\N	b7b37acfceed74bfbef8122f1e382313559659d20509719d48eb516925a27032	\N	\N	\N
d40a0b3b-c71a-4b71-a68c-16c63923237e	TEMP-POOL-20251203-D40A0B3B	dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf	PDF	104331	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	SM3	/tmp/nexusarchive/uploads/d40a0b3b-c71a-4b71-a68c-16c63923237e.pdf	2025-12-03 11:07:09.700349	\N	1165696cfe9c6cdaa1410e2c472e4c90d3812cd5169acedd8eabda5416c99194	\N	\N	\N
9a3bb2ec-bb21-4452-a217-9e2ddc40247d	TEMP-POOL-20251203-9A3BB2EC	uploaded_image_1764731325354.png	PNG	23830	7d6b2deb694df24897871aeacf0658350df7d65a5df4fd43c197b05a44001928	SM3	/tmp/nexusarchive/uploads/9a3bb2ec-bb21-4452-a217-9e2ddc40247d.png	2025-12-03 11:10:16.7173	\N	7d6b2deb694df24897871aeacf0658350df7d65a5df4fd43c197b05a44001928	\N	\N	\N
cfc15d3f-285b-4a44-9d59-c15c2469ae71	TEMP-POOL-20251203-CFC15D3F	uploaded_image_1764731325354.png	PNG	23830	7d6b2deb694df24897871aeacf0658350df7d65a5df4fd43c197b05a44001928	SM3	/tmp/nexusarchive/uploads/cfc15d3f-285b-4a44-9d59-c15c2469ae71.png	2025-12-03 11:11:43.28171	\N	7d6b2deb694df24897871aeacf0658350df7d65a5df4fd43c197b05a44001928	\N	\N	\N
ee9d859c-3fdb-4bea-b717-a4178e8a7954	TEMP-POOL-20251203-EE9D859C	uploaded_image_1764731325354.png	PNG	23830	7d6b2deb694df24897871aeacf0658350df7d65a5df4fd43c197b05a44001928	SM3	/tmp/nexusarchive/uploads/ee9d859c-3fdb-4bea-b717-a4178e8a7954.png	2025-12-03 11:12:58.28397	\N	7d6b2deb694df24897871aeacf0658350df7d65a5df4fd43c197b05a44001928	\N	\N	\N
abe4d09f-cbbb-4415-a0b6-755c2032b2e0	TEMP-POOL-20251203-ABE4D09F	fapiao1203.png	PNG	288872	ee0b12918a61e0a1f995309624b03a3a6507d700925f8c65e49b97fd1228795e	SM3	/tmp/nexusarchive/uploads/abe4d09f-cbbb-4415-a0b6-755c2032b2e0.png	2025-12-03 11:21:37.389706	\N	ee0b12918a61e0a1f995309624b03a3a6507d700925f8c65e49b97fd1228795e	\N	\N	\N
f20f16f1-5a6f-4f19-a858-ceeabc514386	TEMP-POOL-20251203-F20F16F1	uploaded_image_1764732123186.png	PNG	80714	ec20fa8feb8f15e79ce1716474ebf48c9e64b5035d8e2f72cc593b90fbf24e9c	SM3	/tmp/nexusarchive/uploads/f20f16f1-5a6f-4f19-a858-ceeabc514386.png	2025-12-03 11:25:10.266665	\N	ec20fa8feb8f15e79ce1716474ebf48c9e64b5035d8e2f72cc593b90fbf24e9c	\N	\N	\N
7b8251e6-1d63-4954-9fe3-fa626dddb00d	TEMP-POOL-20251203-7B8251E6	fapiao1203.png	PNG	288872	ee0b12918a61e0a1f995309624b03a3a6507d700925f8c65e49b97fd1228795e	SM3	/tmp/nexusarchive/uploads/7b8251e6-1d63-4954-9fe3-fa626dddb00d.png	2025-12-03 11:31:17.774977	\N	ee0b12918a61e0a1f995309624b03a3a6507d700925f8c65e49b97fd1228795e	\N	\N	\N
0fdf3fa0-2165-47d3-b94b-8abef52c9d0d	TEMP-POOL-20251203-0FDF3FA0	uploaded_image_1764732946267.png	PNG	160480	daebaeb537fd863ba107bb8db22a6e2e36f433bf020a62ef5c5abc9b48723840	SM3	/tmp/nexusarchive/uploads/0fdf3fa0-2165-47d3-b94b-8abef52c9d0d.png	2025-12-03 11:36:02.114186	\N	daebaeb537fd863ba107bb8db22a6e2e36f433bf020a62ef5c5abc9b48723840	\N	\N	\N
a6c7aafb-9537-41c6-bf79-e76604490a81	TEMP-POOL-20251203-A6C7AAFB	uploaded_image_1764732946267.png	PNG	160480	daebaeb537fd863ba107bb8db22a6e2e36f433bf020a62ef5c5abc9b48723840	SM3	/tmp/nexusarchive/uploads/a6c7aafb-9537-41c6-bf79-e76604490a81.png	2025-12-03 11:37:44.917444	\N	daebaeb537fd863ba107bb8db22a6e2e36f433bf020a62ef5c5abc9b48723840	\N	\N	\N
c88d9dff-e18b-49ca-8619-7f95c50b6d54	TEMP-POOL-20251203-C88D9DFF	uploaded_image_1764732946267.png	PNG	160480	daebaeb537fd863ba107bb8db22a6e2e36f433bf020a62ef5c5abc9b48723840	SM3	/tmp/nexusarchive/uploads/c88d9dff-e18b-49ca-8619-7f95c50b6d54.png	2025-12-03 11:41:11.277853	\N	daebaeb537fd863ba107bb8db22a6e2e36f433bf020a62ef5c5abc9b48723840	\N	\N	\N
1e2d3e6b-0b65-4de9-8635-cfab1736fc3b	TEMP-POOL-20251203-1E2D3E6B	fapiao1203.png	PNG	288872	ee0b12918a61e0a1f995309624b03a3a6507d700925f8c65e49b97fd1228795e	SM3	/tmp/nexusarchive/uploads/1e2d3e6b-0b65-4de9-8635-cfab1736fc3b.png	2025-12-03 11:44:55.829561	\N	ee0b12918a61e0a1f995309624b03a3a6507d700925f8c65e49b97fd1228795e	\N	\N	\N
FILE-001	COMP001-2023-10Y-FIN-AC01-V0051	aliyun_invoice.xml	XML	1024	hash1	SM3	/tmp/aliyun.xml	2025-12-04 09:50:11.233883	\N	hash1	\N	\N	\N
FILE-002	COMP001-2023-10Y-FIN-AC01-V0052	jd_invoice.pdf	PDF	2048	hash2	SM3	/tmp/jd.pdf	2025-12-04 09:50:11.233883	\N	hash2	\N	\N	\N
1800000000000000089	COMP001-2023-10Y-FIN-AC01-V0088	voucher_v0088.ofd	ofd	10240	dummy_hash_1	SHA-256	/tmp/archives/COMP001/2023/10Y/AC01/COMP001-2023-10Y-FIN-AC01-V0088/content/voucher_v0088.ofd	2025-12-04 11:22:23.973336	\N	dummy_hash_1	\N	\N	\N
1800000000000000090	COMP001-2023-10Y-FIN-AC01-V0088	contract_2023_001.pdf	pdf	20480	dummy_hash_2	SHA-256	/tmp/archives/COMP001/2023/10Y/AC01/COMP001-2023-10Y-FIN-AC01-V0088/content/contract_2023_001.pdf	2025-12-04 11:22:23.975299	\N	dummy_hash_2	\N	\N	\N
1800000000000000091	COMP001-2023-10Y-FIN-AC01-V0088	bank_slip_001.jpg	jpg	5120	dummy_hash_3	SHA-256	/tmp/archives/COMP001/2023/10Y/AC01/COMP001-2023-10Y-FIN-AC01-V0088/content/bank_slip_001.jpg	2025-12-04 11:22:23.975536	\N	dummy_hash_3	\N	\N	\N
\.


--
-- Data for Name: arc_file_metadata_index; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_file_metadata_index (id, file_id, invoice_code, invoice_number, total_amount, seller_name, issue_date, parsed_time, parser_type) FROM stdin;
META-001	FILE-001	011002200111	12345678	12800.00	阿里云计算有限公司	2023-11-01	2025-12-04 09:50:11.229551	XML_V1
META-002	FILE-002	033002200333	87654321	5000.00	京东商城	2023-11-02	2025-12-04 09:50:11.229551	PDF_REGEX
\.


--
-- Data for Name: arc_signature_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.arc_signature_log (id, archive_id, file_id, signer_name, signer_cert_sn, signer_org, sign_time, sign_algorithm, signature_value, verify_result, verify_time, verify_message, created_time) FROM stdin;
\.


--
-- Data for Name: audit_inspection_log; Type: TABLE DATA; Schema: public; Owner: user
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
\.


--
-- Data for Name: bas_location; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bas_location (id, name, code, type, parent_id, path, capacity, used_count, status, rfid_tag, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: biz_archive_approval; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.biz_archive_approval (id, archive_id, archive_code, archive_title, applicant_id, applicant_name, application_reason, approver_id, approver_name, status, approval_comment, approval_time, created_at, updated_at, deleted) FROM stdin;
AA-2024-001	ARCH-2024-001	QZ-2024-KJ-001	2024年1月记账凭证	user001	张三	完成四性检测，申请正式归档	\N	\N	PENDING	\N	\N	2025-11-26 17:26:04.86296	2025-12-06 08:38:23.29056	0
AA-2024-002	ARCH-2024-002	QZ-2024-KJ-002	2024年2月记账凭证	user002	李四	凭证已完成关联，申请归档	\N	\N	PENDING	\N	\N	2025-11-26 17:26:04.86296	2025-12-06 08:38:23.29056	0
AA-2024-003	ARCH-2024-003	QZ-2024-BB-001	2024年第一季度财务报告	user003	王五	季度报告已审核，申请归档	\N	\N	PENDING	\N	\N	2025-11-26 17:26:04.86296	2025-12-06 08:38:23.29056	0
AA-2024-004	ARCH-2024-004	QZ-2024-KJ-003	2024年3月记账凭证	user004	赵六	月度凭证整理完成，申请归档	\N	\N	PENDING	\N	\N	2025-11-26 17:26:04.86296	2025-12-06 08:38:23.29056	0
AA-2024-005	ARCH-2024-005	QZ-2024-ZB-001	2024年总账（1-3月）	user002	李四	季度总账，申请归档	\N	\N	PENDING	\N	\N	2025-11-26 17:26:04.86296	2025-12-06 08:38:23.29056	0
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
OA-2014-001	ARCH-2014-001	QZ-2014-KJ-001	2014年1月记账凭证	10Y	INTERNAL	\N	\N	\N	\N	\N	\N	PENDING	2025-11-26 17:26:04.867076	2025-12-06 08:38:23.29469	0
OA-2014-002	ARCH-2014-002	QZ-2014-KJ-002	2014年2月记账凭证	10Y	INTERNAL	\N	\N	\N	\N	\N	\N	PENDING	2025-11-26 17:26:04.867076	2025-12-06 08:38:23.29469	0
OA-2014-003	ARCH-2014-003	QZ-2014-BB-001	2014年第一季度财务报告	10Y	INTERNAL	\N	\N	\N	\N	\N	\N	PENDING	2025-11-26 17:26:04.867076	2025-12-06 08:38:23.29469	0
OA-2013-004	ARCH-2013-004	QZ-2013-HT-001	2013年设备采购合同	10Y	INTERNAL	\N	\N	\N	\N	\N	\N	PENDING	2025-11-26 17:26:04.867076	2025-12-06 08:38:23.29469	0
OA-2013-005	ARCH-2013-005	QZ-2013-KJ-012	2013年12月记账凭证	10Y	INTERNAL	\N	\N	\N	\N	\N	\N	PENDING	2025-11-26 17:26:04.867076	2025-12-06 08:38:23.29469	0
OA-2013-006	ARCH-2013-006	QZ-2013-BB-004	2013年度财务决算报告	10Y	SECRET	\N	\N	\N	\N	\N	\N	PENDING	2025-11-26 17:26:04.867076	2025-12-06 08:38:23.29469	0
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	postgres	2025-12-05 13:10:04.396411	0	t
2	2.0.0	init auth	SQL	V2.0.0__init_auth.sql	-52622364	postgres	2025-12-05 13:10:04.459721	24	t
3	3	smart parser tables	SQL	V3__smart_parser_tables.sql	-1210006438	postgres	2025-12-05 13:10:04.513271	29	t
4	4	fix archive and audit columns	SQL	V4__fix_archive_and_audit_columns.sql	-1455544849	postgres	2025-12-05 13:30:44.94887	1182	t
5	5	ingest request status	SQL	V5__ingest_request_status.sql	-1904538745	postgres	2025-12-05 13:30:46.17319	122	t
6	6	add business modules	SQL	V6__add_business_modules.sql	-1930095926	postgres	2025-12-05 13:30:46.304839	55	t
7	7	add archive approval	SQL	V7__add_archive_approval.sql	1997964789	postgres	2025-12-05 13:34:32.928559	20	t
8	8	add open appraisal	SQL	V8__add_open_appraisal.sql	1916481524	postgres	2025-12-05 13:34:32.990227	44	t
9	9	ensure metadata tables	SQL	V9__ensure_metadata_tables.sql	1360829760	postgres	2025-12-05 13:34:33.171192	6	t
10	10	compliance schema update	SQL	V10__compliance_schema_update.sql	1929945299	postgres	2025-12-05 13:34:33.197418	42	t
11	11	add missing archive columns	SQL	V11__add_missing_archive_columns.sql	-2012880184	postgres	2025-12-05 13:34:33.255091	14	t
12	12	add missing timestamps	SQL	V12__add_missing_timestamps.sql	-119234559	postgres	2025-12-06 07:50:45.938296	96	t
15	20	compliance enhancement	SQL	V20__compliance_enhancement.sql	-266136302	postgres	2025-12-06 12:23:03.599102	6	t
16	21	add compliance fields	SQL	V21__add_compliance_fields.sql	758204874	postgres	2025-12-06 20:26:45.692538	19	t
17	22	add admin user	SQL	V22__add_admin_user.sql	659711366	postgres	2025-12-06 20:45:39.535454	24	t
18	23	add signature log	SQL	V23__add_signature_log.sql	1720095927	postgres	2025-12-07 11:57:52.029001	35	t
19	24	enhance audit log	SQL	V24__enhance_audit_log.sql	822112610	postgres	2025-12-07 11:57:52.079428	6	t
20	25	add archive summary	SQL	V25__add_archive_summary.sql	-511296424	postgres	2025-12-07 11:57:52.092434	1	t
21	26	ofd convert log	SQL	V26__ofd_convert_log.sql	-1702961857	postgres	2025-12-07 13:05:22.881633	16	t
22	27	erp config	SQL	V27__erp_config.sql	1010520792	postgres	2025-12-07 13:05:22.915525	8	t
\.


--
-- Data for Name: sys_archival_code_sequence; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_archival_code_sequence (fonds_code, fiscal_year, category_code, current_val, updated_time) FROM stdin;
\.


--
-- Data for Name: sys_audit_log; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_audit_log (id, user_id, username, role_type, action, resource_type, resource_id, operation_result, risk_level, details, data_before, data_after, session_id, ip_address, user_agent, created_at, mac_address, object_digest, prev_log_hash, log_hash, device_fingerprint) FROM stdin;
df9888d3f6a94260bcc98c3d3ba00d07	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"admin123"}	\N	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-07 13:06:49.563622	UNKNOWN	\N	\N	ab4b89019c3e7b634d50c5b6200ca1f7bb19663d88c7d3171ddcfb3c9d36460b	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
700998fa62dd4487a7cc5a1dbf4885fe	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"admin123"}	\N	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-07 13:06:50.696023	UNKNOWN	\N	ab4b89019c3e7b634d50c5b6200ca1f7bb19663d88c7d3171ddcfb3c9d36460b	ccda57d688f13cc21d7f2555ed0e17a3d55c1dae55ca86898f0d6517612e18c0	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
81808b241c3840fea2c7df4465925782	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"admin123"}	\N	\N	0:0:0:0:0:0:0:1	curl/8.7.1	2025-12-07 13:07:09.441356	UNKNOWN	\N	ccda57d688f13cc21d7f2555ed0e17a3d55c1dae55ca86898f0d6517612e18c0	70cbc82813d8203a1c6c657311b3a2ce4a1394f1b226eb7344c20e333de46548	curl/8.7.1|||
5b91d394cc964ed5a68405e37f53ef9a	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	FAILURE	HIGH	用户登录	{"username":"admin","password":"admin123"}	\N	\N	0:0:0:0:0:0:0:1	curl/8.7.1	2025-12-07 13:10:44.133554	UNKNOWN	\N	70cbc82813d8203a1c6c657311b3a2ce4a1394f1b226eb7344c20e333de46548	cf291111e10606ecb90af636bccf7387792605819ff05027f458615d3de25c20	curl/8.7.1|||
48e50d185b84494f9290a95b27b5e49a	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0MzcyLCJleHAiOjE3NjUxNzA3NzJ9.cZsAzK1vhSYzHf8LdhOKbH3Kx2ajRYXiJw5Uc-eiziU","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084372149}	\N	0:0:0:0:0:0:0:1	curl/8.7.1	2025-12-07 13:12:52.157793	UNKNOWN	\N	cf291111e10606ecb90af636bccf7387792605819ff05027f458615d3de25c20	a7cf294e976bfb20a737615ea53b3e8861c76672c9a6812136cf2b405f6fd405	curl/8.7.1|||
ad6b757dd5b54d89a7905eda9a1ec417	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0NDIyLCJleHAiOjE3NjUxNzA4MjJ9.R5R7AOKfHagdGCdLzTMHa4_4PwvWoh5xuMOjH9qHh0I","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084422549}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-07 13:13:42.549782	UNKNOWN	\N	a7cf294e976bfb20a737615ea53b3e8861c76672c9a6812136cf2b405f6fd405	14126df66b4674b46a1d85fc6e950fab28bc10d13059ea3d00efae1b6cb00557	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
28a069ce1f37485593f7d986d3d44450	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0NDMyLCJleHAiOjE3NjUxNzA4MzJ9.A5PffrWOaIII4f1Ozi_sl0saUALZRqTs4KBeDgP4HfY","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084432542}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-07 13:13:52.542796	UNKNOWN	\N	14126df66b4674b46a1d85fc6e950fab28bc10d13059ea3d00efae1b6cb00557	1b35f94cf6f67eaba93b958000f77d5b7fe1438cf0a190ae050a9dcdd9bf8182	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
71024851ba8c477aa45afe21392474bb	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0NDY1LCJleHAiOjE3NjUxNzA4NjV9.yAVCq6E1fbPfagfqgp-V_FpnZBwSJITHZ3GNU-vnda0","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084465322}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	2025-12-07 13:14:25.323135	UNKNOWN	\N	1b35f94cf6f67eaba93b958000f77d5b7fe1438cf0a190ae050a9dcdd9bf8182	c3b6972d05670ce4dac78b427ba5c313d60e0c7176fb2a7bbc6bc48646b73f86	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
724635602e974e6998f3ff5631a520fe	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0NTQzLCJleHAiOjE3NjUxNzA5NDN9.eswrA9rSr75D6CJNcTGXgSnB_O9nIT9Di2U2hma5HBs","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084543833}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	2025-12-07 13:15:43.834518	UNKNOWN	\N	c3b6972d05670ce4dac78b427ba5c313d60e0c7176fb2a7bbc6bc48646b73f86	ba2d5c5b9a46468536c8216ca0e4075898430c2d51a007172bf715d0187e96f3	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
31291448c3cf4d23bafce2ec0935a7c3	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0NjM0LCJleHAiOjE3NjUxNzEwMzR9.rCJUHD70CC5Xc8gnrpUQ13VephSbuttROTzrnfYYKqs","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084634288}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	2025-12-07 13:17:14.288868	UNKNOWN	\N	ba2d5c5b9a46468536c8216ca0e4075898430c2d51a007172bf715d0187e96f3	b3d66cb58b1177e9caf3174e2271cc8f152bf4e998d8804417657493dbf9a452	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
461d3f539de149fab32560abaa465adb	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg0NjgwLCJleHAiOjE3NjUxNzEwODB9.KPTP933xecMybqd0yGb-CWMJCh8jQ6z4tuUbddntwjU","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765084680072}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36	2025-12-07 13:18:00.073	UNKNOWN	\N	b3d66cb58b1177e9caf3174e2271cc8f152bf4e998d8804417657493dbf9a452	f207a4ad490e7cffff15c9a31ff0763679cc8c1dac700a7a58efb879cb823910	Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
0c03fb52f1f84c6981d7845319d1d537	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg1MTAwLCJleHAiOjE3NjUxNzE1MDB9.NkeArjLSWE-FCUoCNQyNBMyd4WsMIaDxFOFSm2UjTKM","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765085100371}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	2025-12-07 13:25:00.372736	UNKNOWN	\N	f207a4ad490e7cffff15c9a31ff0763679cc8c1dac700a7a58efb879cb823910	c8a5de9c6e984c88cd3428f5b6ed782f12b1ba700059da7f9ff61e234ced5f96	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
f82e9435bdc248c2b46fe3b28a6a7639	user_admin	admin	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg1NTg3LCJleHAiOjE3NjUxNzE5ODd9.-Nz9oFrwk576dDtk5fJDJin7BfXkZzAYCflPF8Frg4E","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765085587755}	\N	0:0:0:0:0:0:0:1	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36	2025-12-07 13:33:07.765539	UNKNOWN	\N	c8a5de9c6e984c88cd3428f5b6ed782f12b1ba700059da7f9ff61e234ced5f96	6b188d4d2d92f934c6297423416ef9840528b5f18ba6584e74d1789ac1a17872	Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36|zh-CN,zh;q=0.9|gzip, deflate, br, zstd|
8545dcc80bb3429b9e55cd07d5b1fd26	UNKNOWN	anonymousUser	\N	LOGIN	AUTH	\N	SUCCESS	LOW	用户登录	{"username":"admin","password":"admin123"}	{"code":200,"message":"登录成功","data":{"token":"eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiJ1c2VyX2FkbWluIiwidXNlcm5hbWUiOiJhZG1pbiIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzY1MDg1NjA5LCJleHAiOjE3NjUxNzIwMDl9.uhQmvP9hOUzbwIhUALxKh6cDUgYKU1IxC84fYfD2dzk","user":{"id":"user_admin","username":"admin","fullName":"系统管理员","email":null,"avatar":null,"departmentId":null,"status":"active","roles":["SYSTEM_ADMIN","super_admin"],"permissions":["nav:settings","nav:portal","nav:all","manage_settings","nav:archive_mgmt","manage_roles","nav:collection","nav:query","nav:panorama","nav:borrowing","nav:stats","nav:warehouse","manage_users","system_admin","manage_org","nav:destruction","nav:pre_archive"]}},"timestamp":1765085609852}	\N	0:0:0:0:0:0:0:1	curl/8.7.1	2025-12-07 13:33:29.853641	UNKNOWN	\N	6b188d4d2d92f934c6297423416ef9840528b5f18ba6584e74d1789ac1a17872	0c19a6ed81406d80ae62759cd72b30f48b187baccea57c982ad34641922a0f36	curl/8.7.1|||
\.


--
-- Data for Name: sys_department; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_department (id, name, code, parent_id, manager_id, description, "order", status, type, path, created_at, updated_at, deleted) FROM stdin;
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
-- Data for Name: sys_position; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_position (id, name, code, department_id, description, status, created_at, updated_at, deleted) FROM stdin;
\.


--
-- Data for Name: sys_role; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_role (id, name, code, role_category, is_exclusive, description, permissions, data_scope, type, created_at, updated_at, deleted) FROM stdin;
role_security_admin	安全保密员	SECURITY_ADMIN	security_admin	t	负责权限管理和密钥管理	["manage_roles","manage_users"]	all	system	2025-11-21 18:24:42.384587	2025-11-21 18:24:42.384587	0
role_audit_admin	安全审计员	AUDIT_ADMIN	audit_admin	t	负责查看和审计系统日志	["audit_logs","view_dashboard"]	all	system	2025-11-21 18:24:42.384587	2025-11-21 18:24:42.384587	0
role_archivist	档案员	ARCHIVIST	business_user	f	负责档案管理和归档	["view_archives","manage_archives","borrow_archives"]	dept	system	2025-11-21 18:24:42.384587	2025-11-21 18:24:42.384587	0
1992237898015150082	Updated Role Name	role_test_1763821503509	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1991848186641268738	Updated Role Name	role_test_1763728589089	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1991848597519532034	Updated Role Name	role_test_1763728687055	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1991848910427176961	Updated Role Name	role_test_1763728761636	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992238169608941569	Updated Role Name	role_test_1763821568258	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1991852505348341762	Updated Role Name	role_test_1763729618739	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1991852698877706241	Updated Role Name	role_test_1763729664888	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1993576555511398401	Updated Role Name	role_test_1764140664339	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1991853542096400386	Updated Role Name	role_test_1763729865928	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992238418553352194	Updated Role Name	role_test_1763821627633	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992072542812991490	Updated Role Name	role_test_1763782079746	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992078711430397953	Updated Role Name	role_test_1763783550456	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
role_system_admin	系统管理员	SYSTEM_ADMIN	system_admin	t	负责系统运维和配置管理	["manage_org","manage_users","manage_roles","manage_settings","nav:all"]	all	system	2025-11-21 18:24:42.384587	2025-11-21 18:24:42.384587	0
1992081147121455105	Updated Role Name	role_test_1763784131084	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992239255585423361	Updated Role Name	role_test_1763821827186	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992082447695425537	Updated Role Name	role_test_1763784441198	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992143549537353729	Updated Role Name	role_test_1763799009065	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
fb259c264baa409e94c760105344b996	系统管理员	role_system_admin	SYSTEM	f	\N	["nav:all"]	self	custom	2025-11-22 11:52:28.516259	\N	0
1992144945036820482	Updated Role Name	role_test_1763799341688	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992239554882617346	Updated Role Name	role_test_1763821898554	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992146091373707266	Updated Role Name	role_test_1763799615085	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992146470253559810	Updated Role Name	role_test_1763799705433	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992239753730367490	Updated Role Name	role_test_1763821945965	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992240136011812866	Updated Role Name	role_test_1763822037109	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
role_super_admin	超级管理员	super_admin	system_admin	f	\N	["nav:portal","nav:panorama","nav:pre_archive","nav:collection","nav:archive_mgmt","nav:query","nav:borrowing","nav:destruction","nav:warehouse","nav:stats","nav:settings","nav:all","system_admin","manage_users"]	self	custom	2025-12-06 20:45:39.543521	2025-12-06 20:45:39.543521	0
1992383282540666881	Updated Role Name	role_test_1763856165898	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992383443044106242	Updated Role Name	role_test_1763856204156	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
1992383705859203074	Updated Role Name	role_test_1763856266825	BUSINESS_USER	f	Test Description	\N	self	custom	\N	\N	1
\.


--
-- Data for Name: sys_setting; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_setting (key, value, description, "group", updated_at) FROM stdin;
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_user (id, username, password_hash, full_name, org_code, email, phone, avatar, department_id, status, last_login_at, employee_id, job_title, join_date, created_at, updated_at, deleted) FROM stdin;
e40215f239344e35bdf2b426851a3015	testuser_1763799007861	$argon2id$v=19$m=65536,t=3,p=4$qcnU7VSW/0zZ++yvUPrTeQWiR/nplcMdJEx8Mg1C8cE$puuzKa0f6VMpRQ9pclvqeOZlujntQ85vkrZ6QnY47GYvlFhtUvqMsiyzKxVAZlxlIVtddO5Q+RUQAN1K1iwOXw	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 16:10:08.165305	2025-11-22 16:10:08.525558	0
8bd94eee5a4e46efbb2c522f3dd0a728	testuser_1763729865613	$argon2id$v=19$m=65536,t=3,p=4$BM6c3itaBBZTTiRQjYFpyoVIPSEYi4jNjhiXl7nb6zU$kaVWTW2cQ4NTIJMRX83mm20ZgtyicWs+NNFowlUcDix9DY711HPU6zobXx3X8pITzNqktlWwLT1oin1a+B+lYQ	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-21 20:57:45.739013	2025-11-21 20:57:45.808151	0
554d386ec3b141568b40f12995284e3f	testuser_1763821567800	$argon2id$v=19$m=65536,t=3,p=4$qRoVDzxhvcabt76mKRo+kO1ZDi2HeDIxtTAUsfMDlFs$FayKlXRj9zZFVWOAabGT+xT9E53NyAvzobM/qLTJx4EGMbV0EMK+GJsg/cGvgz1Fbdr4iPyunrZpwB3a5BcKNw	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:26:07.893736	2025-11-22 22:26:08.036077	0
bdec23ae0d00478483df8f65e3fb27aa	testuser_1763799703156	$argon2id$v=19$m=65536,t=3,p=4$iGn0JxdPnyYFz+cgFK7rcRqtmcbwB0pVCCK9wKlvob4$X85Fl39EIwPzTSdExMmLN2KEW7ZjpmHffUNpyUu15jc9a2ZbzcZtQVVfwLMQ/33oTTjyTaABYGzJ+EP24l3m2g	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 16:21:43.930709	2025-11-22 16:21:44.691139	0
ecb38e2075fc4bc9a52e1471b4b40ea8	testuser_1763782076337	$argon2id$v=19$m=65536,t=3,p=4$WaG0qkw74MQslQLN8Nnr8BmzTW/4bdGjJV7EjtgAPrU$hYz4CBVXPsQ77Q6R3aJ2dXszPzpg+PMziezy3chume/38Tv1IMTTl9pE+8k/CADa8hfye7PQAwcZlbk09BzU7A	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 11:27:57.32427	2025-11-22 11:27:57.324299	0
e35d6d2c2c074ef3bff7fdfb94db7b56	testuser_1763782508358	$argon2id$v=19$m=65536,t=3,p=4$kAwARP2sXiBSkgRQWhLxI7zSCGJcXzdHxoHknlkFfgo$PRpXnniLDvXaRwztAGY9KnByYjf9JLIyzucxyUvaAtalXUpu2ISu2JFNMVIAf/s5869JKoaZTNrSMAnHi9q+Gw	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 11:35:09.432384	2025-11-22 11:35:09.432423	0
820dd10691b741bc98feba569582d687	testuser_1763799341249	$argon2id$v=19$m=65536,t=3,p=4$eRqAnTor0k3q5col/ThoO2v2glRy7frBX16Wpw4qAr4$7T8g66J8LEtov0fWg8VNEAb9AWgJ4Sh3liL0TTWQXpMEnJDM8JJ10Q5DIi0FWUNefW43pyyIW7eEY1sXG/ZeoQ	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 16:15:41.381542	2025-11-22 16:15:41.495744	0
8260b3dfd4644240af1287b3e42e2a6e	testuser_1763728761364	$argon2id$v=19$m=65536,t=3,p=4$CYi0GovUQd7CD6wVriVxLf5Eqi41anMQqLjqEwRDlfA$dInmGamaKyzDqCeWSLpP8XVBceJZdv0rK4sbHL3EIV2HFoU9lspyEyMErkw43SCVNn/PNn+A5GomYTbyaCPNzA	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-21 20:39:21.457255	2025-11-21 20:39:21.508678	0
38a62ebc769448b1ba6ec799e33d19bb	testuser_1763783549158	$argon2id$v=19$m=65536,t=3,p=4$uSVFf5dqyhgyOYrUeBh/qG4dPJ5cQW7HcJLEDp7UaJ0$EeYJTmeUjpCtwZzUERnX+ZKMeuZfKFMXN7vWR1KBIbb5kj4ZYBt0Q7gbZ9qFeDVyAMLXYrS1Otg7Wyh8TAg4pA	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 11:52:29.505376	2025-11-22 11:52:29.505435	0
bb88c8eee5c0414d9a925e281d64e5b5	testuser_1763821826873	$argon2id$v=19$m=65536,t=3,p=4$u2rAQY/vxJciVSRDK1TACHOdrZl3ppbmQb1VXYLKVEY$hNMX5C45vk+jTsp8J9XAIQvMVtl7ALKlCSypX5vULGLBxC2BGDr6xhgJNobAsUzRF1Dr3dwszS6n4ic2BGaUzg	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:30:26.948159	2025-11-22 22:30:27.040521	0
17dc369153e94352b5997c43b199d584	testuser_1763729618458	$argon2id$v=19$m=65536,t=3,p=4$ptqazG3lyowbUxHw1yRSBNtovo0yU3l15OGB3ZKckCs$malxYZH/NF+Fhklg/Y08q+C/QCKST+Q/s6bavUpn/joYitzfsd9MGpGt+vIf1VebyNZ2vI4a8hIYg61nH30UbA	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-21 20:53:38.568349	2025-11-21 20:53:38.624806	0
a36ebf52922749e1833a3b4f3ec15730	testuser_1763799614304	$argon2id$v=19$m=65536,t=3,p=4$D6s7jFcYvS0EBVM/VuhXl9/qHyutfZhWLUBZWhgjSCc$iOlb1R6FGGrgBZ+jl5iqyn3jF27pL3bjeSjVehRWkdHdijJe1y4cAiVlEYUBcL7xlI+8fQXee1IpD55gKkTvEg	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 16:20:14.58904	2025-11-22 16:20:14.759378	0
7b8c3f4627814223a87adc36e05cf855	testuser_1763729664410	$argon2id$v=19$m=65536,t=3,p=4$NRz5CKcU9H5Ebz7XEgA+fEqnoYn633FjbpSiFnLwWwg$LCbgmMqABacU9+jHLjprcOCG/6IfcjykidiBW9jDblfNznr9zwF6n1/M+8gRnnan6ysD5p2cyKQk0AomoHsY3A	Test User	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-21 20:54:24.664197	2025-11-21 20:54:24.746863	0
67d3dfb49ab2496b9563e46c16bcdafe	testuser_1763821503073	$argon2id$v=19$m=65536,t=3,p=4$Lx8ZqUIRnKL53xGE0QHDUYU/T1+EkN7+6qtwguLTmPw$KhRUzxLdm2T1e7XkiWM2SRWBsnTfJ3GfGr3AGlpMYhC0vwBKY8ybNlgyZ3295Cwo0Yx5a8zbmR/kIvqqUoxbOg	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:25:03.190421	2025-11-22 22:25:03.369372	0
fa6c6c7b9b864116ae81051291cf3bb3	testuser_1763821627432	$argon2id$v=19$m=65536,t=3,p=4$YFArfjh2qM3V4RyH5MGKxqckvzrm2hY8bdvirkqsmkE$irp2TUBsvmFhNZqo8fsoXvpSxe1ze1hlerEoQXxyg0SuHtUD4/RBP4zBNCLBnA83lsEPEPYPv/UQF7QdFmWulQ	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:27:07.498012	2025-11-22 22:27:07.559422	0
eb93a7ec533f4cb28a8121f24c2f5961	testuser_1763822036946	$argon2id$v=19$m=65536,t=3,p=4$7O/G6svjixcEBcZ3UrafSohmuOXEbuyjHqPriyfsQsI$D9hSRKlqLISXJUREqDa6DR4a2FsraU7cD3C1D0TFdKeBheC8qd2HxPX+T7K9M3Hat7s6+pRqwDm3ddkpMrxF9A	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:33:56.998666	2025-11-22 22:33:57.041914	0
9237db44761c486a9741a7d4a35a175e	testuser_1763821945801	$argon2id$v=19$m=65536,t=3,p=4$KdbE3Os6HgMpws9+2b5eElWT23sEXTFGmhXByieXrWw$zB//j5HU2TdrJ5APOeGVV54H5spXMzE36LpoIiqGfB1ouLfASxUVtdFH/1zG0kBondOe1bXNmYvzwDfZvDDFlw	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:32:25.855285	2025-11-22 22:32:25.890668	0
621c4cdb03644490860ba755ab77f2d8	testuser_1763856165697	$argon2id$v=19$m=65536,t=3,p=4$1OrLYZFR1lXulp9ACeMhjm80ohyxvC2PmbdZRAyV6KU$wUcjpU3nRUUt76/5H4A1MqB4DIFCe4kPvC9w9HEpNOOKMWSU9AlXO3TlEA6F1Oq+GwbuIqsfALVcmbZ2QuLEJQ	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-23 08:02:45.77404	2025-11-23 08:02:45.824181	0
1986669fecc6484aa9e9e027087df606	testuser_1763821898392	$argon2id$v=19$m=65536,t=3,p=4$s3xZDCzaa8zC5rYWLNjea1MKjlmE2/SvAWu2hKMa72A$w6At7Q2gKtfwIPrIcU9TWq3NcM+UhBOIUfZlkDIbcnv/Snx6CAr9f7Wy7Q0n51suJGUwXWraa6bEogDl6/hX5g	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-22 22:31:38.454243	2025-11-22 22:31:38.487698	0
system	system	$2a$10$dummyhash	系统	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-12-04 16:48:49.345939	2025-12-04 16:48:49.345939	0
9215e8918a644b7da13510b07ae20be4	testuser_1763856203897	$argon2id$v=19$m=65536,t=3,p=4$hKJV6kYTXtKJpTJbtlvi7U/Wx2peUe7mo5h8jnsMmvA$QXmlVY3lc6wotwUwQv9U4UjI8+COwKVlrKbhfi68+mheaIr0FIFQSgJXCEgPH9QGk+sRTyrzdshFqU9SM6IRSQ	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-23 08:03:23.97105	2025-11-23 08:03:24.037644	0
9681f75302b249699f76da59030fd5fe	testuser_1763856266605	$argon2id$v=19$m=65536,t=3,p=4$feOZt9EafBCEjtOepE+byQIK45iVZ8sqvUj0owZfbIM$MaKDEOq5f6QVJ0dal+VO9wmy37X8PzcLGEYro2DNyCuaf1/2KFWcYj7PffPRTZe+wDIYo0KgRZtygly1NRivhQ	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-23 08:04:26.669755	2025-11-23 08:04:26.738826	0
561c445ebc1d429e9db20014a1fc83af	testuser_1764140664001	$argon2id$v=19$m=65536,t=3,p=4$LPgiSP1bCNqoK/lGKh+S85AV8u/5HmOt2KTmXE4HtmA$pnzLqiDAEVIyy5/UG75Fgxn0T1olxnHMhyVeP98DDH0YoG52i4Rz4wXFSFI/sxFMnUMUlmkvvweGEhpjbMC24g	Updated Name	\N	\N	\N	\N	\N	active	\N	\N	\N	\N	2025-11-26 15:04:24.112694	2025-11-26 15:04:24.209145	0
user_admin	admin	$argon2id$v=19$m=65536,t=3,p=4$QUhlnmU7EnVOa7WhgfBUmppJ2BCUkonerXwPZnbZHSs$40xST5BPysI+qQGaEH+IbBODPcgMEGtFakH3B6PPHtJjIcs+84coZx5B4PdIW7PnKrTIzYufELTzfncq0zlzjA	系统管理员	\N	\N	\N	\N	\N	active	2025-12-07 13:33:29.846829	\N	\N	\N	2025-11-21 18:24:42.385218	\N	0
\.


--
-- Data for Name: sys_user_department; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_user_department (user_id, department_id, is_primary) FROM stdin;
\.


--
-- Data for Name: sys_user_role; Type: TABLE DATA; Schema: public; Owner: user
--

COPY public.sys_user_role (user_id, role_id) FROM stdin;
user_admin	role_system_admin
8260b3dfd4644240af1287b3e42e2a6e	role_system_admin
17dc369153e94352b5997c43b199d584	role_system_admin
7b8c3f4627814223a87adc36e05cf855	role_system_admin
8bd94eee5a4e46efbb2c522f3dd0a728	role_system_admin
ecb38e2075fc4bc9a52e1471b4b40ea8	role_system_admin
e35d6d2c2c074ef3bff7fdfb94db7b56	role_system_admin
38a62ebc769448b1ba6ec799e33d19bb	role_system_admin
e40215f239344e35bdf2b426851a3015	fb259c264baa409e94c760105344b996
820dd10691b741bc98feba569582d687	fb259c264baa409e94c760105344b996
a36ebf52922749e1833a3b4f3ec15730	fb259c264baa409e94c760105344b996
bdec23ae0d00478483df8f65e3fb27aa	fb259c264baa409e94c760105344b996
67d3dfb49ab2496b9563e46c16bcdafe	fb259c264baa409e94c760105344b996
554d386ec3b141568b40f12995284e3f	fb259c264baa409e94c760105344b996
fa6c6c7b9b864116ae81051291cf3bb3	fb259c264baa409e94c760105344b996
bb88c8eee5c0414d9a925e281d64e5b5	fb259c264baa409e94c760105344b996
1986669fecc6484aa9e9e027087df606	fb259c264baa409e94c760105344b996
9237db44761c486a9741a7d4a35a175e	fb259c264baa409e94c760105344b996
eb93a7ec533f4cb28a8121f24c2f5961	fb259c264baa409e94c760105344b996
621c4cdb03644490860ba755ab77f2d8	fb259c264baa409e94c760105344b996
9215e8918a644b7da13510b07ae20be4	fb259c264baa409e94c760105344b996
9681f75302b249699f76da59030fd5fe	fb259c264baa409e94c760105344b996
561c445ebc1d429e9db20014a1fc83af	fb259c264baa409e94c760105344b996
user_admin	role_super_admin
\.


--
-- Name: acc_archive acc_archive_archive_code_key; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_archive_code_key UNIQUE (archive_code);


--
-- Name: acc_archive acc_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_pkey PRIMARY KEY (id);


--
-- Name: acc_archive_relation acc_archive_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_pkey PRIMARY KEY (id);


--
-- Name: acc_archive_volume acc_archive_volume_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_archive_volume
    ADD CONSTRAINT acc_archive_volume_pkey PRIMARY KEY (id);


--
-- Name: acc_volume acc_volume_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_volume
    ADD CONSTRAINT acc_volume_pkey PRIMARY KEY (id);


--
-- Name: acc_volume acc_volume_volume_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.acc_volume
    ADD CONSTRAINT acc_volume_volume_code_key UNIQUE (volume_code);


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
-- Name: audit_inspection_log audit_inspection_log_pkey; Type: CONSTRAINT; Schema: public; Owner: user
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
-- Name: sys_audit_log sys_audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_audit_log
    ADD CONSTRAINT sys_audit_log_pkey PRIMARY KEY (id);


--
-- Name: sys_department sys_department_code_key; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_department
    ADD CONSTRAINT sys_department_code_key UNIQUE (code);


--
-- Name: sys_department sys_department_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_department
    ADD CONSTRAINT sys_department_pkey PRIMARY KEY (id);


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
-- Name: sys_position sys_position_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_position
    ADD CONSTRAINT sys_position_code_key UNIQUE (code);


--
-- Name: sys_position sys_position_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_position
    ADD CONSTRAINT sys_position_pkey PRIMARY KEY (id);


--
-- Name: sys_role sys_role_code_key; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_code_key UNIQUE (code);


--
-- Name: sys_role sys_role_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (id);


--
-- Name: sys_setting sys_setting_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_setting
    ADD CONSTRAINT sys_setting_pkey PRIMARY KEY (key);


--
-- Name: sys_user_department sys_user_department_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT sys_user_department_pkey PRIMARY KEY (user_id, department_id);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user_role sys_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: sys_user sys_user_username_key; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_username_key UNIQUE (username);


--
-- Name: bas_erp_config uk_erp_config_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bas_erp_config
    ADD CONSTRAINT uk_erp_config_name UNIQUE (name);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


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
-- Name: idx_archive_category; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_category ON public.acc_archive USING btree (category_code);


--
-- Name: idx_archive_code; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_code ON public.acc_archive USING btree (archive_code);


--
-- Name: idx_archive_destruction_hold; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_destruction_hold ON public.acc_archive USING btree (destruction_hold);


--
-- Name: idx_archive_fonds_id; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_fonds_id ON public.acc_archive USING btree (fonds_id);


--
-- Name: idx_archive_fonds_year; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_fonds_year ON public.acc_archive USING btree (fonds_no, fiscal_year);


--
-- Name: idx_archive_status; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_status ON public.acc_archive USING btree (status);


--
-- Name: idx_archive_unique_biz_id; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_unique_biz_id ON public.acc_archive USING btree (unique_biz_id);


--
-- Name: idx_archive_volume; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_volume ON public.acc_archive USING btree (volume_id);


--
-- Name: idx_archive_volume_id; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_archive_volume_id ON public.acc_archive USING btree (volume_id);


--
-- Name: idx_audit_inspection_archive_compliance; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_inspection_archive_compliance ON public.audit_inspection_log USING btree (archive_id, is_compliant);


--
-- Name: idx_audit_inspection_compliance; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_inspection_compliance ON public.audit_inspection_log USING btree (is_compliant, inspection_time);


--
-- Name: idx_audit_log_created_at; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_log_created_at ON public.sys_audit_log USING btree (created_at);


--
-- Name: idx_audit_log_hash; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_log_hash ON public.sys_audit_log USING btree (log_hash);


--
-- Name: idx_audit_risk; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_risk ON public.sys_audit_log USING btree (risk_level);


--
-- Name: idx_audit_role; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_role ON public.sys_audit_log USING btree (role_type);


--
-- Name: idx_audit_time; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_time ON public.sys_audit_log USING btree (created_at);


--
-- Name: idx_audit_user; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_audit_user ON public.sys_audit_log USING btree (username);


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
-- Name: idx_convert_log_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_convert_log_status ON public.arc_convert_log USING btree (status);


--
-- Name: idx_convert_log_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_convert_log_time ON public.arc_convert_log USING btree (convert_time);


--
-- Name: idx_created_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_created_time ON public.arc_file_content USING btree (created_time);


--
-- Name: idx_dept_code; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_dept_code ON public.sys_department USING btree (code);


--
-- Name: idx_dept_parent; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_dept_parent ON public.sys_department USING btree (parent_id);


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
-- Name: idx_inspection_archive; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_inspection_archive ON public.audit_inspection_log USING btree (archive_id);


--
-- Name: idx_inspection_stage; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_inspection_stage ON public.audit_inspection_log USING btree (inspection_stage);


--
-- Name: idx_inspection_time; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_inspection_time ON public.audit_inspection_log USING btree (inspection_time);


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
-- Name: idx_position_dept; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_position_dept ON public.sys_position USING btree (department_id);


--
-- Name: idx_position_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_position_status ON public.sys_position USING btree (status);


--
-- Name: idx_relation_source; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_relation_source ON public.acc_archive_relation USING btree (source_id);


--
-- Name: idx_relation_target; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_relation_target ON public.acc_archive_relation USING btree (target_id);


--
-- Name: idx_relation_type; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_relation_type ON public.acc_archive_relation USING btree (relation_type);


--
-- Name: idx_role_category; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_role_category ON public.sys_role USING btree (role_category);


--
-- Name: idx_role_code; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_role_code ON public.sys_role USING btree (code);


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
-- Name: idx_sys_user_role_role; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_sys_user_role_role ON public.sys_user_role USING btree (role_id);


--
-- Name: idx_sys_user_role_user; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_sys_user_role_user ON public.sys_user_role USING btree (user_id);


--
-- Name: idx_user_department; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_user_department ON public.sys_user USING btree (department_id);


--
-- Name: idx_user_employee; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_user_employee ON public.sys_user USING btree (employee_id);


--
-- Name: idx_user_username; Type: INDEX; Schema: public; Owner: user
--

CREATE INDEX idx_user_username ON public.sys_user USING btree (username);


--
-- Name: idx_volume_period; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_volume_period ON public.acc_volume USING btree (fiscal_period);


--
-- Name: idx_volume_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_volume_status ON public.acc_volume USING btree (status);


--
-- Name: acc_archive acc_archive_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.sys_user(id);


--
-- Name: acc_archive acc_archive_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive
    ADD CONSTRAINT acc_archive_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_department(id);


--
-- Name: acc_archive_relation acc_archive_relation_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.sys_user(id);


--
-- Name: acc_archive_relation acc_archive_relation_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: acc_archive_relation acc_archive_relation_target_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.acc_archive_relation
    ADD CONSTRAINT acc_archive_relation_target_id_fkey FOREIGN KEY (target_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: audit_inspection_log audit_inspection_log_archive_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.audit_inspection_log
    ADD CONSTRAINT audit_inspection_log_archive_id_fkey FOREIGN KEY (archive_id) REFERENCES public.acc_archive(id) ON DELETE CASCADE;


--
-- Name: audit_inspection_log audit_inspection_log_inspector_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.audit_inspection_log
    ADD CONSTRAINT audit_inspection_log_inspector_id_fkey FOREIGN KEY (inspector_id) REFERENCES public.sys_user(id);


--
-- Name: sys_user_department sys_user_department_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT sys_user_department_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_department(id) ON DELETE CASCADE;


--
-- Name: sys_user sys_user_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.sys_department(id);


--
-- Name: sys_user_department sys_user_department_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT sys_user_department_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE;


--
-- Name: sys_user_role sys_user_role_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_role(id) ON DELETE CASCADE;


--
-- Name: sys_user_role sys_user_role_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

