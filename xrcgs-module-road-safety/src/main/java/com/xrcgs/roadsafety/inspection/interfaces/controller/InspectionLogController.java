package com.xrcgs.roadsafety.inspection.interfaces.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.roadsafety.inspection.application.service.InspectionLogApplicationService;
import com.xrcgs.roadsafety.inspection.application.service.InspectionLogSubmitExportService;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest;
import com.xrcgs.syslog.annotation.OpLog;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/road-safety/inspection/logs")
@RequiredArgsConstructor
@Validated
public class InspectionLogController {

    private static final Logger log = LoggerFactory.getLogger(InspectionLogController.class);

    private final InspectionLogApplicationService applicationService;
    private final InspectionLogSubmitExportService submitExportService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping("/export")
    @OpLog("导出巡查日志")
    public void export(@Valid @RequestBody InspectionLogSubmitRequest request,
                       HttpServletResponse response) throws IOException {
        Path exportFile = applicationService.generateInspectionLog(request);
        streamFile(exportFile, response);
    }

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
