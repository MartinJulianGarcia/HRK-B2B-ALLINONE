package com.hrk.tienda_b2b.repository;
import com.hrk.tienda_b2b.model.DetallePedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> { }