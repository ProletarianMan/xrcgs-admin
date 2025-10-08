package com.xrcgs.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.datascope.DataScopeUtil;
import com.xrcgs.iam.datascope.EffectiveDataScope;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.file.config.FileProperties;
import com.xrcgs.file.enums.FileStatus;
import com.xrcgs.file.enums.FileType;
import com.xrcgs.file.mapper.SysFileMapper;
import com.xrcgs.file.model.entity.SysFile;
import com.xrcgs.file.model.vo.FileVO;
import com.xrcgs.file.service.SysFileService;
import com.xrcgs.file.storage.FileStorage;
import com.xrcgs.infrastructure.audit.UserIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
    private final DataScopeManager dataScopeManager;
    private final UserIdProvider userIdProvider;
    private final SysUserMapper userMapper;

    /**
     * 文件上传
     * @param files 多文件本身
     * @param bizType 业务类型
     * @return 文件属性
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<FileVO> upload(List<MultipartFile> files, String bizType) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("没有上传文件");
        }

        // 校验业务类型
        if (props.getBizTypeWhitelist() != null && !props.getBizTypeWhitelist().isEmpty()) {
            if (!props.getBizTypeWhitelist().contains(bizType)) {
                throw new IllegalArgumentException("不被允许的业务类型: " + bizType);
            }
        }
        String traceId = Optional.ofNullable(MDC.get("traceId")).orElse("-");
        Long operatorId = userIdProvider.getCurrentUserId();
        if (operatorId == null) {
            throw new IllegalStateException("无法获取当前登录用户信息");
        }
        Long operatorDeptId = resolveDeptId(operatorId);
        List<FileVO> result = new ArrayList<>();
        for (MultipartFile f : files) {
            // 1) 先计算 sha256（不加载全量到内存）
            String sha256;
            try (InputStream in = f.getInputStream()) {
                sha256 = com.xrcgs.file.util.DigestUtils.sha256Hex(in);
            } catch (Exception e) {
                throw new RuntimeException("sha256 校验失败", e);
            }

            // 2) 查是否已有（未删除）的记录
            SysFile existed = findReusableBySha(sha256);

            if (existed != null) {
                // 2.1 已存在：不落盘，直接返回旧记录的属性
                result.add(toVO(existed, true));
                continue;
            }


            // 3) 不存在：正常保存 + 入库（仍需做 MIME/后缀白名单校验，LocalFileStorage.save 内部已有）
            FileStorage.SaveResult saved;
            try {
                saved = storage.save(f, bizType);
            } catch (java.nio.file.FileAlreadyExistsException faee) {
                // 文件名冲突时，优先尝试复用数据库中已有的记录；如果只是磁盘残留的旧文件，则清理后重试保存
                SysFile reuse = findReusableBySha(sha256);
                if (reuse != null) {
                    result.add(toVO(reuse, true));
                    continue;
                }
                Path conflicted = safeResolveConflictPath(faee);
                if (conflicted != null) {
                    try {
                        Files.deleteIfExists(conflicted);
                        saved = storage.save(f, bizType);
                    } catch (IOException ex2) {
                        throw new RuntimeException("文件入库失败: " + f.getOriginalFilename(), ex2);
                    }
                } else {
                    throw new RuntimeException("文件入库失败: " + f.getOriginalFilename(), faee);
                }
            } catch (IOException e) {
                throw new RuntimeException("文件入库失败: " + f.getOriginalFilename(), e);
            }

            // 组件文件实体
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
            ent.setDeptId(operatorDeptId);


            try {
                this.save(ent);
            } catch (DuplicateKeyException dke) {
                // 3.1 并发场景：别的请求刚好插入了同 sha256；此时回查并复用
                SysFile concurrent = findReusableBySha(sha256);

                if (concurrent != null) {
                    result.add(toVO(concurrent, true));
                    // 可选：删除刚刚写入的物理文件（因为没入库成功）；但一般 CREATE_NEW 打开，插不进去就没写到磁盘
                    continue;
                }
                // 抛出
                deletePhysicalFileQuietly(saved);
                throw dke;
            } catch (Exception e) {
                deletePhysicalFileQuietly(saved);
                throw (e instanceof RuntimeException re) ? re : new RuntimeException("文件入库失败: " + f.getOriginalFilename(), e);
            }

            result.add(toVO(ent, false));
            log.info("上传成功 traceId={} id={} path={}", traceId, ent.getId(), ent.getStoragePath());

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
        Long userId = userIdProvider.getCurrentUserId();
        EffectiveDataScope scope = dataScopeManager.getEffectiveDataScope(userId);
        DataScopeUtil.apply(qw, scope, userId, SysFile::getCreatedBy, SysFile::getDeptId);
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
        Long userId = userIdProvider.getCurrentUserId();
        EffectiveDataScope scope = dataScopeManager.getEffectiveDataScope(userId);
        DataScopeUtil.apply(qw, scope, userId, "created_by", "dept_id");
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


    /**
     * 实体类转传输类型
     * @param f
     * @param reused
     * @return
     */
    private FileVO toVO(SysFile f, boolean reused) {
        String url = "/api/file/download/" + f.getId();
        String pv = f.getPreviewPath() != null ? "/api/file/preview/" + f.getId() : null;

        return FileVO.builder()
                .id(f.getId())
                .url(url)
                .previewUrl(pv)
                .fileType(f.getFileType())
                .bizType(f.getBizType())
                .mime(f.getMime())
                .size(f.getSize())
                .originalName(f.getOriginalName())
                .status(f.getStatus())
                .reused(reused)
                .build();
    }

    private SysFile findReusableBySha(String sha256) {
        if (!StringUtils.hasText(sha256)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getSha256, sha256)
                .ne(SysFile::getStatus, FileStatus.DELETED.name())
                .last("limit 1"));
    }

    private Long resolveDeptId(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("未找到用户，无法确定部门，userId={}", userId);
                return null;
            }
            return user.getDeptId();
        } catch (Exception ex) {
            log.warn("查询用户部门失败 userId={} err={}", userId, cut(ex.getMessage(), 256));
            return null;
        }
    }

    private void deletePhysicalFileQuietly(FileStorage.SaveResult saved) {
        if (saved == null) {
            return;
        }
        Path path = saved.getAbsolutePath();
        if (path == null && StringUtils.hasText(saved.getStorageRelativePath())) {
            path = storage.resolveStorageAbsolute(saved.getStorageRelativePath());
        }
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ex) {
            log.warn("清理物理文件失败 path={} err={}", path, cut(ex.getMessage(), 256));
        }
    }

    private Path safeResolveConflictPath(java.nio.file.FileAlreadyExistsException ex) {
        if (ex == null) {
            return null;
        }
        String file = ex.getFile();
        if (!StringUtils.hasText(file)) {
            return null;
        }
        try {
            return Path.of(file);
        } catch (Exception ignore) {
            return null;
        }
    }
}
