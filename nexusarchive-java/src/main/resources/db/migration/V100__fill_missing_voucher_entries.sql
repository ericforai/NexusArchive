-- V100: 为现有凭证补充会计分录数据
-- 解决全景视图显示"暂无分录数据"的问题
--
-- 注意：使用 pg_column_size() 检测空 JSONB 值

-- Helper function: 先设置 NULL 再更新（避免空 JSONB 值问题）
-- 策略：对于空值，先设为 NULL，再更新为目标值

-- voucher-2024-09-001: 购买固定资产-服务器
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-09-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 185000, "credit_org": 0, "accsubject": {"code": "1601", "name": "固定资产"}, "description": "购买服务器设备"}, {"id": "2", "debit_org": 0, "credit_org": 185000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付设备款"}]'::jsonb WHERE id = 'voucher-2024-09-001' AND custom_metadata IS NULL;

-- voucher-2024-09-002: 收到销售佣金收入
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-09-002' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 68000, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到佣金收入"}, {"id": "2", "debit_org": 0, "credit_org": 64150.94, "accsubject": {"code": "6051", "name": "其他业务收入"}, "description": "佣金收入"}, {"id": "3", "debit_org": 0, "credit_org": 3849.06, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额6%"}]'::jsonb WHERE id = 'voucher-2024-09-002' AND custom_metadata IS NULL;

-- voucher-2024-10-001: 支付房租及物业费
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-10-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 72000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-租赁费"}, "description": "办公室租金"}, {"id": "2", "debit_org": 13000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-物业费"}, "description": "物业费"}, {"id": "3", "debit_org": 0, "credit_org": 85000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付房租物业费"}]'::jsonb WHERE id = 'voucher-2024-10-001' AND custom_metadata IS NULL;

-- voucher-2024-10-002: 采购原材料入库
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-10-002' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 414159.29, "credit_org": 0, "accsubject": {"code": "1403", "name": "原材料"}, "description": "原材料采购入库"}, {"id": "2", "debit_org": 53840.71, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额13%"}, {"id": "3", "debit_org": 0, "credit_org": 468000, "accsubject": {"code": "2202", "name": "应付账款"}, "description": "应付供应商货款"}]'::jsonb WHERE id = 'voucher-2024-10-002' AND custom_metadata IS NULL;

-- voucher-2024-10-003: 计提本月工资
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-10-003' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 450000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-工资薪金"}, "description": "管理部门工资"}, {"id": "2", "debit_org": 320000, "credit_org": 0, "accsubject": {"code": "6601", "name": "销售费用-工资薪金"}, "description": "销售部门工资"}, {"id": "3", "debit_org": 72000, "credit_org": 0, "accsubject": {"code": "5101", "name": "生产成本-直接人工"}, "description": "生产部门工资"}, {"id": "4", "debit_org": 0, "credit_org": 842000, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "description": "计提本月工资"}]'::jsonb WHERE id = 'voucher-2024-10-003' AND custom_metadata IS NULL;

-- voucher-2024-10-004: 支付水电费
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-10-004' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 22000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-水电费"}, "description": "电费"}, {"id": "2", "debit_org": 6600, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-水电费"}, "description": "水费"}, {"id": "3", "debit_org": 0, "credit_org": 28600, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付水电费"}]'::jsonb WHERE id = 'voucher-2024-10-004' AND custom_metadata IS NULL;

-- voucher-2024-10-005: 结转本月成本
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-10-005' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 380000, "credit_org": 0, "accsubject": {"code": "6401", "name": "主营业务成本"}, "description": "结转销售成本"}, {"id": "2", "debit_org": 0, "credit_org": 380000, "accsubject": {"code": "1405", "name": "库存商品"}, "description": "结转库存商品"}]'::jsonb WHERE id = 'voucher-2024-10-005' AND custom_metadata IS NULL;

-- voucher-mfg-2024-11-001: 生产材料领用
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-mfg-2024-11-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 156000, "credit_org": 0, "accsubject": {"code": "5101", "name": "生产成本-直接材料"}, "description": "生产领用原材料"}, {"id": "2", "debit_org": 0, "credit_org": 156000, "accsubject": {"code": "1403", "name": "原材料"}, "description": "原材料出库"}]'::jsonb WHERE id = 'voucher-mfg-2024-11-001' AND custom_metadata IS NULL;

-- voucher-2024-11-004: 支付阿里云服务器费用
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-11-004' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 12075.47, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-服务费"}, "description": "阿里云服务器费用"}, {"id": "2", "debit_org": 724.53, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额6%"}, {"id": "3", "debit_org": 0, "credit_org": 12800, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付云服务费"}]'::jsonb WHERE id = 'voucher-2024-11-004' AND custom_metadata IS NULL;

