# 第三阶段：私有化部署产品化改造方案

## 概述

针对私有化部署系统的特点，第三阶段将重点围绕以下三个维度进行产品化改造：

1. **用户体验优化**：为非技术用户提供直观、易用的管理界面
2. **自动化运维**：建立完善的自动化测试、部署和运维体系
3. **交付与实施**：设计适合私有化部署的交付流程和工具

## 第一部分：用户体验优化（3-4周）

### 1. 管理界面重新设计

#### 1.1 系统管理控制台
创建一个专门面向非技术用户的管理控制台，提供图形化的系统配置和监控界面：

```java
// 控制台控制器
@RestController
@RequestMapping("/api/admin/console")
@RequiredArgsConstructor
public class AdminConsoleController {
    
    private final SystemMonitorService systemMonitorService;
    private final SystemConfigurationService configurationService;
    
    /**
     * 系统概览仪表板
     */
    @GetMapping("/dashboard")
    public ResponseEntity<SystemDashboard> getSystemDashboard() {
        SystemDashboard dashboard = systemMonitorService.getSystemDashboard();
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * 系统配置管理
     */
    @GetMapping("/config")
    public ResponseEntity<SystemConfiguration> getSystemConfiguration() {
        SystemConfiguration config = configurationService.getCurrentConfiguration();
        return ResponseEntity.ok(config);
    }
    
    @PutMapping("/config")
    public ResponseEntity<Void> updateSystemConfiguration(@RequestBody SystemConfiguration config) {
        configurationService.updateConfiguration(config);
        return ResponseEntity.ok().build();
    }
}
```

#### 1.2 档案管理简化界面
为非技术用户提供简化的档案管理界面：

```typescript
// React组件：简化档案管理界面
interface SimplifiedArchiveViewProps {}

const SimplifiedArchiveView: React.FC<SimplifiedArchiveViewProps> = () => {
  const [activeTab, setActiveTab] = useState('ingest');
  const [archives, setArchives] = useState<Archive[]>([]);
  const [loading, setLoading] = useState(false);
  
  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      {/* 步骤导航 */}
      <div className="mb-6">
        <Steps current={getCurrentStep()} className="mb-6">
          <Step title="档案导入" description="上传凭证和附件" />
          <Step title="自动关联" description="系统自动关联凭证和附件" />
          <Step title="合规检查" description="执行四性检测和符合性检查" />
          <Step title="归档完成" description="档案正式归档" />
        </Steps>
      </div>
      
      {/* 内容区域 */}
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="档案导入" key="ingest">
          <ArchiveIngestWizard onIngestComplete={handleIngestComplete} />
        </TabPane>
        <TabPane tab="待处理档案" key="pending">
          <PendingArchivesTable archives={pendingArchives} />
        </TabPane>
        <TabPane tab="已归档档案" key="archived">
          <ArchivedArchivesTable archives={archivedArchives} />
        </TabPane>
        <TabPane tab="合规检查" key="compliance">
          <ComplianceCheckResults archives={complianceResults} />
        </TabPane>
      </Tabs>
    </div>
  );
};
```

### 2. 智能引导工作流

#### 2.1 首次部署引导
为首次部署提供分步骤引导：

```java
@Service
@RequiredArgsConstructor
public class DeploymentGuideService {
    
    private final SystemConfigurationService configurationService;
    private final UserService userService;
    
    /**
     * 获取部署引导步骤
     */
    public List<DeploymentStep> getDeploymentGuideSteps() {
        List<DeploymentStep> steps = new ArrayList<>();
        
        // 步骤1：基础配置
        steps.add(DeploymentStep.builder()
            .stepNumber(1)
            .title("基础配置")
            .description("配置系统基本信息和存储路径")
            .completed(isBasicConfigCompleted())
            .action("/admin/setup/basic")
            .build());
            
        // 步骤2：用户管理
        steps.add(DeploymentStep.builder()
            .stepNumber(2)
            .title("用户管理")
            .description("创建管理员账号和初始用户")
            .completed(isUserSetupCompleted())
            .action("/admin/setup/users")
            .build());
            
        // 步骤3：部门组织
        steps.add(DeploymentStep.builder()
            .stepNumber(3)
            .title("部门组织")
            .description("设置组织架构和部门")
            .completed(isOrganizationSetupCompleted())
            .action("/admin/setup/organization")
            .build());
            
        // 步骤4：全宗设置
        steps.add(DeploymentStep.builder()
            .stepNumber(4)
            .title("全宗设置")
            .description("设置档案全宗和分类")
            .completed(isFondsSetupCompleted())
            .action("/admin/setup/fonds")
            .build());
            
        // 步骤5：ERP集成
        steps.add(DeploymentStep.builder()
            .stepNumber(5)
            .title("ERP集成")
            .description("配置与ERP系统的集成参数")
            .completed(isErpIntegrationCompleted())
            .action("/admin/setup/erp")
            .build());
            
        // 步骤6：系统检查
        steps.add(DeploymentStep.builder()
            .stepNumber(6)
            .title("系统检查")
            .description("执行系统检查和测试")
            .completed(isSystemCheckCompleted())
            .action("/admin/setup/check")
            .build());
            
        return steps;
    }
}
```

#### 2.2 业务流程引导
为非技术用户提供业务流程引导：

```typescript
// React组件：业务流程引导
interface BusinessProcessGuideProps {}

const BusinessProcessGuide: React.FC<BusinessProcessGuideProps> = () => {
  const [currentProcess, setCurrentProcess] = useState<BusinessProcess>();
  const [step, setStep] = useState(0);
  
  const processes: BusinessProcess[] = [
    {
      id: 'voucher-archive',
      title: '凭证归档',
      description: '将电子凭证进行整理、关联和归档',
      steps: [
        { title: '导入凭证', description: '从ERP系统导入电子凭证' },
        { title: '上传附件', description: '上传与凭证相关的附件文件' },
        { title: '自动关联', description: '系统自动关联凭证与附件' },
        { title: '合规检查', description: '执行四性检测和符合性检查' },
        { title: '正式归档', description: '将档案正式归档' }
      ]
    },
    {
      id: 'archive-borrow',
      title: '档案借阅',
      description: '申请、审批和归还档案',
      steps: [
        { title: '申请借阅', description: '选择需要借阅的档案' },
        { title: '审批流程', description: '等待管理员审批' },
        { title: '借阅使用', description: '查看和使用借阅的档案' },
        { title: '归还档案', description: '确认归还档案' }
      ]
    },
    {
      id: 'archive-transfer',
      title: '档案移交',
      description: '将档案移交给其他部门或组织',
      steps: [
        { title: '选择档案', description: '选择需要移交的档案' },
        { title: '指定接收方', description: '指定接收部门或组织' },
        { title: '移交审核', description: '等待移交审核' },
        { title: '完成移交', description: '确认移交完成' }
      ]
    }
  ];
  
  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      <h2 className="text-lg font-semibold mb-4">业务流程引导</h2>
      
      {/* 流程选择 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        {processes.map(process => (
          <div 
            key={process.id}
            className={`border rounded-lg p-4 cursor-pointer transition-colors ${
              currentProcess?.id === process.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200'
            }`}
            onClick={() => {
              setCurrentProcess(process);
              setStep(0);
            }}
          >
            <h3 className="font-medium mb-2">{process.title}</h3>
            <p className="text-sm text-gray-600">{process.description}</p>
          </div>
        ))}
      </div>
      
      {/* 步骤详情 */}
      {currentProcess && (
        <div className="border rounded-lg p-4">
          <div className="flex items-center mb-4">
            <h3 className="text-lg font-medium mr-4">{currentProcess.title}</h3>
            <Steps current={step} size="small">
              {currentProcess.steps.map((s, index) => (
                <Step key={index} title={s.title} />
              ))}
            </Steps>
          </div>
          
          <div className="mb-4">
            <h4 className="font-medium mb-2">
              步骤 {step + 1}: {currentProcess.steps[step].title}
            </h4>
            <p className="text-gray-600 mb-4">{currentProcess.steps[step].description}</p>
          </div>
          
          <div className="flex justify-between">
            <Button 
              disabled={step === 0} 
              onClick={() => setStep(step - 1)}
            >
              上一步
            </Button>
            <Button 
              type="primary" 
              onClick={() => {
                if (step < currentProcess.steps.length - 1) {
                  setStep(step + 1);
                }
              }}
            >
              {step === currentProcess.steps.length - 1 ? '完成' : '下一步'}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
```

### 3. 模板化配置

#### 3.1 行业模板
提供不同行业的预配置模板：

```java
@Service
@RequiredArgsConstructor
public class IndustryTemplateService {
    
    private final SystemConfigurationService configurationService;
    private final FondsService fondsService;
    
    /**
     * 应用行业模板
     */
    @Transactional
    public void applyIndustryTemplate(IndustryTemplateType templateType) {
        switch (templateType) {
            case FINANCE:
                applyFinanceTemplate();
                break;
            case MANUFACTURING:
                applyManufacturingTemplate();
                break;
            case GOVERNMENT:
                applyGovernmentTemplate();
                break;
            case HEALTHCARE:
                applyHealthcareTemplate();
                break;
            default:
                throw new IllegalArgumentException("不支持的行业模板类型");
        }
    }
    
    /**
     * 应用金融行业模板
     */
    private void applyFinanceTemplate() {
        // 创建金融行业专用全宗
        createFonds("金融行业全宗", "FIN", "适用于银行、证券、保险等金融机构");
        
        // 设置金融行业专用档案分类
        createArchiveCategories("金融行业");
        
        // 设置金融行业专用保管期限
        createRetentionPeriods("金融行业");
        
        // 配置金融行业专用合规规则
        configureComplianceRules("金融行业");
        
        // 设置金融行业专用工作流
        configureWorkflows("金融行业");
    }
    
    /**
     * 应用制造业模板
     */
    private void applyManufacturingTemplate() {
        // 创建制造业专用全宗
        createFonds("制造业全宗", "MFG", "适用于各类制造企业");
        
        // 设置制造业专用档案分类
        createArchiveCategories("制造业");
        
        // 设置制造业专用保管期限
        createRetentionPeriods("制造业");
        
        // 配置制造业专用合规规则
        configureComplianceRules("制造业");
        
        // 设置制造业专用工作流
        configureWorkflows("制造业");
    }
}
```

#### 3.2 企业规模模板
提供不同企业规模的预配置模板：

