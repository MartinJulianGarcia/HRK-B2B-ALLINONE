package com.hrk.tienda_b2b.repository;
import com.hrk.tienda_b2b.model.ProductoVariante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.stereotype.Repository;

public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {
    Optional<ProductoVariante> findBySku(String sku);
    
    @Modifying
    @Query("DELETE FROM ProductoVariante v WHERE v.producto.id = :productoId")
    void deleteByProductoId(@Param("productoId") Long productoId);
}