package com.hrk.tienda_b2b.controller;


import com.hrk.tienda_b2b.model.Pedido;
import com.hrk.tienda_b2b.service.DevolucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devoluciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DevolucionController {

    private final DevolucionService devolucionService;

    @PostMapping("/crear")
    public ResponseEntity<Pedido> crear(@RequestParam Long clienteId,
                                        @RequestParam(required = false) Long pedidoOrigenId) {
        return ResponseEntity.ok(devolucionService.crearDevolucion(clienteId, pedidoOrigenId));
    }

    @PostMapping("/{devId}/items")
    public ResponseEntity<Pedido> agregarItem(@PathVariable Long devId,
                                              @RequestParam Long varianteId,
                                              @RequestParam int cantidad,
                                              @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(devolucionService.agregarItem(devId, varianteId, cantidad, motivo));
    }

    @PostMapping("/{devId}/aprobar-apta")
    public ResponseEntity<Pedido> aprobarApta(@PathVariable Long devId) {
        return ResponseEntity.ok(devolucionService.aprobarApta(devId));
    }

    @PostMapping("/{devId}/aprobar-scrap")
    public ResponseEntity<Pedido> aprobarScrap(@PathVariable Long devId) {
        return ResponseEntity.ok(devolucionService.aprobarScrap(devId));
    }

    @GetMapping("/{devId}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long devId) {
        // Si querés un DTO, mapealo aquí.

        return ResponseEntity.ok(/* repo o service */ devolucionService.findById(devId)); // <-- reemplazar por find
    }
}