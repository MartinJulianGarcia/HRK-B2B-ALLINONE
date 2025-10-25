package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.ProductoVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Long> {
}
