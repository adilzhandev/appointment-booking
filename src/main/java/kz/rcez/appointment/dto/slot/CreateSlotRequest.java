package kz.rcez.appointment.dto.slot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Создание одиночного слота")
public record CreateSlotRequest(
        @Schema(example = "2026-09-20") @NotNull LocalDate slotDate,
        @Schema(example = "09:00") @NotNull LocalTime startTime,
        @Schema(example = "09:15") @NotNull LocalTime endTime
) {
}
