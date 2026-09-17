package kz.rcez.appointment.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.rcez.appointment.entity.enums.Role;

@Schema(description = "Выданный токен доступа")
public record JwtResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "3600") long expiresInSeconds,
        String username,
        String fullName,
        Role role
) {
}
