# 借阅审批类型一致性修复计划

修复 `BorrowApprovalDrawer.tsx` 组件与 `borrowing.ts` API 层之间的 TypeScript 类型错误和方法调用不规范。

## 修改建议

### API 层 [borrowing.ts]
#### [MODIFY] [borrowing.ts](file:///Users/user/nexusarchive/src/api/borrowing.ts)
- 扩展 `BorrowingRecord` 接口，补全 `userId`, `userName`, `borrowPurpose`, `urgencyLevel`, `contactInfo`, `createdTime` 等关键字段。
- 将 `approveBorrowing` 的 `payload` 参数中的 `approverId` 和 `approverName` 改为可选（通常后端可从 Token 获取）。
- 将 `payload` 中的 `comment` 类型改为 `string | undefined`。
- 将 `returnArchive` 方法重命名为 `userReturnArchive`，以匹配组件调用，并将 `operatorId` 参数改为可选。

### 业务组件 [BorrowApprovalDrawer.tsx]
#### [MODIFY] [BorrowApprovalDrawer.tsx](file:///Users/user/nexusarchive/src/components/borrowing/BorrowApprovalDrawer.tsx)
- 移除组件内部定义的 `BorrowingRecord` 接口，改为直接从 `../../api/borrowing` 导入，确保类型源唯一。
- 修正 `handleUserReturn` 的调用逻辑，确保与 API 签名保持一致。
- 修正 `handleApprove` 中传递给 API 的参数结构。

## 验证计划

### 自动化测试
- 运行 TypeScript 类型检查，确保 `BorrowApprovalDrawer.tsx` 无类型错误：
  ```bash
  npx tsc --noEmit src/components/borrowing/BorrowApprovalDrawer.tsx
  ```
- 运行相关的单元测试（如果存在）：
  ```bash
  npm run test -- src/components/borrowing/
  ```

### 手动验证
- 检查 `BorrowApprovalDrawer` 的属性和 API 调用是否在 IDE 中能正确补全和提示。
