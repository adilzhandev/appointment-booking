package kz.rcez.appointment.dto.patient;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kz.rcez.appointment.entity.enums.Gender;

import java.time.LocalDate;

@Schema(description = "Создание/обновление пациента")
public record PatientRequest(
        @Schema(example = "900101300123")
        @NotBlank(message = "ИИН обязателен")
        @Pattern(regexp = "^[0-9]{12}$", message = "ИИН должен состоять из 12 цифр") String iin,

        @NotBlank @Size(max = 128) String lastName,
        @NotBlank @Size(max = 128) String firstName,
        @Size(max = 128) String middleName,

        @NotNull @Past(message = "Дата рождения должна быть в прошлом") LocalDate birthDate,
        @NotNull Gender gender,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Некорректный номер телефона") String phone,
        @Size(max = 512) String address
) {
}
