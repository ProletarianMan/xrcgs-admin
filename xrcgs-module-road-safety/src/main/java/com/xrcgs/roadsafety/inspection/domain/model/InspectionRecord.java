package com.xrcgs.roadsafety.inspection.domain.model;

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
public class InspectionRecord {

    private Long id;

    /**
     * 巡查日期。
     */
    private LocalDate date;

    /**
     * 单位名称，用于表头展示。
     */
    private String unitName;

    /**
     * 天气情况。
     */
    private String weather;

    /**
     * 巡查人员或班组。
     */
    private String patrolTeam;

    /**
     * 巡查路线、里程与桩号。
     */
    private String location;

    /**
     * 巡查内容概述。
     */
    private String inspectionContent;

    /**
     * 巡查中发现的问题描述。
     */
    private String issuesFound;

    /**
     * 原始填写的处理情况文字。
     */
    private String handlingSituationRaw;

    /**
     * 结构化的处理情况分类。
     */
    @Builder.Default
    private HandlingCategoryGroup handlingDetails = new HandlingCategoryGroup();

    /**
     * 照片集合。
     */
    @Builder.Default
    private List<PhotoItem> photos = new ArrayList<>();

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String exportedBy;
    private LocalDateTime exportedAt;

    /**
     * 导出文件名，若为空则使用默认名称。
     */
    private String exportFileName;
}
