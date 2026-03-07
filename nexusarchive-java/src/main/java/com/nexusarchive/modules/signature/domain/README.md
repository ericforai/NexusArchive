一旦我所属的文件夹有所变化，请更新我。
本目录存放签名验签模块的领域层。

## 职责

- 定义验签适配端口 `SignatureVerificationPort`
- 定义纯业务结果模型与聚合 `ArchiveSignatureVerification`
- 定义仓储接口 `ArchiveSignatureVerificationRepository`

## 依赖规则

- ✅ 可依赖: JDK、Lombok
- ❌ 禁止依赖: Spring、MyBatis、具体 PDF/OFD SDK
