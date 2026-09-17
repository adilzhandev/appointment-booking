package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.appointment.AppointmentResponse;
import kz.rcez.appointment.dto.appointment.CancelAppointmentRequest;
import kz.rcez.appointment.dto.appointment.CompleteAppointmentRequest;
import kz.rcez.appointment.dto.appointment.CreateAppointmentRequest;
import kz.rcez.appointment.dto.common.PageResponse;
import kz.rcez.appointment.entity.Appointment;
import kz.rcez.appointment.entity.Patient;
import kz.rcez.appointment.entity.TimeSlot;
import kz.rcez.appointment.entity.enums.AppointmentStatus;
import kz.rcez.appointment.entity.enums.SlotStatus;
import kz.rcez.appointment.exception.ConflictException;
import kz.rcez.appointment.exception.NotFoundException;
import kz.rcez.appointment.mapper.AppointmentMapper;
import kz.rcez.appointment.repository.AppointmentRepository;
import kz.rcez.appointment.repository.TimeSlotRepository;
import kz.rcez.appointment.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Записи на приём. Создание и отмена выполняются в одной транзакции со сменой
 * статуса слота, слот при этом берётся под пессимистичную блокировку.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentMapper appointmentMapper;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final CurrentUser currentUser;
    private final Clock clock;

    /**
     * Записывает пациента на свободный слот и переводит слот в BOOKED.
     * Отклоняет занятые, заблокированные и прошедшие слоты, а также повторную
     * активную запись к тому же врачу в тот же день.
     */
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        Patient patient = patientService.getEntity(request.patientId());

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(request.timeSlotId())
                .orElseThrow(() -> new NotFoundException("Слот", request.timeSlotId()));

        if (slot.isDeleted()) {
            throw new NotFoundException("Слот", request.timeSlotId());
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new ConflictException("SLOT_ALREADY_BOOKED", "Слот уже занят другой записью");
        }
        if (slot.getStatus() == SlotStatus.BLOCKED) {
            throw new ConflictException("SLOT_BLOCKED", "Слот заблокирован и недоступен для записи");
        }
        if (slot.getStartsAt().isBefore(LocalDateTime.now(clock))) {
            throw new ConflictException("SLOT_IN_PAST", "Нельзя записаться на прошедший слот");
        }
        if (appointmentRepository.existsActiveForPatientAndDoctorOnDate(
                patient.getId(), slot.getDoctor().getId(), slot.getSlotDate())) {
            throw new ConflictException("DUPLICATE_APPOINTMENT",
                    "У пациента уже есть активная запись к этому врачу на " + slot.getSlotDate());
        }

        slot.setStatus(SlotStatus.BOOKED);

        Appointment appointment = Appointment.builder()
                .timeSlot(slot)
                .patient(patient)
                .doctor(slot.getDoctor())
                .status(AppointmentStatus.SCHEDULED)
                .complaint(request.complaint())
                .createdBy(currentUser.username())
                .build();

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    /** Отменяет запись и возвращает слот в статус FREE. */
    @Transactional
    public AppointmentResponse cancel(Long id, CancelAppointmentRequest request) {
        Appointment appointment = getEntity(id);
        requireActive(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(request.reason());
        appointment.setClosedAt(Instant.now(clock));

        TimeSlot slot = appointment.getTimeSlot();
        if (slot.getStatus() == SlotStatus.BOOKED) {
            slot.setStatus(SlotStatus.FREE);
        }

        return appointmentMapper.toResponse(appointment);
    }

    /** Завершение приёма врачом; слот остаётся занятым как след состоявшегося визита. */
    @Transactional
    public AppointmentResponse complete(Long id, CompleteAppointmentRequest request) {
        Appointment appointment = getEntity(id);
        requireActive(appointment);
        doctorService.requireManageAccess(appointment.getDoctor());

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setConclusion(request.conclusion());
        appointment.setClosedAt(Instant.now(clock));

        return appointmentMapper.toResponse(appointment);
    }

    /** Отметка о неявке пациента. */
    @Transactional
    public AppointmentResponse markNoShow(Long id) {
        Appointment appointment = getEntity(id);
        requireActive(appointment);

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointment.setClosedAt(Instant.now(clock));

        return appointmentMapper.toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        return appointmentMapper.toResponse(getEntity(id));
    }

    /** История записей пациента, свежие сверху. */
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> patientHistory(Long patientId,
                                                            AppointmentStatus status,
                                                            Pageable pageable) {
        patientService.getEntity(patientId);
        return PageResponse.of(
                appointmentRepository.findPatientHistory(patientId, status, pageable),
                appointmentMapper::toResponse);
    }

    /** Приёмы врача на конкретный день. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> doctorDaySchedule(Long doctorId, LocalDate date, AppointmentStatus status) {
        doctorService.getEntity(doctorId);
        return appointmentRepository.findDoctorDaySchedule(doctorId, date, status).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    private void requireActive(Appointment appointment) {
        if (appointment.getStatus().isTerminal()) {
            throw new ConflictException("APPOINTMENT_NOT_ACTIVE",
                    "Запись уже в статусе " + appointment.getStatus() + " и не может быть изменена");
        }
    }

    private Appointment getEntity(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Запись на приём", id));
    }
}
