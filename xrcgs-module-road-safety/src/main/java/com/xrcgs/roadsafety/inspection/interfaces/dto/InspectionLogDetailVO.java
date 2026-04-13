package com.xrcgs.roadsafety.inspection.interfaces.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.xrcgs.common.enums.ApprovalStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionLogDetailVO {

    private Long recordId;

    private LocalDate recordDate;

    private String squadCode;

    private ApprovalStatus approvalStatus;

    private LocalDateTime createdAt;

    private JsonNode replayPayload;
}
