package com.xrcgs.roadsafety.inspection.application.service;

import com.xrcgs.roadsafety.inspection.config.InspectionLogProperties;
import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest.ContactDelivery;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest.DetailType;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest.HandoverInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest.InspectionDetail;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest.Mileage;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest.MileageInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 巡查日志应用服务：负责将前端提交的数据结构转换为领域实体，并驱动 Excel 导出流程。
 */
@Service
@RequiredArgsConstructor
public class InspectionLogApplicationService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    // 兼容前端传入的 /api/file/preview/{id} 或 /api/file/download/{id} 相对路径，提取其中的文件主键
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("/api/file/(?:preview|download)/(\\d+)");

    private final InspectionRecordExcelExporter excelExporter;
    private final InspectionLogProperties logProperties;

    /**
     * 根据巡查日志请求生成 Excel 文件。
     *
     * @param request 前端提交的巡查日志数据
     * @return 生成 Excel 文件的路径
     */
    public Path generateInspectionLog(InspectionLogSubmitRequest request) throws IOException {
        InspectionRecord record = buildInspectionRecord(request);
        Path storageDirectory = resolveStorageDirectory();
        Path exported = excelExporter.export(record, storageDirectory);
        // 保障导出文件存在且可读，否则抛出异常给调用方感知。
        if (!Files.exists(exported)) {
            throw new IOException("巡查日志导出失败，未生成文件");
        }
        return exported;
    }

    private Path resolveStorageDirectory() throws IOException {
        String configuredDirectory = Optional.ofNullable(logProperties.getInspectionLog())
                .map(String::trim)
                .orElse("");
        if (!StringUtils.hasText(configuredDirectory)) {
            throw new IllegalStateException("巡查日志存储目录未配置，请在 road-safety.inspection-log 中指定");
        }
        try {
            Path storagePath = Paths.get(configuredDirectory).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);
            return storagePath;
        } catch (InvalidPathException ex) {
            throw new IOException("巡查日志存储目录配置非法：" + configuredDirectory, ex);
        }
    }

    private InspectionRecord buildInspectionRecord(InspectionLogSubmitRequest request) {
        LocalDate inspectionDate = parseDate(request.getDate());
        String shiftText = translateShift(request.getShiftType());
        String routeText = joinWithDelimiter("、", request.getRoutes());
        String mileageText = buildMileageText(request.getMileage());
        String location = joinNonBlank("；", routeText, mileageText);
        List<InspectionDetail> detailList = Optional.ofNullable(request.getDetails()).orElseGet(Collections::emptyList);
        HandlingCategoryGroup categoryGroup = buildHandlingGroup(detailList);
        String issuesFound = buildIssuesFound(detailList);
        String handlingRaw = buildHandlingSituationRaw(detailList);
        String inspectionContent = buildInspectionContent(request, shiftText, routeText, mileageText, detailList.size());
        String remark = buildRemark(request);
        String handoverSummary = buildHandoverSummary(request.getHandover());
        List<PhotoItem> photos = buildPhotos(detailList);
        LocalDateTime now = LocalDateTime.now();
        String inspectorNames = joinWithDelimiter("、", Optional.ofNullable(request.getHandover())
                .map(HandoverInfo::getInspectors)
                .orElse(Collections.emptyList()));

        return InspectionRecord.builder()
                .date(inspectionDate)
                .unitName(StringUtils.trimWhitespace(request.getUnitName()))
                .weather(StringUtils.trimWhitespace(request.getWeather()))
                .patrolTeam(inspectorNames)
                .patrolVehicle(StringUtils.trimWhitespace(request.getVehicle()))
                .location(location)
                .inspectionContent(inspectionContent)
                .issuesFound(issuesFound)
                .handlingSituationRaw(handlingRaw)
                .handlingDetails(categoryGroup)
                .handoverSummary(handoverSummary)
                .photos(photos)
                .remark(remark)
                .createdBy(inspectorNames)
                .createdAt(now)
                .updatedAt(now)
                .exportedBy(inspectorNames)
                .exportedAt(now)
                .exportFileName(determineExportFileName(request, inspectionDate))
                .build();
    }

    private LocalDate parseDate(String dateText) {
        try {
            return LocalDate.parse(dateText, ISO_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("巡查日期格式错误，应为 YYYY-MM-DD", ex);
        }
    }

    private String translateShift(InspectionLogSubmitRequest.ShiftType shiftType) {
        return switch (shiftType) {
            case DAY -> "白班";
            case NIGHT -> "夜班";
            case BOTH -> "白班+夜班";
        };
    }

    private String buildMileageText(Mileage mileage) {
        if (mileage == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        if (mileage.getDay() != null) {
            String dayText = normalizeMileageDisplay(mileage.getDay());
            if (StringUtils.hasText(dayText)) {
                segments.add("白班：" + dayText);
            }
        }
        if (mileage.getNight() != null) {
            String nightText = normalizeMileageDisplay(mileage.getNight());
            if (StringUtils.hasText(nightText)) {
                segments.add("夜班：" + nightText);
            }
        }
        return segments.isEmpty() ? null : String.join("；", segments);
    }

    private String normalizeMileageDisplay(MileageInfo info) {
        if (info == null) {
            return "";
        }
        if (StringUtils.hasText(info.getDisplayText())) {
            return info.getDisplayText().trim();
        }
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(info.getStartStake()) && StringUtils.hasText(info.getEndStake())) {
            segments.add(info.getStartStake().trim() + "-" + info.getEndStake().trim());
        }
        if (info.getTotalKm() != null) {
            segments.add(String.format("总计%.3fKM", info.getTotalKm()));
        }
        return segments.isEmpty() ? "" : String.join("（", segments) + (segments.size() > 1 ? "）" : "");
    }

    private HandlingCategoryGroup buildHandlingGroup(List<InspectionDetail> details) {
        Map<DetailType, List<String>> bucket = new EnumMap<>(DetailType.class);
        for (DetailType type : DetailType.values()) {
            bucket.put(type, new ArrayList<>());
        }
        for (InspectionDetail detail : details) {
            if (detail == null || detail.getType() == null) {
                continue;
            }
            String summary = buildDetailSummary(detail);
            bucket.get(detail.getType()).add(summary);
        }
        return HandlingCategoryGroup.builder()
                .roadDamage(bucket.get(DetailType.ROAD_DAMAGE))
                .trafficAccidents(bucket.get(DetailType.ACCIDENT))
                .roadRescue(bucket.get(DetailType.RESCUE))
                .facilityCompensations(bucket.get(DetailType.COMPENSATION))
                .largeVehicleChecks(bucket.get(DetailType.OVERSIZE))
                .overloadVehicleHandling(bucket.get(DetailType.OVERLOAD))
                .constructionChecks(bucket.get(DetailType.CONSTRUCTION))
                .illegalInfringements(bucket.get(DetailType.VIOLATION))
                .otherMatters(bucket.get(DetailType.OTHER))
                .build();
    }

    private String buildIssuesFound(List<InspectionDetail> details) {
        List<String> lines = details.stream()
                .filter(Objects::nonNull)
                .map(this::formatIssueLine)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        return joinLines(lines);
    }

    private String formatIssueLine(InspectionDetail detail) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(detail.getTime())) {
            parts.add(detail.getTime().trim());
        }
        if (StringUtils.hasText(detail.getLocation())) {
            parts.add(detail.getLocation().trim());
        }
        if (StringUtils.hasText(detail.getDescription())) {
            parts.add(detail.getDescription().trim());
        }
        return parts.isEmpty() ? null : parts.stream().collect(Collectors.joining("：", "", ""));
    }

    private String buildHandlingSituationRaw(List<InspectionDetail> details) {
        List<String> lines = details.stream()
                .filter(Objects::nonNull)
                .map(detail -> {
                    if (!StringUtils.hasText(detail.getResult())) {
                        return null;
                    }
                    List<String> parts = new ArrayList<>();
                    if (StringUtils.hasText(detail.getTime())) {
                        parts.add(detail.getTime().trim());
                    }
                    if (StringUtils.hasText(detail.getLocation())) {
                        parts.add(detail.getLocation().trim());
                    }
                    parts.add("处理结果：" + detail.getResult().trim());
                    return String.join("，", parts);
                })
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        return joinLines(lines);
    }

    private String buildInspectionContent(InspectionLogSubmitRequest request, String shiftText,
                                          String routeText, String mileageText, int detailCount) {
        List<String> segments = new ArrayList<>();
        segments.add("班次：" + shiftText);
        if (StringUtils.hasText(routeText)) {
            segments.add("巡查路线：" + routeText);
        }
        if (StringUtils.hasText(mileageText)) {
            segments.add("巡查里程：" + mileageText);
        }
        segments.add("天气：" + defaultText(request.getWeather()));
        segments.add("巡查事件：" + detailCount + " 项");
        return String.join("；", segments);
    }

    private String buildRemark(InspectionLogSubmitRequest request) {
        List<String> remarkLines = new ArrayList<>();
        if (StringUtils.hasText(request.getRemark())) {
            remarkLines.add(request.getRemark().trim());
        }
        Optional.ofNullable(request.getHandover())
                .map(HandoverInfo::getRemark)
                .filter(StringUtils::hasText)
                .ifPresent(value -> remarkLines.add("交接备注：" + value.trim()));
        if (!CollectionUtils.isEmpty(request.getDeliveries())) {
            String deliveries = request.getDeliveries().stream()
                    .filter(Objects::nonNull)
                    .map(this::formatDelivery)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("；"));
            if (StringUtils.hasText(deliveries)) {
                remarkLines.add("送达联系单：" + deliveries);
            }
        }
        if (Boolean.TRUE.equals(request.getDraft())) {
            remarkLines.add("当前记录为草稿，尚未正式提交审批。");
        }
        return joinLines(remarkLines);
    }

    private String formatDelivery(ContactDelivery delivery) {
        List<String> parts = new ArrayList<>();
        if (delivery == null) {
            return null;
        }
        if (StringUtils.hasText(delivery.getUnit())) {
            parts.add(delivery.getUnit().trim());
        }
        if (StringUtils.hasText(delivery.getNumber())) {
            parts.add("编号：" + delivery.getNumber().trim());
        }
        if (StringUtils.hasText(delivery.getDate())) {
            parts.add("日期：" + delivery.getDate().trim());
        }
        if (StringUtils.hasText(delivery.getRemark())) {
            parts.add(delivery.getRemark().trim());
        }
        return parts.isEmpty() ? null : String.join("，", parts);
    }

    private String buildHandoverSummary(HandoverInfo handover) {
        if (handover == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(handover.getNote())) {
            parts.add(handover.getNote().trim());
        }
        if (!CollectionUtils.isEmpty(handover.getInspectors())) {
            parts.add("巡查人员：" + joinWithDelimiter("、", handover.getInspectors()));
        }
        if (!CollectionUtils.isEmpty(handover.getHandoverFrom())) {
            parts.add("交班人员：" + joinWithDelimiter("、", handover.getHandoverFrom()));
        }
        if (!CollectionUtils.isEmpty(handover.getHandoverTo())) {
            parts.add("接班人员：" + joinWithDelimiter("、", handover.getHandoverTo()));
        }
        if (StringUtils.hasText(handover.getRemark())) {
            parts.add("备注：" + handover.getRemark().trim());
        }
        return joinNonBlank("；", parts.toArray(new String[0]));
    }

    private List<PhotoItem> buildPhotos(List<InspectionDetail> details) {
        List<PhotoItem> photos = new ArrayList<>();
        AtomicInteger order = new AtomicInteger(1);
        for (InspectionDetail detail : details) {
            if (detail == null || CollectionUtils.isEmpty(detail.getPhotos())) {
                continue;
            }
            for (InspectionLogSubmitRequest.PhotoItem photoItem : detail.getPhotos()) {
                if (photoItem == null || !StringUtils.hasText(photoItem.getUrl())) {
                    continue;
                }
                String description = StringUtils.hasText(photoItem.getCaption())
                        ? photoItem.getCaption().trim()
                        : Optional.ofNullable(detail.getSummaryText())
                                .filter(StringUtils::hasText)
                                .map(String::trim)
                                .orElseGet(() -> defaultText(detail.getDescription()));
                // 自动识别相对路径中的文件 ID，供后续导出环节换取真实存储路径
                Long fileId = extractFileId(photoItem.getUrl());
                photos.add(PhotoItem.builder()
                        .fileId(fileId)
                        .imagePath(photoItem.getUrl().trim())
                        .description(description)
                        .sortOrder(order.getAndIncrement())
                        .build());
            }
        }
        return photos;
    }

    private Long extractFileId(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        Matcher matcher = FILE_ID_PATTERN.matcher(url.trim());
        if (matcher.find()) {
            try {
                return Long.valueOf(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String buildDetailSummary(InspectionDetail detail) {
        if (detail == null) {
            return "";
        }
        if (StringUtils.hasText(detail.getSummaryText())) {
            return detail.getSummaryText().trim();
        }
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(detail.getTime())) {
            segments.add(detail.getTime().trim());
        }
        if (StringUtils.hasText(detail.getLocation())) {
            segments.add(detail.getLocation().trim());
        }
        if (StringUtils.hasText(detail.getDescription())) {
            segments.add(detail.getDescription().trim());
        }
        if (StringUtils.hasText(detail.getResult())) {
            segments.add("处置：" + detail.getResult().trim());
        }
        if (StringUtils.hasText(detail.getCompany())) {
            segments.add("施工单位：" + detail.getCompany().trim());
        }
        if (StringUtils.hasText(detail.getWorkContent())) {
            segments.add("作业内容：" + detail.getWorkContent().trim());
        }
        if (StringUtils.hasText(detail.getSiteCondition())) {
            segments.add("现场情况：" + detail.getSiteCondition().trim());
        }
        if (StringUtils.hasText(detail.getSafetyMeasure())) {
            segments.add("安全措施：" + detail.getSafetyMeasure().trim());
        }
        if (detail.getAmount() != null) {
            segments.add("金额：" + detail.getAmount());
        }
        if (StringUtils.hasText(detail.getPricingBasis())) {
            segments.add("计价依据：" + detail.getPricingBasis().trim());
        }
        if (StringUtils.hasText(detail.getPlateNo())) {
            segments.add("车牌：" + detail.getPlateNo().trim());
        }
        if (StringUtils.hasText(detail.getFaultReason())) {
            segments.add("原因：" + detail.getFaultReason().trim());
        }
        if (StringUtils.hasText(detail.getEvacuateAt())) {
            segments.add("离场时间：" + detail.getEvacuateAt().trim());
        }
        return segments.isEmpty() ? defaultText(detail.getDescription()) : String.join("，", segments);
    }

    private String joinWithDelimiter(String delimiter, List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        String joined = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(delimiter));
        return StringUtils.hasText(joined) ? joined : null;
    }

    private String joinNonBlank(String delimiter, String... values) {
        String joined = Stream.of(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(delimiter));
        return StringUtils.hasText(joined) ? joined : null;
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "无";
    }

    private String joinLines(List<String> lines) {
        if (CollectionUtils.isEmpty(lines)) {
            return null;
        }
        return lines.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String determineExportFileName(InspectionLogSubmitRequest request, LocalDate date) {
        if (StringUtils.hasText(request.getFileName())) {
            return ensureExcelExtension(sanitizeFileName(request.getFileName().trim()));
        }
        String unitName = request.getUnitName();
        if (StringUtils.hasText(unitName)) {
            return ensureExcelExtension(sanitizeFileName(unitName.trim()) + "巡查日志");
        }
        String datePart = date != null ? date.format(DateTimeFormatter.BASIC_ISO_DATE) : "inspection";
        return "inspection_log_" + datePart + ".xlsx";
    }

    private String ensureExcelExtension(String fileName) {
        String normalized = fileName.toLowerCase();
        if (normalized.endsWith(".xlsx")) {
            return fileName;
        }
        if (normalized.endsWith(".xls")) {
            return fileName.substring(0, fileName.length() - 4) + ".xlsx";
        }
        return fileName + ".xlsx";
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
