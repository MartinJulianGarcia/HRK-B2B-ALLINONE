package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.dto.TemporadaRequest;
import com.hrk.tienda_b2b.model.Producto;
import com.hrk.tienda_b2b.model.Temporada;
import com.hrk.tienda_b2b.repository.ProductoRepository;
import com.hrk.tienda_b2b.repository.TemporadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TemporadaService {

    private final TemporadaRepository temporadaRepository;
    private final ProductoRepository productoRepository;

    public List<Temporada> listarTodas() {
        return temporadaRepository.findAll();
    }

    public Temporada obtenerPorId(Long id) {
        return temporadaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Temporada no encontrada con id " + id));
    }

    @Transactional
    public Temporada crearTemporada(TemporadaRequest request) {
        validarNombre(request.getNombre());

        Temporada temporada = Temporada.builder()
                .nombre(request.getNombre().trim())
                .build();

        Set<Producto> productos = cargarProductosDesdeIds(request.getProductoIds());
        temporada.setProductos(productos);

        return temporadaRepository.save(temporada);
    }

    @Transactional
    public Temporada actualizarTemporada(Long id, TemporadaRequest request) {
        Temporada temporada = obtenerPorId(id);

        if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            String nuevoNombre = request.getNombre().trim();
            if (!nuevoNombre.equalsIgnoreCase(temporada.getNombre())) {
                validarNombre(nuevoNombre);
                temporada.setNombre(nuevoNombre);
            }
        }

        if (request.getProductoIds() != null) {
            Set<Producto> productos = cargarProductosDesdeIds(request.getProductoIds());
            temporada.setProductos(productos);
        }

        return temporadaRepository.save(temporada);
    }

    @Transactional
    public void eliminarTemporada(Long id) {
        Temporada temporada = obtenerPorId(id);
        temporada.getProductos().clear();
        temporadaRepository.delete(temporada);
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la temporada es obligatorio");
        }
        temporadaRepository.findByNombreIgnoreCase(nombre.trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una temporada con el nombre '" + nombre + "'");
                });
    }

    private Set<Producto> cargarProductosDesdeIds(List<Long> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Producto> productos = productoRepository.findAllById(productoIds);
        if (productos.size() != productoIds.size()) {
            throw new IllegalArgumentException("Algunos productos no existen o fueron eliminados");
        }
        return new HashSet<>(productos);
    }

    public List<Producto> obtenerProductosDeTemporada(Long temporadaId) {
        Temporada temporada = obtenerPorId(temporadaId);
        return new ArrayList<>(temporada.getProductos());
    }
}


