import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, catchError } from 'rxjs/operators';
import { API_BASE_URL } from './backend-url';

export interface Usuario {
  id: number;
  nombreRazonSocial: string;
  email: string;
  cuit: string;
  tipoUsuario: 'CLIENTE' | 'ADMIN';
  fechaCreacion: string;
  activo: boolean;
  mustChangePassword?: boolean;
}

export interface Cliente {
  id: number;
  nombre: string;
  email: string;
  telefono?: string;
  direccion?: string;
  activo: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  nombreRazonSocial: string;
  cuit: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  usuario: Usuario;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject = new BehaviorSubject<Usuario | null>(null);
  private selectedClientSubject = new BehaviorSubject<Cliente | null>(null);
  private tokenSubject = new BehaviorSubject<string | null>(null);
  
  public currentUser$ = this.currentUserSubject.asObservable();
  public selectedClient$ = this.selectedClientSubject.asObservable();
  
  private readonly API_URL = API_BASE_URL;

  constructor(private http: HttpClient) {
    // Verificar si hay usuario logueado en localStorage (solo en el cliente)
    if (typeof window !== 'undefined' && window.localStorage) {
      const savedUser = localStorage.getItem('currentUser');
      const savedToken = localStorage.getItem('token');
      console.log('🔵 [AUTH SERVICE] Constructor - savedUser:', savedUser ? 'exists' : 'null');
      console.log('🔵 [AUTH SERVICE] Constructor - savedToken:', savedToken ? savedToken.substring(0, 20) + '...' : 'null');
      if (savedUser && savedToken) {
        try {
          const user = JSON.parse(savedUser);
          this.currentUserSubject.next(user);
          this.tokenSubject.next(savedToken);
          console.log('🔵 [AUTH SERVICE] Usuario y token cargados desde localStorage');
        } catch (error) {
          console.error('🔴 [AUTH SERVICE] Error al parsear usuario desde localStorage:', error);
          // Limpiar datos corruptos
          localStorage.removeItem('currentUser');
          localStorage.removeItem('token');
        }
      } else {
        console.log('🔵 [AUTH SERVICE] No hay usuario o token en localStorage');
      }
    }
  }

  // Mock data para desarrollo (ya no se usa, todo viene del backend)

  private mockClients: Cliente[] = [
    { id: 1, nombre: 'Distribuidora Norte', email: 'norte@dist.com', telefono: '011-1234-5678', direccion: 'Av. Corrientes 1234', activo: true },
    { id: 2, nombre: 'Mayorista Sur', email: 'sur@mayorista.com', telefono: '011-8765-4321', direccion: 'Av. Santa Fe 5678', activo: true },
    { id: 3, nombre: 'Comercial Este', email: 'este@comercial.com', telefono: '011-5555-1234', direccion: 'Av. Rivadavia 9999', activo: true }
  ];

