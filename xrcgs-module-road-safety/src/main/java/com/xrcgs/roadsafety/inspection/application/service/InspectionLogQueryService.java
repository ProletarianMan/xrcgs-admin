package com.xrcgs.roadsafety.inspection.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionRecordMapper;
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
                        .id(record.getId())
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
}
