package com.hrk.tienda_b2b.controller;



import com.hrk.tienda_b2b.model.Pedido;
import com.hrk.tienda_b2b.service.CarritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    //public CarritoController(CarritoService carritoService) {
    //    this.carritoService = carritoService;
    //}

    @PostMapping("/crear")
    public ResponseEntity<Long> crear(@RequestParam Long clienteId) {
        return ResponseEntity.ok(carritoService.crearCarrito(clienteId));
    }

    @PostMapping("/{carritoId}/items")
    public ResponseEntity<Void> agregarItem(@PathVariable Long carritoId,
                                            @RequestParam Long varianteId,
                                            @RequestParam int cantidad) {
        carritoService.agregarItem(carritoId, varianteId, cantidad);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> quitarItem(@PathVariable Long itemId) {
        carritoService.quitarItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{carritoId}/vaciar")
    public ResponseEntity<Void> vaciar(@PathVariable Long carritoId) {
        carritoService.vaciar(carritoId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{carritoId}/a-pedido")
    public ResponseEntity<Pedido> convertir(@PathVariable Long carritoId) {
        return ResponseEntity.ok(carritoService.convertirACreatePedido(carritoId));
    }
}