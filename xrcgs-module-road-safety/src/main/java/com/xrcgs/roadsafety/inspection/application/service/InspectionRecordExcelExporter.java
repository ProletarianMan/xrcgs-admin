package com.xrcgs.roadsafety.inspection.application.service;

import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于巡查记录模板的 Excel 导出能力，负责将结构化巡查数据写入模板并生成正式文档。
 */
@Service
public class InspectionRecordExcelExporter {

    private static final Logger log = LoggerFactory.getLogger(InspectionRecordExcelExporter.class);

    private static final String TEMPLATE_CLASSPATH = "excel/inspectionRecord.xlsx";
    private static final String DEFAULT_EXPORT_NAME = "inspection_record_output.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
    private static final int PHOTOS_PER_PAGE = 2;
    private static final int THIRD_PAGE_START_ROW = 65;
    private static final int THIRD_PAGE_END_ROW = 114;
    private static final int ROWS_PER_ADDITIONAL_PAGE = THIRD_PAGE_END_ROW - THIRD_PAGE_START_ROW + 1;

    private static final List<PhotoSlotTemplate> BASE_PHOTO_SLOTS = List.of(
            new PhotoSlotTemplate(1, 14, 4, 38, 39, 39, 2),
            new PhotoSlotTemplate(1, 40, 4, 63, 64, 64, 2),
            new PhotoSlotTemplate(1, 66, 4, 87, 88, 90, 2),
            new PhotoSlotTemplate(1, 91, 4, 111, 112, 114, 2)
    );

    private static final List<PhotoSlotTemplate> ADDITIONAL_PAGE_SLOTS = List.of(
            new PhotoSlotTemplate(1, 66, 4, 87, 88, 90, 2),
            new PhotoSlotTemplate(1, 91, 4, 111, 112, 114, 2)
    );

    // 注入模板资源，便于在不同运行环境或测试中覆盖默认模板。
    private final Resource templateResource;

    public InspectionRecordExcelExporter() {
        this(new ClassPathResource(TEMPLATE_CLASSPATH));
    }

    InspectionRecordExcelExporter(Resource templateResource) {
        this.templateResource = templateResource;
    }

    /**
     * 使用系统临时目录作为导出位置。
     */
    public Path export(InspectionRecord record) throws IOException {
        return export(record, Paths.get(System.getProperty("java.io.tmpdir")));
    }

