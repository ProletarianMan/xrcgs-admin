package com.xrcgs.file.util;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * SHA-256检测
 */
public final class DigestUtils {
    private DigestUtils(){}

    public static String sha256Hex(InputStream in) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            md.update(buf, 0, len);
        }
        byte[] digest = md.digest();
        return HexFormat.of().withUpperCase().formatHex(digest).toLowerCase();
    }
}
