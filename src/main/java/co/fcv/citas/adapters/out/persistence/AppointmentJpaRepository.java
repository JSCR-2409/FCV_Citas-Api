package co.fcv.citas.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data JPA de la tabla de citas. */
interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long> {
}
