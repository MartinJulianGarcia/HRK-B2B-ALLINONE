package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.model.Categoria;

import java.util.List;

public interface CategoriaService {
    List<Categoria> findAll();
    Categoria findById(Long id);
    Categoria save(Categoria categoria);
    void deleteById(Long id);
}