package kz.rcez.appointment.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на аутентификацию")
public record LoginRequest(
        @Schema(example = "registrar") @NotBlank(message = "Логин обязателен") String username,
        @Schema(example = "registrar123") @NotBlank(message = "Пароль обязателен") String password
) {
}
