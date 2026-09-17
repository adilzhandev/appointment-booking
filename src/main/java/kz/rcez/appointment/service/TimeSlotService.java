package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.slot.CreateSlotRequest;
import kz.rcez.appointment.dto.slot.GenerateScheduleRequest;
import kz.rcez.appointment.dto.slot.GenerateScheduleResponse;
import kz.rcez.appointment.dto.slot.SlotResponse;
import kz.rcez.appointment.entity.Doctor;
import kz.rcez.appointment.entity.TimeSlot;
import kz.rcez.appointment.entity.enums.SlotStatus;
import kz.rcez.appointment.entity.enums.Specialty;
import kz.rcez.appointment.exception.BadRequestException;
import kz.rcez.appointment.exception.ConflictException;
import kz.rcez.appointment.exception.NotFoundException;
import kz.rcez.appointment.mapper.TimeSlotMapper;
import kz.rcez.appointment.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Управление расписанием врача: одиночные слоты, пакетная генерация, блокировка. */
@Service
@RequiredArgsConstructor
public class TimeSlotService {

    /** Защита от случайной генерации расписания на годы вперёд. */
    private static final int MAX_GENERATION_DAYS = 62;

    private final TimeSlotRepository timeSlotRepository;
    private final TimeSlotMapper timeSlotMapper;
    private final DoctorService doctorService;
    private final Clock clock;

