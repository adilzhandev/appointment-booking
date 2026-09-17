package kz.rcez.appointment.exception;

import org.springframework.http.HttpStatus;

/** Нарушение бизнес-инварианта: занятый слот, пересечение расписания, дубль ИИН. */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
