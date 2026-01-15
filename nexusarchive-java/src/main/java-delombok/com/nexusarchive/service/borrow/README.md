一旦我所属的文件夹有所变化，请更新我。

# Borrow Service
包含借阅业务的核心逻辑。

## 文件列表
- `BorrowRequestService.java`: **借阅申请核心接口**。
  - 定义了提交、审批、出库、归还的原子化契约。
- `impl/BorrowRequestServiceImpl.java`: **借阅申请实现类**。
  - 核心逻辑实现，采用强校验和明确的状态机。
