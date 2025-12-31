// Input: JUnit
// Output: Fonds isolation red-team tests
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FondsIsolationInterceptorTests {
    private final FondsIsolationInterceptor interceptor = new FondsIsolationInterceptor();

    @AfterEach
    void tearDown() {
        FondsContext.clear();
        ArchiveYearContext.clear();
    }

    @Test
    void shouldInjectFondsForSelect() {
        FondsContext.setFondsNo("F001");
        ArchiveYearContext.setArchiveYear(2024);
        String sql = "SELECT * "
                + "FROM acc_archive ORDER BY created_time DESC";
        String result = interceptor.applyIsolation(sql);
        String expected = "SELECT * "
                + "FROM acc_archive WHERE fonds_no = 'F001' AND fiscal_year = 2024 ORDER BY created_time DESC";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void shouldSkipNonProtectedSql() {
        FondsContext.setFondsNo("F001");
        ArchiveYearContext.setArchiveYear(2024);
        String sql = "SELECT * "
                + "FROM sys_user WHERE id = 'U1'";
        String result = interceptor.applyIsolation(sql);
        Assertions.assertEquals(sql, result);
    }

    @Test
    void shouldRejectDmlWithoutFonds() {
        FondsContext.setFondsNo("F001");
        ArchiveYearContext.setArchiveYear(2024);
        String sql = "UPDATE acc_archive "
                + "SET status = 'ARCHIVED' WHERE id = 'A1'";
        Assertions.assertThrows(FondsIsolationException.class, () -> interceptor.applyIsolation(sql));
    }

    @Test
    void shouldRejectInvalidFondsNo() {
        FondsContext.setFondsNo("F001' OR 1=1 --");
        ArchiveYearContext.setArchiveYear(2024);
        String sql = "SELECT * "
                + "FROM acc_archive";
        Assertions.assertThrows(FondsIsolationException.class, () -> interceptor.applyIsolation(sql));
    }

    @Test
    void shouldRespectExistingFondsClause() {
        FondsContext.setFondsNo("F001");
        ArchiveYearContext.setArchiveYear(2024);
        String sql = "SELECT * "
                + "FROM acc_archive WHERE fonds_no = 'F999' AND fiscal_year = 2024";
        String result = interceptor.applyIsolation(sql);
        Assertions.assertEquals(sql, result);
    }

    @Test
    void shouldRejectMissingArchiveYearContext() {
        FondsContext.setFondsNo("F001");
        String sql = "SELECT * "
                + "FROM acc_archive";
        Assertions.assertThrows(FondsIsolationException.class, () -> interceptor.applyIsolation(sql));
    }
}
