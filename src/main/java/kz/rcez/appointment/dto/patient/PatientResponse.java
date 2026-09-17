package kz.rcez.appointment.dto.patient;

import kz.rcez.appointment.entity.enums.Gender;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String iin,
        String lastName,
        String firstName,
        String middleName,
        String fullName,
        LocalDate birthDate,
        Gender gender,
        String phone,
        String address
) {
}
