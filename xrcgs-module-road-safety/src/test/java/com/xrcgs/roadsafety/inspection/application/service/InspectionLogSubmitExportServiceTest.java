package com.xrcgs.roadsafety.inspection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.file.service.SysFileService;
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
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogUpdateSubmitExportRequest;
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
    @Mock
    private SysFileService sysFileService;

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
                authCacheService,
                sysFileService
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

    @Test
    void shouldUpdateExistingRecordById() throws Exception {
        InspectionLogUpdateSubmitExportRequest request = buildUpdateRequest(400L);
        JsonNode rawPayload = objectMapper.readTree("{\"id\":400,\"details\":[{\"type\":\"OTHER\",\"summaryText\":\"new\"}]}");
        CanonicalInspectionExportModel canonical = buildCanonicalWithOneDetailAndPhoto();
        Path exported = tempDir.resolve("submit-export-update.xlsx");
        Files.writeString(exported, "ok");

        InspectionRecord existing = new InspectionRecord();
        existing.setId(400L);
        existing.setCreatedBy("creator");
        existing.setCreatedAt(LocalDate.of(2026, 3, 1).atStartOfDay());

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(recordMapper.selectById(400L)).thenReturn(existing);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(recordMapper.updateById(any(InspectionRecord.class))).thenReturn(1);
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);

        Path result = service.updateAndExportById(request, rawPayload);

        assertThat(result).isEqualTo(exported);
        verify(recordMapper, never()).insert(any(InspectionRecord.class));
        verify(recordMapper).updateById(any(InspectionRecord.class));
        verify(detailMapper).delete(any());
        verify(photoMapper).delete(any());
        verify(detailMapper).insert(any());
        verify(photoMapper).insert(any());
    }

    @Test
    void shouldFailWhenUpdateRecordNotFound() throws Exception {
        InspectionLogUpdateSubmitExportRequest request = buildUpdateRequest(401L);
        JsonNode rawPayload = objectMapper.readTree("{\"id\":401,\"details\":[]}");
        CanonicalInspectionExportModel canonical = buildCanonical();

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(recordMapper.selectById(401L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateAndExportById(request, rawPayload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inspection record not found");

        verify(recordMapper, never()).updateById(any(InspectionRecord.class));
        verify(excelExporter, never()).export(any(InspectionRecord.class), any(Path.class));
    }

    @Test
    void shouldCleanupRemovedFileWhenUpdateRemovesPhoto() throws Exception {
        InspectionLogUpdateSubmitExportRequest request = buildUpdateRequest(402L);
        JsonNode rawPayload = objectMapper.readTree("{\"id\":402,\"details\":[]}");
        CanonicalInspectionExportModel canonical = buildCanonical();
        Path exported = tempDir.resolve("submit-export-update-cleanup.xlsx");
        Files.writeString(exported, "ok");

        InspectionRecord existing = new InspectionRecord();
        existing.setId(402L);
        existing.setCreatedBy("creator");
        existing.setCreatedAt(LocalDate.of(2026, 3, 1).atStartOfDay());

        PhotoItem oldPhoto = PhotoItem.builder()
                .recordId(402L)
                .fileId(999L)
                .build();

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(recordMapper.selectById(402L)).thenReturn(existing);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(photoMapper.selectList(any())).thenReturn(List.of(oldPhoto));
        when(photoMapper.selectCount(any())).thenReturn(0L);
        when(recordMapper.updateById(any(InspectionRecord.class))).thenReturn(1);
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);

        Path result = service.updateAndExportById(request, rawPayload);

        assertThat(result).isEqualTo(exported);
        verify(sysFileService).softDelete(999L, true);
    }

    @Test
    void shouldNotCleanupFileWhenStillReferenced() throws Exception {
        InspectionLogUpdateSubmitExportRequest request = buildUpdateRequest(403L);
        JsonNode rawPayload = objectMapper.readTree("{\"id\":403,\"details\":[]}");
        CanonicalInspectionExportModel canonical = buildCanonical();
        Path exported = tempDir.resolve("submit-export-update-no-cleanup.xlsx");
        Files.writeString(exported, "ok");

        InspectionRecord existing = new InspectionRecord();
        existing.setId(403L);
        existing.setCreatedBy("creator");
        existing.setCreatedAt(LocalDate.of(2026, 3, 1).atStartOfDay());

        PhotoItem oldPhoto = PhotoItem.builder()
                .recordId(403L)
                .fileId(888L)
                .build();

        when(mapper.toCanonical(request, rawPayload)).thenReturn(canonical);
        when(recordMapper.selectById(403L)).thenReturn(existing);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(authCacheService.getCachedDict(any())).thenReturn(null);
        when(dictService.getByTypes(anyList(), isNull())).thenReturn(fullDictMap());
        when(photoMapper.selectList(any())).thenReturn(List.of(oldPhoto));
        when(photoMapper.selectCount(any())).thenReturn(1L);
        when(recordMapper.updateById(any(InspectionRecord.class))).thenReturn(1);
        when(excelExporter.export(any(InspectionRecord.class), any(Path.class))).thenReturn(exported);

        Path result = service.updateAndExportById(request, rawPayload);

        assertThat(result).isEqualTo(exported);
        verify(sysFileService, never()).softDelete(anyLong(), anyBoolean());
    }

    @Test
    void shouldResolveExportFilesByIdsInInputOrder() throws Exception {
        Path firstFile = tempDir.resolve("export-600.xlsx");
        Path secondFile = tempDir.resolve("export-601.xlsx");
        Files.writeString(firstFile, "f1");
        Files.writeString(secondFile, "f2");

        InspectionRecord firstRecord = new InspectionRecord();
        firstRecord.setId(600L);
        firstRecord.setExportFileName("export-600.xlsx");

        InspectionRecord secondRecord = new InspectionRecord();
        secondRecord.setId(601L);
        secondRecord.setExportFileName("export-601.xlsx");

        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());
        when(recordMapper.selectBatchIds(List.of(600L, 601L))).thenReturn(List.of(secondRecord, firstRecord));

        List<InspectionLogSubmitExportService.ExportFileResource> files = service.resolveExportFilesByIds(List.of(600L, 601L));

        assertThat(files).hasSize(2);
        assertThat(files.get(0).recordId()).isEqualTo(600L);
        assertThat(files.get(0).filePath()).isEqualTo(firstFile);
        assertThat(files.get(0).entryName()).startsWith("600_");
        assertThat(files.get(1).recordId()).isEqualTo(601L);
        assertThat(files.get(1).filePath()).isEqualTo(secondFile);
        assertThat(files.get(1).entryName()).startsWith("601_");
    }

    @Test
    void shouldFailResolveExportFilesByIdsWhenRecordMissing() {
        InspectionRecord firstRecord = new InspectionRecord();
        firstRecord.setId(700L);
        firstRecord.setExportFileName("export-700.xlsx");

        when(recordMapper.selectBatchIds(List.of(700L, 701L))).thenReturn(List.of(firstRecord));

        assertThatThrownBy(() -> service.resolveExportFilesByIds(List.of(700L, 701L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inspection record not found")
                .hasMessageContaining("701");
    }

    @Test
    void shouldDeleteRecordsByIdsInBatch() throws Exception {
        Long firstId = 800L;
        Long secondId = 801L;
        Path firstExportFile = tempDir.resolve("delete-800.xlsx");
        Path secondExportFile = tempDir.resolve("delete-801.xlsx");
        Files.writeString(firstExportFile, "to-delete-800");
        Files.writeString(secondExportFile, "to-delete-801");

        InspectionRecord firstRecord = new InspectionRecord();
        firstRecord.setId(firstId);
        firstRecord.setExportFileName("delete-800.xlsx");

        InspectionRecord secondRecord = new InspectionRecord();
        secondRecord.setId(secondId);
        secondRecord.setExportFileName("delete-801.xlsx");

        when(recordMapper.selectById(firstId)).thenReturn(firstRecord);
        when(recordMapper.selectById(secondId)).thenReturn(secondRecord);
        when(photoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(recordMapper.deleteById(firstId)).thenReturn(1);
        when(recordMapper.deleteById(secondId)).thenReturn(1);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());

        service.deleteByIds(List.of(firstId, secondId));

        verify(recordMapper).deleteById(firstId);
        verify(recordMapper).deleteById(secondId);
        assertThat(Files.exists(firstExportFile)).isFalse();
        assertThat(Files.exists(secondExportFile)).isFalse();
    }

    @Test
    void shouldDeleteRecordAndRelatedResourcesById() throws Exception {
        Long recordId = 500L;
        Path exportFile = tempDir.resolve("delete-log.xlsx");
        Files.writeString(exportFile, "to-delete");

        InspectionRecord existing = new InspectionRecord();
        existing.setId(recordId);
        existing.setExportFileName("delete-log.xlsx");

        PhotoItem photoA = PhotoItem.builder().recordId(recordId).fileId(9001L).build();
        PhotoItem photoB = PhotoItem.builder().recordId(recordId).fileId(9002L).build();
        PhotoItem duplicatedPhotoA = PhotoItem.builder().recordId(recordId).fileId(9001L).build();

        when(recordMapper.selectById(recordId)).thenReturn(existing);
        when(photoMapper.selectList(any())).thenReturn(List.of(photoA, photoB, duplicatedPhotoA));
        when(recordMapper.deleteById(recordId)).thenReturn(1);
        when(photoMapper.selectCount(any())).thenReturn(0L, 0L);
        when(sysFileService.softDelete(9001L, true)).thenReturn(true);
        when(sysFileService.softDelete(9002L, true)).thenReturn(true);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());

        service.deleteById(recordId);

        verify(detailMapper).delete(any());
        verify(photoMapper).delete(any());
        verify(recordMapper).deleteById(recordId);
        verify(sysFileService).softDelete(9001L, true);
        verify(sysFileService).softDelete(9002L, true);
        assertThat(Files.exists(exportFile)).isFalse();
    }

    @Test
    void shouldFailDeleteWhenExportFileCannotBeRemoved() throws Exception {
        Long recordId = 501L;
        Path exportPath = tempDir.resolve("delete-broken.xlsx");
        Files.createDirectories(exportPath);
        Files.writeString(exportPath.resolve("child.txt"), "block-delete");

        InspectionRecord existing = new InspectionRecord();
        existing.setId(recordId);
        existing.setExportFileName("delete-broken.xlsx");

        when(recordMapper.selectById(recordId)).thenReturn(existing);
        when(photoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(recordMapper.deleteById(recordId)).thenReturn(1);
        when(logProperties.getInspectionLog()).thenReturn(tempDir.toString());

        assertThatThrownBy(() -> service.deleteById(recordId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delete inspection export file failed");
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

    private InspectionLogUpdateSubmitExportRequest buildUpdateRequest(Long id) {
        InspectionLogUpdateSubmitExportRequest request = new InspectionLogUpdateSubmitExportRequest();
        request.setId(id);
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
