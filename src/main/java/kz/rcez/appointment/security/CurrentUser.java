package kz.rcez.appointment.security;

import kz.rcez.appointment.entity.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Доступ к текущему аутентифицированному пользователю. */
@Component
public class CurrentUser {

    public Optional<AppUserDetails> details() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            return Optional.empty();
        }
        return Optional.of(details);
    }

    public String username() {
        return details().map(AppUserDetails::getUsername).orElse("system");
    }

    public Optional<Long> userId() {
        return details().map(AppUserDetails::getUserId);
    }

    public boolean hasRole(Role role) {
        return details().map(d -> d.getRole().equals(role.name())).orElse(false);
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}
