package com.xrcgs.file.util;

import com.xrcgs.file.config.FileProperties;
import com.xrcgs.file.enums.FileType;
import org.apache.tika.Tika;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Locale;

/**
 * MimeUtils（MIME 检测、白名单校验、扩展名归一化）
 */
public final class MimeUtils {
    private static final Tika TIKA = new Tika();

    private MimeUtils(){}

    public static String detectMime(InputStream in, String filenameFallback) throws Exception {
        // 先用 tika；失败时根据文件名推断
        String mime = TIKA.detect(in, filenameFallback);
        return StringUtils.hasText(mime) ? mime : "application/octet-stream";
    }

    public static String normalizeExt(String originalName) {
        String ext = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot + 1);
            }
        }
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }

    public static boolean allowed(String ext, String mime, FileProperties props) {
        String e = (ext == null ? "" : ext.replace(".", "").toLowerCase(Locale.ROOT));
        boolean extOk = props.getAllowedExts().stream()
                .map(s -> s.replace(".", "").toLowerCase(Locale.ROOT))
                .anyMatch(s -> s.equals(e));
        boolean mimeOk = props.getAllowedMimes().stream()
                .anyMatch(allowed -> mime != null && (allowed.endsWith("/") ? mime.startsWith(allowed) : allowed.equalsIgnoreCase(mime)));
        return extOk && mimeOk;
    }

    public static FileType classify(String mime) {
        return FileType.fromMime(mime);
    }
}
