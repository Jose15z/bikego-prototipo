package com.bikego.repository;

import com.bikego.domain.Estacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstacionRepository extends JpaRepository<Estacion, String> { }
