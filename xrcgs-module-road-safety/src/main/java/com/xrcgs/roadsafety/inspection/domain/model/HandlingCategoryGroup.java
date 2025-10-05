package com.xrcgs.roadsafety.inspection.domain.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 巡查处理情况的分类结构，支持将原始文本内容拆分为不同的业务分类，方便后续格式化输出。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandlingCategoryGroup {

    @Builder.Default
    private List<String> roadDamage = new ArrayList<>();

    @Builder.Default
    private List<String> trafficAccidents = new ArrayList<>();

    @Builder.Default
    private List<String> roadRescue = new ArrayList<>();

    @Builder.Default
    private List<String> facilityCompensations = new ArrayList<>();

    @Builder.Default
    private List<String> largeVehicleChecks = new ArrayList<>();

    @Builder.Default
    private List<String> overloadVehicleHandling = new ArrayList<>();

    @Builder.Default
    private List<String> constructionChecks = new ArrayList<>();

    @Builder.Default
    private List<String> illegalInfringements = new ArrayList<>();

    @Builder.Default
    private List<String> otherMatters = new ArrayList<>();
}
