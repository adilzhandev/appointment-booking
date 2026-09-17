package kz.rcez.appointment.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.rcez.appointment.entity.enums.Role;

@Schema(description = "Создание учётной записи (только ADMIN)")
public record RegisterUserRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 255) String fullName,
        @NotNull Role role
) {
}
