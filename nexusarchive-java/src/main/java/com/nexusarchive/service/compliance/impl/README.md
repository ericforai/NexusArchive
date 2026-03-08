一旦我所属的文件夹有所变化，请更新我。

# impl

本目录存放合规/四性检测相关的服务实现。
当前包含异步档案四性检测任务执行逻辑，以及任务内的签名验证结果留痕。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `AsyncFourNatureCheckServiceImpl.java` | Java 类 | 异步执行档案四性检测，并将 PDF/OFD 的验签结果写入 `arc_signature_log` |
