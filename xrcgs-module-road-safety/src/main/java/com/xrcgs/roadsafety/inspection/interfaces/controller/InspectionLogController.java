package com.xrcgs.roadsafety.inspection.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.core.R;
import com.xrcgs.roadsafety.inspection.application.service.InspectionLogApplicationService;
import com.xrcgs.roadsafety.inspection.application.service.InspectionLogQueryService;
import com.xrcgs.roadsafety.inspection.application.service.InspectionLogSubmitExportService;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogDetailVO;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogPageItemVO;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogUpdateSubmitExportRequest;
import com.xrcgs.syslog.annotation.OpLog;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/road-safety/inspection/logs")
@RequiredArgsConstructor
@Validated
public class InspectionLogController {

    private static final Logger log = LoggerFactory.getLogger(InspectionLogController.class);

    private final InspectionLogApplicationService applicationService;
    private final InspectionLogQueryService queryService;
    private final InspectionLogSubmitExportService submitExportService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * 查询日志列表
     * @param recordDate 记录日期
     * @param squadCode 中队范围
     * @param pageNo 当前页
     * @param pageSize 每页条数
     * @return
     */
    @GetMapping("/page")
    public R<Page<InspectionLogPageItemVO>> page(
            @RequestParam(name = "record_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate,
            @RequestParam(name = "squad_code", required = false) String squadCode,
            @RequestParam(defaultValue = "1") @Min(1) long pageNo,
            @RequestParam(defaultValue = "15") @Min(1) long pageSize) {
        return R.ok(queryService.page(recordDate, squadCode, pageNo, pageSize));
    }

    /**
     * 查询巡查日志详情
     * @param id 日志ID
     * @return
     */
    @GetMapping("/detail")
    public R<InspectionLogDetailVO> detail(@RequestParam(name = "id") @Min(1) long id) {
        return R.ok(queryService.detail(id));
    }

    @PostMapping("/export")
    @OpLog("导出巡查日志")
    public void export(@Valid @RequestBody InspectionLogSubmitRequest request,
                       HttpServletResponse response) throws IOException {
        Path exportFile = applicationService.generateInspectionLog(request);
        streamFile(exportFile, response);
    }

    /**
     * 提交巡查日志并导出
     * @param payload 日志内容json格式
     * @param response
     * @throws IOException
     */
    @PostMapping("/submit-export")
    @OpLog("提交导出巡查日志")
    public void submitExport(@RequestBody JsonNode payload, HttpServletResponse response) throws IOException {
        InspectionLogSubmitExportRequest request = objectMapper.treeToValue(payload, InspectionLogSubmitExportRequest.class);
        Set<ConstraintViolation<InspectionLogSubmitExportRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Path exportFile = submitExportService.submitAndExport(request, payload);
        streamFile(exportFile, response);
    }

    /**
     * 修改巡查日志
     * @param payload 日志内容
     * @param response
     * @throws IOException
     */
    @PostMapping("/update")
    @OpLog("修改巡查日志数据")
    public void update(@RequestBody JsonNode payload, HttpServletResponse response) throws IOException {
        InspectionLogUpdateSubmitExportRequest request = objectMapper.treeToValue(payload, InspectionLogUpdateSubmitExportRequest.class);
        Set<ConstraintViolation<InspectionLogUpdateSubmitExportRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Path exportFile = submitExportService.updateAndExportById(request, payload);
        streamFile(exportFile, response);
    }

    private void streamFile(Path exportFile, HttpServletResponse response) throws IOException {
        try (InputStream inputStream = Files.newInputStream(exportFile)) {
            String fileName = exportFile.getFileName().toString();
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName
                    + "\"; filename*=utf-8''" + encodedName);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            inputStream.transferTo(response.getOutputStream());
            response.flushBuffer();
        } catch (IOException ex) {
            log.error("inspection log export failed", ex);
            throw ex;
        }
    }
}
