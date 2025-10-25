package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);
}
