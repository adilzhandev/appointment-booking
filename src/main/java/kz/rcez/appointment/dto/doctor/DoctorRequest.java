package kz.rcez.appointment.dto.doctor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kz.rcez.appointment.entity.enums.Specialty;

@Schema(description = "Создание/обновление врача")
public record DoctorRequest(
        @Schema(example = "Ахметов Данияр Серикович")
        @NotBlank(message = "ФИО врача обязательно") @Size(max = 255) String fullName,

        @Schema(example = "THERAPIST")
        @NotNull(message = "Специальность обязательна") Specialty specialty,

        @Schema(example = "312")
        @Size(max = 16) String cabinet,

        @Schema(example = "+77012345678")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный номер телефона") String phone,

        @Schema(example = "15", description = "Длительность приёма по умолчанию, мин")
        @Min(5) @Max(120) Integer defaultSlotMinutes,

        @Schema(description = "Идентификатор учётной записи врача (роль DOCTOR)")
        Long userAccountId
) {
}
