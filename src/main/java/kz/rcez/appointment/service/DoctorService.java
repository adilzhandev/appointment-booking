package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.common.PageResponse;
import kz.rcez.appointment.dto.doctor.DoctorRequest;
import kz.rcez.appointment.dto.doctor.DoctorResponse;
import kz.rcez.appointment.entity.Doctor;
import kz.rcez.appointment.entity.UserAccount;
import kz.rcez.appointment.entity.enums.Role;
import kz.rcez.appointment.entity.enums.Specialty;
import kz.rcez.appointment.exception.AccessDeniedAppException;
import kz.rcez.appointment.exception.BadRequestException;
import kz.rcez.appointment.exception.ConflictException;
import kz.rcez.appointment.exception.NotFoundException;
import kz.rcez.appointment.mapper.DoctorMapper;
import kz.rcez.appointment.repository.DoctorRepository;
import kz.rcez.appointment.repository.UserAccountRepository;
import kz.rcez.appointment.security.AppUserDetails;
import kz.rcez.appointment.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserAccountRepository userAccountRepository;
    private final DoctorMapper doctorMapper;
    private final CurrentUser currentUser;

    @Transactional
    public DoctorResponse create(DoctorRequest request) {
        Doctor doctor = Doctor.builder()
                .fullName(request.fullName())
                .specialty(request.specialty())
                .cabinet(request.cabinet())
                .phone(request.phone())
                .defaultSlotMinutes(request.defaultSlotMinutes() == null ? 15 : request.defaultSlotMinutes())
                .userAccount(resolveUserAccount(request.userAccountId(), null))
                .build();

        return doctorMapper.toResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorRequest request) {
        Doctor doctor = getEntity(id);
        doctor.setFullName(request.fullName());
        doctor.setSpecialty(request.specialty());
        doctor.setCabinet(request.cabinet());
        doctor.setPhone(request.phone());
        if (request.defaultSlotMinutes() != null) {
            doctor.setDefaultSlotMinutes(request.defaultSlotMinutes());
        }
        doctor.setUserAccount(resolveUserAccount(request.userAccountId(), id));
        return doctorMapper.toResponse(doctor);
    }

    /** Мягкое удаление: врач исчезает из выборок, но история записей остаётся целой. */
    @Transactional
    public void delete(Long id) {
        Doctor doctor = getEntity(id);
        doctor.setDeleted(true);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getById(Long id) {
        return doctorMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> search(Specialty specialty, String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return PageResponse.of(doctorRepository.search(specialty, q, pageable), doctorMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Doctor getEntity(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new NotFoundException("Врач", id));
    }

    /**
     * Проверяет право управлять расписанием и приёмами врача:
     * ADMIN — любым, DOCTOR — только своим, остальные роли — нет.
     */
    public void requireManageAccess(Doctor doctor) {
        if (currentUser.isAdmin()) {
            return;
        }
        Long currentUserId = currentUser.details().map(AppUserDetails::getUserId).orElse(null);
        Long ownerId = doctor.getUserAccount() == null ? null : doctor.getUserAccount().getId();
        if (currentUserId == null || !currentUserId.equals(ownerId)) {
            throw new AccessDeniedAppException("Врач может управлять только собственным расписанием");
        }
    }

    /** Карточка врача, привязанная к текущей учётной записи. */
    @Transactional(readOnly = true)
    public Doctor getCurrentDoctor() {
        Long userId = currentUser.userId()
                .orElseThrow(() -> new AccessDeniedAppException("Текущий пользователь не определён"));
        return doctorRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new NotFoundException("Карточка врача не привязана к текущей учётной записи"));
    }

    private UserAccount resolveUserAccount(Long userAccountId, Long currentDoctorId) {
        if (userAccountId == null) {
            return null;
        }
        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new NotFoundException("Учётная запись", userAccountId));
        if (account.getRole() != Role.DOCTOR) {
            throw new BadRequestException("INVALID_ROLE",
                    "К карточке врача можно привязать только учётную запись с ролью DOCTOR");
        }
        doctorRepository.findByUserAccountId(userAccountId)
                .filter(existing -> !existing.getId().equals(currentDoctorId))
                .ifPresent(existing -> {
                    throw new ConflictException("USER_ALREADY_LINKED",
                            "Учётная запись уже привязана к врачу id=" + existing.getId());
                });
        return account;
    }
}
