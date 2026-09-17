package kz.rcez.appointment.mapper;

import kz.rcez.appointment.dto.patient.PatientRequest;
import kz.rcez.appointment.dto.patient.PatientResponse;
import kz.rcez.appointment.entity.Patient;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface PatientMapper {

    PatientResponse toResponse(Patient patient);

    Patient toEntity(PatientRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(PatientRequest request, @MappingTarget Patient patient);
}
