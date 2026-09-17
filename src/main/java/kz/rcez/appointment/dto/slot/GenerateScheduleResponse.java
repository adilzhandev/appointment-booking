package kz.rcez.appointment.dto.slot;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Итог генерации расписания")
public record GenerateScheduleResponse(
        @Schema(example = "96") int created,
        @Schema(example = "4", description = "Пропущено из-за пересечения с существующими слотами") int skipped,
        List<SlotResponse> slots
) {
}
