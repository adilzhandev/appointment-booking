package kz.rcez.appointment.config;

import kz.rcez.appointment.entity.Doctor;
import kz.rcez.appointment.entity.Patient;
import kz.rcez.appointment.entity.UserAccount;
import kz.rcez.appointment.entity.enums.Gender;
import kz.rcez.appointment.entity.enums.Role;
import kz.rcez.appointment.entity.enums.Specialty;
import kz.rcez.appointment.repository.DoctorRepository;
import kz.rcez.appointment.repository.PatientRepository;
import kz.rcez.appointment.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Демонстрационные данные для локального запуска: учётные записи трёх ролей,
 * несколько врачей и пациентов. Отключается флагом app.seed.enabled=false.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DataSeeder {

    private final UserAccountRepository userAccountRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedData() {
        return args -> seed();
    }

    @Transactional
    protected void seed() {
        if (userAccountRepository.count() > 0) {
            log.info("Демо-данные уже загружены, инициализация пропущена");
            return;
        }

        createUser("admin", "admin123", "Администратор системы", Role.ADMIN);
        createUser("registrar", "registrar123", "Сериккали Айгуль Маратовна", Role.REGISTRAR);

        UserAccount therapistAccount = createUser("doctor", "doctor123", "Ахметов Данияр Серикович", Role.DOCTOR);
        UserAccount cardiologistAccount =
                createUser("doctor2", "doctor123", "Исаева Гульнар Бахытовна", Role.DOCTOR);

        doctorRepository.save(Doctor.builder()
                .userAccount(therapistAccount)
                .fullName("Ахметов Данияр Серикович")
                .specialty(Specialty.THERAPIST)
                .cabinet("312")
                .phone("+77012345678")
                .defaultSlotMinutes(15)
                .build());

        doctorRepository.save(Doctor.builder()
                .userAccount(cardiologistAccount)
                .fullName("Исаева Гульнар Бахытовна")
                .specialty(Specialty.CARDIOLOGIST)
                .cabinet("205")
                .phone("+77017654321")
                .defaultSlotMinutes(20)
                .build());

        doctorRepository.save(Doctor.builder()
                .fullName("Оспанов Ерлан Канатович")
                .specialty(Specialty.SURGEON)
                .cabinet("108")
                .phone("+77011112233")
                .defaultSlotMinutes(30)
                .build());

        patientRepository.save(Patient.builder()
                .iin("900101300123")
                .lastName("Нурланов")
                .firstName("Асхат")
                .middleName("Бекович")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("+77051234567")
                .address("г. Астана, ул. Кенесары, 42, кв. 17")
                .build());

        patientRepository.save(Patient.builder()
                .iin("850615400456")
                .lastName("Абдирова")
                .firstName("Жанна")
                .middleName("Талгатовна")
                .birthDate(LocalDate.of(1985, 6, 15))
                .gender(Gender.FEMALE)
                .phone("+77057654321")
                .address("г. Астана, пр. Абая, 8, кв. 3")
                .build());

        log.info("Демо-данные загружены: 4 учётные записи, 3 врача, 2 пациента");
    }

    private UserAccount createUser(String username, String rawPassword, String fullName, Role role) {
        return userAccountRepository.save(UserAccount.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build());
    }
}
