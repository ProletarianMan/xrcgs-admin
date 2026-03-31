package com.xrcgs.roadsafety.inspection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel.CanonicalDetail;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel.CanonicalDetailType;
import com.xrcgs.roadsafety.inspection.application.mapper.InspectionLogSubmitExportMapper;
import com.xrcgs.roadsafety.inspection.config.InspectionLogProperties;
import com.xrcgs.roadsafety.inspection.domain.model.HandlingCategoryGroup;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionHandlingDetailMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionPhotoMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionRecordMapper;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Delivery;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.HandoverInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Mileage;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.MileageInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class InspectionLogSubmitExportServiceTest {

    @Mock
    private InspectionLogSubmitExportMapper mapper;
    @Mock
    private InspectionRecordExcelExporter excelExporter;
    @Mock
    private InspectionLogProperties logProperties;
    @Mock
    private InspectionRecordMapper recordMapper;
    @Mock
    private InspectionHandlingDetailMapper detailMapper;
    @Mock
    private InspectionPhotoMapper photoMapper;
    @Mock
    private DictService dictService;
    @Mock
    private AuthCacheService authCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InspectionLogSubmitExportService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new InspectionLogSubmitExportService(
                mapper,
                excelExporter,
                logProperties,
                recordMapper,
                detailMapper,
                photoMapper,
                objectMapper,
                dictService,
                authCacheService
        );
    }

    @Test
    void shouldTranslateLabelsForExportButKeepValuesForPersistence() throws Exception {
        InspectionLogSubmitExportRequest request = buildRequest();
        JsonNode rawPayload = objectMapper.readTree("{\"details\":[]}");
        CanonicalInspectionExportModel canonical = buildCanonical();
        Path exported = tempDir.resolve("submit-export.xlsx");
        Files.writeString(exported, "ok");

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);
        when(recordMapper.insert(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(100L);
            return 1;
        });

        Path result = service.submitAndExport(request, rawPayload);

        assertThat(result).isEqualTo(exported);

        ArgumentCaptor<InspectionRecord> exportCaptor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(excelExporter).export(exportCaptor.capture(), any(Path.class));
        InspectionRecord exportRecord = exportCaptor.getValue();
        assertThat(exportRecord.getUnitName()).isEqualTo("乌鲁木齐分公司");
        assertThat(exportRecord.getWeather()).isEqualTo("晴");
        assertThat(exportRecord.getPatrolVehicle()).isEqualTo("巡逻车A001");
        assertThat(exportRecord.getRouteDisplay()).isEqualTo("五家渠站、阜康站");
        assertThat(exportRecord.getMileageDisplay())
                .isEqualTo("白班:K10+000–K20+000；夜班:K20+000–K35+500（总计25.500KM）");
        assertThat(exportRecord.getHandoverFromDisplay()).isEqualTo("交班甲、交班乙");
        assertThat(exportRecord.getHandoverToDisplay()).isEqualTo("接班甲、接班乙");
        assertThat(exportRecord.getDeliveryContactDisplay()).isEqualTo("送达2026-001号《工作联系单》\n被送达单位：交警大队");

        ArgumentCaptor<InspectionRecord> persistCaptor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(recordMapper).insert(persistCaptor.capture());
        InspectionRecord persistenceRecord = persistCaptor.getValue();
        assertThat(persistenceRecord.getUnitName()).isEqualTo("U1");
        assertThat(persistenceRecord.getWeather()).isEqualTo("W1");
        assertThat(persistenceRecord.getPatrolVehicle()).isEqualTo("V1");
    }

    @Test
    void shouldAllowSingleShiftMileageWhenOtherShiftIsEmpty() throws Exception {
        InspectionLogSubmitExportRequest request = buildRequest();
        request.getMileage().setNight(new MileageInfo());
        JsonNode rawPayload = objectMapper.readTree("{\"details\":[]}");
        CanonicalInspectionExportModel canonical = buildCanonical();
        Path exported = tempDir.resolve("submit-export-day-only.xlsx");
        Files.writeString(exported, "ok");

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);
        when(recordMapper.insert(any(InspectionRecord.class))).thenAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(101L);
            return 1;
        });

        Path result = service.submitAndExport(request, rawPayload);

        assertThat(result).isEqualTo(exported);
        ArgumentCaptor<InspectionRecord> exportCaptor = ArgumentCaptor.forClass(InspectionRecord.class);
        verify(excelExporter).export(exportCaptor.capture(), any(Path.class));
        InspectionRecord exportRecord = exportCaptor.getValue();
        assertThat(exportRecord.getMileageDisplay()).isEqualTo("白班:K10+000–K20+000（总计10.000KM）");
    }

    @Test
    void shouldFailWhenRouteDictValueMissing() throws Exception {
        InspectionLogSubmitExportRequest request = buildRequest();
        JsonNode rawPayload = objectMapper.readTree("{\"details\":[]}");
        CanonicalInspectionExportModel canonical = buildCanonical();

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(dictMapMissingRoute());

        assertThatThrownBy(() -> service.submitAndExport(request, rawPayload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("巡查路段字典值缺失");

        verify(excelExporter, never()).export(any(InspectionRecord.class), any(Path.class));
        verify(recordMapper, never()).insert(any(InspectionRecord.class));
    }

    @Test
    void shouldOverwriteExistingRecordForSameDateAndTeam() throws Exception {
        InspectionLogSubmitExportRequest request = buildRequest();
        JsonNode rawPayload = objectMapper.readTree("{\"details\":[{\"type\":\"OTHER\",\"summaryText\":\"new\"}]}");
        CanonicalInspectionExportModel canonical = buildCanonicalWithOneDetailAndPhoto();
        Path exported = tempDir.resolve("submit-export-overwrite.xlsx");
        Files.writeString(exported, "ok");

        InspectionRecord existing = new InspectionRecord();
        existing.setId(200L);
        existing.setDate(LocalDate.of(2026, 3, 30));
        existing.setSquadCode("TEAM_01");

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(recordMapper.selectList(any())).thenReturn(List.of(existing));
        when(recordMapper.updateById(any(InspectionRecord.class))).thenReturn(1);
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);

        Path result = service.submitAndExport(request, rawPayload);

        assertThat(result).isEqualTo(exported);
        verify(recordMapper, never()).insert(any(InspectionRecord.class));
        verify(recordMapper).updateById(any(InspectionRecord.class));
        verify(detailMapper).delete(any());
        verify(photoMapper).delete(any());
        verify(detailMapper).insert(any());
        verify(photoMapper).insert(any());
    }

    @Test
    void shouldFallbackToOverwriteWhenConcurrentInsertHitsUniqueConstraint() throws Exception {
        InspectionLogSubmitExportRequest request = buildRequest();
        JsonNode rawPayload = objectMapper.readTree("{\"details\":[{\"type\":\"OTHER\",\"summaryText\":\"new\"}]}");
        CanonicalInspectionExportModel canonical = buildCanonicalWithOneDetailAndPhoto();
        Path exported = tempDir.resolve("submit-export-race.xlsx");
        Files.writeString(exported, "ok");

        InspectionRecord latest = new InspectionRecord();
        latest.setId(300L);
        latest.setDate(LocalDate.of(2026, 3, 30));
        latest.setSquadCode("TEAM_01");

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(recordMapper.selectList(any())).thenReturn(Collections.emptyList(), List.of(latest));
        when(recordMapper.insert(any(InspectionRecord.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(recordMapper.updateById(any(InspectionRecord.class))).thenReturn(1);
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);

        Path result = service.submitAndExport(request, rawPayload);

        assertThat(result).isEqualTo(exported);
        verify(recordMapper).insert(any(InspectionRecord.class));
        verify(recordMapper).updateById(any(InspectionRecord.class));
        verify(detailMapper).delete(any());
        verify(photoMapper).delete(any());
    }

    private InspectionLogSubmitExportRequest buildRequest() {
        InspectionLogSubmitExportRequest request = new InspectionLogSubmitExportRequest();
        request.setDate("2026-03-30");
        request.setTeam("TEAM_01");
        request.setUnitName("U1");
        request.setShiftType(InspectionLogSubmitExportRequest.ShiftType.BOTH);
        request.setRoutes(List.of("S1", "S2"));
        request.setVehicle("V1");
        request.setWeather("W1");
        request.setMileage(buildMileage());
        request.setHandover(buildHandover());
        request.setDeliveries(buildDeliveries());
        request.setDetails(Collections.emptyList());
        request.setDraft(false);
        return request;
    }

    private Mileage buildMileage() {
        Mileage mileage = new Mileage();
        MileageInfo day = new MileageInfo();
        day.setStartStake("K10+000");
        day.setEndStake("K20+000");
        day.setTotalKm(10.0d);
        mileage.setDay(day);

        MileageInfo night = new MileageInfo();
        night.setStartStake("K20+000");
        night.setEndStake("K35+500");
        night.setTotalKm(15.5d);
        mileage.setNight(night);
        return mileage;
    }

    private HandoverInfo buildHandover() {
        HandoverInfo handover = new HandoverInfo();
        handover.setInspectors(List.of("inspectorA"));
        handover.setHandoverFrom(List.of("交班甲", "交班乙"));
        handover.setHandoverTo(List.of("接班甲", "接班乙"));
        return handover;
    }

    private List<Delivery> buildDeliveries() {
        Delivery delivery = new Delivery();
        delivery.setNumber("2026-001");
        delivery.setUnit("交警大队");
        return List.of(delivery);
    }

    private CanonicalInspectionExportModel buildCanonical() {
        return CanonicalInspectionExportModel.builder()
                .date(LocalDate.of(2026, 3, 30))
                .teamCode("TEAM_01")
                .unitName("U1")
                .weather("W1")
                .patrolTeam("inspectorA")
                .patrolVehicle("V1")
                .location("S1、S2；day:K10+000-K20+000;night:K20+000-K35+500")
                .inspectionContent("inspection content")
                .issuesFound("issues")
                .handlingSituationRaw("handling")
                .handlingGroup(new HandlingCategoryGroup())
                .handoverSummary("handover")
                .remark("remark")
                .exportFileName("submit-export.xlsx")
                .draft(false)
                .photos(Collections.emptyList())
                .details(Collections.emptyList())
                .build();
    }

    private CanonicalInspectionExportModel buildCanonicalWithOneDetailAndPhoto() {
        ObjectNode rawDetail = objectMapper.createObjectNode();
        rawDetail.put("type", "OTHER");
        rawDetail.put("summaryText", "new");

        CanonicalDetail detail = CanonicalDetail.builder()
                .categoryCode("OTHER")
                .categoryName("OTHER")
                .type(CanonicalDetailType.OTHER)
                .summaryText("new")
                .rawPayload(rawDetail)
                .detailOrder(1)
                .build();

        PhotoItem photo = PhotoItem.builder()
                .fileId(123L)
                .description("p")
                .sortOrder(1)
                .build();

        return CanonicalInspectionExportModel.builder()
                .date(LocalDate.of(2026, 3, 30))
                .teamCode("TEAM_01")
                .unitName("U1")
                .weather("W1")
                .patrolTeam("inspectorA")
                .patrolVehicle("V1")
                .location("S1")
                .inspectionContent("inspection content")
                .issuesFound("issues")
                .handlingSituationRaw("handling")
                .handlingGroup(new HandlingCategoryGroup())
                .handoverSummary("handover")
                .remark("remark")
                .exportFileName("submit-export.xlsx")
                .draft(false)
                .photos(List.of(photo))
                .details(List.of(detail))
                .build();
    }

    private Map<String, DictVO> fullDictMap() {
        Map<String, DictVO> map = new LinkedHashMap<>();
        map.put("unitNames", dict("unitNames", Map.of("U1", "乌鲁木齐分公司")));
        map.put("weathers", dict("weathers", Map.of("W1", "晴")));
        map.put("officialVehicles", dict("officialVehicles", Map.of("V1", "巡逻车A001")));
        map.put("allSite", dict("allSite", Map.of("S1", "五家渠站", "S2", "阜康站")));
        return map;
    }

    private Map<String, DictVO> dictMapMissingRoute() {
        Map<String, DictVO> map = new LinkedHashMap<>();
        map.put("unitNames", dict("unitNames", Map.of("U1", "乌鲁木齐分公司")));
        map.put("weathers", dict("weathers", Map.of("W1", "晴")));
        map.put("officialVehicles", dict("officialVehicles", Map.of("V1", "巡逻车A001")));
        map.put("allSite", dict("allSite", Map.of("S1", "五家渠站")));
        return map;
    }

    private DictVO dict(String typeCode, Map<String, String> entries) {
        DictVO vo = new DictVO();
        vo.setType(typeCode);
        List<DictVO.Item> items = entries.entrySet().stream().map(entry -> {
            DictVO.Item item = new DictVO.Item();
            item.setValue(entry.getKey());
            item.setLabel(entry.getValue());
            return item;
        }).toList();
        vo.setItems(items);
        return vo;
    }
}
