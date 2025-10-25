package com.hrk.tienda_b2b.repository;

import com.hrk.tienda_b2b.model.Categoria;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.model.TipoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // Cambiado de String a Categoria (enum)
    List<Producto> findByCategoria(Categoria categoria);

    List<Producto> findByTipo(TipoProducto tipo);

    // Cambiado de String a Categoria (enum)
    List<Producto> findByCategoriaAndTipo(Categoria categoria, TipoProducto tipo);
}
