package com.xrcgs.roadsafety.inspection.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * Full frontend payload for submit-and-export.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionLogSubmitExportRequest {

    @NotBlank(message = "巡查日期不能为空")
    private String date;

    @NotBlank(message = "中队不能为空")
    private String team;

    @Size(max = 128, message = "单位名称长度不能超过128")
    private String unitName;

    @NotNull(message = "班次不能为空")
    private ShiftType shiftType;

    @Valid
    private ShiftSchedule shiftSchedule;

    @NotEmpty(message = "巡查路段不能为空")
    private List<@NotBlank String> routes;

    @NotBlank(message = "巡查车辆不能为空")
    private String vehicle;

    @NotBlank(message = "天气不能为空")
    private String weather;

    @Valid
    private Mileage mileage;

    @Valid
    @NotEmpty(message = "巡查明细不能为空")
    private List<@Valid InspectionDetail> details;

    @Valid
    @NotNull(message = "交接信息不能为空")
    private HandoverInfo handover;

    @Valid
    private List<Delivery> deliveries;

    private String remark;

    @Size(max = 128, message = "文件名长度不能超过128")
    private String fileName;

    private Boolean draft;

    public enum ShiftType {
        DAY,
        NIGHT,
        BOTH
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShiftSchedule {
        @Valid
        private ShiftScheduleItem day;
        @Valid
        private ShiftScheduleItem night;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShiftScheduleItem {
        private Boolean enabled;
        private List<String> timeRange;
        private Boolean endNextDay;
        private Boolean crossDay;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mileage {
        @Valid
        private MileageInfo day;
        @Valid
        private MileageInfo night;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MileageInfo {
        private String startStake;
        private String endStake;
        private Double totalKm;
        private String displayText;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InspectionDetail {
        private String id;
        private String category;
        @NotBlank(message = "明细类型不能为空")
        private String type;
        private String time;
        private String station;
        private String locationType;
        private String locationStake;
        private String location;
        private String accidentFacilityDamage;
        private String accidentHandleType;
        private String oversizeCompliance;
        private String constructionCompliance;
        private List<String> plateNos;
        private List<String> responsibles;
        private String description;
        private String result;
        private String summaryText;
        @Valid
        private List<PhotoPayload> photos;
        private String evacuateAt;
        private String constructionUnit;
        private String safetyMeasure;

        // Keep legacy fields for backward-compatible payloads.
        private String company;
        private String workContent;
        private String siteCondition;
        private Double amount;
        private String pricingBasis;
        private String plateNo;
        private String faultReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotoPayload {
        private String name;
        private String url;
        private String caption;
        private String uploadedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HandoverInfo {
        @NotEmpty(message = "巡查人员不能为空")
        private List<@NotBlank String> inspectors;
        private List<@NotBlank String> handoverFrom;
        private List<@NotBlank String> handoverTo;
        private String note;
        private String remark;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delivery {
        private String unit;
        private String number;
        private String date;
        private String remark;
    }
}

