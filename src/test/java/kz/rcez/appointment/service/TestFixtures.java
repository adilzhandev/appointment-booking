package kz.rcez.appointment.service;

import kz.rcez.appointment.entity.Doctor;
import kz.rcez.appointment.entity.Patient;
import kz.rcez.appointment.entity.TimeSlot;
import kz.rcez.appointment.entity.UserAccount;
import kz.rcez.appointment.entity.enums.Gender;
import kz.rcez.appointment.entity.enums.Role;
import kz.rcez.appointment.entity.enums.SlotStatus;
import kz.rcez.appointment.entity.enums.Specialty;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Общие объекты для тестов бизнес-логики. */
final class TestFixtures {

    static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
    /** Фиксированное «сейчас»: 17 сентября 2026, 10:00. */
    static final Clock FIXED_CLOCK = Clock.fixed(
            ZonedDateTime.of(2026, 9, 17, 10, 0, 0, 0, ZONE).toInstant(), ZONE);

    static final LocalDate TOMORROW = LocalDate.of(2026, 9, 18);
    static final LocalDate YESTERDAY = LocalDate.of(2026, 9, 16);

    private TestFixtures() {
    }

    static UserAccount userAccount(Long id, Role role) {
        UserAccount account = UserAccount.builder()
                .username("user" + id)
                .passwordHash("hash")
                .fullName("Пользователь " + id)
                .role(role)
                .enabled(true)
                .build();
        account.setId(id);
        return account;
    }

    static Doctor doctor(Long id) {
        Doctor doctor = Doctor.builder()
                .fullName("Ахметов Данияр Серикович")
                .specialty(Specialty.THERAPIST)
                .cabinet("312")
                .defaultSlotMinutes(15)
                .build();
        doctor.setId(id);
        return doctor;
    }

    static Patient patient(Long id) {
        Patient patient = Patient.builder()
                .iin("900101300123")
                .lastName("Нурланов")
                .firstName("Асхат")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .build();
        patient.setId(id);
        return patient;
    }

    static TimeSlot slot(Long id, Doctor doctor, LocalDate date, LocalTime start, SlotStatus status) {
        TimeSlot slot = TimeSlot.builder()
                .doctor(doctor)
                .slotDate(date)
                .startTime(start)
                .endTime(start.plusMinutes(15))
                .status(status)
                .build();
        slot.setId(id);
        return slot;
    }
}
