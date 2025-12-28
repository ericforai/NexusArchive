#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
NexusArchive 端到端完整测试脚本

运行方式:
    python3 tests/e2e_full_test.py

    # 指定后端地址
    BASE_URL=http://localhost:19090/api python3 tests/e2e_full_test.py

    # 跳过数据清理
    SKIP_CLEANUP=1 python3 tests/e2e_full_test.py

测试覆盖:
    1. ERP 同步 (YonSuite 凭证同步)
    2. 电子凭证池
    3. 归档提交与审批
    4. 四性检测
    5. 组卷功能
    6. 库房管理
    7. AIP 包导出
    8. 穿透联查
    9. 借阅申请
    10. 开放鉴定
    11. 销毁鉴定
    12. 归档批次
    13. 审计日志
"""

import os
import sys
import json
import time
import requests
from datetime import datetime
from typing import Optional, Dict, Any, List, Tuple

# ============================
# 配置
# ============================
BASE_URL = os.environ.get("BASE_URL", "http://localhost:19090/api")
USERNAME = os.environ.get("USERNAME", "admin")
PASSWORD = os.environ.get("PASSWORD", "admin123")
SKIP_CLEANUP = os.environ.get("SKIP_CLEANUP", "0") == "1"

# 颜色
GREEN = "\033[0;32m"
YELLOW = "\033[1;33m"
RED = "\033[0;31m"
BLUE = "\033[0;34m"
NC = "\033[0m"

# 统计
PASSED = 0
FAILED = 0
WARNINGS = 0


def log_header(msg: str):
    print()
    print(f"{BLUE}{'═' * 60}{NC}")
    print(f"{BLUE}  {msg}{NC}")
    print(f"{BLUE}{'═' * 60}{NC}")


def log_step(msg: str):
    print(f"{YELLOW}▶ {msg}{NC}")


def log_success(msg: str):
    global PASSED
    print(f"{GREEN}✓ {msg}{NC}")
    PASSED += 1


def log_fail(msg: str):
    global FAILED
    print(f"{RED}✗ {msg}{NC}")
    FAILED += 1


def log_warn(msg: str):
    global WARNINGS
    print(f"{YELLOW}⚠ {msg}{NC}")
    WARNINGS += 1


def log_info(msg: str):
    print(f"  {msg}")


# ============================
# API 客户端
# ============================
class APIClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.token: Optional[str] = None
        self.session = requests.Session()

    def login(self, username: str, password: str) -> bool:
        resp = self.session.post(
            f"{self.base_url}/auth/login",
            json={"username": username, "password": password},
            headers={"Content-Type": "application/json"},
        )
        data = resp.json()
        if data.get("code") == 200 and data.get("data", {}).get("token"):
            self.token = data["data"]["token"]
            self.session.headers["Authorization"] = f"Bearer {self.token}"
            return True
        return False

    def get(self, path: str, **kwargs) -> Dict[str, Any]:
        resp = self.session.get(f"{self.base_url}{path}", **kwargs)
        try:
            return resp.json()
        except:
            return {"code": resp.status_code, "raw": resp.text[:500]}

    def post(self, path: str, data: Any = None, **kwargs) -> Dict[str, Any]:
        resp = self.session.post(
            f"{self.base_url}{path}",
            json=data,
            headers={"Content-Type": "application/json"},
            **kwargs,
        )
        try:
            return resp.json()
        except:
            return {"code": resp.status_code, "raw": resp.text[:500]}

    def delete(self, path: str, **kwargs) -> Dict[str, Any]:
        resp = self.session.delete(f"{self.base_url}{path}", **kwargs)
        try:
            return resp.json()
        except:
            return {"code": resp.status_code, "raw": resp.text[:500]}

    def get_file(self, path: str) -> Tuple[int, bytes, Dict[str, str]]:
        resp = self.session.get(f"{self.base_url}{path}", stream=True)
        content = b"".join(resp.iter_content(chunk_size=8192))
        return resp.status_code, content, dict(resp.headers)


# ============================
# 测试步骤
# ============================
def test_erp_sync(client: APIClient) -> Optional[List[str]]:
    """测试 ERP 同步"""
    log_header("1. ERP 同步 (VOUCHER_SYNC)")

    # 触发同步
    log_step("触发 VOUCHER_SYNC 同步...")
    resp = client.post("/erp/scenario/1/sync")
    code = resp.get("code")
    log_info(f"HTTP Code: {code}")

    if code == 500:
        log_warn("ERP 同步返回 500 (外部 API 可能未配置)")
    elif code != 200:
        log_warn(f"ERP 同步返回非预期状态: {code}")

    # 等待同步完成
    log_step("等待同步完成...")
    time.sleep(8)

    # 检查历史
    history = client.get("/erp/scenario/1/history")
    if history.get("code") == 200:
        data = history.get("data", [])
        if data:
            latest = data[0]
            status = latest.get("status")
            total = latest.get("totalCount", 0)
            success = latest.get("successCount", 0)
            log_info(f"状态: {status}, 获取: {total}, 成功: {success}")
            if status == "SUCCESS":
                log_success(f"ERP 同步成功 (新增 {success} 条)")
            elif status == "FAILED":
                error_msg = latest.get("errorMessage", "")
                log_warn(f"同步失败: {error_msg[:100]}")
            else:
                log_warn(f"同步状态: {status}")
        else:
            log_warn("无同步历史记录")
        log_success("ERP 历史查询成功")
    else:
        log_fail("无法获取同步历史")
        return None

    # 获取同步的凭证 ID
    return get_yonsuite_file_ids(client)


def get_yonsuite_file_ids(client: APIClient) -> List[str]:
    """获取 YonSuite 同步的文件 ID"""
    resp = client.get("/pool/list")
    ids = []
    if resp.get("code") == 200:
        records = resp.get("data", [])
        for r in records:
            if "用友" in str(r.get("sourceSystem", "")):
                ids.append(r.get("id"))
    return ids[:3]  # 返回前3个


def test_pool(client: APIClient):
    """测试电子凭证池"""
    log_header("2. 电子凭证池")

    # 统计
    log_step("获取凭证池统计...")
    stats = client.get("/pool/stats/status")
    if stats.get("code") == 200:
        data = stats.get("data", {})
        log_info(f"统计: {json.dumps(data, ensure_ascii=False)}")
        log_success("凭证池统计获取成功")
    else:
        log_fail("凭证池统计获取失败")

    # 列表
    log_step("获取凭证池列表...")
    pool = client.get("/pool/list")
    if pool.get("code") == 200:
        records = pool.get("data", [])
        log_info(f"凭证池项目数: {len(records)}")
        for r in records[:3]:
            log_info(f"  - {r.get('erpVoucherNo', 'N/A')}: {r.get('fileName', 'N/A')}")
        log_success("凭证池列表获取成功")
    else:
        log_fail("凭证池列表获取失败")


def test_archive_submit(client: APIClient, file_ids: List[str]):
    """测试归档提交"""
    log_header("3. 归档提交与审批")

    if not file_ids:
        log_warn("没有可提交的文件，跳过")
        return

    # 提交归档申请
    log_step(f"提交归档申请 ({len(file_ids)} 个文件)...")
    submit_data = {
        "fileIds": file_ids,
        "applicantId": "user_admin_001",
        "applicantName": "系统管理员",
        "reason": "端到端测试归档",
    }
    resp = client.post("/pool/submit/batch", submit_data)
    if resp.get("code") == 200:
        log_success("归档申请提交成功")
    else:
        log_fail(f"归档申请提交失败: {resp.get('message', '')}")
        return

    # 获取待审批列表
    log_step("获取待审批列表...")
    approvals = client.get("/archive-approval/list?status=PENDING")
    if approvals.get("code") == 200:
        records = approvals.get("data", {}).get("records", [])
        log_info(f"待审批数量: {len(records)}")

        # 审批前3个
        for r in records[:3]:
            aid = r.get("id")
            log_step(f"审批 {aid}...")
            approve_resp = client.post(
                "/archive-approval/approve",
                {"id": aid, "comment": "端到端测试通过"},
            )
            if approve_resp.get("code") == 200:
                log_success(f"审批 {aid} 通过")
            else:
                log_fail(f"审批 {aid} 失败")
    else:
        log_warn("获取待审批列表失败")


def test_compliance(client: APIClient, file_ids: List[str]):
    """测试四性检测"""
    log_header("4. 四性检测")

    if not file_ids:
        log_warn("没有可检测的文件，跳过")
        return

    log_step(f"执行批量四性检测 ({len(file_ids)} 个文件)...")
    resp = client.post("/pool/check/batch", file_ids)
    if resp.get("code") == 200:
        results = resp.get("data", [])
        for r in results[:3]:
            status = r.get("status", "N/A")
            auth = r.get("authenticity", {}).get("status", "N/A")
            log_info(f"  检测状态: {status}, 真实性: {auth}")
        log_success("四性检测完成")
    else:
        log_fail(f"四性检测失败: {resp.get('message', '')}")


def test_volume(client: APIClient):
    """测试组卷功能"""
    log_header("5. 组卷功能")

    # 获取案卷列表
    log_step("获取案卷列表...")
    volumes = client.get("/volumes?page=1&limit=10")
    if volumes.get("code") == 200:
        records = volumes.get("data", {}).get("records", [])
        log_info(f"现有案卷数: {len(records)}")

        if records:
            # 获取第一个案卷详情
            vol = records[0]
            vol_id = vol.get("id")
            log_step(f"获取案卷详情: {vol.get('volumeCode')}...")

            detail = client.get(f"/volumes/{vol_id}")
            if detail.get("code") == 200:
                v = detail.get("data", {})
                log_info(f"  案卷号: {v.get('volumeCode')}")
                log_info(f"  标题: {v.get('title')}")
                log_info(f"  文件数: {v.get('fileCount')}")
                log_info(f"  状态: {v.get('status')}")
                log_success("案卷详情获取成功")

            # 获取卷内文件
            log_step("获取卷内文件...")
            files = client.get(f"/volumes/{vol_id}/files")
            if files.get("code") == 200:
                file_list = files.get("data", [])
                log_info(f"  卷内文件数: {len(file_list)}")
                log_success("卷内文件获取成功")

            # 生成归档登记表
            log_step("生成归档登记表...")
            form = client.get(f"/volumes/{vol_id}/registration-form")
            if form.get("code") == 200:
                f = form.get("data", {})
                log_info(f"  登记编号: {f.get('registrationNo')}")
                log_success("归档登记表生成成功")
        else:
            log_warn("没有案卷数据")
    else:
        log_fail("案卷列表获取失败")


def test_warehouse(client: APIClient):
    """测试库房管理"""
    log_header("6. 库房管理")

    # 获取货架列表
    log_step("获取货架列表...")
    shelves = client.get("/warehouse/shelves")
    if shelves.get("code") == 200:
        shelf_list = shelves.get("data", [])
        log_info(f"货架数量: {len(shelf_list)}")
        for s in shelf_list[:3]:
            log_info(
                f"  - {s.get('code')}: {s.get('name')} ({s.get('usedCount')}/{s.get('capacity')})"
            )
        log_success("货架列表获取成功")
    else:
        log_fail("货架列表获取失败")

    # 获取环境监测数据
    log_step("获取环境监测数据...")
    env = client.get("/warehouse/environment")
    if env.get("code") == 200:
        data = env.get("data", {})
        log_info(f"  温度: {data.get('temperature')}°C")
        log_info(f"  湿度: {data.get('humidity')}%")
        log_success("环境监测数据获取成功")
    else:
        log_fail("环境监测数据获取失败")

    # 测试密集架控制
    if shelves.get("code") == 200 and shelves.get("data"):
        shelf_id = shelves["data"][0].get("id")
        log_step("测试密集架控制命令...")
        cmd_resp = client.post(f"/warehouse/racks/{shelf_id}/command", {"action": "OPEN"})
        if cmd_resp.get("code") == 200:
            log_success("密集架控制命令执行成功")
        else:
            log_warn("密集架控制命令执行失败")


def test_aip_export(client: APIClient):
    """测试 AIP 包导出"""
    log_header("7. AIP 包导出")

    # 获取已归档案卷
    volumes = client.get("/volumes?page=1&limit=10")
    archived_vol = None
    if volumes.get("code") == 200:
        for v in volumes.get("data", {}).get("records", []):
            if v.get("status") == "archived":
                archived_vol = v
                break

    if not archived_vol:
        log_warn("没有已归档的案卷，跳过 AIP 导出测试")
        return

    vol_id = archived_vol.get("id")
    vol_code = archived_vol.get("volumeCode")
    log_step(f"导出案卷 AIP 包: {vol_code}...")

    status, content, headers = client.get_file(f"/volumes/{vol_id}/export-aip")
    if status == 200 and len(content) > 0:
        log_info(f"  文件大小: {len(content)} bytes")
        log_info(f"  Content-Type: {headers.get('Content-Type', 'N/A')}")

        # 验证 ZIP 结构
        import zipfile
        import io

        try:
            with zipfile.ZipFile(io.BytesIO(content), "r") as zf:
                names = zf.namelist()
                log_info(f"  包内文件数: {len(names)}")
                for name in names[:5]:
                    log_info(f"    - {name}")
                log_success("AIP 包导出成功")
        except Exception as e:
            log_fail(f"AIP 包验证失败: {e}")
    else:
        log_fail(f"AIP 包导出失败 (HTTP {status})")


def test_relations(client: APIClient):
    """测试穿透联查"""
    log_header("8. 穿透联查")

    # 获取一个档案 ID
    archives = client.get("/archives?page=1&limit=1")
    if archives.get("code") != 200 or not archives.get("data", {}).get("records"):
        log_warn("没有档案数据，跳过穿透联查测试")
        return

    archive_id = archives["data"]["records"][0].get("id")

    # 测试关联文件
    log_step(f"获取关联文件 (ID: {archive_id[:16]}...)...")
    files = client.get(f"/relations/{archive_id}/files")
    if files.get("code") == 200:
        log_info(f"  关联文件数: {len(files.get('data', []))}")
        log_success("关联文件获取成功")
    else:
        log_warn("关联文件获取失败")

    # 测试合规信息
    log_step("获取合规信息...")
    compliance = client.get(f"/relations/{archive_id}/compliance")
    if compliance.get("code") == 200:
        log_success("合规信息获取成功")
    else:
        log_warn("合规信息获取失败")


def test_borrowing(client: APIClient):
    """测试借阅申请"""
    log_header("9. 借阅申请")

    # 获取借阅列表
    log_step("获取借阅列表...")
    list_resp = client.get("/borrowing?page=1&size=10")
    if list_resp.get("code") == 200:
        records = list_resp.get("data", {}).get("records", [])
        log_info(f"现有借阅记录: {len(records)}")
        log_success("借阅列表获取成功")
    else:
        log_fail("借阅列表获取失败")


def test_open_appraisal(client: APIClient):
    """测试开放鉴定"""
    log_header("10. 开放鉴定")

    # 获取鉴定列表
    log_step("获取开放鉴定列表...")
    list_resp = client.get("/open-appraisal/list?page=1&limit=10")
    if list_resp.get("code") == 200:
        data = list_resp.get("data", {})
        total = data.get("total", 0)
        log_info(f"现有鉴定记录: {total}")
        log_success("开放鉴定列表获取成功")
    else:
        log_fail("开放鉴定列表获取失败")


def test_destruction(client: APIClient):
    """测试销毁鉴定"""
    log_header("11. 销毁鉴定")

    # 获取销毁列表
    log_step("获取销毁鉴定列表...")
    list_resp = client.get("/destruction?page=1&limit=10")
    if list_resp.get("code") == 200:
        records = list_resp.get("data", {}).get("records", [])
        log_info(f"现有销毁记录: {len(records)}")
        log_success("销毁鉴定列表获取成功")
    else:
        log_fail("销毁鉴定列表获取失败")

    # 获取统计
    log_step("获取销毁统计...")
    stats = client.get("/destruction/stats")
    if stats.get("code") == 200:
        log_success("销毁统计获取成功")
    else:
        log_warn("销毁统计获取失败")


def test_archive_batch(client: APIClient):
    """测试归档批次"""
    log_header("12. 归档批次")

    # 获取批次列表
    log_step("获取归档批次列表...")
    list_resp = client.get("/archive-batch?page=1&size=10")
    if list_resp.get("code") == 200:
        data = list_resp.get("data", {})
        records = data.get("records", [])
        log_info(f"现有批次数: {len(records)}")
        for r in records[:3]:
            log_info(f"  - {r.get('batchNo')}: {r.get('status')}")
        log_success("归档批次列表获取成功")
    else:
        log_fail("归档批次列表获取失败")

    # 获取统计
    log_step("获取归档批次统计...")
    stats = client.get("/archive-batch/stats")
    if stats.get("code") == 200:
        log_success("归档批次统计获取成功")
    else:
        log_warn("归档批次统计获取失败")


def test_audit_log(client: APIClient):
    """测试审计日志"""
    log_header("13. 审计日志")

    log_step("获取审计日志...")
    logs = client.get("/audit-logs?page=1&limit=20")
    if logs.get("code") == 200:
        records = logs.get("data", {}).get("records", [])
        log_info(f"审计日志记录数: {len(records)}")
        for r in records[:5]:
            log_info(
                f"  - [{r.get('createdTime', '?')}] {r.get('action', '?')}: {r.get('resourceType', '?')}"
            )
        log_success("审计日志获取成功")
    else:
        log_fail("审计日志获取失败")


def test_panorama(client: APIClient):
    """测试全景视图"""
    log_header("14. 全景视图检索")

    # 搜索已归档凭证
    log_step("搜索已归档凭证...")
    resp = client.get("/archives?page=1&size=10&status=ARCHIVED")
    if resp.get("code") == 200:
        records = resp.get("data", {}).get("records", [])
        log_info(f"已归档凭证数: {len(records)}")
        for r in records[:3]:
            log_info(f"  - {r.get('archiveCode')}: {r.get('status')}")
        log_success("全景视图检索成功")
    else:
        log_fail("全景视图检索失败")


# ============================
# 主流程
# ============================
def print_summary():
    print()
    log_header("测试执行完成")
    print()
    print(f"  {GREEN}通过: {PASSED}{NC}")
    print(f"  {RED}失败: {FAILED}{NC}")
    print(f"  {YELLOW}警告: {WARNINGS}{NC}")
    print()

    if FAILED > 0:
        print(f"{RED}存在失败项，请检查相关功能{NC}")
        return 1
    elif WARNINGS > 0:
        print(f"{YELLOW}存在警告项，部分功能需要手动验证{NC}")
        return 0
    else:
        print(f"{GREEN}所有测试项通过！{NC}")
        return 0


def main():
    print()
    print(f"{BLUE}╔══════════════════════════════════════════════════════════╗{NC}")
    print(f"{BLUE}║     NexusArchive 端到端完整测试                          ║{NC}")
    print(f"{BLUE}║     {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}                                 ║{NC}")
    print(f"{BLUE}╚══════════════════════════════════════════════════════════╝{NC}")
    print()
    print(f"  BASE_URL: {BASE_URL}")
    print(f"  USERNAME: {USERNAME}")
    print()

    # 初始化客户端
    client = APIClient(BASE_URL)

    # 登录
    log_step("登录...")
    if not client.login(USERNAME, PASSWORD):
        log_fail("登录失败")
        return 1
    log_success("登录成功")

    # 执行测试
    file_ids = test_erp_sync(client)
    test_pool(client)
    test_archive_submit(client, file_ids or [])
    test_compliance(client, file_ids or [])
    test_volume(client)
    test_warehouse(client)
    test_aip_export(client)
    test_relations(client)
    test_borrowing(client)
    test_open_appraisal(client)
    test_destruction(client)
    test_archive_batch(client)
    test_audit_log(client)
    test_panorama(client)

    return print_summary()


if __name__ == "__main__":
    sys.exit(main())
