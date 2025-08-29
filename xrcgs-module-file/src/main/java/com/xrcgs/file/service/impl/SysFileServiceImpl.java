package com.xrcgs.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xrcgs.file.config.FileProperties;
import com.xrcgs.file.enums.FileStatus;
import com.xrcgs.file.enums.FileType;
import com.xrcgs.file.mapper.SysFileMapper;
import com.xrcgs.file.model.entity.SysFile;
import com.xrcgs.file.model.vo.FileVO;
import com.xrcgs.file.service.SysFileService;
import com.xrcgs.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 文件操作
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private final FileStorage storage;
    private final FileProperties props;
    private final SysFileMapper mapper;

    /**
     * 文件上传
     * @param files 多文件本身
     * @param bizType 业务类型
     * @param userId 用户Id
     * @return 文件属性
     */
    @Transactional
    @Override
    public List<FileVO> upload(List<MultipartFile> files, String bizType, Long userId) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("没有上传文件");
        }
        if (props.getBizTypeWhitelist() != null && !props.getBizTypeWhitelist().isEmpty()) {
            if (!props.getBizTypeWhitelist().contains(bizType)) {
                throw new IllegalArgumentException("不被允许的业务类型: " + bizType);
            }
        }
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElse("-");
        List<FileVO> result = new ArrayList<>();
        for (MultipartFile f : files) {
            try {
                var saved = storage.save(f, bizType);
                SysFile ent = new SysFile();
                ent.setBizType(bizType);
                ent.setFileType(saved.getFileType().name());
                ent.setOriginalName(f.getOriginalFilename());
                ent.setExt(saved.getExt());
                ent.setMime(saved.getMime());
                ent.setSize(saved.getSize());
                ent.setSha256(saved.getSha256());
                ent.setStoragePath(saved.getStorageRelativePath());
                ent.setStatus(FileStatus.UPLOADED.name());
                ent.setCreatedAt(LocalDateTime.now());
                ent.setCreatedBy(userId);

                this.save(ent);

                String url = "/api/file/download/" + ent.getId();
                String pv = ent.getPreviewPath() != null ? "/api/file/preview/" + ent.getId() : null;
                result.add(FileVO.builder()
                        .id(ent.getId())
                        .url(url)
                        .previewUrl(pv)
                        .fileType(ent.getFileType())
                        .bizType(ent.getBizType())
                        .mime(ent.getMime())
                        .size(ent.getSize())
                        .originalName(ent.getOriginalName())
                        .status(ent.getStatus())
                        .build());

                log.info("上传成功 traceId={} id={} path={}", traceId, ent.getId(), ent.getStoragePath());
            } catch (Exception ex) {
                String msg = cut(ex.getMessage(), 2000);
                log.error("上传失败 traceId={} err={}", traceId, msg, ex);
                throw new RuntimeException("文件上传失败: " + msg);
            }
        }
        return result;
    }

    /**
     * 通过Id获取文件
     * @param id 文件ID
     * @return 文件本身
     */
    @Override
    public SysFile getOneById(Long id) {
        return this.getById(id);
    }

    /**
     * 分页查找
     * @param bizType 业务类型
     * @param fileType 文件类型
     * @param from 开始时间
     * @param to 结束时间
     * @param keyword 文件名称
     * @param page 当前页
     * @param size 一页多少条
     * @return 文件本身集合
     */
    @Override
    public Page<SysFile> pageQuery(String bizType, String fileType, LocalDateTime from, LocalDateTime to, String keyword, long page, long size) {
        LambdaQueryWrapper<SysFile> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(bizType)) qw.eq(SysFile::getBizType, bizType);
        if (StringUtils.hasText(fileType)) qw.eq(SysFile::getFileType, fileType);
        if (from != null) qw.ge(SysFile::getCreatedAt, from);
        if (to != null) qw.le(SysFile::getCreatedAt, to);
        if (StringUtils.hasText(keyword)) {
            qw.and(c -> c.like(SysFile::getOriginalName, keyword).or().like(SysFile::getSha256, keyword));
        }
        qw.ne(SysFile::getStatus, FileStatus.DELETED.name());
        qw.orderByDesc(SysFile::getCreatedAt);
        return this.page(Page.of(page, size), qw);
    }

    /**
     * 说明：
     *  1) select 里用聚合表达式（COUNT/SUM）与别名，不需要整段 SQL
     * 2) 条件、分组都用 Wrapper API
     * 3) 返回 listMaps：[{status: "UPLOADED", cnt: 12, totalSize: 12345}, ...]
     * @param bizType 业务类型
     * @return
     */
    @Override
    public List<Map<String, Object>> statsByBizType(String bizType) {
        // 说明：聚合列（COUNT/SUM）用字符串；条件与分组也直接用 QueryWrapper
        var qw = new QueryWrapper<SysFile>()
                .select("status", "COUNT(*) AS cnt", "COALESCE(SUM(size),0) AS totalSize")
                .eq("biz_type", bizType)
                .groupBy("status");
        return this.listMaps(qw);
    }

    /**
     * 文件删除
     * @param id 文件ID
     * @param hardDeletePhysical 是否物理删除
     * @return
     */
    @Override
    public boolean softDelete(Long id, boolean hardDeletePhysical) {
        SysFile sf = this.getById(id);
        if (sf == null) return true;
        sf.setStatus(FileStatus.DELETED.name());
        sf.setErrorMsg(null);
        boolean ok = this.updateById(sf);
        if (ok && hardDeletePhysical) {
            try {
                Path abs = storage.resolveStorageAbsolute(sf.getStoragePath());
                Files.deleteIfExists(abs);
                if (sf.getPreviewPath() != null) {
                    Path pv = storage.resolvePreviewAbsolute(sf.getPreviewPath());
                    Files.deleteIfExists(pv);
                }
            } catch (Exception e) {
                log.warn("文件硬删除失败 id={} err={}", id, cut(e.getMessage(), 512));
            }
        }
        return ok;
    }

    /**
     * 信息裁切
     * @param s  传入要裁切的字符串
     * @param max 从头开始保留几个字
     * @return 裁切后的字符串
     */
    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
