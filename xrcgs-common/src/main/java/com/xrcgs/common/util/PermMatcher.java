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
    public static boolean match(Set<String> userPerms, String targetPerm) {
        if (userPerms == null || userPerms.isEmpty() || targetPerm == null) return false;
        String[] t = split(targetPerm);
        for (String p : userPerms) {
            if (p == null) continue;
            String[] s = split(p);
            if (part(s[0], t[0]) && part(s[1], t[1]) && part(s[2], t[2])) return true;
        }
        return false;
    }
    private static boolean part(String a, String b){ return "*".equals(a) || a.equalsIgnoreCase(b); }
    private static String[] split(String perm){
        String[] arr = perm.split(":", 3);
        return new String[]{ arr.length>0?arr[0]:"*", arr.length>1?arr[1]:"*", arr.length>2?arr[2]:"*" };
    }
}