    /**
     * 按照模板将巡查记录导出为 Excel 文件。
     *
     * @param record         巡查记录数据
     * @param outputDirectory 导出目录
     * @return 生成文件的路径
     */
    public Path export(InspectionRecord record, Path outputDirectory) throws IOException {
        Objects.requireNonNull(record, "inspection record must not be null");
        if (outputDirectory == null) {
            outputDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
        }
        Files.createDirectories(outputDirectory);

        try (InputStream templateStream = openTemplate();
             Workbook workbook = WorkbookFactory.create(templateStream)) {
            XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);

            // 逐段将巡查信息写入到模板对应区域，保持原有样式与格式。
            fillHeaderInfo(sheet, record);
            fillHandlingSection(sheet, record);
            fillRemarks(sheet, record);
            writePhotos(workbook, sheet, Optional.ofNullable(record.getPhotos()).orElse(Collections.emptyList()));

            Path exportPath = outputDirectory.resolve(determineFileName(record));
            try (OutputStream outputStream = Files.newOutputStream(exportPath)) {
                workbook.write(outputStream);
            }
            return exportPath;
        }
    }

    private InputStream openTemplate() throws IOException {
        if (!templateResource.exists()) {
            throw new IOException("巡查记录模板不存在: " + templateResource.getDescription());
        }
        return templateResource.getInputStream();
    }

    private void fillHeaderInfo(Sheet sheet, InspectionRecord record) {
        setCellToRightOfLabel(sheet, "巡查时间", formatDate(record.getDate()));
        setCellToRightOfLabel(sheet, "天气情况", defaultText(record.getWeather()));
        setCellToRightOfLabel(sheet, "巡查人员", defaultText(record.getPatrolTeam()));
        // 模板中存在“巡查车辆”字段，当前未提供专门字段，保持空白即可。
        setCellToRightOfLabel(sheet, "巡查里程", defaultText(record.getLocation()));
        setCellToRightOfLabel(sheet, "巡查路段", defaultText(record.getLocation()));
    }

    private void fillHandlingSection(Sheet sheet, InspectionRecord record) {
        // 构建巡查概述+处理情况的正文，按模板顺序组织段落。
        StringBuilder builder = new StringBuilder();
        builder.append("巡查内容：").append(defaultText(record.getInspectionContent())).append(System.lineSeparator());
        builder.append("发现的问题：").append(defaultText(record.getIssuesFound())).append(System.lineSeparator());
        builder.append("处理情况（原始记录）：").append(defaultText(record.getHandlingSituationRaw())).append(System.lineSeparator());
        builder.append("处理情况（分类汇总）：").append(System.lineSeparator());
        builder.append(buildHandlingDetails(record.getHandlingDetails()));
        setCellToRightOfLabel(sheet, "巡查、处理情况", builder.toString().trim());
    }

    private void fillRemarks(Sheet sheet, InspectionRecord record) {
        String remark = buildRemark(record);
        if (StringUtils.hasText(remark)) {
            setCellToRightOfLabel(sheet, "备注", remark);
        }
    }

    private String buildRemark(InspectionRecord record) {
        List<String> lines = new ArrayList<>();
        if (StringUtils.hasText(record.getCreatedBy()) || record.getCreatedAt() != null) {
            lines.add("创建：" + buildNameWithTime(record.getCreatedBy(), record.getCreatedAt()));
        }
        if (record.getUpdatedAt() != null) {
            lines.add("最后更新时间：" + DATE_TIME_FORMATTER.format(record.getUpdatedAt()));
        }
        if (StringUtils.hasText(record.getExportedBy()) || record.getExportedAt() != null) {
            lines.add("导出：" + buildNameWithTime(record.getExportedBy(), record.getExportedAt()));
        }
        if (StringUtils.hasText(record.getExportFileName())) {
            lines.add("导出文件：" + ensureExcelExtension(record.getExportFileName()));
        }
        return lines.isEmpty() ? "" : String.join(System.lineSeparator(), lines);
    }

    private String buildNameWithTime(String name, LocalDateTime time) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(name)) {
            builder.append(name.trim());
        }
        if (time != null) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("(").append(DATE_TIME_FORMATTER.format(time)).append(")");
        }
        return builder.length() == 0 ? "无" : builder.toString();
    }

    private void writePhotos(Workbook workbook, XSSFSheet sheet, List<PhotoItem> photos) throws IOException {
        if (photos.isEmpty()) {
            return;
        }
        // 根据照片数量动态准备插槽（复制模板页、计算坐标等）。
        List<PhotoSlotTemplate> slots = preparePhotoSlots(sheet, photos.size());
        CreationHelper helper = workbook.getCreationHelper();
        XSSFDrawing drawing = sheet.createDrawingPatriarch();

        for (int i = 0; i < photos.size(); i++) {
            PhotoItem photo = photos.get(i);
            if (photo == null || !StringUtils.hasText(photo.getImagePath())) {
                continue;
            }
            PhotoSlotTemplate slot = slots.get(i);
            insertPhoto(workbook, helper, drawing, sheet, slot, photo);
        }
    }

    private List<PhotoSlotTemplate> preparePhotoSlots(XSSFSheet sheet, int photoCount) {
        List<PhotoSlotTemplate> slots = new ArrayList<>(BASE_PHOTO_SLOTS);
        if (photoCount <= BASE_PHOTO_SLOTS.size()) {
            return slots;
        }
        int additionalPhotos = photoCount - BASE_PHOTO_SLOTS.size();
        int additionalPages = (int) Math.ceil(additionalPhotos / (double) PHOTOS_PER_PAGE);

        int baseStart = THIRD_PAGE_START_ROW - 1;
        int baseEnd = THIRD_PAGE_END_ROW - 1;
        int insertPosition = baseEnd + 1;
        for (int i = 0; i < additionalPages; i++) {
            // 模板第三页作为扩展页模板，逐页复制后计算新的照片、说明单元格位置。
            sheet.copyRows(baseStart, baseEnd, insertPosition, new CellCopyPolicy());
            int rowShift = ROWS_PER_ADDITIONAL_PAGE * (i + 1);
            for (PhotoSlotTemplate template : ADDITIONAL_PAGE_SLOTS) {
                slots.add(template.shift(rowShift));
            }
            insertPosition += ROWS_PER_ADDITIONAL_PAGE;
        }
        return slots;
    }

    private void insertPhoto(Workbook workbook, CreationHelper helper, XSSFDrawing drawing, XSSFSheet sheet,
                              PhotoSlotTemplate slot, PhotoItem photo) throws IOException {
        Path imagePath = Paths.get(photo.getImagePath());
        if (!Files.exists(imagePath)) {
            throw new IOException("巡查照片不存在：" + imagePath);
        }
        byte[] imageBytes = Files.readAllBytes(imagePath);
        int pictureType = resolvePictureType(imagePath.getFileName().toString());
        int pictureIndex = workbook.addPicture(imageBytes, pictureType);

        ClientAnchor anchor = helper.createClientAnchor();
        anchor.setCol1(slot.col1() - 1);
        anchor.setRow1(slot.row1() - 1);
        anchor.setCol2(slot.col2());
        anchor.setRow2(slot.row2());
        anchor.setDx1(Units.toEMU(10));
        anchor.setDy1(Units.toEMU(10));
        anchor.setDx2(Units.toEMU(10));
        anchor.setDy2(Units.toEMU(10));
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);

        XSSFPicture picture = drawing.createPicture(anchor, pictureIndex);

        // 图片说明单独写入模板预留的文字区域。
        writePhotoDescription(sheet, slot, defaultText(photo.getDescription()));
    }

    private void writePhotoDescription(Sheet sheet, PhotoSlotTemplate slot, String description) {
        Row row = sheet.getRow(slot.descRowStart() - 1);
        if (row == null) {
            row = sheet.createRow(slot.descRowStart() - 1);
        }
        Cell cell = row.getCell(slot.descColumn() - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(description);
        for (int r = slot.descRowStart(); r <= slot.descRowEnd(); r++) {
            if (r == slot.descRowStart()) {
                continue;
            }
            Row extraRow = sheet.getRow(r - 1);
            if (extraRow == null) {
                extraRow = sheet.createRow(r - 1);
            }
            Cell extraCell = extraRow.getCell(slot.descColumn() - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            if (StringUtils.hasText(extraCell.getStringCellValue())) {
                extraCell.setCellValue("");
            }
        }
    }

    private void setCellToRightOfLabel(Sheet sheet, String label, String value) {
        findCellByLabel(sheet, label).ifPresent(labelCell -> {
            Row row = sheet.getRow(labelCell.getRowIndex());
            if (row == null) {
                return;
            }
            Cell target = row.getCell(labelCell.getColumnIndex() + 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            target.setCellValue(Optional.ofNullable(value).orElse(""));
        });
    }

    private Optional<Cell> findCellByLabel(Sheet sheet, String label) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String text = cell.getStringCellValue();
                    if (text != null && text.contains(label)) {
                        return Optional.of(cell);
                    }
                }
            }
        }
        log.debug("未在模板中找到标签：{}", label);
        return Optional.empty();
    }

    private String buildHandlingDetails(HandlingCategoryGroup group) {
        HandlingCategoryGroup effective = Optional.ofNullable(group).orElseGet(HandlingCategoryGroup::new);
        StringBuilder sb = new StringBuilder();
        // 分类段落顺序遵循业务要求，保持与模板一致。
        appendCategory(sb, "一、道路病害或损坏情况", effective.getRoadDamage());
        appendCompositeCategory(sb, "二、交通事故或清障救援情况",
                new SubCategory("（交通事故）", effective.getTrafficAccidents()),
                new SubCategory("（清障救援）", effective.getRoadRescue()));
        appendCategory(sb, "三、设施赔补偿情况", effective.getFacilityCompensations());
        appendCompositeCategory(sb, "四、大件或超限车辆检查",
                new SubCategory("（大件检查）", effective.getLargeVehicleChecks()),
                new SubCategory("（超限车辆处理）", effective.getOverloadVehicleHandling()));
        appendCategory(sb, "五、涉路施工检查", effective.getConstructionChecks());
        appendCategory(sb, "六、违法侵权事件", effective.getIllegalInfringements());
        appendCategory(sb, "七、其他情况", effective.getOtherMatters());
        return sb.toString().trim();
    }

    private void appendCategory(StringBuilder sb, String header, List<String> items) {
        if (sb.length() > 0) {
            sb.append(System.lineSeparator());
        }
        sb.append(header).append(System.lineSeparator());
        writeItems(sb, items);
    }

    private void appendCompositeCategory(StringBuilder sb, String header, SubCategory... subCategories) {
        if (sb.length() > 0) {
            sb.append(System.lineSeparator());
        }
        sb.append(header).append(System.lineSeparator());
        for (SubCategory sub : subCategories) {
            sb.append(sub.title()).append(System.lineSeparator());
            writeItems(sb, sub.items());
        }
    }

    private void writeItems(StringBuilder sb, List<String> items) {
        List<String> normalized = Optional.ofNullable(items).orElseGet(Collections::emptyList)
                .stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (normalized.isEmpty()) {
            sb.append("无").append(System.lineSeparator());
        } else {
            normalized.forEach(item -> sb.append("- ").append(item).append(System.lineSeparator()));
        }
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "无";
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private String ensureExcelExtension(String fileName) {
        String trimmed = fileName.trim();
        return trimmed.toLowerCase(Locale.ROOT).endsWith(".xlsx") ? trimmed : trimmed + ".xlsx";
    }

    private int resolvePictureType(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png")) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        if (lowerName.endsWith(".bmp")) {
            return Workbook.PICTURE_TYPE_DIB;
        }
        throw new IllegalArgumentException("不支持的图片格式：" + fileName);
    }

    private String determineFileName(InspectionRecord record) {
        String fileName = record.getExportFileName();
        if (!StringUtils.hasText(fileName)) {
            return DEFAULT_EXPORT_NAME;
        }
        return ensureExcelExtension(fileName);
    }

    private record PhotoSlotTemplate(int col1, int row1, int col2, int row2,
                                     int descRowStart, int descRowEnd, int descColumn) {
        PhotoSlotTemplate shift(int rowShift) {
            return new PhotoSlotTemplate(col1, row1 + rowShift, col2, row2 + rowShift,
                    descRowStart + rowShift, descRowEnd + rowShift, descColumn);
        }
    }

    private record SubCategory(String title, List<String> items) {
    }
}
