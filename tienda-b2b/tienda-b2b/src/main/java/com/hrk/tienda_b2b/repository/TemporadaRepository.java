package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemporadaRepository extends JpaRepository<Temporada, Long> {
    Optional<Temporada> findByNombreIgnoreCase(String nombre);
}


