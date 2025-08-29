package com.xrcgs.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xrcgs.file.model.entity.SysFile;
import com.xrcgs.file.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件接口
 */
public interface SysFileService extends IService<SysFile> {

    // 上传
    List<FileVO> upload(List<MultipartFile> files, String bizType, Long userId);

    // 通过Id获取文件
    SysFile getOneById(Long id);

    // 分页查询
    Page<SysFile> pageQuery(String bizType, String fileType, LocalDateTime from, LocalDateTime to, String keyword, long page, long size);

    // 通过文件业务类型查找状态
    List<Map<String,Object>> statsByBizType(String bizType);

    // 软删除
    boolean softDelete(Long id, boolean hardDeletePhysical);
}
