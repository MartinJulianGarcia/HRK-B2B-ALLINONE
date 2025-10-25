package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.dto.LoginRequest;
import com.hrk.tienda_b2b.dto.RegisterRequest;
import com.hrk.tienda_b2b.model.*;
import com.hrk.tienda_b2b.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.hrk.tienda_b2b.config.SimplePasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final SimplePasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Usuario registrar(RegisterRequest request) {
        // Validar que no exista email o CUIT
        if (existeEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (existeCuit(request.getCuit())) {
            throw new IllegalArgumentException("El CUIT ya está registrado");
        }

        // Hashear password
        String passwordHasheada = passwordEncoder.encode(request.getPassword());

        // Crear usuario
        Usuario usuario = Usuario.builder()
                .nombreRazonSocial(request.getNombreRazonSocial())
                .cuit(request.getCuit())
                .email(request.getEmail())
                .password(passwordHasheada)
                .tipoUsuario(TipoUsuario.CLIENTE) // Por defecto es CLIENTE
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .build();

        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        return usuario;
    }

    @Override
    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findByActivoTrue();
    }

    @Override
    public List<Usuario> obtenerPorTipo(TipoUsuario tipo) {
        return usuarioRepository.findByTipoUsuario(tipo);
    }

    @Override
    @Transactional
    public Usuario actualizar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Override
    public boolean existeCuit(String cuit) {
        return usuarioRepository.existsByCuit(cuit);
    }
}