package com.xrcgs.roadsafety.inspection.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.common.enums.ApprovalStatus;
import com.xrcgs.file.service.SysFileService;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel;
import com.xrcgs.roadsafety.inspection.application.dto.CanonicalInspectionExportModel.CanonicalDetail;
import com.xrcgs.roadsafety.inspection.application.mapper.InspectionLogSubmitExportMapper;
import com.xrcgs.roadsafety.inspection.config.InspectionLogProperties;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionHandlingDetail;
import com.xrcgs.roadsafety.inspection.domain.model.InspectionRecord;
import com.xrcgs.roadsafety.inspection.domain.model.PhotoItem;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionHandlingDetailMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionPhotoMapper;
import com.xrcgs.roadsafety.inspection.infrastructure.mapper.InspectionRecordMapper;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Delivery;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.Mileage;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogSubmitExportRequest.MileageInfo;
import com.xrcgs.roadsafety.inspection.interfaces.dto.InspectionLogUpdateSubmitExportRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InspectionLogSubmitExportService {

    private static final Logger log = LoggerFactory.getLogger(InspectionLogSubmitExportService.class);
    private static final String DEFAULT_EXPORT_FILE_NAME = "inspection_record_output.xlsx";

    private static final String DICT_UNIT_NAMES = "unitNames";
    private static final String DICT_WEATHERS = "weathers";
    private static final String DICT_OFFICIAL_VEHICLES = "officialVehicles";
    private static final String DICT_ALL_SITE = "allSite";
    private static final List<String> REQUIRED_EXPORT_DICT_TYPES = List.of(
            DICT_UNIT_NAMES,
            DICT_WEATHERS,
            DICT_OFFICIAL_VEHICLES,
            DICT_ALL_SITE
    );
    private static final String INVALID_FILE_NAME_PATTERN = "[\\\\/:*?\"<>|]";
    private static final Pattern FILE_ID_URL_PATTERN = Pattern.compile("/api/file/(?:preview|download)/(\\d+)");

    private final InspectionLogSubmitExportMapper mapper;
    private final InspectionRecordExcelExporter excelExporter;
    private final InspectionLogProperties logProperties;
    private final InspectionRecordMapper recordMapper;
    private final InspectionHandlingDetailMapper detailMapper;
    private final InspectionPhotoMapper photoMapper;
    private final ObjectMapper objectMapper;
    private final DictService dictService;
    private final AuthCacheService authCacheService;
    private final SysFileService sysFileService;

    public record ExportFileResource(Long recordId, String entryName, Path filePath) {
    }

    @Transactional(rollbackFor = Exception.class)
    public Path submitAndExport(InspectionLogSubmitExportRequest request, JsonNode rawPayload) throws IOException {
        CanonicalInspectionExportModel canonical = mapper.toCanonical(request, rawPayload);
        LocalDateTime now = LocalDateTime.now();
        InspectionRecord existingRecord = findLatestByDateAndSquad(canonical.getDate(), canonical.getTeamCode());
        Set<Long> retainedFileIds = collectFileIds(canonical.getPhotos());
        Set<Long> removedFileIds = Collections.emptySet();
        InspectionRecord persistenceRecord = InspectionRecord.builder()
                .id(existingRecord == null ? null : existingRecord.getId())
                .date(canonical.getDate())
                .unitName(canonical.getUnitName())
                .weather(canonical.getWeather())
                .patrolTeam(canonical.getPatrolTeam())
                .patrolVehicle(canonical.getPatrolVehicle())
                .location(canonical.getLocation())
                .inspectionContent(canonical.getInspectionContent())
                .issuesFound(canonical.getIssuesFound())
                .handlingSituationRaw(canonical.getHandlingSituationRaw())
                .handlingDetails(canonical.getHandlingGroup())
                .photos(Optional.ofNullable(canonical.getPhotos()).orElse(Collections.emptyList()))
                .handoverSummary(canonical.getHandoverSummary())
                .remark(canonical.getRemark())
                .createdBy(resolveCreatedBy(existingRecord, canonical.getPatrolTeam()))
                .createdAt(resolveCreatedAt(existingRecord, now))
                .updatedAt(now)
                .exportedBy(canonical.getPatrolTeam())
                .exportedAt(now)
                .exportFileName(canonical.getExportFileName())
                .approvalStatus(ApprovalStatus.UNSUBMITTED)
                .squadCode(canonical.getTeamCode())
                .formPayloadJson(writeJson(rawPayload))
                .detailsPayloadJson(writeJson(rawPayload == null ? null : rawPayload.get("details")))
                .summaryPayloadJson(writeJson(canonical.getSummaryPayload()))
                .build();
        Map<String, Map<String, String>> dictLabelMappings = resolveDictLabelMappings();
        InspectionRecord exportRecord = buildExportRecord(canonical, persistenceRecord, request, dictLabelMappings);

        Path storageDirectory = resolveStorageDirectory();
        Path exported = excelExporter.export(exportRecord, storageDirectory);
        if (!Files.exists(exported)) {
            throw new IOException("检查导出文件未生成!");
        }

        if (existingRecord == null) {
            try {
                recordMapper.insert(persistenceRecord);
            } catch (DuplicateKeyException ex) {
                InspectionRecord latestRecord = findLatestByDateAndSquad(canonical.getDate(), canonical.getTeamCode());
                if (latestRecord == null || latestRecord.getId() == null) {
                    throw ex;
                }
                persistenceRecord.setId(latestRecord.getId());
                persistenceRecord.setCreatedBy(resolveCreatedBy(latestRecord, canonical.getPatrolTeam()));
                persistenceRecord.setCreatedAt(resolveCreatedAt(latestRecord, now));
                removedFileIds = resolveRemovedFileIds(persistenceRecord.getId(), retainedFileIds);
                overwriteExistingRecord(persistenceRecord);
            }
        } else {
            removedFileIds = resolveRemovedFileIds(persistenceRecord.getId(), retainedFileIds);
            overwriteExistingRecord(persistenceRecord);
        }
        saveDetailRows(persistenceRecord.getId(), canonical.getDetails(), now);
        savePhotoRows(persistenceRecord.getId(), canonical.getPhotos(), now);
        scheduleCleanupRemovedFiles(removedFileIds);
        return exported;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long submitWithoutExport(InspectionLogSubmitExportRequest request, JsonNode rawPayload, String storedFileName) {
        CanonicalInspectionExportModel canonical = mapper.toCanonical(request, rawPayload);
        LocalDateTime now = LocalDateTime.now();
        InspectionRecord existingRecord = findLatestByDateAndSquad(canonical.getDate(), canonical.getTeamCode());
        Set<Long> retainedFileIds = collectFileIds(canonical.getPhotos());
        Set<Long> removedFileIds = Collections.emptySet();

        String boundFileName = normalizeBoundFileName(storedFileName, canonical.getExportFileName());
        InspectionRecord persistenceRecord = InspectionRecord.builder()
                .id(existingRecord == null ? null : existingRecord.getId())
                .date(canonical.getDate())
                .unitName(canonical.getUnitName())
                .weather(canonical.getWeather())
                .patrolTeam(canonical.getPatrolTeam())
                .patrolVehicle(canonical.getPatrolVehicle())
                .location(canonical.getLocation())
                .inspectionContent(canonical.getInspectionContent())
                .issuesFound(canonical.getIssuesFound())
                .handlingSituationRaw(canonical.getHandlingSituationRaw())
                .handlingDetails(canonical.getHandlingGroup())
                .photos(Optional.ofNullable(canonical.getPhotos()).orElse(Collections.emptyList()))
                .handoverSummary(canonical.getHandoverSummary())
                .remark(canonical.getRemark())
                .createdBy(resolveCreatedBy(existingRecord, canonical.getPatrolTeam()))
                .createdAt(resolveCreatedAt(existingRecord, now))
                .updatedAt(now)
                .exportedBy(canonical.getPatrolTeam())
                .exportedAt(now)
                .exportFileName(boundFileName)
                .approvalStatus(ApprovalStatus.UNSUBMITTED)
                .squadCode(canonical.getTeamCode())
                .formPayloadJson(writeJson(rawPayload))
                .detailsPayloadJson(writeJson(rawPayload == null ? null : rawPayload.get("details")))
                .summaryPayloadJson(writeJson(canonical.getSummaryPayload()))
                .build();

        if (existingRecord == null) {
            try {
                recordMapper.insert(persistenceRecord);
            } catch (DuplicateKeyException ex) {
                InspectionRecord latestRecord = findLatestByDateAndSquad(canonical.getDate(), canonical.getTeamCode());
                if (latestRecord == null || latestRecord.getId() == null) {
                    throw ex;
                }
                persistenceRecord.setId(latestRecord.getId());
                persistenceRecord.setCreatedBy(resolveCreatedBy(latestRecord, canonical.getPatrolTeam()));
                persistenceRecord.setCreatedAt(resolveCreatedAt(latestRecord, now));
                removedFileIds = resolveRemovedFileIds(persistenceRecord.getId(), retainedFileIds);
                overwriteExistingRecord(persistenceRecord);
            }
        } else {
            removedFileIds = resolveRemovedFileIds(persistenceRecord.getId(), retainedFileIds);
            overwriteExistingRecord(persistenceRecord);
        }

        saveDetailRows(persistenceRecord.getId(), canonical.getDetails(), now);
        savePhotoRows(persistenceRecord.getId(), canonical.getPhotos(), now);
        scheduleCleanupRemovedFiles(removedFileIds);
        return persistenceRecord.getId();
    }

    public Path storeUploadedOriginalFile(MultipartFile file, String preferredFileName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("uploaded file is empty");
        }
        Path storageDirectory = resolveStorageDirectory();
        String fileName = buildStoredOriginalFileName(file.getOriginalFilename(), preferredFileName);
        Path target = resolveUniqueFilePath(storageDirectory, fileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    @Transactional(rollbackFor = Exception.class)
    public Path updateAndExportById(InspectionLogUpdateSubmitExportRequest request, JsonNode rawPayload) throws IOException {
        CanonicalInspectionExportModel canonical = mapper.toCanonical(request, rawPayload);
        LocalDateTime now = LocalDateTime.now();
        InspectionRecord existingRecord = findById(request.getId());
        if (existingRecord == null || existingRecord.getId() == null) {
            throw new IllegalArgumentException("inspection record not found, id=" + request.getId());
        }
        Set<Long> retainedFileIds = collectFileIds(canonical.getPhotos());
        Set<Long> removedFileIds = resolveRemovedFileIds(existingRecord.getId(), retainedFileIds);

        InspectionRecord persistenceRecord = InspectionRecord.builder()
                .id(existingRecord.getId())
                .date(canonical.getDate())
                .unitName(canonical.getUnitName())
                .weather(canonical.getWeather())
                .patrolTeam(canonical.getPatrolTeam())
                .patrolVehicle(canonical.getPatrolVehicle())
                .location(canonical.getLocation())
                .inspectionContent(canonical.getInspectionContent())
                .issuesFound(canonical.getIssuesFound())
                .handlingSituationRaw(canonical.getHandlingSituationRaw())
                .handlingDetails(canonical.getHandlingGroup())
                .photos(Optional.ofNullable(canonical.getPhotos()).orElse(Collections.emptyList()))
                .handoverSummary(canonical.getHandoverSummary())
                .remark(canonical.getRemark())
                .createdBy(resolveCreatedBy(existingRecord, canonical.getPatrolTeam()))
                .createdAt(resolveCreatedAt(existingRecord, now))
                .updatedAt(now)
                .exportedBy(canonical.getPatrolTeam())
                .exportedAt(now)
                .exportFileName(canonical.getExportFileName())
                .approvalStatus(ApprovalStatus.UNSUBMITTED)
                .squadCode(canonical.getTeamCode())
                .formPayloadJson(writeJson(rawPayload))
                .detailsPayloadJson(writeJson(rawPayload == null ? null : rawPayload.get("details")))
                .summaryPayloadJson(writeJson(canonical.getSummaryPayload()))
                .build();
        Map<String, Map<String, String>> dictLabelMappings = resolveDictLabelMappings();
        InspectionRecord exportRecord = buildExportRecord(canonical, persistenceRecord, request, dictLabelMappings);

        Path storageDirectory = resolveStorageDirectory();
        Path exported = excelExporter.export(exportRecord, storageDirectory);
        if (!Files.exists(exported)) {
            throw new IOException("妫€鏌ュ鍑烘枃浠舵湭鐢熸垚!");
        }

        overwriteExistingRecord(persistenceRecord);
        saveDetailRows(persistenceRecord.getId(), canonical.getDetails(), now);
        savePhotoRows(persistenceRecord.getId(), canonical.getPhotos(), now);
        scheduleCleanupRemovedFiles(removedFileIds);
        return exported;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        deleteOneById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : normalizeRecordIds(ids)) {
            deleteOneById(id);
        }
    }

    public List<ExportFileResource> resolveExportFilesByIds(List<Long> ids) {
        List<Long> normalizedIds = normalizeRecordIds(ids);
        List<InspectionRecord> records = recordMapper.selectBatchIds(normalizedIds);
        Map<Long, InspectionRecord> recordById = new LinkedHashMap<>();
        if (records != null) {
            for (InspectionRecord record : records) {
                if (record == null || record.getId() == null) {
                    continue;
                }
                recordById.put(record.getId(), record);
            }
        }
        List<Long> missingIds = new ArrayList<>();
        for (Long id : normalizedIds) {
            if (!recordById.containsKey(id)) {
                missingIds.add(id);
            }
        }
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("inspection record not found, ids=" + missingIds);
        }

        List<ExportFileResource> files = new ArrayList<>();
        Set<String> usedEntryNames = new LinkedHashSet<>();
        for (Long id : normalizedIds) {
            InspectionRecord record = recordById.get(id);
            Path exportPath = resolveExportFilePath(record);
            if (!Files.exists(exportPath)) {
                throw new IllegalStateException("inspection export file not found, id=" + id + ", path=" + exportPath);
            }
            String entryName = buildZipEntryName(id, exportPath.getFileName().toString(), usedEntryNames);
            files.add(new ExportFileResource(id, entryName, exportPath));
        }
        return files;
    }

    @Transactional(readOnly = true)
    public Long resolveLatestRecordId(LocalDate date, String squadCode) {
        InspectionRecord record = findLatestByDateAndSquad(date, squadCode);
        return record == null ? null : record.getId();
    }

    private void deleteOneById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("inspection record id is invalid: " + id);
        }
        InspectionRecord existingRecord = findById(id);
        if (existingRecord == null || existingRecord.getId() == null) {
            throw new IllegalArgumentException("inspection record not found, id=" + id);
        }

        Set<Long> relatedFileIds = collectRelatedFileIds(id);
        relatedFileIds.addAll(collectRelatedFileIdsFromPayload(existingRecord));
        deleteDetailRows(id);
        deletePhotoRows(id);
        int deletedRows = recordMapper.deleteById(id);
        if (deletedRows <= 0) {
            throw new IllegalStateException("delete inspection record failed, id=" + id);
        }

        deleteRelatedPhotoFiles(relatedFileIds);
        deleteExportFile(existingRecord);
    }

    private List<Long> normalizeRecordIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("inspection record ids are required");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("inspection record id is invalid: " + id);
            }
            uniqueIds.add(id);
        }
        if (uniqueIds.isEmpty()) {
            throw new IllegalArgumentException("inspection record ids are required");
        }
        return new ArrayList<>(uniqueIds);
    }

    private String buildZipEntryName(Long recordId, String originalFileName, Set<String> usedEntryNames) {
        String normalizedFileName = normalizeExportFileName(originalFileName).replaceAll(INVALID_FILE_NAME_PATTERN, "_");
        if (!StringUtils.hasText(normalizedFileName)) {
            normalizedFileName = DEFAULT_EXPORT_FILE_NAME;
        }
        String candidate = recordId + "_" + normalizedFileName;
        String candidateKey = candidate.toLowerCase(Locale.ROOT);
        int suffix = 1;
        while (usedEntryNames.contains(candidateKey)) {
            candidate = recordId + "_" + suffix + "_" + normalizedFileName;
            candidateKey = candidate.toLowerCase(Locale.ROOT);
            suffix++;
        }
        usedEntryNames.add(candidateKey);
        return candidate;
    }

    private Set<Long> collectFileIds(List<PhotoItem> photos) {
        if (photos == null || photos.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (PhotoItem photo : photos) {
            if (photo == null || photo.getFileId() == null) {
                continue;
            }
            ids.add(photo.getFileId());
        }
        return ids;
    }

    private Set<Long> collectRelatedFileIds(Long recordId) {
        if (recordId == null) {
            return Collections.emptySet();
        }
        List<PhotoItem> photos = photoMapper.selectList(Wrappers.lambdaQuery(PhotoItem.class)
                .eq(PhotoItem::getRecordId, recordId));
        return collectFileIds(photos);
    }

    private Set<Long> collectRelatedFileIdsFromPayload(InspectionRecord record) {
        if (record == null) {
            return Collections.emptySet();
        }
        Set<Long> fileIds = new LinkedHashSet<>();
        collectFileIdsFromPayload(record.getFormPayloadJson(), fileIds);
        collectFileIdsFromPayload(record.getDetailsPayloadJson(), fileIds);
        return fileIds;
    }

    private void collectFileIdsFromPayload(String payload, Set<Long> output) {
        if (!StringUtils.hasText(payload) || output == null) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            collectFileIdsFromJsonNode(root, output);
        } catch (Exception ex) {
            log.debug("parse inspection payload for file ids failed", ex);
            collectFileIdsByUrlPattern(payload, output);
        }
    }

    private void collectFileIdsFromJsonNode(JsonNode node, Set<Long> output) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectFileIdsFromJsonNode(child, output);
            }
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode value = field.getValue();
                if (value == null || value.isNull()) {
                    continue;
                }
                if (("fileId".equalsIgnoreCase(fieldName) || "file_id".equalsIgnoreCase(fieldName))
                        && value.canConvertToLong()) {
                    Long fileId = value.longValue();
                    if (fileId != null && fileId > 0) {
                        output.add(fileId);
                    }
                    continue;
                }
                if (value.isTextual()) {
                    collectFileIdsByUrlPattern(value.asText(), output);
                    continue;
                }
                collectFileIdsFromJsonNode(value, output);
            }
            return;
        }
        if (node.isTextual()) {
            collectFileIdsByUrlPattern(node.asText(), output);
        }
    }

    private void collectFileIdsByUrlPattern(String text, Set<Long> output) {
        if (!StringUtils.hasText(text) || output == null) {
            return;
        }
        Matcher matcher = FILE_ID_URL_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                Long fileId = Long.valueOf(matcher.group(1));
                if (fileId > 0) {
                    output.add(fileId);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid file id segments
            }
        }
    }

    private void deleteRelatedPhotoFiles(Set<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            if (fileId == null || fileId <= 0) {
                continue;
            }
            Long refCount = photoMapper.selectCount(Wrappers.lambdaQuery(PhotoItem.class)
                    .eq(PhotoItem::getFileId, fileId));
            if (refCount != null && refCount > 0) {
                continue;
            }
            try {
                boolean deleted = sysFileService.softDelete(fileId, true);
                if (!deleted) {
                    throw new IllegalStateException("delete inspection photo failed, fileId=" + fileId);
                }
                boolean removed = sysFileService.removeById(fileId);
                if (!removed) {
                    log.warn("delete sys_file row skipped or already removed, fileId={}", fileId);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("delete inspection photo failed, fileId=" + fileId, ex);
            }
        }
    }

    private void deleteExportFile(InspectionRecord record) {
        Path exportPath = resolveExportFilePath(record);
        if (!Files.exists(exportPath)) {
            return;
        }
        try {
            Files.delete(exportPath);
        } catch (IOException ex) {
            throw new IllegalStateException("delete inspection export file failed: " + exportPath, ex);
        }
        if (Files.exists(exportPath)) {
            throw new IllegalStateException("inspection export file still exists after delete: " + exportPath);
        }
    }

    private Path resolveExportFilePath(InspectionRecord record) {
        Path storageDirectory;
        try {
            storageDirectory = resolveStorageDirectory();
        } catch (IOException ex) {
            throw new IllegalStateException("inspection export storage directory is unavailable", ex);
        }
        String exportFileName = normalizeExportFileName(record == null ? null : record.getExportFileName());
        Path exportPath = storageDirectory.resolve(exportFileName).toAbsolutePath().normalize();
        if (!exportPath.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("inspection export file name is invalid: " + exportFileName);
        }
        return exportPath;
    }

    private String normalizeExportFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return DEFAULT_EXPORT_FILE_NAME;
        }
        String normalized = fileName.trim().replaceAll(INVALID_FILE_NAME_PATTERN, "_");
        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_EXPORT_FILE_NAME;
        }
        if (normalized.lastIndexOf('.') <= 0 || normalized.endsWith(".")) {
            return normalized + ".xlsx";
        }
        return normalized;
    }

    private String normalizeBoundFileName(String storedFileName, String fallbackName) {
        if (StringUtils.hasText(storedFileName)) {
            return normalizeExportFileName(storedFileName);
        }
        return normalizeExportFileName(fallbackName);
    }

    private String buildStoredOriginalFileName(String originalName, String preferredFileName) {
        String candidate = StringUtils.hasText(originalName) ? originalName.trim() : trimToNull(preferredFileName);
        if (!StringUtils.hasText(candidate)) {
            candidate = "inspection_import_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        }
        String normalized = candidate.replaceAll(INVALID_FILE_NAME_PATTERN, "_");
        if (!StringUtils.hasText(normalized)) {
            return DEFAULT_EXPORT_FILE_NAME;
        }
        if (normalized.lastIndexOf('.') <= 0 || normalized.endsWith(".")) {
            return normalized + ".xlsx";
        }
        return normalized;
    }

    private Path resolveUniqueFilePath(Path directory, String fileName) throws IOException {
        Path normalizedDir = directory.toAbsolutePath().normalize();
        String baseName = normalizeExportFileName(fileName);
        String stem = baseName;
        String ext = "";
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < baseName.length() - 1) {
            stem = baseName.substring(0, dotIndex);
            ext = baseName.substring(dotIndex);
        }
        int suffix = 0;
        while (true) {
            String candidate = suffix == 0 ? stem + ext : stem + "_" + suffix + ext;
            Path target = normalizedDir.resolve(candidate).toAbsolutePath().normalize();
            if (!target.startsWith(normalizedDir)) {
                throw new IOException("stored file path is invalid: " + candidate);
            }
            if (!Files.exists(target)) {
                return target;
            }
            suffix++;
        }
    }

    private Set<Long> resolveRemovedFileIds(Long recordId, Set<Long> retainedFileIds) {
        if (recordId == null) {
            return Collections.emptySet();
        }
        List<PhotoItem> existingPhotos = photoMapper.selectList(Wrappers.lambdaQuery(PhotoItem.class)
                .eq(PhotoItem::getRecordId, recordId));
        if (existingPhotos == null || existingPhotos.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> removed = new LinkedHashSet<>();
        for (PhotoItem existingPhoto : existingPhotos) {
            if (existingPhoto == null || existingPhoto.getFileId() == null) {
                continue;
            }
            if (retainedFileIds == null || !retainedFileIds.contains(existingPhoto.getFileId())) {
                removed.add(existingPhoto.getFileId());
            }
        }
        return removed;
    }

    private void scheduleCleanupRemovedFiles(Set<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Set<Long> ids = new LinkedHashSet<>(fileIds);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupRemovedFiles(ids);
                }
            });
            return;
        }
        cleanupRemovedFiles(ids);
    }

    private void cleanupRemovedFiles(Set<Long> fileIds) {
        for (Long fileId : fileIds) {
            if (fileId == null || fileId <= 0) {
                continue;
            }
            Long refCount = photoMapper.selectCount(Wrappers.lambdaQuery(PhotoItem.class)
                    .eq(PhotoItem::getFileId, fileId));
            if (refCount != null && refCount > 0) {
                continue;
            }
            try {
                boolean deleted = sysFileService.softDelete(fileId, true);
                if (deleted) {
                    sysFileService.removeById(fileId);
                }
            } catch (Exception ex) {
                log.warn("cleanup inspection file failed, fileId={}", fileId, ex);
            }
        }
    }

    private void overwriteExistingRecord(InspectionRecord persistenceRecord) {
        int updated = recordMapper.updateById(persistenceRecord);
        if (updated <= 0) {
            throw new IllegalStateException("inspection record overwrite failed: no row updated");
        }
        deleteDetailRows(persistenceRecord.getId());
        deletePhotoRows(persistenceRecord.getId());
    }

    private InspectionRecord findLatestByDateAndSquad(LocalDate date, String squadCode) {
        if (date == null || !StringUtils.hasText(squadCode)) {
            return null;
        }
        LambdaQueryWrapper<InspectionRecord> query = new LambdaQueryWrapper<InspectionRecord>()
                .eq(InspectionRecord::getDate, date)
                .eq(InspectionRecord::getSquadCode, squadCode.trim())
                .orderByDesc(InspectionRecord::getUpdatedAt)
                .orderByDesc(InspectionRecord::getId)
                .last("LIMIT 1");
        List<InspectionRecord> records = recordMapper.selectList(query);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private InspectionRecord findById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        return recordMapper.selectById(id);
    }

    private String resolveCreatedBy(InspectionRecord existingRecord, String fallbackCreatedBy) {
        if (existingRecord != null && StringUtils.hasText(existingRecord.getCreatedBy())) {
            return existingRecord.getCreatedBy();
        }
        return fallbackCreatedBy;
    }

    private LocalDateTime resolveCreatedAt(InspectionRecord existingRecord, LocalDateTime now) {
        if (existingRecord != null && existingRecord.getCreatedAt() != null) {
            return existingRecord.getCreatedAt();
        }
        return now;
    }

    private InspectionRecord buildExportRecord(CanonicalInspectionExportModel canonical,
                                               InspectionRecord persistenceRecord,
                                               InspectionLogSubmitExportRequest request,
                                               Map<String, Map<String, String>> dictLabelMappings) {
        return InspectionRecord.builder()
                .date(canonical.getDate())
                .unitName(resolveOptionalLabel(dictLabelMappings, DICT_UNIT_NAMES, request.getUnitName(), "单位"))
                .weather(resolveRequiredLabel(dictLabelMappings, DICT_WEATHERS, request.getWeather(), "天气"))
                .patrolTeam(canonical.getPatrolTeam())
                .patrolVehicle(resolveRequiredLabel(dictLabelMappings, DICT_OFFICIAL_VEHICLES, request.getVehicle(), "巡查车辆"))
                .location(joinLocationDisplay(
                        resolveRouteDisplay(dictLabelMappings, request.getRoutes()),
                        buildMileageDisplay(request.getMileage()),
                        canonical.getLocation()))
                .routeDisplay(resolveRouteDisplay(dictLabelMappings, request.getRoutes()))
                .mileageDisplay(buildMileageDisplay(request.getMileage()))
                .handoverFromDisplay(buildPeopleDisplay(Optional.ofNullable(request.getHandover())
                        .map(InspectionLogSubmitExportRequest.HandoverInfo::getHandoverFrom)
                        .orElse(Collections.emptyList())))
                .handoverToDisplay(buildPeopleDisplay(Optional.ofNullable(request.getHandover())
                        .map(InspectionLogSubmitExportRequest.HandoverInfo::getHandoverTo)
                        .orElse(Collections.emptyList())))
                .deliveryContactDisplay(buildDeliveryContactDisplay(request.getDeliveries()))
                .inspectionContent(canonical.getInspectionContent())
                .issuesFound(canonical.getIssuesFound())
                .handlingSituationRaw(canonical.getHandlingSituationRaw())
                .handlingDetails(canonical.getHandlingGroup())
                .photos(Optional.ofNullable(canonical.getPhotos()).orElse(Collections.emptyList()))
                .handoverSummary(canonical.getHandoverSummary())
                .remark(canonical.getRemark())
                .createdBy(persistenceRecord.getCreatedBy())
                .createdAt(persistenceRecord.getCreatedAt())
                .updatedAt(persistenceRecord.getUpdatedAt())
                .exportedBy(persistenceRecord.getExportedBy())
                .exportedAt(persistenceRecord.getExportedAt())
                .exportFileName(canonical.getExportFileName())
                .build();
    }

    /*
    private String joinLocationDisplay(String routeDisplay, String mileageDisplay, String fallbackLocation) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(routeDisplay)) {
            parts.add(routeDisplay.trim());
        }
        if (StringUtils.hasText(mileageDisplay)) {
            parts.add(mileageDisplay.trim());
        }
        if (!parts.isEmpty()) {
            return String.join("，", parts);
        }
        return fallbackLocation;
    }
    */

    private String joinLocationDisplay(String routeDisplay, String mileageDisplay, String fallbackLocation) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(routeDisplay)) {
            parts.add(routeDisplay.trim());
        }
        if (StringUtils.hasText(mileageDisplay)) {
            parts.add(mileageDisplay.trim());
        }
        if (!parts.isEmpty()) {
            return String.join("\uFF0C", parts);
        }
        return fallbackLocation;
    }

    private Map<String, Map<String, String>> resolveDictLabelMappings() {
        Map<String, DictVO> dictByType = new LinkedHashMap<>();
        List<String> missingFromCache = new ArrayList<>();
        for (String typeCode : REQUIRED_EXPORT_DICT_TYPES) {
            DictVO cached = loadDictFromCache(typeCode);
            if (cached != null) {
                dictByType.put(typeCode, cached);
                continue;
            }
            missingFromCache.add(typeCode);
        }

        if (!missingFromCache.isEmpty()) {
            Map<String, DictVO> loaded = dictService.getByTypes(missingFromCache, null);
            for (String typeCode : missingFromCache) {
                DictVO dict = loaded == null ? null : loaded.get(typeCode);
                if (dict == null) {
                    throw new IllegalArgumentException("导出失败，缺少字典类型: " + typeCode);
                }
                dictByType.put(typeCode, dict);
            }
        }

        Map<String, Map<String, String>> labelMappings = new LinkedHashMap<>();
        for (String typeCode : REQUIRED_EXPORT_DICT_TYPES) {
            DictVO dict = dictByType.get(typeCode);
            if (dict == null || dict.getItems() == null || dict.getItems().isEmpty()) {
                throw new IllegalArgumentException("导出失败，字典为空: " + typeCode);
            }
            Map<String, String> mapping = new LinkedHashMap<>();
            for (DictVO.Item item : dict.getItems()) {
                if (item == null || !StringUtils.hasText(item.getValue())) {
                    continue;
                }
                String value = item.getValue().trim();
                String label = StringUtils.hasText(item.getLabel()) ? item.getLabel().trim() : value;
                mapping.put(value, label);
            }
            if (mapping.isEmpty()) {
                throw new IllegalArgumentException("导出失败，字典未配置有效项: " + typeCode);
            }
            labelMappings.put(typeCode, mapping);
        }
        return labelMappings;
    }

    private DictVO loadDictFromCache(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return null;
        }
        String cached = authCacheService.getCachedDict(typeCode);
        if (!StringUtils.hasText(cached)) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, DictVO.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /*
    private String resolveRouteDisplay(Map<String, Map<String, String>> dictLabelMappings, List<String> routes) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        List<String> labels = new ArrayList<>();
        for (String routeCode : routes) {
            labels.add(resolveRequiredLabel(dictLabelMappings, DICT_ALL_SITE, routeCode, "巡查路段"));
        }
        return String.join("、", labels);
    }

    private String resolveRequiredLabel(Map<String, Map<String, String>> dictLabelMappings,
                                        String typeCode,
                                        String value,
                                        String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("导出失败，" + fieldName + "不能为空");
        }
        String normalized = value.trim();
        String label = Optional.ofNullable(dictLabelMappings.get(typeCode))
                .map(mapping -> mapping.get(normalized))
                .orElse(null);
        if (!StringUtils.hasText(label)) {
            throw new IllegalArgumentException("导出失败，" + fieldName + "字典值缺失: typeCode="
                    + typeCode + ", value=" + normalized);
        }
        return label.trim();
    }

    */

    private String resolveRouteDisplay(Map<String, Map<String, String>> dictLabelMappings, List<String> routes) {
        if (routes == null || routes.isEmpty()) {
            return null;
        }
        List<String> labels = new ArrayList<>();
        for (String routeCode : routes) {
            labels.add(resolveRequiredLabel(dictLabelMappings, DICT_ALL_SITE, routeCode, "route"));
        }
        return String.join("\u3001", labels);
    }

    private String resolveRequiredLabel(Map<String, Map<String, String>> dictLabelMappings,
                                        String typeCode,
                                        String value,
                                        String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("export failed: " + fieldName + " is blank");
        }
        String normalized = value.trim();
        String label = Optional.ofNullable(dictLabelMappings.get(typeCode))
                .map(mapping -> mapping.get(normalized))
                .orElse(null);
        if (!StringUtils.hasText(label)) {
            throw new IllegalArgumentException("export failed: dictionary value is missing, typeCode="
                    + typeCode + ", value=" + normalized);
        }
        return label.trim();
    }

    private String resolveOptionalLabel(Map<String, Map<String, String>> dictLabelMappings,
                                        String typeCode,
                                        String value,
                                        String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return resolveRequiredLabel(dictLabelMappings, typeCode, value, fieldName);
    }

    /*
    private String buildMileageDisplay(Mileage mileage) {
        if (mileage == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        double totalKm = 0d;
        boolean hasStructuredSegment = false;
        if (hasStructuredMileageInput(mileage.getDay())) {
            MileageInfo day = mileage.getDay();
            segments.add("白班:" + formatMileageInfo(day, "白班"));
            totalKm += day.getTotalKm();
            hasStructuredSegment = true;
        } else if (hasDisplayOnlyMileageInput(mileage.getDay())) {
            segments.add("白班:" + mileage.getDay().getDisplayText().trim());
        }
        if (hasStructuredMileageInput(mileage.getNight())) {
            MileageInfo night = mileage.getNight();
            segments.add("夜班:" + formatMileageInfo(night, "夜班"));
            totalKm += night.getTotalKm();
            hasStructuredSegment = true;
        } else if (hasDisplayOnlyMileageInput(mileage.getNight())) {
            segments.add("夜班:" + mileage.getNight().getDisplayText().trim());
        }
        if (segments.isEmpty()) {
            return null;
        }
        if (!hasStructuredSegment) {
            return String.join("；", segments);
        }
        return String.join("；", segments)
                + "（总计" + String.format(Locale.ROOT, "%.3f", totalKm) + "KM）";
    }

    private String formatMileageInfo(MileageInfo info, String shiftName) {
        String startStake = trimToNull(info.getStartStake());
        String endStake = trimToNull(info.getEndStake());
        if (startStake == null || endStake == null) {
            throw new IllegalArgumentException("导出失败，" + shiftName + "巡查里程桩号不完整");
        }
        if (info.getTotalKm() == null) {
            throw new IllegalArgumentException("导出失败，" + shiftName + "巡查里程总公里缺失");
        }
        return startStake + "–" + endStake;
    }

    */

    private String buildMileageDisplay(Mileage mileage) {
        if (mileage == null) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        double totalKm = 0d;
        boolean hasStructuredSegment = false;
        if (hasStructuredMileageInput(mileage.getDay())) {
            MileageInfo day = mileage.getDay();
            segments.add("\u767D\u73ED:" + formatMileageInfo(day, "\u767D\u73ED"));
            totalKm += day.getTotalKm();
            hasStructuredSegment = true;
        } else if (hasDisplayOnlyMileageInput(mileage.getDay())) {
            segments.add("\u767D\u73ED:" + mileage.getDay().getDisplayText().trim());
        }
        if (hasStructuredMileageInput(mileage.getNight())) {
            MileageInfo night = mileage.getNight();
            segments.add("\u591C\u73ED:" + formatMileageInfo(night, "\u591C\u73ED"));
            totalKm += night.getTotalKm();
            hasStructuredSegment = true;
        } else if (hasDisplayOnlyMileageInput(mileage.getNight())) {
            segments.add("\u591C\u73ED:" + mileage.getNight().getDisplayText().trim());
        }
        if (segments.isEmpty()) {
            return null;
        }
        if (!hasStructuredSegment) {
            return String.join("\uFF1B", segments);
        }
        return String.join("\uFF1B", segments)
                + "\uFF08\u603B\u8BA1" + String.format(Locale.ROOT, "%.3f", totalKm) + "KM\uFF09";
    }

    private String formatMileageInfo(MileageInfo info, String shiftName) {
        String startStake = trimToNull(info.getStartStake());
        String endStake = trimToNull(info.getEndStake());
        if (startStake == null || endStake == null) {
            throw new IllegalArgumentException("export failed, " + shiftName + " mileage stake is incomplete");
        }
        if (info.getTotalKm() == null) {
            throw new IllegalArgumentException("export failed, " + shiftName + " mileage total is missing");
        }
        return startStake + "-" + endStake;
    }

    private boolean hasStructuredMileageInput(MileageInfo info) {
        if (info == null) {
            return false;
        }
        return StringUtils.hasText(info.getStartStake())
                || StringUtils.hasText(info.getEndStake())
                || info.getTotalKm() != null;
    }

    private boolean hasDisplayOnlyMileageInput(MileageInfo info) {
        if (info == null) {
            return false;
        }
        return !hasStructuredMileageInput(info) && StringUtils.hasText(info.getDisplayText());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /*
    private String buildPeopleDisplay(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join("、", normalized);
    }

    private String buildDeliveryContactDisplay(List<Delivery> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            return null;
        }
        List<String> blocks = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            if (delivery == null) {
                continue;
            }
            String number = trimToNull(delivery.getNumber());
            String unit = trimToNull(delivery.getUnit());
            if (number == null && unit == null) {
                continue;
            }
            blocks.add("送达" + Optional.ofNullable(number).orElse("")
                    + "号《工作联系单》" + System.lineSeparator()
                    + "被送达单位：" + Optional.ofNullable(unit).orElse(""));
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return String.join(System.lineSeparator(), blocks);
    }

    */

    private String buildPeopleDisplay(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            normalized.add(value.trim());
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return String.join("\u3001", normalized);
    }

    private String buildDeliveryContactDisplay(List<Delivery> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            return null;
        }
        List<String> blocks = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            if (delivery == null) {
                continue;
            }
            String number = trimToNull(delivery.getNumber());
            String unit = trimToNull(delivery.getUnit());
            if (number == null && unit == null) {
                continue;
            }
            blocks.add("\u9001\u8FBE" + Optional.ofNullable(number).orElse("")
                    + "\u53F7\u300A\u5DE5\u4F5C\u8054\u7CFB\u5355\u300B" + System.lineSeparator()
                    + "\u88AB\u9001\u8FBE\u5355\u4F4D\uFF1A" + Optional.ofNullable(unit).orElse(""));
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return String.join(System.lineSeparator(), blocks);
    }

    private Path resolveStorageDirectory() throws IOException {
        String configuredDirectory = Optional.ofNullable(logProperties.getInspectionLog())
                .map(String::trim)
                .orElse("");
        if (!StringUtils.hasText(configuredDirectory)) {
            throw new IllegalStateException("road-safety.inspection-log is not configured");
        }
        try {
            Path storagePath = Paths.get(configuredDirectory).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);
            return storagePath;
        } catch (InvalidPathException ex) {
            throw new IOException("inspection storage path is invalid: " + configuredDirectory, ex);
        }
    }

    private void saveDetailRows(Long recordId, List<CanonicalDetail> details, LocalDateTime now) {
        if (recordId == null || details == null) {
            return;
        }
        for (CanonicalDetail detail : details) {
            InspectionHandlingDetail entity = InspectionHandlingDetail.builder()
                    .recordId(recordId)
                    .categoryCode(detail.getCategoryCode())
                    .categoryName(detail.getCategoryName())
                    .detailText(writeJson(detail.getRawPayload()))
                    .detailOrder(detail.getDetailOrder())
                    .createdAt(now)
                    .build();
            detailMapper.insert(entity);
        }
    }

    private void deleteDetailRows(Long recordId) {
        if (recordId == null) {
            return;
        }
        detailMapper.delete(Wrappers.lambdaQuery(InspectionHandlingDetail.class)
                .eq(InspectionHandlingDetail::getRecordId, recordId));
    }

    private void savePhotoRows(Long recordId, List<PhotoItem> photos, LocalDateTime now) {
        if (recordId == null || photos == null) {
            return;
        }
        for (PhotoItem photo : photos) {
            if (photo.getFileId() == null) {
                continue;
            }
            PhotoItem entity = PhotoItem.builder()
                    .recordId(recordId)
                    .fileId(photo.getFileId())
                    .description(photo.getDescription())
                    .sortOrder(photo.getSortOrder())
                    .createdAt(now)
                    .build();
            photoMapper.insert(entity);
        }
    }

    private void deletePhotoRows(Long recordId) {
        if (recordId == null) {
            return;
        }
        photoMapper.delete(Wrappers.lambdaQuery(PhotoItem.class)
                .eq(PhotoItem::getRecordId, recordId));
    }

    private String writeJson(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("json serialization failed", ex);
        }
    }
}
