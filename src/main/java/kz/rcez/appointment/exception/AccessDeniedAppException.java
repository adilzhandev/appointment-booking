package kz.rcez.appointment.exception;

import org.springframework.http.HttpStatus;

/** Пользователь аутентифицирован, но не владеет ресурсом (врач и чужое расписание). */
public class AccessDeniedAppException extends ApiException {

    public AccessDeniedAppException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCESS_DENIED", message);
    }
}
