package kz.rcez.appointment.repository;

import kz.rcez.appointment.entity.Appointment;
import kz.rcez.appointment.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByTimeSlotIdAndStatus(Long timeSlotId, AppointmentStatus status);

    @Query("""
            select a from Appointment a
            join fetch a.doctor d
            join fetch a.timeSlot s
            where a.patient.id = :patientId
              and (:status is null or a.status = :status)
            order by s.slotDate desc, s.startTime desc
            """)
    Page<Appointment> findPatientHistory(@Param("patientId") Long patientId,
                                         @Param("status") AppointmentStatus status,
                                         Pageable pageable);

    @Query("""
            select a from Appointment a
            join fetch a.patient p
            join fetch a.timeSlot s
            where a.doctor.id = :doctorId
              and s.slotDate = :date
              and (:status is null or a.status = :status)
            order by s.startTime
            """)
    List<Appointment> findDoctorDaySchedule(@Param("doctorId") Long doctorId,
                                            @Param("date") LocalDate date,
                                            @Param("status") AppointmentStatus status);

    /** Активная запись того же пациента к тому же врачу на ту же дату — защита от дублей. */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.patient.id = :patientId
              and a.doctor.id = :doctorId
              and a.timeSlot.slotDate = :date
              and a.status = kz.rcez.appointment.entity.enums.AppointmentStatus.SCHEDULED
            """)
    boolean existsActiveForPatientAndDoctorOnDate(@Param("patientId") Long patientId,
                                                  @Param("doctorId") Long doctorId,
                                                  @Param("date") LocalDate date);
}