```java
@Service
@RequiredArgsConstructor
public class EnterpriseScaleTemplateService {
    
    /**
     * 应用企业规模模板
     */
    @Transactional
    public void applyEnterpriseScaleTemplate(EnterpriseScaleType scaleType) {
        switch (scaleType) {
            case SMALL:
                applySmallEnterpriseTemplate();
                break;
            case MEDIUM:
                applyMediumEnterpriseTemplate();
                break;
            case LARGE:
                applyLargeEnterpriseTemplate();
                break;
            case GROUP:
                applyGroupEnterpriseTemplate();
                break;
            default:
                throw new IllegalArgumentException("不支持的企业规模类型");
        }
    }
    
    /**
     * 应用小型企业模板
     */
    private void applySmallEnterpriseTemplate() {
        // 简化组织架构
        createSimpleOrganizationStructure();
        
        // 减少审批层级
        configureSimpleApprovalFlow();
        
        // 降低系统资源要求
        configureLowResourceSettings();
    }
    
    /**
     * 应用集团企业模板
     */
    private void applyGroupEnterpriseTemplate() {
        // 复杂组织架构
        createComplexOrganizationStructure();
        
        // 多级审批流程
        configureMultiLevelApprovalFlow();
        
        // 跨部门协作
        enableCrossDepartmentCollaboration();
        
        // 高级权限管理
        configureAdvancedPermissionManagement();
    }
}
```

## 第二部分：自动化运维（3-4周）

### 1. 系统健康监控

#### 1.1 自动健康检查
实现全面的系统健康检查机制：

```java
@Component
@RequiredArgsConstructor
public class SystemHealthMonitor {
    
    private final SystemHealthCheckService healthCheckService;
    private final NotificationService notificationService;
    
    /**
     * 定时健康检查
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void performScheduledHealthCheck() {
        SystemHealthReport report = healthCheckService.performFullHealthCheck();
        
        // 检查是否有严重问题
        if (report.hasCriticalIssues()) {
            // 发送告警通知
            notificationService.sendSystemAlert(
                "系统健康检查发现严重问题",
                report.getCriticalIssuesSummary(),
                NotificationPriority.HIGH
            );
            
            // 记录到系统日志
            log.error("系统健康检查发现严重问题: {}", report.getCriticalIssuesSummary());
        }
        
        // 保存健康检查报告
        healthCheckService.saveHealthReport(report);
    }
    
    /**
     * 系统健康检查服务
     */
    @Service
    @RequiredArgsConstructor
    public static class SystemHealthCheckService {
        
        private final DataSource dataSource;
        private final RedisTemplate<String, Object> redisTemplate;
        private final FileStorageService fileStorageService;
        
        /**
         * 执行全面健康检查
         */
        public SystemHealthReport performFullHealthCheck() {
            SystemHealthReport.Builder reportBuilder = SystemHealthReport.builder();
            
            // 数据库连接检查
            checkDatabaseConnection(reportBuilder);
            
            // Redis连接检查
            checkRedisConnection(reportBuilder);
            
            // 文件存储检查
            checkFileStorage(reportBuilder);
            
            // 系统资源检查
            checkSystemResources(reportBuilder);
            
            // 业务逻辑检查
            checkBusinessLogic(reportBuilder);
            
            return reportBuilder.build();
        }
        
        /**
         * 检查数据库连接
         */
        private void checkDatabaseConnection(SystemHealthReport.Builder reportBuilder) {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    reportBuilder.databaseStatus(DatabaseHealthStatus.HEALTHY);
                } else {
                    reportBuilder.databaseStatus(DatabaseHealthStatus.UNHEALTHY)
                        .addIssue(new HealthIssue(
                            HealthIssueSeverity.HIGH,
                            "数据库连接超时",
                            "数据库连接测试失败，可能是网络问题或数据库服务器故障"
                        ));
                }
            } catch (SQLException e) {
                reportBuilder.databaseStatus(DatabaseHealthStatus.UNHEALTHY)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.CRITICAL,
                        "数据库连接失败",
                        "无法获取数据库连接: " + e.getMessage()
                    ));
            }
        }
        
        /**
         * 检查Redis连接
         */
        private void checkRedisConnection(SystemHealthReport.Builder reportBuilder) {
            try {
                redisTemplate.opsForValue().set("health_check", "ok", 10, TimeUnit.SECONDS);
                String result = (String) redisTemplate.opsForValue().get("health_check");
                
                if ("ok".equals(result)) {
                    reportBuilder.redisStatus(RedisHealthStatus.HEALTHY);
                } else {
                    reportBuilder.redisStatus(RedisHealthStatus.UNHEALTHY)
                        .addIssue(new HealthIssue(
                            HealthIssueSeverity.MEDIUM,
                            "Redis读写测试失败",
                            "Redis连接正常但读写测试失败"
                        ));
                }
            } catch (Exception e) {
                reportBuilder.redisStatus(RedisHealthStatus.UNHEALTHY)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.HIGH,
                        "Redis连接失败",
                        "无法连接到Redis服务器: " + e.getMessage()
                    ));
            }
        }
        
        /**
         * 检查文件存储
         */
        private void checkFileStorage(SystemHealthReport.Builder reportBuilder) {
            try {
                // 检查存储空间
                DiskSpaceInfo diskSpace = fileStorageService.getDiskSpaceInfo();
                
                if (diskSpace.getFreePercentage() < 10) {
                    reportBuilder.storageStatus(StorageHealthStatus.WARNING)
                        .addIssue(new HealthIssue(
                            HealthIssueSeverity.MEDIUM,
                            "存储空间不足",
                            String.format("剩余存储空间仅%.2f%%，建议清理或扩容", diskSpace.getFreePercentage())
                        ));
                } else if (diskSpace.getFreePercentage() < 5) {
                    reportBuilder.storageStatus(StorageHealthStatus.CRITICAL)
                        .addIssue(new HealthIssue(
                            HealthIssueSeverity.HIGH,
                            "存储空间严重不足",
                            String.format("剩余存储空间仅%.2f%%，需要立即清理或扩容", diskSpace.getFreePercentage())
                        ));
                } else {
                    reportBuilder.storageStatus(StorageHealthStatus.HEALTHY);
                }
                
                // 检查文件读写权限
                if (fileStorageService.testReadWritePermissions()) {
                    reportBuilder.storagePermissions(StoragePermissionsStatus.HEALTHY);
                } else {
                    reportBuilder.storagePermissions(StoragePermissionsStatus.UNHEALTHY)
                        .addIssue(new HealthIssue(
                            HealthIssueSeverity.HIGH,
                            "文件存储权限问题",
                            "无法正常读写文件存储目录，请检查权限配置"
                        ));
                }
            } catch (Exception e) {
                reportBuilder.storageStatus(StorageHealthStatus.UNHEALTHY)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.CRITICAL,
                        "文件存储检查失败",
                        "检查文件存储时发生错误: " + e.getMessage()
                    ));
            }
        }
        
        /**
         * 检查系统资源
         */
        private void checkSystemResources(SystemHealthReport.Builder reportBuilder) {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // CPU使用率检查
            double cpuLoad = osBean.getProcessCpuLoad() * 100;
            if (cpuLoad > 90) {
                reportBuilder.cpuStatus(CpuHealthStatus.CRITICAL)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.HIGH,
                        "CPU使用率过高",
                        String.format("当前CPU使用率%.1f%%，系统负载过高", cpuLoad)
                    ));
            } else if (cpuLoad > 80) {
                reportBuilder.cpuStatus(CpuHealthStatus.WARNING)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.MEDIUM,
                        "CPU使用率较高",
                        String.format("当前CPU使用率%.1f%%，建议关注", cpuLoad)
                    ));
            } else {
                reportBuilder.cpuStatus(CpuHealthStatus.HEALTHY);
            }
            
            // 内存使用率检查
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double memoryUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
            
            if (memoryUsagePercent > 90) {
                reportBuilder.memoryStatus(MemoryHealthStatus.CRITICAL)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.HIGH,
                        "内存使用率过高",
                        String.format("当前内存使用率%.1f%%，系统可能发生OOM", memoryUsagePercent)
                    ));
            } else if (memoryUsagePercent > 80) {
                reportBuilder.memoryStatus(MemoryHealthStatus.WARNING)
                    .addIssue(new HealthIssue(
                        HealthIssueSeverity.MEDIUM,
                        "内存使用率较高",
                        String.format("当前内存使用率%.1f%%，建议关注", memoryUsagePercent)
                    ));
            } else {
                reportBuilder.memoryStatus(MemoryHealthStatus.HEALTHY);
            }
        }
        
        /**
         * 检查业务逻辑
         */
        private void checkBusinessLogic(SystemHealthReport.Builder reportBuilder) {
            // 检查定时任务状态
            checkScheduledTasks(reportBuilder);
            
            // 检查关键业务流程
            checkCriticalBusinessProcesses(reportBuilder);
        }
        
        /**
         * 检查定时任务状态
         */
        private void checkScheduledTasks(SystemHealthReport.Builder reportBuilder) {
            // 这里可以检查定时任务是否正常执行
            // 例如检查最近一次健康巡检是否正常完成
        }
        
        /**
         * 检查关键业务流程
         */
        private void checkCriticalBusinessProcesses(SystemHealthReport.Builder reportBuilder) {
            // 这里可以检查关键业务流程是否正常
            // 例如检查档案创建、合规检查等流程是否正常
        }
    }
}
```

### 2. 自动备份与恢复

#### 2.1 自动备份系统
实现全面的自动备份系统：

