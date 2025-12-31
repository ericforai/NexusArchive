// Input: ThreadLocal context
// Output: archive_year 作用域读取与校验
// Pos: NexusCore 归档年度上下文
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.time.Year;

public final class ArchiveYearContext {
    private static final ThreadLocal<Integer> CURRENT_ARCHIVE_YEAR = new ThreadLocal<>();
    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = Year.now().getValue() + 5;

    private ArchiveYearContext() {
    }

    public static void setArchiveYear(Integer archiveYear) {
        CURRENT_ARCHIVE_YEAR.set(archiveYear);
    }

    public static int requireArchiveYear() {
        Integer archiveYear = CURRENT_ARCHIVE_YEAR.get();
        if (archiveYear == null) {
            throw new FondsIsolationException("Missing archive_year in context");
        }
        if (archiveYear < MIN_YEAR || archiveYear > MAX_YEAR) {
            throw new FondsIsolationException("Invalid archive_year range");
        }
        return archiveYear;
    }

    public static void clear() {
        CURRENT_ARCHIVE_YEAR.remove();
    }
}
