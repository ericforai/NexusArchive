// Input: ThreadLocal、Java 标准库
// Output: FondsContext 类
// Pos: 安全模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.security;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class FondsContext {
    private static final ThreadLocal<String> CURRENT_FONDS = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> ALLOWED_FONDS = new ThreadLocal<>();
    private static final Pattern SAFE_FONDS = Pattern.compile("^[A-Za-z0-9_-]+$");

    private FondsContext() {
    }

    public static void setCurrentFondsNo(String fondsNo) {
        CURRENT_FONDS.set(fondsNo);
    }

    public static String getCurrentFondsNo() {
        return CURRENT_FONDS.get();
    }

    public static String requireCurrentFondsNo() {
        String fondsNo = CURRENT_FONDS.get();
        if (fondsNo == null || fondsNo.isBlank()) {
            throw new IllegalStateException("Missing fonds_no in context");
        }
        if (!SAFE_FONDS.matcher(fondsNo).matches()) {
            throw new IllegalStateException("Invalid fonds_no format");
        }
        return fondsNo;
    }

    public static void setAllowedFonds(List<String> fondsList) {
        if (fondsList == null || fondsList.isEmpty()) {
            ALLOWED_FONDS.set(Collections.emptyList());
            return;
        }
        ALLOWED_FONDS.set(Collections.unmodifiableList(fondsList));
    }

    public static List<String> getAllowedFonds() {
        List<String> list = ALLOWED_FONDS.get();
        return Objects.requireNonNullElse(list, Collections.emptyList());
    }

    public static boolean isActive() {
        String fondsNo = CURRENT_FONDS.get();
        return fondsNo != null && !fondsNo.isBlank();
    }

    public static void clear() {
        CURRENT_FONDS.remove();
        ALLOWED_FONDS.remove();
    }
}
