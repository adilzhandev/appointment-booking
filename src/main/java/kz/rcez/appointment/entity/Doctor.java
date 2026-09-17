package kz.rcez.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kz.rcez.appointment.entity.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** Врач медицинской организации. */
@Entity
@Table(name = "doctor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class Doctor extends BaseEntity {

    /** Учётная запись врача; может отсутствовать, если врач ещё не заведён в системе как пользователь. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", unique = true)
    private UserAccount userAccount;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false, length = 64)
    private Specialty specialty;

    @Column(name = "cabinet", length = 16)
    private String cabinet;

    @Column(name = "phone", length = 32)
    private String phone;

    /** Длительность приёма по умолчанию, минут — используется при генерации расписания. */
    @Column(name = "default_slot_minutes", nullable = false)
    @Builder.Default
    private Integer defaultSlotMinutes = 15;
}
