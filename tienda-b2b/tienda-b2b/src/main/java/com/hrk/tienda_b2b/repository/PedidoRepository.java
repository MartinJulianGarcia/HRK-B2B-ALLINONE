package com.hrk.tienda_b2b.repository;
import com.hrk.tienda_b2b.model.Pedido;


import com.hrk.tienda_b2b.model.Producto;

import com.hrk.tienda_b2b.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByIdAndTipo(Long id, TipoDocumento tipo);
    List<Pedido> findAllByTipoOrderByFechaDesc(TipoDocumento tipo);
    List<Pedido> findAllByClienteIdAndTipoOrderByFechaDesc(Long clienteId, TipoDocumento tipo);

    List<Pedido> findByClienteId(Long clienteId);
}