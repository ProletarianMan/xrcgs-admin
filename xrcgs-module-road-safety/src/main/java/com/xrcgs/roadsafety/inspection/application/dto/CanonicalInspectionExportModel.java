package com.xrcgs.roadsafety.inspection.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Backend canonical model used for DB persistence and Excel export.
 */
@Data
@Builder
public class CanonicalInspectionExportModel {

    private LocalDate date;
    private String teamCode;
    private String unitName;
    private String weather;
    private String patrolTeam;
    private String patrolVehicle;
    private String location;
    private String inspectionContent;
    private String issuesFound;
    private String handlingSituationRaw;
    private HandlingCategoryGroup handlingGroup;
    private String handoverSummary;
    private String remark;
    private String exportFileName;
    private Boolean draft;
    @Builder.Default
    private List<PhotoItem> photos = new ArrayList<>();
    @Builder.Default
    private List<CanonicalDetail> details = new ArrayList<>();
    private JsonNode summaryPayload;

    @Data
    @Builder
    public static class CanonicalDetail {
        private String categoryCode;
        private String categoryName;
        private CanonicalDetailType type;
        private String summaryText;
        private JsonNode rawPayload;
        private Integer detailOrder;
    }

    public enum CanonicalDetailType {
        DAILY_PATROL,
        ROAD_DAMAGE,
        ACCIDENT,
        RESCUE,
        COMPENSATION,
        OVERSIZE,
        OVERLOAD,
        CONSTRUCTION,
        VIOLATION,
        OTHER
    }
}
