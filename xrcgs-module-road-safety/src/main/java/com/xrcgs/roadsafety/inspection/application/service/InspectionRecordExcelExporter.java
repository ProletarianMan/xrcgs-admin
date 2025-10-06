package com.xrcgs.roadsafety.inspection.application.service;

import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    private static final String TEMPLATE_CLASSPATH = "excel/inspection_record.xlsx";
    private static final String DEFAULT_EXPORT_NAME = "inspection_record_output.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
    private static final int PHOTOS_PER_PAGE = 2;
    private static final double PHOTO_MARGIN_POINTS = 2.0;
    private static final long EMU_PER_PIXEL = 9525L;
    private static final long EMU_PER_POINT = 12700L;

    private static final List<PhotoSlotTemplate> PAGE_PHOTO_SLOTS = List.of(
            new PhotoSlotTemplate(1, 2, 4, 26, 27, 27, 1),
            new PhotoSlotTemplate(1, 28, 4, 51, 52, 52, 1)
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
             Workbook workbookDelegate = WorkbookFactory.create(templateStream)) {
            XSSFWorkbook workbook = (XSSFWorkbook) workbookDelegate;
            XSSFSheet infoSheet = workbook.getSheetAt(0);

            // 逐段将巡查信息写入到模板对应区域，保持原有样式与格式。
            fillHeaderInfo(infoSheet, record);
            fillHandlingSection(infoSheet, record);
            fillRemarks(infoSheet, record);
            fillAuditTrail(infoSheet, record);
            writePhotos(workbook, Optional.ofNullable(record.getPhotos()).orElse(Collections.emptyList()));

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

    private void fillHeaderInfo(XSSFSheet sheet, InspectionRecord record) {
        adjustRowHeight(sheet, 0, 29.25f);
        adjustRowHeight(sheet, 1, 25f);
        for (int i = 2; i <= 5; i++) {
            adjustRowHeight(sheet, i, 28f);
        }

        setCellValue(sheet, 0, 0, "巡查记录表");
        setCellValue(sheet, 1, 0, "单位：" + defaultText(record.getUnitName()));

        setCellToRightOfLabel(sheet, "巡查时间", formatDate(record.getDate()));
        setCellToRightOfLabel(sheet, "天气情况", defaultText(record.getWeather()));
        setCellToRightOfLabel(sheet, "巡查人员", defaultText(record.getPatrolTeam()));
        setCellToRightOfLabel(sheet, "巡查车辆", defaultText(record.getPatrolVehicle()));
        setCellToRightOfLabel(sheet, "巡查里程", defaultText(record.getLocation()));
        setCellToRightOfLabel(sheet, "巡查路段", defaultText(record.getLocation()));
        setCellToRightOfLabel(sheet, "巡查车辆、装备、案件等交接情况", defaultText(record.getHandoverSummary()));
    }

    private void setCellValue(XSSFSheet sheet, int rowIndex, int columnIndex, String value) {
        XSSFRow row = Optional.ofNullable(sheet.getRow(rowIndex)).orElseGet(() -> sheet.createRow(rowIndex));
        row.setZeroHeight(false);
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(Optional.ofNullable(value).orElse(""));
    }

    private void adjustRowHeight(XSSFSheet sheet, int rowIndex, float height) {
        XSSFRow row = Optional.ofNullable(sheet.getRow(rowIndex)).orElseGet(() -> sheet.createRow(rowIndex));
        row.setHeightInPoints(height);
    }

    private void fillHandlingSection(Sheet sheet, InspectionRecord record) {
        // 构建巡查概述+处理情况的正文，按模板顺序组织段落。
        StringBuilder builder = new StringBuilder();
        builder.append("巡查内容：").append(defaultText(record.getInspectionContent())).append(System.lineSeparator());
        builder.append("问题描述：").append(defaultText(record.getIssuesFound())).append(System.lineSeparator());
        builder.append("处理情况（原始记录）：").append(defaultText(record.getHandlingSituationRaw())).append(System.lineSeparator());
        builder.append("处理情况（分类汇总）：").append(System.lineSeparator());
        builder.append(buildHandlingDetails(record.getHandlingDetails()));
        setCellToRightOfLabel(sheet, "巡查、处理情况", builder.toString().stripTrailing());
    }

    private void fillRemarks(Sheet sheet, InspectionRecord record) {
        String remark = buildRemark(record);
        setCellToRightOfLabel(sheet, "备注", remark);
    }

    private void fillAuditTrail(XSSFSheet sheet, InspectionRecord record) {
        int startRowIndex = Math.max(sheet.getLastRowNum() + 2, 30);
        XSSFRow keyRow = Optional.ofNullable(sheet.getRow(startRowIndex)).orElseGet(() -> sheet.createRow(startRowIndex));
        XSSFRow valueRow = Optional.ofNullable(sheet.getRow(startRowIndex + 1)).orElseGet(() -> sheet.createRow(startRowIndex + 1));
        keyRow.setZeroHeight(true);
        valueRow.setZeroHeight(true);

        writeAuditCell(keyRow, 0, "createdBy");
        writeAuditCell(valueRow, 0, normalizeAuditValue(record.getCreatedBy()));

        writeAuditCell(keyRow, 1, "createdAt");
        writeAuditCell(valueRow, 1, formatDateTime(record.getCreatedAt()));

        writeAuditCell(keyRow, 2, "updatedAt");
        writeAuditCell(valueRow, 2, formatDateTime(record.getUpdatedAt()));

        writeAuditCell(keyRow, 3, "exportedBy");
        writeAuditCell(valueRow, 3, normalizeAuditValue(record.getExportedBy()));

        writeAuditCell(keyRow, 4, "exportedAt");
        writeAuditCell(valueRow, 4, formatDateTime(record.getExportedAt()));
    }

    private void writeAuditCell(Row row, int columnIndex, String value) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(Optional.ofNullable(value).orElse(""));
    }

    private String normalizeAuditValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String buildRemark(InspectionRecord record) {
        List<String> lines = new ArrayList<>();
        if (StringUtils.hasText(record.getRemark())) {
            lines.add(record.getRemark().trim());
        }
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
        if (lines.isEmpty()) {
            lines.add("无");
        }
        return String.join(System.lineSeparator(), lines);
    }

    private String buildNameWithTime(String name, LocalDateTime time) {
        StringBuilder builder = new StringBuilder();
        String normalizedName = normalizeAuditValue(name);
        if (StringUtils.hasText(normalizedName)) {
            builder.append(normalizedName);
        }
        String formattedTime = formatDateTime(time);
        if (StringUtils.hasText(formattedTime)) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append("(").append(formattedTime).append(")");
        }
        return builder.length() == 0 ? "无" : builder.toString();
    }

    private void writePhotos(XSSFWorkbook workbook, List<PhotoItem> photos) {
        XSSFSheet templateSheet = locatePhotoTemplate(workbook);
        if (templateSheet == null) {
            return;
        }
        int templateIndex = workbook.getSheetIndex(templateSheet);
        if (photos.isEmpty()) {
            workbook.removeSheetAt(templateIndex);
            return;
        }

        CreationHelper helper = workbook.getCreationHelper();
        int totalPages = (int) Math.ceil(photos.size() / (double) PHOTOS_PER_PAGE);
        for (int page = 0; page < totalPages; page++) {
            XSSFSheet photoSheet = workbook.cloneSheet(templateIndex);
            int newIndex = workbook.getSheetIndex(photoSheet);
            workbook.setSheetName(newIndex, "照片页" + (page + 1));
            XSSFDrawing drawing = photoSheet.createDrawingPatriarch();

            for (int slotIndex = 0; slotIndex < PAGE_PHOTO_SLOTS.size(); slotIndex++) {
                int photoIndex = page * PAGE_PHOTO_SLOTS.size() + slotIndex;
                PhotoSlotTemplate slot = PAGE_PHOTO_SLOTS.get(slotIndex);
                if (photoIndex >= photos.size()) {
                    clearDescriptionCell(photoSheet, slot);
                    continue;
                }

                PhotoItem photo = photos.get(photoIndex);
                if (photo == null || !StringUtils.hasText(photo.getImagePath())) {
                    clearDescriptionCell(photoSheet, slot);
                    continue;
                }

                try {
                    ImageResource resource = loadImageResource(photo.getImagePath());
                    if (resource == null) {
                        clearDescriptionCell(photoSheet, slot);
                        continue;
                    }
                    int pictureIndex = workbook.addPicture(resource.data(), resource.pictureType());
                    XSSFClientAnchor anchor = (XSSFClientAnchor) helper.createClientAnchor();
                    configureAnchor(photoSheet, anchor, slot, resource);
                    anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
                    drawing.createPicture(anchor, pictureIndex);
                    writePhotoDescription(photoSheet, slot, defaultText(photo.getDescription()));
                } catch (IOException ex) {
                    log.warn("巡查照片写入失败: {}", photo.getImagePath(), ex);
                    clearDescriptionCell(photoSheet, slot);
                }
            }
        }

        int templatePosition = workbook.getSheetIndex(templateSheet);
        if (templatePosition >= 0) {
            workbook.removeSheetAt(templatePosition);
        }
    }

    private XSSFSheet locatePhotoTemplate(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.getSheet("Sheet2");
        if (sheet != null) {
            return sheet;
        }
        if (workbook.getNumberOfSheets() > 1) {
            return workbook.getSheetAt(1);
        }
        log.warn("巡查照片模板页缺失，跳过照片写入");
        return null;
    }

    private void clearDescriptionCell(XSSFSheet sheet, PhotoSlotTemplate slot) {
        Row row = sheet.getRow(slot.descRowStart() - 1);
        if (row == null) {
            row = sheet.createRow(slot.descRowStart() - 1);
        }
        Cell cell = row.getCell(slot.descColumn() - 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue("");
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

    private void configureAnchor(XSSFSheet sheet, XSSFClientAnchor anchor, PhotoSlotTemplate slot, ImageResource resource) {
        long slotWidthEmu = calculateSlotWidthEmu(sheet, slot);
        long slotHeightEmu = calculateSlotHeightEmu(sheet, slot);
        long marginEmu = Units.toEMU(PHOTO_MARGIN_POINTS);

        long availableWidth = Math.max(slotWidthEmu - 2 * marginEmu, slotWidthEmu / 2);
        long availableHeight = Math.max(slotHeightEmu - 2 * marginEmu, slotHeightEmu / 2);
        if (availableWidth <= 0) {
            availableWidth = Math.max(slotWidthEmu, EMU_PER_PIXEL);
        }
        if (availableHeight <= 0) {
            availableHeight = Math.max(slotHeightEmu, EMU_PER_POINT);
        }

        long imageWidth = resource.widthPx() > 0 ? Math.round(resource.widthPx() * EMU_PER_PIXEL) : availableWidth;
        long imageHeight = resource.heightPx() > 0 ? Math.round(resource.heightPx() * EMU_PER_PIXEL) : availableHeight;

        double widthScale = availableWidth > 0 ? (double) availableWidth / imageWidth : 1.0;
        double heightScale = availableHeight > 0 ? (double) availableHeight / imageHeight : 1.0;
        double scale = Math.min(widthScale, heightScale);
        scale = Math.min(scale, 1.0);

        long scaledWidth = Math.max(1L, Math.round(imageWidth * scale));
        long scaledHeight = Math.max(1L, Math.round(imageHeight * scale));

        long horizontalStart = Math.max(marginEmu, (slotWidthEmu - scaledWidth) / 2);
        long horizontalEnd = horizontalStart + scaledWidth;
        long rightMargin = Math.max(0, slotWidthEmu - horizontalEnd);
        if (rightMargin < marginEmu) {
            long adjustment = marginEmu - rightMargin;
            horizontalStart = Math.max(marginEmu, horizontalStart - adjustment);
            horizontalEnd = horizontalStart + scaledWidth;
        }
        if (horizontalStart < marginEmu) {
            horizontalStart = marginEmu;
            horizontalEnd = Math.min(slotWidthEmu - marginEmu, horizontalStart + scaledWidth);
        }
        if (horizontalEnd > slotWidthEmu - marginEmu) {
            horizontalEnd = slotWidthEmu - marginEmu;
            horizontalStart = Math.max(marginEmu, horizontalEnd - scaledWidth);
        }
        if (horizontalEnd <= horizontalStart) {
            horizontalEnd = Math.min(slotWidthEmu, horizontalStart + Math.max(scaledWidth, EMU_PER_PIXEL));
        }

        long verticalStart = Math.max(marginEmu, (slotHeightEmu - scaledHeight) / 2);
        long verticalEnd = verticalStart + scaledHeight;
        long bottomMargin = Math.max(0, slotHeightEmu - verticalEnd);
        if (bottomMargin < marginEmu) {
            long adjustment = marginEmu - bottomMargin;
            verticalStart = Math.max(marginEmu, verticalStart - adjustment);
            verticalEnd = verticalStart + scaledHeight;
        }
        if (verticalStart < marginEmu) {
            verticalStart = marginEmu;
            verticalEnd = Math.min(slotHeightEmu - marginEmu, verticalStart + scaledHeight);
        }
        if (verticalEnd > slotHeightEmu - marginEmu) {
            verticalEnd = slotHeightEmu - marginEmu;
            verticalStart = Math.max(marginEmu, verticalEnd - scaledHeight);
        }
        if (verticalEnd <= verticalStart) {
            verticalEnd = Math.min(slotHeightEmu, verticalStart + Math.max(scaledHeight, EMU_PER_POINT));
        }

        AnchorCoordinate columnStart = resolveColumnCoordinate(sheet, slot.col1() - 1, slot.col2() - 1, horizontalStart);
        AnchorCoordinate columnEnd = resolveColumnCoordinate(sheet, slot.col1() - 1, slot.col2() - 1, horizontalEnd);
        AnchorCoordinate rowStart = resolveRowCoordinate(sheet, slot.row1() - 1, slot.row2() - 1, verticalStart);
        AnchorCoordinate rowEnd = resolveRowCoordinate(sheet, slot.row1() - 1, slot.row2() - 1, verticalEnd);

        anchor.setCol1(columnStart.index());
        anchor.setDx1(columnStart.offset());
        anchor.setCol2(columnEnd.index());
        anchor.setDx2(columnEnd.offset());
        anchor.setRow1(rowStart.index());
        anchor.setDy1(rowStart.offset());
        anchor.setRow2(rowEnd.index());
        anchor.setDy2(rowEnd.offset());
    }

    private long calculateSlotWidthEmu(XSSFSheet sheet, PhotoSlotTemplate slot) {
        long width = 0;
        for (int column = slot.col1() - 1; column <= slot.col2() - 1; column++) {
            width += columnWidthInEmu(sheet, column);
        }
        return width;
    }

    private long calculateSlotHeightEmu(XSSFSheet sheet, PhotoSlotTemplate slot) {
        long height = 0;
        for (int rowIndex = slot.row1() - 1; rowIndex <= slot.row2() - 1; rowIndex++) {
            height += rowHeightInEmu(sheet, rowIndex);
        }
        return height;
    }

    private AnchorCoordinate resolveColumnCoordinate(Sheet sheet, int startColumn, int endColumn, long offsetEmu) {
        long clamped = Math.max(0, offsetEmu);
        long consumed = 0;
        for (int column = startColumn; column <= endColumn; column++) {
            long columnWidth = columnWidthInEmu(sheet, column);
            long next = consumed + columnWidth;
            if (clamped < next || column == endColumn) {
                long position = Math.min(Math.max(0, clamped - consumed), columnWidth);
                return new AnchorCoordinate(column, (int) Math.min(position, Integer.MAX_VALUE));
            }
            consumed = next;
        }
        return new AnchorCoordinate(endColumn, 0);
    }

    private AnchorCoordinate resolveRowCoordinate(Sheet sheet, int startRow, int endRow, long offsetEmu) {
        long clamped = Math.max(0, offsetEmu);
        long consumed = 0;
        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            long rowHeight = rowHeightInEmu(sheet, rowIndex);
            long next = consumed + rowHeight;
            if (clamped < next || rowIndex == endRow) {
                long position = Math.min(Math.max(0, clamped - consumed), rowHeight);
                return new AnchorCoordinate(rowIndex, (int) Math.min(position, Integer.MAX_VALUE));
            }
            consumed = next;
        }
        return new AnchorCoordinate(endRow, 0);
    }

    private long columnWidthInEmu(Sheet sheet, int columnIndex) {
        if (sheet.isColumnHidden(columnIndex)) {
            return EMU_PER_PIXEL;
        }
        double pixels = sheet.getColumnWidth(columnIndex) / 256.0 * Units.DEFAULT_CHARACTER_WIDTH;
        if (Double.isNaN(pixels) || pixels <= 0) {
            double fallbackCharacters = sheet.getDefaultColumnWidth();
            pixels = fallbackCharacters * Units.DEFAULT_CHARACTER_WIDTH;
        }
        if (Double.isNaN(pixels) || pixels <= 0) {
            pixels = 64.0;
        }
        return Math.max(EMU_PER_PIXEL, Math.round(pixels * EMU_PER_PIXEL));
    }

    private long rowHeightInEmu(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        double points = row != null ? row.getHeightInPoints() : sheet.getDefaultRowHeightInPoints();
        if (points <= 0) {
            points = sheet.getDefaultRowHeightInPoints();
        }
        return Math.max(EMU_PER_POINT, Math.round(points * EMU_PER_POINT));
    }

    private ImageResource loadImageResource(String location) throws IOException {
        String value = location.trim();
        if (value.startsWith("data:image")) {
            return loadFromDataUri(value);
        }
        try {
            Path path = Paths.get(value);
            if (Files.exists(path)) {
                byte[] data = Files.readAllBytes(path);
                int type = resolvePictureType(path.getFileName().toString());
                return buildImageResource(data, type);
            }
        } catch (InvalidPathException ignored) {
            // treat as base64 below
        }
        ImageResource base64Resource = tryDecodeBase64(value);
        if (base64Resource != null) {
            return base64Resource;
        }
        log.warn("无法识别的巡查照片路径：{}", location);
        return null;
    }

    private ImageResource loadFromDataUri(String uri) throws IOException {
        int commaIndex = uri.indexOf(',');
        if (commaIndex < 0) {
            throw new IOException("无效的图片数据: " + uri.substring(0, Math.min(uri.length(), 32)));
        }
        String metadata = uri.substring(5, commaIndex);
        int semicolonIndex = metadata.indexOf(';');
        String mime = semicolonIndex >= 0 ? metadata.substring(0, semicolonIndex) : metadata;
        String base64 = uri.substring(commaIndex + 1);
        byte[] data = Base64.getDecoder().decode(base64);
        int type = resolvePictureTypeFromMime(mime);
        return buildImageResource(data, type);
    }

    private ImageResource tryDecodeBase64(String value) {
        try {
            byte[] data = Base64.getDecoder().decode(value);
            int type = detectPictureType(data);
            return buildImageResource(data, type);
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    private ImageResource buildImageResource(byte[] data, int pictureType) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("无法解析图片内容");
            }
            return new ImageResource(data, pictureType, image.getWidth(), image.getHeight());
        }
    }

    private int resolvePictureTypeFromMime(String mimeType) {
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/png" -> Workbook.PICTURE_TYPE_PNG;
            case "image/jpeg", "image/jpg" -> Workbook.PICTURE_TYPE_JPEG;
            case "image/bmp" -> Workbook.PICTURE_TYPE_DIB;
            default -> throw new IllegalArgumentException("不支持的图片类型: " + mimeType);
        };
    }

    private int detectPictureType(byte[] data) {
        if (data.length >= 8
                && data[0] == (byte) 0x89
                && data[1] == (byte) 0x50
                && data[2] == (byte) 0x4E
                && data[3] == (byte) 0x47) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        if (data.length >= 2 && data[0] == 0x42 && data[1] == 0x4D) {
            return Workbook.PICTURE_TYPE_DIB;
        }
        throw new IllegalArgumentException("无法识别的图片格式");
    }

    private void setCellToRightOfLabel(Sheet sheet, String label, String value) {
        findCellByLabel(sheet, label).ifPresent(labelCell -> {
            Cell target = locateTargetCell(sheet, labelCell);
            if (target == null) {
                Row row = sheet.getRow(labelCell.getRowIndex());
                if (row != null) {
                    target = row.getCell(labelCell.getColumnIndex() + 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                }
                if (target == null) {
                    log.debug("未找到标签 [{}] 对应的录入单元格，保持模板原样", label);
                    return;
                }
            }
            target.setCellValue(Optional.ofNullable(value).orElse(""));
        });
    }

    private Cell locateTargetCell(Sheet sheet, Cell labelCell) {
        Row row = sheet.getRow(labelCell.getRowIndex());
        if (row == null) {
            return null;
        }

        Cell mergedTarget = findMergedTarget(sheet, labelCell, row);
        if (mergedTarget != null) {
            return mergedTarget;
        }

        int labelColumn = labelCell.getColumnIndex();
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            lastCellNum = (short) (labelColumn + 2);
        }
        for (int column = labelColumn + 1; column <= lastCellNum; column++) {
            Cell candidate = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (candidate == null) {
                candidate = row.getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            }
            if (candidate == null) {
                continue;
            }
            if (candidate.getCellType() == CellType.STRING) {
                String text = candidate.getStringCellValue();
                if (text != null && text.contains("：")) {
                    continue;
                }
            }
            return candidate;
        }
        return row.getCell(labelColumn + 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private Cell findMergedTarget(Sheet sheet, Cell labelCell, Row row) {
        int labelColumn = labelCell.getColumnIndex();
        int rowIndex = labelCell.getRowIndex();
        CellRangeAddress nearest = null;
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstColumn() > labelColumn
                    && region.getFirstRow() <= rowIndex
                    && region.getLastRow() >= rowIndex) {
                if (nearest == null || region.getFirstColumn() < nearest.getFirstColumn()) {
                    nearest = region;
                }
            }
        }
        if (nearest == null) {
            return null;
        }
        Row targetRow = sheet.getRow(nearest.getFirstRow());
        if (targetRow == null) {
            targetRow = sheet.createRow(nearest.getFirstRow());
        }
        return targetRow.getCell(nearest.getFirstColumn(), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
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
        return sb.toString().stripTrailing();
    }

    private void appendCategory(StringBuilder sb, String header, List<String> items) {
        if (sb.length() > 0) {
            sb.append(System.lineSeparator());
        }
        sb.append(header).append("：").append(System.lineSeparator());
        writeItems(sb, items);
    }

    private void appendCompositeCategory(StringBuilder sb, String header, SubCategory... subCategories) {
        if (sb.length() > 0) {
            sb.append(System.lineSeparator());
        }
        sb.append(header).append("：").append(System.lineSeparator());
        for (int i = 0; i < subCategories.length; i++) {
            SubCategory sub = subCategories[i];
            sb.append(sub.title()).append(System.lineSeparator());
            writeItems(sb, sub.items());
            if (i < subCategories.length - 1) {
                sb.append(System.lineSeparator());
            }
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

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : DATE_TIME_FORMATTER.format(time);
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
    }

    private record AnchorCoordinate(int index, int offset) {
    }

    private record ImageResource(byte[] data, int pictureType, int widthPx, int heightPx) {
    }

    private record SubCategory(String title, List<String> items) {
    }
}
