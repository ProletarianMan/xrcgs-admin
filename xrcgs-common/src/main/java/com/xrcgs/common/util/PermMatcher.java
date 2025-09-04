package com.xrcgs.common.util;

import java.util.Set;

/**
 * 统一权限码格式：module:resource:action
 * 支持通配符：
 *   - iam:*           -> 匹配 iam 模块下全部
 *   - iam:user:*      -> 匹配 iam:user 的所有 action
 *   - *:*:*           -> 超级通配（谨慎使用）
 */
public final class PermMatcher {
    private PermMatcher(){}

    /** 判断 userPerms 是否包含 targetPerm（考虑通配符） */
    public static boolean match(Set<String> userPerms, String targetPerm) {
        if (userPerms == null || userPerms.isEmpty() || targetPerm == null) return false;
        String[] t = split(targetPerm);
        for (String p : userPerms) {
            if (p == null) continue;
            String[] s = split(p);
            if (partMatch(s[0], t[0]) && partMatch(s[1], t[1]) && partMatch(s[2], t[2])) {
                return true;
            }
        }
        return false;
    }

    private static boolean partMatch(String src, String tgt) {
        return "*".equals(src) || src.equalsIgnoreCase(tgt);
    }

    private static String[] split(String perm) {
        String[] arr = perm.split(":", 3);
        String a = arr.length > 0 ? arr[0] : "*";
        String b = arr.length > 1 ? arr[1] : "*";
        String c = arr.length > 2 ? arr[2] : "*";
        return new String[]{a, b, c};
    }
}
