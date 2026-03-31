package com.xrcgs.roadsafety.inspection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 巡查记录 Excel 导出能力的集成测试，验证写入文本与分页照片能力。
 */
class InspectionRecordExcelExporterTest {

    private static final Logger log = LoggerFactory.getLogger(InspectionRecordExcelExporterTest.class);

    @TempDir
    Path tempDir;

    @Test
    void shouldExportInspectionRecordWithPhotosAndHandlingDetails() throws Exception {
        Path template = Paths.get("src/test/resources/excel/inspection_record.xlsx");
        InspectionRecordExcelExporter exporter = new InspectionRecordExcelExporter(new FileSystemResource(template));
        Path photoDir = Files.createDirectories(tempDir.resolve("photos"));
        List<PhotoItem> photos = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Path image = createSampleImage(photoDir.resolve("photo-" + i + ".jpg"), "照片" + i);
            photos.add(PhotoItem.builder().imagePath(image.toString()).description("第" + i + "张照片").build());
        }

        HandlingCategoryGroup categoryGroup = HandlingCategoryGroup.builder()
                .roadDamage(List.of("路面沉陷处设置警示标志。"))
                .trafficAccidents(List.of("收费站出口追尾事故处理完毕。"))
                .roadRescue(List.of("拖移故障车辆1辆。"))
                .facilityCompensations(Collections.emptyList())
                .largeVehicleChecks(List.of("检查大件运输车辆2辆，手续齐全。"))
                .overloadVehicleHandling(List.of("劝返超限车辆1辆。"))
                .constructionChecks(Collections.emptyList())
                .illegalInfringements(Collections.emptyList())
                .otherMatters(List.of("与交警联合巡查。"))
                .build();

        InspectionRecord record = InspectionRecord.builder()
                .id(1L)
                .date(LocalDate.of(2024, 12, 1))
                .unitName("乌鲁木齐葛洲坝电建路桥绕城高速公路有限公司")
                .weather("晴")
                .patrolTeam("巡查一队")
                .patrolVehicle("巡逻车A123")
                .location("K10+000-K20+000")
                .inspectionContent("路面、桥涵专项巡查。")
                .issuesFound("发现1处沉陷。")
                .handlingSituationRaw("现场设置警戒并安排抢修。")
                .handlingDetails(categoryGroup)
                .handoverSummary("交接巡查车辆与装备完毕。")
                .handoverFromDisplay("张三、李四")
                .handoverToDisplay("王五")
                .deliveryContactDisplay("送达2026-001号《工作联系单》\n被送达单位：交警大队")
                .photos(photos)
                .remark("现场秩序良好。")
                .createdBy("张三")
                .createdAt(LocalDateTime.of(2024, 12, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2024, 12, 1, 18, 0))
                .exportedBy("李四")
                .exportedAt(LocalDateTime.of(2024, 12, 1, 18, 30))
                .exportFileName("record.xlsx")
                .build();

        Path exportDir = Paths.get("src/test/resources/export");
        Files.createDirectories(exportDir);
        Path expectedOutput = exportDir.resolve("record.xlsx");
        Files.deleteIfExists(expectedOutput);

        Path output = exporter.export(record, exportDir);
        log.info("巡查记录导出文件路径: {}", output.toAbsolutePath());
        assertThat(output).isEqualTo(expectedOutput);
        assertThat(Files.exists(output)).isTrue();
        assertThat(output.getFileName().toString()).isEqualTo("record.xlsx");

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            XSSFSheet infoSheet = workbook.getSheetAt(0);

