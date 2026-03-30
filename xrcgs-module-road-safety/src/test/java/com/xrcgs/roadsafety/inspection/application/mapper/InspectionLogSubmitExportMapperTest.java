package com.xrcgs.roadsafety.inspection.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class InspectionLogSubmitExportMapperTest {

    private final InspectionLogSubmitExportMapper mapper = new InspectionLogSubmitExportMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseSummaryTextOnlyForHandlingSection() throws Exception {
        String payload = """
                {
                  "date": "2026-03-06",
                  "team": "WUJIAQU",
                  "unitName": "unit",
                  "shiftType": "BOTH",
                  "routes": ["G30"],
                  "vehicle": "A12345",
                  "weather": "SUNNY",
                  "details": [
                    {
                      "category": "ACCIDENT_RESCUE",
                      "type": "ACCIDENT",
                      "time": "10:30",
                      "location": "K150+200",
                      "description": "desc",
                      "result": "done",
                      "summaryText": "summary-1"
                    },
                    {
                      "category": "ACCIDENT_RESCUE",
                      "type": "RESCUE",
                      "time": "11:30",
                      "location": "K151+200",
                      "description": "desc2",
                      "result": "done2",
                      "summaryText": ""
                    }
                  ],
                  "handover": {
                    "inspectors": ["zhangsan"]
                  }
                }
                """;
        JsonNode raw = objectMapper.readTree(payload);
        InspectionLogSubmitExportRequest req = objectMapper.treeToValue(raw, InspectionLogSubmitExportRequest.class);

        CanonicalInspectionExportModel model = mapper.toCanonical(req, raw);

        assertThat(model.getHandlingGroup().getTrafficAccidents()).containsExactly("summary-1");
        assertThat(model.getHandlingGroup().getRoadRescue()).isEmpty();
        assertThat(model.getDetails()).hasSize(2);
    }

    @Test
    void shouldMapCompatibleDetailTypes() throws Exception {
        String payload = """
                {
                  "date": "2026-03-06",
                  "team": "WUJIAQU",
                  "shiftType": "DAY",
                  "routes": ["G30"],
                  "vehicle": "A12345",
                  "weather": "SUNNY",
                  "details": [
                    {"type": "OVERSIZE_CHECK", "summaryText": "a"},
                    {"type": "OVERLIMIT_HANDLING", "summaryText": "b"},
                    {"type": "INFRINGEMENT", "summaryText": "c"}
                  ],
                  "handover": {"inspectors": ["zhangsan"]}
                }
                """;
        JsonNode raw = objectMapper.readTree(payload);
        InspectionLogSubmitExportRequest req = objectMapper.treeToValue(raw, InspectionLogSubmitExportRequest.class);

        CanonicalInspectionExportModel model = mapper.toCanonical(req, raw);

        assertThat(model.getDetails().stream().map(d -> d.getType().name()).toList())
                .isEqualTo(List.of("OVERSIZE", "OVERLOAD", "VIOLATION"));
        assertThat(model.getHandlingGroup().getLargeVehicleChecks()).containsExactly("a");
        assertThat(model.getHandlingGroup().getOverloadVehicleHandling()).containsExactly("b");
        assertThat(model.getHandlingGroup().getIllegalInfringements()).containsExactly("c");
    }

    @Test
    void shouldBuildCleanHandoverSummaryWithoutInternalPrefixes() throws Exception {
        String payload = """
                {
                  "date": "2026-03-06",
                  "team": "WUJIAQU",
                  "shiftType": "DAY",
                  "routes": ["G30"],
                  "vehicle": "A12345",
                  "weather": "SUNNY",
                  "details": [
                    {"type": "OTHER", "summaryText": "ok"}
                  ],
                  "handover": {
                    "inspectors": ["admin"],
                    "handoverFrom": ["admin"],
                    "handoverTo": ["admin"],
                    "note": "交接情况正文",
                    "remark": "补充备注"
                  }
                }
                """;
        JsonNode raw = objectMapper.readTree(payload);
        InspectionLogSubmitExportRequest req = objectMapper.treeToValue(raw, InspectionLogSubmitExportRequest.class);

        CanonicalInspectionExportModel model = mapper.toCanonical(req, raw);

        assertThat(model.getHandoverSummary()).isEqualTo("交接情况正文" + System.lineSeparator() + "注：补充备注");
        assertThat(model.getHandoverSummary()).doesNotContain("inspectors:");
        assertThat(model.getHandoverSummary()).doesNotContain("from:");
        assertThat(model.getHandoverSummary()).doesNotContain("to:");
        assertThat(model.getHandoverSummary()).doesNotContain("remark:");
    }

    @Test
    void shouldKeepRemarkOnlyFromTopLevelRemarkField() throws Exception {
        String payload = """
                {
                  "date": "2026-03-06",
                  "team": "WUJIAQU",
                  "shiftType": "DAY",
                  "routes": ["G30"],
                  "vehicle": "A12345",
                  "weather": "SUNNY",
                  "remark": "主备注内容",
                  "draft": true,
                  "details": [
                    {"type": "OTHER", "summaryText": "ok"}
                  ],
                  "handover": {
                    "inspectors": ["admin"],
                    "remark": "交接备注不应进入A12"
                  }
                }
                """;
        JsonNode raw = objectMapper.readTree(payload);
        InspectionLogSubmitExportRequest req = objectMapper.treeToValue(raw, InspectionLogSubmitExportRequest.class);

        CanonicalInspectionExportModel model = mapper.toCanonical(req, raw);

        assertThat(model.getRemark()).isEqualTo("主备注内容");
        assertThat(model.getRemark()).doesNotContain("handoverRemark:");
        assertThat(model.getRemark()).doesNotContain("draft");
        assertThat(model.getRemark()).doesNotContain("交接备注");
    }
}
