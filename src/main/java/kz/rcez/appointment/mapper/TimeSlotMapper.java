package kz.rcez.appointment.mapper;

import kz.rcez.appointment.dto.slot.SlotResponse;
import kz.rcez.appointment.entity.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TimeSlotMapper {

    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "specialty", source = "doctor.specialty")
    @Mapping(target = "cabinet", source = "doctor.cabinet")
    SlotResponse toResponse(TimeSlot slot);
}
