package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.appointment.AppointmentResponse;
import kz.rcez.appointment.dto.appointment.CancelAppointmentRequest;
import kz.rcez.appointment.dto.appointment.CompleteAppointmentRequest;
import kz.rcez.appointment.dto.appointment.CreateAppointmentRequest;
import kz.rcez.appointment.entity.Appointment;
import kz.rcez.appointment.entity.Doctor;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalTime;
import java.util.Optional;

import static kz.rcez.appointment.service.TestFixtures.FIXED_CLOCK;
import static kz.rcez.appointment.service.TestFixtures.TOMORROW;
import static kz.rcez.appointment.service.TestFixtures.YESTERDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppointmentService — запись на приём")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private PatientService patientService;
    @Mock
    private DoctorService doctorService;
    @Mock
    private CurrentUser currentUser;

    private AppointmentService appointmentService;

    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        Clock clock = FIXED_CLOCK;
        appointmentService = new AppointmentService(appointmentRepository, timeSlotRepository, appointmentMapper,
                patientService, doctorService, currentUser, clock);

        doctor = TestFixtures.doctor(1L);
        patient = TestFixtures.patient(10L);

        lenient().when(currentUser.username()).thenReturn("registrar");
        lenient().when(patientService.getEntity(10L)).thenReturn(patient);
        lenient().when(appointmentMapper.toResponse(any())).thenReturn(sampleResponse());
        lenient().when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static AppointmentResponse sampleResponse() {
        return new AppointmentResponse(1L, 10L, "900101300123", "Нурланов Асхат",
                1L, "Ахметов Данияр Серикович", null, "312",
                100L, TOMORROW, LocalTime.of(9, 0), LocalTime.of(9, 15),
                AppointmentStatus.SCHEDULED, null, null, null, "registrar", null);
    }

    @Nested
    @DisplayName("Создание записи")
    class Create {

        @Test
        @DisplayName("занимает свободный слот и создаёт запись в статусе SCHEDULED")
        void booksFreeSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.FREE);
            when(timeSlotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));
            when(appointmentRepository.existsActiveForPatientAndDoctorOnDate(10L, 1L, TOMORROW)).thenReturn(false);

            appointmentService.create(new CreateAppointmentRequest(10L, 100L, "Головная боль"));

            assertThat(slot.getStatus())
                    .as("слот должен стать занятым в той же транзакции")
                    .isEqualTo(SlotStatus.BOOKED);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            Appointment saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
            assertThat(saved.getPatient()).isEqualTo(patient);
            assertThat(saved.getDoctor()).isEqualTo(doctor);
            assertThat(saved.getComplaint()).isEqualTo("Головная боль");
            assertThat(saved.getCreatedBy()).isEqualTo("registrar");
        }

        @Test
        @DisplayName("отклоняет уже занятый слот")
        void rejectsBookedSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            when(timeSlotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(10L, 100L, null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("уже занят");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("отклоняет заблокированный слот")
        void rejectsBlockedSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BLOCKED);
            when(timeSlotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(10L, 100L, null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("заблокирован");
        }

        @Test
        @DisplayName("отклоняет прошедший слот")
        void rejectsPastSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, YESTERDAY, LocalTime.of(9, 0), SlotStatus.FREE);
            when(timeSlotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(10L, 100L, null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("прошедший");

            assertThat(slot.getStatus()).isEqualTo(SlotStatus.FREE);
        }

        @Test
        @DisplayName("отклоняет повторную запись к тому же врачу в тот же день")
        void rejectsDuplicate() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.FREE);
            when(timeSlotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));
            when(appointmentRepository.existsActiveForPatientAndDoctorOnDate(10L, 1L, TOMORROW)).thenReturn(true);

            assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(10L, 100L, null)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("активная запись");
        }

        @Test
        @DisplayName("возвращает 404, если слот не найден")
        void rejectsMissingSlot() {
            when(timeSlotRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> appointmentService.create(new CreateAppointmentRequest(10L, 404L, null)))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Отмена записи")
    class Cancel {

        @Test
        @DisplayName("освобождает слот и сохраняет причину отмены")
        void releasesSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            Appointment appointment = appointment(slot, AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            appointmentService.cancel(1L, new CancelAppointmentRequest("Пациент перенёс визит"));

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment.getCancelReason()).isEqualTo("Пациент перенёс визит");
            assertThat(appointment.getClosedAt()).isNotNull();
            assertThat(slot.getStatus())
                    .as("освобождённый слот снова доступен для записи")
                    .isEqualTo(SlotStatus.FREE);
        }

        @Test
        @DisplayName("не отменяет уже закрытую запись")
        void rejectsTerminalAppointment() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            Appointment appointment = appointment(slot, AppointmentStatus.COMPLETED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.cancel(1L, new CancelAppointmentRequest("причина")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("COMPLETED");

            assertThat(slot.getStatus())
                    .as("слот завершённого приёма остаётся занятым")
                    .isEqualTo(SlotStatus.BOOKED);
        }
    }

    @Nested
    @DisplayName("Завершение приёма")
    class Complete {

        @Test
        @DisplayName("переводит запись в COMPLETED, слот остаётся занятым")
        void completesAppointment() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            Appointment appointment = appointment(slot, AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            appointmentService.complete(1L, new CompleteAppointmentRequest("ОРВИ"));

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            assertThat(appointment.getConclusion()).isEqualTo("ОРВИ");
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
            verify(doctorService).requireManageAccess(doctor);
        }

        @Test
        @DisplayName("отметка о неявке закрывает запись, не освобождая слот")
        void marksNoShow() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            Appointment appointment = appointment(slot, AppointmentStatus.SCHEDULED);
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

            appointmentService.markNoShow(1L);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
            assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        }
    }

    private Appointment appointment(TimeSlot slot, AppointmentStatus status) {
        Appointment appointment = Appointment.builder()
                .timeSlot(slot)
                .patient(patient)
                .doctor(doctor)
                .status(status)
                .build();
        appointment.setId(1L);
        return appointment;
    }
}
