package kz.rcez.appointment.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Единый формат ошибки API. */
@Schema(description = "Описание ошибки")
public record ApiError(
        @Schema(example = "2026-09-17T09:15:30Z") Instant timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "SLOT_ALREADY_BOOKED") String code,
        @Schema(example = "Слот уже занят") String message,
        @Schema(example = "/api/v1/appointments") String path,
        @Schema(description = "Ошибки валидации по полям") List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {
    }
}
