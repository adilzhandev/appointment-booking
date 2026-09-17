package kz.rcez.appointment.mapper;

import kz.rcez.appointment.dto.doctor.DoctorResponse;
import kz.rcez.appointment.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DoctorMapper {

    @Mapping(target = "specialtyTitle", expression = "java(doctor.getSpecialty().getTitle())")
    @Mapping(target = "userAccountId", source = "userAccount.id")
    DoctorResponse toResponse(Doctor doctor);
}
