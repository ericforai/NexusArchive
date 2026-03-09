# Input: spec json
# Output: generated demo data + attachments + sql outputs
# Pos: demo data generator
# 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

from __future__ import annotations

import argparse
import hashlib
import json
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

DEFAULT_TIME = "2026-01-19 00:00:00"

NODE_ORDER = [
    ("basis", "AC04"),
    ("invoice", "AC01"),
    ("voucher", "AC01"),
    ("payment", "AC04"),
    ("bank", "AC04"),
    ("report", "AC03"),
]

DOC_PREFIX = {
    "basis": "HT",
    "invoice": "FP",
    "voucher": "JZ",
    "payment": "FK",
    "bank": "HD",
    "report": "BB",
}

DOC_DAY = {
    "basis": 3,
    "invoice": 8,
    "voucher": 15,
    "payment": 20,
    "bank": 20,
    "report": 28,
}

RETENTION_BY_NODE = {
    "report": "10Y",
}

ATTACHMENT_NODE_TYPES = {"invoice", "bank", "report"}
ATTACHMENT_TYPE_MAP = {
    "invoice": "invoice",
    "bank": "bank_slip",
    "report": "other",
}
ATTACHMENT_DESC_MAP = {
    "invoice": "原始发票",
    "bank": "银行回单附件",
    "report": "报表附件",
}


def load_spec(spec_path: Path) -> dict:
    return json.loads(spec_path.read_text(encoding="utf-8"))


def _format_date(year: int, month: str, day: int) -> str:
    return f"{year}-{month}-{day:02d}"


def _doc_no(prefix: str, year: int, month: str, seq: int) -> str:
    return f"{prefix}-{year}-{month}-{seq:03d}"


def _archive_code(fonds_code: str, year: int, retention: str, category_code: str, seq: int) -> str:
    return f"{fonds_code}-{year}-{retention}-FIN-{category_code}-{seq:04d}"


