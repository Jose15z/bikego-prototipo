package com.bikego.repository;
import com.bikego.domain.EventoFlota;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventoFlotaRepository extends JpaRepository<EventoFlota, Long> {
    java.util.List<EventoFlota> findAllByOrderByFechaDesc();
    boolean existsByBicicletaIdAndClaseAndEstado(String bicicleta, String clase, String estado);
}
