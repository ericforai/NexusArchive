#!/usr/bin/env python3
# 生成演示 PDF 发票文件
# 每个文件内容与数据库中的凭证信息匹配

from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import os

# 尝试注册中文字体
try:
    pdfmetrics.registerFont(TTFont('Heiti', '/System/Library/Fonts/STHeiti Light.ttc'))
    FONT_NAME = 'Heiti'
except:
    try:
        pdfmetrics.registerFont(TTFont('Heiti', '/System/Library/Fonts/PingFang.ttc'))
        FONT_NAME = 'Heiti'
    except:
        FONT_NAME = 'Helvetica'

def create_invoice_pdf(filename, voucher_no, summary, counterparty, amount, invoice_type="增值税普通发票"):
    """创建一个简单的发票样式 PDF"""
    c = canvas.Canvas(filename, pagesize=A4)
    width, height = A4
    
    # 标题
    c.setFont(FONT_NAME, 24)
    c.drawCentredString(width/2, height - 50*mm, invoice_type)
    
    # 发票号码
    c.setFont(FONT_NAME, 12)
    c.drawString(50*mm, height - 70*mm, f"发票号码: {voucher_no}")
    
    # 购买方信息框
    c.rect(30*mm, height - 130*mm, 150*mm, 50*mm)
    c.setFont(FONT_NAME, 10)
    c.drawString(35*mm, height - 90*mm, "购买方信息")
    c.drawString(35*mm, height - 100*mm, f"名称: {counterparty or '未指定'}")
    c.drawString(35*mm, height - 110*mm, "纳税人识别号: 91310000XXXXXXXX")
    
    # 商品明细表头
    y = height - 150*mm
    c.rect(30*mm, y - 40*mm, 150*mm, 40*mm)
    c.setFont(FONT_NAME, 10)
    c.drawString(35*mm, y - 10*mm, "商品名称")
    c.drawString(100*mm, y - 10*mm, "金额")
    
    # 商品明细
    c.drawString(35*mm, y - 25*mm, summary or "商品/服务")
    c.drawString(100*mm, y - 25*mm, f"¥ {amount:.2f}" if amount else "¥ 0.00")
    
    # 合计金额
    c.setFont(FONT_NAME, 14)
    c.drawString(35*mm, y - 60*mm, f"价税合计 (大写): ")
    c.setFont(FONT_NAME, 12)
    c.drawString(35*mm, y - 75*mm, f"(小写) ¥ {amount:.2f}" if amount else "(小写) ¥ 0.00")
    
    # 销售方信息
    c.rect(30*mm, y - 130*mm, 150*mm, 40*mm)
    c.setFont(FONT_NAME, 10)
    c.drawString(35*mm, y - 100*mm, "销售方信息")
    c.drawString(35*mm, y - 115*mm, "名称: 示例销售公司")
    
    # 底部备注
    c.setFont(FONT_NAME, 8)
    c.drawCentredString(width/2, 30*mm, f"演示发票 - {voucher_no}")
    
    c.save()
    print(f"Created: {filename}")

def create_bank_receipt_pdf(filename, voucher_no, summary, counterparty, amount):
    """创建一个简单的银行回单样式 PDF"""
    c = canvas.Canvas(filename, pagesize=A4)
    width, height = A4
    
    # 标题
    c.setFont(FONT_NAME, 20)
    c.drawCentredString(width/2, height - 40*mm, "银行转账回单")
    
    # 回单编号
    c.setFont(FONT_NAME, 12)
    c.drawString(50*mm, height - 60*mm, f"回单编号: {voucher_no}")
    
    # 交易信息框
    c.rect(30*mm, height - 150*mm, 150*mm, 80*mm)
    
    c.setFont(FONT_NAME, 10)
    y = height - 80*mm
    c.drawString(35*mm, y, f"付款人: 本公司账户")
    c.drawString(35*mm, y - 15*mm, f"收款人: {counterparty or '对方账户'}")
    c.drawString(35*mm, y - 30*mm, f"交易金额: ¥ {amount:.2f}" if amount else "交易金额: ¥ 0.00")
    c.drawString(35*mm, y - 45*mm, f"摘要: {summary or '转账'}")
    c.drawString(35*mm, y - 60*mm, "交易状态: 已完成")
    
    # 底部备注
    c.setFont(FONT_NAME, 8)
    c.drawCentredString(width/2, 30*mm, f"演示银行回单 - {voucher_no}")
    
    c.save()
    print(f"Created: {filename}")

# 目标目录
output_dir = "/Users/user/nexusarchive/nexusarchive-java/data/archives/uploads/demo"
os.makedirs(output_dir, exist_ok=True)

# 根据数据库中的凭证信息生成对应的 PDF 文件
demo_files = [
    # (文件名, 凭证编号, 摘要, 对手方, 金额, 类型)
    ("office_100.pdf", "OV-2025-TEST-01", "办公用品采购", "京东办公", 100.00, "invoice"),
    ("food_100.pdf", "OV-2025-TEST-02", "客户接待", "海底捞餐饮", 100.00, "invoice"),
    ("bank_45k.pdf", "OV-2024-BANK-45K", "服务费收款", "某大客户", 45000.00, "bank"),
    ("invoice_50.pdf", "OV-2025-INV-004", "销售服务费发票", "Test Customer", 50.00, "invoice"),
    ("bank_50.pdf", "OV-2025-BANK-004", "服务费收款回单", "Test Customer", 50.00, "bank"),
    ("invoice_30.pdf", "OV-2025-INV-003", "商品销售发票", "Test Customer", 30.00, "invoice"),
    ("上海米山神鸡餐饮管理有限公司_发票金额201.00元.pdf", "25312000000350552461", "餐饮服务", "上海米山神鸡餐饮管理有限公司", 201.00, "invoice"),
]

for file_info in demo_files:
    filename, voucher_no, summary, counterparty, amount, file_type = file_info
    filepath = os.path.join(output_dir, filename)
    
    if file_type == "bank":
        create_bank_receipt_pdf(filepath, voucher_no, summary, counterparty, amount)
    else:
        create_invoice_pdf(filepath, voucher_no, summary, counterparty, amount)

print("\nAll demo PDF files created successfully!")
