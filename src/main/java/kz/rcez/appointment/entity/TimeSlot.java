package kz.rcez.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kz.rcez.appointment.entity.enums.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Слот в расписании врача. */
@Entity
@Table(name = "time_slot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class TimeSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private SlotStatus status = SlotStatus.FREE;

    public LocalDateTime getStartsAt() {
        return LocalDateTime.of(slotDate, startTime);
    }

    public LocalDateTime getEndsAt() {
        return LocalDateTime.of(slotDate, endTime);
    }

    public boolean isFree() {
        return status == SlotStatus.FREE;
    }

    /** Пересекается ли слот с интервалом [from, to) в тот же день. */
    public boolean overlaps(LocalTime from, LocalTime to) {
        return startTime.isBefore(to) && from.isBefore(endTime);
    }
}
