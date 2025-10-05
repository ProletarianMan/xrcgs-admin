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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.file.Paths;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
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

        Path exportDir = Files.createDirectories(tempDir.resolve("export"));
        Path output = exporter.export(record, exportDir);
        log.info("巡查记录导出文件路径: {}", output.toAbsolutePath());
        assertThat(Files.exists(output)).isTrue();

        try (InputStream inputStream = Files.newInputStream(output);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            XSSFSheet infoSheet = (XSSFSheet) workbook.getSheetAt(0);

            assertThat(infoSheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("巡查记录表");
            assertThat(infoSheet.getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("单位：" + record.getUnitName());

            assertThat(readValueRightOfLabel(infoSheet, "巡查时间")).contains("2024年12月01日");
            assertThat(readValueRightOfLabel(infoSheet, "天气情况")).isEqualTo("晴");
            assertThat(readValueRightOfLabel(infoSheet, "巡查人员")).isEqualTo("巡查一队");

            String handlingText = readValueRightOfLabel(infoSheet, "巡查、处理情况");
            String normalizedHandling = handlingText.replace("\r\n", "\n");
            assertThat(normalizedHandling)
                    .contains("一、道路病害或损坏情况：")
                    .contains("- 路面沉陷处设置警示标志。")
                    .contains("二、交通事故或清障救援情况：")
                    .contains("（交通事故）")
                    .contains("- 收费站出口追尾事故处理完毕。")
                    .contains("（清障救援）")
                    .contains("- 拖移故障车辆1辆。")
                    .contains("三、设施赔补偿情况：\n无")
                    .contains("七、其他情况：")
                    .contains("- 与交警联合巡查。");

            String remarkText = readValueRightOfLabel(infoSheet, "备注").replace("\r\n", "\n");
            assertThat(remarkText)
                    .contains("创建：张三 (2024年12月01日 09:30)")
                    .contains("最后更新时间：2024年12月01日 18:00")
                    .contains("导出：李四 (2024年12月01日 18:30)");

            boolean auditFound = false;
            for (Row row : infoSheet) {
                if (row instanceof XSSFRow xssfRow && xssfRow.getZeroHeight()) {
                    Cell keyCell = row.getCell(0);
                    if (keyCell != null && "createdBy".equals(keyCell.getStringCellValue())) {
                        XSSFRow valueRow = (XSSFRow) infoSheet.getRow(row.getRowNum() + 1);
                        assertThat(valueRow).isNotNull();
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