```java
@Component
@RequiredArgsConstructor
public class AutoBackupManager {
    
    private final SystemConfigurationService configurationService;
    private final BackupStorageService backupStorageService;
    private final NotificationService notificationService;
    
    /**
     * 定时执行全量备份
     */
    @Scheduled(cron = "${backup.full.cron:0 0 2 * * ?}") // 默认每天凌晨2点执行
    public void performFullBackup() {
        if (!configurationService.isBackupEnabled()) {
            log.debug("自动备份已禁用，跳过执行");
            return;
        }
        
        try {
            log.info("开始执行全量备份");
            
            BackupJob backupJob = BackupJob.builder()
                .type(BackupType.FULL)
                .startTime(LocalDateTime.now())
                .build();
            
            // 备份数据库
            backupDatabase(backupJob);
            
            // 备份文件存储
            backupFileStorage(backupJob);
            
            // 备份配置文件
            backupConfiguration(backupJob);
            
            // 清理过期备份
            cleanupExpiredBackups();
            
            backupJob.setEndTime(LocalDateTime.now());
            backupJob.setStatus(BackupStatus.SUCCESS);
            
            // 保存备份记录
            backupStorageService.saveBackupJob(backupJob);
            
            log.info("全量备份完成，耗时: {}秒", 
                Duration.between(backupJob.getStartTime(), backupJob.getEndTime()).getSeconds());
            
        } catch (Exception e) {
            log.error("全量备份失败", e);
            
            // 发送告警通知
            notificationService.sendSystemAlert(
                "系统备份失败",
                "全量备份执行失败: " + e.getMessage(),
                NotificationPriority.HIGH
            );
        }
    }
    
    /**
     * 定时执行增量备份
     */
    @Scheduled(cron = "${backup.incremental.cron:0 30 */4 * * ?}") // 默认每4小时执行一次
    public void performIncrementalBackup() {
        if (!configurationService.isIncrementalBackupEnabled()) {
            log.debug("增量备份已禁用，跳过执行");
            return;
        }
        
        try {
            log.info("开始执行增量备份");
            
            BackupJob backupJob = BackupJob.builder()
                .type(BackupType.INCREMENTAL)
                .startTime(LocalDateTime.now())
                .build();
            
            // 获取上一次备份时间点
            LocalDateTime lastBackupTime = backupStorageService.getLastBackupTime();
            
            // 备份数据库增量
            backupDatabaseIncremental(backupJob, lastBackupTime);
            
            // 备份文件存储增量
            backupFileStorageIncremental(backupJob, lastBackupTime);
            
            backupJob.setEndTime(LocalDateTime.now());
            backupJob.setStatus(BackupStatus.SUCCESS);
            
            // 保存备份记录
            backupStorageService.saveBackupJob(backupJob);
            
            log.info("增量备份完成，耗时: {}秒", 
                Duration.between(backupJob.getStartTime(), backupJob.getEndTime()).getSeconds());
            
        } catch (Exception e) {
            log.error("增量备份失败", e);
            
            // 发送告警通知
            notificationService.sendSystemAlert(
                "系统增量备份失败",
                "增量备份执行失败: " + e.getMessage(),
                NotificationPriority.MEDIUM
            );
        }
    }
    
    /**
     * 备份数据库
     */
    private void backupDatabase(BackupJob backupJob) {
        String databaseType = configurationService.getDatabaseType();
        
        switch (databaseType) {
            case "postgresql":
                backupPostgreSQLDatabase(backupJob);
                break;
            case "mysql":
                backupMySQLDatabase(backupJob);
                break;
            case "dameng":
                backupDamengDatabase(backupJob);
                break;
            case "kingbase":
                backupKingbaseDatabase(backupJob);
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
    }
    
    /**
     * 备份PostgreSQL数据库
     */
    private void backupPostgreSQLDatabase(BackupJob backupJob) {
        String host = configurationService.getDatabaseHost();
        String port = configurationService.getDatabasePort();
        String database = configurationService.getDatabaseName();
        String username = configurationService.getDatabaseUsername();
        String password = configurationService.getDatabasePassword();
        
        String backupPath = backupStorageService.getBackupPath() + "/database/" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        
        // 构建备份命令
        String[] command = {
            "pg_dump",
            "-h", host,
            "-p", port,
            "-U", username,
            "-d", database,
            "-f", backupPath,
            "--verbose",
            "--no-password",
            "--format=custom"
        };
        
        // 设置环境变量
        Map<String, String> env = new HashMap<>();
        env.put("PGPASSWORD", password);
        
        // 执行备份命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(env);
        
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                backupJob.addBackupItem(new BackupItem(
                    BackupItemType.DATABASE,
                    backupPath,
                    new File(backupPath).length()
                ));
                
                log.info("PostgreSQL数据库备份完成: {}", backupPath);
            } else {
                throw new RuntimeException("PostgreSQL数据库备份失败，退出码: " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("PostgreSQL数据库备份异常", e);
        }
    }
    
    /**
     * 备份文件存储
     */
    private void backupFileStorage(BackupJob backupJob) {
        String sourcePath = configurationService.getFileStoragePath();
        String backupPath = backupStorageService.getBackupPath() + "/files/" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        try {
            // 创建备份目录
            Files.createDirectories(Paths.get(backupPath));
            
            // 复制文件
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(backupPath);
            
            if (Files.exists(source)) {
                // 使用rsync或robocopy进行高效复制
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                
                String[] command;
                if (isWindows) {
                    // Windows使用robocopy
                    command = new String[]{
                        "robocopy",
                        sourcePath,
                        backupPath,
                        "/E",
                        "/COPY:DAT",
                        "/R:2",
                        "/W:5"
                    };
                } else {
                    // Linux/Unix使用rsync
                    command = new String[]{
                        "rsync",
                        "-a",
                        sourcePath + "/",
                        backupPath + "/"
                    };
                }
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                
                // robocopy退出码0-7都表示成功
                if (isWindows && exitCode <= 7) {
                    exitCode = 0;
                }
                
                if (exitCode == 0) {
                    long totalSize = calculateDirectorySize(Paths.get(backupPath));
                    
                    backupJob.addBackupItem(new BackupItem(
                        BackupItemType.FILE_STORAGE,
                        backupPath,
                        totalSize
                    ));
                    
                    log.info("文件存储备份完成: {}", backupPath);
                } else {
                    throw new RuntimeException("文件存储备份失败，退出码: " + exitCode);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("文件存储备份异常", e);
        }
    }
    
    /**
     * 清理过期备份
     */
    private void cleanupExpiredBackups() {
        int retentionDays = configurationService.getBackupRetentionDays();
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
        
        List<BackupJob> expiredBackups = backupStorageService.getExpiredBackups(expireTime);
        
        for (BackupJob backup : expiredBackups) {
            try {
                // 删除备份文件
                for (BackupItem item : backup.getBackupItems()) {
                    Path path = Paths.get(item.getPath());
                    if (Files.exists(path)) {
                        Files.delete(path);
                        log.info("删除过期备份文件: {}", item.getPath());
                    }
                }
                
                // 更新备份状态
                backup.setStatus(BackupStatus.DELETED);
                backupStorageService.saveBackupJob(backup);
                
            } catch (Exception e) {
                log.error("清理过期备份失败: " + backup.getId(), e);
            }
        }
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        return Files.walk(directory)
            .filter(Files::isRegularFile)
            .mapToLong(file -> {
                try {
                    return Files.size(file);
                } catch (IOException e) {
                    log.warn("无法获取文件大小: " + file, e);
                    return 0L;
                }
            })
            .sum();
    }
}
```

#### 2.2 恢复系统
实现便捷的恢复系统：

```java
@RestController
@RequestMapping("/api/admin/restore")
@RequiredArgsConstructor
public class SystemRestoreController {
    
    private final SystemRestoreService restoreService;
    
    /**
     * 获取可用的备份列表
     */
    @GetMapping("/backups")
    public ResponseEntity<List<BackupInfo>> getAvailableBackups() {
        List<BackupInfo> backups = restoreService.getAvailableBackups();
        return ResponseEntity.ok(backups);
    }
    
    /**
     * 获取备份详情
     */
    @GetMapping("/backups/{backupId}")
    public ResponseEntity<BackupDetail> getBackupDetail(@PathVariable Long backupId) {
        BackupDetail detail = restoreService.getBackupDetail(backupId);
        return ResponseEntity.ok(detail);
    }
    
    /**
     * 执行系统恢复
     */
    @PostMapping("/execute")
    public ResponseEntity<Void> executeRestore(@RequestBody RestoreRequest request) {
        // 创建恢复任务
        RestoreTask task = restoreService.createRestoreTask(request);
        
        // 异步执行恢复
        restoreService.executeRestoreAsync(task.getId());
        
        return ResponseEntity.accepted().build();
    }
    
    /**
     * 获取恢复任务状态
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<RestoreTask> getRestoreTaskStatus(@PathVariable Long taskId) {
        RestoreTask task = restoreService.getRestoreTask(taskId);
        return ResponseEntity.ok(task);
    }
}

/**
 * 系统恢复服务
 */
@Service
@RequiredArgsConstructor
public class SystemRestoreService {
    
    private final BackupStorageService backupStorageService;
    private final SystemConfigurationService configurationService;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;
    
    /**
     * 创建恢复任务
     */
    @Transactional
    public RestoreTask createRestoreTask(RestoreRequest request) {
        // 验证备份
        BackupJob backup = backupStorageService.getBackupJob(request.getBackupId());
        if (backup == null) {
            throw new IllegalArgumentException("备份不存在");
        }
        
        // 创建恢复任务
        RestoreTask task = RestoreTask.builder()
            .backupId(request.getBackupId())
            .restoreType(request.getRestoreType())
            .status(RestoreTaskStatus.PENDING)
            .createdTime(LocalDateTime.now())
            .requestedBy(request.getUserId())
            .restoreOptions(request.getOptions())
            .build();
        
        return restoreTaskRepository.save(task);
    }
    
    /**
     * 异步执行恢复
     */
    public void executeRestoreAsync(Long taskId) {
        taskExecutor.execute(() -> {
            try {
                executeRestore(taskId);
            } catch (Exception e) {
                log.error("系统恢复失败: " + taskId, e);
            }
        });
    }
    
    /**
     * 执行恢复
     */
    @Transactional
    public void executeRestore(Long taskId) {
        RestoreTask task = restoreTaskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("恢复任务不存在"));
        
        try {
            // 更新任务状态
            task.setStatus(RestoreTaskStatus.RUNNING);
            task.setStartTime(LocalDateTime.now());
            restoreTaskRepository.save(task);
            
            // 获取备份
            BackupJob backup = backupStorageService.getBackupJob(task.getBackupId());
            
            // 根据恢复类型执行恢复
            switch (task.getRestoreType()) {
                case FULL:
                    executeFullRestore(task, backup);
                    break;
                case DATABASE:
                    executeDatabaseRestore(task, backup);
                    break;
                case FILES:
                    executeFilesRestore(task, backup);
                    break;
                case CONFIGURATION:
                    executeConfigurationRestore(task, backup);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的恢复类型: " + task.getRestoreType());
            }
            
            // 更新任务状态
            task.setStatus(RestoreTaskStatus.SUCCESS);
            task.setEndTime(LocalDateTime.now());
            restoreTaskRepository.save(task);
            
            // 发送通知
            notificationService.sendSystemNotification(
                task.getRequestedBy(),
                "系统恢复完成",
                "您的系统恢复请求已成功完成"
            );
            
        } catch (Exception e) {
            // 更新任务状态
            task.setStatus(RestoreTaskStatus.FAILED);
            task.setEndTime(LocalDateTime.now());
            task.setErrorMessage(e.getMessage());
            restoreTaskRepository.save(task);
            
            // 发送通知
            notificationService.sendSystemNotification(
                task.getRequestedBy(),
                "系统恢复失败",
                "您的系统恢复请求失败: " + e.getMessage()
            );
            
            throw new RuntimeException("系统恢复失败", e);
        }
    }
    
    /**
     * 执行全量恢复
     */
    private void executeFullRestore(RestoreTask task, BackupJob backup) {
        // 停止应用服务
        stopApplicationServices();
        
        try {
            // 恢复数据库
            executeDatabaseRestore(task, backup);
            
            // 恢复文件存储
            executeFilesRestore(task, backup);
            
            // 恢复配置
            executeConfigurationRestore(task, backup);
            
            // 重新启动应用服务
            startApplicationServices();
            
        } catch (Exception e) {
            // 尝试重新启动应用服务
            startApplicationServices();
            throw e;
        }
    }
    
    /**
     * 恢复数据库
     */
    private void executeDatabaseRestore(RestoreTask task, BackupJob backup) {
        BackupItem databaseBackup = backup.getBackupItems().stream()
            .filter(item -> item.getType() == BackupItemType.DATABASE)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("备份中不包含数据库备份"));
        
        String databaseType = configurationService.getDatabaseType();
        
        switch (databaseType) {
            case "postgresql":
                restorePostgreSQLDatabase(databaseBackup.getPath());
                break;
            case "mysql":
                restoreMySQLDatabase(databaseBackup.getPath());
                break;
            case "dameng":
                restoreDamengDatabase(databaseBackup.getPath());
                break;
            case "kingbase":
                restoreKingbaseDatabase(databaseBackup.getPath());
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        }
        
        // 更新任务进度
        updateTaskProgress(task, "数据库恢复完成");
    }
    
    /**
     * 恢复PostgreSQL数据库
     */
    private void restorePostgreSQLDatabase(String backupPath) {
        String host = configurationService.getDatabaseHost();
        String port = configurationService.getDatabasePort();
        String database = configurationService.getDatabaseName();
        String username = configurationService.getDatabaseUsername();
        String password = configurationService.getDatabasePassword();
        
        // 构建恢复命令
        String[] command = {
            "pg_restore",
            "-h", host,
            "-p", port,
            "-U", username,
            "-d", database,
            "--verbose",
            "--clean",
            "--if-exists",
            "--no-password",
            backupPath
        };
        
        // 设置环境变量
        Map<String, String> env = new HashMap<>();
        env.put("PGPASSWORD", password);
        
        // 执行恢复命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(env);
        
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new RuntimeException("PostgreSQL数据库恢复失败，退出码: " + exitCode);
            }
            
            log.info("PostgreSQL数据库恢复完成: {}", backupPath);
        } catch (Exception e) {
            throw new RuntimeException("PostgreSQL数据库恢复异常", e);
        }
    }
    
    /**
     * 恢复文件存储
     */
    private void executeFilesRestore(RestoreTask task, BackupJob backup) {
        BackupItem filesBackup = backup.getBackupItems().stream()
            .filter(item -> item.getType() == BackupItemType.FILE_STORAGE)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("备份中不包含文件存储备份"));
        
        String targetPath = configurationService.getFileStoragePath();
        String backupPath = filesBackup.getPath();
        
        try {
            // 备份当前文件（以防恢复失败）
            String currentBackupPath = targetPath + ".backup." + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            Path source = Paths.get(targetPath);
            if (Files.exists(source)) {
                Files.move(source, Paths.get(currentBackupPath));
            }
            
            // 恢复文件
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            
            String[] command;
            if (isWindows) {
                // Windows使用robocopy
                command = new String[]{
                    "robocopy",
                    backupPath,
                    targetPath,
                    "/E",
                    "/COPY:DAT",
                    "/R:2",
                    "/W:5"
                };
            } else {
                // Linux/Unix使用rsync
                command = new String[]{
                    "rsync",
                    "-a",
                    backupPath + "/",
                    targetPath + "/"
                };
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            // robocopy退出码0-7都表示成功
            if (isWindows && exitCode <= 7) {
                exitCode = 0;
            }
            
            if (exitCode != 0) {
                // 恢复失败，恢复原始文件
                Files.delete(Paths.get(targetPath));
                if (Files.exists(Paths.get(currentBackupPath))) {
                    Files.move(Paths.get(currentBackupPath), source);
                }
                
                throw new RuntimeException("文件存储恢复失败，退出码: " + exitCode);
            }
            
            // 删除临时备份
            Files.deleteIfExists(Paths.get(currentBackupPath));
            
            // 更新任务进度
            updateTaskProgress(task, "文件存储恢复完成");
            
            log.info("文件存储恢复完成: {}", backupPath);
        } catch (Exception e) {
            throw new RuntimeException("文件存储恢复异常", e);
        }
    }
    
    /**
     * 更新任务进度
     */
    private void updateTaskProgress(RestoreTask task, String progress) {
        task.setProgress(progress);
        restoreTaskRepository.save(task);
    }
    
    /**
     * 停止应用服务
     */
    private void stopApplicationServices() {
        // 实现停止应用服务的逻辑
        // 例如：通过systemctl停止服务或调用管理接口
    }
    
    /**
     * 启动应用服务
     */
    private void startApplicationServices() {
        // 实现启动应用服务的逻辑
        // 例如：通过systemctl启动服务或调用管理接口
    }
}
```

