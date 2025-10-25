package com.hrk.tienda_b2b.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.*;
import lombok.*;

import com.hrk.tienda_b2b.model.TipoProducto;




@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_razon_social", nullable = false)
    private String nombreRazonSocial;

    @Column(name = "cuit", nullable = false, unique = true)
    private String cuit;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // Se hasheará con BCrypt

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "activo")
    private Boolean activo = true;

    // Constructor para registro
    public Usuario(String nombreRazonSocial, String cuit, String email, String password, TipoUsuario tipoUsuario) {
        this.nombreRazonSocial = nombreRazonSocial;
        this.cuit = cuit;
        this.email = email;
        this.password = password; // Se hasheará en el service
        this.tipoUsuario = tipoUsuario;
        this.fechaCreacion = LocalDateTime.now();
        this.activo = true;
    }
}