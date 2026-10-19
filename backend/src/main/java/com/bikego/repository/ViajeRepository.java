package com.bikego.repository;

import com.bikego.domain.EstadoViaje;
import com.bikego.domain.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    boolean existsByUsuarioIdAndEstado(String usuarioId, EstadoViaje estado);
    java.util.List<Viaje> findByUsuarioIdOrderByHoraInicioDesc(String usuarioId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Viaje v where v.idViaje = :id")
    java.util.Optional<Viaje> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
