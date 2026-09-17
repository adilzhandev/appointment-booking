package kz.rcez.appointment.repository;

import kz.rcez.appointment.entity.Doctor;
import kz.rcez.appointment.entity.enums.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserAccountId(Long userAccountId);

    @Query("""
            select d from Doctor d
            where (:specialty is null or d.specialty = :specialty)
              and (:q is null or lower(d.fullName) like lower(concat('%', :q, '%')))
            """)
    Page<Doctor> search(@Param("specialty") Specialty specialty,
                        @Param("q") String q,
                        Pageable pageable);
}
