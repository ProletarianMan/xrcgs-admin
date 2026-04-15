package com.xrcgs.roadsafety.inspection.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.file.model.vo.FileVO;
import com.xrcgs.file.service.SysFileService;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.HandoverInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.InspectionDetail;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Mileage;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.MileageInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.PhotoPayload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InspectionLogImportService {

    private static final Logger log = LoggerFactory.getLogger(InspectionLogImportService.class);
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.CHINA);

    private static final String DICT_UNIT_NAMES = "unitNames";
    private static final String DICT_WEATHERS = "weathers";
    private static final String DICT_OFFICIAL_VEHICLES = "officialVehicles";
    private static final String DICT_ALL_SITE = "allSite";
    private static final List<String> REQUIRED_IMPORT_DICT_TYPES = List.of(
            DICT_UNIT_NAMES,
            DICT_WEATHERS,
            DICT_OFFICIAL_VEHICLES,
            DICT_ALL_SITE
    );

    private static final Pattern DATE_CN_PATTERN = Pattern.compile("(\\d{4})\\s*\\u5E74\\s*(\\d{1,2})\\s*\\u6708\\s*(\\d{1,2})\\s*\\u65E5");
    private static final Pattern DATE_ISO_PATTERN = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern MILEAGE_SEGMENT_PATTERN = Pattern.compile(
            "(\\u767D\\u73ED|\\u591C\\u73ED)\\s*[:\\uFF1A]\\s*(.+?)\\s*[\\u2014\\-~\\u81F3]\\s*(.+?)(?:[\\uFF1B;\\uFF0C,\\(\\uFF08]|$)");
    private static final Pattern MILEAGE_TOTAL_PATTERN = Pattern.compile("\\u603B\\u8BA1\\s*([0-9]+(?:\\.[0-9]+)?)\\s*KM", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELIVERY_PATTERN = Pattern.compile(
            "\\u9001\\u8FBE\\s*(.*?)\\s*\\u53F7\\u300A\\u5DE5\\u4F5C\\u8054\\u7CFB\\u5355\\u300B\\s*\\R\\s*\\u88AB\\u9001\\u8FBE\\u5355\\u4F4D\\s*[:\\uFF1A]\\s*(.*?)(?=\\R\\s*\\u9001\\u8FBE|$)",
            Pattern.DOTALL);

    private static final int UNIT_ROW = 1;
    private static final int UNIT_COL = 0;
    private static final int DATE_ROW = 2;
    private static final int DATE_COL = 1;
    private static final int WEATHER_ROW = 2;
    private static final int WEATHER_COL = 3;
    private static final int PATROL_TEAM_ROW = 3;
    private static final int PATROL_TEAM_COL = 1;
    private static final int VEHICLE_ROW = 3;
    private static final int VEHICLE_COL = 3;
    private static final int MILEAGE_ROW = 4;
    private static final int MILEAGE_COL = 1;
    private static final int ROUTE_ROW = 5;
    private static final int ROUTE_COL = 1;
    private static final int HANDLING_ROW = 6;
    private static final int HANDLING_COL = 0;
    private static final int HANDOVER_ROW = 7;
    private static final int HANDOVER_COL = 0;
    private static final int HANDOVER_FROM_ROW = 8;
    private static final int HANDOVER_FROM_COL = 1;
    private static final int HANDOVER_TO_ROW = 8;
    private static final int HANDOVER_TO_COL = 3;
    private static final int DELIVERY_ROW = 10;
    private static final int DELIVERY_COL = 0;
    private static final int REMARK_ROW = 11;
    private static final int REMARK_COL = 0;

    private static final int PHOTO_TOP_DESC_ROW = 26;
    private static final int PHOTO_BOTTOM_DESC_ROW = 51;
    private static final int PHOTO_DESC_COL = 0;

    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile("^\\s*([\\u4E00\\u4E8C\\u4E09\\u56DB\\u4E94\\u516D\\u4E03])[\\u3001.]\\s*(.*)$");
    private static final Pattern LINE_INDEX_PATTERN = Pattern.compile("^\\s*\\d+[.\\u3001]\\s*");

    private static final String IMPORT_PHOTO_BIZ_TYPE = "road-safety-inspection";

    private final ObjectMapper objectMapper;
    private final DictService dictService;
    private final AuthCacheService authCacheService;
    private final SysFileService sysFileService;
    private final InspectionLogSubmitExportService submitExportService;

    public Long importAndSubmit(MultipartFile excelFile, String teamCode, Boolean draft) throws IOException {
        if (excelFile == null || excelFile.isEmpty()) {
            throw new IllegalArgumentException("excel file is required");
        }
        if (!StringUtils.hasText(teamCode)) {
            throw new IllegalArgumentException("team code is required");
        }
        ImportPayload payload = parseWorkbook(excelFile, teamCode.trim(), Boolean.TRUE.equals(draft));
        Path storedFile = submitExportService.storeUploadedOriginalFile(excelFile, payload.request().getFileName());
        log.info("inspection log original file stored, file={}", storedFile);

        try {
            Long recordId = submitExportService.submitWithoutExport(
                    payload.request(),
                    payload.rawPayload(),
                    storedFile.getFileName().toString());
            if (recordId == null) {
                throw new IllegalStateException("import succeeded but record id is null");
            }
            return recordId;
        } catch (Exception ex) {
            Files.deleteIfExists(storedFile);
            throw ex;
        }
    }

    private ImportPayload parseWorkbook(MultipartFile excelFile, String teamCode, boolean draft) throws IOException {
        Map<String, Map<String, String>> reverseDictMappings = resolveReverseDictMappings();
        try (InputStream inputStream = excelFile.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet infoSheet = workbook.getSheetAt(0);
            if (infoSheet == null) {
                throw new IllegalArgumentException("excel first sheet is missing");
            }

            String unitLabel = normalizeWithPrefix(readCellText(infoSheet, UNIT_ROW, UNIT_COL));
            String weatherLabel = readCellText(infoSheet, WEATHER_ROW, WEATHER_COL);
            String patrolTeamText = readCellText(infoSheet, PATROL_TEAM_ROW, PATROL_TEAM_COL);
            String vehicleLabel = readCellText(infoSheet, VEHICLE_ROW, VEHICLE_COL);
            String routeText = readCellText(infoSheet, ROUTE_ROW, ROUTE_COL);
            String mileageText = readCellText(infoSheet, MILEAGE_ROW, MILEAGE_COL);
            String handlingText = normalizeWithPrefix(readCellText(infoSheet, HANDLING_ROW, HANDLING_COL));
            String handoverSummary = normalizeWithPrefix(readCellText(infoSheet, HANDOVER_ROW, HANDOVER_COL));
            String remarkText = normalizeWithPrefix(readCellText(infoSheet, REMARK_ROW, REMARK_COL));
            String recordDateText = readCellText(infoSheet, DATE_ROW, DATE_COL);

            LocalDate recordDate = parseRecordDate(recordDateText);
            List<String> inspectors = splitPeople(patrolTeamText);
            List<String> handoverFrom = splitPeople(readCellText(infoSheet, HANDOVER_FROM_ROW, HANDOVER_FROM_COL));
            List<String> handoverTo = splitPeople(readCellText(infoSheet, HANDOVER_TO_ROW, HANDOVER_TO_COL));

            if (inspectors.isEmpty()) {
                inspectors = new ArrayList<>(handoverFrom);
            }
            if (inspectors.isEmpty()) {
                inspectors = new ArrayList<>(handoverTo);
            }
            if (inspectors.isEmpty()) {
                inspectors = List.of(teamCode);
            }

            String unitCode = resolveOptionalCode(reverseDictMappings.get(DICT_UNIT_NAMES), unitLabel);
            String weatherCode = resolveOptionalCode(reverseDictMappings.get(DICT_WEATHERS), weatherLabel);
            String vehicleCode = resolveOptionalCode(reverseDictMappings.get(DICT_OFFICIAL_VEHICLES), vehicleLabel);
            List<String> routeCodes = parseRouteCodes(routeText, reverseDictMappings.get(DICT_ALL_SITE));

            Mileage mileage = parseMileage(mileageText);
            InspectionLogSubmitExportRequest.ShiftType shiftType = resolveShiftType(mileage);

            List<InspectionDetail> details = parseHandlingDetails(handlingText);
            List<ParsedPhoto> parsedPhotos = extractPhotos(workbook);
            attachPhotosToDetails(details, parsedPhotos);
            ensureAtLeastOneDetail(details);

            HandoverInfo handoverInfo = new HandoverInfo();
            handoverInfo.setInspectors(inspectors);
            handoverInfo.setHandoverFrom(handoverFrom);
            handoverInfo.setHandoverTo(handoverTo);
            handoverInfo.setNote(handoverSummary);

            InspectionLogSubmitExportRequest request = new InspectionLogSubmitExportRequest();
            request.setDate(recordDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            request.setTeam(teamCode);
            request.setUnitName(unitCode);
            request.setShiftType(shiftType);
            request.setRoutes(routeCodes.isEmpty() ? null : routeCodes);
            request.setVehicle(vehicleCode);
            request.setWeather(weatherCode);
            request.setMileage(mileage);
            request.setDetails(details);
            request.setHandover(handoverInfo);
            request.setRemark(trimToNull(remarkText));
            request.setDraft(draft);
            request.setFileName(buildImportFileName(excelFile.getOriginalFilename()));
            request.setDeliveries(parseDeliveries(readCellText(infoSheet, DELIVERY_ROW, DELIVERY_COL)));

            JsonNode rawPayload = objectMapper.valueToTree(request);
            return new ImportPayload(request, rawPayload);
        }
    }

    private LocalDate parseRecordDate(String value) {
        String text = trimToNull(value);
        if (!StringUtils.hasText(text)) {
            return LocalDate.now();
        }
        Matcher cnMatcher = DATE_CN_PATTERN.matcher(text);
        if (cnMatcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(cnMatcher.group(1)),
                        Integer.parseInt(cnMatcher.group(2)),
                        Integer.parseInt(cnMatcher.group(3)));
            } catch (Exception ex) {
                log.warn("record date cn parse failed, value={}", text);
            }
        }
        Matcher isoMatcher = DATE_ISO_PATTERN.matcher(text);
        if (isoMatcher.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(isoMatcher.group(1)),
                        Integer.parseInt(isoMatcher.group(2)),
                        Integer.parseInt(isoMatcher.group(3)));
            } catch (Exception ex) {
                log.warn("record date iso parse failed, value={}", text);
            }
        }
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            log.warn("record date parse failed, fallback to today, value={}", text);
            return LocalDate.now();
        }
    }

    private List<String> parseRouteCodes(String routeText, Map<String, String> reverseMapping) {
        List<String> routeLabels = splitByRegex(routeText, "[\\u3001,\\uFF0C;\\uFF1B\\n\\r]+");
        if (routeLabels.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> routeCodes = new LinkedHashSet<>();
        for (String routeLabel : routeLabels) {
            String routeCode = resolveCode(reverseMapping, routeLabel);
            if (StringUtils.hasText(routeCode)) {
                routeCodes.add(routeCode);
            } else {
                log.warn("route mapping skipped, label={}", routeLabel);
            }
        }
        return new ArrayList<>(routeCodes);
    }

    private Mileage parseMileage(String mileageText) {
        String text = trimToNull(mileageText);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher segmentMatcher = MILEAGE_SEGMENT_PATTERN.matcher(text);
        MileageInfo day = null;
        MileageInfo night = null;
        int matchedCount = 0;
        while (segmentMatcher.find()) {
            String shift = trimToNull(segmentMatcher.group(1));
            MileageInfo info = new MileageInfo();
            info.setStartStake(trimToNull(segmentMatcher.group(2)));
            info.setEndStake(trimToNull(segmentMatcher.group(3)));
            if ("\u767D\u73ED".equals(shift)) {
                day = info;
            } else if ("\u591C\u73ED".equals(shift)) {
                night = info;
            }
            matchedCount++;
        }
        Matcher totalMatcher = MILEAGE_TOTAL_PATTERN.matcher(text);
        if (totalMatcher.find()) {
            Double totalKm = Double.valueOf(totalMatcher.group(1));
            if (day != null && day.getTotalKm() == null) {
                day.setTotalKm(totalKm);
            }
            if (night != null && night.getTotalKm() == null) {
                night.setTotalKm(totalKm);
            }
        }
        if (day == null && night == null && matchedCount == 0) {
            MileageInfo raw = new MileageInfo();
            raw.setDisplayText(text);
            Mileage mileage = new Mileage();
            mileage.setDay(raw);
            return mileage;
        }
        Mileage mileage = new Mileage();
        mileage.setDay(day);
        mileage.setNight(night);
        return mileage;
    }

    private InspectionLogSubmitExportRequest.ShiftType resolveShiftType(Mileage mileage) {
        boolean hasDay = mileage != null && mileage.getDay() != null;
        boolean hasNight = mileage != null && mileage.getNight() != null;
        if (hasDay && hasNight) {
            return InspectionLogSubmitExportRequest.ShiftType.BOTH;
        }
        if (hasDay) {
            return InspectionLogSubmitExportRequest.ShiftType.DAY;
        }
        if (hasNight) {
            return InspectionLogSubmitExportRequest.ShiftType.NIGHT;
        }
        return InspectionLogSubmitExportRequest.ShiftType.BOTH;
    }

    private List<InspectionDetail> parseHandlingDetails(String handlingText) {
        if (!StringUtils.hasText(handlingText)) {
            return new ArrayList<>();
        }
        String normalized = handlingText.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        String currentSection = null;
        List<InspectionDetail> details = new ArrayList<>();
        for (String line : lines) {
            String text = trimToNull(line);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            Matcher sectionMatcher = SECTION_HEADER_PATTERN.matcher(text);
            if (sectionMatcher.find()) {
                currentSection = sectionMatcher.group(1);
                String trailing = trimToNull(sectionMatcher.group(2));
                appendDetailFromLine(details, currentSection, extractSectionInlineContent(trailing));
                continue;
            }
            if (currentSection == null) {
                continue;
            }
            appendDetailFromLine(details, currentSection, text);
        }
        return details;
    }

    private String extractSectionInlineContent(String trailing) {
        String text = trimToNull(trailing);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        int colonIndex = Math.max(text.indexOf('\uFF1A'), text.indexOf(':'));
        if (colonIndex < 0 || colonIndex + 1 >= text.length()) {
            return null;
        }
        return trimToNull(text.substring(colonIndex + 1));
    }

    private void appendDetailFromLine(List<InspectionDetail> details, String section, String rawLine) {
        String line = trimToNull(rawLine);
        if (!StringUtils.hasText(line)) {
            return;
        }
        line = trimToNull(LINE_INDEX_PATTERN.matcher(line).replaceFirst(""));
        if (!StringUtils.hasText(line)) {
            return;
        }
        if ("\u65E0".equals(line) || "\u65E0\u3002".equals(line)) {
            return;
        }
        String type = mapSectionToDetailType(section, line);
        InspectionDetail detail = new InspectionDetail();
        detail.setType(type);
        detail.setCategory(type);
        detail.setSummaryText(line);
        detail.setDescription(line);
        details.add(detail);
    }

    private String mapSectionToDetailType(String section, String text) {
        if ("\u4E00".equals(section)) {
            return "ROAD_DAMAGE";
        }
        if ("\u4E8C".equals(section)) {
            if (text.contains("\u6551\u63F4") || text.contains("\u6E05\u969C")) {
                return "RESCUE";
            }
            return "ACCIDENT";
        }
        if ("\u4E09".equals(section)) {
            return "COMPENSATION";
        }
        if ("\u56DB".equals(section)) {
            if (text.contains("\u8D85\u9650") || text.contains("\u8D85\u8F7D")) {
                return "OVERLOAD";
            }
            return "OVERSIZE";
        }
        if ("\u4E94".equals(section)) {
            return "CONSTRUCTION";
        }
        if ("\u516D".equals(section)) {
            return "VIOLATION";
        }
        return "OTHER";
    }

    private List<ParsedPhoto> extractPhotos(Workbook workbook) {
        if (!(workbook instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook xssfWorkbook)) {
            return Collections.emptyList();
        }
        List<ParsedPhoto> photos = new ArrayList<>();
        for (int i = 1; i < xssfWorkbook.getNumberOfSheets(); i++) {
            XSSFSheet sheet = xssfWorkbook.getSheetAt(i);
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing == null || drawing.getShapes() == null || drawing.getShapes().isEmpty()) {
                continue;
            }
            List<XSSFPicture> pictures = drawing.getShapes().stream()
                    .filter(XSSFPicture.class::isInstance)
                    .map(XSSFPicture.class::cast)
                    .sorted(Comparator.comparingInt(this::resolvePictureOrder))
                    .toList();
            for (XSSFPicture picture : pictures) {
                PictureData pictureData = picture.getPictureData();
                if (pictureData == null || pictureData.getData() == null || pictureData.getData().length == 0) {
                    continue;
                }
                int descRow = resolveDescriptionRow(picture);
                String description = trimToNull(readCellText(sheet, descRow, PHOTO_DESC_COL));
                String extension = Optional.ofNullable(pictureData.suggestFileExtension())
                        .filter(StringUtils::hasText)
                        .map(String::toLowerCase)
                        .orElse("jpg");
                photos.add(new ParsedPhoto(pictureData.getData(), extension, description));
            }
        }
        return photos;
    }

    private int resolvePictureOrder(XSSFPicture picture) {
        XSSFClientAnchor anchor = picture.getPreferredSize();
        if (anchor == null) {
            return Integer.MAX_VALUE;
        }
        return anchor.getRow1() * 100 + anchor.getCol1();
    }

    private int resolveDescriptionRow(XSSFPicture picture) {
        XSSFClientAnchor anchor = picture.getPreferredSize();
        if (anchor == null) {
            return PHOTO_TOP_DESC_ROW;
        }
        return anchor.getRow1() >= 27 ? PHOTO_BOTTOM_DESC_ROW : PHOTO_TOP_DESC_ROW;
    }

    private void attachPhotosToDetails(List<InspectionDetail> details, List<ParsedPhoto> parsedPhotos) {
        if (parsedPhotos == null || parsedPhotos.isEmpty()) {
            return;
        }
        int index = 1;
        for (ParsedPhoto parsedPhoto : parsedPhotos) {
            PhotoPayload photoPayload;
            try {
                photoPayload = uploadPhoto(parsedPhoto, index++);
            } catch (Exception ex) {
                log.warn("photo parse/upload skipped during import", ex);
                continue;
            }
            PhotoBinding binding = resolvePhotoBinding(photoPayload.getCaption());
            InspectionDetail target = findOrCreateDetailByTypeAndOrdinal(
                    details,
                    binding.type(),
                    binding.ordinal(),
                    photoPayload.getCaption());
            if (target.getPhotos() == null) {
                target.setPhotos(new ArrayList<>());
            }
            target.getPhotos().add(photoPayload);
        }
    }

    private int locateTargetDetailIndex(List<InspectionDetail> details, String caption) {
        String normalizedCaption = normalizeCompareTextSafe(caption);
        if (!StringUtils.hasText(normalizedCaption) || details == null || details.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < details.size(); i++) {
            InspectionDetail detail = details.get(i);
            if (detail == null) {
                continue;
            }
            String summary = normalizeCompareTextSafe(detail.getSummaryText());
            String description = normalizeCompareTextSafe(detail.getDescription());
            if (normalizedCaption.equals(summary) || normalizedCaption.equals(description)) {
                return i;
            }
        }
        for (int i = 0; i < details.size(); i++) {
            InspectionDetail detail = details.get(i);
            if (detail == null) {
                continue;
            }
            String summary = normalizeCompareTextSafe(detail.getSummaryText());
            String description = normalizeCompareTextSafe(detail.getDescription());
            if ((StringUtils.hasText(summary) && (normalizedCaption.contains(summary) || summary.contains(normalizedCaption)))
                    || (StringUtils.hasText(description)
                    && (normalizedCaption.contains(description) || description.contains(normalizedCaption)))) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeCompareText(String text) {
        String normalized = trimToNull(text);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String withoutIndex = trimToNull(LINE_INDEX_PATTERN.matcher(normalized).replaceFirst(""));
        if (!StringUtils.hasText(withoutIndex)) {
            return null;
        }
        return withoutIndex
                .replaceAll("[\\s\\p{Punct}，。；：、“”‘’（）()【】《》\\-—~]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeCompareTextSafe(String text) {
        String normalized = trimToNull(text);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String withoutIndex = trimToNull(LINE_INDEX_PATTERN.matcher(normalized).replaceFirst(""));
        if (!StringUtils.hasText(withoutIndex)) {
            return null;
        }
        return withoutIndex
                .replaceAll("[\\s\\p{Punct}\\u3001\\u3002\\uFF0C\\uFF1B\\uFF1A\\u201C\\u201D\\u2018\\u2019\\uFF08\\uFF09\\u3010\\u3011\\u300A\\u300B\\-\\u2014~]+", "")
                .toLowerCase(Locale.ROOT);
    }

    private InspectionDetail findOrCreateDetailByType(List<InspectionDetail> details, String type, String caption) {
        for (InspectionDetail detail : details) {
            if (detail == null) {
                continue;
            }
            String currentType = trimToNull(detail.getType());
            if (StringUtils.hasText(currentType) && currentType.equalsIgnoreCase(type)) {
                return detail;
            }
        }
        InspectionDetail created = new InspectionDetail();
        created.setType(type);
        created.setCategory(type);
        String summary = extractPhotoSummary(caption);
        created.setSummaryText(StringUtils.hasText(summary) ? summary : "import photo");
        created.setDescription(StringUtils.hasText(summary) ? summary : "import photo");
        details.add(created);
        return created;
    }

    private String resolvePhotoTypeByCaption(String caption) {
        String text = trimToNull(caption);
        if (!StringUtils.hasText(text)) {
            return "OTHER";
        }
        int separatorIndex = Math.max(text.indexOf('\uFF1A'), text.indexOf(':'));
        String prefix = separatorIndex >= 0 ? text.substring(0, separatorIndex) : text;
        String normalized = trimToNull(LINE_INDEX_PATTERN.matcher(prefix).replaceFirst(""));
        if (!StringUtils.hasText(normalized)) {
            return "OTHER";
        }
        normalized = normalized.replaceAll("[\\s\\u3000]+", "");
        if (normalized.contains("\u9053\u8DEF\u75C5\u5BB3") || normalized.contains("\u635F\u574F")) {
            return "ROAD_DAMAGE";
        }
        if (normalized.contains("\u4EA4\u901A\u4E8B\u6545")) {
            return "ACCIDENT";
        }
        if (normalized.contains("\u6E05\u969C") || normalized.contains("\u6551\u63F4")) {
            return "RESCUE";
        }
        if (normalized.contains("\u8D54\u8865\u507F")) {
            return "COMPENSATION";
        }
        if (normalized.contains("\u8D85\u9650") || normalized.contains("\u8D85\u8F7D")) {
            return "OVERLOAD";
        }
        if (normalized.contains("\u5927\u4EF6")) {
            return "OVERSIZE";
        }
        if (normalized.contains("\u65BD\u5DE5")) {
            return "CONSTRUCTION";
        }
        if (normalized.contains("\u8FDD\u6CD5") || normalized.contains("\u4FB5\u6743")) {
            return "VIOLATION";
        }
        if (normalized.contains("\u5176\u4ED6")) {
            return "OTHER";
        }
        return "OTHER";
    }

    private InspectionDetail findOrCreateDetailByTypeAndOrdinal(List<InspectionDetail> details,
                                                                 String type,
                                                                 int ordinal,
                                                                 String caption) {
        int targetOrdinal = Math.max(1, ordinal);
        List<InspectionDetail> sameType = new ArrayList<>();
        for (InspectionDetail detail : details) {
            if (detail == null) {
                continue;
            }
            String currentType = trimToNull(detail.getType());
            if (StringUtils.hasText(currentType) && currentType.equalsIgnoreCase(type)) {
                sameType.add(detail);
            }
        }
        while (sameType.size() < targetOrdinal) {
            InspectionDetail created = new InspectionDetail();
            created.setType(type);
            created.setCategory(type);
            created.setSummaryText("无");
            created.setDescription("无");
            details.add(created);
            sameType.add(created);
        }
        InspectionDetail target = sameType.get(targetOrdinal - 1);
        String summary = extractPhotoSummary(caption);
        if (StringUtils.hasText(summary)) {
            if (!StringUtils.hasText(target.getSummaryText()) || "无".equals(trimToNull(target.getSummaryText()))) {
                target.setSummaryText(summary);
            }
            if (!StringUtils.hasText(target.getDescription()) || "无".equals(trimToNull(target.getDescription()))) {
                target.setDescription(summary);
            }
        }
        return target;
    }

    private PhotoBinding resolvePhotoBinding(String caption) {
        String text = trimToNull(caption);
        if (!StringUtils.hasText(text)) {
            return new PhotoBinding("OTHER", 1);
        }
        int separatorIndex = Math.max(text.indexOf('\uFF1A'), text.indexOf(':'));
        String prefix = separatorIndex >= 0 ? text.substring(0, separatorIndex) : text;
        String normalizedPrefix = trimToNull(LINE_INDEX_PATTERN.matcher(prefix).replaceFirst(""));
        if (!StringUtils.hasText(normalizedPrefix)) {
            return new PhotoBinding("OTHER", 1);
        }
        normalizedPrefix = normalizedPrefix.replaceAll("[\\s\\u3000]+", "");
        Matcher matcher = Pattern.compile("^(.*?)(\\d+)$").matcher(normalizedPrefix);
        String category = normalizedPrefix;
        int ordinal = 1;
        if (matcher.find()) {
            category = trimToNull(matcher.group(1));
            try {
                ordinal = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
                ordinal = 1;
            }
        }
        return new PhotoBinding(resolvePhotoTypeByCategory(category), Math.max(1, ordinal));
    }

    private String resolvePhotoTypeByCategory(String category) {
        String normalized = trimToNull(category);
        if (!StringUtils.hasText(normalized)) {
            return "OTHER";
        }
        normalized = normalized.replaceAll("[\\s\\u3000]+", "");
        if (normalized.contains("\u65E5\u5E38\u5DE1\u67E5")) {
            return "DAILY_PATROL";
        }
        if (normalized.contains("\u9053\u8DEF\u75C5\u5BB3") || normalized.contains("\u635F\u574F")) {
            return "ROAD_DAMAGE";
        }
        if (normalized.contains("\u4EA4\u901A\u4E8B\u6545")) {
            return "ACCIDENT";
        }
        if (normalized.contains("\u6E05\u969C") || normalized.contains("\u6551\u63F4")) {
            return "RESCUE";
        }
        if (normalized.contains("\u8D54\u8865\u507F")) {
            return "COMPENSATION";
        }
        if (normalized.contains("\u5927\u4EF6")) {
            return "OVERSIZE";
        }
        if (normalized.contains("\u8D85\u9650") || normalized.contains("\u8D85\u8F7D")) {
            return "OVERLOAD";
        }
        if (normalized.contains("\u65BD\u5DE5")) {
            return "CONSTRUCTION";
        }
        if (normalized.contains("\u8FDD\u6CD5") || normalized.contains("\u4FB5\u6743")) {
            return "VIOLATION";
        }
        if (normalized.contains("\u5176\u4ED6")) {
            return "OTHER";
        }
        return "OTHER";
    }

    private String extractPhotoSummary(String caption) {
        String text = trimToNull(caption);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        int separatorIndex = Math.max(text.indexOf('\uFF1A'), text.indexOf(':'));
        if (separatorIndex < 0 || separatorIndex + 1 >= text.length()) {
            return text;
        }
        return trimToNull(text.substring(separatorIndex + 1));
    }

    private PhotoPayload uploadPhoto(ParsedPhoto parsedPhoto, int index) {
        String extension = sanitizeExtension(parsedPhoto.extension());
        String fileName = "inspection-import-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + index + "." + extension;
        MultipartFile multipartFile = new ByteArrayMultipartFile(
                fileName,
                fileName,
                resolveMimeType(extension),
                parsedPhoto.bytes());
        List<FileVO> uploaded = sysFileService.upload(List.of(multipartFile), IMPORT_PHOTO_BIZ_TYPE);
        if (uploaded == null || uploaded.isEmpty() || uploaded.get(0).getId() == null) {
            throw new IllegalStateException("upload photo failed during import");
        }
        Long fileId = uploaded.get(0).getId();
        PhotoPayload payload = new PhotoPayload();
        payload.setName(fileName);
        payload.setUrl("/api/file/preview/" + fileId);
        payload.setCaption(parsedPhoto.description());
        return payload;
    }

    private void ensureAtLeastOneDetail(List<InspectionDetail> details) {
        if (details != null && !details.isEmpty()) {
            return;
        }
        InspectionDetail detail = new InspectionDetail();
        detail.setType("OTHER");
        detail.setCategory("OTHER");
        /*
        detail.setSummaryText("无");
        detail.setDescription("无");
        */
        detail.setSummaryText("\u65E0");
        detail.setDescription("\u65E0");
        details.add(detail);
    }

    private List<InspectionLogSubmitExportRequest.Delivery> parseDeliveries(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return Collections.emptyList();
        }
        String normalized = rawText.replace("\r\n", "\n").replace('\r', '\n');
        Matcher matcher = DELIVERY_PATTERN.matcher(normalized);
        List<InspectionLogSubmitExportRequest.Delivery> deliveries = new ArrayList<>();
        while (matcher.find()) {
            String number = trimToNull(matcher.group(1));
            String unit = trimToNull(matcher.group(2));
            if (!StringUtils.hasText(number) && !StringUtils.hasText(unit)) {
                continue;
            }
            InspectionLogSubmitExportRequest.Delivery delivery = new InspectionLogSubmitExportRequest.Delivery();
            delivery.setNumber(number);
            delivery.setUnit(unit);
            deliveries.add(delivery);
        }
        return deliveries;
    }

    private String resolveOptionalCode(Map<String, String> reverseMapping, String input) {
        String code = resolveCode(reverseMapping, input);
        if (!StringUtils.hasText(code) && StringUtils.hasText(input)) {
            log.warn("dictionary mapping skipped, input={}", input);
        }
        return code;
    }

    private String resolveCode(Map<String, String> reverseMapping, String input) {
        String normalized = trimToNull(input);
        if (!StringUtils.hasText(normalized) || reverseMapping == null || reverseMapping.isEmpty()) {
            return null;
        }
        String direct = reverseMapping.get(normalized);
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        return reverseMapping.get(normalized.replace(" ", ""));
    }

    private Map<String, Map<String, String>> resolveReverseDictMappings() {
        Map<String, DictVO> dictByType = new LinkedHashMap<>();
        List<String> missingFromCache = new ArrayList<>();
        for (String typeCode : REQUIRED_IMPORT_DICT_TYPES) {
            DictVO cached = loadDictFromCache(typeCode);
            if (cached != null) {
                dictByType.put(typeCode, cached);
                continue;
            }
            missingFromCache.add(typeCode);
        }
        if (!missingFromCache.isEmpty()) {
            Map<String, DictVO> loaded;
            try {
                loaded = dictService.getByTypes(missingFromCache, null);
            } catch (Exception ex) {
                log.warn("load dict for import failed, fallback to empty mapping", ex);
                loaded = Collections.emptyMap();
            }
            for (String typeCode : missingFromCache) {
                DictVO dict = loaded == null ? null : loaded.get(typeCode);
                if (dict == null) {
                    log.warn("dictionary missing for import, typeCode={}", typeCode);
                    continue;
                }
                dictByType.put(typeCode, dict);
            }
        }
        Map<String, Map<String, String>> reverse = new LinkedHashMap<>();
        for (String typeCode : REQUIRED_IMPORT_DICT_TYPES) {
            DictVO dict = dictByType.get(typeCode);
            if (dict == null || dict.getItems() == null || dict.getItems().isEmpty()) {
                reverse.put(typeCode, new LinkedHashMap<>());
                continue;
            }
            Map<String, String> mapping = new LinkedHashMap<>();
            for (DictVO.Item item : dict.getItems()) {
                if (item == null || !StringUtils.hasText(item.getValue())) {
                    continue;
                }
                String value = item.getValue().trim();
                String label = StringUtils.hasText(item.getLabel()) ? item.getLabel().trim() : value;
                mapping.put(value, value);
                mapping.put(label, value);
                mapping.put(label.replace(" ", ""), value);
            }
            reverse.put(typeCode, mapping);
        }
        return reverse;
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

    private String buildImportFileName(String originalFileName) {
        String fileName = trimToNull(originalFileName);
        if (!StringUtils.hasText(fileName)) {
            return "inspection_import_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String readCellText(Sheet sheet, int rowIndex, int columnIndex) {
        if (sheet == null) {
            return null;
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String text = DATA_FORMATTER.formatCellValue(cell);
        return trimToNull(text);
    }

    private String normalizeWithPrefix(String value) {
        String text = trimToNull(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        int lineBreakIndex = text.indexOf('\n');
        if (lineBreakIndex >= 0 && lineBreakIndex + 1 < text.length()) {
            text = text.substring(lineBreakIndex + 1);
        } else {
            int colonIndex = Math.max(text.indexOf('\uFF1A'), text.indexOf(':'));
            if (colonIndex >= 0 && colonIndex + 1 < text.length()) {
                text = text.substring(colonIndex + 1);
            }
        }
        return trimToNull(text);
    }

    private List<String> splitPeople(String value) {
        return splitByRegex(value, "[\\u3001,\\uFF0C;\\uFF1B\\s]+");
    }

    private List<String> splitByRegex(String value, String regex) {
        String text = trimToNull(value);
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        String[] parts = text.split(regex);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String normalized = trimToNull(part);
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String sanitizeExtension(String ext) {
        String normalized = trimToNull(ext);
        if (!StringUtils.hasText(normalized)) {
            return "jpg";
        }
        return normalized.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String resolveMimeType(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) {
            throw new UnsupportedOperationException("transferTo(File) is not supported");
        }

        @Override
        public void transferTo(Path dest) throws IOException {
            Files.write(dest, content);
        }
    }

    private record PhotoBinding(String type, int ordinal) {
    }

    private record ParsedPhoto(byte[] bytes, String extension, String description) {
    }

    public record ImportPayload(InspectionLogSubmitExportRequest request, JsonNode rawPayload) {
    }
}
