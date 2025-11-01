package com.hrk.tienda_b2b.repository;
import com.hrk.tienda_b2b.model.DetallePedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    // Verificar si una variante tiene pedidos asociados
    boolean existsByVarianteId(Long varianteId);
    
    // Contar cantidad de pedidos para una variante
    @Query("SELECT COUNT(d) FROM DetallePedido d WHERE d.variante.id = :varianteId")
    long countByVarianteId(@Param("varianteId") Long varianteId);
    
    // Buscar detalles por pedido
    List<DetallePedido> findByPedidoId(Long pedidoId);
    
    // Buscar detalles por pedido con JOIN FETCH para evitar lazy loading
    @Query("SELECT d FROM DetallePedido d " +
           "LEFT JOIN FETCH d.variante v " +
           "LEFT JOIN FETCH v.producto p " +
           "WHERE d.pedido.id = :pedidoId")
    List<DetallePedido> findByPedidoIdWithRelations(@Param("pedidoId") Long pedidoId);
}