// Input: DDL 生成与类型映射测试
// Output: 适配器原型验证
// Pos: NexusCore 测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import com.nexusarchive.core.adapter.ColumnDefinition;
import com.nexusarchive.core.adapter.DataType;
import com.nexusarchive.core.adapter.DbAdapter;
import com.nexusarchive.core.adapter.DbAdapters;
import com.nexusarchive.core.adapter.DbVendor;
import com.nexusarchive.core.adapter.SchemaManager;
import com.nexusarchive.core.adapter.TableDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DbAdapterPrototypeTests {
    @Test
    void postgresUsesJsonbMapping() {
        DbAdapter adapter = DbAdapters.forVendor(DbVendor.POSTGRESQL);
        String ddl = renderCreateTable(adapter);
        assertTrue(ddl.contains("JSONB"));
    }

    @Test
    void damengUsesClobForJson() {
        DbAdapter adapter = DbAdapters.forVendor(DbVendor.DAMENG);
        String ddl = renderCreateTable(adapter);
        assertTrue(ddl.contains("CLOB"));
    }

    @Test
    void kingbaseUsesJsonType() {
        DbAdapter adapter = DbAdapters.forVendor(DbVendor.KINGBASE);
        String ddl = renderCreateTable(adapter);
        assertTrue(ddl.contains(" JSON"));
    }

    @Test
    void primaryKeyIncludesFondsAndYear() {
        DbAdapter adapter = DbAdapters.forVendor(DbVendor.POSTGRESQL);
        String ddl = renderCreateTable(adapter);
        assertTrue(ddl.contains("PRIMARY KEY (fonds_no, archive_year)"));
    }

    private String renderCreateTable(DbAdapter adapter) {
        TableDefinition table = TableDefinition.builder("arc_account_item")
                .addColumn(ColumnDefinition.builder("fonds_no", DataType.STRING)
                        .length(32)
                        .primaryKey(true)
                        .nullable(false)
                        .comment("全宗号")
                        .build())
                .addColumn(ColumnDefinition.builder("archive_year", DataType.INTEGER)
                        .primaryKey(true)
                        .nullable(false)
                        .build())
                .addColumn(ColumnDefinition.builder("metadata", DataType.JSON)
                        .nullable(true)
                        .build())
                .comment("会计档案案卷")
                .build();
        List<String> statements = new SchemaManager(adapter).generateCreateTable(table);
        return String.join(" ", statements);
    }
}
