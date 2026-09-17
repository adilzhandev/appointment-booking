package kz.rcez.appointment.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Отмена записи")
public record CancelAppointmentRequest(
        @Schema(example = "Пациент перенёс визит")
        @NotBlank(message = "Причина отмены обязательна") @Size(max = 512) String reason
) {
}
