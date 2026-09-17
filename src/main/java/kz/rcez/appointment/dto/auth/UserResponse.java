package kz.rcez.appointment.dto.auth;

import kz.rcez.appointment.entity.enums.Role;

public record UserResponse(Long id, String username, String fullName, Role role, boolean enabled) {
}
