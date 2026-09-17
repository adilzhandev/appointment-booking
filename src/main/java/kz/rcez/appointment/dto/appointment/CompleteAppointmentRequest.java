package kz.rcez.appointment.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Завершение приёма врачом")
public record CompleteAppointmentRequest(
        @Schema(example = "ОРВИ. Назначено симптоматическое лечение.")
        @Size(max = 2000) String conclusion
) {
}
