// xrcgs-module-file/src/main/java/com/xrcgs/file/controller/FileController.java
package com.xrcgs.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.file.config.FileProperties;
import com.xrcgs.file.enums.FileStatus;
import com.xrcgs.file.model.dto.BatchDownloadRequest;
import com.xrcgs.file.model.entity.SysFile;
import com.xrcgs.file.model.vo.FileVO;
import com.xrcgs.file.service.FileConvertService;
import com.xrcgs.file.service.SysFileService;
import com.xrcgs.file.storage.FileStorage;
import com.xrcgs.file.web.RangeSender;
import com.xrcgs.syslog.annotation.OpLog; // 按你实际包名替换
import com.xrcgs.common.core.R;           // 按你实际包名替换
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件操作接口
 * 上传、转换、删除走鉴权（@PreAuthorize("isAuthenticated()")）；下载/预览默认匿名，可通过配置开关切换。
 * 预览优先返回 PDF（若已转换）；否则直接预览原文件（图片/视频/音频天然支持 Range）。
 *
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final SysFileService fileService;
    private final FileConvertService convertService;
    private final FileStorage storage;
    private final FileProperties props;

    // ============ 上传 ============
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @OpLog("文件上传")
    public R<List<FileVO>> upload(@RequestParam("files") List<MultipartFile> files,
                                  @RequestParam("bizType") @NotBlank String bizType) {
        List<FileVO> list = fileService.upload(files, bizType);
        return R.ok(list);
    }

    // ============ 找元数据 ============
    @GetMapping("/{id}")
    public R<SysFile> get(@PathVariable("id") Long id) {
        SysFile sf = fileService.getOneById(id);
        return sf == null ? R.fail("Not found") : R.ok(sf);
    }



    /**
     * 分页查询资料库
     * @param bizType 业务类型
     * @param fileType 文件类型
     * @param from 上传时间段1
     * @param to 上传时间段2
     * @param keyword 文件名称
     * @param page 当前页
     * @param size 每页条数
     * @return
     */
    @GetMapping("/page")
    public R<Page<SysFile>> page( @RequestParam(name = "bizType", required = false) String bizType,
                                  @RequestParam(name = "fileType", required = false) String fileType,
                                  @RequestParam(name = "from", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
                                  @RequestParam(name = "to", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
                                  @RequestParam(name = "keyword", required = false) String keyword,
                                  @RequestParam(name = "page", defaultValue = "1") long page,
                                  @RequestParam(name = "size", defaultValue = "10") long size) {
        return R.ok(fileService.pageQuery(bizType, fileType, from, to, keyword, page, size));
    }

    // ============ 下载 ============
    @GetMapping("/download/{id}")
    @OpLog("文件下载")
    public void download(@PathVariable("id") Long id, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (resp.isCommitted()) return; // 防重复写
        resp.setHeader("Cache-Control", "private, max-age=0, no-cache");
        resp.setHeader("Pragma", "no-cache");

        SysFile f = fileService.getById(id);
        if (f == null || FileStatus.DELETED.name().equals(f.getStatus())) {
            resp.setStatus(404); return;
        }
        Path abs = storage.resolveStorageAbsolute(f.getStoragePath());
        RangeSender.send(abs, f.getMime(), f.getOriginalName(), false, req, resp);
    }

    // ============ 预览（支持 Range） ============
//    @PreAuthorize("isAuthenticated()")   // 预览权限验证
    @GetMapping("/preview/{id}")
    @OpLog("文件预览")
    public void preview(@PathVariable("id") Long id, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (resp.isCommitted()) return;
        resp.setHeader("Cache-Control", "private, max-age=0, no-cache");
        resp.setHeader("Pragma", "no-cache");

        if (props.isPreviewAuthRequired()) {
            // 需要鉴权时，交由 Spring Security；可加 @PreAuthorize
            // 这里若需要鉴权，直接抛异常/return，用统一异常处理返回JSON，避免又去写二进制
            // throw new AccessDeniedException("preview requires auth");

        }
        SysFile f = fileService.getById(id);
        if (f == null || FileStatus.DELETED.name().equals(f.getStatus())) {
            resp.setStatus(404); return;
        }
        // 若有 previewPath（PDF 等），优先预览
        Path abs;
        String mime = f.getMime();
        if (StringUtils.hasText(f.getPreviewPath())) {
            abs = storage.resolvePreviewAbsolute(f.getPreviewPath());
            mime = "application/pdf";
        } else {
            abs = storage.resolveStorageAbsolute(f.getStoragePath());
        }
        RangeSender.send(abs, mime, f.getOriginalName(), true, req, resp);
    }

    // ============ 转 PDF ============
    @PostMapping("/convert/pdf")
    @PreAuthorize("isAuthenticated()")
    @OpLog("文件转PDF")
    public R<Boolean> convert(@RequestParam(name = "fileId") Long fileId) {
        boolean ok = convertService.convertToPdfAsync(fileId);
        return ok ? R.ok(true) : R.fail("Not convertible or not found");
    }

    // ============ 删除（软删；可选硬删） ============
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @OpLog("文件删除")
    public R<Boolean> delete(@PathVariable("id") Long id,
                             @RequestParam(name = "hard", defaultValue = "false") boolean hard) {
        return fileService.softDelete(id, hard) ? R.ok(true) : R.fail("Delete fail");
    }

    // ============ 统计 ============
    @GetMapping("/stats")
    public R<List<Map<String,Object>>> stats(@RequestParam String bizType) {
        return R.ok(fileService.statsByBizType(bizType));
    }

    /**
     * 按条件进行批量下载
     * @param req 批量下载条件
     * @param request 请求
     * @param response 响应
     * @throws Exception 异常
     */
    @PostMapping("/batch/download")
    @PreAuthorize("isAuthenticated()")
    @OpLog("文件批量下载")
    public void batchDownload(@RequestBody BatchDownloadRequest req,
                              HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 构建候选文件列表（不含 DELETED）
        var qw = new LambdaQueryWrapper<SysFile>()
                .ne(SysFile::getStatus, FileStatus.DELETED.name());

        // id下载
        if (req.getIds() != null && !req.getIds().isEmpty()) {
            qw.in(SysFile::getId, req.getIds());
        }
        // 业务类型
        if (StringUtils.hasText(req.getBizType())) {
            qw.eq(SysFile::getBizType, req.getBizType());
        }
        // 文件类型
        if (StringUtils.hasText(req.getFileType())) {
            qw.eq(SysFile::getFileType, req.getFileType());
        }
        // 查询日期起点
        if (req.getFrom() != null) {
            qw.ge(SysFile::getCreatedAt, req.getFrom());
        }
        // 查询日期重点
        if (req.getTo() != null) {
            qw.le(SysFile::getCreatedAt, req.getTo());
        }
        // 文件名称关键字
        if (StringUtils.hasText(req.getKeyword())) {
            qw.and(c -> c.like(SysFile::getOriginalName, req.getKeyword())
                    .or().like(SysFile::getSha256, req.getKeyword()));
        }
        // 升序排列
        qw.orderByAsc(SysFile::getFileType)
                .orderByAsc(SysFile::getId);

        List<SysFile> list = fileService.list(qw);

        // 安全限制（防止一次打包过多）
        final int MAX_FILES = req.getMaxSize();  // 可按需抽为配置
        if (list.size() > MAX_FILES) {
            list = list.subList(0, MAX_FILES);
        }

        // 响应头（ZIP）
        String base = org.springframework.util.StringUtils.hasText(req.getZipName())
                ? req.getZipName() : ("files-" + System.currentTimeMillis());
        String zipName = base.replaceAll("[\\\\/:*?\"<>|]", "_") + ".zip";

        if (response.isCommitted()) return;
        response.reset();
        response.setBufferSize(8192);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipName + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            byte[] buf = new byte[8192];
            for (var f : list) {
                // 选择“原文件”打包（不是预览 PDF）；如需 DOC 优先放 PDF，可自定策略
                Path path = storage.resolveStorageAbsolute(f.getStoragePath());  //原文件打包
                if (!Files.exists(path)) continue;

                // 重置文件名
                String safeName = sanitizeEntryName(f);
                ZipEntry entry = new ZipEntry(safeName);
                entry.setTime(System.currentTimeMillis());
                try {
                    zos.putNextEntry(entry);
                    try (java.io.InputStream in = java.nio.file.Files.newInputStream(path)) {
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            zos.write(buf, 0, n);
                        }
                    }
                } catch (Exception e) {
                    // 写不进去就跳过该条，避免整包失败
                } finally {
                    try { zos.closeEntry(); } catch (Exception ignore) {}
                }
            }
            zos.finish();
            zos.flush();
        }
    }

    /**
     * 重置文件名
     * @param f 文件本身
     * @return
     */
    private String sanitizeEntryName(SysFile f) {
        String typeDir = f.getFileType().toLowerCase(); // images/docs/videos/audios
        String baseName = (f.getOriginalName() == null ? f.getSha256() : f.getOriginalName());
        // 清理非法字符，避免路径穿越
        baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
        // 形如：docs/000123_原始名.ext
        return typeDir + "/" + String.format("%06d_", f.getId()) + baseName;
    }

}
