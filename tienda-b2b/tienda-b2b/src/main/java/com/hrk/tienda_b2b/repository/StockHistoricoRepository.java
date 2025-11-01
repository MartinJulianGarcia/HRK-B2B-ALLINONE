package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.StockHistorico;
import com.hrk.tienda_b2b.model.ProductoVariante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoricoRepository extends JpaRepository<StockHistorico, Long> {
    
    List<StockHistorico> findByVarianteOrderByFechaAsc(ProductoVariante variante);
    
    // Obtener el stock acumulado final de una variante (el último registro)
    StockHistorico findFirstByVarianteOrderByFechaDesc(ProductoVariante variante);
}