    @Transactional
    public SlotResponse createSlot(Long doctorId, CreateSlotRequest request) {
        Doctor doctor = doctorService.getEntity(doctorId);
        doctorService.requireManageAccess(doctor);

        validateInterval(request.startTime(), request.endTime());
        requireNotInPast(request.slotDate(), request.startTime());

        if (timeSlotRepository.existsOverlapping(doctorId, request.slotDate(),
                request.startTime(), request.endTime(), null)) {
            throw new ConflictException("SLOT_OVERLAP",
                    "Слот пересекается с существующим слотом расписания врача");
        }

        TimeSlot slot = TimeSlot.builder()
                .doctor(doctor)
                .slotDate(request.slotDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(SlotStatus.FREE)
                .build();

        return timeSlotMapper.toResponse(timeSlotRepository.save(slot));
    }

    /**
     * Нарезает рабочий день на слоты фиксированной длительности.
     * Слоты, пересекающиеся с уже существующими, пропускаются, а не роняют операцию.
     */
    @Transactional
    public GenerateScheduleResponse generateSchedule(Long doctorId, GenerateScheduleRequest request) {
        Doctor doctor = doctorService.getEntity(doctorId);
        doctorService.requireManageAccess(doctor);

        validateGenerationRequest(request);

        List<TimeSlot> created = new ArrayList<>();
        int skipped = 0;

        for (LocalDate date = request.dateFrom(); !date.isAfter(request.dateTo()); date = date.plusDays(1)) {
            if (request.skipWeekendsOrDefault() && isWeekend(date)) {
                continue;
            }
            for (LocalTime start = request.workStart();
                 !start.plusMinutes(request.slotMinutes()).isAfter(request.workEnd());
                 start = start.plusMinutes(request.slotMinutes())) {

                LocalTime end = start.plusMinutes(request.slotMinutes());

                if (request.hasBreak() && overlapsBreak(start, end, request)) {
                    continue;
                }
                if (LocalDateTime.of(date, start).isBefore(LocalDateTime.now(clock))) {
                    skipped++;
                    continue;
                }
                if (timeSlotRepository.existsOverlapping(doctorId, date, start, end, null)) {
                    skipped++;
                    continue;
                }

                created.add(TimeSlot.builder()
                        .doctor(doctor)
                        .slotDate(date)
                        .startTime(start)
                        .endTime(end)
                        .status(SlotStatus.FREE)
                        .build());
            }
        }

        List<SlotResponse> saved = timeSlotRepository.saveAll(created).stream()
                .map(timeSlotMapper::toResponse)
                .toList();

        return new GenerateScheduleResponse(saved.size(), skipped, saved);
    }

    /** Поиск свободных слотов по врачу / специальности / диапазону дат. */
    @Transactional(readOnly = true)
    public List<SlotResponse> findFreeSlots(Long doctorId, Specialty specialty, LocalDate dateFrom, LocalDate dateTo) {
        LocalDate from = dateFrom == null ? LocalDate.now(clock) : dateFrom;
        LocalDate to = dateTo == null ? from.plusDays(7) : dateTo;
        if (to.isBefore(from)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Дата окончания раньше даты начала");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        return timeSlotRepository.searchSlots(SlotStatus.FREE, doctorId, specialty, from, to).stream()
                .filter(slot -> slot.getStartsAt().isAfter(now))
                .map(timeSlotMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> getDoctorDaySchedule(Long doctorId, LocalDate date) {
        doctorService.getEntity(doctorId);
        return timeSlotRepository.findByDoctorIdAndSlotDateOrderByStartTime(doctorId, date).stream()
                .map(timeSlotMapper::toResponse)
                .toList();
    }

    /** Блокировка свободного слота (отпуск, совещание). */
    @Transactional
    public SlotResponse block(Long slotId) {
        TimeSlot slot = getEntity(slotId);
        doctorService.requireManageAccess(slot.getDoctor());
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new ConflictException("SLOT_BOOKED", "Нельзя заблокировать слот с активной записью");
        }
        slot.setStatus(SlotStatus.BLOCKED);
        return timeSlotMapper.toResponse(slot);
    }

    @Transactional
    public SlotResponse unblock(Long slotId) {
        TimeSlot slot = getEntity(slotId);
        doctorService.requireManageAccess(slot.getDoctor());
        if (slot.getStatus() != SlotStatus.BLOCKED) {
            throw new ConflictException("SLOT_NOT_BLOCKED", "Слот не заблокирован");
        }
        slot.setStatus(SlotStatus.FREE);
        return timeSlotMapper.toResponse(slot);
    }

    /** Мягкое удаление слота; занятый слот удалить нельзя. */
    @Transactional
    public void delete(Long slotId) {
        TimeSlot slot = getEntity(slotId);
        doctorService.requireManageAccess(slot.getDoctor());
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new ConflictException("SLOT_BOOKED", "Нельзя удалить слот с активной записью");
        }
        slot.setDeleted(true);
    }

    @Transactional(readOnly = true)
    public TimeSlot getEntity(Long id) {
        return timeSlotRepository.findById(id).orElseThrow(() -> new NotFoundException("Слот", id));
    }

    private void validateGenerationRequest(GenerateScheduleRequest request) {
        if (request.dateTo().isBefore(request.dateFrom())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Дата окончания раньше даты начала");
        }
        if (request.dateFrom().plusDays(MAX_GENERATION_DAYS).isBefore(request.dateTo())) {
            throw new BadRequestException("RANGE_TOO_LONG",
                    "Расписание можно сгенерировать не более чем на " + MAX_GENERATION_DAYS + " дней");
        }
        validateInterval(request.workStart(), request.workEnd());
        if (request.hasBreak()) {
            validateInterval(request.breakStart(), request.breakEnd());
        }
    }

    private void validateInterval(LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("INVALID_TIME_RANGE", "Время начала должно быть раньше времени окончания");
        }
    }

    private void requireNotInPast(LocalDate date, LocalTime start) {
        if (LocalDateTime.of(date, start).isBefore(LocalDateTime.now(clock))) {
            throw new BadRequestException("SLOT_IN_PAST", "Нельзя создать слот в прошлом");
        }
    }

    private boolean overlapsBreak(LocalTime start, LocalTime end, GenerateScheduleRequest request) {
        return start.isBefore(request.breakEnd()) && request.breakStart().isBefore(end);
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
