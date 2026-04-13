package com.xrcgs.roadsafety.inspection.interfaces.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request payload for updating an existing inspection log and exporting file.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionLogUpdateSubmitExportRequest extends InspectionLogSubmitExportRequest {

    @NotNull(message = "log id must not be null")
    @Min(value = 1, message = "log id must be greater than 0")
    private Long id;
}
