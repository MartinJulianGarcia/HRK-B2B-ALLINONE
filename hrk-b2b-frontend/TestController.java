package com.hrk.tienda_b2b.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        System.out.println("🔵 [TEST] Endpoint ping llamado");
        return "Pong - Backend funcionando";
    }

    @PostMapping("/echo")
    public String echo(@RequestBody String data) {
        System.out.println("🔵 [TEST] Endpoint echo llamado con datos: " + data);
        return "Echo: " + data;
    }
}
