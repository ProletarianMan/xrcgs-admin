package com.xrcgs.file.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 范围发送器
 * 和基础设计模块进行通信
 */
public final class RangeSender {

    private RangeSender(){}

    /**
     * 发送下载传输
     * @param file 文件地址
     * @param contentType 文件类型
     * @param downloadName 下载名称
     * @param inline 是否内嵌（inline）还是下载（attachment）
     * @param req 请求
     * @param resp 响应
     * @throws Exception 异常
     */
    public static void send(Path file, String contentType, String downloadName,
                            boolean inline, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        // 若响应已提交，直接返回避免再次写入
        if (resp.isCommitted()) return;

        long length = Files.size(file);
        String range = req.getHeader("Range");

        // 关键：接管响应，清除可能由别的组件设置的Writer/headers
        resp.reset();                       // <— 修复点1
        resp.setBufferSize(8192);
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setContentType(contentType != null ? contentType : "application/octet-stream");
        if (inline) {
            resp.setHeader("Content-Disposition", "inline; filename=\"" + encode(downloadName) + "\"");
        } else {
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + encode(downloadName) + "\"");
        }

        long start = 0, end = length - 1;
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.substring(6).split("-", 2);
            try {
                start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isBlank()) {
                    end = Long.parseLong(parts[1]);
                }
            } catch (NumberFormatException ignore) {
                // 非法range，按全量处理
                start = 0; end = length - 1;
            }
            if (end >= length) end = length - 1;
            if (start > end || start >= length) {
                resp.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                resp.setHeader("Content-Range", "bytes */" + length);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            resp.setContentLengthLong((end - start + 1));
        } else {
            resp.setContentLengthLong(length);
        }

        try (InputStream in = Files.newInputStream(file);
             OutputStream out = resp.getOutputStream()) {   // <— 只使用 OutputStream
            // 跳到起始位置并流式写出
            long skipped = 0;
            while (skipped < start) {
                long s = in.skip(start - skipped);
                if (s <= 0) break;
                skipped += s;
            }
            long toWrite = end - start + 1;
            byte[] buf = new byte[8192];
            int r;
            while (toWrite > 0 && (r = in.read(buf, 0, (int)Math.min(buf.length, toWrite))) != -1) {
                out.write(buf, 0, r);
                toWrite -= r;
            }
            out.flush();
        }
    }

    private static String encode(String s) {
        if (s == null) return "file";
        return s.replace("\"", "_");
    }
}
