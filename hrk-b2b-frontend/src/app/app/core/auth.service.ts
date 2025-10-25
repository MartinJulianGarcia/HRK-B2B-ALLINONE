import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map, catchError } from 'rxjs/operators';

export interface Usuario {
  id: number;
  nombreRazonSocial: string;
  email: string;
  cuit: string;
  tipoUsuario: 'CLIENTE' | 'ADMIN';
  fechaCreacion: string;
  activo: boolean;
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
  
  private readonly API_URL = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {
    // Verificar si hay usuario logueado en localStorage (solo en el cliente)
    if (typeof window !== 'undefined' && window.localStorage) {
      const savedUser = localStorage.getItem('currentUser');
      const savedToken = localStorage.getItem('token');
      if (savedUser && savedToken) {
        this.currentUserSubject.next(JSON.parse(savedUser));
        this.tokenSubject.next(savedToken);
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
    return this.http.post<AuthResponse>(`${this.API_URL}/auth/login`, credentials)
      .pipe(
        map(response => {
          // Guardar token y usuario
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('token', response.token);
            localStorage.setItem('currentUser', JSON.stringify(response.usuario));
          }
          
          this.tokenSubject.next(response.token);
          this.currentUserSubject.next(response.usuario);
          return response.usuario;
        }),
        catchError(error => {
          console.error('Error en login:', error);
          return throwError(() => new Error('Credenciales inválidas'));
        })
      );
  }

  register(userData: RegisterRequest): Observable<Usuario> {
    console.log('🔵 [FRONTEND] Enviando petición de registro:', userData);
    console.log('🔵 [FRONTEND] URL:', `${this.API_URL}/auth/register`);
    
    return this.http.post<AuthResponse>(`${this.API_URL}/auth/register`, userData)
      .pipe(
        map(response => {
          // ✅ CRÍTICO: Validar que response no sea null
          if (!response || !response.token || !response.usuario) {
            throw new Error('Respuesta de registro incompleta o inválida.');
          }
          
          // Guardar token y usuario
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.setItem('token', response.token);
            localStorage.setItem('currentUser', JSON.stringify(response.usuario));
          }
          
          this.tokenSubject.next(response.token);
          this.currentUserSubject.next(response.usuario);
          return response.usuario;
        }),
        catchError(error => {
          console.error('Error en registro:', error);
          let errorMessage = 'Error al registrar usuario';
          
          if (error.error && error.error.message) {
            errorMessage = error.error.message;
          } else if (error.status === 400) {
            errorMessage = 'Datos inválidos. Verifica que el email y CUIT no estén registrados.';
          }
          
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
    return new HttpHeaders({
      'Authorization': token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json'
    });
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

  // Métodos para selección de cliente (solo para vendedores)
  getClientes(): Observable<Cliente[]> {
    return of(this.mockClients);
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
