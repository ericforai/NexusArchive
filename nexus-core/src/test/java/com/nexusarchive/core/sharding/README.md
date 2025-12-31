一旦我所属的文件夹有所变化，请更新我。
本目录存放 Sharding-JDBC 隔离 POC（适配 5.4.1 API）。
用于验证全宗分片与双键分片（fonds_no + fiscal_year）行为。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `FondsStrictShardingAlgorithm.java` | 类 | 全宗强制分片算法 |
| `FondsStrictShardingAlgorithmTests.java` | 测试 | 分片算法验证 |
| `FondsYearComplexShardingAlgorithm.java` | 类 | 双键复合分片算法 |
| `FondsYearComplexShardingAlgorithmTests.java` | 测试 | 双键分片回归测试 |
| `ShardingIsolationEndToEndTests.java` | 测试 | 端到端隔离验证 |
