package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.common.PageResponse;
import kz.rcez.appointment.dto.patient.PatientRequest;
import kz.rcez.appointment.dto.patient.PatientResponse;
import kz.rcez.appointment.entity.Patient;
import kz.rcez.appointment.exception.ConflictException;
import kz.rcez.appointment.exception.NotFoundException;
import kz.rcez.appointment.mapper.PatientMapper;
import kz.rcez.appointment.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional
    public PatientResponse create(PatientRequest request) {
        if (patientRepository.existsByIin(request.iin())) {
            throw new ConflictException("IIN_ALREADY_EXISTS", "Пациент с ИИН " + request.iin() + " уже зарегистрирован");
        }
        return patientMapper.toResponse(patientRepository.save(patientMapper.toEntity(request)));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = getEntity(id);
        patientRepository.findByIin(request.iin())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("IIN_ALREADY_EXISTS",
                            "ИИН " + request.iin() + " принадлежит другому пациенту");
                });
        patientMapper.update(request, patient);
        return patientMapper.toResponse(patient);
    }

    @Transactional
    public void delete(Long id) {
        getEntity(id).setDeleted(true);
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        return patientMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PatientResponse getByIin(String iin) {
        return patientRepository.findByIin(iin)
                .map(patientMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Пациент с ИИН " + iin + " не найден"));
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> search(String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return PageResponse.of(patientRepository.search(q, pageable), patientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Patient getEntity(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new NotFoundException("Пациент", id));
    }
}