  login(credentials: LoginRequest): Observable<Usuario> {
    console.log('🔵 [AUTH SERVICE] Intentando login con email:', credentials.email);
    return this.http.post<any>(`${this.API_URL}/auth/login`, credentials)
      .pipe(
        map(response => {
          console.log('🔵 [AUTH SERVICE] Respuesta del login:', response);
          
          // El backend puede devolver dos formatos:
          // 1. { token, usuario } (formato antiguo)
          // 2. { success, message, usuario, token, mustChangePassword } (formato nuevo)
          const token = response.token || response.data?.token;
          const usuario = response.usuario || response.data?.usuario;
          
          if (!token || !usuario) {
            console.error('🔴 [AUTH SERVICE] Respuesta de login inválida:', response);
            throw new Error('Respuesta de login inválida');
          }
          
          // Guardar token y usuario
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('token', token);
            localStorage.setItem('currentUser', JSON.stringify(usuario));
            console.log('🔵 [AUTH SERVICE] Token y usuario guardados en localStorage');
          }
          
          this.tokenSubject.next(token);
          this.currentUserSubject.next(usuario);
          console.log('🔵 [AUTH SERVICE] Login exitoso para usuario:', usuario.email);
          return usuario;
        }),
        catchError(error => {
          console.error('🔴 [AUTH SERVICE] Error en login:', error);
          console.error('🔴 [AUTH SERVICE] Error status:', error.status);
          console.error('🔴 [AUTH SERVICE] Error body:', error.error);
          
          let errorMessage = 'Credenciales inválidas';
          if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.message) {
            errorMessage = error.message;
          }
          
          return throwError(() => new Error(errorMessage));
        })
      );
  }

  register(userData: RegisterRequest): Observable<Usuario> {
    console.log('🔵 [AUTH SERVICE] Enviando petición de registro:', userData);
    console.log('🔵 [AUTH SERVICE] URL:', `${this.API_URL}/auth/register`);
    
    return this.http.post<any>(`${this.API_URL}/auth/register`, userData)
      .pipe(
        map(response => {
          console.log('🔵 [AUTH SERVICE] Respuesta del registro:', response);
          
          // El backend puede devolver dos formatos:
          // 1. { token, usuario } (formato antiguo)
          // 2. { success, message, usuario, token, mustChangePassword } (formato nuevo)
          const token = response.token || response.data?.token;
          const usuario = response.usuario || response.data?.usuario;
          
          // ✅ CRÍTICO: Validar que response no sea null
          if (!token || !usuario) {
            console.error('🔴 [AUTH SERVICE] Respuesta de registro inválida:', response);
            throw new Error('Respuesta de registro incompleta o inválida.');
          }
          
          // Guardar token y usuario
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('token', token);
            localStorage.setItem('currentUser', JSON.stringify(usuario));
            console.log('🔵 [AUTH SERVICE] Token y usuario guardados en localStorage');
          }
          
          this.tokenSubject.next(token);
          this.currentUserSubject.next(usuario);
          console.log('🔵 [AUTH SERVICE] Registro exitoso para usuario:', usuario.email);
          return usuario;
        }),
        catchError(error => {
          console.error('🔴 [AUTH SERVICE] Error en registro:', error);
          console.error('🔴 [AUTH SERVICE] Error status:', error.status);
          console.error('🔴 [AUTH SERVICE] Error body:', error.error);
          
          let errorMessage = 'Error al registrar usuario';
          
          if (error.error?.message) {
            errorMessage = error.error.message;
          } else if (error.status === 400) {
            errorMessage = 'Datos inválidos. Verifica que el email y CUIT no estén registrados.';
          }
          
          return throwError(() => new Error(errorMessage));
        })
      );
  }

  recuperarContraseña(email: string): Observable<{ success: boolean; message: string }> {
    console.log('🔵 [AUTH SERVICE] Solicitando recuperación de contraseña para:', email);
    return this.http.post<{ success: boolean; message: string }>(
      `${this.API_URL}/auth/recuperar-contraseña`,
      { email }
    ).pipe(
      catchError((error: any) => {
        console.error('🔴 [AUTH SERVICE] Error al recuperar contraseña:', error);
        const errorMessage = error.error?.message || 'Error al solicitar recuperación de contraseña';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  actualizarPerfil(usuarioId: number, datos: { nombreRazonSocial?: string; cuit?: string }): Observable<Usuario> {
    console.log('🔵 [AUTH SERVICE] Actualizando perfil para usuario:', usuarioId, datos);
    return this.http.put<Usuario>(`${this.API_URL}/usuarios/${usuarioId}`, datos, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(usuarioActualizado => {
        // Actualizar el usuario en el estado actual y localStorage
        const currentUser = this.getCurrentUser();
        if (currentUser && currentUser.id === usuarioActualizado.id) {
          this.currentUserSubject.next(usuarioActualizado);
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('currentUser', JSON.stringify(usuarioActualizado));
          }
        }
        return usuarioActualizado;
      }),
      catchError((error: any) => {
        console.error('🔴 [AUTH SERVICE] Error al actualizar perfil:', error);
        const errorMessage = error.error?.error || error.error?.message || 'Error al actualizar el perfil';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  logout(): void {
    this.currentUserSubject.next(null);
    this.selectedClientSubject.next(null);
    this.tokenSubject.next(null);
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('currentUser');
      localStorage.removeItem('selectedClient');
      localStorage.removeItem('token');
    }
  }

  getCurrentUser(): Usuario | null {
    return this.currentUserSubject.value;
  }

  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  isAdmin(): boolean {
    return this.currentUserSubject.value?.tipoUsuario === 'ADMIN';
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }

  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    const headers: { [key: string]: string } = {
      'Content-Type': 'application/json'
    };
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    
    return new HttpHeaders(headers);
  }

  // Método para cambiar el rol del usuario
  cambiarRol(usuarioId: number, nuevoRol: 'CLIENTE' | 'ADMIN'): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.API_URL}/usuarios/${usuarioId}/rol`, 
      { tipoUsuario: nuevoRol }, 
      { headers: this.getAuthHeaders() }
    ).pipe(
      map(usuarioActualizado => {
        // Actualizar el usuario actual si es el mismo
        const currentUser = this.getCurrentUser();
        if (currentUser && currentUser.id === usuarioActualizado.id) {
          this.currentUserSubject.next(usuarioActualizado);
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('currentUser', JSON.stringify(usuarioActualizado));
          }
        }
        return usuarioActualizado;
      }),
      catchError(error => {
        console.error('Error al cambiar rol:', error);
        return throwError(() => new Error('Error al cambiar rol'));
      })
    );
  }

  cambiarPassword(usuarioId: number, payload: { passwordActual?: string; nuevaPassword: string; confirmarPassword?: string }): Observable<Usuario> {
    return this.http.put<{ success: boolean; usuario: Usuario }>(
      `${this.API_URL}/usuarios/${usuarioId}/password`,
      payload,
      { headers: this.getAuthHeaders() }
    ).pipe(
      map(response => {
        if (!response || !response.success || !response.usuario) {
          throw new Error('Respuesta inválida al cambiar la contraseña');
        }
        const usuarioActualizado = response.usuario;
        const currentUser = this.getCurrentUser();
        if (currentUser && currentUser.id === usuarioActualizado.id) {
          this.currentUserSubject.next(usuarioActualizado);
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('currentUser', JSON.stringify(usuarioActualizado));
          }
        }
        return usuarioActualizado;
      }),
      catchError(error => {
        const errorMessage = error.error?.error || error.error?.message || 'Error al cambiar la contraseña';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  // Métodos para selección de cliente (solo para vendedores)
  getClientes(): Observable<Cliente[]> {
    return of(this.mockClients);
  }

  // Obtener todos los usuarios (solo para administradores)
  getUsuarios(): Observable<Usuario[]> {
    const token = this.getToken();
    if (!token) {
      console.error('🔴 [AUTH SERVICE] No hay token disponible para obtener usuarios');
      return throwError(() => new Error('No hay token de autenticación. Por favor, inicia sesión nuevamente.'));
    }
    
    console.log('🔵 [AUTH SERVICE] Obteniendo usuarios con token:', token.substring(0, 20) + '...');
    return this.http.get<Usuario[]>(`${this.API_URL}/usuarios`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(error => {
          console.error('🔴 [AUTH SERVICE] Error al obtener usuarios:', error);
          console.error('🔴 [AUTH SERVICE] Status:', error.status);
          console.error('🔴 [AUTH SERVICE] StatusText:', error.statusText);
          console.error('🔴 [AUTH SERVICE] Error message:', error.message);
          console.error('🔴 [AUTH SERVICE] Error body:', error.error);
          
          if (error.status === 401 || error.status === 403) {
            return throwError(() => new Error('No tienes permisos para acceder a la lista de usuarios. Por favor, inicia sesión nuevamente.'));
          }
          
          return throwError(() => new Error(error.error?.message || error.message || 'Error al cargar usuarios'));
        })
      );
  }

  puedeCambiarRolAAdmin(usuarioId: number | null | undefined): Observable<boolean> {
    if (usuarioId == null) {
      return of(false);
    }

    return this.http.get<{ allowed: boolean }>(
      `${this.API_URL}/usuarios/${usuarioId}/puede-cambiar-a-admin`,
      { headers: this.getAuthHeaders() }
    ).pipe(
      map(response => {
        if (!response) {
          return false;
        }
        return response.allowed ?? false;
      }),
      catchError(error => {
        console.error('Error al verificar permiso para cambiar a admin:', error);
        return of(false);
      })
    );
  }

  selectClient(cliente: Cliente): void {
    this.selectedClientSubject.next(cliente);
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem('selectedClient', JSON.stringify(cliente));
    }
  }

  getSelectedClient(): Cliente | null {
    return this.selectedClientSubject.value;
  }

  clearSelectedClient(): void {
    this.selectedClientSubject.next(null);
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('selectedClient');
    }
  }

  // Método para acceso demo sin autenticación (deshabilitado, usar backend)
}
