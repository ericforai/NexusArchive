from pathlib import Path

import pdfplumber
import pypdfium2 as pdfium
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import KeepInFrame, Paragraph, SimpleDocTemplate, Spacer


ROOT = Path("/Users/user/nexusarchive")
OUTPUT_PDF = ROOT / "output/pdf/nexusarchive-app-summary.pdf"
PREVIEW_PNG = ROOT / "tmp/pdfs/nexusarchive-app-summary-page-1.png"


def register_fonts() -> None:
    pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))


def styles():
    base = getSampleStyleSheet()
    title = ParagraphStyle(
        "TitleCN",
        parent=base["Title"],
        fontName="STSong-Light",
        fontSize=22,
        leading=27,
        textColor=colors.HexColor("#0f172a"),
        spaceAfter=5.5 * mm,
        alignment=TA_LEFT,
    )
    section = ParagraphStyle(
        "SectionCN",
        parent=base["Heading2"],
        fontName="STSong-Light",
        fontSize=12,
        leading=14.5,
        textColor=colors.HexColor("#0b5cab"),
        spaceBefore=2.4 * mm,
        spaceAfter=1.4 * mm,
    )
    body = ParagraphStyle(
        "BodyCN",
        parent=base["BodyText"],
        fontName="STSong-Light",
        fontSize=9.4,
        leading=12.2,
        textColor=colors.HexColor("#1f2937"),
        allowWidows=1,
        allowOrphans=1,
    )
    bullet = ParagraphStyle(
        "BulletCN",
        parent=body,
        leftIndent=11,
        firstLineIndent=-6.5,
        bulletIndent=0,
        spaceBefore=0.7 * mm,
    )
    note = ParagraphStyle(
        "NoteCN",
        parent=body,
        fontSize=8.2,
        leading=10.3,
        textColor=colors.HexColor("#475569"),
    )
    return title, section, body, bullet, note


def build_story():
    title, section, body, bullet, note = styles()
    story = []

    story.append(Paragraph("NexusArchive 应用摘要", title))
    story.append(
        Paragraph(
            "基于仓库内 README、前后端入口、路由/API/配置与启动脚本整理。仅使用仓库证据，不补充仓库外信息。",
            note,
        )
    )
    story.append(Spacer(1, 2.5 * mm))

    sections = [
        (
            "What It Is",
            [
                "NexusArchive 是一个面向企业私有化部署的电子会计档案系统，覆盖电子凭证采集、预归档、正式归档、利用与审计相关流程。",
                "仓库文档声明其遵循 DA/T 94-2022；代码层面可见前端 React/Vite 与后端 Spring Boot/MyBatis 的前后端分离实现。",
            ],
        ),
        (
            "Who It’s For",
            [
                "主要面向企业财务档案团队与档案管理员；仓库内角色/菜单/借阅/审批/审计配置也表明系统服务于业务操作员、系统管理员、安全保密员与审计相关岗位。",
            ],
        ),
        (
            "What It Does",
            [
                "预归档库管理，覆盖凭证池、单据池、OCR、异常数据与自动/手动关联。",
                "资料收集能力，包含在线接收、批量上传、移动扫描上传与外部系统同步入口。",
                "档案管理与作业流，包含档案列表、详情、组卷、审批、批次、开放鉴定与销毁流程。",
                "利用能力，包含关系查询、借阅流程、库房管理与档案全景查看。",
                "统计与质量视图，提供 dashboard、趋势、存储与代码质量监控入口。",
                "系统设置与安全能力，包含用户/角色、MFA、审计、集成、授权票据与 License 激活页。",
            ],
        ),
        (
            "How It Works",
            [
                "前端: `src/index.tsx` 用 React Query + `App.tsx` 路由入口挂载应用；`src/routes/index.tsx` 组织公开首页与 `/system` 业务路由；`src/api/client.ts` 统一注入 Bearer Token 与 `X-Fonds-No`。",
                "接口流: Vite 将 `/api` 代理到后端目标地址，前端 API 模块按领域拆分到 `src/api/*.ts`。",
                "后端: `NexusArchiveApplication` 启动 Spring Boot 应用，`application.yml` 显示上下文路径为 `/api`、启用 Flyway、Redis、MyBatis-Plus 与异步任务。",
                "数据流: Controller 接收请求后调用 Service，再经 Mapper 访问 PostgreSQL；例如统计链路可见 `StatsController -> StatsServiceImpl -> Mapper`。",
                "存储: 结构化数据落 PostgreSQL，Redis 用于缓存/运行时支持，档案与临时文件分别写入 `archive.root.path` 与 `archive.temp.path`；生产环境用 Nginx 托管前端、Docker Compose 编排前后端与基础设施。",
            ],
        ),
        (
            "How To Run",
            [
                "安装依赖: `npm install`。",
                "准备运行环境: 机器需可执行 `docker-compose`、`mvn` 与 `npm`；`scripts/dev.sh` 会在缺失时从 `.env.example` 创建 `.env.local`。",
                "启动开发环境: `npm run dev`。",
                "访问地址: 前端 `http://localhost:15175`，后端 `http://localhost:19090/api`。",
                "停止环境: `npm run dev:stop`。",
            ],
        ),
    ]

    for heading, items in sections:
        story.append(Paragraph(heading, section))
        for item in items:
            story.append(Paragraph(item, bullet, bulletText="•"))
        story.append(Spacer(1, 1.2 * mm))

    story.append(
        Paragraph(
            "Not found in repo: 单独的正式“目标客户”定义页、精确商业版定价/版本矩阵。",
            note,
        )
    )
    return story


def generate_pdf() -> None:
    register_fonts()
    OUTPUT_PDF.parent.mkdir(parents=True, exist_ok=True)

    doc = SimpleDocTemplate(
        str(OUTPUT_PDF),
        pagesize=A4,
        leftMargin=13 * mm,
        rightMargin=13 * mm,
        topMargin=12 * mm,
        bottomMargin=12 * mm,
    )

    max_width = A4[0] - doc.leftMargin - doc.rightMargin
    max_height = A4[1] - doc.topMargin - doc.bottomMargin
    frame_story = [KeepInFrame(max_width, max_height, build_story(), mode="shrink")]
    doc.build(frame_story)


def verify_pdf() -> None:
    reader = PdfReader(str(OUTPUT_PDF))
    if len(reader.pages) != 1:
        raise SystemExit(f"Expected 1 page, got {len(reader.pages)}")

    with pdfplumber.open(str(OUTPUT_PDF)) as pdf:
        text = (pdf.pages[0].extract_text() or "").strip()
        if "NexusArchive" not in text or "How To Run" not in text:
            raise SystemExit("PDF text verification failed")


def render_preview() -> None:
    pdf = pdfium.PdfDocument(str(OUTPUT_PDF))
    page = pdf[0]
    bitmap = page.render(scale=2.0)
    pil_image = bitmap.to_pil()
    PREVIEW_PNG.parent.mkdir(parents=True, exist_ok=True)
    pil_image.save(PREVIEW_PNG)


if __name__ == "__main__":
    generate_pdf()
    verify_pdf()
    render_preview()
    print(OUTPUT_PDF)
    print(PREVIEW_PNG)
