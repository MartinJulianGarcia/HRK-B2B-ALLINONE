package com.hrk.tienda_b2b.controller;

import com.hrk.tienda_b2b.dto.TemporadaRequest;
import com.hrk.tienda_b2b.dto.TemporadaResponseDTO;
import com.hrk.tienda_b2b.model.Temporada;
import com.hrk.tienda_b2b.service.TemporadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/temporadas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TemporadaController {

    private final TemporadaService temporadaService;

    @GetMapping
    public ResponseEntity<List<TemporadaResponseDTO>> listarTemporadas() {
        List<TemporadaResponseDTO> temporadas = temporadaService.listarTodas().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(temporadas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemporadaResponseDTO> obtenerPorId(@PathVariable Long id) {
        Temporada temporada = temporadaService.obtenerPorId(id);
        return ResponseEntity.ok(mapearADTO(temporada));
    }

    @PostMapping
    public ResponseEntity<TemporadaResponseDTO> crearTemporada(@RequestBody TemporadaRequest request) {
        Temporada temporada = temporadaService.crearTemporada(request);
        return ResponseEntity.ok(mapearADTO(temporada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemporadaResponseDTO> actualizarTemporada(@PathVariable Long id,
                                                                    @RequestBody TemporadaRequest request) {
        Temporada temporada = temporadaService.actualizarTemporada(id, request);
        return ResponseEntity.ok(mapearADTO(temporada));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTemporada(@PathVariable Long id) {
        temporadaService.eliminarTemporada(id);
        return ResponseEntity.noContent().build();
    }

    private TemporadaResponseDTO mapearADTO(Temporada temporada) {
        return TemporadaResponseDTO.builder()
                .id(temporada.getId())
                .nombre(temporada.getNombre())
                .productoIds(
                        temporada.getProductos().stream()
                                .map(producto -> producto.getId())
                                .collect(Collectors.toList())
                )
                .build();
    }
}


