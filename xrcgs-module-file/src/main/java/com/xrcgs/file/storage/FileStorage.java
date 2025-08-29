package com.xrcgs.file.storage;

import com.xrcgs.file.enums.FileType;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;


/**
 * 上传接口，自定义调用
 */
public interface FileStorage {

    @Data @Builder
    class SaveResult {
        private String sha256;
        private String ext;
        private String mime;
        private long size;
        private String storageRelativePath; // e.g. images/2025/08/28/ab12cd....jpg
        private Path absolutePath;
        private FileType fileType;
    }

    // 保存
    SaveResult save(MultipartFile file, String bizType) throws IOException;

    // 解析存储绝对路径
    Path resolveStorageAbsolute(String storageRelativePath);

    // 解析预览绝对路径
    Path resolvePreviewAbsolute(String previewRelativePath);

    // 构建相对路径 文件名按业务类型重命名
    /** 目录：{type}/yyyy/MM/dd/；文件名：{sha256}_{bizType}.{ext} */
    String buildDatedRelativePath(FileType fileType, String sha256, String ext, String bizType);
}
