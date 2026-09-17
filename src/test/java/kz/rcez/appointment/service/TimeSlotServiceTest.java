package kz.rcez.appointment.service;

import kz.rcez.appointment.dto.slot.CreateSlotRequest;
import kz.rcez.appointment.dto.slot.GenerateScheduleRequest;
import kz.rcez.appointment.dto.slot.GenerateScheduleResponse;
import kz.rcez.appointment.entity.Doctor;
import kz.rcez.appointment.entity.TimeSlot;
import kz.rcez.appointment.entity.enums.SlotStatus;
import kz.rcez.appointment.exception.AccessDeniedAppException;
import kz.rcez.appointment.exception.BadRequestException;
import kz.rcez.appointment.exception.ConflictException;
import kz.rcez.appointment.mapper.TimeSlotMapper;
import kz.rcez.appointment.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static kz.rcez.appointment.service.TestFixtures.FIXED_CLOCK;
import static kz.rcez.appointment.service.TestFixtures.TOMORROW;
import static kz.rcez.appointment.service.TestFixtures.YESTERDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TimeSlotService — расписание врача")
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private TimeSlotMapper timeSlotMapper;
    @Mock
    private DoctorService doctorService;

    private TimeSlotService timeSlotService;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        Clock clock = FIXED_CLOCK;
        timeSlotService = new TimeSlotService(timeSlotRepository, timeSlotMapper, doctorService, clock);

        doctor = TestFixtures.doctor(1L);
        lenient().when(doctorService.getEntity(1L)).thenReturn(doctor);
        lenient().when(timeSlotRepository.save(any(TimeSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(timeSlotRepository.saveAll(any()))
                .thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));
    }

    @Nested
    @DisplayName("Создание слота")
    class CreateSlot {

        @Test
        @DisplayName("создаёт слот, если пересечений нет")
        void createsSlot() {
            when(timeSlotRepository.existsOverlapping(eq(1L), eq(TOMORROW), any(), any(), isNull()))
                    .thenReturn(false);

            timeSlotService.createSlot(1L,
                    new CreateSlotRequest(TOMORROW, LocalTime.of(9, 0), LocalTime.of(9, 15)));

            verify(timeSlotRepository).save(any(TimeSlot.class));
        }

        @Test
        @DisplayName("отклоняет слот, пересекающийся с существующим")
        void rejectsOverlappingSlot() {
            when(timeSlotRepository.existsOverlapping(eq(1L), eq(TOMORROW), any(), any(), isNull()))
                    .thenReturn(true);

            assertThatThrownBy(() -> timeSlotService.createSlot(1L,
                    new CreateSlotRequest(TOMORROW, LocalTime.of(9, 10), LocalTime.of(9, 25))))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("пересекается");

            verify(timeSlotRepository, never()).save(any());
        }

        @Test
        @DisplayName("отклоняет слот в прошлом")
        void rejectsPastSlot() {
            assertThatThrownBy(() -> timeSlotService.createSlot(1L,
                    new CreateSlotRequest(YESTERDAY, LocalTime.of(9, 0), LocalTime.of(9, 15))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("в прошлом");
        }

        @Test
        @DisplayName("отклоняет интервал, где начало не раньше окончания")
        void rejectsInvalidInterval() {
            assertThatThrownBy(() -> timeSlotService.createSlot(1L,
                    new CreateSlotRequest(TOMORROW, LocalTime.of(10, 0), LocalTime.of(9, 0))))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("раньше времени окончания");
        }

        @Test
        @DisplayName("не даёт врачу создать слот в чужом расписании")
        void rejectsForeignSchedule() {
            doThrowAccessDenied();

            assertThatThrownBy(() -> timeSlotService.createSlot(1L,
                    new CreateSlotRequest(TOMORROW, LocalTime.of(9, 0), LocalTime.of(9, 15))))
                    .isInstanceOf(AccessDeniedAppException.class);
        }

        private void doThrowAccessDenied() {
            org.mockito.Mockito.doThrow(new AccessDeniedAppException("чужое расписание"))
                    .when(doctorService).requireManageAccess(doctor);
        }
    }

    @Nested
    @DisplayName("Генерация расписания")
    class Generate {

        @Test
        @DisplayName("нарезает рабочий день на слоты и вычитает перерыв")
        void generatesDay() {
            when(timeSlotRepository.existsOverlapping(anyLong(), any(), any(), any(), isNull())).thenReturn(false);

            GenerateScheduleResponse response = timeSlotService.generateSchedule(1L,
                    new GenerateScheduleRequest(TOMORROW, TOMORROW,
                            LocalTime.of(9, 0), LocalTime.of(12, 0), 30,
                            LocalTime.of(10, 0), LocalTime.of(10, 30), false));

            // 09:00–12:00 по 30 минут = 6 слотов, минус один в перерыве 10:00–10:30
            assertThat(response.created()).isEqualTo(5);
            assertThat(response.skipped()).isZero();
        }

        @Test
        @DisplayName("пропускает интервалы, пересекающиеся с уже существующими слотами")
        void skipsOverlapping() {
            when(timeSlotRepository.existsOverlapping(anyLong(), any(), any(), any(), isNull()))
                    .thenReturn(true, false, false, false);

            GenerateScheduleResponse response = timeSlotService.generateSchedule(1L,
                    new GenerateScheduleRequest(TOMORROW, TOMORROW,
                            LocalTime.of(9, 0), LocalTime.of(11, 0), 30,
                            null, null, false));

            assertThat(response.created()).isEqualTo(3);
            assertThat(response.skipped()).isEqualTo(1);
        }

        @Test
        @DisplayName("пропускает выходные дни")
        void skipsWeekends() {
            when(timeSlotRepository.existsOverlapping(anyLong(), any(), any(), any(), isNull())).thenReturn(false);

            // 19 сентября 2026 — суббота, 20-е — воскресенье, 21-е — понедельник
            GenerateScheduleResponse response = timeSlotService.generateSchedule(1L,
                    new GenerateScheduleRequest(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 21),
                            LocalTime.of(9, 0), LocalTime.of(10, 0), 30,
                            null, null, true));

            assertThat(response.created())
                    .as("слоты создаются только на понедельник")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("отклоняет период, где дата окончания раньше даты начала")
        void rejectsInvertedRange() {
            assertThatThrownBy(() -> timeSlotService.generateSchedule(1L,
                    new GenerateScheduleRequest(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 21),
                            LocalTime.of(9, 0), LocalTime.of(17, 0), 15, null, null, true)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("раньше даты начала");
        }

        @Test
        @DisplayName("отклоняет слишком длинный период")
        void rejectsTooLongRange() {
            assertThatThrownBy(() -> timeSlotService.generateSchedule(1L,
                    new GenerateScheduleRequest(TOMORROW, TOMORROW.plusDays(100),
                            LocalTime.of(9, 0), LocalTime.of(17, 0), 15, null, null, true)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("не более чем");
        }
    }

    @Nested
    @DisplayName("Блокировка и удаление")
    class BlockAndDelete {

        @Test
        @DisplayName("нельзя заблокировать слот с активной записью")
        void rejectsBlockingBookedSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            when(timeSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> timeSlotService.block(100L))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("удаление занятого слота запрещено")
        void rejectsDeletingBookedSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.BOOKED);
            when(timeSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> timeSlotService.delete(100L))
                    .isInstanceOf(ConflictException.class);
            assertThat(slot.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("свободный слот помечается удалённым (soft delete)")
        void softDeletesFreeSlot() {
            TimeSlot slot = TestFixtures.slot(100L, doctor, TOMORROW, LocalTime.of(9, 0), SlotStatus.FREE);
            when(timeSlotRepository.findById(100L)).thenReturn(Optional.of(slot));

            timeSlotService.delete(100L);

            assertThat(slot.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Поиск свободных слотов")
    class FindFree {

        @Test
        @DisplayName("не возвращает слоты, время которых уже прошло")
        void filtersOutPastSlots() {
            TimeSlot past = TestFixtures.slot(1L, doctor, LocalDate.of(2026, 9, 17), LocalTime.of(9, 0), SlotStatus.FREE);
            TimeSlot future = TestFixtures.slot(2L, doctor, LocalDate.of(2026, 9, 17), LocalTime.of(14, 0), SlotStatus.FREE);
            when(timeSlotRepository.searchSlots(eq(SlotStatus.FREE), any(), any(), any(), any()))
                    .thenReturn(List.of(past, future));

            timeSlotService.findFreeSlots(1L, null, LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 17));

            verify(timeSlotMapper).toResponse(future);
            verify(timeSlotMapper, never()).toResponse(past);
        }

        @Test
        @DisplayName("отклоняет перевёрнутый диапазон дат")
        void rejectsInvertedRange() {
            assertThatThrownBy(() -> timeSlotService.findFreeSlots(null, null,
                    LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 20)))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