-- voucher-2024-11-005: 计提本月工资
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-11-005' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 460000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-工资薪金"}, "description": "管理部门工资"}, {"id": "2", "debit_org": 330000, "credit_org": 0, "accsubject": {"code": "6601", "name": "销售费用-工资薪金"}, "description": "销售部门工资"}, {"id": "3", "debit_org": 66000, "credit_org": 0, "accsubject": {"code": "5101", "name": "生产成本-直接人工"}, "description": "生产部门工资"}, {"id": "4", "debit_org": 0, "credit_org": 856000, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "description": "计提本月工资"}]'::jsonb WHERE id = 'voucher-2024-11-005' AND custom_metadata IS NULL;

-- voucher-2024-11-006: 固定资产折旧计提
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-11-006' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 18000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-折旧费"}, "description": "管理部门折旧"}, {"id": "2", "debit_org": 15000, "credit_org": 0, "accsubject": {"code": "6601", "name": "销售费用-折旧费"}, "description": "销售部门折旧"}, {"id": "3", "debit_org": 12600, "credit_org": 0, "accsubject": {"code": "5101", "name": "生产成本-制造费用"}, "description": "生产部门折旧"}, {"id": "4", "debit_org": 0, "credit_org": 45600, "accsubject": {"code": "1602", "name": "累计折旧"}, "description": "计提本月折旧"}]'::jsonb WHERE id = 'voucher-2024-11-006' AND custom_metadata IS NULL;

-- voucher-2024-11-007: 支付供应商货款
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-11-007' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 286500, "credit_org": 0, "accsubject": {"code": "2202", "name": "应付账款"}, "description": "支付供应商货款"}, {"id": "2", "debit_org": 0, "credit_org": 286500, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}]'::jsonb WHERE id = 'voucher-2024-11-007' AND custom_metadata IS NULL;

-- voucher-2024-11-008: 销售商品确认收入
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-11-008' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 520000, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到销售款"}, {"id": "2", "debit_org": 0, "credit_org": 460176.99, "accsubject": {"code": "6001", "name": "主营业务收入"}, "description": "产品销售收入"}, {"id": "3", "debit_org": 0, "credit_org": 59823.01, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额13%"}]'::jsonb WHERE id = 'voucher-2024-11-008' AND custom_metadata IS NULL;

-- voucher-sales-2024-11-001: 销售产品收入
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-sales-2024-11-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 1280000, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到大额销售款"}, {"id": "2", "debit_org": 0, "credit_org": 1132743.36, "accsubject": {"code": "6001", "name": "主营业务收入"}, "description": "产品销售收入"}, {"id": "3", "debit_org": 0, "credit_org": 167256.64, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额13%"}]'::jsonb WHERE id = 'voucher-sales-2024-11-001' AND custom_metadata IS NULL;

