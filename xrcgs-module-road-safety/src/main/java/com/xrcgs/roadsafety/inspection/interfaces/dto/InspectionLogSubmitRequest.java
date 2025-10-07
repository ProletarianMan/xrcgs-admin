package com.xrcgs.roadsafety.inspection.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 巡查日志提交入参，请求结构贴合前端模型，便于后续扩展。
 */
@Data
public class InspectionLogSubmitRequest {

    /**
     * 巡查日期（格式：YYYY-MM-DD）。
     */
    @NotBlank(message = "巡查日期不能为空")
    private String date;

    /**
     * 班次类型：DAY=白班，NIGHT=夜班，BOTH=两班都有。
     */
    @NotNull(message = "班次类型不能为空")
    private ShiftType shiftType;

    /**
     * 巡查路段ID数组（例如：["R001","R002"]）。
     */
    @NotEmpty(message = "巡查路段不能为空")
    private List<@NotBlank String> routes;

    /**
     * 巡查车辆编号（或车牌号）。
     */
    @NotBlank(message = "巡查车辆不能为空")
    private String vehicle;

    /**
     * 天气描述（如：晴 / 阴 / 雨 / 雪 / 风沙 / 其他）。
     */
    @NotBlank(message = "天气情况不能为空")
    private String weather;

    /**
     * 里程信息，区分白班与夜班。
     */
    @Valid
    private Mileage mileage;

    /**
     * 备注信息（可选）。
     */
    private String remark;

    /**
     * 巡查、处理情况记录明细。
     */
    @NotEmpty(message = "巡查明细不能为空")
    private List<@Valid InspectionDetail> details;

    /**
     * 人员与交接信息。
     */
    @Valid
    @NotNull(message = "交接信息不能为空")
    private HandoverInfo handover;

    /**
     * 送达联系单信息（可选，多条）。
     */
    @Valid
    private List<ContactDelivery> deliveries;

    /**
     * 客户端保存草稿标志（true=仅保存草稿，false=正式提交）。
     */
    private Boolean draft;

    /**
     * 导出文件名称（可选，后缀自动补齐为 .xlsx）。
     */
    @Size(max = 128, message = "导出文件名长度不能超过128个字符")
    private String fileName;

    /**
     * 单位名称，用于表头展示。
     */
    @Size(max = 128, message = "单位名称长度不能超过128个字符")
    private String unitName;

    /**
     * 班次类型枚举。
     */
    public enum ShiftType {
        /** 白班。 */
        DAY,
        /** 夜班。 */
        NIGHT,
        /** 白班+夜班。 */
        BOTH
    }

    /**
     * 里程信息对象。
     */
    @Data
    public static class Mileage {

        /**
         * 白班里程（可为空）。
         */
        @Valid
        private MileageInfo day;

        /**
         * 夜班里程（可为空）。
         */
        @Valid
        private MileageInfo night;
    }

    /**
     * 单段里程信息。
     */
    @Data
    public static class MileageInfo {

        /** 起点桩号（格式示例："K0+000"）。 */
        @NotBlank(message = "里程起点不能为空")
        private String startStake;

        /** 终点桩号（格式示例："K45+000"）。 */
        @NotBlank(message = "里程终点不能为空")
        private String endStake;

        /** 总里程（单位：千米，保留3位小数）。 */
        @NotNull(message = "总里程不能为空")
        private Double totalKm;

        /** 展示用规范文本（例如："K0+000–K45+000（总计45.000KM）"）。 */
        @NotBlank(message = "里程展示文本不能为空")
        private String displayText;
    }

    /**
     * 巡查/处理情况条目。
     */
    @Data
    public static class InspectionDetail {

        /** 类型（ROAD_DAMAGE、ACCIDENT、RESCUE、COMPENSATION、OVERSIZE、OVERLOAD、CONSTRUCTION、VIOLATION、OTHER）。 */
        @NotNull(message = "明细类型不能为空")
        private DetailType type;

        /** 发生时间（格式：HH:mm）。 */
        private String time;

        /** 位置（如："上行K45+600"）。 */
        private String location;

        /** 现象或事件描述。 */
        @NotBlank(message = "事件描述不能为空")
        private String description;

        /** 处理结果或采取措施（文本，可为空）。 */
        private String result;

        /** 施工单位名称（仅施工类使用）。 */
        private String company;

        /** 作业内容（施工类使用）。 */
        private String workContent;

        /** 现场情况（施工类使用）。 */
        private String siteCondition;

        /** 安全措施（施工类使用）。 */
        private String safetyMeasure;

        /** 金额或赔偿测算（仅赔补偿类使用）。 */
        private Double amount;

        /** 计价依据或标准说明（赔补偿类使用）。 */
        private String pricingBasis;

        /** 车牌号（用于事故、清障、大件、超限）。 */
        private String plateNo;

        /** 故障原因或事故原因（用于清障救援）。 */
        private String faultReason;

        /** 拖离/驶离时间（用于事故、救援）。 */
        private String evacuateAt;

        /** 照片列表（照片与描述一一对应）。 */
        @Valid
        private List<PhotoItem> photos;

        /** 系统自动生成的汇总句子（用户可修改）。 */
        private String summaryText;
    }

    /**
     * 巡查照片信息。
     */
    @Data
    public static class PhotoItem {

        /** 照片访问URL或Base64数据。 */
        @NotBlank(message = "照片URL不能为空")
        private String url;

        /** 照片描述。 */
        private String caption;

        /** 上传时间（可选）。 */
        private String uploadedAt;
    }

    /**
     * 人员与交接信息。
     */
    @Data
    public static class HandoverInfo {

        /** 巡查人员ID数组（或姓名数组）。 */
        @NotEmpty(message = "巡查人员不能为空")
        private List<@NotBlank String> inspectors;

        /** 交班人ID数组。 */
        private List<@NotBlank String> handoverFrom;

        /** 接班人ID数组。 */
        private List<@NotBlank String> handoverTo;

        /** 车辆、案件等交接情况说明。 */
        @NotBlank(message = "交接说明不能为空")
        private String note;

        /** 其他备注（选填）。 */
        private String remark;
    }

    /**
     * 送达联系单信息。
     */
    @Data
    public static class ContactDelivery {

        /** 送达单位名称。 */
        @NotBlank(message = "送达单位不能为空")
        private String unit;

        /** 联系单编号。 */
        @NotBlank(message = "联系单编号不能为空")
        private String number;

        /** 送达日期（格式：YYYY-MM-DD）。 */
        @NotBlank(message = "送达日期不能为空")
        private String date;

        /** 备注说明。 */
        private String remark;
    }

    /**
     * 明细类型枚举，与前端约定的类型保持一致。
     */
    public enum DetailType {
        /** 道路病害或损坏情况。 */
        ROAD_DAMAGE,
        /** 交通事故。 */
        ACCIDENT,
        /** 清障救援。 */
        RESCUE,
        /** 设施赔补偿情况。 */
        COMPENSATION,
        /** 大件检查。 */
        OVERSIZE,
        /** 超限处理。 */
        OVERLOAD,
        /** 涉路施工检查。 */
        CONSTRUCTION,
        /** 违法侵权事件。 */
        VIOLATION,
        /** 其他情况。 */
        OTHER
    }
}
