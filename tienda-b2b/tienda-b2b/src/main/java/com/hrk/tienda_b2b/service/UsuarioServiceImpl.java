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
import java.security.SecureRandom;

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
                .mustChangePassword(false)
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

        if (passwordEncoder.needsMigration(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            if (usuario.getMustChangePassword() == null) {
                usuario.setMustChangePassword(false);
            }
            usuario = usuarioRepository.save(usuario);
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

    @Override
    public boolean existeAdministradorActivo() {
        return usuarioRepository.existsByTipoUsuarioAndActivoTrue(TipoUsuario.ADMIN);
    }

    @Override
    public boolean puedeCambiarRolAAdmin(Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }

        if (existeAdministradorActivo()) {
            return false;
        }

        return usuarioRepository.findFirstByActivoTrueOrderByFechaCreacionAsc()
                .or(() -> usuarioRepository.findFirstByActivoTrueOrderByIdAsc())
                .map(usuario -> usuario.getId().equals(usuarioId))
                .orElse(false);
    }

    @Override
    @Transactional
    public Usuario actualizarPassword(Usuario usuario, String nuevaPassword, boolean mustChangePassword) {
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setMustChangePassword(mustChangePassword);
        return usuarioRepository.save(usuario);
    }

    @Override
    public String generarPasswordTemporal() {
        final String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%^&*";
        final int longitud = 12;
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            int index = random.nextInt(caracteres.length());
            builder.append(caracteres.charAt(index));
        }
        return builder.toString();
    }

    @Override
    @Transactional
    public Usuario cambiarPassword(Usuario usuario, String passwordActual, String nuevaPassword, boolean validarPasswordActual) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no válido");
        }
        if (nuevaPassword == null || nuevaPassword.trim().length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres");
        }

        if (validarPasswordActual) {
            if (passwordActual == null || passwordActual.isBlank()) {
                throw new IllegalArgumentException("Debes ingresar tu contraseña actual");
            }
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                throw new IllegalArgumentException("La contraseña actual no es correcta");
            }
        } else if (passwordActual != null && !passwordActual.isBlank()) {
            // Si no es obligatorio pero llega, validar y fallar si no coincide
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                throw new IllegalArgumentException("La contraseña actual no es correcta");
            }
        }

        if (passwordEncoder.matches(nuevaPassword, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual");
        }

        return actualizarPassword(usuario, nuevaPassword, false);
    }
}