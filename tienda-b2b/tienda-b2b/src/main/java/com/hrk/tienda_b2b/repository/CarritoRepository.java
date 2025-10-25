package com.hrk.tienda_b2b.repository;




import com.hrk.tienda_b2b.model.Carrito;
import com.hrk.tienda_b2b.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    List<Carrito> findByClienteId(Long clienteId); // Mantener por compatibilidad

    // NUEVO: Buscar por Usuario
    List<Carrito> findByUsuario(Usuario usuario);

    Optional<Carrito> findByUsuarioAndFechaCreacionBetween(Usuario usuario, LocalDateTime inicio, LocalDateTime fin);
}