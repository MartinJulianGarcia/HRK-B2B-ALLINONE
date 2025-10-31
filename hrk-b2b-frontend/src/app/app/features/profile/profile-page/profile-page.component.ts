import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-page.component.html',
  styleUrls: ['./profile-page.component.scss']
})
export class ProfilePageComponent implements OnInit {
  user: any = null;
  memberSince: string = '';
  editando = false;
  editForm = {
    nombreRazonSocial: '',
    cuit: ''
  };
  guardando = false;
  error = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserData();
  }

  loadUserData(): void {
    const userData = this.authService.getCurrentUser();
    if (userData) {
      this.user = userData;
      // Simular fecha de registro (en un caso real vendría del backend)
      this.memberSince = this.getMemberSince();
    } else {
      // Si no hay usuario logueado, redirigir al login
      this.router.navigate(['/login']);
    }
  }

  getMemberSince(): string {
    if (this.user && this.user.fechaCreacion) {
      const date = new Date(this.user.fechaCreacion);
      const months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                     'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
      return `${months[date.getMonth()]} ${date.getFullYear()}`;
    }
    // Fallback si no hay fecha
    const currentDate = new Date();
    const months = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
                   'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    return `${months[currentDate.getMonth()]} ${currentDate.getFullYear()}`;
  }

  goBack(): void {
    this.router.navigate(['/catalog']);
  }

  editProfile(): void {
    if (!this.user) return;
    
    this.editando = true;
    this.editForm = {
      nombreRazonSocial: this.user.nombreRazonSocial || '',
      cuit: this.user.cuit || ''
    };
    this.error = '';
  }

  cancelarEdicion(): void {
    this.editando = false;
    this.editForm = {
      nombreRazonSocial: '',
      cuit: ''
    };
    this.error = '';
  }

  guardarCambios(): void {
    if (!this.user) return;

    // Validaciones
    if (!this.editForm.nombreRazonSocial || this.editForm.nombreRazonSocial.trim().length === 0) {
      this.error = 'El nombre es requerido';
      return;
    }

    if (!this.editForm.cuit || this.editForm.cuit.trim().length === 0) {
      this.error = 'El CUIT es requerido';
      return;
    }

    this.guardando = true;
    this.error = '';

    const datosActualizacion: { nombreRazonSocial: string; cuit: string } = {
      nombreRazonSocial: this.editForm.nombreRazonSocial.trim(),
      cuit: this.editForm.cuit.trim()
    };

    this.authService.actualizarPerfil(this.user.id, datosActualizacion).subscribe({
      next: (usuarioActualizado) => {
        this.user = usuarioActualizado;
        this.editando = false;
        this.guardando = false;
        alert('Perfil actualizado exitosamente');
      },
      error: (error) => {
        console.error('Error al actualizar perfil:', error);
        this.error = error.message || 'Error al actualizar el perfil';
        this.guardando = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewOrderHistory(): void {
    this.router.navigate(['/orders-history']);
  }

  cambiarRol(): void {
    if (!this.user) return;
    
    const nuevoRol = this.user.tipoUsuario === 'CLIENTE' ? 'ADMIN' : 'CLIENTE';
    const confirmacion = confirm(`¿Estás seguro que quieres cambiar tu rol a ${nuevoRol}?`);
    
    if (confirmacion) {
      this.authService.cambiarRol(this.user.id, nuevoRol).subscribe({
        next: (usuarioActualizado) => {
          this.user = usuarioActualizado;
          alert(`Rol cambiado exitosamente a ${nuevoRol}`);
        },
        error: (error) => {
          console.error('Error al cambiar rol:', error);
          alert('Error al cambiar el rol. Inténtalo de nuevo.');
        }
      });
    }
  }
}
