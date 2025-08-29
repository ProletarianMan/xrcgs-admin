package com.xrcgs.file.service.impl;

import com.xrcgs.file.enums.FileStatus;
import com.xrcgs.file.enums.FileType;
import com.xrcgs.file.model.entity.SysFile;
import com.xrcgs.file.service.FileConvertService;
import com.xrcgs.file.service.SysFileService;
import com.xrcgs.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.DocumentConverter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * 文件类型转换
 * UPLOADED → CONVERTING → READY/FAIL。失败自动重试 3 次。
 * 并发与超时：由 OfficeManager 的 taskTimeout 及线程池控制
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileConvertServiceImpl implements FileConvertService {

    private final SysFileService fileService;
    private final DocumentConverter documentConverter; // 来自 infrastructure 配置
    private final FileStorage storage;
    private final TaskExecutor convertExecutor; // 见 infra 配置中定义的 Bean（线程池）

    /**
     * 异步转换为PDF文件
     * 仅对 fileType=DOC 文档类尝试转换；其余类型跳过
     * @param fileId 文件ID
     * @return 是否成功
     */
    @Override
    public boolean convertToPdfAsync(Long fileId) {
        SysFile f = fileService.getById(fileId);
        if (f == null) return false;
        if (!FileType.DOC.name().equals(f.getFileType())) {
            // 仅对文档类处理；其他类型跳过（可扩展）
            return false;
        }
        // 已在转换/转换完成则跳过
        if (FileStatus.CONVERTING.name().equals(f.getStatus()) ||
                FileStatus.READY.equals(FileStatus.valueOf(f.getStatus()))) {
            return true;
        }

        f.setStatus(FileStatus.CONVERTING.name());
        f.setErrorMsg(null);
        fileService.updateById(f);

        convertExecutor.execute(() -> doConvertWithRetry(f.getId(), 3));
        return true;
    }


    /**
     * 尝试转换（并）重试
     * @param id 文件ID
     * @param maxRetry 最大重试次数
     */
    private void doConvertWithRetry(Long id, int maxRetry) {
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElse("-");
        int attempt = 0;
        while (attempt < maxRetry) {
            attempt++;
            try {
                SysFile f = fileService.getById(id);
                if (f == null) return;

                Path in = storage.resolveStorageAbsolute(f.getStoragePath());
                // preview 相对路径与存储相同子目录+PDF 扩展，但注意 preview 根不同
                String relPdf = f.getStoragePath()
                        .replaceFirst("^[^/]+/", "") // 去掉一级目录（images/docs/...）之后保留 yyyy/...
                        .replaceAll("\\.[^.]+$", ".pdf");
                String typeDir = "docs"; // 预览输出统一放 preview/docs/yyyy/..（便于静态服务）
                String previewRel = typeDir + "/" + relPdf;
                Path out = storage.resolvePreviewAbsolute(previewRel);
                Files.createDirectories(out.getParent());

                documentConverter.convert(in.toFile()).to(out.toFile()).execute();

                f.setPreviewPath(previewRel);
                f.setStatus(FileStatus.READY.name());
                f.setErrorMsg(null);
                fileService.updateById(f);
                log.info("转换成功 traceId={} id={} out={}", traceId, id, previewRel);
                return;
            } catch (Exception ex) {
                log.warn("转换尝试 {} failed id={} err={}", attempt, id, cut(ex.getMessage(), 1000));
                if (attempt >= maxRetry) {
                    SysFile f2 = fileService.getById(id);
                    if (f2 != null) {
                        f2.setStatus(FileStatus.FAIL.name());
                        f2.setErrorMsg(cut(ex.getMessage(), 1000));
                        fileService.updateById(f2);
                    }
                }
            }
        }
    }

    /**
     * 字符串切割
     * @param s 传入字符串
     * @param max 切割到第几个字
     * @return
     */
    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
