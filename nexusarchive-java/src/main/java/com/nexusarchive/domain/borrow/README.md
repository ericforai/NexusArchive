一旦我所属的文件夹有所变化，请更新我。

# Borrow Domain
包含借阅业务的领域对象和指令（Commands）。

## 文件列表
- `SubmitBorrowRequestCommand.java`: **提交借阅申请指令** (Record)。
  - 功能：封装申请人信息、档案列表和日期范围，包含基础校验。
- `ApproveBorrowRequestCommand.java`: **审批借阅申请指令** (Record)。
  - 功能：封装审批人、结果及意见。
