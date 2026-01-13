一旦我所属的文件夹有所变化，请更新我。

# 坏文件样本库 (test-samples)

> 一旦我所属的文件夹有所变化，请更新我。

## 目录说明

用于四性检测引擎的测试样本，包含各类"坏文件"以验证检测能力。

**警告**: 部分文件用于安全测试，请勿在非测试环境使用。

## 文件清单

| 文件 | 类型 | 用途 | 预期行为 |
| --- | --- | --- | --- |
| `eicar.txt` | 病毒样本 | ClamAV 检测 | 报警: Eicar-Test-Signature |
| `fake-pdf.exe` | 改后缀 | Magic Number 检测 | 检测为: UNKNOWN (非 PDF) |
| `valid-invoice.xml` | 正常 XML | 完整性检测 | 通过 |
| `mismatched-amount.xml` | 金额不一致 | 完整性检测 | 失败: amount 不一致 |
| `expired-signature.pdf` | 过期签名 | 签名验证 | 失败: 证书过期 |
| `no-signature.pdf` | 无签名 | 签名验证 | 返回: 无签名 |

## 使用方式

```java
Path sampleDir = Paths.get("src/test/resources/test-samples");
Path eicar = sampleDir.resolve("eicar.txt");
VirusScanResult result = virusScanService.scan(eicar);
assertFalse(result.clean());
```
