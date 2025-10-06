package com.xrcgs.roadsafety.inspection.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 巡查记录处理分类明细，对应 road_inspection_handling_detail 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("road_inspection_handling_detail")
public class InspectionHandlingDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("category_name")
    private String categoryName;

    @TableField("detail_text")
    private String detailText;

    @TableField("detail_order")
    private Integer detailOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
