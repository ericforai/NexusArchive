// Input: ThreadLocal context
// Output: Fonds scope accessor
// Pos: NexusCore isolation
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.util.Objects;
import java.util.regex.Pattern;

public final class FondsContext {
    private static final ThreadLocal<String> CURRENT_FONDS = new ThreadLocal<>();
    private static final Pattern SAFE_FONDS = Pattern.compile("^[A-Za-z0-9_-]+$");

    private FondsContext() {
    }

    public static void setFondsNo(String fondsNo) {
        CURRENT_FONDS.set(fondsNo);
    }

    public static String requireFondsNo() {
        String fondsNo = CURRENT_FONDS.get();
        if (fondsNo == null || fondsNo.isBlank()) {
            throw new FondsIsolationException("Missing fonds_no in context");
        }
        if (!SAFE_FONDS.matcher(fondsNo).matches()) {
            throw new FondsIsolationException("Invalid fonds_no format");
        }
        return fondsNo;
    }

    public static boolean isActive() {
        String fondsNo = CURRENT_FONDS.get();
        return Objects.nonNull(fondsNo) && !fondsNo.isBlank();
    }

    public static void clear() {
        CURRENT_FONDS.remove();
    }
}
