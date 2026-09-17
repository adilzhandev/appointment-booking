package kz.rcez.appointment.dto.appointment;

import kz.rcez.appointment.entity.enums.AppointmentStatus;
import kz.rcez.appointment.entity.enums.Specialty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientIin,
        String patientFullName,
        Long doctorId,
        String doctorFullName,
        Specialty specialty,
        String cabinet,
        Long timeSlotId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        String complaint,
        String conclusion,
        String cancelReason,
        String createdBy,
        Instant createdAt
) {
}