def _escape_pdf_text(text: str) -> str:
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def _make_simple_pdf(text: str) -> bytes:
    safe_text = _escape_pdf_text(text)
    content = f"BT /F1 12 Tf 50 780 Td ({safe_text}) Tj ET"
    objects = []
    objects.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    objects.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
    objects.append(
        "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
        "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
    )
    objects.append(
        f"4 0 obj\n<< /Length {len(content)} >>\nstream\n{content}\nendstream\nendobj\n"
    )
    objects.append("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

    pdf = "%PDF-1.4\n"
    offsets = []
    for obj in objects:
        offsets.append(len(pdf))
        pdf += obj

    xref_offset = len(pdf)
    pdf += f"xref\n0 {len(objects) + 1}\n"
    pdf += "0000000000 65535 f \n"
    for offset in offsets:
        pdf += f"{offset:010d} 00000 n \n"
    pdf += f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\n"
    pdf += f"startxref\n{xref_offset}\n%%EOF\n"

    return pdf.encode("latin-1")


def build_archives(spec: dict) -> list[dict]:
    year = int(spec["year"])
    archives: list[dict] = []
    seq_start = spec.get("default", {}).get("sequence_start", 1)

    for fonds in spec["fonds"]:
        counters = {"AC01": 0, "AC03": 0, "AC04": 0}
        fonds_code = fonds["fonds_code"]
        org_name = fonds["org_name"]
        month = fonds["month"]
        scene = fonds["scene"]
        amount = scene["amount"]
        counterparty = scene["counterparty"]
        creator = fonds.get("creator") or spec.get("default", {}).get("creator")
        prefixes = fonds.get("prefixes", {})

        for node_type, category_code in NODE_ORDER:
            counters[category_code] += 1
            seq = seq_start + counters[category_code] - 1
            prefix = prefixes.get(node_type, DOC_PREFIX[node_type])
            doc_no = _doc_no(prefix, year, month, seq)
            retention = RETENTION_BY_NODE.get(
                node_type, spec.get("default", {}).get("retention_period", "30Y")
            )

            archive = {
                "id": f"demo-2025-{fonds_code.lower()}-{node_type}-001",
                "fonds_no": fonds_code,
                "archive_code": _archive_code(fonds_code, year, retention, category_code, seq),
                "category_code": category_code,
                "title": scene["titles"][node_type],
                "fiscal_year": str(year),
                "fiscal_period": month,
                "retention_period": retention,
                "org_name": org_name,
                "creator": creator,
                "status": "archived",
                "amount": amount if node_type in {"invoice", "voucher", "payment", "bank"} else None,
                "doc_date": _format_date(year, month, DOC_DAY[node_type]),
                "unique_biz_id": doc_no,
                "custom_metadata": {},
                "security_level": spec.get("default", {}).get("security_level", "internal"),
                "created_by": "system",
                "summary": scene["label"],
                "counterparty": counterparty,
                "voucher_no": doc_no if node_type == "voucher" else None,
                "invoice_no": doc_no if node_type == "invoice" else None,
                "retention_start_date": _format_date(year, month, DOC_DAY[node_type]),
                "node_type": node_type,
            }

            if node_type == "report":
                archive["custom_metadata"] = {
                    "reportType": scene.get("report_type", "MONTHLY"),
                    "period": f"{year}-{month}",
                }

            archives.append(archive)

    return archives


def build_relations(spec: dict, archives: list[dict]) -> list[dict]:
    by_fonds: dict[str, dict[str, dict]] = {}
    for archive in archives:
        by_fonds.setdefault(archive["fonds_no"], {})[archive["node_type"]] = archive

    relations: list[dict] = []
    for fonds in spec["fonds"]:
        fonds_code = fonds["fonds_code"]
        nodes = by_fonds[fonds_code]
        rels = [
            ("basis", "voucher", "BASIS", "依据单据"),
            ("invoice", "voucher", "ORIGINAL_VOUCHER", "原始凭证"),
            ("voucher", "payment", "CASH_FLOW", "付款流程"),
            ("payment", "bank", "CASH_FLOW", "银行回单"),
            ("voucher", "report", "ARCHIVE", "报表归档"),
        ]
        for idx, (source_type, target_type, rel_type, rel_desc) in enumerate(rels, start=1):
            relations.append(
                {
                    "id": f"demo-rel-{fonds_code.lower()}-{idx:02d}",
                    "source_id": nodes[source_type]["id"],
                    "target_id": nodes[target_type]["id"],
                    "relation_type": rel_type,
                    "relation_desc": rel_desc,
                    "created_by": "system",
                    "created_time": DEFAULT_TIME,
                    "deleted": 0,
                    "created_at": DEFAULT_TIME,
                }
            )

    return relations


def build_file_contents(spec: dict, archives: list[dict]) -> list[dict]:
    files: list[dict] = []

    for archive in archives:
        node_type = archive["node_type"]
        if node_type not in ATTACHMENT_NODE_TYPES:
            continue

        file_name = f"{archive['unique_biz_id']}_{node_type}.pdf"
        text = f"{archive['unique_biz_id']} {node_type} {archive['amount'] or ''}"
        content = _make_simple_pdf(text)
        file_hash = hashlib.sha256(content).hexdigest()

        attachment_type = ATTACHMENT_TYPE_MAP.get(node_type, "other")

        files.append(
            {
                "id": f"demo-file-{archive['fonds_no'].lower()}-{node_type}-001",
                "archival_code": archive["archive_code"],
                "file_name": file_name,
                "file_type": "pdf",
                "file_size": len(content),
                "file_hash": file_hash,
                "hash_algorithm": "SHA256",
                "storage_path": (
                    f"uploads/demo/{archive['fonds_no']}/{archive['unique_biz_id']}/{file_name}"
                ),
                "created_time": DEFAULT_TIME,
                "item_id": archive["id"],
                "fiscal_year": archive["fiscal_year"],
                "voucher_type": archive["category_code"],
                "creator": archive["creator"],
                "fonds_code": archive["fonds_no"],
                "source_system": "DEMO",
                "business_doc_no": archive["unique_biz_id"],
                "summary": archive["title"],
                "doc_date": archive["doc_date"],
                "pre_archive_status": "PENDING_CHECK",
                "archive_id": archive["id"],
                "attachment_type": attachment_type,
                "attachment_desc": ATTACHMENT_DESC_MAP.get(node_type, "附件"),
                "content_bytes": content,
            }
        )

    return files


def write_demo_files(output_dir: Path, files: list[dict]) -> list[Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    written = []

    for file_info in files:
        content = file_info.get("content_bytes") or _make_simple_pdf(file_info["file_name"])
        file_path = output_dir / file_info["file_name"]
        file_path.write_bytes(content)
        written.append(file_path)

    return written


def render_sql_blocks(spec: dict) -> str:
    def decimal_amount(value: object) -> Decimal | None:
        if value is None:
            return None
        return Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    def sql_literal(value: object) -> str:
        if value is None:
            return "NULL"
        if isinstance(value, bool):
            return "TRUE" if value else "FALSE"
        if isinstance(value, (int, Decimal)):
            return str(value)
        if isinstance(value, float):
            return str(Decimal(str(value)))
        if isinstance(value, (dict, list)):
            payload = json.dumps(value, ensure_ascii=False, separators=(",", ":"))
            return "'" + payload.replace("'", "''") + "'"
        return "'" + str(value).replace("'", "''") + "'"

    def render_insert(table: str, columns: list[str], rows: list[dict]) -> str:
        if not rows:
            return ""
        values_rows = []
        for row in rows:
            values = ", ".join(sql_literal(row.get(col)) for col in columns)
            values_rows.append(f"({values})")
        values_sql = ",\n  ".join(values_rows)
        columns_sql = ", ".join(columns)
        return f"INSERT INTO {table} ({columns_sql}) VALUES\n  {values_sql}\nON CONFLICT DO NOTHING;\n"

    year = int(spec["year"])
    archives = build_archives(spec)
    relations = build_relations(spec, archives)
    files = build_file_contents(spec, archives)

    archives_by_fonds: dict[str, list[dict]] = {}
    files_by_fonds: dict[str, list[dict]] = {}
    nodes_by_fonds: dict[str, dict[str, dict]] = {}

    for archive in archives:
        archives_by_fonds.setdefault(archive["fonds_no"], []).append(archive)
        nodes_by_fonds.setdefault(archive["fonds_no"], {})[archive["node_type"]] = archive

    for file_info in files:
        files_by_fonds.setdefault(file_info["fonds_code"], []).append(file_info)

    volume_rows: list[dict] = []
    volume_id_by_fonds: dict[str, str] = {}
    for fonds in spec["fonds"]:
        fonds_code = fonds["fonds_code"]
        month = fonds["month"]
        volume_id = f"demo-volume-{fonds_code.lower()}-{year}-{month}"
        volume_id_by_fonds[fonds_code] = volume_id
        file_count = sum(
            1
            for archive in archives_by_fonds.get(fonds_code, [])
            if archive["category_code"] == "AC01"
        )
        volume_rows.append(
            {
                "id": volume_id,
                "volume_code": f"AJ-{year}-{fonds_code}-{month}",
                "title": f"{year}年{int(month)}月会计凭证",
                "fonds_no": fonds_code,
                "fiscal_year": str(year),
                "fiscal_period": month,
                "category_code": "AC01",
                "file_count": file_count,
                "retention_period": spec.get("default", {}).get("retention_period", "30Y"),
                "volume_status": "archived",
                "archived_at": DEFAULT_TIME,
                "created_time": DEFAULT_TIME,
                "last_modified_time": DEFAULT_TIME,
                "deleted": 0,
                "updated_time": DEFAULT_TIME,
            }
        )

    for archive in archives:
        if archive["category_code"] == "AC01":
            archive["volume_id"] = volume_id_by_fonds.get(archive["fonds_no"])

    archive_batch_rows: list[dict] = []
    archive_batch_by_fonds: dict[str, int] = {}
    for idx, fonds in enumerate(spec["fonds"], start=1):
        fonds_code = fonds["fonds_code"]
        batch_id = 5000 + idx
        batch_no = f"DEMO-ARCH-{year}-{fonds_code}"
        batch_hash = hashlib.sha256(batch_no.encode("utf-8")).hexdigest()
        archive_batch_rows.append(
            {
                "id": batch_id,
                "batch_no": batch_no,
                "prev_batch_hash": None,
                "current_batch_hash": batch_hash,
                "chained_hash": batch_hash,
                "hash_algo": "SHA256",
                "item_count": len(archives_by_fonds.get(fonds_code, [])),
                "operator_id": "system",
                "created_time": DEFAULT_TIME,
                "batch_sequence": batch_id,
            }
        )
        archive_batch_by_fonds[fonds_code] = batch_id

    for archive in archives:
        archive["batch_id"] = archive_batch_by_fonds.get(archive["fonds_no"])

    attachment_rows: list[dict] = []
    file_rows: list[dict] = []
    for file_info in files:
        file_rows.append(
            {
                "id": file_info["id"],
                "archival_code": file_info["archival_code"],
                "file_name": file_info["file_name"],
                "file_type": file_info["file_type"],
                "file_size": file_info["file_size"],
                "file_hash": file_info["file_hash"],
                "hash_algorithm": file_info["hash_algorithm"],
                "storage_path": file_info["storage_path"],
                "created_time": file_info["created_time"],
                "item_id": file_info["item_id"],
                "fiscal_year": file_info["fiscal_year"],
                "voucher_type": file_info["voucher_type"],
                "creator": file_info["creator"],
                "fonds_code": file_info["fonds_code"],
                "source_system": file_info["source_system"],
                "business_doc_no": file_info["business_doc_no"],
                "summary": file_info["summary"],
                "doc_date": file_info["doc_date"],
                "pre_archive_status": file_info["pre_archive_status"],
            }
        )
        attachment_rows.append(
            {
                "id": f"demo-att-{file_info['id']}",
                "archive_id": file_info["archive_id"],
                "file_id": file_info["id"],
                "attachment_type": file_info["attachment_type"],
                "relation_desc": file_info["attachment_desc"],
                "created_by": "system",
                "created_time": DEFAULT_TIME,
                "created_at": DEFAULT_TIME,
            }
        )

    collection_rows: list[dict] = []
    ingest_rows: list[dict] = []
    original_voucher_rows: list[dict] = []
    voucher_seq_rows: list[dict] = []

    for idx, fonds in enumerate(spec["fonds"], start=1):
        fonds_code = fonds["fonds_code"]
        month = fonds["month"]
        fonds_id = fonds.get("fonds_id") or f"fonds-{fonds_code.lower()}"
        batch_id = 2025010000 + idx
        batch_no = f"COL-{year}{month}01-{idx:03d}"
        files_for_fonds = files_by_fonds.get(fonds_code, [])
        total_size = sum(file_info["file_size"] for file_info in files_for_fonds)
        collection_rows.append(
            {
                "id": batch_id,
                "batch_no": batch_no,
                "batch_name": f"{year}年{int(month)}月{fonds['fonds_name']}采集批次",
                "fonds_id": fonds_id,
                "fonds_code": fonds_code,
                "fiscal_year": str(year),
                "fiscal_period": month,
                "archival_category": "VOUCHER",
                "status": "ARCHIVED",
                "total_files": len(files_for_fonds),
                "uploaded_files": len(files_for_fonds),
                "failed_files": 0,
                "total_size_bytes": total_size,
                "created_by": 1,
                "created_time": DEFAULT_TIME,
                "last_modified_time": DEFAULT_TIME,
                "completed_time": DEFAULT_TIME,
            }
        )
        ingest_rows.append(
            {
                "request_id": f"demo-ingest-{fonds_code.lower()}-{year}",
                "status": "COMPLETED",
                "message": "Demo data ingest completed",
                "created_time": DEFAULT_TIME,
                "updated_time": DEFAULT_TIME,
                "created_at": DEFAULT_TIME,
                "updated_at": DEFAULT_TIME,
                "fonds_no": fonds_code,
            }
        )
        invoice_archive = nodes_by_fonds[fonds_code]["invoice"]
        original_voucher_rows.append(
            {
                "id": f"demo-ov-{fonds_code.lower()}-inv-001",
                "voucher_no": invoice_archive["unique_biz_id"],
                "voucher_category": "INVOICE",
                "voucher_type": "VAT_INVOICE",
                "business_date": invoice_archive["doc_date"],
                "amount": decimal_amount(invoice_archive["amount"]),
                "currency": "CNY",
                "counterparty": invoice_archive["counterparty"],
                "summary": invoice_archive["title"],
                "creator": invoice_archive["creator"],
                "source_system": "DEMO",
                "source_doc_id": invoice_archive["id"],
                "fonds_code": fonds_code,
                "fiscal_year": invoice_archive["fiscal_year"],
                "retention_period": invoice_archive["retention_period"],
                "archive_status": "DRAFT",
                "created_by": "system",
                "created_time": DEFAULT_TIME,
                "deleted": 0,
                "pool_status": "ENTRY",
            }
        )
        voucher_seq_rows.append(
            {
                "id": f"demo-ov-seq-{fonds_code.lower()}",
                "fonds_code": fonds_code,
                "fiscal_year": str(year),
                "voucher_category": "INVOICE",
                "current_seq": 1,
                "last_updated": DEFAULT_TIME,
            }
        )

    archive_rows: list[dict] = []
    for archive in archives:
        archive_rows.append(
            {
                "id": archive["id"],
                "fonds_no": archive["fonds_no"],
                "archive_code": archive["archive_code"],
                "category_code": archive["category_code"],
                "title": archive["title"],
                "fiscal_year": archive["fiscal_year"],
                "fiscal_period": archive["fiscal_period"],
                "retention_period": archive["retention_period"],
                "org_name": archive["org_name"],
                "creator": archive["creator"],
                "status": archive["status"],
                "amount": decimal_amount(archive["amount"]),
                "doc_date": archive["doc_date"],
                "unique_biz_id": archive["unique_biz_id"],
                "standard_metadata": None,
                "custom_metadata": archive["custom_metadata"],
                "security_level": archive["security_level"],
                "created_by": archive["created_by"],
                "volume_id": archive.get("volume_id"),
                "created_time": DEFAULT_TIME,
                "last_modified_time": DEFAULT_TIME,
                "deleted": 0,
                "summary": archive["summary"],
                "batch_id": archive.get("batch_id"),
                "archived_at": DEFAULT_TIME,
                "counterparty": archive["counterparty"],
                "voucher_no": archive["voucher_no"],
                "invoice_no": archive["invoice_no"],
                "retention_start_date": archive["retention_start_date"],
                "destruction_status": "NORMAL",
            }
        )

    archive_relation_rows: list[dict] = []
    for relation in relations:
        archive_relation_rows.append(
            {
                "id": relation["id"],
                "source_id": relation["source_id"],
                "target_id": relation["target_id"],
                "relation_type": relation["relation_type"],
                "relation_desc": relation["relation_desc"],
                "created_by": relation["created_by"],
                "created_time": relation["created_time"],
                "deleted": relation["deleted"],
                "created_at": relation["created_at"],
            }
        )

    archive_seq_rows: list[dict] = []
    category_order = ["AC01", "AC03", "AC04"]
    for fonds in spec["fonds"]:
        fonds_code = fonds["fonds_code"]
        for category in category_order:
            count = sum(
                1
                for archive in archives_by_fonds.get(fonds_code, [])
                if archive["category_code"] == category
            )
            if count:
                archive_seq_rows.append(
                    {
                        "fonds_code": fonds_code,
                        "fiscal_year": str(year),
                        "category_code": category,
                        "current_val": count,
                        "updated_time": DEFAULT_TIME,
                    }
                )

    borrow_rows: list[dict] = []
    destruction_rows: list[dict] = []
    audit_rows: list[dict] = []
    search_rows: list[dict] = []
    storage_rows: list[dict] = []
    recon_rows: list[dict] = []

    for idx, fonds in enumerate(spec["fonds"], start=1):
        fonds_code = fonds["fonds_code"]
        voucher_archive = nodes_by_fonds[fonds_code]["voucher"]
        files_for_fonds = files_by_fonds.get(fonds_code, [])
        file_count = len(files_for_fonds)
        total_bytes = sum(file_info["file_size"] for file_info in files_for_fonds)
        used_size_gb = Decimal(total_bytes) / Decimal(1024**3)
        used_size_gb = used_size_gb.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

        borrow_rows.append(
            {
                "id": f"demo-borrow-{fonds_code.lower()}-001",
                "user_id": "user-demo",
                "user_name": "演示用户",
                "archive_id": voucher_archive["id"],
                "archive_title": voucher_archive["title"],
                "reason": "演示借阅申请",
                "borrow_date": voucher_archive["doc_date"],
                "expected_return_date": voucher_archive["doc_date"],
                "status": "APPROVED",
                "created_at": DEFAULT_TIME,
                "last_modified_time": DEFAULT_TIME,
                "deleted": 0,
                "fonds_no": fonds_code,
                "archive_year": year,
                "type": "electronic",
                "updated_at": DEFAULT_TIME,
            }
        )

        snapshot = json.dumps(
            {
                "archive_id": voucher_archive["id"],
                "archive_code": voucher_archive["archive_code"],
                "title": voucher_archive["title"],
            },
            ensure_ascii=False,
            separators=(",", ":"),
        )
        destruction_rows.append(
            {
                "id": f"demo-destroy-{fonds_code.lower()}",
                "fonds_no": fonds_code,
                "archive_year": year,
                "archive_object_id": voucher_archive["id"],
                "retention_policy_id": "policy-30Y",
                "approval_ticket_id": f"demo-destroy-ticket-{fonds_code.lower()}",
                "destroyed_by": "system",
                "destroyed_at": DEFAULT_TIME,
                "trace_id": f"demo-destroy-trace-{fonds_code.lower()}",
                "snapshot": snapshot,
                "prev_hash": None,
                "curr_hash": None,
                "sig": None,
                "created_at": DEFAULT_TIME,
            }
        )

        audit_rows.append(
            {
                "id": f"demo-audit-{fonds_code.lower()}-001",
                "user_id": "system",
                "username": "系统",
                "action": "ARCHIVE",
                "resource_type": "acc_archive",
                "resource_id": voucher_archive["id"],
                "operation_result": "SUCCESS",
                "risk_level": "LOW",
                "details": f"{fonds_code} demo archive action",
                "created_time": DEFAULT_TIME,
                "created_at": DEFAULT_TIME,
                "source_fonds": fonds_code,
            }
        )

        search_rows.append(
            {
                "id": f"demo-search-{fonds_code.lower()}",
                "fonds_no": fonds_code,
                "search_type": "ARCHIVE",
                "search_duration_ms": 120 + idx,
                "result_count": len(archives_by_fonds.get(fonds_code, [])),
                "user_id": "system",
                "recorded_at": DEFAULT_TIME,
                "created_at": DEFAULT_TIME,
            }
        )

        storage_rows.append(
            {
                "id": f"demo-storage-{fonds_code.lower()}",
                "fonds_no": fonds_code,
                "total_size_gb": Decimal("10.00"),
                "used_size_gb": used_size_gb,
                "file_count": file_count,
                "recorded_at": DEFAULT_TIME,
                "created_at": DEFAULT_TIME,
            }
        )

        recon_rows.append(
            {
                "id": f"demo-recon-{fonds_code.lower()}",
                "fonds_code": fonds_code,
                "fiscal_year": str(year),
                "fiscal_period": fonds["month"],
                "erp_voucher_count": 1,
                "arc_voucher_count": sum(
                    1
                    for archive in archives_by_fonds.get(fonds_code, [])
                    if archive["category_code"] == "AC01"
                ),
                "attachment_count": file_count,
                "attachment_missing_count": 0,
                "recon_status": "MATCHED",
                "recon_message": "Demo data reconciliation",
                "recon_time": DEFAULT_TIME,
                "source_system": "DEMO",
                "recon_start_date": voucher_archive["doc_date"],
                "recon_end_date": voucher_archive["doc_date"],
            }
        )

    sql_sections = [
        render_insert(
            "collection_batch",
            [
                "id",
                "batch_no",
                "batch_name",
                "fonds_id",
                "fonds_code",
                "fiscal_year",
                "fiscal_period",
                "archival_category",
                "status",
                "total_files",
                "uploaded_files",
                "failed_files",
                "total_size_bytes",
                "created_by",
                "created_time",
                "last_modified_time",
                "completed_time",
            ],
            collection_rows,
        ),
        render_insert(
            "sys_ingest_request_status",
            [
                "request_id",
                "status",
                "message",
                "created_time",
                "updated_time",
                "created_at",
                "updated_at",
                "fonds_no",
            ],
            ingest_rows,
        ),
        render_insert(
            "arc_original_voucher",
            [
                "id",
                "voucher_no",
                "voucher_category",
                "voucher_type",
                "business_date",
                "amount",
                "currency",
                "counterparty",
                "summary",
                "creator",
                "source_system",
                "source_doc_id",
                "fonds_code",
                "fiscal_year",
                "retention_period",
                "archive_status",
                "created_by",
                "created_time",
                "deleted",
                "pool_status",
            ],
            original_voucher_rows,
        ),
        render_insert(
            "arc_original_voucher_sequence",
            [
                "id",
                "fonds_code",
                "fiscal_year",
                "voucher_category",
                "current_seq",
                "last_updated",
            ],
            voucher_seq_rows,
        ),
        render_insert(
            "arc_file_content",
            [
                "id",
                "archival_code",
                "file_name",
                "file_type",
                "file_size",
                "file_hash",
                "hash_algorithm",
                "storage_path",
                "created_time",
                "item_id",
                "fiscal_year",
                "voucher_type",
                "creator",
                "fonds_code",
                "source_system",
                "business_doc_no",
                "summary",
                "doc_date",
                "pre_archive_status",
            ],
            file_rows,
        ),
        render_insert(
            "acc_archive",
            [
                "id",
                "fonds_no",
                "archive_code",
                "category_code",
                "title",
                "fiscal_year",
                "fiscal_period",
                "retention_period",
                "org_name",
                "creator",
                "status",
                "amount",
                "doc_date",
                "unique_biz_id",
                "standard_metadata",
                "custom_metadata",
                "security_level",
                "created_by",
                "volume_id",
                "created_time",
                "last_modified_time",
                "deleted",
                "summary",
                "batch_id",
                "archived_at",
                "counterparty",
                "voucher_no",
                "invoice_no",
                "retention_start_date",
                "destruction_status",
            ],
            archive_rows,
        ),
        render_insert(
            "acc_archive_attachment",
            [
                "id",
                "archive_id",
                "file_id",
                "attachment_type",
                "relation_desc",
                "created_by",
                "created_time",
                "created_at",
            ],
            attachment_rows,
        ),
        render_insert(
            "acc_archive_relation",
            [
                "id",
                "source_id",
                "target_id",
                "relation_type",
                "relation_desc",
                "created_by",
                "created_time",
                "deleted",
                "created_at",
            ],
            archive_relation_rows,
        ),
        render_insert(
            "acc_archive_volume",
            [
                "id",
                "volume_code",
                "title",
                "fonds_no",
                "fiscal_year",
                "fiscal_period",
                "category_code",
                "file_count",
                "retention_period",
                "volume_status",
                "archived_at",
                "created_time",
                "last_modified_time",
                "deleted",
                "updated_time",
            ],
            volume_rows,
        ),
        render_insert(
            "arc_archive_batch",
            [
                "id",
                "batch_no",
                "prev_batch_hash",
                "current_batch_hash",
                "chained_hash",
                "hash_algo",
                "item_count",
                "operator_id",
                "created_time",
                "batch_sequence",
            ],
            archive_batch_rows,
        ),
        render_insert(
            "sys_archival_code_sequence",
            ["fonds_code", "fiscal_year", "category_code", "current_val", "updated_time"],
            archive_seq_rows,
        ),
        render_insert(
            "biz_borrowing",
            [
                "id",
                "user_id",
                "user_name",
                "archive_id",
                "archive_title",
                "reason",
                "borrow_date",
                "expected_return_date",
                "status",
                "created_at",
                "last_modified_time",
                "deleted",
                "fonds_no",
                "archive_year",
                "type",
                "updated_at",
            ],
            borrow_rows,
        ),
        render_insert(
            "destruction_log",
            [
                "id",
                "fonds_no",
                "archive_year",
                "archive_object_id",
                "retention_policy_id",
                "approval_ticket_id",
                "destroyed_by",
                "destroyed_at",
                "trace_id",
                "snapshot",
                "prev_hash",
                "curr_hash",
                "sig",
                "created_at",
            ],
            destruction_rows,
        ),
        render_insert(
            "sys_audit_log",
            [
                "id",
                "user_id",
                "username",
                "action",
                "resource_type",
                "resource_id",
                "operation_result",
                "risk_level",
                "details",
                "created_time",
                "created_at",
                "source_fonds",
            ],
            audit_rows,
        ),
        render_insert(
            "search_performance_stats",
            [
                "id",
                "fonds_no",
                "search_type",
                "search_duration_ms",
                "result_count",
                "user_id",
                "recorded_at",
                "created_at",
            ],
            search_rows,
        ),
        render_insert(
            "storage_capacity_stats",
            ["id", "fonds_no", "total_size_gb", "used_size_gb", "file_count", "recorded_at", "created_at"],
            storage_rows,
        ),
        render_insert(
            "arc_reconciliation_record",
            [
                "id",
                "fonds_code",
                "fiscal_year",
                "fiscal_period",
                "erp_voucher_count",
                "arc_voucher_count",
                "attachment_count",
                "attachment_missing_count",
                "recon_status",
                "recon_message",
                "recon_time",
                "source_system",
                "recon_start_date",
                "recon_end_date",
            ],
            recon_rows,
        ),
    ]

    sql_body = "\n".join(section for section in sql_sections if section).rstrip()
    return "\n".join(
        [
            "-- DEMO DATA START",
            "-- Generated by scripts/generate_demo_data.py",
            "SET search_path TO public;",
            sql_body,
            "-- DEMO DATA END",
        ]
    ).rstrip() + "\n"


def write_outputs(
    *,
    spec_path: Path,
    seed_path: Path,
    migration_path: Path,
    docs_dir: Path,
) -> None:
    spec = load_spec(spec_path)
    sql_block = render_sql_blocks(spec)
    archives = build_archives(spec)
    files = build_file_contents(spec, archives)

    migration_path.parent.mkdir(parents=True, exist_ok=True)
    migration_path.write_text(sql_block, encoding="utf-8")

    seed_text = seed_path.read_text(encoding="utf-8")
    start_marker = "-- DEMO DATA START"
    end_marker = "-- DEMO DATA END"
    insert_marker = "-- PostgreSQL database dump complete"

    if start_marker in seed_text and end_marker in seed_text:
        before = seed_text.split(start_marker, 1)[0]
        after = seed_text.split(end_marker, 1)[1]
        updated = before.rstrip() + "\n\n" + sql_block + "\n" + after.lstrip()
    else:
        if insert_marker not in seed_text:
            raise ValueError("Seed file missing dump completion marker")
        before, after = seed_text.rsplit(insert_marker, 1)
        updated = before.rstrip() + "\n\n" + sql_block + "\n" + insert_marker + after

    seed_path.write_text(updated, encoding="utf-8")
    write_demo_files(docs_dir, files)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate demo data SQL and attachments")
    parser.add_argument("--spec", default="scripts/demo_data_spec.json")
    parser.add_argument("--seed", default="tmp/generated-demo-data/seed-data.sql")
    parser.add_argument(
        "--migration",
        default="tmp/generated-demo-data/V20260119__seed_demo_full_data.sql",
    )
    parser.add_argument("--docs-dir", default="tmp/generated-demo-data/demo-files")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    spec_path = Path(args.spec)
    if args.write:
        write_outputs(
            spec_path=spec_path,
            seed_path=Path(args.seed),
            migration_path=Path(args.migration),
            docs_dir=Path(args.docs_dir),
        )
    else:
        print(render_sql_blocks(load_spec(spec_path)))
