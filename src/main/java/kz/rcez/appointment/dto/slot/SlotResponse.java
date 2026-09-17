package kz.rcez.appointment.dto.slot;

import kz.rcez.appointment.entity.enums.SlotStatus;
import kz.rcez.appointment.entity.enums.Specialty;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotResponse(
        Long id,
        Long doctorId,
        String doctorFullName,
        Specialty specialty,
        String cabinet,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        SlotStatus status
) {
}
