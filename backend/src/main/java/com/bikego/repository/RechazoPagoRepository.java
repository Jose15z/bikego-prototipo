package com.bikego.repository;

import com.bikego.domain.RechazoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechazoPagoRepository extends JpaRepository<RechazoPago, Long> {

    List<RechazoPago> findByUsuarioId(String usuarioId);
}