            assertThat(infoSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("巡查记录表");
            assertThat(infoSheet.getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("单位：" + record.getUnitName());

            assertThat(readValueRightOfLabel(infoSheet, "巡查时间")).contains("2024年12月01日");
            assertThat(readValueRightOfLabel(infoSheet, "天气情况")).isEqualTo("晴");
            assertThat(readValueRightOfLabel(infoSheet, "巡查人员")).isEqualTo("巡查一队");
            assertThat(readValueRightOfLabel(infoSheet, "巡查车辆")).isEqualTo("巡逻车A123");
            String handoverText = readValueRightOfLabel(infoSheet, "巡查车辆、装备、案件等交接情况");
            assertThat(handoverText)
                    .startsWith("巡查车辆、装备、案件等交接情况：")
                    .contains("交接巡查车辆与装备完毕。");
            assertThat(getCellString(infoSheet, 8, 1)).isEqualTo("张三、李四");
            assertThat(getCellString(infoSheet, 8, 3)).isEqualTo("王五");
            String deliveryText = getCellString(infoSheet, 10, 0).replace("\r\n", "\n");
            assertThat(deliveryText)
                    .startsWith("送达2026-001号《工作联系单》")
                    .contains("被送达单位：交警大队");
            Cell deliveryCell = infoSheet.getRow(10).getCell(0);
            assertThat(deliveryCell.getCellStyle().getVerticalAlignment()).isEqualTo(VerticalAlignment.TOP);
            assertThat(deliveryCell.getCellStyle().getAlignment()).isEqualTo(HorizontalAlignment.LEFT);

            String handlingText = readValueRightOfLabel(infoSheet, "巡查、处理情况");
            String normalizedHandling = handlingText.replace("\r\n", "\n");
            assertThat(normalizedHandling)
                    .startsWith("巡查、处理情况：")
                    .contains("日常巡查时间白班16：00-18：00，夜班：22：00-次日1：00巡查期间发现以下问题。")
                    .contains("一、道路病害或损坏情况：\n路面沉陷处设置警示标志。")
                    .contains("二、交通事故或清障救援情况：\n1.收费站出口追尾事故处理完毕。\n2.拖移故障车辆1辆。")
                    .contains("三、设施赔补偿情况：\n无")
                    .contains("四、大件或超限车辆检查：\n1.检查大件运输车辆2辆，手续齐全。\n2.劝返超限车辆1辆。")
                    .contains("五、涉路施工检查：\n无")
                    .contains("六、违法侵权事件：\n无")
                    .contains("七、其他情况：\n与交警联合巡查。");

            String remarkText = readValueRightOfLabel(infoSheet, "备注").replace("\r\n", "\n");
            assertThat(remarkText)
                    .startsWith("备注：")
                    .isEqualTo("备注：\n现场秩序良好。");

            boolean auditFound = false;
            for (Row row : infoSheet) {
                if (row instanceof XSSFRow xssfRow && xssfRow.getZeroHeight()) {
                    Cell keyCell = row.getCell(0);
                    if (keyCell != null && "createdBy".equals(keyCell.getStringCellValue())) {
                        XSSFRow valueRow = (XSSFRow) infoSheet.getRow(row.getRowNum() + 1);
                        assertThat((Object) valueRow).isNotNull();
                        assertThat(valueRow.getCell(0).getStringCellValue()).isEqualTo("张三");
                        assertThat(valueRow.getCell(1).getStringCellValue()).contains("2024年12月01日 09:30");
                        assertThat(valueRow.getCell(4).getStringCellValue()).contains("2024年12月01日 18:30");
                        auditFound = true;
                        break;
                    }
                }
            }
            assertThat(auditFound).isTrue();

            int expectedPhotoSheets = (int) Math.ceil(photos.size() / 2.0);
            assertThat(workbook.getSheet("Sheet2")).isNull();
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1 + expectedPhotoSheets);

            for (int page = 1; page <= expectedPhotoSheets; page++) {
                XSSFSheet photoSheet = workbook.getSheet("照片页" + page);
                assertThat(photoSheet).isNotNull();

                XSSFDrawing drawing = (XSSFDrawing) photoSheet.getDrawingPatriarch();
                assertThat(drawing).isNotNull();
                long pictureCount = drawing.getShapes().stream().filter(XSSFPicture.class::isInstance).count();
                int startIndex = (page - 1) * 2;
                int expectedPictures = Math.min(2, Math.max(0, photos.size() - startIndex));
                assertThat(pictureCount).isEqualTo(expectedPictures);

                Row topDescRow = photoSheet.getRow(26);
                if (startIndex < photos.size()) {
                    assertThat(topDescRow).isNotNull();
                    assertThat(topDescRow.getCell(0).getStringCellValue())
                            .isEqualTo("第" + (startIndex + 1) + "张照片");
                }
                Row bottomDescRow = photoSheet.getRow(51);
                if (startIndex + 1 < photos.size()) {
                    assertThat(bottomDescRow).isNotNull();
                    assertThat(bottomDescRow.getCell(0).getStringCellValue())
                            .isEqualTo("第" + (startIndex + 2) + "张照片");
                } else {
                    Cell bottomCell = bottomDescRow == null ? null : bottomDescRow.getCell(0);
                    assertThat(bottomCell == null ? "" : bottomCell.getStringCellValue()).isEmpty();
                }
            }
        }
    }

    @Test
    void shouldShrinkFontAndExpandRowForOverflowText() throws Exception {
        Path template = Paths.get("src/test/resources/excel/inspection_record.xlsx");
        InspectionRecordExcelExporter exporter = new InspectionRecordExcelExporter(new FileSystemResource(template));

        InspectionRecord baseline = buildOverflowRecord(
                "baseline.xlsx",
                "张三、李四",
                "王五",
                "送达2026-001号《工作联系单》\n被送达单位：交警大队",
                "现场秩序良好。");
        InspectionRecord overflow = buildOverflowRecord(
                "overflow.xlsx",
                repeat("交班人员", 24),
                repeat("接班人员", 20),
                "送达" + repeat("2026-001", 12) + "号《工作联系单》\n被送达单位：" + repeat("交警大队", 14),
                repeat("备注内容较长需要缩放并避免打印截断。", 120));

        Path baselinePath = exporter.export(baseline, tempDir);
        Path overflowPath = exporter.export(overflow, tempDir);

        try (InputStream baselineIn = Files.newInputStream(baselinePath);
             XSSFWorkbook baselineWb = new XSSFWorkbook(baselineIn);
             InputStream overflowIn = Files.newInputStream(overflowPath);
             XSSFWorkbook overflowWb = new XSSFWorkbook(overflowIn)) {
            XSSFSheet baselineSheet = baselineWb.getSheetAt(0);
            XSSFSheet overflowSheet = overflowWb.getSheetAt(0);

            Cell baselineHandoverCell = baselineSheet.getRow(8).getCell(1);
            Cell overflowHandoverCell = overflowSheet.getRow(8).getCell(1);
            assertThat(overflowHandoverCell.getCellStyle().getShrinkToFit()).isTrue();
            assertThat(getFontSizeInPoints(overflowWb, overflowHandoverCell))
                    .isLessThan(getFontSizeInPoints(baselineWb, baselineHandoverCell));

            Cell baselineRemarkCell = baselineSheet.getRow(11).getCell(0);
            Cell overflowRemarkCell = overflowSheet.getRow(11).getCell(0);
            assertThat((Object) baselineRemarkCell).isNotNull();
            assertThat((Object) overflowRemarkCell).isNotNull();
            assertThat(overflowRemarkCell.getCellStyle().getShrinkToFit()).isTrue();
            assertThat(getFontSizeInPoints(overflowWb, overflowRemarkCell))
                    .isLessThanOrEqualTo(getFontSizeInPoints(baselineWb, baselineRemarkCell));

            float baselineRemarkRowHeight = baselineSheet.getRow(baselineRemarkCell.getRowIndex()).getHeightInPoints();
            float overflowRemarkRowHeight = overflowSheet.getRow(overflowRemarkCell.getRowIndex()).getHeightInPoints();
            assertThat(overflowRemarkRowHeight).isGreaterThan(baselineRemarkRowHeight);
        }
    }

    private Path createSampleImage(Path path, String text) throws IOException {
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, 40, image.getHeight() / 2);
        graphics.dispose();
        javax.imageio.ImageIO.write(image, "jpg", path.toFile());
        return path;
    }

    private InspectionRecord buildOverflowRecord(String fileName,
                                                 String handoverFrom,
                                                 String handoverTo,
                                                 String deliveryText,
                                                 String remark) {
        return InspectionRecord.builder()
                .id(1L)
                .date(LocalDate.of(2024, 12, 1))
                .unitName("乌鲁木齐葛洲坝电建路桥绕城高速公司")
                .weather("晴")
                .patrolTeam("巡查一队")
                .patrolVehicle("巡逻车A123")
                .location("K10+000-K20+000")
                .handoverSummary("交接巡查车辆与装备完毕。")
                .handoverFromDisplay(handoverFrom)
                .handoverToDisplay(handoverTo)
                .deliveryContactDisplay(deliveryText)
                .remark(remark)
                .photos(Collections.emptyList())
                .exportFileName(fileName)
                .build();
    }

    private String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }

    private short getFontSizeInPoints(XSSFWorkbook workbook, Cell cell) {
        Font font = workbook.getFontAt(cell.getCellStyle().getFontIndex());
        return font.getFontHeightInPoints();
    }

    private String readValueRightOfLabel(Sheet sheet, String label) {
        Cell targetCell = findTargetCell(sheet, label);
        if (targetCell == null) {
            throw new IllegalStateException("未找到标签: " + label);
        }
        return targetCell.getStringCellValue();
    }

    private Cell findTargetCell(Sheet sheet, String label) {
        Cell labelCell = findLabelCell(sheet, label);
        if (labelCell == null) {
            return null;
        }
        CellRangeAddress labelRegion = findMergedRegionContaining(sheet, labelCell);
        if (labelRegion != null && labelRegion.getFirstColumn() == labelCell.getColumnIndex()) {
            return labelCell;
        }
        Row row = sheet.getRow(labelCell.getRowIndex());
        int labelColumn = labelCell.getColumnIndex();
        CellRangeAddress nearest = null;
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstColumn() > labelColumn
                    && region.getFirstRow() <= labelCell.getRowIndex()
                    && region.getLastRow() >= labelCell.getRowIndex()) {
                if (nearest == null || region.getFirstColumn() < nearest.getFirstColumn()) {
                    nearest = region;
                }
            }
        }
        if (nearest != null) {
            Row targetRow = sheet.getRow(nearest.getFirstRow());
            if (targetRow == null) {
                targetRow = sheet.createRow(nearest.getFirstRow());
            }
            Cell mergedCell = targetRow.getCell(nearest.getFirstColumn());
            if (mergedCell != null) {
                return mergedCell;
            }
        }
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0) {
            lastCellNum = (short) (labelColumn + 2);
        }
        for (int column = labelColumn + 1; column <= lastCellNum; column++) {
            Cell candidate = row.getCell(column);
            if (candidate == null) {
                continue;
            }
            if (isLabelLikeCell(candidate)) {
                continue;
            }
            return candidate;
        }
        return row.getCell(labelColumn + 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
    }

    private CellRangeAddress findMergedRegionContaining(Sheet sheet, Cell cell) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(cell.getRowIndex(), cell.getColumnIndex())) {
                return region;
            }
        }
        return null;
    }

    private Cell findLabelCell(Sheet sheet, String label) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(label)) {
                    return cell;
                }
            }
        }
        return null;
    }

    private boolean isLabelLikeCell(Cell cell) {
        if (cell.getCellType() != CellType.STRING) {
            return false;
        }
        String text = cell.getStringCellValue();
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.endsWith("：") && !trimmed.contains("\n") && !trimmed.contains("\r");
    }

    private String getCellString(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return cell.getStringCellValue();
    }
}
