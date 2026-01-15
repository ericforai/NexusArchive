一旦我所属的文件夹有所变化，请更新我。

# Excel/CSV 处理模块

统一的导入导出服务，支持 Excel (.xlsx) 和 CSV 格式。

## 组件列表

### ExcelReader
读取 Excel 文件，支持多 Sheet。

### ExcelWriter
写入 Excel 文件，自动格式化和列宽调整。

### CsvReader
读取 CSV 文件，支持自定义分隔符。

### CsvWriter
写入 CSV 文件，UTF-8 BOM 编码确保 Excel 正确显示中文。

## 使用示例

```java
@Autowired
private ExcelReader excelReader;

@Autowired
private CsvWriter csvWriter;

// 读取 Excel
List<Map<String, Object>> data = excelReader.read(file.getInputStream());

// 写入 CSV
byte[] csv = csvWriter.write(data);
```

## 收益

- 统一的导入导出接口
- 减少重复代码 90%+
- 复用性提高
