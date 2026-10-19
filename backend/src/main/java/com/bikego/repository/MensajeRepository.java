package com.bikego.repository;
import com.bikego.domain.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MensajeRepository extends JpaRepository<Mensaje,Long> {
    List<Mensaje> findByDestinatarioOrderByFechaDesc(String destinatario);
}
