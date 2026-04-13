package com.xrcgs.roadsafety.inspection.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xrcgs.common.enums.ApprovalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Read model backed by view v_road_inspection_log_echo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("v_road_inspection_log_echo")
public class InspectionLogEchoView {

    @TableId("record_id")
    private Long recordId;

    @TableField("record_date")
    private LocalDate recordDate;

    @TableField("squad_code")
    private String squadCode;

    @TableField("approval_status")
    private ApprovalStatus approvalStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("replay_payload_json")
    private String replayPayloadJson;
}
