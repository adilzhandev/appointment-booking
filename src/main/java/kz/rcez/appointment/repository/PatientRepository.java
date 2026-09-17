package kz.rcez.appointment.repository;

import kz.rcez.appointment.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIin(String iin);

    boolean existsByIin(String iin);

    @Query("""
            select p from Patient p
            where (:q is null
                   or p.iin like concat('%', :q, '%')
                   or lower(concat(p.lastName, ' ', p.firstName)) like lower(concat('%', :q, '%')))
            """)
    Page<Patient> search(@Param("q") String q, Pageable pageable);
}
