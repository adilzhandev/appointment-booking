package kz.rcez.appointment.mapper;

import kz.rcez.appointment.dto.appointment.AppointmentResponse;
import kz.rcez.appointment.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface AppointmentMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientIin", source = "patient.iin")
    @Mapping(target = "patientFullName", source = "patient.fullName")
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "specialty", source = "doctor.specialty")
    @Mapping(target = "cabinet", source = "doctor.cabinet")
    @Mapping(target = "timeSlotId", source = "timeSlot.id")
    @Mapping(target = "slotDate", source = "timeSlot.slotDate")
    @Mapping(target = "startTime", source = "timeSlot.startTime")
    @Mapping(target = "endTime", source = "timeSlot.endTime")
    AppointmentResponse toResponse(Appointment appointment);
}
