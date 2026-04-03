package com.xrcgs.roadsafety.inspection.interfaces.dto;

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
public class InspectionLogPageItemVO {

    private Long id;

    private LocalDate recordDate;

    private String squadCode;

    private LocalDateTime createdAt;

    private ApprovalStatus approvalStatus;
}
