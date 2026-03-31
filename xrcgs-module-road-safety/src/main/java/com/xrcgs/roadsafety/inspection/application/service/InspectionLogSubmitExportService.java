package com.xrcgs.roadsafety.inspection.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.common.enums.ApprovalStatus;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel.CanonicalDetail;
import com.xrcgs.roadsafety.inspection.application.mapper.InspectionLogSubmitExportMapper;
import com.xrcgs.roadsafety.inspection.config.InspectionLogProperties;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionHandlingDetail;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionHandlingDetailMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionPhotoMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionRecordMapper;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Delivery;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Mileage;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.MileageInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InspectionLogSubmitExportService {

    private static final String DICT_UNIT_NAMES = "unitNames";
    private static final String DICT_WEATHERS = "weathers";
    private static final String DICT_OFFICIAL_VEHICLES = "officialVehicles";
    private static final String DICT_ALL_SITE = "allSite";
    private static final List<String> REQUIRED_EXPORT_DICT_TYPES = List.of(
            DICT_UNIT_NAMES,
            DICT_WEATHERS,
            DICT_OFFICIAL_VEHICLES,
            DICT_ALL_SITE
    );

    private final InspectionLogSubmitExportMapper mapper;
    private final InspectionRecordExcelExporter excelExporter;
    private final InspectionLogProperties logProperties;
    private final InspectionRecordMapper recordMapper;
    private final InspectionHandlingDetailMapper detailMapper;
    private final InspectionPhotoMapper photoMapper;
    private final ObjectMapper objectMapper;
    private final DictService dictService;
    private final AuthCacheService authCacheService;

    @Transactional(rollbackFor = Exception.class)
    public Path submitAndExport(InspectionLogSubmitExportRequest request, JsonNode rawPayload) throws IOException {
        CanonicalInspectionExportModel canonical = mapper.toCanonical(request, rawPayload);
        LocalDateTime now = LocalDateTime.now();
        InspectionRecord existingRecord = findLatestByDateAndSquad(canonical.getDate(), canonical.getTeamCode());
        InspectionRecord persistenceRecord = InspectionRecord.builder()
                .id(existingRecord == null ? null : existingRecord.getId())
                .date(canonical.getDate())
                .unitName(canonical.getUnitName())
                .weather(canonical.getWeather())
                .patrolTeam(canonical.getPatrolTeam())
                .patrolVehicle(canonical.getPatrolVehicle())
                .location(canonical.getLocation())
                .inspectionContent(canonical.getInspectionContent())
                .issuesFound(canonical.getIssuesFound())
                .handlingSituationRaw(canonical.getHandlingSituationRaw())
                .handlingDetails(canonical.getHandlingGroup())
                .photos(Optional.ofNullable(canonical.getPhotos()).orElse(Collections.emptyList()))
                .handoverSummary(canonical.getHandoverSummary())
                .remark(canonical.getRemark())
                .createdBy(resolveCreatedBy(existingRecord, canonical.getPatrolTeam()))
                .createdAt(resolveCreatedAt(existingRecord, now))
                .updatedAt(now)
                .exportedBy(canonical.getPatrolTeam())
                .exportedAt(now)
                .exportFileName(canonical.getExportFileName())
                .approvalStatus(Boolean.TRUE.equals(canonical.getDraft()) ? ApprovalStatus.UNSUBMITTED : ApprovalStatus.IN_PROGRESS)
                .squadCode(canonical.getTeamCode())
                .formPayloadJson(writeJson(rawPayload))
                .detailsPayloadJson(writeJson(rawPayload == null ? null : rawPayload.get("details")))
                .summaryPayloadJson(writeJson(canonical.getSummaryPayload()))
                .build();
        Map<String, Map<String, String>> dictLabelMappings = resolveDictLabelMappings();
        InspectionRecord exportRecord = buildExportRecord(canonical, persistenceRecord, request, dictLabelMappings);

        Path storageDirectory = resolveStorageDirectory();
        Path exported = excelExporter.export(exportRecord, storageDirectory);
        if (!Files.exists(exported)) {
            throw new IOException("检查导出文件未生成!");
        }

        if (existingRecord == null) {
            try {
                recordMapper.insert(persistenceRecord);
            } catch (DuplicateKeyException ex) {
                InspectionRecord latestRecord = findLatestByDateAndSquad(canonical.getDate(), canonical.getTeamCode());
                if (latestRecord == null || latestRecord.getId() == null) {
                    throw ex;
                }
                persistenceRecord.setId(latestRecord.getId());
                persistenceRecord.setCreatedBy(resolveCreatedBy(latestRecord, canonical.getPatrolTeam()));
                persistenceRecord.setCreatedAt(resolveCreatedAt(latestRecord, now));
                overwriteExistingRecord(persistenceRecord);
            }
        } else {
            overwriteExistingRecord(persistenceRecord);
        }
        saveDetailRows(persistenceRecord.getId(), canonical.getDetails(), now);
        savePhotoRows(persistenceRecord.getId(), canonical.getPhotos(), now);
        return exported;
    }

    private void overwriteExistingRecord(InspectionRecord persistenceRecord) {
        int updated = recordMapper.updateById(persistenceRecord);
        if (updated <= 0) {
            throw new IllegalStateException("inspection record overwrite failed: no row updated");
        }
        deleteDetailRows(persistenceRecord.getId());
        deletePhotoRows(persistenceRecord.getId());
    }

    private InspectionRecord findLatestByDateAndSquad(LocalDate date, String squadCode) {
        if (date == null || !StringUtils.hasText(squadCode)) {
            return null;
        }
        LambdaQueryWrapper<InspectionRecord> query = new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getDate, date)
                .eq(InspectionRecord::getSquadCode, squadCode.trim())
                .orderByDesc(InspectionRecord::getUpdatedAt)
                .orderByDesc(InspectionRecord::getId)
                .last("LIMIT 1");
        List<InspectionRecord> records = recordMapper.selectList(query);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private String resolveCreatedBy(InspectionRecord existingRecord, String fallbackCreatedBy) {
        if (existingRecord != null && StringUtils.hasText(existingRecord.getCreatedBy())) {
            return existingRecord.getCreatedBy();
        }
        return fallbackCreatedBy;
    }

    private LocalDateTime resolveCreatedAt(InspectionRecord existingRecord, LocalDateTime now) {
        if (existingRecord != null && existingRecord.getCreatedAt() != null) {
            return existingRecord.getCreatedAt();
        }
        return now;
    }

    private InspectionRecord buildExportRecord(CanonicalInspectionExportModel canonical,
                                               InspectionRecord persistenceRecord,
                                               InspectionLogSubmitExportRequest request,
                                               Map<String, Map<String, String>> dictLabelMappings) {
        return InspectionRecord.builder()
                .date(canonical.getDate())
                .unitName(resolveOptionalLabel(dictLabelMappings, DICT_UNIT_NAMES, request.getUnitName(), "单位"))
                .weather(resolveRequiredLabel(dictLabelMappings, DICT_WEATHERS, request.getWeather(), "天气"))
                .patrolTeam(canonical.getPatrolTeam())
                .patrolVehicle(resolveRequiredLabel(dictLabelMappings, DICT_OFFICIAL_VEHICLES, request.getVehicle(), "巡查车辆"))
                .location(canonical.getLocation())
                .routeDisplay(resolveRouteDisplay(dictLabelMappings, request.getRoutes()))
                .mileageDisplay(buildMileageDisplay(request.getMileage()))
                .handoverFromDisplay(buildPeopleDisplay(Optional.ofNullable(request.getHandover())
                        .map(InspectionLogSubmitExportRequest.HandoverInfo::getHandoverFrom)
                        .orElse(Collections.emptyList())))
                .handoverToDisplay(buildPeopleDisplay(Optional.ofNullable(request.getHandover())
                        .map(InspectionLogSubmitExportRequest.HandoverInfo::getHandoverTo)
                        .orElse(Collections.emptyList())))
                .deliveryContactDisplay(buildDeliveryContactDisplay(request.getDeliveries()))
                .inspectionContent(canonical.getInspectionContent())
                .issuesFound(canonical.getIssuesFound())
                .handlingSituationRaw(canonical.getHandlingSituationRaw())
                .handlingDetails(canonical.getHandlingGroup())
                .photos(Optional.ofNullable(canonical.getPhotos()).orElse(Collections.emptyList()))
                .handoverSummary(canonical.getHandoverSummary())
                .remark(canonical.getRemark())
                .createdBy(persistenceRecord.getCreatedBy())
                .createdAt(persistenceRecord.getCreatedAt())
                .updatedAt(persistenceRecord.getUpdatedAt())
                .exportedBy(persistenceRecord.getExportedBy())
                .exportedAt(persistenceRecord.getExportedAt())
                .exportFileName(canonical.getExportFileName())
                .build();
    }

    private Map<String, Map<String, String>> resolveDictLabelMappings() {
        Map<String, DictVO> dictByType = new LinkedHashMap<>();
        List<String> missingFromCache = new ArrayList<>();
        for (String typeCode : REQUIRED_EXPORT_DICT_TYPES) {
            DictVO cached = loadDictFromCache(typeCode);
            if (cached != null) {
                dictByType.put(typeCode, cached);
                continue;
            }
            missingFromCache.add(typeCode);
        }

        if (!missingFromCache.isEmpty()) {
            Map<String, DictVO> loaded = dictService.getByTypes(missingFromCache, null);
            for (String typeCode : missingFromCache) {
                DictVO dict = loaded == null ? null : loaded.get(typeCode);
                if (dict == null) {
                    throw new IllegalArgumentException("导出失败，缺少字典类型: " + typeCode);
                }
                dictByType.put(typeCode, dict);
            }
        }

        Map<String, Map<String, String>> labelMappings = new LinkedHashMap<>();
        for (String typeCode : REQUIRED_EXPORT_DICT_TYPES) {
            DictVO dict = dictByType.get(typeCode);
            if (dict == null || dict.getItems() == null || dict.getItems().isEmpty()) {
                throw new IllegalArgumentException("导出失败，字典为空: " + typeCode);
            }
            Map<String, String> mapping = new LinkedHashMap<>();
            for (DictVO.Item item : dict.getItems()) {
                if (item == null || !StringUtils.hasText(item.getValue())) {
                    continue;
                }
                String value = item.getValue().trim();
                String label = StringUtils.hasText(item.getLabel()) ? item.getLabel().trim() : value;
                mapping.put(value, label);
            }
            if (mapping.isEmpty()) {
                throw new IllegalArgumentException("导出失败，字典未配置有效项: " + typeCode);
            }
            labelMappings.put(typeCode, mapping);
        }
        return labelMappings;
    }

    private DictVO loadDictFromCache(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return null;
        }
        String cached = authCacheService.getCachedDict(typeCode);
        if (!StringUtils.hasText(cached)) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, DictVO.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String resolveRouteDisplay(Map<String, Map<String, String>> dictLabelMappings, List<String> routes) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        List<String> labels = new ArrayList<>();
        for (String routeCode : routes) {
            labels.add(resolveRequiredLabel(dictLabelMappings, DICT_ALL_SITE, routeCode, "巡查路段"));
        }
        return String.join("、", labels);
    }

    private String resolveRequiredLabel(Map<String, Map<String, String>> dictLabelMappings,
                                        String typeCode,
                                        String value,
                                        String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("导出失败，" + fieldName + "不能为空");
        }
        String normalized = value.trim();
        String label = Optional.ofNullable(dictLabelMappings.get(typeCode))
                .map(mapping -> mapping.get(normalized))
                .orElse(null);
        if (!StringUtils.hasText(label)) {
            throw new IllegalArgumentException("导出失败，" + fieldName + "字典值缺失: typeCode="
                    + typeCode + ", value=" + normalized);
        }
        return label.trim();
    }

    private String resolveOptionalLabel(Map<String, Map<String, String>> dictLabelMappings,
                                        String typeCode,
                                        String value,
                                        String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return resolveRequiredLabel(dictLabelMappings, typeCode, value, fieldName);
    }

    private String buildMileageDisplay(Mileage mileage) {
        if (mileage == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        double totalKm = 0d;
        if (hasMileageInput(mileage.getDay())) {
            MileageInfo day = mileage.getDay();
            segments.add("白班:" + formatMileageInfo(day, "白班"));
            totalKm += day.getTotalKm();
        }
        if (hasMileageInput(mileage.getNight())) {
            MileageInfo night = mileage.getNight();
            segments.add("夜班:" + formatMileageInfo(night, "夜班"));
            totalKm += night.getTotalKm();
        }
        if (segments.isEmpty()) {
            return null;
        }
        return String.join("；", segments)
                + "（总计" + String.format(Locale.ROOT, "%.3f", totalKm) + "KM）";
    }

    private String formatMileageInfo(MileageInfo info, String shiftName) {
        String startStake = trimToNull(info.getStartStake());
        String endStake = trimToNull(info.getEndStake());
        if (startStake == null || endStake == null) {
            throw new IllegalArgumentException("导出失败，" + shiftName + "巡查里程桩号不完整");
        }
        if (info.getTotalKm() == null) {
            throw new IllegalArgumentException("导出失败，" + shiftName + "巡查里程总公里缺失");
        }
        return startStake + "–" + endStake;
    }

    private boolean hasMileageInput(MileageInfo info) {
        if (info == null) {
            return false;
        }
        return StringUtils.hasText(info.getStartStake())
                || StringUtils.hasText(info.getEndStake())
                || info.getTotalKm() != null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildPeopleDisplay(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join("、", normalized);
    }

    private String buildDeliveryContactDisplay(List<Delivery> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            return null;
        }
        List<String> blocks = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            if (delivery == null) {
                continue;
            }
            String number = trimToNull(delivery.getNumber());
            String unit = trimToNull(delivery.getUnit());
            if (number == null && unit == null) {
                continue;
            }
            blocks.add("送达" + Optional.ofNullable(number).orElse("")
                    + "号《工作联系单》" + System.lineSeparator()
                    + "被送达单位：" + Optional.ofNullable(unit).orElse(""));
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return String.join(System.lineSeparator(), blocks);
    }

    private Path resolveStorageDirectory() throws IOException {
        String configuredDirectory = Optional.ofNullable(logProperties.getInspectionLog())
                .map(String::trim)
                .orElse("");
        if (!StringUtils.hasText(configuredDirectory)) {
            throw new IllegalStateException("road-safety.inspection-log is not configured");
        }
        try {
            Path storagePath = Paths.get(configuredDirectory).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);
            return storagePath;
        } catch (InvalidPathException ex) {
            throw new IOException("inspection storage path is invalid: " + configuredDirectory, ex);
        }
    }

    private void saveDetailRows(Long recordId, List<CanonicalDetail> details, LocalDateTime now) {
        if (recordId == null || details == null) {
            return;
        }
        for (CanonicalDetail detail : details) {
            InspectionHandlingDetail entity = InspectionHandlingDetail.builder()
                    .recordId(recordId)
                    .categoryCode(detail.getCategoryCode())
                    .categoryName(detail.getCategoryName())
                    .detailText(writeJson(detail.getRawPayload()))
                    .detailOrder(detail.getDetailOrder())
                    .createdAt(now)
                    .build();
            detailMapper.insert(entity);
        }
    }

    private void deleteDetailRows(Long recordId) {
        if (recordId == null) {
            return;
        }
        detailMapper.delete(Wrappers.lambdaQuery(InspectionHandlingDetail.class)
                .eq(InspectionHandlingDetail::getRecordId, recordId));
    }

    private void savePhotoRows(Long recordId, List<PhotoItem> photos, LocalDateTime now) {
        if (recordId == null || photos == null) {
            return;
        }
        for (PhotoItem photo : photos) {
            if (photo.getFileId() == null) {
                continue;
            }
            PhotoItem entity = PhotoItem.builder()
                    .recordId(recordId)
                    .fileId(photo.getFileId())
                    .description(photo.getDescription())
                    .sortOrder(photo.getSortOrder())
                    .createdAt(now)
                    .build();
            photoMapper.insert(entity);
        }
    }

    private void deletePhotoRows(Long recordId) {
        if (recordId == null) {
            return;
        }
        photoMapper.delete(Wrappers.lambdaQuery(PhotoItem.class)
                .eq(PhotoItem::getRecordId, recordId));
    }

    private String writeJson(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("json serialization failed", ex);
        }
    }
}
