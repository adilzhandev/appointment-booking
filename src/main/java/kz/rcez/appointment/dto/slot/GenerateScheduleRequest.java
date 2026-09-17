package kz.rcez.appointment.dto.slot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Пакетная генерация расписания: на каждый день диапазона нарезаются слоты
 * длительностью slotMinutes от workStart до workEnd, с вычетом перерыва.
 */
@Schema(description = "Генерация расписания врача на период")
public record GenerateScheduleRequest(
        @Schema(example = "2026-09-21") @NotNull LocalDate dateFrom,
        @Schema(example = "2026-09-25") @NotNull LocalDate dateTo,
        @Schema(example = "09:00") @NotNull LocalTime workStart,
        @Schema(example = "17:00") @NotNull LocalTime workEnd,
        @Schema(example = "15") @NotNull @Min(5) @Max(120) Integer slotMinutes,
        @Schema(example = "13:00", description = "Начало перерыва, необязательно") LocalTime breakStart,
        @Schema(example = "14:00", description = "Конец перерыва, необязательно") LocalTime breakEnd,
        @Schema(example = "true", description = "Пропускать субботу и воскресенье") Boolean skipWeekends
) {
    public boolean skipWeekendsOrDefault() {
        return skipWeekends == null || skipWeekends;
    }

    public boolean hasBreak() {
        return breakStart != null && breakEnd != null;
    }
}
