package kz.rcez.appointment.dto.doctor;

import kz.rcez.appointment.entity.enums.Specialty;

public record DoctorResponse(
        Long id,
        String fullName,
        Specialty specialty,
        String specialtyTitle,
        String cabinet,
        String phone,
        Integer defaultSlotMinutes,
        Long userAccountId
) {
}
