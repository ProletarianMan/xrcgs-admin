package com.xrcgs.file.storage;

import com.xrcgs.file.config.FileProperties;
import com.xrcgs.file.enums.FileType;
import com.xrcgs.file.util.DigestUtils;
import com.xrcgs.file.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 对文件上传接口做实现
 */
@Component
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private final FileProperties props;

    private static final DateTimeFormatter DF_YMD = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public SaveResult save(MultipartFile file, String bizType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("空文件");
        }
        if (file.getSize() > props.getMaxSize()) {
            throw new IOException("文件过大: " + DataSize.ofBytes(file.getSize()));
        }

        String originName = file.getOriginalFilename();
        String ext = MimeUtils.normalizeExt(originName);
        String sha256;
        String mime;

        // 同时计算 SHA-256 与 MIME
        try (InputStream in = file.getInputStream()) {
            sha256 = DigestUtils.sha256Hex(in);
        } catch (Exception e) {
            throw new IOException("SHA-256 计算失败", e);
        }
        try (InputStream in2 = file.getInputStream()) {
            mime = MimeUtils.detectMime(in2, originName);
        } catch (Exception e) {
            mime = "application/octet-stream";
        }

        if (!MimeUtils.allowed(ext, mime, props)) {
            throw new IOException("文件类型不允许: ext=" + ext + ", mime=" + mime);
        }

        FileType fileType = MimeUtils.classify(mime);
        String relative = buildDatedRelativePath(fileType, sha256, ext, bizType);

        Path absPath = resolveStorageAbsolute(relative);
        Files.createDirectories(absPath.getParent());

        try (InputStream in3 = file.getInputStream();
             OutputStream out = Files.newOutputStream(absPath, StandardOpenOption.CREATE_NEW)) {
            in3.transferTo(out);
        }

        return SaveResult.builder()
                .sha256(sha256)
                .ext(ext)
                .mime(mime)
                .size(file.getSize())
                .storageRelativePath(relative)
                .absolutePath(absPath)
                .fileType(fileType)
                .build();
    }

    /**
     * 源文件路径
     * @param storageRelativePath 源文件路径
     * @return
     */
    @Override
    public Path resolveStorageAbsolute(String storageRelativePath) {
        Path root = Paths.get(props.getStorageRoot()).toAbsolutePath().normalize();
        Path p = root.resolve(storageRelativePath).normalize();
        if (!p.startsWith(root)) {
            throw new SecurityException("检测到路径遍历");
        }
        return p;
    }

    /**
     * 解析预览路径
     * @param previewRelativePath 绝对预览地址
     * @return
     */
    @Override
    public Path resolvePreviewAbsolute(String previewRelativePath) {
        Path root = Paths.get(props.getPreviewRoot()).toAbsolutePath().normalize();
        Path p = root.resolve(previewRelativePath).normalize();
        if (!p.startsWith(root)) {
            throw new SecurityException("检测到路径遍历");
        }
        return p;
    }

    /**
     * 构建相对路径 文件名按业务类型重命名
     * @param fileType 文件类型
     * @param sha256 原名
     * @param ext 后缀
     * @param bizType 业务类型
     * @return 存储路径
     */
    @Override
    public String buildDatedRelativePath(FileType fileType, String sha256, String ext, String bizType) {
        String datePath = DF_YMD.format(LocalDate.now());
        String typeDir = switch (fileType) {
            case IMAGE -> "images";
            case DOC -> "docs";
            case VIDEO -> "videos";
            case AUDIO -> "audios";
        };
        String safeExt = (ext == null || ext.isBlank())
                ? FilenameUtils.getExtension("bin")
                : ext.toLowerCase();
        String safeBiz = sanitizeBizType(bizType);
        // 文件名：{sha256}_{BIZ}.{ext}
        String filename = sha256 + "_" + safeBiz + "." + safeExt;
        return typeDir + "/" + datePath + "/" + filename;
    }

    /** 仅允许 [A-Z0-9_-]，其余替换为 '-'，统一转大写，并限制长度（默认 32） */
    private String sanitizeBizType(String bizType) {
        String s = (bizType == null || bizType.isBlank()) ? "NA" : bizType.trim().toUpperCase();
        s = s.replaceAll("[^A-Z0-9_-]", "-");
        if (s.length() > 32) {
            s = s.substring(0, 32);
        }
        return s;
    }

}
