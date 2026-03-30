package com.xrcgs.roadsafety.inspection.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel.CanonicalDetail;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel.CanonicalDetailType;
import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.HandoverInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.InspectionDetail;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Mileage;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.MileageInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.PhotoPayload;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class InspectionLogSubmitExportMapper {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern FILE_ID_PATTERN = Pattern.compile("/api/file/(?:preview|download)/(\\d+)");

    public CanonicalInspectionExportModel toCanonical(InspectionLogSubmitExportRequest request, JsonNode rawPayload) {
        LocalDate date = parseDate(request.getDate());
        List<InspectionDetail> sourceDetails = Optional.ofNullable(request.getDetails()).orElseGet(Collections::emptyList);
        ArrayNode rawDetailNodes = extractRawDetailNodes(rawPayload);
        List<CanonicalDetail> canonicalDetails = new ArrayList<>();
        for (int i = 0; i < sourceDetails.size(); i++) {
            InspectionDetail detail = sourceDetails.get(i);
            JsonNode rawDetail = i < rawDetailNodes.size() ? rawDetailNodes.get(i) : JsonNodeFactory.instance.objectNode();
            canonicalDetails.add(CanonicalDetail.builder()
                    .categoryCode(resolveCategoryCode(detail))
                    .categoryName(resolveCategoryName(detail))
                    .type(mapDetailType(detail.getType()))
                    .summaryText(normalizeText(detail.getSummaryText()))
                    .rawPayload(rawDetail)
                    .detailOrder(i + 1)
                    .build());
        }

        HandlingCategoryGroup handlingGroup = buildHandlingGroup(canonicalDetails);
        ObjectNode summaryPayload = buildSummaryPayload(handlingGroup);
        String routeText = joinWithDelimiter("、", request.getRoutes());
        String mileageText = buildMileageText(request.getMileage());
        String location = joinNonBlank("；", routeText, mileageText);

        return CanonicalInspectionExportModel.builder()
                .date(date)
                .teamCode(normalizeText(request.getTeam()))
                .unitName(normalizeText(request.getUnitName()))
                .weather(normalizeText(request.getWeather()))
                .patrolTeam(joinWithDelimiter("、", Optional.ofNullable(request.getHandover())
                        .map(HandoverInfo::getInspectors).orElse(Collections.emptyList())))
                .patrolVehicle(normalizeText(request.getVehicle()))
                .location(location)
                .inspectionContent(buildInspectionContent(request, routeText, mileageText, sourceDetails.size()))
                .issuesFound(buildIssuesFound(sourceDetails))
                .handlingSituationRaw(buildHandlingSituationRaw(sourceDetails))
                .handlingGroup(handlingGroup)
                .handoverSummary(buildHandoverSummary(request.getHandover()))
                .remark(buildRemark(request))
                .exportFileName(determineExportFileName(request, date))
                .draft(request.getDraft())
                .photos(buildPhotos(sourceDetails))
                .details(canonicalDetails)
                .summaryPayload(summaryPayload)
                .build();
    }

    private LocalDate parseDate(String dateText) {
        try {
            return LocalDate.parse(dateText, ISO_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("date must be YYYY-MM-DD", ex);
        }
    }

    private ArrayNode extractRawDetailNodes(JsonNode rawPayload) {
        JsonNode detailsNode = rawPayload == null ? null : rawPayload.get("details");
        if (detailsNode == null || !detailsNode.isArray()) {
            return JsonNodeFactory.instance.arrayNode();
        }
        return (ArrayNode) detailsNode;
    }

    private String resolveCategoryCode(InspectionDetail detail) {
        if (detail != null && StringUtils.hasText(detail.getCategory())) {
            return detail.getCategory().trim();
        }
        return mapDetailType(detail == null ? null : detail.getType()).name();
    }

    private String resolveCategoryName(InspectionDetail detail) {
        CanonicalDetailType type = mapDetailType(detail == null ? null : detail.getType());
        return switch (type) {
            case ROAD_DAMAGE -> "ROAD_DAMAGE";
            case ACCIDENT, RESCUE -> "ACCIDENT_RESCUE";
            case COMPENSATION -> "COMPENSATION";
            case OVERSIZE, OVERLOAD -> "OVERSIZE_OVERLOAD";
            case CONSTRUCTION -> "CONSTRUCTION";
            case VIOLATION -> "VIOLATION";
            case OTHER -> "OTHER";
        };
    }

    private CanonicalDetailType mapDetailType(String type) {
        if (!StringUtils.hasText(type)) {
            return CanonicalDetailType.OTHER;
        }
        String normalized = type.trim().toUpperCase();
        return switch (normalized) {
            case "ROAD_DAMAGE" -> CanonicalDetailType.ROAD_DAMAGE;
            case "ACCIDENT" -> CanonicalDetailType.ACCIDENT;
            case "RESCUE" -> CanonicalDetailType.RESCUE;
            case "COMPENSATION" -> CanonicalDetailType.COMPENSATION;
            case "OVERSIZE", "OVERSIZE_CHECK" -> CanonicalDetailType.OVERSIZE;
            case "OVERLOAD", "OVERLIMIT_HANDLING" -> CanonicalDetailType.OVERLOAD;
            case "CONSTRUCTION" -> CanonicalDetailType.CONSTRUCTION;
            case "VIOLATION", "INFRINGEMENT" -> CanonicalDetailType.VIOLATION;
            default -> CanonicalDetailType.OTHER;
        };
    }

    private HandlingCategoryGroup buildHandlingGroup(List<CanonicalDetail> details) {
        Map<CanonicalDetailType, List<String>> bucket = new EnumMap<>(CanonicalDetailType.class);
        for (CanonicalDetailType type : CanonicalDetailType.values()) {
            bucket.put(type, new ArrayList<>());
        }
        for (CanonicalDetail detail : details) {
            if (detail == null || !StringUtils.hasText(detail.getSummaryText())) {
                continue;
            }
            bucket.get(detail.getType()).add(detail.getSummaryText().trim());
        }
        return HandlingCategoryGroup.builder()
                .roadDamage(bucket.get(CanonicalDetailType.ROAD_DAMAGE))
                .trafficAccidents(bucket.get(CanonicalDetailType.ACCIDENT))
                .roadRescue(bucket.get(CanonicalDetailType.RESCUE))
                .facilityCompensations(bucket.get(CanonicalDetailType.COMPENSATION))
                .largeVehicleChecks(bucket.get(CanonicalDetailType.OVERSIZE))
                .overloadVehicleHandling(bucket.get(CanonicalDetailType.OVERLOAD))
                .constructionChecks(bucket.get(CanonicalDetailType.CONSTRUCTION))
                .illegalInfringements(bucket.get(CanonicalDetailType.VIOLATION))
                .otherMatters(bucket.get(CanonicalDetailType.OTHER))
                .build();
    }

    private ObjectNode buildSummaryPayload(HandlingCategoryGroup group) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        HandlingCategoryGroup effective = Optional.ofNullable(group).orElseGet(HandlingCategoryGroup::new);
        node.putPOJO("roadDamage", effective.getRoadDamage());
        node.putPOJO("trafficAccidents", effective.getTrafficAccidents());
        node.putPOJO("roadRescue", effective.getRoadRescue());
        node.putPOJO("facilityCompensations", effective.getFacilityCompensations());
        node.putPOJO("largeVehicleChecks", effective.getLargeVehicleChecks());
        node.putPOJO("overloadVehicleHandling", effective.getOverloadVehicleHandling());
        node.putPOJO("constructionChecks", effective.getConstructionChecks());
        node.putPOJO("illegalInfringements", effective.getIllegalInfringements());
        node.putPOJO("otherMatters", effective.getOtherMatters());
        return node;
    }

    private String buildInspectionContent(InspectionLogSubmitExportRequest request, String routeText,
                                          String mileageText, int detailCount) {
        List<String> segments = new ArrayList<>();
        segments.add("shiftType:" + Optional.ofNullable(request.getShiftType()).map(Enum::name).orElse("BOTH"));
        if (StringUtils.hasText(routeText)) {
            segments.add("routes:" + routeText);
        }
        if (StringUtils.hasText(mileageText)) {
            segments.add("mileage:" + mileageText);
        }
        segments.add("weather:" + defaultText(request.getWeather()));
        segments.add("details:" + detailCount);
        return String.join(";", segments);
    }

    private String buildIssuesFound(List<InspectionDetail> details) {
        List<String> lines = details.stream()
                .map(detail -> joinNonBlank(" ", normalizeText(detail.getTime()),
                        firstNonBlank(detail.getLocation(), detail.getStation(), detail.getLocationStake()),
                        normalizeText(detail.getDescription())))
                .filter(StringUtils::hasText)
                .toList();
        return joinLines(lines);
    }

    private String buildHandlingSituationRaw(List<InspectionDetail> details) {
        List<String> lines = details.stream()
                .map(detail -> {
                    if (!StringUtils.hasText(detail.getResult())) {
                        return null;
                    }
                    return joinNonBlank(" ", normalizeText(detail.getTime()),
                            firstNonBlank(detail.getLocation(), detail.getStation(), detail.getLocationStake()),
                            "result:" + detail.getResult().trim());
                })
                .filter(StringUtils::hasText)
                .toList();
        return joinLines(lines);
    }

    private String buildRemark(InspectionLogSubmitExportRequest request) {
        return StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null;
    }

    private String buildHandoverSummary(HandoverInfo handover) {
        if (handover == null) {
            return null;
        }
        String note = normalizeText(handover.getNote());
        String remark = normalizeText(handover.getRemark());
        if (!StringUtils.hasText(note) && !StringUtils.hasText(remark)) {
            return null;
        }
        if (StringUtils.hasText(note) && StringUtils.hasText(remark)) {
            return note + System.lineSeparator() + "注：" + remark;
        }
        if (StringUtils.hasText(note)) {
            return note;
        }
        return "注：" + remark;
    }

    private List<PhotoItem> buildPhotos(List<InspectionDetail> details) {
        List<PhotoItem> photos = new ArrayList<>();
        AtomicInteger order = new AtomicInteger(1);
        for (InspectionDetail detail : details) {
            if (detail == null || CollectionUtils.isEmpty(detail.getPhotos())) {
                continue;
            }
            for (PhotoPayload photoPayload : detail.getPhotos()) {
                if (photoPayload == null || !StringUtils.hasText(photoPayload.getUrl())) {
                    continue;
                }
                String description = firstNonBlank(photoPayload.getCaption(), detail.getSummaryText(), detail.getDescription(), "photo");
                photos.add(PhotoItem.builder()
                        .fileId(extractFileId(photoPayload.getUrl()))
                        .imagePath(photoPayload.getUrl().trim())
                        .description(description)
                        .sortOrder(order.getAndIncrement())
                        .build());
            }
        }
        return photos;
    }

    private Long extractFileId(String url) {
        Matcher matcher = FILE_ID_PATTERN.matcher(url == null ? "" : url.trim());
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildMileageText(Mileage mileage) {
        if (mileage == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        if (mileage.getDay() != null) {
            String day = normalizeMileageDisplay(mileage.getDay());
            if (StringUtils.hasText(day)) {
                segments.add("day:" + day);
            }
        }
        if (mileage.getNight() != null) {
            String night = normalizeMileageDisplay(mileage.getNight());
            if (StringUtils.hasText(night)) {
                segments.add("night:" + night);
            }
        }
        return segments.isEmpty() ? null : String.join(";", segments);
    }

    private String normalizeMileageDisplay(MileageInfo info) {
        if (info == null) {
            return null;
        }
        if (StringUtils.hasText(info.getDisplayText())) {
            return info.getDisplayText().trim();
        }
        return joinNonBlank(" ",
                joinNonBlank("-", normalizeText(info.getStartStake()), normalizeText(info.getEndStake())),
                info.getTotalKm() == null ? null : String.format("total:%.3fKM", info.getTotalKm()));
    }

    private String determineExportFileName(InspectionLogSubmitExportRequest request, LocalDate date) {
        if (StringUtils.hasText(request.getFileName())) {
            return ensureExcelExtension(sanitizeFileName(request.getFileName().trim()));
        }
        if (StringUtils.hasText(request.getUnitName())) {
            return ensureExcelExtension(sanitizeFileName(request.getUnitName().trim()) + "inspection_log");
        }
        String datePart = date == null ? "inspection" : date.format(DateTimeFormatter.BASIC_ISO_DATE);
        return "inspection_log_" + datePart + ".xlsx";
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
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
        String joined = Arrays.stream(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(delimiter));
        return StringUtils.hasText(joined) ? joined : null;
    }

    private String joinLines(List<String> lines) {
        if (CollectionUtils.isEmpty(lines)) {
            return null;
        }
        String value = lines.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(System.lineSeparator()));
        return StringUtils.hasText(value) ? value : null;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "none";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
