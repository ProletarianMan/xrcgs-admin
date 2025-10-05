package com.xrcgs.roadsafety.inspection.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 巡查照片信息，包含文件路径以及对应说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoItem {

    /**
     * 图片所在的本地路径或网络下载后的缓存路径。
     */
    private String imagePath;

    /**
     * 对应的文字说明，写入在图片下方。
     */
    private String description;
}
