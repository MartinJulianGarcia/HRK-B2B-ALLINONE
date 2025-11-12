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
  verProductosOcultos = false; // Preferencia para ver productos ocultos (solo admin)
  mostrarModalCodigo = false; // Controla si se muestra el modal de código
  codigoValidacion = ''; // Código ingresado por el usuario
  errorCodigo = ''; // Error del código
  private readonly CODIGO_ADMIN = 'cascuino'; // Código hardcodeado para cambio a admin
  mostrarBotonCambioRol = false;
  verificandoPermisoCambioRol = false;
  mostrarCambioPassword = false;
  cambiandoPassword = false;
  passwordForm = {
    passwordActual: '',
    nuevaPassword: '',
    confirmarPassword: ''
  };
  errorPassword = '';
  mensajePassword = '';

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    // Cargar preferencia de ver productos ocultos desde localStorage (solo admin)
    if (this.authService.isAdmin()) {
      const preferenciaGuardada = localStorage.getItem('verProductosOcultos');
      this.verProductosOcultos = preferenciaGuardada === 'true';
    }
  }

  loadUserData(): void {
    const userData = this.authService.getCurrentUser();
    if (userData) {
      this.user = userData;
      // Simular fecha de registro (en un caso real vendría del backend)
      this.memberSince = this.getMemberSince();
      this.actualizarDisponibilidadCambioRol();
      if (this.user.mustChangePassword) {
        this.editProfile();
        this.mostrarCambioPassword = true;
      }
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
    if (this.user.mustChangePassword) {
      this.mostrarCambioPassword = true;
    }
  }

  cancelarEdicion(): void {
    if (this.user?.mustChangePassword) {
      this.error = 'Debes actualizar tu contraseña antes de salir.';
      return;
    }
    this.editando = false;
    this.editForm = {
      nombreRazonSocial: '',
      cuit: ''
    };
    this.error = '';
    this.resetPasswordForm();
    this.mostrarCambioPassword = false;
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

  toggleCambioPassword(): void {
    if (this.user?.mustChangePassword) {
      return;
    }
    this.mostrarCambioPassword = !this.mostrarCambioPassword;
    this.errorPassword = '';
    this.mensajePassword = '';
    if (!this.mostrarCambioPassword) {
      this.resetPasswordForm();
    } else if (this.user?.mustChangePassword) {
      // Si debe cambiar la contraseña, asegurarse de limpiar campos
      this.passwordForm.passwordActual = '';
    }
  }

  guardarNuevaPassword(): void {
    if (!this.user) {
      return;
    }

    this.errorPassword = '';
    this.mensajePassword = '';

    if (!this.passwordForm.nuevaPassword || this.passwordForm.nuevaPassword.trim().length < 6) {
      this.errorPassword = 'La nueva contraseña debe tener al menos 6 caracteres';
      return;
    }

    if (this.passwordForm.nuevaPassword !== this.passwordForm.confirmarPassword) {
      this.errorPassword = 'Las contraseñas no coinciden';
      return;
    }

    if (!this.authService.isAdmin() && (!this.passwordForm.passwordActual || this.passwordForm.passwordActual.trim().length === 0)) {
      this.errorPassword = 'Debes ingresar tu contraseña actual';
      return;
    }

    this.cambiandoPassword = true;

    this.authService.cambiarPassword(this.user.id, {
      passwordActual: this.passwordForm.passwordActual || undefined,
      nuevaPassword: this.passwordForm.nuevaPassword.trim(),
      confirmarPassword: this.passwordForm.confirmarPassword.trim()
    }).subscribe({
      next: (usuarioActualizado) => {
        this.user = usuarioActualizado;
        this.cambiandoPassword = false;
        this.mensajePassword = 'Contraseña actualizada correctamente';
        this.passwordForm.passwordActual = '';
        this.passwordForm.nuevaPassword = '';
        this.passwordForm.confirmarPassword = '';
        if (this.user.mustChangePassword) {
          this.user.mustChangePassword = false;
        }
      },
      error: (error) => {
        console.error('Error al cambiar contraseña:', error);
        this.errorPassword = error.message || 'Error al cambiar la contraseña';
        this.cambiandoPassword = false;
      }
    });
  }

  private resetPasswordForm(): void {
    this.passwordForm = {
      passwordActual: '',
      nuevaPassword: '',
      confirmarPassword: ''
    };
    this.errorPassword = '';
    this.mensajePassword = '';
    this.cambiandoPassword = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  viewOrderHistory(): void {
    this.router.navigate(['/orders-history']);
  }

  goToDashboards(): void {
    this.router.navigate(['/dashboards']);
  }

  goToManageProducts(): void {
    this.router.navigate(['/manage-products']);
  }

  toggleVerProductosOcultos(event: any): void {
    this.verProductosOcultos = event.target.checked;
    // Guardar preferencia en localStorage
    localStorage.setItem('verProductosOcultos', this.verProductosOcultos.toString());
    console.log('🔵 [PROFILE] Preferencia de ver productos ocultos actualizada:', this.verProductosOcultos);
  }

  cambiarRol(): void {
    if (!this.user) return;
    
    const nuevoRol = this.user.tipoUsuario === 'CLIENTE' ? 'ADMIN' : 'CLIENTE';
    
    // Si el nuevo rol es ADMIN, pedir código de validación
    if (nuevoRol === 'ADMIN') {
      this.mostrarModalCodigo = true;
      this.codigoValidacion = '';
      this.errorCodigo = '';
    } else {
      // Si es CLIENTE, cambiar directamente
      const confirmacion = confirm(`¿Estás seguro que quieres cambiar tu rol a ${nuevoRol}?`);
      
      if (confirmacion) {
        this.ejecutarCambioRol(nuevoRol);
      }
    }
  }

  cerrarModalCodigo(): void {
    this.mostrarModalCodigo = false;
    this.codigoValidacion = '';
    this.errorCodigo = '';
  }

  validarYCambiarRol(): void {
    if (!this.codigoValidacion || this.codigoValidacion.trim().length === 0) {
      this.errorCodigo = 'Por favor ingresa el código de validación';
      return;
    }

    // Validar código (case-insensitive)
    if (this.codigoValidacion.trim().toLowerCase() !== this.CODIGO_ADMIN.toLowerCase()) {
      this.errorCodigo = 'Código incorrecto. Inténtalo de nuevo.';
      return;
    }

    // Código correcto, cerrar modal y proceder con el cambio
    this.cerrarModalCodigo();
    const confirmacion = confirm('¿Estás seguro que quieres cambiar tu rol a ADMIN?');
    
    if (confirmacion) {
      this.ejecutarCambioRol('ADMIN');
    }
  }

  private ejecutarCambioRol(nuevoRol: 'CLIENTE' | 'ADMIN'): void {
    if (!this.user) return;

    this.authService.cambiarRol(this.user.id, nuevoRol).subscribe({
      next: (usuarioActualizado) => {
        this.user = usuarioActualizado;
        alert(`Rol cambiado exitosamente a ${nuevoRol}`);
        this.actualizarDisponibilidadCambioRol();
      },
      error: (error) => {
        console.error('Error al cambiar rol:', error);
        alert('Error al cambiar el rol. Inténtalo de nuevo.');
      }
    });
  }

  private actualizarDisponibilidadCambioRol(): void {
    if (!this.user) {
      this.mostrarBotonCambioRol = false;
      return;
    }

    this.verificandoPermisoCambioRol = true;
    this.authService.puedeCambiarRolAAdmin(this.user.id).subscribe({
      next: (permitido) => {
        this.mostrarBotonCambioRol = permitido && this.user?.tipoUsuario !== 'ADMIN';
        this.verificandoPermisoCambioRol = false;
      },
      error: () => {
        this.mostrarBotonCambioRol = false;
        this.verificandoPermisoCambioRol = false;
      }
    });
  }
}
