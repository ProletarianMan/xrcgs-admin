package com.xrcgs.roadsafety.inspection.interfaces.controller;

import com.xrcgs.roadsafety.inspection.application.service.InspectionLogApplicationService;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 巡查日志导出接口，接收前端提交的巡查数据并返回生成的 Excel 文件。
 */
@RestController
@RequestMapping("/api/road-safety/inspection/logs")
@RequiredArgsConstructor
@Validated
public class InspectionLogController {

    private static final Logger log = LoggerFactory.getLogger(InspectionLogController.class);

    private final InspectionLogApplicationService applicationService;

    /**
     * 生成巡查日志 Excel 文件并以附件形式返回。
     *
     * @param request  巡查日志提交数据
     * @param response HTTP 响应对象
     */
    @PostMapping("/export")
    public void export(@Valid @RequestBody InspectionLogSubmitRequest request,
                       HttpServletResponse response) throws IOException {
        Path exportFile = applicationService.generateInspectionLog(request);
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
            log.error("巡查日志导出失败", ex);
            throw ex;
        }
    }
}
