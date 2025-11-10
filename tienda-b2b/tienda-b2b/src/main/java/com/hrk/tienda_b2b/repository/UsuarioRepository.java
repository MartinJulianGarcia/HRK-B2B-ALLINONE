package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.TipoUsuario;
import com.hrk.tienda_b2b.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByCuit(String cuit);

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    List<Usuario> findByTipoUsuario(TipoUsuario tipoUsuario);

    boolean existsByTipoUsuarioAndActivoTrue(TipoUsuario tipoUsuario);

    Optional<Usuario> findFirstByActivoTrueOrderByFechaCreacionAsc();

    Optional<Usuario> findFirstByActivoTrueOrderByIdAsc();

    List<Usuario> findByActivoTrue();

    boolean existsByEmail(String email);

    boolean existsByCuit(String cuit);
}