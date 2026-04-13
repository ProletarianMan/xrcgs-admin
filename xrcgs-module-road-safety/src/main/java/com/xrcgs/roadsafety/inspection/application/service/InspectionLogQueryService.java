package com.xrcgs.roadsafety.inspection.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionLogEchoView;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionLogEchoViewMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionRecordMapper;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogDetailVO;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogPageItemVO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InspectionLogQueryService {

    private final InspectionRecordMapper recordMapper;
    private final InspectionLogEchoViewMapper echoViewMapper;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<InspectionLogPageItemVO> page(LocalDate recordDate, String squadCode, long pageNo, long pageSize) {
        LambdaQueryWrapper<InspectionRecord> query = new LambdaQueryWrapper<InspectionRecord>()
                .select(
                        InspectionRecord::getId,
                        InspectionRecord::getDate,
                        InspectionRecord::getSquadCode,
                        InspectionRecord::getCreatedAt,
                        InspectionRecord::getApprovalStatus
                );

        if (recordDate != null) {
            query.eq(InspectionRecord::getDate, recordDate);
        }
        if (StringUtils.hasText(squadCode)) {
            query.eq(InspectionRecord::getSquadCode, squadCode.trim());
        }

        query.orderByDesc(InspectionRecord::getCreatedAt)
                .orderByDesc(InspectionRecord::getId);

        Page<InspectionRecord> entityPage = recordMapper.selectPage(Page.of(pageNo, pageSize), query);
        List<InspectionLogPageItemVO> records = entityPage.getRecords().stream()
                .map(record -> InspectionLogPageItemVO.builder()
                        .id(record.getId() == null ? null : record.getId().toString())
                        .recordDate(record.getDate())
                        .squadCode(record.getSquadCode())
                        .createdAt(record.getCreatedAt())
                        .approvalStatus(record.getApprovalStatus())
                        .build())
                .toList();

        Page<InspectionLogPageItemVO> result = Page.of(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Transactional(readOnly = true)
    public InspectionLogDetailVO detail(Long recordId) {
        InspectionLogEchoView detail = echoViewMapper.selectById(recordId);
        if (detail == null) {
            throw new IllegalArgumentException("inspection log not found: " + recordId);
        }
        return InspectionLogDetailVO.builder()
                .recordId(detail.getRecordId())
                .recordDate(detail.getRecordDate())
                .squadCode(detail.getSquadCode())
                .approvalStatus(detail.getApprovalStatus())
                .createdAt(detail.getCreatedAt())
                .replayPayload(parseReplayPayload(detail.getReplayPayloadJson(), recordId))
                .build();
    }

    private JsonNode parseReplayPayload(String payload, Long recordId) {
        if (!StringUtils.hasText(payload)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("inspection log payload parse failed: " + recordId, ex);
        }
    }
}
