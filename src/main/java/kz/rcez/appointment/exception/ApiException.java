package kz.rcez.appointment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Базовое бизнес-исключение с машиночитаемым кодом и HTTP-статусом. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
