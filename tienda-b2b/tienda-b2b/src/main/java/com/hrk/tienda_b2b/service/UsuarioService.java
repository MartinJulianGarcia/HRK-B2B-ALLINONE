package com.hrk.tienda_b2b.service;

import com.hrk.tienda_b2b.model.Usuario;

import java.util.List;
import java.util.Optional;


import com.hrk.tienda_b2b.dto.LoginRequest;
import com.hrk.tienda_b2b.dto.RegisterRequest;
import com.hrk.tienda_b2b.model.TipoUsuario;
import com.hrk.tienda_b2b.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    Usuario registrar(RegisterRequest request);

    Usuario login(LoginRequest request);

    Optional<Usuario> obtenerPorEmail(String email);

    Optional<Usuario> obtenerPorId(Long id);

    List<Usuario> obtenerTodos();

    List<Usuario> obtenerPorTipo(TipoUsuario tipo);

    Usuario actualizar(Usuario usuario);

    void eliminar(Long id);

    boolean existeEmail(String email);

    boolean existeCuit(String cuit);
}