-- voucher-trade-2024-11-001: 进口设备采购
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-trade-2024-11-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 820000, "credit_org": 0, "accsubject": {"code": "1601", "name": "固定资产"}, "description": "进口设备入库"}, {"id": "2", "debit_org": 40000, "credit_org": 0, "accsubject": {"code": "6401", "name": "销售费用-关税"}, "description": "进口关税"}, {"id": "3", "debit_org": 0, "credit_org": 860000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付进口设备款"}]'::jsonb WHERE id = 'voucher-trade-2024-11-001' AND custom_metadata IS NULL;

-- voucher-2024-12-001: 支付年终审计费
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-12-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 83018.87, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-审计费"}, "description": "年终审计服务费"}, {"id": "2", "debit_org": 4981.13, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额6%"}, {"id": "3", "debit_org": 0, "credit_org": 88000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付审计费"}]'::jsonb WHERE id = 'voucher-2024-12-001' AND custom_metadata IS NULL;

-- voucher-2024-12-002: 年终奖金计提
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2024-12-002' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 1200000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-奖金"}, "description": "管理部门年终奖"}, {"id": "2", "debit_org": 900000, "credit_org": 0, "accsubject": {"code": "6601", "name": "销售费用-奖金"}, "description": "销售部门年终奖"}, {"id": "3", "debit_org": 480000, "credit_org": 0, "accsubject": {"code": "5101", "name": "生产成本-奖金"}, "description": "生产部门年终奖"}, {"id": "4", "debit_org": 0, "credit_org": 2580000, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "description": "计提年终奖金"}]'::jsonb WHERE id = 'voucher-2024-12-002' AND custom_metadata IS NULL;

-- voucher-2025-01-001: 支付员工年终奖
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2025-01-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 2580000, "credit_org": 0, "accsubject": {"code": "2211", "name": "应付职工薪酬"}, "description": "发放年终奖金"}, {"id": "2", "debit_org": 0, "credit_org": 2580000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行代发工资"}]'::jsonb WHERE id = 'voucher-2025-01-001' AND custom_metadata IS NULL;

-- voucher-2025-02-001: 支付供应商货款
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2025-02-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 125600, "credit_org": 0, "accsubject": {"code": "2202", "name": "应付账款"}, "description": "支付供应商货款"}, {"id": "2", "debit_org": 0, "credit_org": 125600, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行付款"}]'::jsonb WHERE id = 'voucher-2025-02-001' AND custom_metadata IS NULL;

-- voucher-2025-02-002: 销售收入确认
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'voucher-2025-02-002' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 358000, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到销售款"}, {"id": "2", "debit_org": 0, "credit_org": 316814.16, "accsubject": {"code": "6001", "name": "主营业务收入"}, "description": "产品销售收入"}, {"id": "3", "debit_org": 0, "credit_org": 41185.84, "accsubject": {"code": "2221.01", "name": "应交税费-应交增值税(销项)"}, "description": "销项税额13%"}]'::jsonb WHERE id = 'voucher-2025-02-002' AND custom_metadata IS NULL;

-- demo-reimb-jz-001: 记账凭证-差旅费报销
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'demo-reimb-jz-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 1800, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-差旅费"}, "description": "住宿费", "aux_info": "员工: 张三"}, {"id": "2", "debit_org": 1080, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-差旅费"}, "description": "交通费-高铁", "aux_info": "员工: 张三"}, {"id": "3", "debit_org": 400, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-差旅费"}, "description": "餐饮补贴", "aux_info": "员工: 张三"}, {"id": "4", "debit_org": 0, "credit_org": 3280, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "报销转账付款"}]'::jsonb WHERE id = 'demo-reimb-jz-001' AND custom_metadata IS NULL;

-- demo-purchase-jz-001: 记账凭证-设备采购
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'demo-purchase-jz-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 398230.09, "credit_org": 0, "accsubject": {"code": "1601", "name": "固定资产"}, "description": "服务器设备采购"}, {"id": "2", "debit_org": 51769.91, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额13%"}, {"id": "3", "debit_org": 0, "credit_org": 450000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付设备款"}]'::jsonb WHERE id = 'demo-purchase-jz-001' AND custom_metadata IS NULL;

-- demo-office-jz-001: 记账凭证-办公用品
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'demo-office-jz-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 2283.19, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-办公费"}, "description": "办公用品采购"}, {"id": "2", "debit_org": 296.81, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额13%"}, {"id": "3", "debit_org": 0, "credit_org": 2580, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付办公款"}]'::jsonb WHERE id = 'demo-office-jz-001' AND custom_metadata IS NULL;

-- demo-service-jz-001: 记账凭证-审计服务费
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'demo-service-jz-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 84905.66, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用-审计费"}, "description": "季度审计服务费"}, {"id": "2", "debit_org": 5094.34, "credit_org": 0, "accsubject": {"code": "2221.02", "name": "应交税费-应交增值税(进项)"}, "description": "进项税额6%"}, {"id": "3", "debit_org": 0, "credit_org": 90000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付审计费"}]'::jsonb WHERE id = 'demo-service-jz-001' AND custom_metadata IS NULL;

-- arc-2024-001: 2024年1月会计凭证01
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'arc-2024-001' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 150000, "credit_org": 0, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "收到注册资本"}, {"id": "2", "debit_org": 0, "credit_org": 150000, "accsubject": {"code": "4001", "name": "实收资本"}, "description": "股东投资"}]'::jsonb WHERE id = 'arc-2024-001' AND custom_metadata IS NULL;

-- arc-2024-002: 2025年1月会计凭证02
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'arc-2024-002' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 50000, "credit_org": 0, "accsubject": {"code": "1601", "name": "固定资产"}, "description": "购买办公设备"}, {"id": "2", "debit_org": 0, "credit_org": 50000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付设备款"}]'::jsonb WHERE id = 'arc-2024-002' AND custom_metadata IS NULL;

-- arc-2024-003: 2024年3月会计凭证03
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'arc-2024-003' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 80000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用"}, "description": "发放员工工资"}, {"id": "2", "debit_org": 0, "credit_org": 80000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "银行代发工资"}]'::jsonb WHERE id = 'arc-2024-003' AND custom_metadata IS NULL;

-- arc-2024-004: 2024年4月会计凭证04
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'arc-2024-004' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 12000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用"}, "description": "办公室租金"}, {"id": "2", "debit_org": 0, "credit_org": 12000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付房租"}]'::jsonb WHERE id = 'arc-2024-004' AND custom_metadata IS NULL;

-- arc-2024-005: 2024年5月会计凭证05
UPDATE acc_archive SET custom_metadata = NULL WHERE id = 'arc-2024-005' AND pg_column_size(custom_metadata) = 0;
UPDATE acc_archive SET custom_metadata = '[{"id": "1", "debit_org": 30000, "credit_org": 0, "accsubject": {"code": "6602", "name": "管理费用"}, "description": "电信服务费"}, {"id": "2", "debit_org": 0, "credit_org": 30000, "accsubject": {"code": "1002", "name": "银行存款"}, "description": "支付电信费"}]'::jsonb WHERE id = 'arc-2024-005' AND custom_metadata IS NULL;

-- 完成
