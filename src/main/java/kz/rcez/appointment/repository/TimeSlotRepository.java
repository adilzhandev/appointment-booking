package kz.rcez.appointment.repository;

import jakarta.persistence.LockModeType;
import kz.rcez.appointment.entity.TimeSlot;
import kz.rcez.appointment.entity.enums.Specialty;
import kz.rcez.appointment.entity.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * Слот под пессимистичной блокировкой — используется при создании записи,
     * чтобы два регистратора не заняли один и тот же слот.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TimeSlot s where s.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);

    List<TimeSlot> findByDoctorIdAndSlotDateOrderByStartTime(Long doctorId, LocalDate slotDate);

    /** Существует ли у врача слот, пересекающийся с интервалом [from, to) в указанный день. */
    @Query("""
            select count(s) > 0 from TimeSlot s
            where s.doctor.id = :doctorId
              and s.slotDate = :date
              and s.startTime < :to
              and :from < s.endTime
              and (:excludeId is null or s.id <> :excludeId)
            """)
    boolean existsOverlapping(@Param("doctorId") Long doctorId,
                              @Param("date") LocalDate date,
                              @Param("from") LocalTime from,
                              @Param("to") LocalTime to,
                              @Param("excludeId") Long excludeId);

    /** Поиск свободных слотов по врачу / специальности / диапазону дат. */
    @Query("""
            select s from TimeSlot s
            join s.doctor d
            where s.status = :status
              and (:doctorId is null or d.id = :doctorId)
              and (:specialty is null or d.specialty = :specialty)
              and s.slotDate between :dateFrom and :dateTo
            order by s.slotDate, s.startTime
            """)
    List<TimeSlot> searchSlots(@Param("status") SlotStatus status,
                               @Param("doctorId") Long doctorId,
                               @Param("specialty") Specialty specialty,
                               @Param("dateFrom") LocalDate dateFrom,
                               @Param("dateTo") LocalDate dateTo);
}
