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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Paths;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
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
        Path template = createTemplateWorkbook(tempDir.resolve("excel/inspectionRecord.xlsx"));
        InspectionRecordExcelExporter exporter = new InspectionRecordExcelExporter(new FileSystemResource(template));
        Path photoDir = Files.createDirectories(tempDir.resolve("photos"));
        List<PhotoItem> photos = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Path image = createSampleImage(photoDir.resolve("photo-" + i + ".jpg"), "照片" + i);
            photos.add(PhotoItem.builder().imagePath(image.toString()).description("第" + i + "张照片").build());
        }

        HandlingCategoryGroup categoryGroup = HandlingCategoryGroup.builder()
                .roadDamage(List.of("路面沉陷处设置警示标志。"))
                .roadRescue(List.of("拖移故障车辆1辆。"))
                .largeVehicleChecks(List.of("检查大件运输车辆2辆，手续齐全。"))
                .overloadVehicleHandling(List.of("劝返超限车辆1辆。"))
                .otherMatters(List.of("与交警联合巡查。"))
                .build();

        InspectionRecord record = InspectionRecord.builder()
                .id(1L)
                .date(LocalDate.of(2024, 12, 1))
                .weather("晴")
                .patrolTeam("巡查一队")
                .location("K10+000-K20+000")
                .inspectionContent("路面、桥涵专项巡查。")
                .issuesFound("发现1处沉陷。")
                .handlingSituationRaw("现场设置警戒并安排抢修。")
                .handlingDetails(categoryGroup)
                .photos(photos)
                .createdBy("张三")
                .createdAt(LocalDateTime.of(2024, 12, 1, 9, 30))
                .updatedAt(LocalDateTime.of(2024, 12, 1, 18, 0))
                .exportedBy("李四")
                .exportedAt(LocalDateTime.of(2024, 12, 1, 18, 30))
                .exportFileName("record.xlsx")
                .build();

        Path exportDir = Files.createDirectories(Paths.get("src/test/resources/export"));
        Path output = exportDir.resolve("record.xlsx");
        Files.deleteIfExists(output);

        output = exporter.export(record, exportDir);
        log.info("巡查记录导出文件路径: {}", output.toAbsolutePath());
        assertThat(Files.exists(output)).isTrue();

        try (InputStream inputStream = Files.newInputStream(output);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            String dateText = readValueRightOfLabel(sheet, "巡查时间");
            assertThat(dateText).contains("2024年12月01日");

            Row dateRow = sheet.getRow(2);
            assertThat(dateRow.getCell(1).getStringCellValue()).isEqualTo("：");
            Cell targetCell = findTargetCell(sheet, "巡查时间");
            assertThat(targetCell).isNotNull();
            assertThat(targetCell.getColumnIndex()).isEqualTo(2);
            assertThat(targetCell.getStringCellValue()).contains("2024年12月01日");
            assertThat(targetCell.getCellStyle().getBorderBottom()).isEqualTo(BorderStyle.THIN);

            String handlingText = readValueRightOfLabel(sheet, "巡查、处理情况");
            assertThat(handlingText)
                    .contains("一、道路病害或损坏情况")
                    .contains("路面沉陷处设置警示标志")
                    .contains("二、交通事故或清障救援情况")
                    .contains("（清障救援）")
                    .contains("拖移故障车辆1辆")
                    .contains("七、其他情况");

            String remarkText = readValueRightOfLabel(sheet, "备注");
            assertThat(remarkText).contains("创建：张三 (2024年12月01日 09:30)");
            assertThat(remarkText).contains("导出：李四 (2024年12月01日 18:30)");

            boolean foundFifthDescription = false;
            for (Row row : sheet) {
                Cell cell = row.getCell(1);
                if (cell != null && "第5张照片".equals(cell.getStringCellValue())) {
                    assertThat(row.getRowNum()).isGreaterThanOrEqualTo(114);
                    foundFifthDescription = true;
                    break;
                }
            }
            assertThat(foundFifthDescription).isTrue();
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

    /**
     * 构造一个满足导出逻辑的最小化模板，避免在仓库中提交真实业务模板。
     */
    private Path createTemplateWorkbook(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("模板");
            for (int i = 0; i <= 150; i++) {
                sheet.createRow(i);
            }

            createLabeledRow(sheet, 2, "巡查时间");
            createLabeledRow(sheet, 3, "天气情况");
            createLabeledRow(sheet, 4, "巡查人员");
            createLabeledRow(sheet, 5, "巡查里程");
            createLabeledRow(sheet, 6, "巡查路段");

            sheet.getRow(7).createCell(0).setCellValue("巡查、处理情况");
            addColonCell(sheet, 7, 1);
            createMergedInputCell(sheet, 7, 2, 9, BorderStyle.THIN);

            sheet.getRow(11).createCell(0).setCellValue("备注");
            addColonCell(sheet, 11, 1);
            createMergedInputCell(sheet, 11, 2, 5, BorderStyle.THIN);

            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            }
        }
        return path;
    }

    private void createLabeledRow(Sheet sheet, int rowIndex, String label) {
        sheet.getRow(rowIndex).createCell(0).setCellValue(label);
        addColonCell(sheet, rowIndex, 1);
        createMergedInputCell(sheet, rowIndex, 2, 4, BorderStyle.THIN);
    }

    private void addColonCell(Sheet sheet, int rowIndex, int columnIndex) {
        sheet.getRow(rowIndex).createCell(columnIndex).setCellValue("：");
    }

    private void createMergedInputCell(Sheet sheet, int rowIndex, int startColumn, int columnSpan, BorderStyle border) {
        Row row = sheet.getRow(rowIndex);
        for (int i = 0; i < columnSpan; i++) {
            row.createCell(startColumn + i);
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, startColumn, startColumn + columnSpan - 1));
        var workbook = sheet.getWorkbook();
        var style = workbook.createCellStyle();
        style.setBorderBottom(border);
        style.setBorderTop(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
        style.setWrapText(true);
        row.getCell(startColumn).setCellStyle(style);
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
        Row row = sheet.getRow(labelCell.getRowIndex());
        int labelColumn = labelCell.getColumnIndex();
        CellRangeAddress nearest = null;
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstRow() == labelCell.getRowIndex()
                    && region.getLastRow() == labelCell.getRowIndex()
                    && region.getFirstColumn() > labelColumn) {
                if (nearest == null || region.getFirstColumn() < nearest.getFirstColumn()) {
                    nearest = region;
                }
            }
        }
        if (nearest != null) {
            return row.getCell(nearest.getFirstColumn());
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
            if (candidate.getCellType() == CellType.STRING) {
                String text = candidate.getStringCellValue();
                if (text != null && text.contains("：")) {
                    continue;
                }
                if (text == null || text.isBlank()) {
                    return candidate;
                }
            } else {
                return candidate;
            }
        }
        return row.getCell(labelColumn + 1);
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
}
