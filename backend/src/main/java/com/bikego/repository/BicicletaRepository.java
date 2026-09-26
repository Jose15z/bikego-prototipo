package com.bikego.repository;

import com.bikego.domain.Bicicleta;
import com.bikego.domain.EstadoBicicleta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BicicletaRepository extends JpaRepository<Bicicleta, String> {

    List<Bicicleta> findByEstadoAndBateriaGreaterThanEqual(EstadoBicicleta estado, int bateria);

    /** Bloqueo pesimista: evita que dos usuarios alquilen la misma bicicleta al tiempo (CA-7). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bicicleta b where b.id = :id")
    Optional<Bicicleta> findByIdParaActualizar(@Param("id") String id);
}
