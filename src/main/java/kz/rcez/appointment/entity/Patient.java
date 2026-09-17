package kz.rcez.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import kz.rcez.appointment.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/** Пациент. */
@Entity
@Table(name = "patient")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class Patient extends BaseEntity {

    /** ИИН — 12 цифр, уникален. */
    @Column(name = "iin", nullable = false, unique = true, length = 12)
    private String iin;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "middle_name", length = 128)
    private String middleName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 16)
    private Gender gender;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "address", length = 512)
    private String address;

    public String getFullName() {
        return (lastName + " " + firstName + " " + (middleName == null ? "" : middleName)).trim();
    }
}
