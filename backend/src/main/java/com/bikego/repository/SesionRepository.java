package com.bikego.repository;

import com.bikego.domain.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, Long> {
    Optional<Sesion> findByTokenHash(String tokenHash);
}
