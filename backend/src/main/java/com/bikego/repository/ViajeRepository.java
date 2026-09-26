package com.bikego.repository;

import com.bikego.domain.EstadoViaje;
import com.bikego.domain.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {

    boolean existsByUsuarioIdAndEstado(String usuarioId, EstadoViaje estado);
}
