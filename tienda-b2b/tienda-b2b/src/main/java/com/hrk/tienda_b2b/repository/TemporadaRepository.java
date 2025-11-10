package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.Temporada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface TemporadaRepository extends JpaRepository<Temporada, Long> {
    Optional<Temporada> findByNombreIgnoreCase(String nombre);

    Optional<Temporada> findByActivaTrue();

    @Query("SELECT t FROM Temporada t LEFT JOIN FETCH t.productos WHERE t.activa = true")
    Optional<Temporada> findActiveWithProductos();
}