### 3. 自动化测试框架

#### 3.1 集成测试框架
建立全面的集成测试框架：

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public abstract class BaseIntegrationTest {
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * 创建测试档案
     */
    protected Archive createTestArchive() {
        return Archive.builder()
            .archiveCode("TEST-" + System.currentTimeMillis())
            .title("测试档案")
            .accountPeriod("202501")
            .amount(new BigDecimal("100.00"))
            .fondsCode("TEST")
            .status("DRAFT")
            .build();
    }
    
    /**
     * 创建测试用户
     */
    protected User createTestUser(String username, String... roles) {
        return User.builder()
            .username(username)
            .password("$2a$10$dummy.encrypted.password")
            .email(username + "@example.com")
            .status("ACTIVE")
            .roles(Arrays.asList(roles))
            .build();
    }
    
    /**
     * 创建认证头
     */
    protected HttpHeaders createAuthHeaders(String username) {
        String token = createTestToken(username);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    
    /**
     * 创建测试令牌
     */
    protected String createTestToken(String username) {
        // 创建测试JWT令牌
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1小时后过期
            .signWith(SignatureAlgorithm.HS512, "test-secret")
            .compact();
    }
}

/**
 * 档案管理集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ArchiveManagementIntegrationTest extends BaseIntegrationTest {
    
    @Test
    public void testCreateArchive() {
        // 准备测试数据
        Archive archive = createTestArchive();
        
        // 发送请求
        HttpHeaders headers = createAuthHeaders("test-admin");
        HttpEntity<Archive> request = new HttpEntity<>(archive, headers);
        
        ResponseEntity<Archive> response = restTemplate.postForEntity(
            "/api/archives", request, Archive.class);
        
        // 验证结果
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(archive.getArchiveCode(), response.getBody().getArchiveCode());
        assertEquals(archive.getTitle(), response.getBody().getTitle());
    }
    
    @Test
    public void testArchiveComplianceCheck() {
        // 创建测试档案
        Archive archive = createTestArchive();
        HttpHeaders headers = createAuthHeaders("test-admin");
        HttpEntity<Archive> createRequest = new HttpEntity<>(archive, headers);
        
        ResponseEntity<Archive> createResponse = restTemplate.postForEntity(
            "/api/archives", createRequest, Archive.class);
        
        Long archiveId = createResponse.getBody().getId();
        
        // 执行符合性检查
        HttpEntity<Void> checkRequest = new HttpEntity<>(headers);
        ResponseEntity<ComplianceCheckResult> checkResponse = restTemplate.postForEntity(
            "/api/compliance/archives/" + archiveId, checkRequest, ComplianceCheckResult.class);
        
        // 验证结果
        assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
        assertNotNull(checkResponse.getBody());
        assertTrue(checkResponse.getBody().isCompliant());
    }
    
    @Test
    public void testArchiveSearch() {
        // 创建测试数据
        List<Archive> archives = Arrays.asList(
            createTestArchive(),
            createTestArchive(),
            createTestArchive()
        );
        
        HttpHeaders headers = createAuthHeaders("test-admin");
        for (Archive archive : archives) {
            HttpEntity<Archive> request = new HttpEntity<>(archive, headers);
            restTemplate.postForEntity("/api/archives", request, Archive.class);
        }
        
        // 执行搜索
        HttpEntity<Void> searchRequest = new HttpEntity<>(headers);
        ResponseEntity<SearchResult> searchResponse = restTemplate.exchange(
            "/api/archives/search?keyword=测试", HttpMethod.GET, searchRequest, SearchResult.class);
        
        // 验证结果
        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertNotNull(searchResponse.getBody());
        assertTrue(searchResponse.getBody().getTotal() > 0);
    }
}

/**
 * 符合性检查集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ComplianceCheckIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private ComplianceCheckService complianceCheckService;
    
    @Test
    public void testFourNatureCheck() {
        // 创建测试档案
        Archive archive = createTestArchive();
        
        // 创建测试文件
        Map<String, byte[]> files = new HashMap<>();
        files.put("main.pdf", "测试PDF内容".getBytes());
        files.put("attachment1.pdf", "测试附件1内容".getBytes());
        
        // 执行四性检测
        FourNatureReport report = complianceCheckService.performFourNatureCheck(archive, files);
        
        // 验证结果
        assertNotNull(report);
        assertNotNull(report.getAuthenticity());
        assertNotNull(report.getIntegrity());
        assertNotNull(report.getUsability());
        assertNotNull(report.getSafety());
        
        // 在测试环境中，应该通过所有检测
        assertEquals(OverallStatus.PASS, report.getStatus());
    }
    
    @Test
    public void testComplianceCheck() {
        // 创建测试档案
        Archive archive = createTestArchive();
        
        // 创建测试文件
        List<ArcFileContent> files = new ArrayList<>();
        files.add(createTestFileContent(archive.getId(), "main.pdf"));
        files.add(createTestFileContent(archive.getId(), "attachment1.pdf"));
        
        // 执行符合性检查
        ComplianceCheckService.ComplianceResult result = complianceCheckService.checkCompliance(archive, files);
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isCompliant());
        assertTrue(result.getViolations().isEmpty());
    }
    
    private ArcFileContent createTestFileContent(Long archiveId, String fileName) {
        return ArcFileContent.builder()
            .itemId(archiveId)
            .fileName(fileName)
            .fileType("PDF")
            .fileSize(1024L)
            .filePath("/test/" + fileName)
            .hashValue("dummy-hash-value")
            .build();
    }
}

/**
 * 系统性能测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
public class SystemPerformanceIntegrationTest extends BaseIntegrationTest {
    
    private static final int THREAD_COUNT = 10;
    private static final int REQUEST_COUNT = 100;
    
    @Test
    @Order(1)
    public void testArchiveCreatePerformance() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < REQUEST_COUNT; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Archive archive = createTestArchive();
                    archive.setArchiveCode("PERF-TEST-" + index);
                    
                    HttpHeaders headers = createAuthHeaders("test-admin");
                    HttpEntity<Archive> request = new HttpEntity<>(archive, headers);
                    
                    restTemplate.postForEntity("/api/archives", request, Archive.class);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        double totalTime = endTime - startTime;
        double avgTime = totalTime / REQUEST_COUNT;
        double tps = REQUEST_COUNT / (totalTime / 1000.0);
        
        // 输出性能指标
        System.out.println("档案创建性能测试结果:");
        System.out.printf("总时间: %.2f 秒%n", totalTime / 1000.0);
        System.out.printf("平均响应时间: %.2f 毫秒%n", avgTime);
        System.out.printf("吞吐量: %.2f TPS%n", tps);
        
        // 验证性能指标
        assertTrue(avgTime < 5000, "平均响应时间应小于5秒");
        assertTrue(tps > 10, "吞吐量应大于10 TPS");
    }
    
    @Test
    @Order(2)
    public void testArchiveSearchPerformance() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(REQUEST_COUNT);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < REQUEST_COUNT; i++) {
            executor.submit(() -> {
                try {
                    HttpHeaders headers = createAuthHeaders("test-admin");
                    HttpEntity<Void> request = new HttpEntity<>(headers);
                    
                    restTemplate.exchange(
                        "/api/archives/search?keyword=PERF-TEST", 
                        HttpMethod.GET, 
                        request, 
                        SearchResult.class);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        double totalTime = endTime - startTime;
        double avgTime = totalTime / REQUEST_COUNT;
        double tps = REQUEST_COUNT / (totalTime / 1000.0);
        
        // 输出性能指标
        System.out.println("档案搜索性能测试结果:");
        System.out.printf("总时间: %.2f 秒%n", totalTime / 1000.0);
        System.out.printf("平均响应时间: %.2f 毫秒%n", avgTime);
        System.out.printf("吞吐量: %.2f TPS%n", tps);
        
        // 验证性能指标
        assertTrue(avgTime < 1000, "平均响应时间应小于1秒");
        assertTrue(tps > 50, "吞吐量应大于50 TPS");
    }
}

/**
 * CI/CD集成测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class CiCdIntegrationTest extends BaseIntegrationTest {
    
    @Value("${test.data.path}")
    private String testDataPath;
    
    @Test
    public void testDataIntegrity() {
        // 加载测试数据
        List<Archive> testArchives = loadTestData();
        
        // 验证数据完整性
        for (Archive archive : testArchives) {
            assertNotNull(archive.getId());
            assertNotNull(archive.getArchiveCode());
            assertNotNull(archive.getTitle());
            assertNotNull(archive.getAccountPeriod());
            assertNotNull(archive.getAmount());
            assertNotNull(archive.getFondsCode());
            assertNotNull(archive.getStatus());
        }
    }
    
    @Test
    public void testBusinessWorkflow() {
        // 模拟完整的业务流程
        testArchiveWorkflow();
        testComplianceCheckWorkflow();
        testArchiveBorrowWorkflow();
        testArchiveTransferWorkflow();
    }
    
    private void testArchiveWorkflow() {
        // 1. 创建档案
        Archive archive = createTestArchive();
        HttpHeaders headers = createAuthHeaders("test-admin");
        HttpEntity<Archive> createRequest = new HttpEntity<>(archive, headers);
        
        ResponseEntity<Archive> createResponse = restTemplate.postForEntity(
            "/api/archives", createRequest, Archive.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Long archiveId = createResponse.getBody().getId();
        
        // 2. 查询档案
        ResponseEntity<Archive> getResponse = restTemplate.getForEntity(
            "/api/archives/" + archiveId, Archive.class);
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(archiveId, getResponse.getBody().getId());
        
        // 3. 更新档案
        archive.setTitle("更新后的标题");
        HttpEntity<Archive> updateRequest = new HttpEntity<>(archive, headers);
        
        ResponseEntity<Archive> updateResponse = restTemplate.exchange(
            "/api/archives/" + archiveId, HttpMethod.PUT, updateRequest, Archive.class);
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals("更新后的标题", updateResponse.getBody().getTitle());
        
        // 4. 删除档案
        restTemplate.delete("/api/archives/" + archiveId);
        
        // 5. 验证删除
        try {
            restTemplate.getForEntity("/api/archives/" + archiveId, Archive.class);
            fail("删除的档案不应该能够查询到");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
    
    private void testComplianceCheckWorkflow() {
        // 1. 创建测试档案
        Archive archive = createTestArchive();
        HttpHeaders headers = createAuthHeaders("test-admin");
        HttpEntity<Archive> createRequest = new HttpEntity<>(archive, headers);
        
        ResponseEntity<Archive> createResponse = restTemplate.postForEntity(
            "/api/archives", createRequest, Archive.class);
        
        Long archiveId = createResponse.getBody().getId();
        
        // 2. 执行符合性检查
        HttpEntity<Void> checkRequest = new HttpEntity<>(headers);
        ResponseEntity<ComplianceCheckResult> checkResponse = restTemplate.postForEntity(
            "/api/compliance/archives/" + archiveId, checkRequest, ComplianceCheckResult.class);
        
        assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
        assertTrue(checkResponse.getBody().isCompliant());
        
        // 3. 生成检查报告
        ResponseEntity<byte[]> reportResponse = restTemplate.getForEntity(
            "/api/compliance/archives/" + archiveId + "/report", byte[].class);
        
        assertEquals(HttpStatus.OK, reportResponse.getStatusCode());
        assertTrue(reportResponse.getBody().length > 0);
    }
    
    private void testArchiveBorrowWorkflow() {
        // 1. 创建测试档案
        Archive archive = createTestArchive();
        HttpHeaders headers = createAuthHeaders("test-user");
        HttpEntity<Archive> createRequest = new HttpEntity<>(archive, headers);
        
        ResponseEntity<Archive> createResponse = restTemplate.postForEntity(
            "/api/archives", createRequest, Archive.class);
        
        Long archiveId = createResponse.getBody().getId();
        
        // 2. 申请借阅
        BorrowingRequest borrowingRequest = new BorrowingRequest();
        borrowingRequest.setArchiveId(archiveId);
        borrowingRequest.setReason("测试借阅");
        borrowingRequest.setExpectedReturnDate(LocalDate.now().plusDays(7));
        
        HttpEntity<BorrowingRequest> borrowRequest = new HttpEntity<>(borrowingRequest, headers);
        ResponseEntity<Borrowing> borrowResponse = restTemplate.postForEntity(
            "/api/borrowing", borrowRequest, Borrowing.class);
        
        assertEquals(HttpStatus.OK, borrowResponse.getStatusCode());
        Long borrowingId = borrowResponse.getBody().getId();
        assertEquals(BorrowingStatus.PENDING, borrowResponse.getBody().getStatus());
        
        // 3. 审批借阅（使用管理员账号）
        HttpHeaders adminHeaders = createAuthHeaders("test-admin");
        
        BorrowingApproval approval = new BorrowingApproval();
        approval.setApproved(true);
        approval.setComment("审批通过");
        
        HttpEntity<BorrowingApproval> approvalRequest = new HttpEntity<>(approval, adminHeaders);
        ResponseEntity<Borrowing> approvalResponse = restTemplate.postForEntity(
            "/api/borrowing/" + borrowingId + "/approve", approvalRequest, Borrowing.class);
        
        assertEquals(HttpStatus.OK, approvalResponse.getStatusCode());
        assertEquals(BorrowingStatus.APPROVED, approvalResponse.getBody().getStatus());
        
        // 4. 归还档案
        HttpEntity<Void> returnRequest = new HttpEntity<>(headers);
        ResponseEntity<Borrowing> returnResponse = restTemplate.postForEntity(
            "/api/borrowing/" + borrowingId + "/return", returnRequest, Borrowing.class);
        
        assertEquals(HttpStatus.OK, returnResponse.getStatusCode());
        assertEquals(BorrowingStatus.RETURNED, returnResponse.getBody().getStatus());
    }
    
    private void testArchiveTransferWorkflow() {
        // 实现档案移交测试流程
        // ...
    }
    
    private List<Archive> loadTestData() {
        // 从文件或数据库加载测试数据
        // ...
        return new ArrayList<>();
    }
}
```

## 第三部分：交付与实施（3-4周）

### 1. 私有化部署包设计

#### 1.1 一键安装脚本
创建适合私有化部署的一键安装脚本：

```bash
#!/bin/bash

# NexusArchive 电子会计档案系统 - 一键安装脚本
# 版本: 1.0.0
# 更新日期: 2025-12-06

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 检查系统要求
check_system_requirements() {
    log_info "检查系统要求..."
    
    # 检查操作系统
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        log_success "操作系统: Linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        log_success "操作系统: macOS"
    else
        log_error "不支持的操作系统: $OSTYPE"
        exit 1
    fi
    
    # 检查CPU架构
    ARCH=$(uname -m)
    if [[ "$ARCH" == "x86_64" ]]; then
        log_success "CPU架构: x86_64"
    elif [[ "$ARCH" == "aarch64" ]]; then
        log_success "CPU架构: ARM64"
    else
        log_error "不支持的CPU架构: $ARCH"
        exit 1
    fi
    
    # 检查内存
    MEMORY_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    MEMORY_GB=$((MEMORY_KB / 1024 / 1024))
    
    if [[ $MEMORY_GB -lt 8 ]]; then
        log_error "系统内存不足，至少需要8GB，当前: ${MEMORY_GB}GB"
        exit 1
    else
        log_success "系统内存: ${MEMORY_GB}GB"
    fi
    
    # 检查磁盘空间
    DISK_AVAILABLE=$(df -BG . | tail -1 | awk '{print $4}' | tr -d 'G')
    
    if [[ $DISK_AVAILABLE -lt 50 ]]; then
        log_error "磁盘空间不足，至少需要50GB，当前可用: ${DISK_AVAILABLE}GB"
        exit 1
    else
        log_success "磁盘空间: ${DISK_AVAILABLE}GB可用"
    fi
    
    # 检查依赖软件
    check_dependency "docker"
    check_dependency "docker-compose"
    check_dependency "java"
    check_dependency "node"
    check_dependency "npm"
    
    log_success "系统要求检查完成"
}

# 检查依赖软件
check_dependency() {
    if command -v $1 &> /dev/null; then
        log_success "$1 已安装: $($1 --version | head -n 1)"
    else
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 配置安装参数
configure_installation() {
    log_info "配置安装参数..."
    
    # 默认值
    DEFAULT_INSTALL_DIR="/opt/nexusarchive"
    DEFAULT_DATA_DIR="/opt/nexusarchive/data"
    DEFAULT_HTTP_PORT=8080
    DEFAULT_HTTPS_PORT=8443
    DEFAULT_DB_TYPE=postgresql
    DEFAULT_DB_HOST=localhost
    DEFAULT_DB_PORT=5432
    DEFAULT_DB_NAME=nexusarchive
    DEFAULT_DB_USER=nexusarchive
    DEFAULT_DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    DEFAULT_REDIS_HOST=localhost
    DEFAULT_REDIS_PORT=6379
    DEFAULT_REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    DEFAULT_JWT_SECRET=$(openssl rand -base64 64)
    DEFAULT_SM4_KEY=$(openssl rand -hex 16)
    
    # 读取用户输入
    echo ""
    log_info "请输入安装配置参数（直接回车使用默认值）:"
    
    read -p "安装目录 [$DEFAULT_INSTALL_DIR]: " INSTALL_DIR
    INSTALL_DIR=${INSTALL_DIR:-$DEFAULT_INSTALL_DIR}
    
    read -p "数据目录 [$DEFAULT_DATA_DIR]: " DATA_DIR
    DATA_DIR=${DATA_DIR:-$DEFAULT_DATA_DIR}
    
    read -p "HTTP端口 [$DEFAULT_HTTP_PORT]: " HTTP_PORT
    HTTP_PORT=${HTTP_PORT:-$DEFAULT_HTTP_PORT}
    
    read -p "HTTPS端口 [$DEFAULT_HTTPS_PORT]: " HTTPS_PORT
    HTTPS_PORT=${HTTPS_PORT:-$DEFAULT_HTTPS_PORT}
    
    read -p "数据库类型 (postgresql/mysql/dameng/kingbase) [$DEFAULT_DB_TYPE]: " DB_TYPE
    DB_TYPE=${DB_TYPE:-$DEFAULT_DB_TYPE}
    
    read -p "数据库主机 [$DEFAULT_DB_HOST]: " DB_HOST
    DB_HOST=${DB_HOST:-$DEFAULT_DB_HOST}
    
    read -p "数据库端口 [$DEFAULT_DB_PORT]: " DB_PORT
    DB_PORT=${DB_PORT:-$DEFAULT_DB_PORT}
    
    read -p "数据库名称 [$DEFAULT_DB_NAME]: " DB_NAME
    DB_NAME=${DB_NAME:-$DEFAULT_DB_NAME}
    
    read -p "数据库用户 [$DEFAULT_DB_USER]: " DB_USER
    DB_USER=${DB_USER:-$DEFAULT_DB_USER}
    
    read -p "数据库密码 [自动生成]: " DB_PASSWORD
    DB_PASSWORD=${DB_PASSWORD:-$DEFAULT_DB_PASSWORD}
    
    read -p "Redis主机 [$DEFAULT_REDIS_HOST]: " REDIS_HOST
    REDIS_HOST=${REDIS_HOST:-$DEFAULT_REDIS_HOST}
    
    read -p "Redis端口 [$DEFAULT_REDIS_PORT]: " REDIS_PORT
    REDIS_PORT=${REDIS_PORT:-$DEFAULT_REDIS_PORT}
    
    read -p "Redis密码 [自动生成]: " REDIS_PASSWORD
    REDIS_PASSWORD=${REDIS_PASSWORD:-$DEFAULT_REDIS_PASSWORD}
    
    # 确认配置
    echo ""
    log_info "安装配置摘要:"
    echo "安装目录: $INSTALL_DIR"
    echo "数据目录: $DATA_DIR"
    echo "HTTP端口: $HTTP_PORT"
    echo "HTTPS端口: $HTTPS_PORT"
    echo "数据库类型: $DB_TYPE"
    echo "数据库连接: $DB_HOST:$DB_PORT/$DB_NAME"
    echo "Redis连接: $REDIS_HOST:$REDIS_PORT"
    echo ""
    
    read -p "确认配置并继续安装? (y/n): " CONFIRM
    if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
        log_info "安装已取消"
        exit 0
    fi
}

# 准备安装环境
prepare_environment() {
    log_info "准备安装环境..."
    
    # 创建目录
    sudo mkdir -p "$INSTALL_DIR"
    sudo mkdir -p "$DATA_DIR"
    sudo mkdir -p "$DATA_DIR/mysql"
    sudo mkdir -p "$DATA_DIR/postgresql"
    sudo mkdir -p "$DATA_DIR/redis"
    sudo mkdir -p "$DATA_DIR/files"
    sudo mkdir -p "$DATA_DIR/logs"
    sudo mkdir -p "$DATA_DIR/backups"
    
    # 设置权限
    sudo chown -R $USER:$USER "$INSTALL_DIR"
    sudo chown -R $USER:$USER "$DATA_DIR"
    
    # 生成配置文件
    generate_config_files
    
    log_success "安装环境准备完成"
}

# 生成配置文件
generate_config_files() {
    log_info "生成配置文件..."
    
    # 生成环境变量文件
    cat > "$INSTALL_DIR/.env" <<EOF
# NexusArchive 环境配置
# 生成时间: $(date)

# 基础配置
INSTALL_DIR=$INSTALL_DIR
DATA_DIR=$DATA_DIR
HTTP_PORT=$HTTP_PORT
HTTPS_PORT=$HTTPS_PORT

# 数据库配置
DB_TYPE=$DB_TYPE
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD

# Redis配置
REDIS_HOST=$REDIS_HOST
REDIS_PORT=$REDIS_PORT
REDIS_PASSWORD=$REDIS_PASSWORD

# 安全配置
JWT_SECRET=$DEFAULT_JWT_SECRET
SM4_KEY=$DEFAULT_SM4_KEY

# 文件存储配置
FILE_STORAGE_PATH=$DATA_DIR/files

# 日志配置
LOG_PATH=$DATA_DIR/logs

# 备份配置
BACKUP_PATH=$DATA_DIR/backups
BACKUP_RETENTION_DAYS=30
EOF
    
    # 生成Docker Compose文件
    cat > "$INSTALL_DIR/docker-compose.yml" <<EOF
version: '3.8'

services:
  nexusarchive-app:
    image: nexusarchive/nexusarchive:latest
    container_name: nexusarchive-app
    ports:
      - "$HTTP_PORT:8080"
      - "$HTTPS_PORT:8443"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:${DB_TYPE}://\${DB_HOST}:\${DB_PORT}/\${DB_NAME}
      - SPRING_DATASOURCE_USERNAME=\${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=\${DB_PASSWORD}
      - SPRING_REDIS_HOST=\${REDIS_HOST}
      - SPRING_REDIS_PORT=\${REDIS_PORT}
      - SPRING_REDIS_PASSWORD=\${REDIS_PASSWORD}
      - NEXUSARCHIVE_JWT_SECRET=\${JWT_SECRET}
      - NEXUSARCHIVE_SM4_KEY=\${SM4_KEY}
      - NEXUSARCHIVE_FILE_STORAGE_PATH=\${FILE_STORAGE_PATH}
      - NEXUSARCHIVE_LOG_PATH=\${LOG_PATH}
      - NEXUSARCHIVE_BACKUP_PATH=\${BACKUP_PATH}
    volumes:
      - $DATA_DIR/files:/app/files
      - $DATA_DIR/logs:/app/logs
      - $DATA_DIR/backups:/app/backups
    depends_on:
      - nexusarchive-db
      - nexusarchive-redis
    restart: unless-stopped
    networks:
      - nexusarchive-network

  nexusarchive-db:
    image: ${DB_TYPE}:latest
    container_name: nexusarchive-db
    environment:
      - POSTGRES_DB=\${DB_NAME}
      - POSTGRES_USER=\${DB_USER}
      - POSTGRES_PASSWORD=\${DB_PASSWORD}
    volumes:
      - $DATA_DIR/postgresql:/var/lib/postgresql/data
    ports:
      - "$DB_PORT:5432"
    restart: unless-stopped
    networks:
      - nexusarchive-network

  nexusarchive-redis:
    image: redis:latest
    container_name: nexusarchive-redis
    command: redis-server --requirepass \${REDIS_PASSWORD}
    volumes:
      - $DATA_DIR/redis:/data
    ports:
      - "$REDIS_PORT:6379"
    restart: unless-stopped
    networks:
      - nexusarchive-network

networks:
  nexusarchive-network:
    driver: bridge
EOF
    
    # 生成systemd服务文件
    cat > "/tmp/nexusarchive.service" <<EOF
[Unit]
Description=NexusArchive Electronic Accounting Archive System
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF
    
    # 生成启动脚本
    cat > "$INSTALL_DIR/start.sh" <<EOF
#!/bin/bash
# NexusArchive 启动脚本

set -e

SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
cd "\$SCRIPT_DIR"

# 加载环境变量
if [ -f .env ]; then
    export \$(cat .env | grep -v '^#' | xargs)
fi

# 启动服务
echo "启动 NexusArchive 服务..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 检查服务状态
echo "检查服务状态..."
if docker-compose ps | grep -q "Up"; then
    echo "服务启动成功!"
    echo "访问地址: http://localhost:\$HTTP_PORT"
    echo "HTTPS地址: https://localhost:\$HTTPS_PORT"
else
    echo "服务启动失败，请检查日志: docker-compose logs nexusarchive-app"
    exit 1
fi
EOF
    
    # 生成停止脚本
    cat > "$INSTALL_DIR/stop.sh" <<EOF
#!/bin/bash
# NexusArchive 停止脚本

SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
cd "\$SCRIPT_DIR"

# 停止服务
echo "停止 NexusArchive 服务..."
docker-compose down

echo "服务已停止"
EOF
    
    # 生成备份脚本
    cat > "$INSTALL_DIR/backup.sh" <<EOF
#!/bin/bash
# NexusArchive 备份脚本

set -e

SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
cd "\$SCRIPT_DIR"

# 加载环境变量
if [ -f .env ]; then
    export \$(cat .env | grep -v '^#' | xargs)
fi

# 创建备份目录
BACKUP_DIR="\$BACKUP_PATH/\$(date +%Y%m%d_%H%M%S)"
mkdir -p "\$BACKUP_DIR"

# 备份数据库
echo "备份数据库..."
docker exec nexusarchive-db pg_dump -U \${DB_USER} \${DB_NAME} > "\$BACKUP_DIR/database.sql"

# 备份文件
echo "备份文件..."
cp -r "\$FILE_STORAGE_PATH" "\$BACKUP_DIR/files"

# 备份配置
echo "备份配置..."
cp .env "\$BACKUP_DIR/"
cp docker-compose.yml "\$BACKUP_DIR/"

echo "备份完成: \$BACKUP_DIR"
EOF
    
    # 生成恢复脚本
    cat > "$INSTALL_DIR/restore.sh" <<EOF
#!/bin/bash
# NexusArchive 恢复脚本

set -e

if [ \$# -ne 1 ]; then
    echo "用法: \$0 <备份目录>"
    exit 1
fi

BACKUP_DIR=\$1
SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
cd "\$SCRIPT_DIR"

# 检查备份目录
if [ ! -d "\$BACKUP_DIR" ]; then
    echo "错误: 备份目录不存在: \$BACKUP_DIR"
    exit 1
fi

# 加载环境变量
if [ -f .env; then
    export \$(cat .env | grep -v '^#' | xargs)
fi

# 停止服务
echo "停止服务..."
./stop.sh

# 恢复数据库
echo "恢复数据库..."
docker exec -i nexusarchive-db psql -U \${DB_USER} \${DB_NAME} < "\$BACKUP_DIR/database.sql"

# 恢复文件
echo "恢复文件..."
rm -rf "\$FILE_STORAGE_PATH"
cp -r "\$BACKUP_DIR/files" "\$FILE_STORAGE_PATH"

# 启动服务
echo "启动服务..."
./start.sh

echo "恢复完成"
EOF
    
    # 设置脚本权限
    chmod +x "$INSTALL_DIR/start.sh"
    chmod +x "$INSTALL_DIR/stop.sh"
    chmod +x "$INSTALL_DIR/backup.sh"
    chmod +x "$INSTALL_DIR/restore.sh"
    
    log_success "配置文件生成完成"
}

# 执行安装
perform_installation() {
    log_info "开始安装 NexusArchive..."
    
    # 拉取Docker镜像
    log_info "拉取Docker镜像..."
    docker pull nexusarchive/nexusarchive:latest
    docker pull ${DB_TYPE}:latest
    docker pull redis:latest
    
    # 启动服务
    log_info "启动服务..."
    cd "$INSTALL_DIR"
    docker-compose up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 60
    
    # 检查服务状态
    if docker-compose ps | grep -q "Up"; then
        log_success "服务启动成功!"
        log_info "访问地址: http://localhost:$HTTP_PORT"
        log_info "HTTPS地址: https://localhost:$HTTPS_PORT"
    else
        log_error "服务启动失败，请检查日志: docker-compose logs nexusarchive-app"
        exit 1
    fi
    
    # 初始化数据库
    log_info "初始化数据库..."
    docker exec nexusarchive-app sh -c "java -jar app.jar --init-db"
    
    # 安装systemd服务
    if [ "$EUID" -eq 0 ]; then
        cp "/tmp/nexusarchive.service" "/etc/systemd/system/"
        systemctl daemon-reload
        systemctl enable nexusarchive
        log_success "systemd服务已安装并启用"
    else
        log_warn "需要root权限安装systemd服务，请手动运行:"
        log_warn "sudo cp /tmp/nexusarchive.service /etc/systemd/system/"
        log_warn "sudo systemctl daemon-reload"
        log_warn "sudo systemctl enable nexusarchive"
    fi
    
    log_success "NexusArchive 安装完成!"
}

# 显示安装后信息
show_post_install_info() {
    echo ""
    log_info "==================== 安装后信息 ===================="
    log_info "安装目录: $INSTALL_DIR"
    log_info "数据目录: $DATA_DIR"
    log_info "访问地址: http://localhost:$HTTP_PORT"
    log_info "HTTPS地址: https://localhost:$HTTPS_PORT"
    log_info "管理员账号: admin"
    log_info "管理员密码: admin"
    echo ""
    log_info "常用命令:"
    log_info "启动服务: $INSTALL_DIR/start.sh"
    log_info "停止服务: $INSTALL_DIR/stop.sh"
    log_info "备份系统: $INSTALL_DIR/backup.sh"
    log_info "恢复系统: $INSTALL_DIR/restore.sh <备份目录>"
    echo ""
    log_info "配置文件: $INSTALL_DIR/.env"
    log_info "日志目录: $DATA_DIR/logs"
    log_info "备份目录: $DATA_DIR/backups"
    echo ""
    log_info "服务管理:"
    log_info "查看状态: docker-compose ps"
    log_info "查看日志: docker-compose logs nexusarchive-app"
    log_info "进入容器: docker exec -it nexusarchive-app bash"
    echo ""
    log_info "技术支持:"
    log_info "文档: https://docs.nexusarchive.com"
    log_info "社区: https://community.nexusarchive.com"
    log_info "=================================================="
}

# 主函数
main() {
    echo ""
    log_info "==================== NexusArchive 一键安装脚本 ===================="
    log_info "版本: 1.0.0"
    log_info "更新日期: 2025-12-06"
    echo ""
    
    check_system_requirements
    configure_installation
    prepare_environment
    perform_installation
    show_post_install_info
}

# 执行主函数
main "$@"
```

#### 1.2 离线安装包
创建适合无网络环境的离线安装包：

```bash
#!/bin/bash

# NexusArchive 离线安装包生成脚本
# 版本: 1.0.0
# 更新日期: 2025-12-06

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 默认配置
VERSION="2.0.0"
OUTPUT_DIR="./offline-package"
PACKAGE_NAME="nexusarchive-offline-${VERSION}"

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  -v, --version    指定版本号 (默认: 2.0.0)"
            echo "  -o, --output     指定输出目录 (默认: ./offline-package)"
            echo "  -h, --help       显示帮助信息"
            exit 0
            ;;
        *)
            log_error "未知参数: $1"
            exit 1
            ;;
    esac
done

# 设置变量
PACKAGE_PATH="${OUTPUT_DIR}/${PACKAGE_NAME}"
ARCHIVE_NAME="${PACKAGE_NAME}.tar.gz"

log_info "创建离线安装包..."
log_info "版本: $VERSION"
log_info "输出目录: $OUTPUT_DIR"
log_info "包名: $ARCHIVE_NAME"

# 清理并创建目录
rm -rf "$PACKAGE_PATH"
mkdir -p "$PACKAGE_PATH"

# 1. 复制安装脚本
log_info "复制安装脚本..."
cp scripts/install.sh "$PACKAGE_PATH/"
cp scripts/offline-install.sh "$PACKAGE_PATH/"
chmod +x "$PACKAGE_PATH/install.sh"
chmod +x "$PACKAGE_PATH/offline-install.sh"

# 2. 下载并保存Docker镜像
log_info "下载Docker镜像..."
mkdir -p "$PACKAGE_PATH/images"

# 下载应用镜像
docker pull nexusarchive/nexusarchive:$VERSION
docker save nexusarchive/nexusarchive:$VERSION | gzip > "$PACKAGE_PATH/images/nexusarchive-${VERSION}.tar.gz"

# 下载数据库镜像
docker pull postgres:13
docker save postgres:13 | gzip > "$PACKAGE_PATH/images/postgres-13.tar.gz"

# 下载Redis镜像
docker pull redis:6-alpine
docker save redis:6-alpine | gzip > "$PACKAGE_PATH/images/redis-6-alpine.tar.gz"

# 下载Nginx镜像
docker pull nginx:alpine
docker save nginx:alpine | gzip > "$PACKAGE_PATH/images/nginx-alpine.tar.gz"

# 3. 复制应用文件
log_info "复制应用文件..."
mkdir -p "$PACKAGE_PATH/app"

# 复制前端文件
cp -r dist "$PACKAGE_PATH/app/"

# 复制后端JAR文件
cp nexusarchive-java/target/nexusarchive-backend-$VERSION.jar "$PACKAGE_PATH/app/"

# 4. 复制数据库迁移脚本
log_info "复制数据库迁移脚本..."
mkdir -p "$PACKAGE_PATH/db/migrations"
cp -r nexusarchive-java/src/main/resources/db/migration/* "$PACKAGE_PATH/db/migrations/"

# 5. 复制配置文件
log_info "复制配置文件..."
mkdir -p "$PACKAGE_PATH/config"

# 复制Docker Compose文件
cp docker/docker-compose.yml "$PACKAGE_PATH/config/"
cp docker/docker-compose.offline.yml "$PACKAGE_PATH/config/"

# 复制Nginx配置
cp docker/nginx.conf "$PACKAGE_PATH/config/"

# 复制systemd服务文件
cp docker/nexusarchive.service "$PACKAGE_PATH/config/"

# 6. 复制文档
log_info "复制文档..."
mkdir -p "$PACKAGE_PATH/docs"

cp README.md "$PACKAGE_PATH/docs/"
cp -r docs/* "$PACKAGE_PATH/docs/"

# 7. 生成离线安装配置
log_info "生成离线安装配置..."
cat > "$PACKAGE_PATH/config/offline.env" <<EOF
# NexusArchive 离线安装配置
# 生成时间: $(date)

# 应用配置
NEXUSARCHIVE_VERSION=$VERSION
NEXUSARCHIVE_ADMIN_USER=admin
NEXUSARCHIVE_ADMIN_PASSWORD=admin
NEXUSARCHIVE_JWT_SECRET=$(openssl rand -base64 64)
NEXUSARCHIVE_SM4_KEY=$(openssl rand -hex 16)

# 数据库配置
POSTGRES_DB=nexusarchive
POSTGRES_USER=nexusarchive
POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)

# Redis配置
REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)

# 文件存储配置
FILE_STORAGE_PATH=/opt/nexusarchive/data/files

# 日志配置
LOG_PATH=/opt/nexusarchive/data/logs

# 备份配置
BACKUP_PATH=/opt/nexusarchive/data/backups
BACKUP_RETENTION_DAYS=30
EOF

# 8. 生成离线安装脚本
log_info "生成离线安装脚本..."
cat > "$PACKAGE_PATH/offline-install.sh" <<'EOF'
#!/bin/bash

# NexusArchive 离线安装脚本
# 版本: 1.0.0

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 默认配置
DEFAULT_INSTALL_DIR="/opt/nexusarchive"
DEFAULT_DATA_DIR="/opt/nexusarchive/data"
DEFAULT_HTTP_PORT=8080
DEFAULT_HTTPS_PORT=8443

# 检查系统要求
check_system_requirements() {
    log_info "检查系统要求..."
    
    # 检查操作系统
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        log_success "操作系统: Linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        log_success "操作系统: macOS"
    else
        log_error "不支持的操作系统: $OSTYPE"
        exit 1
    fi
    
    # 检查Docker
    if command -v docker &> /dev/null; then
        log_success "Docker 已安装: $(docker --version)"
    else
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if command -v docker-compose &> /dev/null; then
        log_success "Docker Compose 已安装: $(docker-compose --version)"
    else
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    # 检查内存
    MEMORY_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
    MEMORY_GB=$((MEMORY_KB / 1024 / 1024))
    
    if [[ $MEMORY_GB -lt 8 ]]; then
        log_error "系统内存不足，至少需要8GB，当前: ${MEMORY_GB}GB"
        exit 1
    else
        log_success "系统内存: ${MEMORY_GB}GB"
    fi
    
    # 检查磁盘空间
    DISK_AVAILABLE=$(df -BG . | tail -1 | awk '{print $4}' | tr -d 'G')
    
    if [[ $DISK_AVAILABLE -lt 50 ]]; then
        log_error "磁盘空间不足，至少需要50GB，当前可用: ${DISK_AVAILABLE}GB"
        exit 1
    else
        log_success "磁盘空间: ${DISK_AVAILABLE}GB可用"
    fi
    
    log_success "系统要求检查完成"
}

# 配置安装参数
configure_installation() {
    log_info "配置安装参数..."
    
    # 读取默认配置
    if [ -f config/offline.env ]; then
        source config/offline.env
    else
        log_error "离线配置文件不存在: config/offline.env"
        exit 1
    fi
    
    # 读取用户输入
    echo ""
    log_info "请输入安装配置参数（直接回车使用默认值）:"
    
    read -p "安装目录 [$DEFAULT_INSTALL_DIR]: " INSTALL_DIR
    INSTALL_DIR=${INSTALL_DIR:-$DEFAULT_INSTALL_DIR}
    
    read -p "数据目录 [$DEFAULT_DATA_DIR]: " DATA_DIR
    DATA_DIR=${DATA_DIR:-$DEFAULT_DATA_DIR}
    
    read -p "HTTP端口 [$DEFAULT_HTTP_PORT]: " HTTP_PORT
    HTTP_PORT=${HTTP_PORT:-$DEFAULT_HTTP_PORT}
    
    read -p "HTTPS端口 [$DEFAULT_HTTPS_PORT]: " HTTPS_PORT
    HTTPS_PORT=${HTTPS_PORT:-$DEFAULT_HTTPS_PORT}
    
    # 确认配置
    echo ""
    log_info "安装配置摘要:"
    echo "安装目录: $INSTALL_DIR"
    echo "数据目录: $DATA_DIR"
    echo "HTTP端口: $HTTP_PORT"
    echo "HTTPS端口: $HTTPS_PORT"
    echo ""
    
    read -p "确认配置并继续安装? (y/n): " CONFIRM
    if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
        log_info "安装已取消"
        exit 0
    fi
}

# 准备安装环境
prepare_environment() {
    log_info "准备安装环境..."
    
    # 创建目录
    sudo mkdir -p "$INSTALL_DIR"
    sudo mkdir -p "$DATA_DIR"
    sudo mkdir -p "$DATA_DIR/files"
    sudo mkdir -p "$DATA_DIR/logs"
    sudo mkdir -p "$DATA_DIR/backups"
    
    # 设置权限
    sudo chown -R $USER:$USER "$INSTALL_DIR"
    sudo chown -R $USER:$USER "$DATA_DIR"
    
    # 复制应用文件
    cp -r app/* "$INSTALL_DIR/"
    
    # 复制配置文件
    cp -r config/* "$INSTALL_DIR/"
    
    # 生成配置文件
    generate_config_files
    
    log_success "安装环境准备完成"
}

# 加载Docker镜像
load_docker_images() {
    log_info "加载Docker镜像..."
    
    # 加载应用镜像
    docker load -i images/nexusarchive-${NEXUSARCHIVE_VERSION}.tar.gz
    
    # 加载数据库镜像
    docker load -i images/postgres-13.tar.gz
    
    # 加载Redis镜像
    docker load -i images/redis-6-alpine.tar.gz
    
    # 加载Nginx镜像
    docker load -i images/nginx-alpine.tar.gz
    
    log_success "Docker镜像加载完成"
}

# 生成配置文件
generate_config_files() {
    log_info "生成配置文件..."
    
    # 生成环境变量文件
    cat > "$INSTALL_DIR/.env" <<EOF
# NexusArchive 环境配置
# 生成时间: $(date)

# 基础配置
INSTALL_DIR=$INSTALL_DIR
DATA_DIR=$DATA_DIR
HTTP_PORT=$HTTP_PORT
HTTPS_PORT=$HTTPS_PORT

# 数据库配置
POSTGRES_DB=$POSTGRES_DB
POSTGRES_USER=$POSTGRES_USER
POSTGRES_PASSWORD=$POSTGRES_PASSWORD

# Redis配置
REDIS_PASSWORD=$REDIS_PASSWORD

# 安全配置
NEXUSARCHIVE_JWT_SECRET=$NEXUSARCHIVE_JWT_SECRET
NEXUSARCHIVE_SM4_KEY=$NEXUSARCHIVE_SM4_KEY
NEXUSARCHIVE_ADMIN_USER=$NEXUSARCHIVE_ADMIN_USER
NEXUSARCHIVE_ADMIN_PASSWORD=$NEXUSARCHIVE_ADMIN_PASSWORD

# 文件存储配置
FILE_STORAGE_PATH=$FILE_STORAGE_PATH

# 日志配置
LOG_PATH=$LOG_PATH

# 备份配置
BACKUP_PATH=$BACKUP_PATH
BACKUP_RETENTION_DAYS=$BACKUP_RETENTION_DAYS
EOF
    
    # 修改Docker Compose文件
    sed -i "s|8080:8080|$HTTP_PORT:8080|g" "$INSTALL_DIR/docker-compose.offline.yml"
    sed -i "s|8443:8443|$HTTPS_PORT:8443|g" "$INSTALL_DIR/docker-compose.offline.yml"
    
    log_success "配置文件生成完成"
}

# 执行安装
perform_installation() {
    log_info "开始安装 NexusArchive..."
    
    # 启动服务
    cd "$INSTALL_DIR"
    docker-compose -f docker-compose.offline.yml up -d
    
    # 等待服务启动
    log_info "等待服务启动..."
    sleep 60
    
    # 检查服务状态
    if docker-compose -f docker-compose.offline.yml ps | grep -q "Up"; then
        log_success "服务启动成功!"
        log_info "访问地址: http://localhost:$HTTP_PORT"
        log_info "HTTPS地址: https://localhost:$HTTPS_PORT"
    else
        log_error "服务启动失败，请检查日志: docker-compose -f docker-compose.offline.yml logs nexusarchive-app"
        exit 1
    fi
    
    # 安装systemd服务
    if [ "$EUID" -eq 0 ]; then
        cp nexusarchive.service /etc/systemd/system/
        systemctl daemon-reload
        systemctl enable nexusarchive
        log_success "systemd服务已安装并启用"
    else
        log_warn "需要root权限安装systemd服务，请手动运行:"
        log_warn "sudo cp nexusarchive.service /etc/systemd/system/"
        log_warn "sudo systemctl daemon-reload"
        log_warn "sudo systemctl enable nexusarchive"
    fi
    
    log_success "NexusArchive 安装完成!"
}

# 显示安装后信息
show_post_install_info() {
    echo ""
    log_info "==================== 安装后信息 ===================="
    log_info "安装目录: $INSTALL_DIR"
    log_info "数据目录: $DATA_DIR"
    log_info "访问地址: http://localhost:$HTTP_PORT"
    log_info "HTTPS地址: https://localhost:$HTTPS_PORT"
    log_info "管理员账号: $NEXUSARCHIVE_ADMIN_USER"
    log_info "管理员密码: $NEXUSARCHIVE_ADMIN_PASSWORD"
    echo ""
    log_info "常用命令:"
    log_info "启动服务: cd $INSTALL_DIR && docker-compose -f docker-compose.offline.yml up -d"
    log_info "停止服务: cd $INSTALL_DIR && docker-compose -f docker-compose.offline.yml down"
    echo ""
    log_info "配置文件: $INSTALL_DIR/.env"
    log_info "日志目录: $DATA_DIR/logs"
    log_info "备份目录: $DATA_DIR/backups"
    echo ""
    log_info "服务管理:"
    log_info "查看状态: docker-compose -f docker-compose.offline.yml ps"
    log_info "查看日志: docker-compose -f docker-compose.offline.yml logs nexusarchive-app"
    echo ""
    log_info "技术支持:"
    log_info "文档: docs/"
    log_info "=================================================="
}

# 主函数
main() {
    echo ""
    log_info "==================== NexusArchive 离线安装脚本 ===================="
    echo ""
    
    check_system_requirements
    configure_installation
    prepare_environment
    load_docker_images
    perform_installation
    show_post_install_info
}

# 执行主函数
main "$@"
EOF

chmod +x "$PACKAGE_PATH/offline-install.sh"

# 9. 生成安装说明
log_info "生成安装说明..."
cat > "$PACKAGE_PATH/README.md" <<EOF
# NexusArchive 电子会计档案系统 - 离线安装包

## 版本信息
- 版本号: $VERSION
- 发布日期: $(date +%Y-%m-%d)

## 安装要求

### 硬件要求
- CPU: 4核心或以上
- 内存: 8GB或以上
- 硬盘: 50GB可用空间或以上

### 软件要求
- 操作系统: Linux (推荐CentOS 7+, Ubuntu 18+) 或 macOS
- Docker: 20.10+
- Docker Compose: 1.29+

## 安装步骤

### 1. 解压安装包
\`\`\`bash
tar -xzf nexusarchive-offline-$VERSION.tar.gz
cd nexusarchive-offline-$VERSION
\`\`\`

### 2. 运行安装脚本
\`\`\`bash
./offline-install.sh
\`\`\`

### 3. 配置安装参数
按照提示输入安装配置参数，或直接回车使用默认值。

### 4. 等待安装完成
安装过程可能需要10-20分钟，具体时间取决于系统性能。

## 安装后验证

### 1. 检查服务状态
\`\`\`bash
docker-compose ps
\`\`\`

### 2. 访问系统
- HTTP地址: http://localhost:8080
- HTTPS地址: https://localhost:8443
- 默认账号: admin
- 默认密码: admin

## 常用操作

### 启动服务
\`\`\`bash
docker-compose up -d
\`\`\`

### 停止服务
\`\`\`bash
docker-compose down
\`\`\`

### 查看日志
\`\`\`bash
docker-compose logs nexusarchive-app
\`\`\`

### 更新系统
\`\`\`bash
# 备份数据
./backup.sh

# 下载新版本离线包
# 解压并覆盖应用文件
# 重启服务
docker-compose restart
\`\`\`

## 故障排除

### 端口冲突
如果遇到端口冲突，可以修改 \`.env\` 文件中的端口配置：
\`\`\`
HTTP_PORT=8080
HTTPS_PORT=8443
\`\`\`

### 内存不足
如果系统内存不足，可以调整Docker资源限制：
\`\`\`
# 编辑 docker-compose.yml
services:
  nexusarchive-app:
    mem_limit: 4g
\`\`\`

### 磁盘空间不足
如果磁盘空间不足，可以清理不必要的日志和备份：
\`\`\`
# 清理Docker日志
docker system prune -a

# 清理过期备份
find /opt/nexusarchive/data/backups -type d -mtime +30 -exec rm -rf {} \;
\`\`\`

## 技术支持

- 文档: docs/
- 社区: https://community.nexusarchive.com
- 邮箱: support@nexusarchive.com
EOF

# 10. 生成版本信息文件
log_info "生成版本信息文件..."
cat > "$PACKAGE_PATH/VERSION" <<EOF
NexusArchive Electronic Accounting Archive System
Version: $VERSION
Build Date: $(date)
Git Commit: $(git rev-parse HEAD 2>/dev/null || echo "unknown")
EOF

# 11. 创建压缩包
log_info "创建压缩包..."
cd "$OUTPUT_DIR"
tar -czf "$ARCHIVE_NAME" "$PACKAGE_NAME"

# 计算文件大小
PACKAGE_SIZE=$(du -h "$ARCHIVE_NAME" | cut -f1)

# 生成校验和
SHA256_SUM=$(sha256sum "$ARCHIVE_NAME" | cut -d' ' -f1)

# 生成安装包信息
cat > "$PACKAGE_NAME.info" <<EOF
NexusArchive 离线安装包信息
========================
版本: $VERSION
大小: $PACKAGE_SIZE
校验和(SHA256): $SHA256
生成时间: $(date)
========================
EOF

log_success "离线安装包创建完成!"
log_info "包路径: $OUTPUT_DIR/$ARCHIVE_NAME"
log_info "包大小: $PACKAGE_SIZE"
log_info "校验和: $SHA256"

# 清理临时目录
rm -rf "$PACKAGE_PATH"

log_info "临时文件已清理，离线安装包创建完成!"
EOF

chmod +x scripts/create-offline-package.sh

echo ""
log_success "离线安装包生成脚本创建完成!"
log_info "使用方法: ./scripts/create-offline-package.sh"
log_info "选项:"
log_info "  -v, --version    指定版本号"
log_info "  -o, --output     指定输出目录"
log_info "  -h, --help       显示帮助信息"