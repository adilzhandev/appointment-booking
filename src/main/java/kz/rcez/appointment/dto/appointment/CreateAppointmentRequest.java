package kz.rcez.appointment.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Запись пациента на свободный слот")
public record CreateAppointmentRequest(
        @Schema(example = "42") @NotNull(message = "Идентификатор пациента обязателен") Long patientId,
        @Schema(example = "1015") @NotNull(message = "Идентификатор слота обязателен") Long timeSlotId,
        @Schema(example = "Головная боль, температура 37.5") @Size(max = 1000) String complaint
) {
}
