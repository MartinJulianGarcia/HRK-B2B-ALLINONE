import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginRequest } from '../../../core/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent {
  credentials: LoginRequest = { email: '', password: '' };
  loading = false;
  error = '';
  mostrarRecuperacion = false;
  emailRecuperacion = '';
  recuperando = false;
  mensajeRecuperacion = '';
  mostrarModal = false;
  modalMensaje = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit(): void {
    if (this.loading) return;

    this.loading = true;
    this.error = '';

    this.authService.login(this.credentials).subscribe({
      next: (user) => {
        this.loading = false;
        // Todos los usuarios van al catálogo después del login
        this.router.navigate(['/catalog']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.message || 'Error al iniciar sesión';
        this.abrirModal('Datos ingresados erróneos. Por favor, verifica tu email y contraseña.');
        this.clearForm();
        console.log('Login error:', err);
      }
    });
  }

  clearForm(): void {
    this.credentials = { email: '', password: '' };
  }

  toggleRecuperacion(): void {
    this.mostrarRecuperacion = !this.mostrarRecuperacion;
    this.emailRecuperacion = '';
    this.mensajeRecuperacion = '';
  }

  solicitarRecuperacion(): void {
    if (!this.emailRecuperacion || !this.emailRecuperacion.includes('@')) {
      this.abrirModal('Por favor ingresa un email válido.');
      return;
    }

    this.recuperando = true;
    this.mensajeRecuperacion = '';

    this.authService.recuperarContraseña(this.emailRecuperacion).subscribe({
      next: (response) => {
        this.recuperando = false;
        this.mensajeRecuperacion = response.message || 'Se ha enviado un email con tu contraseña';
        this.abrirModal(this.mensajeRecuperacion);
        this.mostrarRecuperacion = false;
        this.emailRecuperacion = '';
      },
      error: (error) => {
        this.recuperando = false;
        this.mensajeRecuperacion = error.message || 'Error al solicitar recuperación de contraseña';
        this.abrirModal(this.mensajeRecuperacion);
      }
    });
  }

  abrirModal(mensaje: string): void {
    this.modalMensaje = mensaje;
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.modalMensaje = '';
  }
}
