package com.xrcgs.roadsafety.inspection.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xrcgs.common.enums.ApprovalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 路产安全巡查记录的核心实体，包含基础信息、处理情况以及照片等要素。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("road_inspection_record")
public class InspectionRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 巡查日期。
     */
    @TableField("record_date")
    private LocalDate date;

    /**
     * 单位名称，用于表头展示。
     */
    @TableField("unit_name")
    private String unitName;

    /**
     * 天气情况。
     */
    private String weather;

    /**
     * 巡查人员或班组。
     */
    @TableField("patrol_team")
    private String patrolTeam;

    /**
     * 巡查车辆。
     */
    @TableField("patrol_vehicle")
    private String patrolVehicle;

    /**
     * 巡查路线、里程与桩号。
     */
    private String location;

    /**
     * 巡查内容概述。
     */
    @TableField("inspection_content")
    private String inspectionContent;

    /**
     * 巡查中发现的问题描述。
     */
    @TableField("issues_found")
    private String issuesFound;

    /**
     * 原始填写的处理情况文字。
     */
    @TableField("handling_situation_raw")
    private String handlingSituationRaw;

    /**
     * 结构化的处理情况分类。
     */
    @Builder.Default
    @TableField(exist = false)
    private HandlingCategoryGroup handlingDetails = new HandlingCategoryGroup();

    /**
     * 照片集合。
     */
    @Builder.Default
    @TableField(exist = false)
    private List<PhotoItem> photos = new ArrayList<>();

    /**
     * 巡查车辆、装备、案件等交接情况。
     */
    @TableField("handover_summary")
    private String handoverSummary;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 巡查日志的审批状态。
     */
    @Builder.Default
    @TableField("approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.UNSUBMITTED;

    @TableField("squad_code")
    private String squadCode;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("exported_by")
    private String exportedBy;

    @TableField("exported_at")
    private LocalDateTime exportedAt;

    /**
     * 导出文件名，若为空则使用默认名称。
     */
    @TableField("export_file_name")
    private String exportFileName;
}
